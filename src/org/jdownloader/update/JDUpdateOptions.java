package org.jdownloader.update;

import org.appwork.update.updateclient.UpdaterOptions;
import org.appwork.utils.Application;

public class JDUpdateOptions implements UpdaterOptions {

    public String getApp() {
        return "JDownloader";
    }

    public String getBranch() {
        return null;
    }

    public boolean getGuiless() {
        return false;
    }

    public boolean getOsFilter() {
        return false;
    }

    public long getPackagePollInterval() {
        return 15000;
    }

    public String getRestart() {
        return null;
    }

    public String[] getUpdServer() {
        return new String[] { "http://upd0.appwork.org/jcgi/" };
    }

    public String getWorkingDirectory() {
        return Application.getRoot();
    }

    public void setApp(String app) {
    }

    public void setBranch(String branch) {
    }

    public void setGuiless(boolean b) {
    }

    public void setOsFilter(boolean b) {
    }

    public void setRestart(String b) {
    }

    public boolean getDebug() {
        return !Application.isJared(JDUpdateOptions.class);
    }

    public void setDebug(boolean b) {

    }

    public void setWorkinfDirectory(String dir) {
    }

}
