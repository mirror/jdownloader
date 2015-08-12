package jd.plugins.download.usenet;

import java.io.IOException;

import jd.plugins.download.usenet.SimpleUseNet.COMMAND;

public class UnrecognizedCommandException extends IOException {

    private final COMMAND command;

    public COMMAND getCommand() {
        return command;
    }

    public String getParameter() {
        return parameter;
    }

    private final String parameter;

    public UnrecognizedCommandException(COMMAND command, final String parameter) {
        this.command = command;
        this.parameter = parameter;
    }

    public UnrecognizedCommandException(COMMAND command) {
        this(command, null);
    }
}
