package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.io.IOException;

import org.appwork.utils.Files;
import org.jdownloader.extensions.eventscripter.EnvironmentException;

public class FilePathSandbox {

    private final File file;

    public FilePathSandbox(String fileOrUrl) {
        file = new File(fileOrUrl);
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

    public FilePathSandbox getParent() {
        return new FilePathSandbox(file.getParent());
    }

    public FilePathSandbox[] getChildren() {
        final File[] files = file.listFiles();
        final FilePathSandbox[] ret;
        if (files == null || files.length == 0) {
            ret = new FilePathSandbox[0];
        } else {
            ret = new FilePathSandbox[files.length];
            for (int i = 0; i < files.length; i++) {
                ret[i] = new FilePathSandbox(files[i].getAbsolutePath());
            }
        }
        return ret;
    }

    public String getPath() {
        return file.getPath();
    }

    public boolean renameTo(String fileOrUrl) throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("rename or move files or folders");
        return file.renameTo(new File(fileOrUrl));
    }

    public boolean moveTo(String folder) throws EnvironmentException {
        org.jdownloader.extensions.eventscripter.sandboxobjects.ScriptEnvironment.askForPermission("move a file to a new folder");
        File dest = new File(folder);
        dest = new File(dest, file.getName());
        final boolean ret = file.renameTo(dest);
        return ret;
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public String toString() {
        return "Filepath Instance: " + getPath();
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
