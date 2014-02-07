package org.jdownloader.gui.dialog;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.Out;

public interface AskToUsePremiumDialogInterface extends ConfirmDialogInterface {
    @Out
    public String getPremiumUrl();

    @Out
    public String getDomain();
}
