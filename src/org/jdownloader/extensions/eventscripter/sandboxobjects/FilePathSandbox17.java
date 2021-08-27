package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

import org.appwork.utils.Files17;

public class FilePathSandbox17 extends FilePathSandbox {
    public FilePathSandbox17(String fileOrUrl) {
        super(fileOrUrl);
    }

    protected FilePathSandbox17(File file) {
        super(file);
    }

    @Override
    public long getModifiedDate() {
        final File file = getFile();
        try {
            if (file != null) {
                return java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
            }
        } catch (IOException ignore) {
        }
        return -1;
    }

    @Override
    protected FilePathSandbox newFilePathSandbox(File file) {
        return new FilePathSandbox17(file);
    }

    public long getFreeDiskSpace() {
        final File file = getFile();
        if (file != null) {
            try {
                return Files17.getUsableSpace(file.toPath());
            } catch (IOException ignore) {
            }
        }
        return super.getFreeDiskSpace();
    }

    @Override
    public long getCreatedDate() {
        final File file = getFile();
        try {
            if (file != null) {
                return java.nio.file.Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
            }
        } catch (IOException ignore) {
        }
        return -1;
    }
}
