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
import jd.gui.swing.jdgui.interfaces.View;

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
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;

public class ConfirmLinksContextAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements GUIListener, ActionContext {

    public static final String SELECTION_ONLY = "selectionOnly";

    public static enum AutoStartOptions {
        @EnumLabel("Autostart: Automode (Quicksettings)")
        AUTO,
        @EnumLabel("Autostart: Never start Downloads")
        DISABLED,
        @EnumLabel("Autostart: Always start Downloads")
        ENABLED

    }

    private boolean ctrlToggle = true;

    @Customizer(name = "CTRL Toggle Enabled")
    public boolean isCtrlToggle() {
        return ctrlToggle;
    }

    public void setCtrlToggle(boolean ctrlToggle) {
        this.ctrlToggle = ctrlToggle;
    }

    public static final String AUTO_START       = "autoStart";

    /**
     * 
     */
    private static final long  serialVersionUID = -3937346180905569896L;

    public static void confirmSelection(final SelectionInfo<CrawledPackage, CrawledLink> selection, final boolean autoStart, final boolean clearLinkgrabber, final boolean doTabSwitch) {
        Thread thread = new Thread() {

            public void run() {
                try {
                    // this validation step also copies the passwords from the CRawledlinks in the archive settings

                    for (Archive a : ArchiveValidator.validate(selection)) {
                        final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(a);
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
                if (autoStart) {
                    DownloadWatchDog.getInstance().startDownloads();
                }
                if (doTabSwitch) switchToDownloadTab();

                if (clearLinkgrabber) {
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
        thread.setName(ConfirmLinksContextAction.class.getName());
        thread.start();
    }

    protected static void switchToDownloadTab() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);
            }
        };

    }

    private AutoStartOptions autoStart             = AutoStartOptions.AUTO;

    private boolean          clearListAfterConfirm = false;

    private boolean          metaCtrl              = false;

    private boolean          selectionOnly         = true;

    public ConfirmLinksContextAction() {
        super(false, true);
        GUIEventSender.getInstance().addListener(this, true);
        metaCtrl = KeyObserver.getInstance().isMetaDown(true) || KeyObserver.getInstance().isControlDown(true);

    }

    public ConfirmLinksContextAction(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        this();
        selection = selectionInfo;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (isSelectionOnly()) {
            confirmSelection(getSelection(), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled());

        } else {
            confirmSelection(LinkGrabberTable.getInstance().getSelectionInfo(false, true), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled());
        }
    }

    protected boolean doAutostart() {
        boolean ret = autoStart == AutoStartOptions.ENABLED || (autoStart == AutoStartOptions.AUTO && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getValue());
        if (metaCtrl && isCtrlToggle()) {
            ret = !ret;
        }
        return ret;
    }

    public AutoStartOptions getAutoStart() {
        return autoStart;
    }

    @Override
    protected void initContextDefaults() {
        setAutoStart(AutoStartOptions.AUTO);
    }

    @Customizer(name = "Clear Linkgrabber after adding links")
    public boolean isClearListAfterConfirm() {
        return clearListAfterConfirm;
    }

    @Customizer(name = "Add only selected Links")
    public boolean isSelectionOnly() {
        return selectionOnly;

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
    }

    @Override
    public void onKeyModifier(int parameter) {
        if (KeyObserver.getInstance().isControlDown(false) || KeyObserver.getInstance().isMetaDown(false)) {
            metaCtrl = true;
        } else {
            metaCtrl = false;
        }

        updateLabelAndIcon();
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (!isSelectionOnly()) {
            selection = LinkGrabberTable.getInstance().getSelectionInfo(false, true);
        }
        updateLabelAndIcon();
    }

    @Customizer(name = "Autostart Downloads afterwards")
    public ConfirmLinksContextAction setAutoStart(AutoStartOptions autoStart) {
        if (autoStart == null) autoStart = AutoStartOptions.AUTO;
        this.autoStart = autoStart;
        updateLabelAndIcon();
        return this;
    }

    @Customizer(name = "Clear Linkgrabber after adding links")
    public void setClearListAfterConfirm(boolean clearListAfterConfirm) {
        this.clearListAfterConfirm = clearListAfterConfirm;
    }

    public void setSelectionOnly(boolean selectionOnly) {
        this.selectionOnly = selectionOnly;
        updateLabelAndIcon();
    }

    protected void updateLabelAndIcon() {

        if (doAutostart()) {
            if (isSelectionOnly()) {
                setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_start());
            } else {
                setName(_GUI._.ConfirmAllContextmenuAction_context_add_and_start());
            }

            Image add = NewTheme.I().getImage("media-playback-start", 16);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 6, 6)));
            setIconKey(null);
        } else {
            if (isSelectionOnly()) {
                setName(_GUI._.ConfirmAction_ConfirmAction_context_add());
            } else {
                setName(_GUI._.ConfirmAllContextmenuAction_context_add());
            }
            setSmallIcon(NewTheme.I().getIcon(IconKey.ICON_GO_NEXT, 20));
        }

    }

}
