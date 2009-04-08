package jd.plugins;

import java.util.EventListener;

public interface FilePackageListener extends EventListener {
    public void handle_FilePackageEvent(FilePackageEvent event);
}
