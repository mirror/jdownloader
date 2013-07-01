package org.jdownloader.updatev2;

import org.appwork.shutdown.BasicShutdownRequest;

public class BasicRestartRequest extends BasicShutdownRequest implements RestartRequest {

    private String[] arguments;

    public BasicRestartRequest(String... arguments) {
        this.arguments = arguments;
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

}
