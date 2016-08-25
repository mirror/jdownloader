package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

public class FilePathSandbox17 extends FilePathSandbox {

    public FilePathSandbox17(String fileOrUrl) {
        super(fileOrUrl);
    }

    @Override
    public long getModifiedDate() {
        try {
            return java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    protected FilePathSandbox newFilePathSandbox(String file) {
        return new FilePathSandbox17(file);
    }

    @Override
    public long getCreatedDate() {
        try {
            return java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
        } catch (IOException e) {
            return -1;
        }
    }
}
