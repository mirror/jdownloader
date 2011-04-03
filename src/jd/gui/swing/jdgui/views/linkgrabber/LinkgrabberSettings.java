package jd.gui.swing.jdgui.views.linkgrabber;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;

public interface LinkgrabberSettings extends ConfigInterface {
    boolean isAddNewLinksOnTop();

    void setAddNewLinksOnTop(boolean selected);

    @DefaultBooleanValue(true)
    boolean isAutoDownloadStartAfterAddingEnabled();

    void setAutoDownloadStartAfterAddingEnabled(boolean selected);

    @DefaultBooleanValue(true)
    boolean isAutoaddLinksAfterLinkcheck();

    void setAutoaddLinksAfterLinkcheck(boolean selected);

    @DefaultObjectValue("[]")
    ArrayList<String[]> getDownloadFolderHistory();

    void setDownloadFolderHistory(ArrayList<String[]> history);

}
