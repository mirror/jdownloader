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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class NixPopOrg extends PluginForDecrypt {

    static private String host             = "nix-pop.org";
    
    static private String        version          = "1.0.1";
    
    static private final Pattern patternSupported = getSupportPattern("http://[*]nix-pop\\.org/html/main/(show|showvid|showspec)\\.php\\?id=[0-9]+");

    public NixPopOrg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = HTTP.getRequest(url);

    			String links[] = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "copy&paste</legend>", "</fieldset>").trim().split("<br />");
    			
    			progress.setRange(links.length);
    			this.default_password.add(SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<p><strong>Passwort</strong>:", "<").trim());
    			
    			// Link der Liste hinzufügen
    			for(int i=0; i<links.length; i++) {
    				decryptedLinks.add(this.createDownloadlink(links[i].trim()));
    				progress.increase(1);
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

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
}