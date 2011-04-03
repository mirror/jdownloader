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
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.DownloadSettings;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class InstallerDialog extends AbstractDialog<Object> {

    private static final long serialVersionUID = 1869417100230097511L;

    public static boolean showDialog(final File dlFolder) {
        final InstallerDialog dialog = new InstallerDialog(dlFolder);
        try {
            Dialog.getInstance().showDialog(dialog);
            return JsonConfig.create(DownloadSettings.class).getDefaultDownloadFolder() != null;

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String     language = null;
    private final File dlFolder;

    private BrowseFile browseFile;

    private InstallerDialog(final File dlFolder) {
        super(UserIO.NO_ICON, JDL.L("installer.gui.title", "JDownloader Installation"), null, null, null);

        final String countryCode = JDL.getCountryCodeByIP();
        final String languageCode = countryCode != null ? countryCode.toLowerCase(Locale.getDefault()) : null;
        if (languageCode != null) {
            for (final JDLocale id : JDL.getLocaleIDs()) {
                if (id.getCountryCode() != null && id.getCountryCode().equalsIgnoreCase(languageCode)) {
                    this.language = languageCode;
                    break;
                }
            }
            if (this.language == null) {
                for (final JDLocale id : JDL.getLocaleIDs()) {
                    if (id.getLanguageCode().equalsIgnoreCase(languageCode)) {
                        this.language = languageCode;
                        break;
                    }
                }
            }
        }
        if (this.language == null) {
            this.language = "en";
        }

        if (dlFolder != null) {
            this.dlFolder = dlFolder;
        } else {
            if (CrossSystem.isMac()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (CrossSystem.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (CrossSystem.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Download");
            } else {
                this.dlFolder = JDUtilities.getResourceFile("downloads");
            }
        }

    }

    @Override
    protected Object createReturnValue() {
        return this.getReturnmask();
    }

    @Override
    public JComponent layoutDialogContent() {
        final JDLocale sel = SubConfiguration.getConfig(JDL.CONFIG).getGenericProperty(JDL.LOCALE_PARAM_ID, JDL.getInstance(this.language));
        JDL.setLocale(sel);

        this.browseFile = new BrowseFile();
        this.browseFile.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        this.browseFile.setCurrentPath(this.dlFolder);

        final JList list = new JList(JDL.getLocaleIDs().toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(sel, true);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                JDL.setConfigLocale((JDLocale) list.getSelectedValue());
                JDL.setLocale(JDL.getConfigLocale());

                InstallerDialog.this.dispose();
                InstallerDialog.showDialog(InstallerDialog.this.browseFile.getCurrentPath());
            }

        });

        final ConfigContainer container = new ConfigContainer();
        container.setGroup(new ConfigGroup(JDL.L("gui.config.gui.language", "Language"), "gui.splash.languages"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(list), "growx,pushx"));
        container.setGroup(new ConfigGroup(JDL.L("gui.config.general.downloaddirectory", "Download directory"), "gui.images.userhome"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, this.browseFile, "growx,pushx"));

        final JLabel lbl = new JLabel(JDL.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        if (CrossSystem.getID() == CrossSystem.OS_WINDOWS_VISTA || CrossSystem.getID() == CrossSystem.OS_WINDOWS_7) {
            final String dir = JDUtilities.getResourceFile("downloads").getParent().substring(3).toLowerCase();

            if (!JDUtilities.getResourceFile("uninstall.exe").exists() && (dir.startsWith("programme\\") || dir.startsWith("program files\\"))) {
                lbl.setText(JDL.LF("installer.vistaDir.warning", "Warning! JD is installed in %s. This causes errors.", JDUtilities.getResourceFile("downloads").getParent()));
                lbl.setForeground(Color.RED);
            }
            if (!JDUtilities.getResourceFile("Updater.jar").canWrite()) {
                lbl.setText(JDL.LF("installer.nowriteDir.warning", "Warning! JD cannot write to %s. Check rights!", JDUtilities.getResourceFile("downloads").getParent()));
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
    protected void setReturnmask(final boolean b) {
        super.setReturnmask(b);

        if (b) {

            JsonConfig.create(DownloadSettings.class).setDefaultDownloadFolder(browseFile.getText());
        }
    }
}
