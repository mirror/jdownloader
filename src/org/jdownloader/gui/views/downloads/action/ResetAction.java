package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;

public class ResetAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {
    
    private static final long serialVersionUID = -5583373118359478729L;
    
    public ResetAction() {
        
        setIconKey("undo");
        setName(_GUI._.gui_table_contextmenu_reset());
    }
    
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                DownloadLinkAggregator agg = new DownloadLinkAggregator();
                agg.setLocalFileUsageEnabled(true);
                agg.update(getSelection().getChildren());
                final String question = _GUI._.gui_downloadlist_reset2(agg.getTotalCount(), SizeFormatter.formatBytes(agg.getBytesLoaded()), agg.getLocalFileCount());
                new EDTRunner() {
                    
                    @Override
                    protected void runInEDT() {
                        ConfirmDialog confirmDialog = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.jd_gui_userio_defaulttitle_confirm(), question, UserIO.getDefaultIcon(question), null, null) {
                            @Override
                            public String getDontShowAgainKey() {
                                return "org.jdownloader.gui.views.downloads.action.ResetAction";
                            }
                        };
                        try {
                            Dialog.getInstance().showDialog(confirmDialog);
                            DownloadWatchDog.getInstance().reset(getSelection().getChildren());
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                    }
                };
                
                return null;
            };
        });
    }
}