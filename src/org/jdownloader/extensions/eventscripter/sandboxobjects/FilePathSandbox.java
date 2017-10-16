package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.io.IOException;

import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.jdownloader.extensions.eventscripter.EnvironmentException;

public class FilePathSandbox {
    protected final File file;

    public FilePathSandbox(String fileOrUrl) {
        file = new File(fileOrUrl);
    }

    @Override
    public int hashCode() {
        if (file != null) {
            return file.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FilePathSandbox) {
            return ((FilePathSandbox) obj).file == file;
        } else {
            return super.equals(obj);
        }
    }

    public boolean isFile() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("check if a filepath is a file");
        return file.isFile();
    }

    public boolean isDirectory() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("check if a filepath is a directory");
        return file.isDirectory();
    }

    public boolean exists() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("check if a filepath exists");
        final boolean ret = file.exists();
        return ret;
    }

    public boolean mkdirs() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("create folders");
        return file.mkdirs();
    }

    public long getModifiedDate() {
        return file.lastModified();
    }

    public long getCreatedDate() {
        return -1;
    }

    public FilePathSandbox getParent() {
        return newFilePathSandbox(file.getParent());
    }

    protected FilePathSandbox newFilePathSandbox(final String file) {
        return new FilePathSandbox(file);
    }

    public FilePathSandbox[] getChildren() {
        final File[] files = file.listFiles();
        final FilePathSandbox[] ret;
        if (files == null || files.length == 0) {
            ret = new FilePathSandbox[0];
        } else {
            ret = new FilePathSandbox[files.length];
            for (int i = 0; i < files.length; i++) {
                ret[i] = newFilePathSandbox(files[i].getAbsolutePath());
            }
        }
        return ret;
    }

    public String getPath() {
        return file.getPath();
    }

    public boolean renameTo(String to) throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("rename or move files or folders");
        final File dest = new File(to);
        return !dest.exists() && file.renameTo(dest);
    }

    public boolean moveTo(String folder) throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("move a file to a new folder");
        File dest = new File(folder);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        dest = new File(dest, file.getName());
        final boolean ret = !dest.exists() && file.renameTo(dest);
        return ret;
    }

    public boolean copyTo(String folder) throws EnvironmentException {
        if (isFile()) {
            org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("copy a file to a new folder");
            File dest = new File(folder);
            if (!dest.exists()) {
                dest.mkdirs();
            }
            dest = new File(dest, file.getName());
            try {
                if (!dest.exists()) {
                    IO.copyFile(file, dest);
                    return true;
                }
            } catch (final IOException e) {
                throw new EnvironmentException(e);
            }
        }
        return false;
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public String toString() {
        return getPath();
    }

    public boolean delete() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("delete a file or folder");
        return file.delete();
    }

    public boolean deleteRecursive() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("delete a file or folder RECURSIVE");
        try {
            Files.deleteRecursiv(file);
        } catch (IOException e) {
            throw new EnvironmentException(e);
        }
        return file.exists();
    }

    public String getName() {
        return file.getName();
    }

    public String getExtension() {
        return org.appwork.utils.Files.getExtension(file.getName());
    }

    public long getSize() throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("get the size of a file");
        return file.length();
    }
}
