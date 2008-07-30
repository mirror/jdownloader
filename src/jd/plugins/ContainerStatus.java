package jd.plugins;

import java.io.File;

public class ContainerStatus {

    public static final int STATUS_FAILED = 1 << 2;
    public static final int STATUS_FINISHED = 1 << 1;
    public static final int TODO = 1 << 0;
    private File container;
    private int status = TODO;

    public ContainerStatus() {

    }

    public ContainerStatus(File lc) {
        container = lc;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;

    }

    public File getContainer() {
        return container;
    }

    public int getLatestStatus() {

        return 0;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status | status) > 0;
    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        this.status ^= status;
    }

    public void setErrorMessage(String string) {

    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatusText(String l) {

    }

}
