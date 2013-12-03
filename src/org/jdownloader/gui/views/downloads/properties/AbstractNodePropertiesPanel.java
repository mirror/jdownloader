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
import java.util.HashSet;
import java.util.List;

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

import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;
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

public abstract class AbstractNodePropertiesPanel extends MigPanel implements ActionListener, GenericConfigEventListener<Boolean> {
    // private ExtTextField downloadPassword;
    protected PseudoCombo<BooleanStatus>          autoExtract;
    private ExtTextField                          checksum;
    protected ExtTextField                        comment;
    protected LinkgrabberSettings                 config;

    protected Archive                             currentArchive;

    protected PathChooser                         destination;
    private ExtTextField                          downloadFrom;
    private ExtTextField                          downloadpassword;
    protected ExtTextField                        filename;
    protected SearchComboBox<PackageHistoryEntry> packagename;
    protected ExtTextField                        password;
    protected PseudoCombo<Priority>               priority;
    private DelayedRunnable                       saveDelayer;
    protected boolean                             saving;

    protected boolean                             setting;

    private DelayedRunnable                       updateDelayer;

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

    }

    public AbstractNodePropertiesPanel() {
        super("ins 0,debug", "[grow,fill]", "[grow,fill]");
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

        saveDelayer = new DelayedRunnable(500l, 2000l) {

            @Override
            public void delayedrun() {
                save();
            }

            @Override
            public String getID() {
                return "PropertiesSaver";
            }

        };
        updateDelayer = new DelayedRunnable(1000l, 2000l) {

            @Override
            public void delayedrun() {
                internalRefresh(false);
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
                return _GUI._.AddLinksDialog_layoutDialogContent_help_destination();
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
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
            protected void sortFound(final List<PackageHistoryEntry> found) {
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
        packagename.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_packagename_help());
        packagename.setSelectedItem(null);
        setListeners(packagename.getTextField());
        comment = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        comment.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_comment_help());
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
        downloadpassword.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_password_tt());
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

                        if (action instanceof CopyAction) { return super.processKeyBinding(ks, e, condition, pressed); }
                        if ("select-all".equals(binding)) return super.processKeyBinding(ks, e, condition, pressed);
                        if (action instanceof TextAction) { return false; }

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
        checksum.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_checksum_tt());
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
        password.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        priority = new PseudoCombo<Priority>(Priority.values()) {

            @Override
            protected Icon getIcon(Priority v, boolean closed) {
                if (v == null) { return getHighestPackagePriorityIcon();

                }
                return v.loadIcon(18);
            }

            @Override
            protected String getLabel(Priority v, boolean closed) {
                if (v == null) {

                return _GUI._.PackagePropertiesPanel_getLabel_mixed_priority(); }
                return v._();
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
        // downloadPassword.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_downloadpassword());
        // downloadPassword.setBorder(BorderFactory.createCompoundBorder(downloadPassword.getBorder(), BorderFactory.createEmptyBorder(2, 6,
        // 1, 6)));

        autoExtract = new PseudoCombo<BooleanStatus>(BooleanStatus.values()) {
            @Override
            protected Icon getIcon(BooleanStatus v, boolean closed) {
                if (closed) {
                    switch ((BooleanStatus) v) {
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
                    switch ((BooleanStatus) v) {
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
                    switch ((BooleanStatus) v) {
                    case FALSE:
                        return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled_closed();

                    case TRUE:
                        return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled_closed();

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true_closed();
                        } else {
                            return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false_closed();
                        }

                    }

                } else {
                    switch ((BooleanStatus) v) {
                    case FALSE:
                        return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled();

                    case TRUE:
                        return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled();

                    case UNSET:
                        if (CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled()) {
                            return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true();
                        } else {
                            return _GUI._.PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false();
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

        // p.add(createIconLabel("downloadpassword", _GUI._.propertiespanel_downloadpassword(),
        // _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "alignx right,aligny center,height " + height + "!");

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
        p.add(createIconLabel(_GUI._.propertiespanel_archivepassword(), _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,alignx right,height " + height + "!");

        p.add(password, "pushx,growx,height " + height + "!,growx,width 10:10:n");

        p.add(autoExtract, "sg right,height " + height + "!");
    }

    protected void addChecksum(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_checksum(), _GUI._.AddLinksDialog_layoutDialogContent_checksum_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(checksum, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addCommentLine(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_comment(), _GUI._.AddLinksDialog_layoutDialogContent_comment_tt()), "alignx right,aligny center,height " + height + "!");
        p.add(comment, "height " + height + "!,growx,width 10:10:n");
        p.add(priority, "sg right,height " + height + "!");
    }

    protected void addDownloadFrom(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_downloadfrom(), _GUI._.AddLinksDialog_layoutDialogContent_downloadfrom_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadFrom, "spanx,height " + height + "!,growx,width 10:10:n");
        // "gaptop 0,spanx,growx,pushx,gapleft 37,gapbottom 5"
    }

    protected void addDownloadPassword(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_passwod(), _GUI._.AddLinksDialog_layoutDialogContent_password_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadpassword, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addFilename(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_filename(), _GUI._.AddLinksDialog_layoutDialogContent_filename_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(filename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addPackagename(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_packagename(), _GUI._.AddLinksDialog_layoutDialogContent_package_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(packagename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addSaveTo(int height, MigPanel p) {
        p.add(createIconLabel(_GUI._.propertiespanel_downloadpath(), _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny center,alignx right,height " + height + "!");

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
        if (setting) return;
        saveDelayer.resetAndStart();
    }

    abstract protected Icon getHighestPackagePriorityIcon();

    abstract protected boolean isArchiveLineEnabled();

    abstract protected boolean isCheckSumEnabled();

    abstract protected boolean isCommentAndPriorityEnabled();

    abstract protected boolean isDownloadFromEnabled();

    abstract protected boolean isDownloadPasswordEnabled();

    abstract protected boolean isFileNameEnabled();

    abstract protected boolean isPackagenameEnabled();

    abstract protected boolean isSaveToEnabled();

    private void layoutComponents() {
        int height = Math.max(24, (int) (comment.getPreferredSize().height * 0.9));
        MigPanel p = this;
        p.removeAll();
        p.setLayout(new MigLayout("ins 0 0 0 0,wrap 3", "[][grow,fill]2[]", "2[]0"));
        if (isPackagenameEnabled()) addPackagename(height, p);
        if (isFileNameEnabled()) addFilename(height, p);
        if (isSaveToEnabled()) addSaveTo(height, p);
        if (isDownloadFromEnabled()) addDownloadFrom(height, p);
        //
        if (isDownloadPasswordEnabled()) addDownloadPassword(height, p);
        if (isCheckSumEnabled()) addChecksum(height, p);
        if (isCommentAndPriorityEnabled()) addCommentLine(height, p);
        if (isArchiveLineEnabled()) addArchiveLine(height, p);
        refresh(true);
        revalidate();

    }

    abstract protected List<Archive> loadArchives();

    abstract protected String loadComment();

    abstract protected String loadDownloadFrom();

    abstract protected String loadDownloadPassword();

    abstract protected String loadFilename();

    abstract protected String loadMD5();

    abstract protected String loadPackageName();

    abstract protected Priority loadPriority();

    abstract protected String loadSaveTo();

    abstract protected String loadSha1();

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

    public void refresh() {
        refresh(false);
    }

    public void refresh(final boolean newData) {
        if (newData) {
            internalRefresh(newData);
        } else {
            updateDelayer.resetAndStart();
        }
    }

    private void internalRefresh(final boolean newData) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!isShowing()) return;

                if (saving) return;
                System.out.println("REFRESH PANEL! " + newData + " +" + this);
                currentArchive = null;
                new Thread("PropertiesPanelUpdater") {
                    public void run() {

                        final List<Archive> archives = loadArchives();

                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                setting = true;
                                try {
                                    autoExtract.setEnabled(archives != null && archives.size() == 1);
                                    password.setEnabled(archives != null && archives.size() == 1);
                                    if (password.isEnabled()) {
                                        updateArchiveInEDT(archives.get(0), newData);

                                    }
                                } finally {
                                    setting = false;
                                }
                            }
                        };
                    }

                }.start();
                setting = true;
                try {
                    updateInEDT(newData);

                } finally {
                    setting = false;
                }
                // extractToggle.setSelected(true);

            }
        };

    }

    public void save() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (setting) return;
                saving = true;
                try {
                    saveInEDT();
                } finally {
                    saving = false;
                }
            }

        };

    }

    abstract protected void saveArchivePasswords(HashSet<String> hashSet);

    abstract protected void saveAutoExtract(BooleanStatus selectedItem);

    abstract protected void saveComment(String text);

    abstract protected void saveDownloadPassword(String text);

    abstract protected void saveFilename(String text);

    protected void saveInEDT() {

        if (priority.isShowing()) {
            Priority priop = priority.getSelectedItem();
            savePriority(priop);

        }
        if (comment.isShowing()) {
            saveComment(comment.getText());
        }
        if (filename.isShowing()) {
            saveFilename(filename.getText());
        }
        if (downloadpassword.isShowing()) {
            saveDownloadPassword(downloadpassword.getText());
        }
        if (checksum.isShowing()) {
            String cs = checksum.getText();
            cs = cs.replaceAll("\\[.*?\\]", "").trim();
            if (cs.length() == 32) {
                saveMd5(cs);

            } else if (cs.length() == 40) {
                saveSha1(cs);

            } else {
                saveMd5(null);
                saveSha1(cs);
            }
        }
        if (packagename.isShowing()) {

            if (!loadPackageName().equals(packagename.getText())) {
                savePackageName(packagename.getText());
                PackageHistoryManager.getInstance().add(packagename.getText());
            }
        }

        if (password.isShowing()) {
            if (currentArchive != null) {
                System.out.println("SAVE");
                ArrayList<String> passwords = null;
                String txt = password.getText().trim();
                if (txt.startsWith("[") && txt.endsWith("]")) {
                    passwords = JSonStorage.restoreFromString(password.getText(), new TypeRef<ArrayList<String>>() {
                    }, null);
                }
                if (passwords != null && passwords.size() > 0) {
                    saveArchivePasswords(new HashSet<String>(passwords));

                } else {
                    HashSet<String> hs = new HashSet<String>();
                    if (StringUtils.isNotEmpty(password.getText())) hs.add(password.getText().trim());

                    saveArchivePasswords(hs);
                }
                saveAutoExtract(autoExtract.getSelectedItem());

            }
        }
        if (destination.getDestination().isShowing() && !loadSaveTo().equals(new File(destination.getPath()))) {
            saveSaveTo(destination.getPath());

            DownloadPathHistoryManager.getInstance().add(destination.getPath());
        }

    }

    abstract protected void saveMd5(String cs);

    abstract protected void savePackageName(String text);

    abstract protected void savePriority(Priority priop);

    abstract protected void saveSaveTo(String path);

    abstract protected void saveSha1(String cs);

    class Listener implements ActionListener, KeyListener, FocusListener {
        private JTextField field;
        private boolean    saveOnFocusLost = true;
        private String     oldText;

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
                System.out.println("set " + oldText);
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
        Listener list = new Listener(filename);
        filename.addActionListener(list);
        filename.addKeyListener(list);
        filename.addFocusListener(list);
    }

    protected void updateArchiveInEDT(final Archive archive, boolean newData) {
        currentArchive = archive;
        if ((newData || !password.hasFocus()) && password.isShowing()) {
            if (currentArchive.getSettings().getPasswords() == null || currentArchive.getSettings().getPasswords().size() == 0) {
                password.setText(null);
            } else if (currentArchive.getSettings().getPasswords().size() == 1) {

                password.setText(currentArchive.getSettings().getPasswords().iterator().next());
            } else {
                password.setText(JSonStorage.toString(currentArchive.getSettings().getPasswords()));
            }
        }
        if (autoExtract.isShowing()) {
            autoExtract.setSelectedItem(currentArchive.getSettings().getAutoExtract());
        }
    }

    protected void updateInEDT(boolean newData) {

        if ((!downloadFrom.hasFocus() || newData) && downloadFrom.isShowing()) {
            downloadFrom.setText(loadDownloadFrom());
        }

        if ((!filename.hasFocus() || newData) && filename.isShowing()) {
            filename.setText(loadFilename());
        }
        if ((!comment.hasFocus() || newData) && comment.isShowing()) {

            comment.setText(loadComment());

        }

        if ((!destination.getDestination().hasFocus() || newData) && destination.getDestination().isShowing()) {
            String saveto = null;
            List<String> pathlist = DownloadPathHistoryManager.getInstance().listPathes(saveto = loadSaveTo(), org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());

            destination.setQuickSelectionList(pathlist);
            destination.setFile(new File(saveto));
        }

        if ((!checksum.hasFocus() || newData) && checksum.isShowing()) {
            //
            // if (dl.getMD5Hash() != null) return "[MD5] " + dl.getMD5Hash();
            String sha1 = loadSha1();
            String md5 = loadMD5();
            if (!StringUtils.isEmpty(sha1)) {
                checksum.setText("[SHA1] " + sha1);
            } else if (!StringUtils.isEmpty(md5)) {
                checksum.setText("[MD5] " + md5);
            } else {
                checksum.setText(null);
            }

        }
        if ((!downloadpassword.hasFocus() || newData) && downloadpassword.isShowing()) {
            downloadpassword.setText(loadDownloadPassword());
        }
        if ((!packagename.hasFocus() || newData) && packagename.isShowing()) {
            packagename.setList(PackageHistoryManager.getInstance().list(new PackageHistoryEntry(loadPackageName())));
            packagename.setSelectedItem(new PackageHistoryEntry(loadPackageName()));
        }

        if ((!password.hasFocus() || newData) && password.isShowing()) password.setText("");
        if (priority.isShowing()) {
            priority.setSelectedItem(loadPriority());
        }

    }

}
