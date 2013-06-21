package jd.controlling.downloadcontroller;

import java.util.HashMap;

import jd.plugins.FilePackage;

import org.jdownloader.settings.IfFileExistsAction;

public class DownloadSession {

    private DownloadSession                     parent;
    private long                                created;
    private HashMap<Object, IfFileExistsAction> fileExistsActions;
    private FileAccessManager                   fileAccessManager;
    private static final FileAccessManager      FILE_ACCESS_MANAGER = new FileAccessManager();

    public DownloadSession getParent() {
        return parent;
    }

    public long getCreated() {
        return created;
    }

    public DownloadSession(DownloadSession session) {
        // happy memleak. designed to make jiaz happy when reviewing this code!
        parent = session;
        created = System.currentTimeMillis();
        fileAccessManager = FILE_ACCESS_MANAGER;
    }

    public IfFileExistsAction getOnFileExistsAction(FilePackage filePackage) {
        return fileExistsActions == null ? null : fileExistsActions.get(filePackage.getUniqueID().toString());
    }

    public synchronized void setOnFileExistsAction(FilePackage filePackage, IfFileExistsAction doAction) {
        if (fileExistsActions == null) fileExistsActions = new HashMap<Object, IfFileExistsAction>();
        if (doAction == null) {
            fileExistsActions.remove(filePackage.getUniqueID().toString());
        } else {
            // let's use the unique id. else this map would hold a reference to the filepackage and avoid gc
            fileExistsActions.put(filePackage.getUniqueID().toString(), doAction);
        }
    }

    public FileAccessManager getFileAccessManager() {
        return fileAccessManager;
    }

}
