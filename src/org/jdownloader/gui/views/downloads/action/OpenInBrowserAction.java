package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class OpenInBrowserAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 7911375550836173693L;

    public OpenInBrowserAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

        setIconKey("browse");
        setName(_GUI._.gui_table_contextmenu_browselink());
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;
        List<DownloadLink> links = getSelection().getChildren();
        if (links.size() > 50) return false;
        if (!CrossSystem.isOpenBrowserSupported()) return false;
        for (DownloadLink link : links) {
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) return true;
            if (link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER && link.gotBrowserUrl()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        new Thread() {
            public void run() {
                if (isEnabled()) { // additional security measure. Someone may call
                    // actionPerformed in the code although the
                    // action should be disabled
                    if (getSelection().getChildren().size() < 5) {
                        for (DownloadLink link : getSelection().getChildren()) {
                            CrossSystem.openURLOrShowMessage(link.getBrowserUrl());
                        }
                        return;
                    }
                    ProgressDialog pg = new ProgressDialog(new ProgressGetter() {
                        private int total = -1;
                        private int current;

                        @Override
                        public void run() throws Exception {
                            total = getSelection().getChildren().size();
                            current = 0;
                            for (DownloadLink link : getSelection().getChildren()) {
                                CrossSystem.openURLOrShowMessage(link.getBrowserUrl());
                                current++;
                                Thread.sleep(500);

                            }
                        }

                        @Override
                        public String getString() {
                            return current + "/" + total;
                        }

                        @Override
                        public int getProgress() {
                            if (total == 0) return -1;
                            int ret = (current * 100) / total;

                            return ret;
                        }

                        @Override
                        public String getLabelString() {
                            return null;
                        }
                    }, 0, _GUI._.OpenInBrowserAction_actionPerformed_open_in_browser__multi(), _GUI._.OpenInBrowserAction_actionPerformed_open_in_browser__multi_msg(getSelection().getChildren().size()), NewTheme.I().getIcon(IconKey.ICON_BROWSE, 32), null, null);

                    try {
                        Dialog.getInstance().showDialog(pg);
                    } catch (DialogClosedException e) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }
}