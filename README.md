# Wi-Fi AI Agent

A learning project for understanding the LLM-agent domain by implementing its core concepts in a simplified, working application.

## 1. Motivation: concepts and implementation

| Concept | Implementation |
| --- | --- |
| Java backend | Java 21, Spring Boot, Spring AI, real RouterOS integration, and Wi-Fi telemetry |
| Tool / function calling | The LLM invokes Java functions to request live RouterOS data |
| Prompt engineering | A system prompt defines the agent's role, constraints, and response format |
| RAG | The RouterOS PDF manual is split into chunks, embedded by Ollama, stored locally, and retrieved as LLM context |
| Context engineering | The model receives the system prompt, current question, up to 8 messages from the conversation, and—when selected—RAG chunks or tool definitions and results |
| LLM orchestration | A keyword-based classifier assigns a request type; the orchestrator then selects tools, RAG, both, or LLM only |
| Agent loops | The agent observes, decides, calls tools, updates context, and stops on completion or configured limits |
| Evaluation | Recurring offline evaluations run predefined scenarios before and after AI-system changes to detect regressions |

## 2. What it does

The application turns RouterOS telemetry and documentation into conversational Wi-Fi support. It can inspect interfaces and connected clients, explain RouterOS concepts, and suggest evidence-based troubleshooting steps.

Business value: faster network diagnostics without manually correlating raw telemetry with vendor documentation.

A web client is intentionally not implemented to keep the demo focused on the agent backend. REST requests are currently sent manually.

## 3. How it works

```text
HTTP request -> Spring Boot -> request classifier -> orchestrator
                                             |-> Ollama LLM
                                             |-> RouterOS tools
                                             |-> RouterOS manual RAG
                                             `-> tools + RAG
```

| Request type | Workflow |
| --- | --- |
| `CURRENT_STATE` | RouterOS tools + LLM |
| `DOCUMENTATION` | RAG + LLM |
| `TROUBLESHOOTING` | RouterOS tools + RAG + LLM |
| `GENERAL` | LLM only |

Classification is deterministic and keyword-based, with precedence: `TROUBLESHOOTING` -> `DOCUMENTATION` -> `CURRENT_STATE` -> `GENERAL`.

- Java 21 and Spring Boot expose the REST API.
- Spring AI integrates Ollama, tool calling, chat memory, and RAG.
- RouterOS REST API provides live router and Wi-Fi telemetry.
- RAG pipeline: bundled RouterOS PDF -> token chunks -> Ollama `nomic-embed-text` embeddings -> Spring AI `SimpleVectorStore` -> relevant chunks in the LLM context.
- The index is persisted in `data/routeros-vector-store.json` and loaded on startup; it is rebuilt from the PDF when the file is absent.
- Context window: system prompt + current question + up to 8 messages for the `conversationId` + optional RAG chunks + optional selected tool definitions and their RouterOS results.
- Guardrails: `ChatMemoryConfig` keeps 8 messages; `wifi.agent.rag.*` limits retrieval to 2 chunks with a `0.4` similarity threshold; `wifi.agent.loop.*` allows at most 2 calls per tool and 4 tool calls per request.

## 4. Run and use

Requirements: Java 21, Ollama, RouterOS with REST API access, and the configured Ollama models.

```powershell
ollama pull llama3.2:3b
ollama pull nomic-embed-text

$env:ROUTEROS_BASE_URL = "https://192.168.88.1"
$env:ROUTEROS_USERNAME = "admin"
$env:ROUTEROS_PASSWORD = "your-password"
$env:ROUTEROS_SSL_INSECURE = "true" # development only
.\mvnw.cmd spring-boot:run
```

Ask the agent:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/agent/ask?conversationId=demo" `
  -ContentType "text/plain" `
  -Body "Which clients are connected and is their signal healthy?"
```

Other example questions:

- `What is the RouterOS Registration Table?`
- `Why is my Wi-Fi slow? Check the current state.`
- `Which interface has the most connected clients?`
- `How many Wi-Fi clients are connected right now?`

## 5. Tests

