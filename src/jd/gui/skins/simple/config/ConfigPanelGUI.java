//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Locale;

import javax.swing.UIManager;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class ConfigPanelGUI extends ConfigPanel {
  /**
	 * 
	 */
	private static final long serialVersionUID = 5474787504978441198L;
//  private Configuration configuration;
    private SubConfiguration guiConfig;
  //  private Vector<String> changer;
    /**
     * serialVersionUID
     */
 
  
    public ConfigPanelGUI(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        //this.configuration = configuration;
        this.guiConfig=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();

        guiConfig.save();
    }
    @SuppressWarnings("unchecked")
	@Override
    public void initPanel() {
        GUIConfigEntry gce;
        ConfigEntry ce;
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_LOCALE, JDLocale.getLocaleIDs().toArray(new String[]{}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault()));
        addGUIConfigEntry(gce);
       
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_THEME, JDTheme.getThemeIDs().toArray(new String[]{}), JDLocale.L("gui.config.gui.theme", "Theme")).setDefaultValue("default"));
        addGUIConfigEntry(gce);
        String[] plafs;

        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        plafs = new String[info.length];

        for (int i = 0; i < plafs.length; i++) {
            plafs[i] = info[i].getName();
        }
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_PLAF, plafs, JDLocale.L("gui.config.gui.plaf", "Style(benötigt JD-Neustart)")).setDefaultValue("Windows"));
        addGUIConfigEntry(gce);

       

        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, JDLocale.L("gui.config.gui.disabledialogs", "Bestätigungsdialoge abschalten")).setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(gce);
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_SHOW_SPLASH, JDLocale.L("gui.config.gui.showSplash", "Splashscreen beim starten zeigen")).setDefaultValue(true));
        addGUIConfigEntry(gce);

        Object[] BrowserArray = (Object[]) guiConfig.getProperty(SimpleGUI.PARAM_BROWSER_VARS, null);

        if (BrowserArray == null) {
            BrowserLauncher launcher;
            List ar = null;
            try {
                launcher = new BrowserLauncher();
                ar = launcher.getBrowserList();
            } catch (BrowserLaunchingInitializingException e) {
                e.printStackTrace();
            } catch (UnsupportedOperatingSystemException e) { 
                e.printStackTrace();
            }
            if (ar.size() < 2) {
                BrowserArray = new Object[]{"JavaBrowser"};
            } else {
                BrowserArray = new Object[ar.size() + 1];
                for (int i = 0; i < BrowserArray.length - 1; i++) {
                    BrowserArray[i] = ar.get(i);
                }
                BrowserArray[BrowserArray.length - 1] = "JavaBrowser";
            }
            guiConfig.setProperty(SimpleGUI.PARAM_BROWSER_VARS, BrowserArray);
            guiConfig.setProperty(SimpleGUI.PARAM_BROWSER, BrowserArray[0]);
            guiConfig.save();
        }

        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_BROWSER, BrowserArray, JDLocale.L("gui.config.gui.Browser", "Browser")).setDefaultValue(BrowserArray[0]).setExpertEntry(true));
        addGUIConfigEntry(gce);

        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, guiConfig, LinkGrabber.PROPERTY_AUTOPACKAGE_LIMIT, JDLocale.L("gui.config.gui.autopackageLimit", "Schwelle der Auto. Paketverwaltung."),0,100).setDefaultValue(98).setExpertEntry(true));
        addGUIConfigEntry(gce);
        gce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, LinkGrabber.PROPERTY_ONLINE_CHECK, JDLocale.L("gui.config.gui.linkgrabber.onlinecheck","Linkgrabber:Linkstatus überprüfen(Verfügbarkeit)")).setDefaultValue(true));
        addGUIConfigEntry(gce);
        gce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDLocale.L("gui.config.reloadContainer","Heruntergeladene Container einlesen")).setDefaultValue(true));
        addGUIConfigEntry(gce);
    
        gce= new GUIConfigEntry(ce= new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,  JDUtilities.getSubConfig("GUI"), Configuration.PARAM_SHOW_CONTAINER_ONLOAD_OVERVIEW, JDLocale.L("gui.config.showContainerOnLoadInfo","Detailierte Containerinformationen beim Öffnen anzeigen")).setDefaultValue(false).setInstantHelp(JDLocale.L("gui.config.showContainerOnLoadInfo.helpurl","http://jdownloader.ath.cx/page.php?id=102")));
     
        addGUIConfigEntry(gce);
        
        
        

        
        gce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, JDLocale.LOCALE_EDIT_MODE, JDLocale.L("gui.config.localeditmode","'Missing-Language-Entries' Datei anlegen")).setDefaultValue(true));
        addGUIConfigEntry(gce);
 
        
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return JDLocale.L("gui.config.gui.gui", "Benutzeroberfläche");
    }
}
