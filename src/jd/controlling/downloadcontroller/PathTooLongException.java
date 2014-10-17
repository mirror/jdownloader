package jd.controlling.downloadcontroller;

import java.io.File;

public class PathTooLongException extends BadDestinationException {

    public PathTooLongException(File file) {
        super(file);
    }

}
