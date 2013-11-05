package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
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
import javax.swing.Timer;
import javax.swing.text.DefaultEditorKit.CopyAction;
import javax.swing.text.TextAction;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;
import jd.plugins.DownloadLink;
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
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryEntry;
import org.jdownloader.gui.packagehistorycontroller.PackageHistoryManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkPropertiesPanel extends MigPanel implements LinkCollectorListener, ActionListener, GenericConfigEventListener<Boolean> {

    protected PathChooser                         destination;
    protected SearchComboBox<PackageHistoryEntry> packagename;
    protected LinkgrabberSettings                 config;
    protected ExtTextField                        comment;

    protected ExtTextField                        password;
    protected PseudoCombo<Priority>               priority;
    // private ExtTextField downloadPassword;
    protected PseudoCombo<BooleanStatus>          autoExtract;
    protected CrawledPackage                      currentPackage;
    private DelayedRunnable                       saveDelayer;
    private Timer                                 timer;
    protected boolean                             saving;
    protected ExtTextField                        filename;
    private ExtTextField                          downloadpassword;
    private ExtTextField                          checksum;
    private DelayedRunnable                       updateDelayer;
    private ExtTextField                          downloadFrom;

    public LinkPropertiesPanel() {
        super("ins 0,debug", "[grow,fill]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
        config = JsonConfig.create(LinkgrabberSettings.class);
        LinkCollector.getInstance().getEventsender().addListener(this, true);
        // some properties like archive password have not listener support
        timer = new Timer(5000, this);
        timer.setRepeats(true);
        // timer.start();
        saveDelayer = new DelayedRunnable(500l, 2000l) {

            @Override
            public String getID() {
                return "PropertiesSaver";
            }

            @Override
            public void delayedrun() {
                save();
            }

        };
        updateDelayer = new DelayedRunnable(1000l, 2000l) {

            @Override
            public String getID() {
                return "updateDelayer";
            }

            @Override
            public void delayedrun() {

            }

        };
        destination = new PathChooser("ADDLinks", true) {
            protected void onChanged(ExtTextField txt2) {

                // delayedSave();
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
                return menu;
            }

            public void setFile(final File file) {
                super.setFile(file);
                save();
            }

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
        };
        setListeners(destination.getTxt());
        packagename = new SearchComboBox<PackageHistoryEntry>() {

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

            @Override
            protected Icon getIconForValue(PackageHistoryEntry value) {
                return null;
            }

            @Override
            protected String getTextForValue(PackageHistoryEntry value) {
                return value == null ? null : value.getName();
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
        filename = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
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
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                downloadpassword.selectAll();
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
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                downloadFrom.selectAll();
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
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                checksum.selectAll();
            }
        });
        checksum.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_checksum_tt());
        setListeners(checksum);

        checksum.setBorder(BorderFactory.createCompoundBorder(checksum.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        String latest = config.getLatestDownloadDestinationFolder();
        if (latest == null || !config.isUseLastDownloadDestinationAsDefault()) {
            destination.setFile(new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));

        } else {
            destination.setFile(new File(latest));

        }

        password = new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
        password.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                password.selectAll();
            }
        });
        setListeners(password);
        password.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        priority = new PseudoCombo<Priority>(Priority.values()) {

            @Override
            public void onChanged(Priority newValue) {
                super.onChanged(newValue);
                save();
            }

            @Override
            protected Icon getPopIcon(boolean closed) {
                if (closed) {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUPDOWN, -1);
                } else {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUP, -1);
                }

            }

            @Override
            protected Icon getIcon(Priority v, boolean closed) {
                if (v == null) { return ImageProvider.getDisabledIcon(currentPackage.getView().getHighestPriority().loadIcon(18));

                }
                return v.loadIcon(18);
            }

            @Override
            protected String getLabel(Priority v, boolean closed) {
                if (v == null) {

                return _GUI._.PackagePropertiesPanel_getLabel_mixed_priority(); }
                return v._();
            }

        };
        priority.setPopDown(true);
        // downloadPassword = new ExtTextField();
        // downloadPassword.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_downloadpassword());
        // downloadPassword.setBorder(BorderFactory.createCompoundBorder(downloadPassword.getBorder(), BorderFactory.createEmptyBorder(2, 6,
        // 1, 6)));

        autoExtract = new PseudoCombo<BooleanStatus>(BooleanStatus.values()) {
            @Override
            protected Icon getPopIcon(boolean closed) {
                if (closed) {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUPDOWN, -1);
                } else {
                    return NewTheme.I().getIcon(IconKey.ICON_POPUP, -1);
                }

            }

            @Override
            public void onChanged(BooleanStatus newValue) {
                super.onChanged(newValue);
                save();
            }

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
        };
        autoExtract.setPopDown(true);

        // p.add(createIconLabel("downloadpassword", _GUI._.propertiespanel_downloadpassword(),
        // _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "alignx right,aligny center,height " + height + "!");

        // p.add(downloadPassword, "height " + height + "!");

        // this.getDialog().setLocation(new Point((int) (screenSize.getWidth() -
        // this.getDialog().getWidth()) / 2, (int) (screenSize.getHeight() -
        // this.getDialog().getHeight()) / 2));
        CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.getEventSender().addListener(this, true);
        update();
        autoExtract.setEnabled(false);
        password.setEnabled(false);

    }

    private void update() {
        int height = Math.max(24, (int) (comment.getPreferredSize().height * 0.9));
        MigPanel p = this;
        p.removeAll();
        p.setLayout(new MigLayout("ins 0 0 0 0,wrap 3", "[][]2[]", "2[]0"));
        if (CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.isEnabled()) addSaveTo(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.isEnabled()) addFilename(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.isEnabled()) addPackagename(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.isEnabled()) addDownloadFrom(height, p);
        //
        if (CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.isEnabled()) addDownloadPassword(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.isEnabled()) addChecksum(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.isEnabled()) addCommentLine(height, p);
        if (CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.isEnabled()) addArchiveLine(height, p);

        revalidate();

    }

    protected void addDownloadFrom(int height, MigPanel p) {
        p.add(createIconLabel(IconKey.ICON_URL, _GUI._.propertiespanel_downloadfrom(), _GUI._.AddLinksDialog_layoutDialogContent_downloadfrom_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadFrom, "spanx,height " + height + "!,growx,width 10:10:n");

        // "gaptop 0,spanx,growx,pushx,gapleft 37,gapbottom 5"

    }

    protected void addArchiveLine(int height, MigPanel p) {
        p.add(createIconLabel("archivepassword", _GUI._.propertiespanel_archivepassword(), _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,alignx right,height " + height + "!");

        p.add(password, "pushx,growx,height " + height + "!,growx,width 10:10:n");

        p.add(autoExtract, "sg right,height " + height + "!");
    }

    protected void addCommentLine(int height, MigPanel p) {
        p.add(createIconLabel("document", _GUI._.propertiespanel_comment(), _GUI._.AddLinksDialog_layoutDialogContent_comment_tt()), "alignx right,aligny center,height " + height + "!");
        p.add(comment, "height " + height + "!,growx,width 10:10:n");
        p.add(priority, "sg right,height " + height + "!");
    }

    protected void addPackagename(int height, MigPanel p) {
        p.add(createIconLabel("package_open", _GUI._.propertiespanel_packagename(), _GUI._.AddLinksDialog_layoutDialogContent_package_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(packagename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addChecksum(int height, MigPanel p) {
        p.add(createIconLabel("package_open", _GUI._.propertiespanel_checksum(), _GUI._.AddLinksDialog_layoutDialogContent_checksum_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(checksum, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addDownloadPassword(int height, MigPanel p) {
        p.add(createIconLabel("password", _GUI._.propertiespanel_passwod(), _GUI._.AddLinksDialog_layoutDialogContent_password_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(downloadpassword, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addFilename(int height, MigPanel p) {
        p.add(createIconLabel("package_open", _GUI._.propertiespanel_filename(), _GUI._.AddLinksDialog_layoutDialogContent_filename_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(filename, "spanx,height " + height + "!,growx,width 10:10:n");
    }

    protected void addSaveTo(int height, MigPanel p) {
        p.add(createIconLabel("save", _GUI._.propertiespanel_downloadpath(), _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny center,alignx right,height " + height + "!");

        p.add(destination.getDestination(), "height " + height + "!,growx,width 10:10:n");
        p.add(destination.getButton(), "sg right,height " + height + "! ");
    }

    protected void setListeners(final JTextField filename) {
        filename.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
        });
        filename.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                if (currentLink != null) {
                    update(currentLink);
                } else if (currentPackage != null) {
                    update(currentPackage);
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });
    }

    protected void delayedSave() {
        if (setting) return;
        saveDelayer.resetAndStart();
    }

    protected void saveInEDT() {
        if (currentPackage != null) {
            if (priority.isShowing()) {
                Priority priop = priority.getSelectedItem();
                currentLink.setPriority(priop);
            }
            if (comment.isShowing()) {
                currentLink.getDownloadLink().setComment(comment.getText());
            }
            if (filename.isShowing()) {
                currentLink.setName(filename.getText());
            }
            if (downloadpassword.isShowing()) {
                currentLink.getDownloadLink().setDownloadPassword(downloadpassword.getText());
            }
            if (checksum.isShowing()) {
                String cs = checksum.getText();
                cs = cs.replaceAll("\\[.*?\\]", "").trim();
                if (cs.length() == 32) {
                    currentLink.getDownloadLink().setMD5Hash(cs);
                } else if (cs.length() == 40) {
                    currentLink.getDownloadLink().setSha1Hash(cs);
                } else {
                    currentLink.getDownloadLink().setMD5Hash(null);
                    currentLink.getDownloadLink().setSha1Hash(null);
                }
            }
            if (packagename.isShowing()) {
                if (!currentPackage.getName().equals(packagename.getText())) {
                    currentPackage.setName(packagename.getText());
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
                        currentArchive.getSettings().setPasswords(new HashSet<String>(passwords));
                    } else {
                        HashSet<String> hs = new HashSet<String>();
                        if (StringUtils.isNotEmpty(password.getText())) hs.add(password.getText().trim());
                        currentArchive.getSettings().setPasswords(hs);
                    }
                    currentArchive.getSettings().setAutoExtract(autoExtract.getSelectedItem());

                    if (!LinkTreeUtils.getRawDownloadDirectory(currentPackage).equals(new File(destination.getPath()))) {
                        currentPackage.setDownloadFolder(destination.getPath());
                        DownloadPathHistoryManager.getInstance().add(destination.getPath());
                    }
                }
            }

        }
    }

    protected void save() {
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

    private Component createIconLabel(String iconKey, String label, String tooltip) {
        JLabel ret = new JLabel();
        // ret.setDisabledIcon(NewTheme.I().getIcon(iconKey, 20));
        ret.setText(label);
        ret.setToolTipText(tooltip);
        SwingUtils.toBold(ret);
        ret.setEnabled(false);
        return ret;
    }

    protected Archive     currentArchive;
    protected boolean     setting;
    protected CrawledLink currentLink;
    private AbstractNode  current;

    public void update(final CrawledPackage pkg) {
        if (!isVisible()) return;
        if (saving) return;
        if (current == pkg) return;
        current = pkg;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                boolean ld = setting;
                setting = true;
                try {
                    updateInEDT(null, pkg);
                    currentLink = null;
                    currentPackage = pkg;
                } finally {
                    setting = ld;
                }
                // extractToggle.setSelected(true);

            }
        };
        currentArchive = null;
        new Thread("PropertiesPanelUpdater") {
            public void run() {

                final List<Archive> archives = ArchiveValidator.validate(new SelectionInfo<CrawledPackage, CrawledLink>(pkg, null, null, null, null, null)).getArchives();

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setting = true;
                        try {
                            autoExtract.setEnabled(archives != null && archives.size() == 1);
                            password.setEnabled(archives != null && archives.size() == 1);
                            if (password.isEnabled()) {
                                updateArchiveInEDT(archives.get(0));

                            }
                        } finally {
                            setting = false;
                        }
                    }
                };
            }
        }.start();
    }

    public void update(final CrawledLink link) {
        if (!isVisible()) return;
        if (saving) return;
        if (current == link) return;
        current = link;
        final CrawledPackage pkg = link.getParentNode();
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                setting = true;
                try {
                    updateInEDT(link, pkg);
                    currentLink = link;
                    currentPackage = pkg;
                } finally {
                    setting = false;
                }
                // extractToggle.setSelected(true);

            }
        };
        currentArchive = null;
        new Thread("PropertiesPanelUpdater") {
            public void run() {

                final List<Archive> archives = ArchiveValidator.validate(new SelectionInfo<CrawledPackage, CrawledLink>(link, null, null, null, null, null)).getArchives();

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setting = true;
                        try {
                            autoExtract.setEnabled(archives != null && archives.size() == 1);
                            password.setEnabled(archives != null && archives.size() == 1);
                            if (password.isEnabled()) {
                                updateArchiveInEDT(archives.get(0));

                            }
                        } finally {
                            setting = false;
                        }
                    }
                };
            }
        }.start();
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        //
        if (event.getParameter() == currentPackage || event.getParameter() == currentLink) {
            if (currentLink != null) {
                update(currentLink);
            } else if (currentPackage != null) {
                update(currentPackage);
            }
        }
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        if (currentLink != null) {
            update(currentLink);
        } else if (currentPackage != null) {
            update(currentPackage);
        }
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentLink != null) {
            update(currentLink);
        } else if (currentPackage != null) {
            update(currentPackage);
        }
    }

    protected void updateInEDT(final CrawledLink link, final CrawledPackage pkg) {
        if (pkg == null) {
            // may happen when we remove links
            return;
        }

        updateDownloadFrom(link);

        if (link != null && !filename.hasFocus()) {
            filename.setText(link.getName());
        }
        if (!comment.hasFocus()) {
            if (link != null) {
                comment.setText(link.getDownloadLink().getComment());
            }
        }
        List<String> pathlist = DownloadPathHistoryManager.getInstance().listPathes(LinkTreeUtils.getRawDownloadDirectory(pkg).getAbsolutePath(), org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());

        if (!destination.getDestination().hasFocus()) {
            destination.setQuickSelectionList(pathlist);
            destination.setFile(LinkTreeUtils.getRawDownloadDirectory(pkg));
        }

        if (!checksum.hasFocus()) {
            //
            // if (dl.getMD5Hash() != null) return "[MD5] " + dl.getMD5Hash();
            if (!StringUtils.isEmpty(link.getDownloadLink().getSha1Hash())) {
                checksum.setText("[SHA1] " + link.getDownloadLink().getSha1Hash());
            } else if (!StringUtils.isEmpty(link.getDownloadLink().getMD5Hash())) {
                checksum.setText("[MD5] " + link.getDownloadLink().getMD5Hash());
            } else {
                checksum.setText(null);
            }

        }
        if (!downloadpassword.hasFocus()) {
            downloadpassword.setText(link.getDownloadLink().getDownloadPassword());
        }
        if (!packagename.hasFocus()) {
            packagename.setList(PackageHistoryManager.getInstance().list(new PackageHistoryEntry(pkg.getName())));
            packagename.setSelectedItem(new PackageHistoryEntry(pkg.getName()));
        }
        if (pkg != currentPackage) {
            if (!password.hasFocus()) password.setText("");
        }
        priority.setSelectedItem(link.getPriority());

    }

    protected void updateDownloadFrom(final CrawledLink link) {
        boolean ld = setting;
        setting = true;
        try {

            DownloadLink dlLink = link.getDownloadLink();
            if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (dlLink.gotBrowserUrl()) {
                    downloadFrom.setText(dlLink.getBrowserUrl());
                } else {
                    downloadFrom.setText("*******************************");
                }

            } else {
                downloadFrom.setText(dlLink.getBrowserUrl());
            }
            downloadFrom.repaint();
        } finally {
            setting = ld;
        }

    }

    protected void updateArchiveInEDT(final Archive archive) {
        currentArchive = archive;
        if (!password.hasFocus()) {
            if (currentArchive.getSettings().getPasswords() == null || currentArchive.getSettings().getPasswords().size() == 0) {
                password.setText(null);
            } else if (currentArchive.getSettings().getPasswords().size() == 1) {

                password.setText(currentArchive.getSettings().getPasswords().iterator().next());
            } else {
                password.setText(JSonStorage.toString(currentArchive.getSettings().getPasswords()));
            }
        }
        autoExtract.setSelectedItem(currentArchive.getSettings().getAutoExtract());
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                update();

            }

        };
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_filename(), CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadfrom(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadpassword(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_comment(), CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_archivepassword(), CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }
}
