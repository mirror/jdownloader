package org.jdownloader.updatev2;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;

public interface UpdateCallbackInterface {

    void updateGuiIcon(ImageIcon icon);

    void updateGuiText(String icon);

    /**
     * progress 0.0 ->100.0
     * 
     * @param progress
     */
    void updateGuiProgress(double progress);

    void setRunning(boolean b);

    boolean isRunning();

    boolean handleException(Exception e);

    void onGuiVisibilityChanged(Window window, boolean oldValue, boolean newValue);

    org.appwork.utils.swing.locator.Locator getGuiLocator();

    boolean doContinueLoopStarted();

    boolean doContinueUpdateAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision);

    boolean doContinuePackageAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision);

    boolean doContinueReadyForExtracting(boolean app, boolean updater, File appFile, File updaterFile);

    void onResults(boolean app, boolean updater, int clientRevision, int clientDestRevision, int selfRevision, int selfDestRevision, File clientAWF, File selfAWF, File selfWOrkingDir, boolean jdLaunched) throws InterruptedException, IOException, Exception;

    Process runExeAsynch(List<String> call, File root) throws IOException;

}
