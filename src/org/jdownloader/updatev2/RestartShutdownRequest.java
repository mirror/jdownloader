package org.jdownloader.updatev2;

public class RestartShutdownRequest {

    private final String[] arguments;

    public RestartShutdownRequest(String... arguments) {
        this.arguments = arguments;
    }

    /**
     * @return the arguments
     */
    public String[] getArguments() {
        return arguments;
    }
}
