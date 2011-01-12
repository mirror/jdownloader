package jd.controlling.reconnect;

import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;

abstract public class ReconnectWizardProgress implements ProgressGetter {
    private String statusMessage;
    private int    progress = -1;

    public void setProgress(int progress) {
        // avoid that reconnect plugin set this value to 100% an cancel the
        // progress dialog this way
        if (progress >= 99) progress = 99;
        this.progress = progress;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getProgress() {
        return progress;
    }

    public String getString() {
        return this.statusMessage;
    }

    abstract public void run() throws Exception;

}
