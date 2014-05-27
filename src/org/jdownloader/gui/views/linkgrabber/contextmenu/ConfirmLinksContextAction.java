package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.swing.MigPanel;
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
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
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
import org.jdownloader.settings.staticreferences.CFG_GUI;

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

    private boolean forceDownloads = false;

    @Customizer(name = "Force Downloads")
    public boolean isForceDownloads() {
        return forceDownloads;
    }

    public void setForceDownloads(boolean forceDownloads) {
        this.forceDownloads = forceDownloads;
    }

    private boolean assignPriorityEnabled = false;

    @Customizer(name = "Enabled Prioritychange")
    public boolean isAssignPriorityEnabled() {
        return assignPriorityEnabled;
    }

    public void setAssignPriorityEnabled(boolean assignPriorityEnabled) {
        this.assignPriorityEnabled = assignPriorityEnabled;
    }

    private Priority piority = Priority.HIGHEST;

    @Customizer(name = "Download Priority:")
    public Priority getPiority() {
        return piority;
    }

    public void setPiority(Priority piority) {
        this.piority = piority;
    }

    public static final String AUTO_START       = "autoStart";

    /**
     * 
     */
    private static final long  serialVersionUID = -3937346180905569896L;

    public static void confirmSelection(final SelectionInfo<CrawledPackage, CrawledLink> selection, final boolean autoStart, final boolean clearLinkgrabber, final boolean doTabSwitch, final Priority newPriority, final BooleanStatus forcedStart) {
        Thread thread = new Thread() {

            public void run() {
                HashSet<CrawledLink> toDelete = new HashSet<CrawledLink>();
                HashSet<CrawledLink> toKeepInLinkgrabber = new HashSet<CrawledLink>();
                try {
                    // this validation step also copies the passwords from the CRawledlinks in the archive settings

                    switch (CFG_GUI.CFG.getConfirmIncompleteArchiveAction()) {
                    case ASK:
                        loop: for (Archive a : ArchiveValidator.validate(selection)) {
                            final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(a);
                            if (!da.isComplete()) {

                                ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ConfirmAction_run_incomplete_archive_title_(a.getName()), _GUI._.ConfirmAction_run_incomplete_archive_msg(), NewTheme.I().getIcon("stop", 32), _GUI._.ConfirmAction_run_incomplete_archive_continue(), null) {
                                    public String getDontShowAgainKey() {
                                        return null;

                                    };

                                    protected MigPanel createBottomPanel() {
                                        // TODO Auto-generated method stub
                                        return new MigPanel("ins 0", "[]20[grow,fill][]", "[]");
                                    }

                                    @Override
                                    protected String getDontShowAgainLabelText() {
                                        return _GUI._.ConfirmLinksContextAction_getDontShowAgainLabelText_object_();
                                    }

                                    @Override
                                    protected DefaultButtonPanel createBottomButtonPanel() {

                                        DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[][][]", "0[]0");
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

                                        }), "");
                                        return ret;
                                    }

                                };

                                Dialog.getInstance().showDialog(d);
                                if (d.isDontShowAgainSelected()) {
                                    break loop;
                                }

                            }

                        }
                        break;
                    case DELETE:
                        loop: for (Archive a : ArchiveValidator.validate(selection)) {
                            final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(a);
                            if (!da.isComplete()) {
                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        toDelete.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                            }
                        }
                        break;
                    case KEEP_IN_LINKGRABBER:
                        loop: for (Archive a : ArchiveValidator.validate(selection)) {
                            final DummyArchive da = ExtractionExtension.getInstance().createDummyArchive(a);
                            if (!da.isComplete()) {
                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        toKeepInLinkgrabber.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                            }
                        }
                        break;
                    case MOVE_TO_DOWNLOADLIST:
                        // do nothing
                        break;
                    }

                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    Log.exception(e);
                }
                ArrayList<CrawledLink> toMove = new ArrayList<CrawledLink>();
                for (CrawledLink cl : selection.getChildren()) {
                    if (toDelete.contains(cl)) continue;
                    if (toKeepInLinkgrabber.contains(cl)) continue;
                    if (forcedStart != null) {
                        switch (forcedStart) {
                        case FALSE:
                            cl.setForcedAutoStartEnabled(false);
                            break;
                        case TRUE:
                            cl.setForcedAutoStartEnabled(true);
                            break;
                        }
                    }
                    cl.setAutoStartEnabled(autoStart);
                    if (newPriority != null) {
                        cl.setPriority(newPriority);
                    }
                    toMove.add(cl);
                }
                if (toDelete.size() > 0) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    LinkCollector.getInstance().removeChildren(new ArrayList<CrawledLink>(toDelete));
                }
                if (toMove.size() == 0) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    return;
                }
                LinkCollector.getInstance().moveLinksToDownloadList(new SelectionInfo<CrawledPackage, CrawledLink>(null, toMove, false));

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

            confirmSelection(getSelection(), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), isAssignPriorityEnabled() ? getPiority() : null, isForceDownloads() ? BooleanStatus.TRUE : BooleanStatus.FALSE);

        } else {
            confirmSelection(LinkGrabberTable.getInstance().getSelectionInfo(false, true), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), isAssignPriorityEnabled() ? getPiority() : null, isForceDownloads() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
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
    public void initContextDefaults() {
        setAutoStart(AutoStartOptions.AUTO);
    }

    @Customizer(name = "Clear Linkgrabber after adding links")
    public boolean isClearListAfterConfirm() {
        return clearListAfterConfirm;
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
        onKeyModifier(-1);
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

    private boolean selectionOnly = true;

    @Customizer(name = "Add only selected Links")
    public boolean isSelectionOnly() {
        return selectionOnly;

    }

    public void setSelectionOnly(boolean selectionOnly) {
        this.selectionOnly = selectionOnly;
        updateLabelAndIcon();
    }

    protected void updateLabelAndIcon() {
        if (isForceDownloads()) {
            if (isSelectionOnly()) {
                setName(_GUI._.ConfirmAction_ConfirmAction_context_add_and_force());
            } else {
                setName(_GUI._.ConfirmAllContextmenuAction_context_add_and_force());
            }

            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("prio_3", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -4, 0, 6, 10)));
            setIconKey(null);
        } else if (doAutostart()) {
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