| Motivation point | Focused test | What it verifies |
| --- | --- | --- |
| Java backend | `RouterOsClientTest`, `RouterOsWifiMapperTest`, `WifiTelemetryServiceTest` | RouterOS REST mapping and Wi-Fi telemetry collection |
| Tool / function calling | `WifiToolsTest` | Tool definitions delegate to the correct telemetry methods |
| Prompt engineering | `WifiAgentPromptTest` | Required role, rules, constraints, and response contract |
| RAG | `RouterOsRagTest` | Similarity search and RAG advisor configuration |
| Context engineering | `ChatMemoryConfigTest` | Conversation history is limited to 8 messages |
| LLM orchestration | `OrchestrationWorkflowTest` | Request classification and mapping to the four workflows |
| Agent loops | `WifiAgentLoopTest` | Tool selection and configured call limits |
| Evaluation | `AgentEvaluatorTest` | Scenario criteria, scoring, and failure reporting |

Run all focused tests:

```powershell
.\mvnw.cmd test
```

Run the tests for one motivation point:

```powershell
.\mvnw.cmd "-Dtest=RouterOsClientTest,RouterOsWifiMapperTest,WifiTelemetryServiceTest" test
.\mvnw.cmd -Dtest=WifiToolsTest test
.\mvnw.cmd -Dtest=WifiAgentPromptTest test
.\mvnw.cmd -Dtest=RouterOsRagTest test
.\mvnw.cmd -Dtest=ChatMemoryConfigTest test
.\mvnw.cmd -Dtest=OrchestrationWorkflowTest test
.\mvnw.cmd -Dtest=WifiAgentLoopTest test
.\mvnw.cmd -Dtest=AgentEvaluatorTest test
```

## 6. Evaluation

Evaluation is a continuous feedback activity, not a final stage. Establish a baseline, rerun evaluations after changes to prompts, tools, RAG, orchestration, or the model, compare results, and add newly discovered failures as regression scenarios.

```text
define expected behavior -> run baseline -> change the agent
  -> rerun scenarios -> compare results -> add discovered failures -> repeat
```

`WifiAgentBehaviorEvaluationTest` calls `WifiAgentService` directly (not the REST endpoint), so the real classifier, orchestrator, Ollama model, RouterOS tools, RAG store, and conversation memory are evaluated together. `AgentEvaluator` then checks the returned response.

`AgentEvaluator` checks every response using deterministic rules:

- response is not empty;
- response starts with `[WIFI-AGENT]`;
- response contains scenario-specific evidence, such as client, Registration Table, telemetry, recommendation, or signal information.

### Run evaluation

Start Ollama, make RouterOS reachable, and set its credentials. The application loads `data/routeros-vector-store.json` automatically.

```powershell
$env:RUN_AI_EVALUATIONS = "true"
$env:ROUTEROS_BASE_URL = "https://192.168.88.1"
$env:ROUTEROS_USERNAME = "admin"
$env:ROUTEROS_PASSWORD = "your-password"
$env:ROUTEROS_SSL_INSECURE = "true" # development/self-signed certificate only
mvn -Dtest=WifiAgentBehaviorEvaluationTest test
```

`BUILD SUCCESS` means every scenario satisfied all acceptance rules. A failure shows which rule failed and the model response that caused it.

### Evaluated scenarios and latest result

Run on 2026-09-01 with `llama3.2:3b` on CPU, a live RouterOS router, and `data/routeros-vector-store.json`:

| Scenario | Agent stages | Time | Automated result | Manual review |
| --- | --- | ---: | --- | --- |
| Current state | `CURRENT_STATE` -> `getWifiClients` -> RouterOS -> LLM | 178.626 s | 4/4 passed | Client count answered |
| Documentation | `DOCUMENTATION` -> RAG -> LLM | 158.715 s | 4/4 passed | Likely wrong meaning of Registration Table |
| Troubleshooting | `TROUBLESHOOTING` -> RAG -> `getWifiTelemetry` -> RouterOS -> LLM | 597.536 s | 5/5 passed | Contains unsupported signal and `ack-timeout` conclusions |
| General | `GENERAL` -> LLM | 41.885 s | 4/4 passed | Relevant, but duplicates `[WIFI-AGENT]` |
| Conversation memory | `CURRENT_STATE` -> memory + `getWifiClients` on both turns -> RouterOS -> LLM | 214.193 s | 4/4 per turn | Follow-up selects `-37 dBm` as worse than `-63 dBm`; incorrect |

