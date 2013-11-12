package org.jdownloader.gui.views.downloads.action;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DeleteTo;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.IconDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;
import org.jdownloader.utils.JDFileUtils;

public class DownloadTabActionUtils {

    public static void deleteLinksRequest(final SelectionInfo<FilePackage, DownloadLink> si, final String msg, final boolean deleteFilesFormDiskEnabled, final boolean byPassDialog) {
        final DownloadLinkAggregator agg = new DownloadLinkAggregator();
        agg.setMirrorHandlingEnabled(false);
        agg.setLocalFileUsageEnabled(true);
        agg.update(si.getChildren());

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (agg.getTotalCount() == 0) {
                    new IconDialog(0, _GUI._.lit_ups_something_is_wrong(), _GUI._.DownloadController_deleteLinksRequest_nolinks(), NewTheme.I().getIcon("robot_sos", 256), null).show();
                    return;
                }
                ConfirmDeleteLinksDialog dialog = new ConfirmDeleteLinksDialog(msg + "\r\n" + _GUI._.DeleteSelectionAction_actionPerformed_affected2(agg.getTotalCount(), SizeFormatter.formatBytes(agg.getBytesLoaded()), DownloadController.getInstance().getChildrenCount() - agg.getTotalCount(), agg.getLocalFileCount()), agg.getBytesLoaded());
                dialog.setRecycleSupported(JDFileUtils.isTrashSupported());

                dialog.setDeleteFilesFromDiskEnabled(deleteFilesFormDiskEnabled);

                ConfirmDeleteLinksDialogInterface ret = null;
                if (!byPassDialog) {
                    ret = dialog.show();
                } else {
                    ret = new ConfirmDeleteLinksDialogInterface() {

                        @Override
                        public String getMessage() {
                            return null;
                        }

                        @Override
                        public String getTitle() {
                            return null;
                        }

                        @Override
                        public CloseReason getCloseReason() {
                            return CloseReason.OK;
                        }

                        @Override
                        public boolean isDeleteFilesFromDiskEnabled() {
                            // Fehlerhafte zurücksetzen
                            return KeyObserver.getInstance().isShiftDown(true);
                        }

                        @Override
                        public boolean isDeleteFilesToRecycle() {
                            return false;
                        }

                        @Override
                        public boolean isRecycleSupported() {
                            return false;
                        }

                        @Override
                        public void throwCloseExceptions() throws DialogClosedException, DialogCanceledException {
                        }

                        @Override
                        public boolean isDontShowAgainSelected() {
                            return false;
                        }

                        @Override
                        public int getFlags() {
                            return 0;
                        }

                    };
                }
                if (ret.getCloseReason() == CloseReason.OK) {
                    if (!byPassDialog) {
                        JDGui.help(_GUI._.DownloadController_deleteLinksRequest_object_help(), _GUI._.DownloadController_deleteLinksRequest_object_msg(), NewTheme.I().getIcon("robot_info", -1));
                    }
                    DownloadController.getInstance().removeChildren(si.getChildren());
                    if (ret.isDeleteFilesFromDiskEnabled()) {
                        DownloadWatchDog.getInstance().delete(si.getChildren(), ret.isDeleteFilesToRecycle() ? DeleteTo.RECYCLE : DeleteTo.NULL);
                    }
                }
            }
        };
    }

}
