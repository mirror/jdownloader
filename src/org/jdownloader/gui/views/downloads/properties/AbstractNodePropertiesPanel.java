package org.jdownloader.gui.views.downloads.properties;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit.CopyAction;
import javax.swing.text.TextAction;

import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;
import jd.plugins.download.HashInfo;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryEntry;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class AbstractNodePropertiesPanel<E extends AbstractNodeProperties> extends MigPanel implements ActionListener, GenericConfigEventListener<Boolean> {

    protected final PseudoCombo<BooleanStatus>          autoExtract;
    protected final ExtTextField                        checksum;
    protected final ExtTextField                        comment;
    protected final LinkgrabberSettings                 config;

    protected final PathChooser                         destination;
    protected final ExtTextField                        downloadFrom;
    protected final ExtTextField                        downloadpassword;
    protected final ExtTextField                        filename;
    protected final SearchComboBox<PackageHistoryEntry> packagename;
    protected final ExtTextField                        password;
    protected final PseudoCombo<Priority>               priority;
    private final DelayedRunnable                       saveDelayer;
    protected final AtomicInteger                       savingLock             = new AtomicInteger(0);
    protected final AtomicInteger                       settingLock            = new AtomicInteger(0);
    private final DelayedRunnable                       updateDelayer;
    private static final ScheduledExecutorService       SERVICE                = DelayedRunnable.getNewScheduledExecutorService();

    protected volatile E                                abstractNodeProperties = null;

    protected E getAbstractNodeProperties() {
        return abstractNodeProperties;
    }

    protected void setAbstractNodeProperties(final E abstractNodeProperties) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                save();
                AbstractNodePropertiesPanel.this.abstractNodeProperties = abstractNodeProperties;
                if (abstractNodeProperties != null) {
                    loadInEDT(true, abstractNodeProperties);
                }
            }
        };

    }

    public AbstractNodePropertiesPanel() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent e) {
                onShowing();
            }

            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                onHidden();
            }
        });
        LAFOptions.getInstance().applyPanelBackground(this);
        config = JsonConfig.create(LinkgrabberSettings.class);

        saveDelayer = new DelayedRunnable(SERVICE, 500l, 2000l) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        save();
                    }
                };
            }

            @Override
            public String getID() {
                return "PropertiesSaver";
            }

        };
        updateDelayer = new DelayedRunnable(SERVICE, 100l, 2000l) {

            @Override
            public void delayedrun() {
                refresh(false);
            }

            @Override
            public String getID() {
                return "updateDelayer";
            }

        };
        destination = new PathChooser("ADDLinks", true) {
            public File doFileChooser() {
                try {
                    return DownloadFolderChooserDialog.open(getFile(), true, getDialogTitle());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
                return null;

            }

            protected String getHelpText() {
                return _GUI.T.AddLinksDialog_layoutDialogContent_help_destination();
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new VariableAction(txt, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
                menu.add(new VariableAction(txt, _GUI.T.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
                return menu;
            }

            protected void onChanged(ExtTextField txt2) {

                // delayedSave();
            }

            public void setFile(final File file) {
                super.setFile(file);
                save();
            }
        };
        setListeners(destination.getTxt());
        packagename = new SearchComboBox<PackageHistoryEntry>() {
            @Override
            protected boolean isSearchCaseSensitive() {
                return true;
            }

            @Override
            protected Icon getIconForValue(PackageHistoryEntry value) {
                return null;
            }

            @Override
            protected String getTextForValue(PackageHistoryEntry value) {
                return value == null ? null : value.getName();
            }

            @Override
            public void onChanged() {
                // delayedSave();
            }

            /**
             * @param found
             */
            protected void sortFound(String search, final List<PackageHistoryEntry> found) {
                Collections.sort(found, new Comparator<PackageHistoryEntry>() {

                    @Override
                    public int compare(PackageHistoryEntry o1, PackageHistoryEntry o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

            }
        };

        packagename.setBadColor(null);
        packagename.setUnkownTextInputAllowed(true);
        packagename.setHelpText(_GUI.T.AddLinksDialog_layoutDialogContent_packagename_help());
        packagename.setSelectedItem(null);
        setListeners(packagename.getTextField());
        comment = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        comment.setHelpText(_GUI.T.AddLinksDialog_layoutDialogContent_comment_help());
        comment.setBorder(BorderFactory.createCompoundBorder(comment.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));
        setListeners(comment);
        filename = createFileNameTextField();
        setListeners(filename);

        filename.setBorder(BorderFactory.createCompoundBorder(filename.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));
        //
        downloadpassword = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        downloadpassword.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                downloadpassword.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        downloadpassword.setHelpText(_GUI.T.AddLinksDialog_layoutDialogContent_password_tt());
        setListeners(downloadpassword);

        downloadpassword.setBorder(BorderFactory.createCompoundBorder(downloadpassword.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        downloadFrom = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                InputMap map = getInputMap(condition);
                ActionMap am = getActionMap();

                if (map != null && am != null && isEnabled()) {
                    Object binding = map.get(ks);
                    Action action = (binding == null) ? null : am.get(binding);

                    if (action != null) {

                        if (action instanceof CopyAction) {
                            return super.processKeyBinding(ks, e, condition, pressed);
                        }
                        if ("select-all".equals(binding)) {
                            return super.processKeyBinding(ks, e, condition, pressed);
                        }
                        if (action instanceof TextAction) {
                            return false;
                        }

                    }

                }
                return super.processKeyBinding(ks, e, condition, pressed);
            }
        };
        // downloadFrom.setEditable(false);

        downloadFrom.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                downloadFrom.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        downloadFrom.setBorder(BorderFactory.createCompoundBorder(downloadFrom.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));
        checksum = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        checksum.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                checksum.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        checksum.setHelpText(_GUI.T.AddLinksDialog_layoutDialogContent_checksum_tt());
        setListeners(checksum);

        checksum.setBorder(BorderFactory.createCompoundBorder(checksum.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        // String latest = config.getLatestDownloadDestinationFolder();
        // if (latest == null || !config.isUseLastDownloadDestinationAsDefault()) {
        // destination.setFile(new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));
        //
        // } else {
        // destination.setFile(new File(latest));
        //
        // }

        password = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        password.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                password.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        setListeners(password);
        password.setHelpText(_GUI.T.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        priority = new PseudoCombo<Priority>(Priority.values()) {

            @Override
            protected Icon getIcon(Priority v, boolean closed) {
                return v.loadIcon(18);
            }

            @Override
            protected String getLabel(Priority v, boolean closed) {
                return v.T();
            }

            @Override
            protected Icon getPopIcon(boolean closed) {
                if (closed) {
                    return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
                } else {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
                }

            }

            @Override
            public void onChanged(Priority newValue) {
                super.onChanged(newValue);
                save();
            }

        };
        priority.setPopDown(true);
        // downloadPassword = new ExtTextField();
        // downloadPassword.setHelpText(_GUI.T.AddLinksDialog_createExtracOptionsPanel_downloadpassword());
        // downloadPassword.setBorder(BorderFactory.createCompoundBorder(downloadPassword.getBorder(), BorderFactory.createEmptyBorder(2, 6,
        // 1, 6)));

        autoExtract = new PseudoCombo<BooleanStatus>(BooleanStatus.values()) {
            @Override
            protected Icon getIcon(BooleanStatus v, boolean closed) {
                if (closed) {
                    switch (v) {
                    case FALSE:
                        return NewTheme.I().getIcon(IconKey.ICON_FALSE, 18);

                    case TRUE:
                        return NewTheme.I().getIcon(IconKey.ICON_TRUE, 18);

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return NewTheme.I().getIcon(IconKey.ICON_TRUE_ORANGE, 18);
                        } else {
                            return NewTheme.I().getIcon(IconKey.ICON_FALSE_ORANGE, 18);
                        }

                    }

                } else {
                    switch (v) {
                    case FALSE:
                        return NewTheme.I().getIcon(IconKey.ICON_FALSE, 18);

                    case TRUE:
                        return NewTheme.I().getIcon(IconKey.ICON_TRUE, 18);

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return NewTheme.I().getIcon(IconKey.ICON_TRUE_ORANGE, 18);
                        } else {
                            return NewTheme.I().getIcon(IconKey.ICON_FALSE_ORANGE, 18);
                        }

                    }

                }
                return null;
            }

            @Override
            protected String getLabel(BooleanStatus v, boolean closed) {
                if (closed) {
                    switch (v) {
                    case FALSE:
                        return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled_closed();

                    case TRUE:
                        return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled_closed();

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true_closed();
                        } else {
                            return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false_closed();
                        }

                    }

                } else {
                    switch (v) {
                    case FALSE:
                        return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled();

                    case TRUE:
                        return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled();

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true();
                        } else {
                            return _GUI.T.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false();
                        }

                    }

                }

                return "";
            }

            @Override
            protected Icon getPopIcon(boolean closed) {
                if (closed) {
                    return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
                } else {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
                }

            }

            @Override
            public void onChanged(BooleanStatus newValue) {
                super.onChanged(newValue);
                save();
            }
        };
        autoExtract.setPopDown(true);

        // p.add(createIconLabel("downloadpassword", _GUI.T.propertiespanel_downloadpassword(),
        // _GUI.T.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "alignx right,aligny center,height " + height + "!");

        // p.add(downloadPassword, "height " + height + "!");

        // this.getDialog().setLocation(new Point((int) (screenSize.getWidth() -
        // this.getDialog().getWidth()) / 2, (int) (screenSize.getHeight() -
        // this.getDialog().getHeight()) / 2));

        layoutComponents();
        autoExtract.setEnabled(false);
        password.setEnabled(false);
        onHidden();
    }

    protected void onHidden() {
        save();
    }

    protected void onShowing() {
        refresh(true);
    }

    protected ExtTextField createFileNameTextField() {
        return new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refresh();
    }

    protected void addArchiveLine(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_archivepassword(), _GUI.T.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(password, "pushx,growx,height " + height + "!,growx,width 10:10:n");
        p.add(autoExtract, "sg right,height " + height + "!,aligny top");

    }

    protected void addChecksum(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_checksum(), _GUI.T.AddLinksDialog_layoutDialogContent_checksum_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(checksum, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addCommentLine(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_comment(), _GUI.T.AddLinksDialog_layoutDialogContent_comment_tt()), "alignx right,aligny center,height " + height + "!");
        p.add(comment, "height " + height + "!,growx,width 10:10:n");
        p.add(priority, "sg right,height " + height + "!,aligny top");

    }

    protected void addDownloadFrom(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_downloadfrom(), _GUI.T.AddLinksDialog_layoutDialogContent_downloadfrom_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadFrom, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addDownloadPassword(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_passwod(), _GUI.T.AddLinksDialog_layoutDialogContent_password_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadpassword, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addFilename(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_filename(), _GUI.T.AddLinksDialog_layoutDialogContent_filename_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(filename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addPackagename(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_packagename(), _GUI.T.AddLinksDialog_layoutDialogContent_package_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(packagename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addSaveTo(int height, MigPanel p) {
        p.add(createIconLabel(_GUI.T.propertiespanel_downloadpath(), _GUI.T.AddLinksDialog_layoutDialogContent_save_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(destination.getDestination(), "height " + height + "!,growx,width 10:10:n");
        p.add(destination.getButton(), "sg right,height " + height + "! ");
    }

    private Component createIconLabel(String label, String tooltip) {
        JLabel ret = new JLabel();
        ret.setText(label);
        ret.setToolTipText(tooltip);
        SwingUtils.toBold(ret);
        ret.setEnabled(false);
        return ret;
    }

    protected void delayedSave() {
        if (settingLock.get() == 0) {
            saveDelayer.resetAndStart();
        }
    }

    abstract protected boolean isArchiveLineEnabled();

    abstract protected boolean isCheckSumEnabled();

    abstract protected boolean isCommentAndPriorityEnabled();

    abstract protected boolean isDownloadFromEnabled();

    abstract protected boolean isDownloadPasswordEnabled();

    abstract protected boolean isFileNameEnabled();

    abstract protected boolean isPackagenameEnabled();

    abstract protected boolean isSaveToEnabled();

    private void layoutComponents() {
        final int height = Math.max(24, (int) (comment.getPreferredSize().height * 0.9));
        final MigPanel p = this;
        p.removeAll();
        // LAFOptions.getInstance().getExtension().customizeOverviewPanelInsets()
        p.setLayout(new MigLayout("ins 0,wrap 3, gap " + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap(), "[][grow,fill][]", "[]"));
        if (isPackagenameEnabled()) {
            addPackagename(height, p);
        }
        if (isFileNameEnabled()) {
            addFilename(height, p);
        }
        if (isSaveToEnabled()) {
            addSaveTo(height, p);
        }
        if (isDownloadFromEnabled()) {
            addDownloadFrom(height, p);
        }
        if (isDownloadPasswordEnabled()) {
            addDownloadPassword(height, p);
        }
        if (isCheckSumEnabled()) {
            addChecksum(height, p);
        }
        if (isCommentAndPriorityEnabled()) {
            addCommentLine(height, p);
        }
        if (isArchiveLineEnabled()) {
            addArchiveLine(height, p);
        }
        refresh(true);
        revalidate();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                layoutComponents();
            }

        };
    }

    private final AtomicLong refreshRequested = new AtomicLong(0);
    private final AtomicLong refreshDone      = new AtomicLong(0);

    public void refresh() {
        refreshRequested.incrementAndGet();
        updateDelayer.resetAndStart();
    }

    protected void refresh(final boolean forceRefresh) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                final E lAbstractNodeProperties = getAbstractNodeProperties();
                if (lAbstractNodeProperties == null || !isShowing() || savingLock.get() > 0) {
                    return;
                }
                loadInEDT(forceRefresh, lAbstractNodeProperties);
            }
        };
    }

    public void setSelectedItem(final AbstractNode abstractNode) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (isDifferent(abstractNode)) {
                    if (abstractNode == null) {
                        setAbstractNodeProperties(null);
                    } else {
                        setAbstractNodeProperties(createAbstractNodeProperties(abstractNode));
                    }
                }
            }
        };
    }

    protected abstract E createAbstractNodeProperties(AbstractNode abstractNode);

    protected boolean isDifferent(AbstractNode abstractNode) {
        final E lAbstractNodeProperties = getAbstractNodeProperties();
        if (lAbstractNodeProperties == null && abstractNode != null) {
            return true;
        } else if (lAbstractNodeProperties != null && abstractNode == null) {
            return true;
        } else {
            return lAbstractNodeProperties != null && abstractNode != null && lAbstractNodeProperties.isDifferent(abstractNode);
        }
    }

    protected boolean isSame(AbstractNode abstractNode) {
        final E lAbstractNodeProperties = getAbstractNodeProperties();
        return lAbstractNodeProperties != null && !lAbstractNodeProperties.isDifferent(abstractNode);
    }

    public void save() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                final E lAbstractNodeProperties = getAbstractNodeProperties();
                if (lAbstractNodeProperties == null || settingLock.get() > 0) {
                    return;
                }
                if (refreshDone.get() != refreshRequested.get()) {
                    loadInEDT(false, lAbstractNodeProperties);
                }
                saveInEDT(lAbstractNodeProperties);
            }
        };
    }

    private String emptyAsNull(final String string) {
        if (StringUtils.isEmpty(string)) {
            return null;
        } else {
            return string;
        }
    }

    protected void saveInEDT(final AbstractNodeProperties abstractNodes) {
        try {
            savingLock.incrementAndGet();
            if (priority.getParent() != null) {
                abstractNodes.savePriority(priority.getSelectedItem());
            }
            if (comment.getParent() != null) {
                abstractNodes.saveComment(emptyAsNull(comment.getText()));
            }
            if (filename.getParent() != null) {
                abstractNodes.saveFilename(filename.getText());
            }
            if (downloadpassword.getParent() != null) {
                abstractNodes.saveDownloadPassword(emptyAsNull(downloadpassword.getText()));
            }
            if (checksum.getParent() != null) {
                String cs = checksum.getText();
                cs = cs.replaceAll("\\[.*?\\]", "").trim();
                final HashInfo hashInfo;
                if (cs.length() == 8) {
                    hashInfo = HashInfo.newInstanceSafe(cs, HashInfo.TYPE.CRC32);
                } else if (cs.length() == 32) {
                    hashInfo = HashInfo.newInstanceSafe(cs, HashInfo.TYPE.MD5);
                } else if (cs.length() == 40) {
                    hashInfo = HashInfo.newInstanceSafe(cs, HashInfo.TYPE.SHA1);
                } else if (cs.length() == 64) {
                    hashInfo = HashInfo.newInstanceSafe(cs, HashInfo.TYPE.SHA256);
                } else if (cs.length() == 128) {
                    hashInfo = HashInfo.newInstanceSafe(cs, HashInfo.TYPE.SHA512);
                } else {
                    hashInfo = new HashInfo("", HashInfo.TYPE.NONE, true, true);
                }
                abstractNodes.saveHashInfo(hashInfo);
            }
            if (packagename.getParent() != null) {
                final String newName = packagename.getText();
                final String oldName = abstractNodes.loadPackageName();
                if (!StringUtils.equals(newName, oldName)) {
                    abstractNodes.savePackageName(newName);
                    PackageHistoryManager.getInstance().add(newName);
                }
            }
            if (abstractNodes.hasLoadedArchives()) {
                final List<Archive> archives = abstractNodes.loadArchives();
                if (archives != null && archives.size() == 1) {
                    if (password.getParent() != null) {
                        ArrayList<String> passwords = null;
                        final String txt = password.getText().trim();
                        if (txt.startsWith("[") && txt.endsWith("]")) {
                            passwords = JSonStorage.restoreFromString(txt, new TypeRef<ArrayList<String>>() {
                            }, null);
                        }
                        if (passwords != null && passwords.size() > 0) {
                            abstractNodes.saveArchivePasswords(new ArrayList<String>(new LinkedHashSet<String>(passwords)));
                        } else {
                            final List<String> passwordsList = new ArrayList<String>();
                            if (StringUtils.isNotEmpty(txt)) {
                                passwordsList.add(txt);
                            }
                            abstractNodes.saveArchivePasswords(passwordsList);
                        }
                    }
                    if (autoExtract.getParent() != null) {
                        abstractNodes.saveAutoExtract(autoExtract.getSelectedItem());
                    }
                }
            }
            if (destination.getDestination().getParent() != null) {
                final String saveTo = abstractNodes.loadSaveTo();
                final String path = destination.getPath();
                if (!StringUtils.equals(saveTo, path)) {
                    abstractNodes.saveSaveTo(PackagizerController.replaceDynamicTags(path, abstractNodes.loadPackageName(), abstractNodes.getCurrentNode()));
                    DownloadPathHistoryManager.getInstance().add(path);
                }
            }
        } finally {
            savingLock.decrementAndGet();
        }
    }

    class Listener implements ActionListener, KeyListener, FocusListener {
        private final JTextField field;
        private boolean          saveOnFocusLost = true;
        private String           oldText;

        public Listener(JTextField filename) {
            this.field = filename;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            save();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                field.setText(oldText);
                field.selectAll();
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            oldText = field.getText();
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (saveOnFocusLost) {
                save();
            } else {
                refresh();
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }
    }

    protected void setListeners(final JTextField filename) {
        final Listener list = new Listener(filename);
        filename.addActionListener(list);
        filename.addKeyListener(list);
        filename.addFocusListener(list);
    }

    protected void loadInEDT(final boolean newData, final AbstractNodeProperties abstractNodes) {
        try {
            settingLock.incrementAndGet();
            if (downloadFrom.getParent() != null && (newData || !downloadFrom.hasFocus())) {
                downloadFrom.setText(abstractNodes.loadDownloadFrom());
            }
            if (filename.getParent() != null && (newData || !filename.hasFocus())) {
                filename.setText(abstractNodes.loadFilename());
            }
            if (comment.getParent() != null && (newData || !comment.hasFocus())) {
                comment.setText(abstractNodes.loadComment());
            }
            if (destination.getDestination().getParent() != null && (newData || !destination.getDestination().hasFocus())) {
                final String saveTo = abstractNodes.loadSaveTo();
                final List<String> pathlist = DownloadPathHistoryManager.getInstance().listPaths(saveTo, org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
                destination.setQuickSelectionList(pathlist);
                destination.setFile(new File(saveTo));
            }
            if (checksum.getParent() != null && (newData || !checksum.hasFocus())) {
                final HashInfo hashInfo = abstractNodes.loadHashInfo();
                if (hashInfo != null) {
                    checksum.setText("[" + hashInfo.getType() + "] " + hashInfo.getHash());
                } else {
                    checksum.setText(null);
                }
            }
            if (downloadpassword.getParent() != null && (newData || !downloadpassword.hasFocus())) {
                downloadpassword.setText(abstractNodes.loadDownloadPassword());
            }
            if (packagename.getParent() != null && (newData || !packagename.hasFocus())) {
                final String packageName = abstractNodes.loadPackageName();
                packagename.setList(PackageHistoryManager.getInstance().list(new PackageHistoryEntry(packageName)));
                packagename.setSelectedItem(new PackageHistoryEntry(packageName));
            }
            if (password.getParent() != null && (newData || !password.hasFocus())) {
                password.setText("");
            }
            if (priority.getParent() != null && (newData || !priority.hasFocus())) {
                priority.setSelectedItem(abstractNodes.loadPriority());
            }
            if (abstractNodes.hasLoadedArchives()) {
                updateArchiveInEDT(newData, abstractNodes);
            } else {
                SERVICE.execute(new Runnable() {

                    @Override
                    public void run() {
                        final E current = getAbstractNodeProperties();
                        if (current == abstractNodes) {
                            abstractNodes.loadArchives();
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    final E current = getAbstractNodeProperties();
                                    if (current == abstractNodes) {
                                        updateArchiveInEDT(newData, abstractNodes);
                                    }
                                }
                            };
                        }
                    }
                });
            }
        } finally {
            settingLock.decrementAndGet();
            refreshDone.set(refreshRequested.get());
        }
    }

    protected void updateArchiveInEDT(final boolean newData, final AbstractNodeProperties abstractNodes) {
        try {
            settingLock.incrementAndGet();
            final List<Archive> archives = abstractNodes.loadArchives();
            final boolean validArchiveSelection = archives != null && archives.size() == 1;
            autoExtract.setEnabled(validArchiveSelection);
            password.setEnabled(validArchiveSelection);
            if (validArchiveSelection) {
                final Archive archive = archives.get(0);
                if (password.getParent() != null && (newData || !password.hasFocus())) {
                    final String finalPassword = archive.getFinalPassword();
                    if (StringUtils.isNotEmpty(finalPassword)) {
                        password.setText(finalPassword);
                    } else {
                        final List<String> passwords = archive.getSettings().getPasswords();
                        if (passwords == null || passwords.size() == 0) {
                            password.setText(null);
                        } else if (passwords.size() == 1) {
                            password.setText(passwords.get(0));
                        } else {
                            password.setText(JSonStorage.toString(passwords));
                        }
                    }
                }
                if (autoExtract.getParent() != null && (newData || !autoExtract.hasFocus())) {
                    autoExtract.setSelectedItem(archive.getSettings().getAutoExtract());
                }
            }
        } finally {
            settingLock.decrementAndGet();
        }
    }
}
