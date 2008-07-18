package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigPanelDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.wizardry7.view.DefaultWizardPage;


public class DownloadFolder extends DefaultWizardPage {
	
	private static final DownloadFolder INSTANCE = new DownloadFolder();
	
	public static DownloadFolder getInstance() {
		return INSTANCE;
	}
	
	private ConfigEntriesPanel cpanel;
	
	private DownloadFolder() {
		super();
	}
	
	protected void initComponents() {
//	    ConfigContainer configContainer = new ConfigContainer(this, "Titel");
//        ConfigEntry ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")).setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
//        configContainer.addEntry(ce);
//
//        cpanel = new ConfigEntriesPanel(configContainer, "Select where files downloaded with JDownloader should be stored.");
	}
	
	protected Component createBody() {
		initComponents();

		int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n ,n));
        panel.setBorder(new EmptyBorder(n,n,n,n));
        
        // Geht nicht
        panel.add(cpanel);
        // Geht auch nicht
        panel.add(new ConfigPanelDownload(JDUtilities.getConfiguration(), SimpleGUI.CURRENTGUI));
        
		return panel;
	}
	
	
	// Validation ##########################################################

	public String forwardValidation() {
		if (true) {
			return "";
		}
		else return "Before you are allowed to continue you have to read and agree to our AGBs";
	}
	
	
	public void enterWizardPageAfterForward() {
	}
}





