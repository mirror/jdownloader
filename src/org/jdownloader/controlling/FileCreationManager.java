package org.jdownloader.controlling;

public class FileCreationManager {
    private static final FileCreationManager INSTANCE = new FileCreationManager();

    /**
     * get the only existing instance of FileCreationManager. This is a
     * singleton
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
     * Create a new instance of FileCreationManager. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private FileCreationManager() {
        eventSender = new FileCreationEventSender();

    }
}
