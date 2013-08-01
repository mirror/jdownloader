package org.jdownloader.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.Application;

public class FileCreationManager {
    private static final FileCreationManager INSTANCE = new FileCreationManager();

    /**
     * get the only existing instance of FileCreationManager. This is a singleton
     * 
     * @return
     */
    public static FileCreationManager getInstance() {
        return FileCreationManager.INSTANCE;
    }

    private FileCreationEventSender eventSender;

    public FileCreationEventSender getEventSender() {
        return eventSender;
    }

    /**
     * Create a new instance of FileCreationManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private FileCreationManager() {
        eventSender = new FileCreationEventSender();
    }

    public boolean mkdir(File folder) {
        if (folder.exists()) return false;

        List<File> backlog = new ArrayList<File>();
        HashSet<String> loopcheck = new HashSet<String>();
        File copy = folder;

        while (copy != null && !copy.exists()) {
            if (loopcheck.add(copy.getAbsolutePath())) {
                backlog.add(copy);
            }
            copy = copy.getParentFile();

        }

        for (int i = backlog.size() - 1; i >= 0; i--) {
            if (backlog.get(i).mkdir()) {
                getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.NEW_FOLDER, backlog.get(i)));
            } else {
                return false;
            }
        }

        return true;
    }

    public boolean delete(File file) {
        if (!file.exists()) return false;
        //
        if (file.delete()) {
            // getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.REMOVE_FILES, file));
            return true;
        } else {
            if (Application.getJavaVersion() >= Application.JAVA17) {
                try {
                    java.nio.file.Files.delete(file.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return !file.exists();
        }
    }
}
