package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.wizardry7.view.DefaultWizardPage;

public class DownloadFolder extends DefaultWizardPage {

    private static final long serialVersionUID = 300768092465899023L;
    private static final DownloadFolder INSTANCE = new DownloadFolder();

    public static DownloadFolder getInstance() {
        return INSTANCE;
    }

    private ConfigEntriesPanel cpanel;

    private DownloadFolder() {
        super();
    }

    @Override
    protected Component createBody() {
        initComponents();

        int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));

        // Geht jetzt. Das ganze ging nur nicht, weil das configpanel intern auf
        // den Controller zugreifen will, der noch nicht initialisiert war.
        panel.add(cpanel);

        return panel;
    }

    @Override
    public void enterWizardPageAfterForward() {
    }

    // Validation ##########################################################

    @Override
    public String forwardValidation() {
        if (true) {
            return "";
        } else {
            return "Before you are allowed to continue you have to read and agree to our AGBs";
        }
    }

    @Override
    protected void initComponents() {
        ConfigContainer configContainer = new ConfigContainer(this, "Titel");
        ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")).setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
        configContainer.addEntry(ce);

        cpanel = new ConfigEntriesPanel(configContainer, "Select where filesdownloaded with JDownloader should be stored.");
        for (Component c : cpanel.getComponents()) {
            System.out.println(c);
        }
        cpanel.setBorder(new LineBorder(Color.red, 1));
    }
}
