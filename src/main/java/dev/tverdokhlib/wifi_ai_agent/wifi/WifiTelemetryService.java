package dev.tverdokhlib.wifi_ai_agent.wifi;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.tverdokhlib.wifi_ai_agent.routeros.RouterOsClient;
import dev.tverdokhlib.wifi_ai_agent.routeros.RouterOsException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class WifiTelemetryService {

    static final String WIFI_INTERFACES = "/rest/interface/wifi";
    static final String WIFI_REGISTRATION = "/rest/interface/wifi/registration-table";
    static final String WIFI_MONITOR = "/rest/interface/wifi/monitor";

    static final String WIRELESS_INTERFACES = "/rest/interface/wireless";
    static final String WIRELESS_REGISTRATION = "/rest/interface/wireless/registration-table";
    static final String WIRELESS_MONITOR = "/rest/interface/wireless/monitor";

    static final String INTERFACE_PRINT = "/rest/interface/print";

    private static final List<String> INTERFACE_STATS_PROPLIST = List.of(
            "name",
            "tx-byte",
            "rx-byte",
            "tx-packet",
            "rx-packet",
            "tx-error",
            "rx-error",
            "tx-drop",
            "rx-drop",
            "tx-queue-drop");

    private final RouterOsClient routerOsClient;
    private final Clock clock;

    public WifiTelemetryService(RouterOsClient routerOsClient, Clock clock) {
        this.routerOsClient = routerOsClient;
        this.clock = clock;
    }

    public WifiTelemetrySnapshot collectSnapshot() {
        Instant collectedAt = Instant.now(clock);
        DetectedWifiMenu menu = detectWifiMenu();

        List<WifiClientTelemetry> clients = mapClients(getRegistrations(menu));
        Map<String, Integer> clientCountByInterface = buildClientCountByInterface(menu.interfaces(), clients);

        Map<String, JsonNode> countersByName = fetchInterfaceStatsSafely(menu.interfaces());
        Map<String, JsonNode> monitorByName = fetchMonitorSafely(menu.monitorPath(), menu.interfaces());

        List<WifiInterfaceTelemetry> interfaces = mapInterfaces(
                menu.interfaces(),
                clientCountByInterface,
                countersByName,
                monitorByName);

        return new WifiTelemetrySnapshot(
                collectedAt,
                interfaces,
                clients,
                clients.size(),
                clientCountByInterface);
    }

    /**
     * Returns Wi-Fi interfaces without delegating to collectSnapshot().
     */
    public List<WifiInterfaceTelemetry> getInterfaces() {
        DetectedWifiMenu menu = detectWifiMenu();
        List<WifiClientTelemetry> clients = mapClients(getRegistrations(menu));
        Map<String, Integer> clientCountByInterface = buildClientCountByInterface(menu.interfaces(), clients);

        return mapInterfaces(
                menu.interfaces(),
                clientCountByInterface,
                fetchInterfaceStatsSafely(menu.interfaces()),
                fetchMonitorSafely(menu.monitorPath(), menu.interfaces()));
    }

    /**
     * Returns connected clients directly from the RouterOS registration table.
     * Interface statistics and monitor calls are intentionally not required.
     */
    public List<WifiClientTelemetry> getClients() {
        DetectedWifiMenu menu = detectWifiMenu();
        return mapClients(getRegistrations(menu));
    }

    /**
     * This demo router uses the legacy /interface/wireless subsystem, so check it
     * first. Fall back to /interface/wifi for routers using the newer WiFi stack.
     */
    private DetectedWifiMenu detectWifiMenu() {
        Optional<List<JsonNode>> wireless = routerOsClient.getArrayIfPresent(WIRELESS_INTERFACES);
        if (wireless.isPresent() && !wireless.get().isEmpty()) {
            return new DetectedWifiMenu(wireless.get(), WIRELESS_REGISTRATION, WIRELESS_MONITOR);
        }

        Optional<List<JsonNode>> wifi = routerOsClient.getArrayIfPresent(WIFI_INTERFACES);
        if (wifi.isPresent() && !wifi.get().isEmpty()) {
            return new DetectedWifiMenu(wifi.get(), WIFI_REGISTRATION, WIFI_MONITOR);
        }

        return new DetectedWifiMenu(List.of(), null, null);
    }

    private List<JsonNode> getRegistrations(DetectedWifiMenu menu) {
        if (menu.registrationPath() == null) {
            return List.of();
        }

        return routerOsClient
                .getArrayIfPresent(menu.registrationPath())
                .orElse(List.of());
    }

    private static List<WifiClientTelemetry> mapClients(List<JsonNode> registrations) {
        return registrations.stream()
                .map(RouterOsWifiMapper::mapClient)
                .toList();
    }

    private static Map<String, Integer> buildClientCountByInterface(
            List<JsonNode> apInterfaces,
            List<WifiClientTelemetry> clients) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (JsonNode ap : apInterfaces) {
            String name = RouterOsWifiMapper.interfaceName(ap);
            if (name != null) {
                counts.put(name, 0);
            }
        }

        for (WifiClientTelemetry client : clients) {
            if (client.interfaceName() != null) {
                counts.merge(client.interfaceName(), 1, Integer::sum);
            }
        }

        return counts;
    }

    private static List<WifiInterfaceTelemetry> mapInterfaces(
            List<JsonNode> apInterfaces,
            Map<String, Integer> clientCountByInterface,
            Map<String, JsonNode> countersByName,
            Map<String, JsonNode> monitorByName) {

        List<WifiInterfaceTelemetry> result = new ArrayList<>(apInterfaces.size());

        for (JsonNode ap : apInterfaces) {
            String name = RouterOsWifiMapper.interfaceName(ap);
            int clientCount = name == null ? 0 : clientCountByInterface.getOrDefault(name, 0);

            result.add(RouterOsWifiMapper.mapInterface(
                    ap,
                    name == null ? null : countersByName.get(name),
                    name == null ? null : monitorByName.get(name),
                    clientCount));
        }

        return List.copyOf(result);
    }

    /**
     * Interface counters are optional enrichment. Failure here must not break
     * client telemetry or the whole Wi-Fi snapshot.
     */
    private Map<String, JsonNode> fetchInterfaceStatsSafely(List<JsonNode> apInterfaces) {
        if (apInterfaces.isEmpty()) {
            return Map.of();
        }

        try {
            return indexByName(routerOsClient.postArray(
                    INTERFACE_PRINT,
                    Map.of(
                            "stats-detail", "",
                            ".proplist", INTERFACE_STATS_PROPLIST)));
        }
        catch (RouterOsException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                throw ex;
            }
            return Map.of();
        }
    }

    /**
     * Monitor data such as channel/noise floor is optional enrichment.
     */
    private Map<String, JsonNode> fetchMonitorSafely(String monitorPath, List<JsonNode> apInterfaces) {
        if (monitorPath == null || apInterfaces.isEmpty()) {
            return Map.of();
        }

        List<String> names = new ArrayList<>();
        for (JsonNode ap : apInterfaces) {
            String name = RouterOsWifiMapper.interfaceName(ap);
            if (name != null) {
                names.add(name);
            }
        }

        if (names.isEmpty()) {
            return Map.of();
        }

        try {
            List<JsonNode> monitorRows = routerOsClient.postArray(
                    monitorPath,
                    Map.of(
                            "numbers", String.join(",", names),
                            "once", ""));

            Map<String, JsonNode> byName = new HashMap<>();
            for (JsonNode row : monitorRows) {
                String name = RouterOsWifiMapper.interfaceName(row);
                if (name != null) {
                    byName.put(name, row);
                }
            }

            // Some RouterOS monitor responses omit the interface name. In that case
            // rows are associated with the requested interface order.
            int limit = Math.min(names.size(), monitorRows.size());
            for (int i = 0; i < limit; i++) {
                byName.putIfAbsent(names.get(i), monitorRows.get(i));
            }

            return byName;
        }
        catch (RouterOsException ex) {
            if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                throw ex;
            }
            return Map.of();
        }
    }

    private static Map<String, JsonNode> indexByName(List<JsonNode> nodes) {
        Map<String, JsonNode> byName = new HashMap<>();
        for (JsonNode node : nodes) {
            String name = RouterOsWifiMapper.interfaceName(node);
            if (name != null) {
                byName.put(name, node);
            }
        }
        return byName;
    }

    private record DetectedWifiMenu(
            List<JsonNode> interfaces,
            String registrationPath,
            String monitorPath) {
    }
}
