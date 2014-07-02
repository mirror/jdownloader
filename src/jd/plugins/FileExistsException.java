package jd.plugins;

import java.io.File;

public class FileExistsException extends CouldNotRenameException {

    public FileExistsException(File old, File newFile) {
        super(old, newFile, "Creating file " + newFile + " failed. " + newFile + " already exists.");

    }

}
