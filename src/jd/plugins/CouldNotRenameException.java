package jd.plugins;

import java.io.File;

public class CouldNotRenameException extends Exception {

    private File source;

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public File getDest() {
        return dest;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    private File dest;

    public CouldNotRenameException(File old, File newFile) {
        this(old, newFile, "Renaming file " + old + " to " + newFile + " failed");
    }

    public CouldNotRenameException(File old, File newFile, String msg) {
        super(msg);

        this.source = old;
        this.dest = newFile;
    }
}
