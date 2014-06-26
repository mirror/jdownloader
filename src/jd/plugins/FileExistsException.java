package jd.plugins;

import java.io.File;

public class FileExistsException extends CouldNotRenameException {

    public FileExistsException(File newFile, File old) {
        super(old, newFile, "Creating file " + newFile + " failed. " + old + " already exists.");

    }

}
