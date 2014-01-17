package org.jdownloader.updatev2;

import java.util.EventListener;

import javax.swing.Icon;

public interface UpdaterListener extends EventListener {

    void onUpdatesAvailable(boolean selfupdate, InstallLog installlog);

    void onUpdaterStatusUpdate(String label, Icon icon, double progress);

}