package org.jdownloader.extensions.streaming.upnp.content;

public class ErrorEntry extends FolderContainer {

    public ErrorEntry(String string) {
        super(string, "[Error] " + string);
    }

}