### Real requests and responses

<details>
<summary>Current state</summary>

**Request:** `How many Wi-Fi clients are currently connected?`

**Response:**

```text
[WIFI-AGENT]

[CONCLUSION]
The current number of Wi-Fi clients connected is 5.

[RECOMMENDATION]
Check the Wi-Fi configuration and ensure that there are no restrictions or limitations that might be preventing additional clients from connecting.

[OBSERVED FACTS]
- The client count is 5.
- The clients are connected to either wlan1 or wlan2.
- The uptime for the clients ranges from 2 minutes to 4 days.
```

</details>

<details>
<summary>Documentation</summary>

**Request:** `According to the RouterOS manual, what is Registration Table?`

**Response:**

```text
[WIFI-AGENT]

According to the RouterOS manual, the Registration Table is used to store information about the installed RouterOS software, including the Software ID, Key, and license level. It is used for license management and is accessible through the /system license menu.
```

</details>

<details>
<summary>Troubleshooting</summary>

**Request:** `Why is the current Wi-Fi slow or unstable? Diagnose it from available evidence.`

**Response:**

```text
[WIFI-AGENT]

Conclusion: The current Wi-Fi slow or unstable issue may be related to the ack-timeout property, which is set to dynamic for both wireless interfaces. This could lead to inconsistent transmission and reception of data, causing slow or unstable Wi-Fi connections.

Observed facts:
- The ack-timeout property is set to dynamic for both wireless interfaces.
- The client count is 3, with 1 client connected to wlan1 and 2 clients connected to wlan2.
- The rssiDbm values for all clients are negative, indicating a weak signal.
- The txRate and rxRate values for all clients are 54Mbps, indicating a high-speed connection.

Recommendation: It is recommended to set the ack-timeout property to a fixed value, such as indoor, to ensure consistent transmission and reception of data. Additionally, it is recommended to check the tx-power property to ensure it is set to an appropriate value for the Atheros-based cards.
```

</details>

<details>
<summary>General</summary>

**Request:** `What kinds of Wi-Fi questions can you help me with?`

**Response:**

```text
[WIFI-AGENT]

I can help with a variety of Wi-Fi-related questions. Here are some examples:

- Issues with Wi-Fi connectivity, such as dropped connections, slow speeds, or no signal
- Configuration questions, such as setting up a new Wi-Fi network or configuring specific settings
- Troubleshooting questions, such as identifying the cause of a problem or resolving a specific issue
- Performance optimization questions, such as improving Wi-Fi speeds or reducing interference
- Questions about Wi-Fi security, such as configuring WPA2 or setting up a guest network
- Questions about Wi-Fi network planning, such as determining the best location for a Wi-Fi access point

Please let me know what specific question you have, and I'll do my best to help.

```
<summary>Conversation memory</summary>

**Request 1:** `List the currently connected Wi-Fi clients.`

**Response 1:** MAC addresses are redacted.

```text
[WIFI-AGENT]

Conclusion: The currently connected Wi-Fi clients are:
- Client 1: MAC address [REDACTED], connected to wlan2
- Client 2: MAC address [REDACTED], connected to wlan2
- Client 3: MAC address [REDACTED], connected to wlan1

Recommendation: Check the Wi-Fi interface settings and ensure that the wireless channels are not overlapping.

Observed facts:
- Client 1 is connected to wlan2 with an RSSI of -63 dBm.
- Client 2 is connected to wlan2 with an RSSI of -58 dBm.
- Client 3 is connected to wlan1 with an RSSI of -39 dBm.
```

**Request 2:** `Which of them has the worst signal?`

**Response 2:**

```text
[WIFI-AGENT]

Conclusion: The client with the worst signal is Client 3, with an RSSI of -37 dBm, connected to wlan1.

Recommendation: Check the wireless channel settings for wlan1 to ensure it is not overlapping with other channels.

Observed facts:
- Client 3 has the worst signal with an RSSI of -37 dBm.
- Client 1 has an RSSI of -63 dBm, which is also a poor signal.
- Client 2 has an RSSI of -58 dBm, which is a slightly better signal than Client 3.
```

</details>

The automated evaluator passed all scenarios because its criteria are structural and keyword-based. Manual review found factual and formatting problems that those criteria do not detect.

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 20:28 min
```
