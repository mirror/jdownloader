package jd.updater;

public class WebUpdaterOptions {

    private boolean restart = true;
    private boolean guiless = false;

    public boolean isRestart() {
        return restart;
    }

    public void setRestart(boolean restart) {
        this.restart = restart;
    }

    public boolean isGuiless() {
        return guiless;
    }

    public void setGuiless(boolean guiless) {
        this.guiless = guiless;
    }

    public boolean isDisableOsfilter() {
        return disableOsfilter;
    }

    public void setDisableOsfilter(boolean disableOsfilter) {
        this.disableOsfilter = disableOsfilter;
    }

    public boolean isRestore() {
        return restore;
    }

    public void setRestore(boolean restore) {
        this.restore = restore;
    }

    private boolean disableOsfilter = false;
    private boolean restore         = false;
    private String  branch;

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
