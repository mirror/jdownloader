package org.jdownloader.controlling.download;

import jd.controlling.downloadcontroller.DownloadController;

import org.appwork.utils.event.SimpleEvent;

public abstract class DownloadControllerEvent extends SimpleEvent<Object, Object, DownloadControllerEvent.TYPE> {

    public static enum TYPE {
        /**
         * no parameters
         */
        REFRESH_STRUCTURE,
        REMOVE_CONTENT,
        ADD_CONTENT,
        /**
         * Paramers:<br>
         * either<br>
         * [0]=FilePackage<br>
         * [1]=FilePackageProperty<br>
         * or <br>
         * [0]=DownloadLink<br>
         * [1]=LinkStatusPropery<br>
         */
        REFRESH_CONTENT
    }

    public DownloadControllerEvent(TYPE type, Object... parameters) {
        super(DownloadController.getInstance(), type, parameters);
    }

    abstract public void sendToListener(DownloadControllerListener listener);
}