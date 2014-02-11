package org.jdownloader.statistics;

import org.appwork.uio.Out;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition;

public interface UploadSessionLogDialogInterface extends OKCancelCloseUserIODefinition {
    @Out
    public String getLinkName();

    @Out
    public long getLinkID();

    @Out
    public String getPackageName();

    @Out
    public String getHost();
}
