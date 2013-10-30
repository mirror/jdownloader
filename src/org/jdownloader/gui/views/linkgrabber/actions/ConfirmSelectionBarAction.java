package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Image;
import java.awt.Toolkit;
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
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
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

public class ConfirmSelectionBarAction extends AppAction implements CachableInterface {

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

    public static final String AUTO_START = "autoStart";

    @Customizer(name = "Autostart Downloads afterwards")
    public ConfirmSelectionBarAction setAutoStart(AutoStartOptions autoStart) {
        if (autoStart == null) autoStart = AutoStartOptions.AUTO;
        this.autoStart = autoStart;

        updateLabelAndIcon();
        return this;
    }

    private void updateLabelAndIcon() {
        if (doAutostart()) {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -2, 0, 8, 10)));
            setIconKey(null);
        } else {
            setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            setIconKey(IconKey.ICON_GO_NEXT);
        }
    }

    protected boolean doAutostart() {
        return autoStart == AutoStartOptions.ENABLED || (autoStart == AutoStartOptions.AUTO && org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue());
    }

    protected void switchToDownloadTab() {
        if (JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled()) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);
                }
            };
        }
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

    public ConfirmSelectionBarAction() {
        setAutoStart(AutoStartOptions.AUTO);

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTableModel.getInstance().createSelectionInfo();
        if (selection == null || selection.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Thread thread = new Thread() {

            public void run() {

                try {

                    // this validation step also copies the passwords from the CRawledlinks in the archive settings
                    ValidateArchiveAction<CrawledPackage, CrawledLink> va = new ValidateArchiveAction<CrawledPackage, CrawledLink>((ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension(), selection);
                    for (Archive a : va.getArchives()) {
                        final DummyArchive da = va.createDummyArchive(a);
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

                LinkCollector.getInstance().moveLinksToDownloadList(selection);
                if (doAutostart()) {
                    DownloadWatchDog.getInstance().startDownloads();
                }
                switchToDownloadTab();

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
    public void setData(String data) {
    }
}
