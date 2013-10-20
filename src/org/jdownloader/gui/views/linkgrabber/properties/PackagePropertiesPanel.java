package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtCheckBox;
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
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.DownloadPath;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.PackageHistoryEntry;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.updatev2.gui.LAFOptions;

public class PackagePropertiesPanel extends MigPanel {

    private PathChooser                         destination;
    private SearchComboBox<PackageHistoryEntry> packagename;
    private LinkgrabberSettings                 config;
    private ExtTextField                        comment;

    private ExtTextField                        password;
    private JComboBox                           priority;
    private ExtTextField                        downloadPassword;
    private ExtCheckBox                         extractToggle;

    public PackagePropertiesPanel() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
        config = JsonConfig.create(LinkgrabberSettings.class);
        destination = new PathChooser("ADDLinks", true) {
            protected void onChanged(ExtTextField txt2) {

            }

            @Override
            public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
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
        ArrayList<PackageHistoryEntry> packageHistory = config.getPackageNameHistory();
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
        comment.setBorder(BorderFactory.createCompoundBorder(comment.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));
        destination.setQuickSelectionList(DownloadPath.loadList(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));

        String latest = config.getLatestDownloadDestinationFolder();
        if (latest == null || !config.isUseLastDownloadDestinationAsDefault()) {
            destination.setFile(new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder()));

        } else {
            destination.setFile(new File(latest));

        }

        password = new ExtTextField();
        password.setHelpText(_GUI._.AddLinksDialog_createExtracOptionsPanel_password());
        password.setBorder(BorderFactory.createCompoundBorder(password.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

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
        downloadPassword.setBorder(BorderFactory.createCompoundBorder(downloadPassword.getBorder(), BorderFactory.createEmptyBorder(2, 6, 1, 6)));

        extractToggle = new ExtCheckBox();

        extractToggle.setSelected(config.isAutoExtractionEnabled());
        // extractToggle.setBorderPainted(false);

        extractToggle.setToolTipText(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_tooltip());
        int height = Math.max(24, (int) (comment.getPreferredSize().height * 0.9));

        MigPanel p = this;
        p.setLayout(new MigLayout("ins 0 0 0 0,wrap 3", "[][grow,fill]2[]", "2[grow," + height + "!]2[grow," + height + "!]2[grow," + height + "!]2[grow," + height + "!]2[grow," + height + "!]0"));
        p.add(createIconLabel("save", _GUI._.propertiespanel_downloadpath(), _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny center,alignx right,height " + height + "!");

        p.add(destination.getDestination(), "height " + height + "!");
        p.add(destination.getButton(), "sg right,height " + height + "! ");

        p.add(createIconLabel("package_open", _GUI._.propertiespanel_packagename(), _GUI._.AddLinksDialog_layoutDialogContent_package_tt()), "aligny center,alignx right,height " + height + "!");
        p.add(packagename, "spanx,height " + height + "!");
        p.add(createIconLabel("document", _GUI._.propertiespanel_comment(), _GUI._.AddLinksDialog_layoutDialogContent_comment_tt()), "alignx right,aligny center,height " + height + "!");
        p.add(comment, "spanx,height " + height + "!");

        p.add(createIconLabel("archivepassword", _GUI._.propertiespanel_archivepassword(), _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "aligny center,alignx right,height " + height + "!");

        p.add(password, "pushx,growx,height " + height + "!");
        MigPanel subpanel = new MigPanel("ins 0", "[grow,fill][]", "[" + height + "!,grow]");
        subpanel.setOpaque(false);
        p.add(subpanel, "sg right,height " + height + "!");
        JLabel lbl;
        subpanel.add(lbl = new JLabel(_GUI._.AddLinksDialog_layoutDialogContent_autoextract_lbl()));
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        subpanel.add(extractToggle, "aligny center,height " + height + "!");

        p.add(createIconLabel("downloadpassword", _GUI._.propertiespanel_downloadpassword(), _GUI._.AddLinksDialog_layoutDialogContent_downloadpassword_tt()), "alignx right,aligny center,height " + height + "!");

        p.add(downloadPassword, "height " + height + "!");
        p.add(priority, "sg right,height " + height + "!");

        // this.getDialog().setLocation(new Point((int) (screenSize.getWidth() -
        // this.getDialog().getWidth()) / 2, (int) (screenSize.getHeight() -
        // this.getDialog().getHeight()) / 2));

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

    public void update(final CrawledPackage pkg) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                comment.setText(pkg.getComment());
                destination.setPath(pkg.getDownloadFolder());
                packagename.setText(pkg.getName());
            }
        };
    }

}
