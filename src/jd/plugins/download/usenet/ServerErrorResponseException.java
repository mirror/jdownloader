package jd.plugins.download.usenet;

import java.io.IOException;

import jd.plugins.download.usenet.SimpleUseNet.CommandResponse;

public class ServerErrorResponseException extends IOException {
    private final CommandResponse commandResponse;

    public CommandResponse getCommandResponse() {
        return commandResponse;
    }

    public ServerErrorResponseException(CommandResponse commandResponse) {
        this.commandResponse = commandResponse;

    }
}
