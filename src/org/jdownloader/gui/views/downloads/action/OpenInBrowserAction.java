package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.images.NewTheme;

public class OpenInBrowserAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> implements ActionContext {
    private static final long   serialVersionUID = 7911375550836173693L;
    private final static String NAME             = _GUI.T.gui_table_contextmenu_browselink();

    public OpenInBrowserAction() {
        setIconKey(IconKey.ICON_BROWSE);
        setName(NAME);
    }

    private int threshold = 50;

    public static String getTranslationMaxOpenThreshold() {
        return _GUI.T.gui_table_contextmenu_browselink_maxurls();
    }

    @Customizer(link = "#getTranslationMaxOpenThreshold")
    public int getMaxOpenThreshold() {
        return threshold;
    }

    public void setMaxOpenThreshold(int threshold) {
        this.threshold = Math.max(-1, threshold);
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        final int threshold = getMaxOpenThreshold();
        if (!CrossSystem.isOpenBrowserSupported() || threshold == 0) {
            setEnabled(false);
            return;
        }
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        if (hasSelection(selection)) {
            if (threshold < 0) {
                setEnabled(true);
            } else {
                final List<DownloadLink> links = selection.getChildren();
                if (links.size() < threshold) {
                    for (final DownloadLink link : links) {
                        if (link.getView().getDisplayUrl() != null) {
                            setEnabled(true);
                            return;
                        }
                    }
                }
            }
        }
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        if (hasSelection(selection)) {
            new Thread("OpenInBrowserAction") {
                public void run() {
                    final Set<String> urls = LinkTreeUtils.getURLs(selection, true);
                    if (urls.size() < 5) {
                        for (String url : urls) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                return;
                            }
                            CrossSystem.openURL(url);
                        }
                        return;
                    }
                    final ProgressDialog pg = new ProgressDialog(new ProgressGetter() {
                        private int total = -1;
                        private int current;

                        @Override
                        public void run() throws Exception {
                            total = urls.size();
                            current = 0;
                            for (String url : urls) {
                                CrossSystem.openURL(url);
                                current++;
                                Thread.sleep(1000);
                            }
                        }

                        @Override
                        public String getString() {
                            return current + "/" + total;
                        }

                        @Override
                        public int getProgress() {
                            if (total == 0) {
                                return -1;
                            }
                            final int ret = (current * 100) / total;
                            return ret;
                        }

                        @Override
                        public String getLabelString() {
                            return null;
                        }
                    }, 0, _GUI.T.OpenInBrowserAction_actionPerformed_open_in_browser__multi(), _GUI.T.OpenInBrowserAction_actionPerformed_open_in_browser__multi_msg(urls.size()), NewTheme.I().getIcon(IconKey.ICON_BROWSE, 32), null, null);
                    try {
                        Dialog.getInstance().showDialog(pg);
                    } catch (DialogClosedException e) {
                        e.printStackTrace();
                    } catch (DialogCanceledException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}