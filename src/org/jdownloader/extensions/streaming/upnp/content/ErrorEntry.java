package org.jdownloader.extensions.streaming.upnp.content;

import org.jdownloader.controlling.UniqueAlltimeID;

public class ErrorEntry extends FolderContainer {

    public ErrorEntry(String string) {
        super(new UniqueAlltimeID().toString(), "[Error] " + string);
    }

}
