package org.jdownloader.updatev2;

import java.util.EventListener;

public interface UpdaterListener extends EventListener {

    void onUpdatesAvailable(boolean selfupdate, InstallLog installlog);

}