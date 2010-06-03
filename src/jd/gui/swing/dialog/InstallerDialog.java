package jd.gui.swing.dialog;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

public class InstallerDialog extends AbstractDialog {
    public static void main(String[] args) {
        InstallerDialog.showDialog(null);
    }

    public static boolean showDialog(final File dlFolder) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                new InstallerDialog(dlFolder);
                return null;
            }

        }.waitForEDT();

        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null) != null;
    }

    private static final long serialVersionUID = 1869417100230097511L;

    private String language = null;
    private final File dlFolder;

    private BrowseFile browseFile;

    private InstallerDialog(File dlFolder) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L("installer.gui.title", "JDownloader Installation"), null, null, null);

        String countryCode = JDL.getCountryCodeByIP();
        String languageCode = (countryCode != null) ? countryCode.toLowerCase(Locale.getDefault()) : null;
        if (languageCode != null) {
            for (JDLocale id : JDL.getLocaleIDs()) {
                if (id.getCountryCode() != null && id.getCountryCode().equalsIgnoreCase(languageCode)) {
                    language = languageCode;
                    break;
                }
            }
            if (language == null) {
                for (JDLocale id : JDL.getLocaleIDs()) {
                    if (id.getLanguageCode().equalsIgnoreCase(languageCode)) {
                        language = languageCode;
                        break;
                    }
                }
            }
        }
        if (language == null) language = "en";

        if (dlFolder != null) {
            this.dlFolder = dlFolder;
        } else {
            if (OSDetector.isMac()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Download");
            } else {
                this.dlFolder = JDUtilities.getResourceFile("downloads");
            }
        }

        init();
    }

    @Override
    public JComponent contentInit() {
        final JDLocale sel = SubConfiguration.getConfig(JDL.CONFIG).getGenericProperty(JDL.LOCALE_PARAM_ID, JDL.getInstance(language));
        JDL.setLocale(sel);

        browseFile = new BrowseFile();
        browseFile.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        browseFile.setCurrentPath(dlFolder);

        final JList list = new JList(JDL.getLocaleIDs().toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(sel, true);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                JDL.setConfigLocale((JDLocale) list.getSelectedValue());
                JDL.setLocale(JDL.getConfigLocale());

                dispose();
                InstallerDialog.showDialog(browseFile.getCurrentPath());
            }

        });

        ConfigContainer container = new ConfigContainer();
        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.language", "Language"), "gui.splash.languages"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(list), "growx,pushx"));
        container.setGroup(new ConfigGroup(JDL.L("gui.config.general.downloaddirectory", "Download directory"), "gui.images.userhome"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, browseFile, "growx,pushx"));

        final JLabel lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getStyle() ^ Font.BOLD));
        lbl.setHorizontalAlignment(JLabel.CENTER);

        if (OSDetector.getOSID() == OSDetector.OS_WINDOWS_VISTA || OSDetector.getOSID() == OSDetector.OS_WINDOWS_7) {
            String dir = JDUtilities.getResourceFile("downloads").getParent().substring(3).toLowerCase();

            if (!JDUtilities.getResourceFile("uninstall.exe").exists() && (dir.startsWith("programme\\") || dir.startsWith("program files\\"))) {
                lbl.setText(JDL.LF("installer.vistaDir.warning", "Warning! JD is installed in %s. This causes errors.", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
            }
            if (!JDUtilities.getResourceFile("tools/tinyupdate.jar").canWrite()) {
                lbl.setText(JDL.LF("installer.nowriteDir.warning", "Warning! JD cannot write to %s. Check rights!", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
            }
        }

        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]25[grow,fill]push[]"));
        panel.add(lbl, "pushx");
        panel.add(AddonConfig.getInstance(container, "", true).getPanel());
        panel.add(new JSeparator(), "pushx");
        return panel;
    }

    @Override
    protected void packed() {
        this.setAlwaysOnTop(true);
    }

    @Override
    protected void setReturnValue(final boolean b) {
        super.setReturnValue(b);

        if (b) {
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, browseFile.getText());
            JDUtilities.getConfiguration().save();
        }
    }
}
