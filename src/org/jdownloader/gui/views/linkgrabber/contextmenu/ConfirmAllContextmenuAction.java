package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.ValidateArchiveAction;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;

public class ConfirmAllContextmenuAction extends AbstractSelectionContextAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -3937346180905569896L;

    public static enum AutoStartOptions {
        @EnumLabel("Autostart: Never start Downloads")
        DISABLED,
        @EnumLabel("Autostart: Always start Downloads")
        ENABLED,
        @EnumLabel("Autostart: Automode (Quicksettings)")
        AUTO

    }

    private AutoStartOptions autoStart = AutoStartOptions.AUTO;

    public AutoStartOptions getAutoStart() {
        return autoStart;
    }

    @Customizer(name = "Autostart Downloads afterwards")
    public ConfirmAllContextmenuAction setAutoStart(AutoStartOptions autoStart) {
        if (autoStart == null) autoStart = AutoStartOptions.AUTO;
        this.autoStart = autoStart;

        updateLabelAndIcon();
        return this;
    }

    private void updateLabelAndIcon() {
        if (doAutostart()) {
            setName(_GUI._.ConfirmAllContextmenuAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -2, 0, 8, 10)));
            setIconKey(null);
        } else {
            setName(_GUI._.ConfirmAllContextmenuAction_context_add());
            setIconKey(IconKey.ICON_GO_NEXT);
        }
    }

    protected boolean doAutostart() {
        return autoStart == AutoStartOptions.ENABLED || (autoStart == AutoStartOptions.AUTO && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getValue());
    }

    private boolean clearListAfterConfirm = false;

    @Customizer(name = "Clear Linkgrabber after adding links")
    public boolean isClearListAfterConfirm() {
        return clearListAfterConfirm;
    }

    @Customizer(name = "Clear Linkgrabber after adding links")
    public void setClearListAfterConfirm(boolean clearListAfterConfirm) {
        this.clearListAfterConfirm = clearListAfterConfirm;
    }

    public void setSelection(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        SelectionInfo<CrawledPackage, CrawledLink> sel = new SelectionInfo<CrawledPackage, CrawledLink>(null, LinkGrabberTableModel.getInstance().getAllChildrenNodes(), null, null, null, LinkGrabberTableModel.getInstance().getTable());
        super.setSelection(sel);
        if (!isItemVisibleForEmptySelection() && !(selection == null || selection.isEmpty())) {
            setVisible(false);
            setEnabled(false);
        } else if (!isItemVisibleForSelections() && !(selection == null || selection.isEmpty())) {
            setVisible(false);
            setEnabled(false);
        } else {
            setVisible(true);
            setEnabled(true);
        }
        updateLabelAndIcon();

    }

    public ConfirmAllContextmenuAction(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        super(null);

        setAutoStart(AutoStartOptions.AUTO);
        setItemVisibleForSelections(false);
        setItemVisibleForEmptySelection(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        Thread thread = new Thread() {

            public void run() {
                try {
                    // this validation step also copies the passwords from the CRawledlinks in the archive settings
                    ValidateArchiveAction<CrawledPackage, CrawledLink> va = new ValidateArchiveAction<CrawledPackage, CrawledLink>((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension(), getSelection());
                    for (Archive a : va.getArchives()) {
                        final DummyArchive da = va.getExtractionExtension().createDummyArchive(a);
                        if (!da.isComplete()) {

                            ConfirmDialog d = new ConfirmDialog(0, _GUI._.ConfirmAction_run_incomplete_archive_title_(a.getName()), _GUI._.ConfirmAction_run_incomplete_archive_msg(), NewTheme.I().getIcon("stop", 32), _GUI._.ConfirmAction_run_incomplete_archive_continue(), null) {
                                protected DefaultButtonPanel createBottomButtonPanel() {

                                    DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[]", "0[]0");
                                    ret.add(new JButton(new AppAction() {
                                        {
                                            setName(_GUI._.ConfirmAction_run_incomplete_archive_details());
                                        }

                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                Dialog.getInstance().showDialog(new DummyArchiveDialog(da));
                                            } catch (DialogClosedException e1) {
                                                e1.printStackTrace();
                                            } catch (DialogCanceledException e1) {
                                                e1.printStackTrace();
                                            }
                                        }

                                    }), "gapleft 32");
                                    return ret;
                                }

                            };

                            Dialog.getInstance().showDialog(d);
                        }

                    }

                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    Log.exception(e);
                }

                LinkCollector.getInstance().moveLinksToDownloadList(getSelection());

                if (doAutostart()) {
                    DownloadWatchDog.getInstance().startDownloads();
                }
                if (JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled()) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);
                        }
                    };
                }

                if (isClearListAfterConfirm()) {
                    LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                        @Override
                        protected Void run() throws RuntimeException {
                            LinkCollector.getInstance().clear();
                            return null;
                        }
                    });
                }
            }

        };
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName(getClass().getName());
        thread.start();

    }

    @Override
    public boolean isEnabled() {
        return hasSelection();
    }
}
