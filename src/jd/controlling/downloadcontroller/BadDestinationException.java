package jd.controlling.downloadcontroller;

import java.io.File;

public class BadDestinationException extends Exception {

    private File file;

    public File getFile() {
        return file;
    }

    public BadDestinationException(File file) {
        this.file = file;
    }

}
