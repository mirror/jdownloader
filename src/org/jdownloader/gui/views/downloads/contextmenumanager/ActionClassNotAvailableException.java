package org.jdownloader.gui.views.downloads.contextmenumanager;

public class ActionClassNotAvailableException extends Exception {

    public ActionClassNotAvailableException(String clazzName) {
        super(clazzName);
    }

}
