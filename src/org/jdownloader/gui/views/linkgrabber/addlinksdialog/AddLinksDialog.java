package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;
import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;
import jd.parser.html.HTMLParser;

import org.appwork.app.gui.copycutpaste.CopyAction;
import org.appwork.app.gui.copycutpaste.CutAction;
import org.appwork.app.gui.copycutpaste.DeleteAction;
import org.appwork.app.gui.copycutpaste.PasteAction;
import org.appwork.app.gui.copycutpaste.SelectAction;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Lists;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class AddLinksDialog extends AbstractDialog<LinkCollectingJob> {

    private ExtTextArea                         input;

    private PathChooser                         destination;

    private SearchComboBox<PackageHistoryEntry> packagename;

    private JScrollPane                         sp;

    private LinkgrabberSettings                 config;

    private ExtTextField                        password;

    private ExtCheckBox                         extractToggle;

    private JButton                             confirmOptions;

    private boolean                             deepAnalyse   = false;

    private ArrayList<PackageHistoryEntry>      packageHistory;

    private JLabel                              errorLabel;

    private DelayedRunnable                     delayedValidate;
    private WindowListener                      listener      = null;

    private ExtTextField                        downloadPassword;

    private JComboBox                           priority;

    private HashSet<String>                     autoPasswords = new HashSet<String>();

    private ExtTextField                        comment;                               ;

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public AddLinksDialog() {
        super(UIOManager.BUTTONS_HIDE_OK, _GUI._.AddLinksDialog_AddLinksDialog_(), null, _GUI._.AddLinksDialog_AddLinksDialog_confirm(), null);
        config = JsonConfig.create(LinkgrabberSettings.class);
        delayedValidate = new DelayedRunnable(IOEQ.TIMINGQUEUE, 500l, 10000l) {

            @Override
            public void delayedrun() {
                validateForm();
            }

        };
        setLocator(new RememberRelativeDialogLocator("AddLinksDialog", JDGui.getInstance().getMainFrame()));
    }

    // @Override
    // public ModalityType getModalityType() {
    // return ModalityType.MODELESS;
    // }
    //
    // @Override
    // public Window getOwner() {
    //
    // return null;
    // }

    @Override
    protected DefaultButtonPanel getDefaultButtonPanel() {
        final DefaultButtonPanel ret = new DefaultButtonPanel("ins 0 0 0 0", "[grow,fill][]0[][]", "0[fill]0");

        confirmOptions = new JButton(new ConfirmOptionsAction(okButton, this)) {
            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 1, y, width + 1, height);
            }
        };

        // Set OK as defaultbutton
        this.getDialog().getRootPane().setDefaultButton(this.okButton);
        this.okButton.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                    final JButton defaultButton = (JButton) e.getComponent();
                    final JRootPane root = SwingUtilities.getRootPane(defaultButton);
                    if (root != null) {
                        root.setDefaultButton(defaultButton);
                    }
                }
            }
        });
        errorLabel = new JLabel();
        errorLabel.setForeground((LAFOptions.getInstance().getColorForErrorForeground()));
        errorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        ret.add(errorLabel, "alignx right");
        ret.add(this.okButton, "alignx right,sizegroup confirms,growx,pushx");
        ret.add(confirmOptions, "width 12!");
        return ret;
    }

    @Override
    protected LinkCollectingJob createReturnValue() {
        LinkCollectingJob ret = new LinkCollectingJob();
        ret.setText(input.getText());
        // if (destination.getSelectedIndex() > 0) {
        //
        ret.setOutputFolder(destination.getFile());
        // }
        ret.setDeepAnalyse(isDeepAnalyse());
        ret.setPackageName(packagename.getText());
        ret.setAutoExtract(this.extractToggle.isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        ret.setCustomComment(getComment());
        String passwordTxt = password.getText();
        if (!StringUtils.isEmpty(passwordTxt)) {
            /* avoid empty hashsets */
            HashSet<String> passwords = JSonStorage.restoreFromString(passwordTxt, new TypeRef<HashSet<String>>() {
            }, new HashSet<String>());
            if (passwords == null || passwords.size() == 0) {
                passwords = new HashSet<String>();
                passwords.add(password.getText());
            }
            ret.setExtractPasswords(passwords);
        }
        ret.setPriority((Priority) priority.getSelectedItem());
        ret.setDownloadPassword(downloadPassword.getText());

        if (!StringUtils.isEmpty(ret.getPackageName())) {
            boolean found = false;
            for (PackageHistoryEntry pe : packageHistory) {
                if (pe.getName().equalsIgnoreCase(ret.getPackageName())) {
                    pe.setTime(System.currentTimeMillis());
                    found = true;
                    break;
                }
            }
            if (!found) {
                packageHistory.add(new PackageHistoryEntry(ret.getPackageName()));
            }
            config.setPackageNameHistory(Lists.unique(packageHistory));
        }
        if (ret.getOutputFolder() != null) {

            DownloadPath.saveList(ret.getOutputFolder().getAbsolutePath());

            config.setLatestDownloadDestinationFolder(ret.getOutputFolder().getAbsolutePath());
        }

        return ret;

    }

    public void actionPerformed(final ActionEvent e) {
        config.setAddDialogHeight(getDialog().getHeight());
        config.setAddDialogWidth(getDialog().getWidth());
        super.actionPerformed(e);
    }

    public String getComment() {
        return new EDTHelper<String>() {

            @Override
            public String edtRun() {
                return comment.getText();
            }
        }.getReturnValue();
    }

    @Override
    public JComponent layoutDialogContent() {

        destination = new PathChooser("ADDLinks", true) {
            protected void onChanged(ExtTextField txt2) {
                delayedValidate.run();
            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, CutAction cutAction, CopyAction copyAction, PasteAction pasteAction, DeleteAction deleteAction, SelectAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
                menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));
                return menu;
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

        packagename = new SearchComboBox<PackageHistoryEntry>() {

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
        packageHistory = config.getPackageNameHistory();
        if (packageHistory == null) {
            packageHistory = new ArrayList<PackageHistoryEntry>();
        }
        for (Iterator<PackageHistoryEntry> it = packageHistory.iterator(); it.hasNext();) {
            PackageHistoryEntry next = it.next();
            if (next == null || StringUtils.isEmpty(next.getName())) {
                it.remove();
                continue;
            }
            if (packageHistory.size() > 25) {
                // if list is very long, remove entries older than 30 days
                if (System.currentTimeMillis() - next.getTime() > 60 * 60 * 24 * 30) {
                    it.remove();
                }
            }

        }
        Collections.sort(packageHistory, new Comparator<PackageHistoryEntry>() {

            public int compare(PackageHistoryEntry o1, PackageHistoryEntry o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        packagename.setList(packageHistory);
        packagename.setUnkownTextInputAllowed(true);
        packagename.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_packagename_help());
        packagename.setSelectedItem(null);

        comment = new ExtTextField();
        comment.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_comment_help());
        comment.setBorder(BorderFactory.createCompoundBorder(comment.getBorder(), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        destination.setQuickSelectionList(DownloadPath.loadList(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));

        String latest = config.getLatestDownloadDestinationFolder();
        if (latest == null || !config.isUseLastDownloadDestinationAsDefault()) {
            destination.setFile(new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));

        } else {
            destination.setFile(new File(latest));

        }
        input = new ExtTextArea() {
            @Override
            public void onChanged() {
                delayedValidate.run();
            }
        };
        // input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_input_help());
        sp = new JScrollPane(input);
        sp.setViewportBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        password = new ExtTextField();
        password.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        priority = new JComboBox(Priority.values());
        final ListCellRenderer org = priority.getRenderer();
        priority.setRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel r = (JLabel) org.getListCellRendererComponent(list, ((Priority) value)._(), index, isSelected, cellHasFocus);
                r.setIcon(((Priority) value).loadIcon(20));
                return r;
            }
        });
        priority.setSelectedItem(Priority.DEFAULT);
        downloadPassword = new ExtTextField();
        downloadPassword.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_downloadpassword());
        downloadPassword.setBorder(BorderFactory.createCompoundBorder(downloadPassword.getBorder(), BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        extractToggle = new ExtCheckBox();

        extractToggle.setSelected(config.isAutoExtractionEnabled());
        // extractToggle.setBorderPainted(false);

        extractToggle.setToolTipText(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_tooltip());
        int height = Math.max(24, (int) (comment.getPreferredSize().height * 0.9));
        MigPanel p = new MigPanel("ins 0 0 3 0,wrap 3", "[][grow,fill][]", "[fill,grow][grow," + height + "!][grow," + height + "!][grow," + height + "!][grow," + height + "!]");

        p.add(new JLabel(NewTheme.I().getIcon("linkgrabber", 32)), "aligny top,height 32!,width 32!");

        p.add(sp, "height 30:100:n,spanx");
        p.add(createIconLabel("save", _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny center,width 32!");

        p.add(destination, "height " + height + "!");
        p.add(destination.getButton(), "sg right");

        p.add(createIconLabel("package_open", _GUI._.AddLinksDialog_layoutDialogContent_package_tt()), "aligny center,width 32!");
        p.add(packagename, "spanx,height " + height + "!");
        p.add(createIconLabel("document", _GUI._.AddLinksDialog_layoutDialogContent_comment_tt()), "aligny center,width 32!");
        p.add(comment, "spanx,height " + height + "!");

        p.add(createIconLabel("archivepassword", _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,height " + height + "!,width 32!");

        p.add(password, "pushx,growx,height " + height + "!");
        MigPanel subpanel = new MigPanel("ins 0", "[grow,fill][]", "[" + height + "!,grow]");

        p.add(subpanel, "sg right");
        JLabel lbl;
        subpanel.add(lbl = new JLabel(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_lbl()));
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        subpanel.add(extractToggle, "aligny center");

        p.add(createIconLabel("downloadpassword", _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,width 32!");

        p.add(downloadPassword);
        p.add(priority, "sg right");

        if (Application.getJavaVersion() >= 17000000) {
            J17DragAndDropDelegater dnd = new J17DragAndDropDelegater(this, input);
            input.setTransferHandler(dnd);
        } else {
            DragAndDropDelegater dnd = new DragAndDropDelegater(this, input);
            input.setTransferHandler(dnd);
        }

        input.setEditable(false);
        this.getDialog().addWindowListener(listener = new WindowListener() {
            public void windowOpened(WindowEvent e) {
                new Thread() {

                    @Override
                    public void run() {
                        inform();
                        String newText = config.getPresetDebugLinks();
                        if (StringUtils.isEmpty(newText)) {
                            newText = ClipboardMonitoring.getINSTANCE().getCurrentContent();
                        }
                        if (config.isAddLinksPreParserEnabled()) {
                            if (!StringUtils.isEmpty(newText)) {
                                new EDTRunner() {
                                    @Override
                                    protected void runInEDT() {
                                        input.setText(_GUI._.AddLinksDialog_ParsingClipboard());
                                    };
                                };
                                parse(newText);
                                newText = (list(HTMLParser.getHttpLinks(newText)));
                            }
                        }
                        final String txt = newText;
                        if (!StringUtils.isEmpty(newText)) {
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    input.setText(txt);
                                    input.setEditable(true);
                                    sp.repaint();
                                    delayedValidate.run();
                                }
                            };
                        } else {
                            new EDTRunner() {
                                @Override
                                protected void runInEDT() {
                                    input.setText("");
                                    input.setEditable(true);
                                    sp.repaint();
                                };
                            };
                        }
                    }
                }.start();
                if (listener != null) getDialog().removeWindowListener(listener);
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }
        });

        // this.getDialog().setLocation(new Point((int) (screenSize.getWidth() -
        // this.getDialog().getWidth()) / 2, (int) (screenSize.getHeight() -
        // this.getDialog().getHeight()) / 2));

        return p;
    }

    public static String list(String[] links) {
        if (links == null || links.length == 0) { return ""; }
        final StringBuilder ret = new StringBuilder();

        for (final String element : links) {
            if (ret.length() > 0) ret.append("\r\n");
            ret.append(element.trim());
        }
        return ret.toString();
    }

    protected void validateForm() {
        if (input == null) return;
        final String[] links = jd.parser.html.HTMLParser.getHttpLinks(input.getText());
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                okButton.setEnabled(true);
                confirmOptions.setEnabled(true);
                errorLabel.setText("");
                if (links.length == 0) {
                    errorLabel.setText(_GUI._.AddLinksDialog_validateForm_input_missing());
                    input.setToolTipText(_GUI._.AddLinksDialog_validateForm_input_missing());
                    okButton.setEnabled(false);
                    confirmOptions.setEnabled(false);
                } else {
                    input.setToolTipText(null);
                }
                if (!validateFolder(destination.getFile().getAbsolutePath())) {
                    if (errorLabel.getText().length() == 0) errorLabel.setText(_GUI._.AddLinksDialog_validateForm_folder_invalid_missing());
                    okButton.setEnabled(false);
                    destination.setToolTipText(_GUI._.AddLinksDialog_validateForm_folder_invalid_missing());
                    confirmOptions.setEnabled(false);
                    destination.setForeground((LAFOptions.getInstance().getColorForErrorForeground()));
                } else {
                    destination.setToolTipText(null);
                    destination.setForeground(null);
                }
                if (okButton.isEnabled()) {
                    if (cancelButton.hasFocus()) okButton.requestFocus();
                }
            }
        };
    }

    private boolean validateFolder(String text) {
        if (text == null) return false;
        File file = new File(text);
        if (file.isDirectory() && file.exists()) return true;
        return file.getParentFile() != null && file.getParentFile().exists();
    }

    private void inform() {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        if (input.isShowing()) {
                            HelpDialog.show(Boolean.FALSE, Boolean.TRUE, new Point(input.getLocationOnScreen().x + input.getWidth() / 2, input.getLocationOnScreen().y + 10), null, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksDialog_AddLinksDialog_(), _GUI._.AddLinksDialog_layoutDialogContent_description(), NewTheme.I().getIcon("linkgrabber", 32));
                        }
                    }
                };
            }
        }.start();

    }

    public void pack() {
        this.getDialog().pack();
    }

    @Override
    protected int getPreferredWidth() {
        return config.getAddDialogWidth();
    }

    protected int getPreferredHeight() {
        return config.getAddDialogHeight();
    }

    protected boolean isResizable() {
        return true;
    }

    public static void main(String[] args) {
        LookAndFeelController.getInstance().setUIManager();
        AddLinksDialog d = new AddLinksDialog();
        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private Component createIconLabel(String iconKey, String tooltip) {
        JLabel ret = new JLabel(NewTheme.I().getIcon(iconKey, 24));
        ret.setToolTipText(tooltip);
        return ret;
    }

    public void parse(String txt) {

        autoPasswords.addAll(PasswordUtils.getPasswords(txt));

        if (autoPasswords.size() > 1) {
            password.setText(JSonStorage.toString(autoPasswords));
        } else if (autoPasswords.size() > 0) {
            password.setText(autoPasswords.toArray(new String[] {})[0]);
        }
    }

}
