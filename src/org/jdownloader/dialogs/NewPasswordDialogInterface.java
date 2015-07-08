package org.jdownloader.dialogs;

import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;

public interface NewPasswordDialogInterface extends OKCancelCloseUserIODefinition {

    @Out
    public String getMessage();

    // input
    @In
    public String getPasswordVerification();

    @In
    public String getPassword();

}
