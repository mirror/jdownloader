package org.jdownloader.gui.views.linkgrabber.actions;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface ResetLinkGrabberDialogOptions extends ConfigInterface {
    @DefaultBooleanValue(true)
    boolean isClearSearchFilter();

    @DefaultBooleanValue(true)
    boolean isInterruptCrawler();

    @DefaultBooleanValue(true)
    boolean isRemoveLinks();

    @DefaultBooleanValue(true)
    boolean isResetSorter();

    void setClearSearchFilter(boolean b);

    void setInterruptCrawler(boolean b);

    void setRemoveLinks(boolean b);

    void setResetSorter(boolean b);

}
