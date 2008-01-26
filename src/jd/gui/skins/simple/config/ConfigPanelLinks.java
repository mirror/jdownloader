package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;

public class ConfigPanelLinks extends ConfigPanel {
 /**
	 * 
	 */
	private static final long serialVersionUID = -7308048370179606684L;
//   private Configuration configuration;
    private SubConfiguration guiConfig;
   
    /**
     * serialVersionUID
     */
 
  
    public ConfigPanelLinks(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
       // this.configuration = configuration;
      
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();
if(guiConfig!=null)
        guiConfig.save();
    }
    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.links.message", "Hier finden Sie einige wichtige Links bezogen auf jDownloader")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.home", "Heimat von JDownloader"),"http://jdownloader.ath.cx"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.links.lblSupport", "Support")));
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.dlccreate", "Online DLContainer erstellen"),"http://jdownloader.ath.cx/dlc.php"));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.support", "Supportforum"),"http://jdownloadersupport.ath.cx"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.email", "Email an jDownloader"),"mailto:jdownloader@freenet.de"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.faq", "FAQ - häufige Fragen"),"http://www.the-lounge.org/viewforum.php?f=222"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.jddownload", "Download: jDownloader"),"http://jdownloaderinstall.ath.cx"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.jdtutorial", "Ausführliches -Erste Schritte- Tutorial"),"http://www.sb-innovation.de/f66/jd-java-downloader-tutorial-1911/"));
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.links.lblDev", "Entwickler")));
         addGUIConfigEntry(ce);
         
         ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.chat", "Entwicklerchat"),"http://jdown.cwsurf.de/index.php?id=2"));
         addGUIConfigEntry(ce);
         
         ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
         addGUIConfigEntry(ce);
         ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.links.lblTools", "Zusätzliche Tools")));
          addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("gui.config.links.unrar.info", "Achtung unrar/rar muss in version 3.7 oder höher installiert sein")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.unrar", "Unrar"),"http://www.rarlab.com/rar_add.htm"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.rar", "RaR"),"http://www.rarlab.com/download.htm"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.winne", "Wine HQ(Linux)"),"http://winehq.org/"));
        
        addGUIConfigEntry(ce);
ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.darwine", "Darwine(MAC)"),"http://darwine.sourceforge.net/"));
        
        addGUIConfigEntry(ce);
 ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LINK,  JDLocale.L("gui.config.general.link.wireshark", "Wireshark"),"http://www.wireshark.org"));
        
        addGUIConfigEntry(ce);
        

        
        
      
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
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
