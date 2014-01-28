package org.jdownloader.updatev2;

import java.util.Arrays;

import org.appwork.shutdown.BasicShutdownRequest;

public class BasicRestartRequest extends BasicShutdownRequest implements RestartRequest {

    private String[] arguments;

    public BasicRestartRequest(String... arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "BasicRestartRequest " + Arrays.toString(arguments);
    }

    @Override
    public String[] getArguments() {
        return arguments;
    }

}
