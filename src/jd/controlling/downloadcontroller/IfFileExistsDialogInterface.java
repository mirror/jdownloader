package jd.controlling.downloadcontroller;

import org.appwork.utils.swing.dialog.UserIODefinition;
import org.jdownloader.settings.IfFileExistsAction;

public interface IfFileExistsDialogInterface extends UserIODefinition {

    public IfFileExistsAction getAction();

    public String getFilePath();

    public String getPackagename();

    public String getPackageID();

}
