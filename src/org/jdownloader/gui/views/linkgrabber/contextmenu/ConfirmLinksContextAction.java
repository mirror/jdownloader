package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.MoveLinksMode;
import jd.controlling.linkcollector.LinkCollector.MoveLinksSettings;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.View;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.swing.MigPanel;
import org.appwork.uio.ComboBoxDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
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
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator.ArchiveValidation;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.ConfirmIncompleteArchiveAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class ConfirmLinksContextAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements GUIListener, ActionContext {
    public static final String SELECTION_ONLY = "selectionOnly";

    public static enum OnOfflineLinksAction implements LabelInterface {
        INCLUDE_OFFLINE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleOfflineLinksOptions_INCLUDE_OFFLINE();
            }
        },
        EXCLUDE_OFFLINE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleOfflineLinksOptions_EXCLUDE_OFFLINE();
            }
        },
        EXCLUDE_OFFLINE_AND_REMOVE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleOfflineLinksOptions_EXCLUDE_OFFLINE_AND_REMOVE();
            }
        },
        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleOfflineLinksOptions_ASK();
            }
        },
        GLOBAL {
            @Override
            public String getLabel() {
                OnOfflineLinksAction dflt = CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction();
                if (dflt == this || dflt == null) {
                    dflt = OnOfflineLinksAction.ASK;
                }
                return _JDT.T.ConfirmLinksContextAction_HandleOfflineLinksOptions_GLOBAL(dflt.getLabel());
            }
        };
    }

    public static enum OnDupesLinksAction implements LabelInterface {
        INCLUDE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleDupesLinksOptions_INCLUDE();
            }
        },
        EXCLUDE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleDupesLinksOptions_EXCLUDE();
            }
        },
        EXCLUDE_AND_REMOVE {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleDupesLinksOptions_EXCLUDE_AND_REMOVE();
            }
        },
        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.ConfirmLinksContextAction_HandleDupesLinksOptions_ASK();
            }
        },
        GLOBAL {
            @Override
            public String getLabel() {
                OnDupesLinksAction dflt = CFG_LINKGRABBER.CFG.getDefaultOnAddedDupesLinksAction();
                if (dflt == this || dflt == null) {
                    dflt = OnDupesLinksAction.ASK;
                }
                return _JDT.T.ConfirmLinksContextAction_HandleDupesLinksOptions_GLOBAL(dflt.getLabel());
            }
        };
    }

    public static enum AutoStartOptions implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return _JDT.T.AutoStartOptions_AUTO();
            }
        },
        DISABLED {
            @Override
            public String getLabel() {
                return _JDT.T.AutoStartOptions_DISABLED();
            }
        },
        ENABLED {
            @Override
            public String getLabel() {
                return _JDT.T.AutoStartOptions_ENABLED();
            }
        };
    }

    private boolean ctrlToggle = true;

    public static String getTranslationForCtrlToggle() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForCtrlToggle();
    }

    @Customizer(link = "#getTranslationForCtrlToggle")
    public boolean isCtrlToggle() {
        return ctrlToggle;
    }

    public void setCtrlToggle(boolean ctrlToggle) {
        this.ctrlToggle = ctrlToggle;
    }

    private boolean forceDownloads = false;

    public static String getTranslationForForceDownloads() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForForceDownloads();
    }

    @Customizer(link = "#getTranslationForForceDownloads")
    public boolean isForceDownloads() {
        return forceDownloads;
    }

    public void setForceDownloads(boolean forceDownloads) {
        this.forceDownloads = forceDownloads;
    }

    private boolean assignPriorityEnabled = false;

    public static String getTranslationForAssignPriorityEnabled() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForAssignPriorityEnabled();
    }

    @Customizer(link = "#getTranslationForAssignPriorityEnabled")
    public boolean isAssignPriorityEnabled() {
        return assignPriorityEnabled;
    }

    public void setAssignPriorityEnabled(boolean assignPriorityEnabled) {
        this.assignPriorityEnabled = assignPriorityEnabled;
    }

    private Priority piority = Priority.DEFAULT;

    public static String getTranslationForPiority() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForPiority();
    }

    @Customizer(link = "#getTranslationForPiority")
    public Priority getPiority() {
        return piority;
    }

    public void setPiority(Priority piority) {
        this.piority = piority;
    }

    public static final String FORCE_START      = "forceDownloads";
    public static final String AUTO_START       = "autoStart";
    /**
     *
     */
    private static final long  serialVersionUID = -3937346180905569896L;

    public static void confirmSelection(final MoveLinksMode moveLinksMode, final SelectionInfo<CrawledPackage, CrawledLink> selection, final boolean autoStart, final boolean clearLinkgrabber, final boolean doTabSwitch, final Priority newPriority, final BooleanStatus forcedStart, final OnOfflineLinksAction handleOfflineLinks, final OnDupesLinksAction handleDupes) {
        final Thread thread = new Thread() {
            public void run() {
                OnOfflineLinksAction handleOfflineLoc = handleOfflineLinks;
                if (handleOfflineLoc == OnOfflineLinksAction.GLOBAL) {
                    handleOfflineLoc = OnOfflineLinksAction.ASK;
                }
                OnDupesLinksAction handleDupesLoc = handleDupes;
                if (handleDupesLoc == OnDupesLinksAction.GLOBAL) {
                    handleDupesLoc = OnDupesLinksAction.ASK;
                }
                HashSet<CrawledLink> toDelete = new HashSet<CrawledLink>();
                HashSet<CrawledLink> toKeepInLinkgrabber = new HashSet<CrawledLink>();
                try {
                    // this validation step also copies the passwords from the CRawledlinks in the archive settings
                    final ExtractionExtension extension = ExtractionExtension.getInstance();
                    if (extension != null) {
                        ConfirmIncompleteArchiveAction doAction = CFG_GUI.CFG.getConfirmIncompleteArchiveAction();
                        final ArchiveValidation result = ArchiveValidator.validate(selection, false);
                        for (Archive a : result.getArchives()) {
                            ConfirmIncompleteArchiveAction doActionForTheCurrentArchive = doAction;
                            final DummyArchive da = extension.createDummyArchive(a);
                            if (da.isComplete()) {
                                continue;
                            }
                            if (doAction == ConfirmIncompleteArchiveAction.ASK) {
                                ConfirmIncompleteArchiveAction[] options = new ConfirmIncompleteArchiveAction[] { ConfirmIncompleteArchiveAction.DELETE, ConfirmIncompleteArchiveAction.KEEP_IN_LINKGRABBER, ConfirmIncompleteArchiveAction.MOVE_TO_DOWNLOADLIST };
                                int def = 0;
                                ConfirmIncompleteArchiveAction s = CFG_LINKGRABBER.CFG.getHandleIncompleteArchiveOnConfirmLatestSelection();
                                for (int i = 0; i < options.length; i++) {
                                    if (s == options[i]) {
                                        def = i;
                                        break;
                                    }
                                }
                                ComboBoxDialog guiDialog = new ComboBoxDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ConfirmAction_run_incomplete_archive_title_(a.getName()), _GUI.T.ConfirmAction_run_incomplete_archive_msg(), options, def, new AbstractIcon(IconKey.ICON_STOP, 32), _GUI.T.lit_continue(), null, null) {
                                    public String getDontShowAgainKey() {
                                        return null;
                                    };

                                    protected MigPanel createBottomPanel() {
                                        // TODO Auto-generated method stub
                                        return new MigPanel("ins 0", "[]20[grow,fill][]", "[]");
                                    }

                                    @Override
                                    protected DefaultButtonPanel createBottomButtonPanel() {
                                        DefaultButtonPanel ret = new DefaultButtonPanel("ins 0", "[][][]", "0[]0");
                                        ret.add(new JButton(new AppAction() {
                                            {
                                                setName(_GUI.T.ConfirmAction_run_incomplete_archive_details());
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

                                    @Override
                                    protected String getDontShowAgainLabelText() {
                                        return _GUI.T.ConfirmLinksContextAction_getDontShowAgainLabelText_object_();
                                    }

                                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                                        // TODO Auto-generated method stub
                                        return new ListCellRenderer() {
                                            @Override
                                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                                if (value == null) {
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.AddActionAction_getListCellRendererComponent_no_action_(), index, isSelected, cellHasFocus);
                                                }
                                                switch (((ConfirmIncompleteArchiveAction) value)) {
                                                // case EXCLUDE_OFFLINE:
                                                // return orgRenderer.getListCellRendererComponent(list,
                                                // _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_OFFLINE(),
                                                // index, isSelected, cellHasFocus);
                                                //
                                                // case EXCLUDE_OFFLINE_AND_REMOVE:
                                                // return orgRenderer.getListCellRendererComponent(list,
                                                // _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_OFFLINE_AND_REMOVE(),
                                                // index, isSelected, cellHasFocus);
                                                //
                                                // case INCLUDE_OFFLINE:
                                                // return orgRenderer.getListCellRendererComponent(list,
                                                // _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_INCLUDE_OFFLINE(),
                                                // index, isSelected, cellHasFocus);
                                                }
                                                JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, ((ConfirmIncompleteArchiveAction) value).getLabel(), index, isSelected, cellHasFocus);
                                                return ret;
                                            }
                                        };
                                    }
                                };
                                ComboBoxDialogInterface response = UIOManager.I().show(ComboBoxDialogInterface.class, guiDialog);
                                response.throwCloseExceptions();
                                doActionForTheCurrentArchive = options[response.getSelectedIndex()];
                                CFG_LINKGRABBER.CFG.setHandleIncompleteArchiveOnConfirmLatestSelection(doActionForTheCurrentArchive);
                                if (response.isDontShowAgainSelected()) {
                                    doAction = doActionForTheCurrentArchive;
                                }
                            }
                            switch (doActionForTheCurrentArchive) {
                            case DELETE:
                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        toDelete.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                                break;
                            case KEEP_IN_LINKGRABBER:
                                for (ArchiveFile af : a.getArchiveFiles()) {
                                    if (af instanceof CrawledLinkArchiveFile) {
                                        toKeepInLinkgrabber.addAll(((CrawledLinkArchiveFile) af).getLinks());
                                    }
                                }
                                break;
                            case MOVE_TO_DOWNLOADLIST:
                                // do nothing
                                break;
                            }
                        }
                    }
                } catch (DialogNoAnswerException e) {
                    return;
                } catch (Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                ArrayList<CrawledLink> offline = new ArrayList<CrawledLink>();
                if (handleOfflineLoc != OnOfflineLinksAction.INCLUDE_OFFLINE) {
                    for (CrawledLink cl : selection.getChildren()) {
                        if (toKeepInLinkgrabber.contains(cl)) {
                            continue;
                        }
                        if (toDelete.contains(cl)) {
                            continue;
                        }
                        if (cl.getDownloadLink().isAvailabilityStatusChecked() && !cl.getDownloadLink().isAvailable()) {
                            offline.add(cl);
                            if (handleOfflineLoc == OnOfflineLinksAction.ASK) {
                                final OnOfflineLinksAction[] options = new OnOfflineLinksAction[] { OnOfflineLinksAction.INCLUDE_OFFLINE, OnOfflineLinksAction.EXCLUDE_OFFLINE, OnOfflineLinksAction.EXCLUDE_OFFLINE_AND_REMOVE };
                                final ComboBoxDialog combo = new ComboBoxDialog(0, _GUI.T.ConfirmLinksContextAction_run_offline_ask_title(), _GUI.T.ConfirmLinksContextAction_run_offline_ask_question(), options, 1, null, null, null, null) {
                                    protected javax.swing.JComboBox getComboBox(Object[] options2) {
                                        OnOfflineLinksAction s = CFG_LINKGRABBER.CFG.getHandleOfflineOnConfirmLatestSelection();
                                        JComboBox ret = super.getComboBox(options2);
                                        if (s != null) {
                                            ret.setSelectedItem(s);
                                        }
                                        return ret;
                                    };

                                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                                        // TODO Auto-generated method stub
                                        return new ListCellRenderer() {
                                            @Override
                                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                                if (value == null) {
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.AddActionAction_getListCellRendererComponent_no_action_(), index, isSelected, cellHasFocus);
                                                }
                                                switch (((OnOfflineLinksAction) value)) {
                                                case EXCLUDE_OFFLINE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_OFFLINE(), index, isSelected, cellHasFocus);
                                                case EXCLUDE_OFFLINE_AND_REMOVE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_OFFLINE_AND_REMOVE(), index, isSelected, cellHasFocus);
                                                case INCLUDE_OFFLINE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_INCLUDE_OFFLINE(), index, isSelected, cellHasFocus);
                                                }
                                                JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, ((OnOfflineLinksAction) value).getLabel(), index, isSelected, cellHasFocus);
                                                return ret;
                                            }
                                        };
                                    }
                                };
                                final ComboBoxDialogInterface result = UIOManager.I().show(ComboBoxDialogInterface.class, combo);
                                try {
                                    result.throwCloseExceptions();
                                } catch (Exception e) {
                                    return;
                                }
                                handleOfflineLoc = options[result.getSelectedIndex()];
                                CFG_LINKGRABBER.CFG.setHandleOfflineOnConfirmLatestSelection(handleOfflineLoc);
                            }
                            switch (handleOfflineLoc) {
                            case EXCLUDE_OFFLINE:
                                toKeepInLinkgrabber.add(cl);
                                break;
                            case EXCLUDE_OFFLINE_AND_REMOVE:
                                toDelete.add(cl);
                                break;
                            }
                        }
                    }
                }
                //
                ArrayList<CrawledLink> dupes = new ArrayList<CrawledLink>();
                if (handleDupesLoc != OnDupesLinksAction.INCLUDE) {
                    for (CrawledLink cl : selection.getChildren()) {
                        if (toKeepInLinkgrabber.contains(cl)) {
                            continue;
                        }
                        if (toDelete.contains(cl)) {
                            continue;
                        }
                        String id = cl.getLinkID();
                        if (DownloadController.getInstance().hasDownloadLinkByID(id)) {
                            dupes.add(cl);
                            if (handleDupesLoc == OnDupesLinksAction.ASK) {
                                final OnDupesLinksAction[] options = new OnDupesLinksAction[] { OnDupesLinksAction.INCLUDE, OnDupesLinksAction.EXCLUDE, OnDupesLinksAction.EXCLUDE_AND_REMOVE };
                                final ComboBoxDialog combo = new ComboBoxDialog(0, _GUI.T.ConfirmLinksContextAction_run_dupes_ask_title(), _GUI.T.ConfirmLinksContextAction_run_dupes_ask_question(), options, 0, null, null, null, null) {
                                    protected javax.swing.JComboBox getComboBox(Object[] options2) {
                                        OnDupesLinksAction s = CFG_LINKGRABBER.CFG.getHandleDupesOnConfirmLatestSelection();
                                        JComboBox ret = super.getComboBox(options2);
                                        if (s != null) {
                                            ret.setSelectedItem(s);
                                        }
                                        return ret;
                                    };

                                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                                        // TODO Auto-generated method stub
                                        return new ListCellRenderer() {
                                            @Override
                                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                                if (value == null) {
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.AddActionAction_getListCellRendererComponent_no_action_(), index, isSelected, cellHasFocus);
                                                }
                                                switch (((OnDupesLinksAction) value)) {
                                                case EXCLUDE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_DUPES(), index, isSelected, cellHasFocus);
                                                case EXCLUDE_AND_REMOVE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_EXCLUDE_DUPES_AND_REMOVE(), index, isSelected, cellHasFocus);
                                                case INCLUDE:
                                                    return orgRenderer.getListCellRendererComponent(list, _GUI.T.ConfirmLinksContextAction_getListCellRendererComponent_INCLUDE_DUPES(), index, isSelected, cellHasFocus);
                                                }
                                                JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, ((OnDupesLinksAction) value).getLabel(), index, isSelected, cellHasFocus);
                                                return ret;
                                            }
                                        };
                                    }
                                };
                                final ComboBoxDialogInterface result = UIOManager.I().show(ComboBoxDialogInterface.class, combo);
                                try {
                                    result.throwCloseExceptions();
                                } catch (Exception e) {
                                    return;
                                }
                                handleDupesLoc = options[result.getSelectedIndex()];
                                CFG_LINKGRABBER.CFG.setHandleDupesOnConfirmLatestSelection(handleDupesLoc);
                            }
                            switch (handleDupesLoc) {
                            case EXCLUDE:
                                toKeepInLinkgrabber.add(cl);
                                break;
                            case EXCLUDE_AND_REMOVE:
                                toDelete.add(cl);
                                break;
                            }
                        }
                    }
                }
                ArrayList<CrawledLink> toMove = new ArrayList<CrawledLink>();
                boolean createNewSelectionInfo = false;
                for (CrawledLink cl : selection.getChildren()) {
                    if (toDelete.contains(cl)) {
                        createNewSelectionInfo = true;
                        continue;
                    }
                    if (toKeepInLinkgrabber.contains(cl)) {
                        createNewSelectionInfo = true;
                        continue;
                    }
                    toMove.add(cl);
                }
                if (toDelete.size() > 0) {
                    LinkCollector.getInstance().removeChildren(new ArrayList<CrawledLink>(toDelete));
                }
                if (toMove.size() == 0) {
                    return;
                }
                if (createNewSelectionInfo) {
                    LinkCollector.getInstance().moveLinksToDownloadList(new MoveLinksSettings(moveLinksMode, autoStart, BooleanStatus.convert(forcedStart), newPriority), new SelectionInfo<CrawledPackage, CrawledLink>(null, toMove));
                } else {
                    LinkCollector.getInstance().moveLinksToDownloadList(new MoveLinksSettings(moveLinksMode, autoStart, BooleanStatus.convert(forcedStart), newPriority), selection);
                }
                if (doTabSwitch) {
                    switchToDownloadTab();
                }
                if (clearLinkgrabber) {
                    LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            if (!Application.isHeadless()) {
                                new EDTRunner() {
                                    @Override
                                    protected void runInEDT() {
                                        LinkgrabberSearchField.getInstance().setText("");
                                        LinkgrabberSearchField.getInstance().onChanged();
                                    }
                                };
                            }
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
        if (!Application.isHeadless()) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);
                }
            };
        }
    }

    private OnOfflineLinksAction handleOffline = OnOfflineLinksAction.GLOBAL;

    public static String getTranslationForHandleOffline() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForHandleOffline();
    }

    @Customizer(link = "#getTranslationForHandleOffline")
    public OnOfflineLinksAction getHandleOffline() {
        return handleOffline;
    }

    public ConfirmLinksContextAction setHandleOffline(OnOfflineLinksAction handleOffline) {
        if (handleOffline == null) {
            handleOffline = OnOfflineLinksAction.GLOBAL;
        }
        this.handleOffline = handleOffline;
        return this;
    }

    //
    private OnDupesLinksAction handleDupes = OnDupesLinksAction.GLOBAL;

    public static String getTranslationForHandleDupes() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForHandleDupes();
    }

    @Customizer(link = "#getTranslationForHandleDupes")
    public OnDupesLinksAction getHandleDupes() {
        return handleDupes;
    }

    public ConfirmLinksContextAction setHandleDupes(OnDupesLinksAction handleDupes) {
        if (handleDupes == null) {
            handleDupes = OnDupesLinksAction.GLOBAL;
        }
        this.handleDupes = handleDupes;
        return this;
    }

    //
    private AutoStartOptions autoStart             = AutoStartOptions.AUTO;
    private boolean          clearListAfterConfirm = false;
    private boolean          metaCtrl              = false;

    public ConfirmLinksContextAction() {
        super(false, true);
        GUIEventSender.getInstance().addListener(this, true);
        metaCtrl = KeyObserver.getInstance().isMetaDown(true) || KeyObserver.getInstance().isControlDown(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        OnOfflineLinksAction handleOffline = getHandleOffline();
        if (handleOffline == OnOfflineLinksAction.GLOBAL) {
            handleOffline = CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction();
        }
        OnDupesLinksAction handleDupes = getHandleDupes();
        if (handleDupes == OnDupesLinksAction.GLOBAL) {
            handleDupes = CFG_LINKGRABBER.CFG.getDefaultOnAddedDupesLinksAction();
        }
        if (isSelectionOnly()) {
            confirmSelection(MoveLinksMode.MANUAL, getSelection(), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), isAssignPriorityEnabled() ? getPiority() : null, isForceDownloads() ? BooleanStatus.TRUE : BooleanStatus.FALSE, handleOffline, handleDupes);
        } else {
            confirmSelection(MoveLinksMode.MANUAL, LinkGrabberTable.getInstance().getSelectionInfo(false, true), doAutostart(), isClearListAfterConfirm(), JsonConfig.create(LinkgrabberSettings.class).isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), isAssignPriorityEnabled() ? getPiority() : null, isForceDownloads() ? BooleanStatus.TRUE : BooleanStatus.FALSE, handleOffline, handleDupes);
        }
    }

    protected boolean doAutostart() {
        final boolean ret = autoStart == AutoStartOptions.ENABLED || (autoStart == AutoStartOptions.AUTO && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled());
        if (metaCtrl && isCtrlToggle()) {
            return !ret;
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

    public static String getTranslationForClearListAfterConfirm() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForClearListAfterConfirm();
    }

    @Customizer(link = "#getTranslationForClearListAfterConfirm")
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
    protected SelectionInfo<CrawledPackage, CrawledLink> getSelection() {
        if (!isSelectionOnly()) {
            return LinkGrabberTable.getInstance().getSelectionInfo(false, true);
        } else {
            return LinkGrabberTable.getInstance().getSelectionInfo(true, true);
        }
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        onKeyModifier(-1);
        updateLabelAndIcon();
    }

    public static String getTranslationForAutoStart() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForAutoStart();
    }

    @Customizer(link = "#getTranslationForAutoStart")
    public ConfirmLinksContextAction setAutoStart(AutoStartOptions autoStart) {
        if (autoStart == null) {
            autoStart = AutoStartOptions.AUTO;
        }
        this.autoStart = autoStart;
        updateLabelAndIcon();
        return this;
    }

    public void setClearListAfterConfirm(boolean clearListAfterConfirm) {
        this.clearListAfterConfirm = clearListAfterConfirm;
    }

    private boolean selectionOnly = true;

    public static String getTranslationForSelectionOnly() {
        return _JDT.T.ConfirmLinksContextAction_getTranslationForSelectionOnly();
    }

    @Customizer(link = "#getTranslationForSelectionOnly")
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
                setName(getTextForForcedSelectionOnly());
            } else {
                setName(getTextForForcedAll());
            }
            Image add = NewTheme.I().getImage("media-playback-start", 20);
            Image play = NewTheme.I().getImage("prio_3", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -4, 0, 6, 10)));
            setIconKey(null);
        } else if (doAutostart()) {
            if (isSelectionOnly()) {
                setName(getTextForAutoStartSelectionOnly());
            } else {
                setName(getTextForAutoStartAll());
            }
            Image add = NewTheme.I().getImage("media-playback-start", 16);
            Image play = NewTheme.I().getImage("add", 14);
            setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, 0, 0, 6, 6)));
            setIconKey(null);
        } else {
            // ContextMenuManager owner = _getOwner();
            if (isSelectionOnly()) {
                setName(getTextForNoAutoStartSelectionOnly());
            } else {
                setName(getTextForNoAutoStartAll());
            }
            setSmallIcon(NewTheme.I().getIcon(IconKey.ICON_GO_NEXT, 20));
        }
    }

    /**
     * @return
     */
    protected String getTextForForcedAll() {
        return _GUI.T.ConfirmAllContextmenuAction_context_add_and_force();
    }

    /**
     * @return
     */
    protected String getTextForForcedSelectionOnly() {
        return _GUI.T.ConfirmAction_ConfirmAction_context_add_and_force();
    }

    /**
     * @return
     */
    protected String getTextForNoAutoStartAll() {
        return _GUI.T.ConfirmAllContextmenuAction_context_add();
    }

    /**
     * @return
     */
    protected String getTextForNoAutoStartSelectionOnly() {
        return _GUI.T.ConfirmAction_ConfirmAction_context_add();
    }

    /**
     * @return
     */
    protected String getTextForAutoStartAll() {
        return _GUI.T.ConfirmAllContextmenuAction_context_add_and_start();
    }

    /**
     * @return
     */
    protected String getTextForAutoStartSelectionOnly() {
        return _GUI.T.ConfirmAction_ConfirmAction_context_add_and_start();
    }
}
