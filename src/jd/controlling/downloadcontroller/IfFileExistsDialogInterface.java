package jd.controlling.downloadcontroller;

import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;
import org.jdownloader.settings.IfFileExistsAction;

public interface IfFileExistsDialogInterface extends OKCancelCloseUserIODefinition {

    public IfFileExistsAction getAction();

    public String getFilePath();

    public String getPackagename();

    public String getPackageID();

}
