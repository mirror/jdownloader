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
    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_LOCALE, JDLocale.getLocaleIDs().toArray(new String[]{}), JDLocale.L("gui.config.gui.language", "Sprache")).setDefaultValue(Locale.getDefault()));
        addGUIConfigEntry(ce);
       
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_THEME, JDTheme.getThemeIDs().toArray(new String[]{}), JDLocale.L("gui.config.gui.theme", "Theme")).setDefaultValue("default"));
        addGUIConfigEntry(ce);
        String[] plafs;

        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        plafs = new String[info.length];

        for (int i = 0; i < plafs.length; i++) {
            plafs[i] = info[i].getName();
        }
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_PLAF, plafs, JDLocale.L("gui.config.gui.plaf", "Style(benötigt JD-Neustart)")).setDefaultValue("Windows"));
        addGUIConfigEntry(ce);

        guiConfig.setProperty(SimpleGUI.PARAM_LANG_EDITMODE,false);

//        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_LANG_EDITMODE, JDLocale.L("gui.config.gui.langeditMode", "Sprachdatei Editiermodus")).setDefaultValue(false).setExpertEntry(true));
//        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, JDLocale.L("gui.config.gui.disabledialogs", "Bestätigungsdialoge abschalten")).setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, SimpleGUI.PARAM_SHOW_SPLASH, JDLocale.L("gui.config.gui.showSplash", "Splashscreen beim starten zeigen")).setDefaultValue(true));
        addGUIConfigEntry(ce);

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

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, guiConfig, SimpleGUI.PARAM_BROWSER, BrowserArray, JDLocale.L("gui.config.gui.Browser", "Browser")).setDefaultValue(BrowserArray[0]).setExpertEntry(true));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, guiConfig, LinkGrabber.PROPERTY_AUTOPACKAGE_LIMIT, JDLocale.L("gui.config.gui.autoPackageLimit", "Schwelle der Auto. Paketverwaltung."),0,100).setDefaultValue(98).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, guiConfig, LinkGrabber.PROPERTY_ONLINE_CHECK, JDLocale.L("gui.config.gui.linkgrabber.onlinecheck","Linkgrabber:Linkstatus überprüfen(Verfügbarkeit)")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_RELOADCONTAINER, JDLocale.L("gui.config.reloadContainer","Heruntergeladene Container einlesen")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        
   
 
        
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
