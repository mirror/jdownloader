package org.jdownloader.extensions.schedulerV2.actions;

public class SetDownloadspeedActionConfig implements IScheduleActionConfig {
    public SetDownloadspeedActionConfig() {
    }

    public int getDownloadspeed() {
        return downloadspeed;
    }

    public void setDownloadspeed(int downloadspeed) {
        this.downloadspeed = downloadspeed;
    }

    private int downloadspeed = 1024 * 1024;

}
