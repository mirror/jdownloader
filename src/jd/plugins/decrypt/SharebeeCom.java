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


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class SharebeeCom extends PluginForDecrypt {

    final static String host             = "sharebee.com";

    private String      version          = "1.0.0.0";
    //sharebee.com/67e489cf
    private Pattern     patternSupported = getSupportPattern("http://[*]sharebee\\.com/[a-zA-Z0-9]{8}");

    public SharebeeCom() {
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
        return "Sharebee.com-1.0.0.";
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

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			
    			// Anzahl der Links zählen
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_ZSHARE",false)) {
    				count++;
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BADONGO",false)) {
    				count++;
    			}
    			progress.setRange( count);
    			
    			// Links auslesen und umdrehen
    			ArrayList<ArrayList<String>> g = getAllSimpleMatches(reqinfo.getHtmlCode(), "u=°\');return false;\">°</a>");
    			
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    			progress.increase(1);
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Rapidshare"))
                            decryptedLinks.add(this.createDownloadlink(g.get(i).get(0)));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_ZSHARE",false)) {
    			progress.increase(1);
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("zSHARE"))
                            decryptedLinks.add(this.createDownloadlink(g.get(i).get(0)));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_MEGAUPLOAD",true)) {
    			progress.increase(1);
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Megaupload"))
                            decryptedLinks.add(this.createDownloadlink(g.get(i).get(0)));
                    }
    			}
    			
    			if((Boolean) this.getProperties().getProperty("USE_BADONGO",false)) {
    			progress.increase(1);
                    for( int i=0; i<g.size();i++){
                        if(g.get(i).get(1).equalsIgnoreCase("Badongo"))
                            decryptedLinks.add(this.createDownloadlink(g.get(i).get(0)));
                    }
    			}
    			    			
    			// Decrypt abschliessen
    			
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_ZSHARE", "zShare.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MEGAUPLOAD", "Megaupload.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_BADONGO", "Badongo.com"));
        cfg.setDefaultValue(false);
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}