package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForContainer;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Upload;

public class DLCConverter extends Interaction implements Serializable {

    private static final String NAME = JDLocale.L("interaction.dlcconverter.name", "DlContainer erstellen");
    private static final String UPLOADEDTO = "UPLOADEDTO";
    private static final String PASTEBINCOM = "PASTEBINCOM";
    private static final String FORMAT = "[url=%URL%][b]%CONTAINERNAME% @ %HOSTER%(%LINKNUM% urls)[/b][/URL]";
    private static final String RAPIDSHARECOM = "RAPIDSHARECOM";
    private static final String RAMZAL = "RAMZAL";
    private static final String UPLOADEDTOUSER = null;
    private static final String UPLOADEDTOPASS = null;

    public DLCConverter() {}

    @Override
    

    public boolean doInteraction(Object arg) {
       
   start();
  
      return true;
    }
 
    private Vector<DownloadLink> getContainerlinks(File file) {

        Vector<PluginForContainer> pluginsForContainer = JDUtilities.getPluginsForContainer();
        Vector<DownloadLink> downloadLinks = new Vector<DownloadLink>();
        PluginForContainer pContainer;
        ProgressController progress = new ProgressController("Containerloader", pluginsForContainer.size());
    

        for (int i = 0; i < pluginsForContainer.size(); i++) {

            pContainer = pluginsForContainer.get(i);       
            progress.setStatusText("Containerplugin: " + pContainer.getPluginName());
            if (pContainer.canHandle(file.getName())) {
             
                try {
                    pContainer = pContainer.getClass().newInstance();
                    progress.setSource(pContainer);
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pContainer));
                    pContainer.initContainer(file.getAbsolutePath());
                    Vector<DownloadLink> links = pContainer.getContainedDownloadlinks();
                    if (links == null || links.size() == 0) {
                        logger.severe("Container Decryption failed (1)");
                    }
                    else {
                        downloadLinks.addAll(links);
                    }
                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pContainer));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            progress.increase(1);
        }
        progress.setStatusText(downloadLinks.size() + " links found");
     
        progress.finalize();
        return downloadLinks;
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {
        JDController controller = JDUtilities.getController();
        DownloadLink link=JDUtilities.getController().getLastFinishedDownloadLink();
        if(link!=null||controller.isContainerFile(new File(link.getFileOutput()))){
            
            if(link.getFileOutput().toLowerCase().endsWith("dlc")&&!getBooleanProperty("DLC",false))return;
            if(link.getFileOutput().toLowerCase().endsWith("rsdf")&&!getBooleanProperty("RSDF",true))return;
            if(link.getFileOutput().toLowerCase().endsWith("ccf")&&!getBooleanProperty("CCF",true))return; 
            ProgressController progress = new ProgressController(JDLocale.L("interaction.dlcconverter.progress.0_title","DLC Converter"),7);
        
           String xml; 
            progress.setStatusText(JDLocale.L("interaction.dlcconverter.progress.1","DLC Converter: parse Container"));
          Vector<DownloadLink> links = getContainerlinks(new File(link.getFileOutput()));
          String ret="";
          progress.increase(1);
          String format;
          int ind=link.getName().lastIndexOf(".");
          String name=link.getName().substring(0,ind);
            for(int i=0; i<this.getIntegerProperty(UPLOADEDTO, 2);i++){
                progress.setStatusText(JDLocale.L("interaction.dlcconverter.progress.2_uploaded","DLC Converter: Upload, uploaded.to ")+(i+1));
                xml = controller.createDLCString(links);
                xml=controller.encryptDLC(xml);
                if(this.getStringProperty(UPLOADEDTOUSER, "").length()>0 &&this.getStringProperty(UPLOADEDTOPASS, "").length()>0){
                    format=getStringProperty("FORMAT",FORMAT);
                    File dest = JDUtilities.getResourceFile(link.getName().substring(0,ind)+".dlc");
                    JDUtilities.writeLocalFile(dest, xml);
                    format= format.replace("%URL%", Upload.toUploadedToPremium(dest, getStringProperty(UPLOADEDTOUSER, ""), getStringProperty(UPLOADEDTOPASS, "")));
                    dest.delete();
                    format= format.replace("%CONTAINERNAME%", name);
                    format=format.replace("%LINKNUM%", ""+links.size());
                    format= format.replace("%HOSTER%", "uploaded.to");
                    
                }else{
             
                
                format=getStringProperty("FORMAT",FORMAT);
                format= format.replace("%URL%", Upload.toUploadedTo(xml,link.getName().substring(0,ind)+".dlc"));
                format= format.replace("%CONTAINERNAME%", name);
                format=format.replace("%LINKNUM%", ""+links.size());
                format= format.replace("%HOSTER%", "uploaded.to");
              
                
                }
                
                ret+=format+"\r\n";
            }
            progress.increase(1);
            for(int i=0; i<this.getIntegerProperty(PASTEBINCOM, 1);i++){
                progress.setStatusText(JDLocale.L("interaction.dlcconverter.progress.3_pastebin","DLC Converter: Upload, pastebin.com ")+(i+1));
                
                xml = controller.createDLCString(links);
                xml=controller.encryptDLC(xml);
                format=getStringProperty("FORMAT",FORMAT);
                format=format.replace("%URL%", Upload.toPastebinCom("dlc://"+xml,link.getName().substring(0,ind)+".dlc"));
                format= format.replace("%CONTAINERNAME%", name);
                format=format.replace("%LINKNUM%", ""+links.size());
                format= format.replace("%HOSTER%", "pastebin.com");
                ret+=format+"\r\n";
        
            }
            progress.increase(1);
            for(int i=0; i<this.getIntegerProperty(RAPIDSHARECOM, 2);i++){
                progress.setStatusText(JDLocale.L("interaction.dlcconverter.progress.2_pastebin","DLC Converter: Upload, Rapidshare.com ")+(i+1));
                
                xml = controller.createDLCString(links);
                xml=controller.encryptDLC(xml);
               
                File dest = JDUtilities.getResourceFile(link.getName().substring(0,ind)+".dlc");
                JDUtilities.writeLocalFile(dest, xml);
                format=getStringProperty("FORMAT",FORMAT);
                format=format.replace("%URL%", Upload.toRapidshareCom(dest));
                dest.delete();
                format= format.replace("%CONTAINERNAME%", name);
                format=format.replace("%LINKNUM%", ""+links.size());
                format= format.replace("%HOSTER%", "pastebin.com");
                ret+=format+"\r\n";
        
            }
            progress.increase(1);
            for(int i=0; i<this.getIntegerProperty(RAMZAL, 2);i++){
                progress.setStatusText(JDLocale.L("interaction.dlcconverter.progress.4_pastebin","DLC Converter: Upload, ramzal.com ")+(i+1));
                
                xml = controller.createDLCString(links);
                xml=controller.encryptDLC(xml);
                File dest = JDUtilities.getResourceFile(link.getName().substring(0,ind)+".dlc");
                JDUtilities.writeLocalFile(dest, xml);
                format=getStringProperty("FORMAT",FORMAT);
                format=format.replace("%URL%", Upload.toRamzahlCom(dest));
                dest.delete();
                format= format.replace("%CONTAINERNAME%", name);
                format=format.replace("%LINKNUM%", ""+links.size());
                format= format.replace("%HOSTER%", "pastebin.com");
                ret+=format+"\r\n";
        
            }
            progress.increase(1);
            progress.finalize();
            if(getBooleanProperty("LOGGER",true)){
                logger.info(ret);
                
            }
            if(getBooleanProperty("WINDOW",true)){
               JDUtilities.getGUI().showTextAreaDialog("Mirrors: "+link, "", ret);
                
            }
            if(getStringProperty("TOFILE",null)!=null&&!new File(getStringProperty("TOFILE",null)).isDirectory()){
               String old = JDUtilities.getLocalFile(new File(getStringProperty("TOFILE",null)));
               new File(getStringProperty("TOFILE",null)).mkdirs();
               old+="\r\n-----------------------------------------\r\n";
               old+="Mirrors of "+link+":\r\n";
               old+=ret+"\r\n\r\n";
               new File(getStringProperty("TOFILE",null)).delete();
               JDUtilities.writeLocalFile( new File(getStringProperty("TOFILE",null)), old);
               
                 
             }
        }else{
            logger.severe("Kein letzter Downloadlink gefunden");
        }
       
    
   
        
    }

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    @Override
    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("interaction.dlcconverter.container","Diese Container werden eingelesen:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,this,"RSDF",  JDLocale.L("interaction.dlcconverter.rsdf","*.RSDF:")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,this,"CCF",  JDLocale.L("interaction.dlcconverter.rsdf","*.CCF:")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,this,"DLC",  JDLocale.L("interaction.dlcconverter.dlc","*.DLC:")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("interaction.dlcconverter.mirrors","Anzahl de Mirrors:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, UPLOADEDTO, JDLocale.L("interaction.dlcconverter.uploadedto","Uploaded.to:"),0,20).setDefaultValue(2));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,this,UPLOADEDTOUSER,  JDLocale.L("interaction.dlcconverter.uploadedto.user","Premium Benutzer:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,this,UPLOADEDTOPASS,  JDLocale.L("interaction.dlcconverter.uploadedto.pass","Premium Passwort:")));
        
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, PASTEBINCOM, JDLocale.L("interaction.dlcconverter.pastebincom","pastebin.com:"),0,20).setDefaultValue(1));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, RAPIDSHARECOM, JDLocale.L("interaction.dlcconverter.rapidshare","rapidshare.com:"),0,20).setDefaultValue(2));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, this, RAMZAL, JDLocale.L("interaction.dlcconverter.ramzal","ramzal.com:"),0,20).setDefaultValue(2));
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("interaction.dlcconverter.output","Ausgabe der Links:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE,this,"TOFILE",  JDLocale.L("interaction.dlcconverter.inFile","In Datei speichern:")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,this,"LOGGER",  JDLocale.L("interaction.dlcconverter.log","Im Log:")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,this,"WINDOW",  JDLocale.L("interaction.dlcconverter.window","Messagebox:")).setDefaultValue(true)); 
        
         config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,  JDLocale.L("interaction.dlcconverter.format","Ausgabe Parameter:")+ "%URL%, %CONTAINERNAME%, %HOSTER%, %LINKNUM%"));
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,this,"FORMAT",  JDLocale.L("interaction.dlcconverter.outputtype","Ausgabeformatierung:")).setDefaultValue(FORMAT));
        
        // int type, Property propertyInstance, String propertyName, Object[] list,
    // String label
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, this,
    // PARAM_LAST_OR_ALL,
    // OPTIONS,JDLocale.L("interaction.resetLink.whichLink","Welcher Link soll
    // zurückgesetzt werden?")).setDefaultValue(OPTIONS[1]));

    }

    @Override
    public void resetInteraction() {}
}
