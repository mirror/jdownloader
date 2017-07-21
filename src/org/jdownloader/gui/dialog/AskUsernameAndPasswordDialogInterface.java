package org.jdownloader.gui.dialog;

import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;

public interface AskUsernameAndPasswordDialogInterface extends OKCancelCloseUserIODefinition {
    @Out
    public String getMessage();

    @Out
    public long getLinkID();

    @Out
    public String getLinkName();

    @Out
    public String getLinkHost();

    @Out
    public String getPackageName();

    // input
    @In
    public String getUsername();

    @In
    public String getPassword();

    @In
    public boolean isRememberSelected();
}