package dev.tverdokhlib.wifi_ai_agent.ai;

/** Concept 6: the four explicit orchestration workflows in this demo. */
public enum RequestType {
    CURRENT_STATE(true, false),
    DOCUMENTATION(false, true),
    TROUBLESHOOTING(true, true),
    GENERAL(false, false);

    private final boolean tools;
    private final boolean rag;

    RequestType(boolean tools, boolean rag) {
        this.tools = tools;
        this.rag = rag;
    }

    public boolean usesTools() {
        return tools;
    }

    public boolean usesRag() {
        return rag;
    }
}
