package org.jdownloader.updatev2;

import java.awt.Window;
import java.io.IOException;
import java.util.List;

public interface UpdateHandler {

    void startIntervalChecker();

    void runUpdateCheck(boolean manually);

    void setGuiVisible(boolean b, boolean toFront);

    boolean isGuiVisible();

    String getAppID();

    // List<File> getFileList(File awfFile) throws InterruptedException, IOException;

    Window getGuiFrame();

    public String[] getOptionalsList() throws IOException;

    void installPendingUpdates(InstallLog log);

    boolean hasPendingUpdates();

    boolean hasPendingClientUpdates();

    boolean hasPendingSelfupdate();

    InstallLog createAWFInstallLog() throws InterruptedException, IOException;

    void setGuiFinished(String message);

    boolean isExtensionInstalled(String id);

    void uninstallExtension(String... ids) throws InterruptedException;

    void installExtension(String... ids) throws InterruptedException;

    void waitForUpdate() throws InterruptedException;

    List<String> getRequestedInstalls();

    List<String> getRequestedUnInstalls();

    void stopIntervalChecker();

}
