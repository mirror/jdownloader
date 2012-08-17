package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.util.ArrayList;

import org.appwork.storage.Storable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Lists;

public class DownloadPath implements Storable {
    @SuppressWarnings("unused")
    private DownloadPath(/* Storable */) {

    }

    public DownloadPath(String myPath) {
        path = myPath;
        time = System.currentTimeMillis();
    }

    public String getPath() {

        return path;
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        return hashCode() == obj.hashCode();
    }

    public int hashCode() {
        return path.hashCode();
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    private String path;
    private long   time;

    public static java.util.List<String> loadList(String pre) {
        java.util.List<DownloadPath> history = JsonConfig.create(LinkgrabberSettings.class).getDownloadDestinationHistory();

        if (history == null) {
            history = new ArrayList<DownloadPath>();
        }
        if (pre != null) history.add(0, new DownloadPath(pre));
        history = Lists.unique(history);
        java.util.List<String> quickSelectionList = new ArrayList<String>();
        for (DownloadPath dp : history) {
            quickSelectionList.add(dp.getPath());
        }
        return quickSelectionList;
    }

    public static void saveList(String absolutePath) {
        java.util.List<DownloadPath> history = JsonConfig.create(LinkgrabberSettings.class).getDownloadDestinationHistory();
        boolean found = false;
        for (DownloadPath pe : history) {
            if (pe != null && absolutePath != null && pe.getPath().equals(absolutePath)) {
                pe.setTime(System.currentTimeMillis());
                found = true;
                break;
            }
        }
        if (!found) {
            if (absolutePath != null) history.add(new DownloadPath(absolutePath));
        }

        JsonConfig.create(LinkgrabberSettings.class).setDownloadDestinationHistory(Lists.unique(history));

    }
}
