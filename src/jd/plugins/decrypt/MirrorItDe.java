//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

//http://www.mirrorit.de/?id=1f430272cb94fd0e
//http://www.mirrorit.de/?id=6fb1b96f995b09
//http://www.mirrorit.de/?id=9b7eee7fd2d98971
//http://www.mirrorit.de/?id=ec37d6f8038c168

public class MirrorItDe extends PluginForDecrypt {
	
    final 	static 			String	host                = "mirrorit.de";
    private 				String	version             = "1.1.1";
    static 	private final 	Pattern	patternSupported	= Pattern.compile("http://.*?mirrorit\\.de/\\?id\\=[a-zA-Z0-9]{16}", Pattern.CASE_INSENSITIVE);
    static 	private final 	String 	CRYPTLINK			= "launchDownloadURL(\'°\', \'°\')";
    
    public MirrorItDe() {
    	
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
        
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host+"-"+version;
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
    
    private static final String[] HOSTERS = new String[] {
		"SimpleUpload.net", "DatenKlo.net", "Sharebase.de", "MegaUpload.com", "Netload.in",
		"FileFactory.com", "Rapidshare.com", "Uploaded.to", "CoCoshare.cc", "Sendspace.com",
		"Load.to", "Archiv.to", "Files.to"};
    
    private boolean getHosterUsed(String link) {
    	
        if ( link == null ) return false;
        link = link.toLowerCase();
        
        for ( int i = 0; i < HOSTERS.length; i++ ) {
        	
            if ( link.contains(HOSTERS[i].toLowerCase()) ) {
                return getProperties().getBooleanProperty(HOSTERS[i], true);
            }
            
        }
        
        return false;
        
    }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            
    		Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		
    		try {
    			
    			RequestInfo reqinfo = getRequest(new URL(parameter));
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), CRYPTLINK);
    			progress.setRange(links.size());
    			
    			for ( int i=0; i<links.size(); i++ ) {
    				
	    			reqinfo = getRequest(new URL("http://www.mirrorit.de/Out?id="
	    					+ URLDecoder.decode(links.get(i).get(0), "UTF-8") + "&num=" + links.get(i).get(1)));
	    			String link = reqinfo.getLocation();
	    			reqinfo = getRequest(new URL(link));
	    			link = reqinfo.getLocation();
	    			
	    			if ( getHosterUsed(link) ) {
	    				
	    				decryptedLinks.add(this.createDownloadlink(link));
	    				progress.increase(1);
	    				
    				}
    				
    			}
    			
    			step.setParameter(decryptedLinks);
    			
    		} catch(IOException e) {
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
        
        for ( int i = 0; i < HOSTERS.length; i++ ) {
        	
            config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
            		getProperties(),HOSTERS[i], HOSTERS[i]));
            cfg.setDefaultValue(true);
            
        }
        
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
}