package org.jdownloader.controlling;

import java.io.File;

import org.appwork.utils.event.SimpleEvent;

public class FileCreationEvent extends SimpleEvent<Object, Object, FileCreationEvent.Type> {

    public static enum Type {
        /**
         * Parameter[0] File[]
         */
        NEW_FILES,
        /**
         * Parameter[0] File
         * 
         * */
        NEW_FOLDER
    }

    private File[] files;

    public File[] getFiles() {
        return files;
    }

    public FileCreationEvent(Object caller, Type type, File... files) {
        super(caller, type);
        this.files = files;
    }
}