package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.gui.swing.laf.LookAndFeelController;
import jd.parser.html.HTMLParser;
import net.miginfocom.swing.MigLayout;

import org.appwork.app.gui.MigPanel;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.Application;
import org.appwork.utils.Lists;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class AddLinksDialog extends AbstractDialog<LinkCollectingJob> {

    private ExtTextArea                         input;

    private SearchComboBox<DownloadPath>        destination;
    private JButton                             browseButton;

    private SearchComboBox<PackageHistoryEntry> packagename;

    private JScrollPane                         sp;

    private LinkgrabberSettings                 config;

    private ExtTextField                        password;

    private JButton                             extractToggle;

    private JButton                             confirmOptions;

    private boolean                             deepAnalyse = false;

    private ArrayList<DownloadPath>             downloadDestinationHistory;

    private ArrayList<PackageHistoryEntry>      packageHistory;

    private JLabel                              errorLabel;

    private DelayedRunnable                     delayedValidate;
    private WindowListener                      listener    = null;

    public boolean isDeepAnalyse() {
        return deepAnalyse;
    }

    public void setDeepAnalyse(boolean deepAnalyse) {
        this.deepAnalyse = deepAnalyse;
    }

    public AddLinksDialog() {
        super(Dialog.BUTTONS_HIDE_OK, _GUI._.AddLinksDialog_AddLinksDialog_(), null, _GUI._.AddLinksDialog_AddLinksDialog_confirm(), null);
        config = JsonConfig.create(LinkgrabberSettings.class);
        delayedValidate = new DelayedRunnable(IOEQ.TIMINGQUEUE, 500l, 10000l) {

            @Override
            public void delayedrun() {
                validateForm();
            }

        };
    }

    @Override
    protected JPanel getDefaultButtonPanel() {
        final JPanel ret = new JPanel(new MigLayout("ins 0", "[grow,fill][]0[][]", "0[fill]0"));

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
        errorLabel.setForeground(new Color(LookAndFeelController.getInstance().getLAFOptions().getErrorForeground()));
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
        if (destination.getSelectedIndex() > 0) ret.setOutputFolder(new File(destination.getText()));
        ret.setDeepAnalyse(isDeepAnalyse());
        ret.setPackageName(packagename.getText());
        ret.setAutoExtract(config.isAutoExtractionEnabled());
        ret.setExtractPassword(password.getText());
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
            boolean found = false;
            for (DownloadPath pe : downloadDestinationHistory) {
                if (pe != null && pe.getPath().equalsIgnoreCase(ret.getOutputFolder().getAbsolutePath())) {
                    pe.setTime(System.currentTimeMillis());
                    found = true;
                    break;
                }
            }
            if (!found) {
                downloadDestinationHistory.add(new DownloadPath(ret.getOutputFolder().getAbsolutePath()));
            }

            config.setDownloadDestinationHistory(Lists.unique(downloadDestinationHistory));
            config.setLatestDownloadDestinationFolder(ret.getOutputFolder().getAbsolutePath());
        }
        config.setAddDialogHeight(getDialog().getHeight());
        config.setAddDialogWidth(getDialog().getWidth());
        return ret;

    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel p = new MigPanel("ins 4,wrap 2", "[][grow,fill]", "[fill,grow][][][]");

        destination = new SearchComboBox<DownloadPath>() {

            @Override
            protected Icon getIconForValue(DownloadPath value) {
                return null;
            }

            @Override
            protected String getTextForValue(DownloadPath value) {
                return value == null ? null : value.getPath();
            }

            @Override
            public void onChanged() {
                delayedValidate.run();
            }

        };
        destination.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_help_destination());
        destination.setUnkownTextInputAllowed(true);
        destination.setBadColor(null);
        destination.setSelectedItem(null);
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
        ArrayList<DownloadPath> history = config.getDownloadDestinationHistory();
        if (history == null) {
            history = new ArrayList<DownloadPath>();
        }
        history.add(0, new DownloadPath(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));
        downloadDestinationHistory = Lists.unique(history);
        for (Iterator<DownloadPath> it = downloadDestinationHistory.iterator(); it.hasNext();) {
            DownloadPath path = it.next();
            if (!validateFolder(path.getPath())) it.remove();
        }

        destination.setList(downloadDestinationHistory);
        String latest = config.getLatestDownloadDestinationFolder();
        if (latest == null || !config.isUseLastDownloadDestinationAsDefault()) {
            destination.setText(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        } else {
            destination.setText(latest);
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
        browseButton = new JButton(_GUI._.literally_browse());
        password = new ExtTextField();
        password.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        password.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                config.setAutoExtractionEnabled(true);
                updateExtractionOptions();
                password.requestFocus();
            }
        });
        extractToggle = new JButton();

        extractToggle.setBorderPainted(false);
        extractToggle.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                config.setAutoExtractionEnabled(!config.isAutoExtractionEnabled());
                updateExtractionOptions();
            }
        });
        p.add(new JLabel(NewTheme.I().getIcon("linkgrabber", 32)), "aligny top,height 32!,width 32!");

        p.add(sp, "height 30:100:n");
        p.add(createIconLabel("save", _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny top,height 32!,width 32!");
        p.add(destination, "split 2,pushx,growx,height 24!");
        p.add(browseButton, "height 24!");
        browseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {
                    File[] ret = Dialog.getInstance().showFileChooser("addlinksdialog", _GUI._.AddLinksDialog_actionPerformed_browse(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.OPEN_DIALOG, new File(destination.getText()));
                    destination.setText(ret[0].getAbsolutePath());
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                }

            }
        });
        p.add(createIconLabel("package_open", _GUI._.AddLinksDialog_layoutDialogContent_package_tt()), "aligny top,height 32!,width 32!");
        p.add(packagename, "pushx,growx,height 24!");
        p.add(extractToggle, "aligny top,height 32!,width 32!");

        p.add(password, "pushx,growx,height 24!");

        updateExtractionOptions();
        if (Application.getJavaVersion() >= 17000000) {
            J17DragAndDropDelegater dnd = new J17DragAndDropDelegater(input);
            input.setTransferHandler(dnd);
        } else {
            DragAndDropDelegater dnd = new DragAndDropDelegater(input);
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
                    input.setBadgeIcon(null);
                }
                if (!validateFolder(destination.getText())) {
                    if (errorLabel.getText().length() == 0) errorLabel.setText(_GUI._.AddLinksDialog_validateForm_folder_invalid_missing());

                    okButton.setEnabled(false);
                    destination.setToolTipText(_GUI._.AddLinksDialog_validateForm_folder_invalid_missing());
                    confirmOptions.setEnabled(false);
                    destination.setForeground(new Color(LookAndFeelController.getInstance().getLAFOptions().getErrorForeground()));
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

    private void updateExtractionOptions() {
        if (config.isAutoExtractionEnabled()) {
            password.setEnabled(true);
            extractToggle.setToolTipText(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_tooltip_enabled());
            extractToggle.setIcon(NewTheme.I().getCheckBoxImage("archive", true, 24));

        } else {
            extractToggle.setToolTipText(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_tooltip());
            extractToggle.setIcon(NewTheme.I().getCheckBoxImage("archive", false, 24));
            password.setEnabled(false);
        }
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
                            try {
                                SimpleTextBallon d = new SimpleTextBallon(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksDialog_AddLinksDialog_(), _GUI._.AddLinksDialog_layoutDialogContent_description(), NewTheme.I().getIcon("linkgrabber", 32)) {
                                    public boolean doExpandToBottom(boolean b) {
                                        return false;
                                    }

                                    public boolean doExpandToRight(boolean b) {

                                        return true;
                                    }
                                };

                                d.setDesiredLocation(new Point(input.getLocationOnScreen().x + input.getWidth() / 2, input.getLocationOnScreen().y + 10));

                                Dialog.getInstance().showDialog(d);
                            } catch (DialogClosedException e) {
                                e.printStackTrace();
                            } catch (DialogCanceledException e) {
                                e.printStackTrace();
                            } catch (OffScreenException e) {
                                e.printStackTrace();
                            }
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

}
