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
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

public class InstallerDialog extends AbstractDialog {

    public static boolean showDialog() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                new InstallerDialog();
                return null;
            }

        }.waitForEDT();

        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null) != null;
    }

    private static final long serialVersionUID = 1869417100230097511L;

    private final String languageCode;
    private String dlFolder = null;

    private AddonConfig config;

    private InstallerDialog() {
        super(UserIO.NO_COUNTDOWN, JDL.L("installer.gui.title", "JDownloader Installation"), null, null, null);

        String countryCode = JDL.getCountryCodeByIP();
        languageCode = (countryCode != null) ? countryCode.toLowerCase(Locale.getDefault()) : null;

        init();
    }

    @Override
    public JComponent contentInit() {
        String def = null;
        if (languageCode != null) {
            for (JDLocale id : JDL.getLocaleIDs()) {
                if (id.getCountryCode() != null && id.getCountryCode().equalsIgnoreCase(languageCode)) {
                    def = languageCode;
                    break;
                }
            }
            if (def == null) {
                for (JDLocale id : JDL.getLocaleIDs()) {
                    if (id.getLanguageCode().equalsIgnoreCase(languageCode)) {
                        def = languageCode;
                        break;
                    }
                }
            }
        }
        if (def == null) {
            def = "en";
        }
        final JDLocale sel = SubConfiguration.getConfig(JDL.CONFIG).getGenericProperty(JDL.LOCALE_PARAM_ID, JDL.getInstance(def));

        JDL.setLocale(sel);

        final ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, "");
        if (dlFolder != null) {
            ce.setDefaultValue(dlFolder);
        } else {
            if (OSDetector.isMac()) {
                ce.setDefaultValue(new File(System.getProperty("user.home") + "/Downloads").getAbsolutePath());
            } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                ce.setDefaultValue(new File(System.getProperty("user.home") + "/Downloads").getAbsolutePath());
            } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                ce.setDefaultValue(new File(System.getProperty("user.home") + "/Download").getAbsolutePath());
            } else {
                ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
            }
        }

        final JList list = new JList(JDL.getLocaleIDs().toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(sel, true);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                JDL.setConfigLocale((JDLocale) list.getSelectedValue());
                JDL.setLocale(JDL.getConfigLocale());
                SubConfiguration.getConfig(JDL.CONFIG).save();
                dlFolder = ce.getGuiListener().getText().toString();

                dispose();
                InstallerDialog.showDialog();
            }

        });

        ConfigContainer container = new ConfigContainer();
        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.language", "Language"), "gui.splash.languages"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(list), "growx,pushx"));
        container.setGroup(new ConfigGroup(JDL.L("gui.config.general.downloaddirectory", "Download directory"), "gui.images.userhome"));
        container.addEntry(ce);

        final JLabel lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getStyle() ^ Font.BOLD));
        lbl.setHorizontalAlignment(JLabel.CENTER);

        if (OSDetector.getOSID() == OSDetector.OS_WINDOWS_VISTA || OSDetector.getOSID() == OSDetector.OS_WINDOWS_7) {
            String dir = JDUtilities.getResourceFile("downloads").getParent().substring(3).toLowerCase();

            if (!JDUtilities.getResourceFile("uninstall.exe").exists() && (dir.startsWith("programme\\") || dir.startsWith("program files\\"))) {
                lbl.setText(JDL.LF("installer.vistaDir.warning", "Warning! JD is installed in %s. This causes errors.", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
                lbl.setBackground(Color.RED);
            }
            if (!JDUtilities.getResourceFile("tools/tinyupdate.jar").canWrite()) {
                lbl.setText(JDL.LF("installer.nowriteDir.warning", "Warning! JD cannot write to %s. Check rights!", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
                lbl.setBackground(Color.RED);
            }
        }

        config = AddonConfig.getInstance(container, "", true);

        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]25[grow,fill]push[]"));
        panel.add(lbl, "pushx");
        panel.add(config.getPanel());
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

        if (b) config.save();
    }

}
