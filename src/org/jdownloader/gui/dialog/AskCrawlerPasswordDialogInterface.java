package org.jdownloader.gui.dialog;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.Out;

public interface AskCrawlerPasswordDialogInterface extends InputDialogInterface {
    @Out
    public String getPluginHost();

    @Out
    public String getUrl();

}
