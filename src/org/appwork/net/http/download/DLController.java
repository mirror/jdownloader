package org.appwork.net.http.download;

public class DLController {
    private DLController() {

    }

    private static final DLController INSTANCE = new DLController();

    public static DLController getInstance() {
        return INSTANCE;
    }

    /**
     * adds a download and waits until it has finished
     * 
     * @param request
     */
    public void addAndWait(DownloadHandler handler) {
        // TODO Auto-generated method stub

    }
}
