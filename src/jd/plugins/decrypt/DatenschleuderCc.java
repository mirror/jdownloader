package jd.plugins.decrypt;

import jd.plugins.DownloadLink;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

public class DatenschleuderCc extends PluginForDecrypt {
	
    final static String host = "datenschleuder.cc";
    private String version = "0.2.0";
    private Pattern patternSupported = getSupportPattern("http://[*]datenschleuder\\.cc/dl/(id|dir)/[0-9]+/[a-zA-Z0-9]+/[+]");
    
    private static final String[] USEARRAY = new String[] {
		"Rapidshare.com", "Netload.in", "Uploaded.to", "Datenklo.net", "Share.Gulli.com",
		"Archiv.to", "Bluehost.to", "Share-Online.biz", "Speedshare.org" };
    
    public DatenschleuderCc() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
    
    @Override
    public String getCoder() {
        return "eXecuTe";
    }
    
    @Override
    public String getHost() {
        return host;
    }
    
    @Override
    public String getPluginID() {
        return host + "-" + version;
    }
    
    @Override
    public String getPluginName() {
        return host;
    }
    
    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    private boolean getUseConfig(String link) {
    	
        if ( link == null ) return false;
        link = link.toLowerCase();
        
        for ( int i = 0; i < USEARRAY.length; i++ ) {
        	
            if ( link.contains(USEARRAY[i].toLowerCase()) ) {
                return getProperties().getBooleanProperty(USEARRAY[i], true);
            }
            
        }
        
        return false;
        
    }
    
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
    	
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
        	
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            
            try {
            	
                RequestInfo reqinfo = getRequest(new URL(parameter), null, null, true);
                Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(),
                		"<a href=\"http://www.datenschleuder.cc/redir.php?id=°\"");
    			
                progress.setRange( links.size() );
                
                for (int i = 0; i < links.size(); i++) {
                	
                	reqinfo = getRequest(new URL("http://www.datenschleuder.cc/redir.php?id="+links.get(i).get(0)));
                	String link = getBetween(reqinfo.getHtmlCode(), "<frame src=\"", "\" name=\"dl\">");
    				link = link.replace("http://anonym.to?", "");
                	progress.increase(1);
    				
                    if ( getUseConfig(link) ) decryptedLinks.add(createDownloadlink(link));
                    
                }
                
                logger.info(decryptedLinks.size() + " "
                		+ JDLocale.L("plugins.decrypt.general.downloadsDecrypted", "Downloads entschlüsselt"));
                step.setParameter(decryptedLinks);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        return null;
        
    }
    
    private void setConfigEelements() {
    	
        ConfigEntry cfg;
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
        		JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster Auswahl")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        
        for ( int i = 0; i < USEARRAY.length; i++ ) {
        	
            config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
            		getProperties(),USEARRAY[i], USEARRAY[i]));
            cfg.setDefaultValue(true);
            
        }
        
    }
    
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
}