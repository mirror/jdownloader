package jd.controlling.downloadcontroller;

import org.appwork.uio.In;
import org.appwork.uio.Out;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;
import org.jdownloader.settings.IfFilenameTooLongAction;

public interface IfFilenameTooLongDialogInterface extends OKCancelCloseUserIODefinition {
    @In
    public IfFilenameTooLongAction getAction();

    @Out
    public String getFilePath();

    @Out
    public String getPackagename();

    @Out
    public String getPackageID();

    @Out
    public String getHost();
}
