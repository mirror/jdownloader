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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.DownloadLink;

//http://save.raidrush.ws/?id=8b891e864bc42ffa7bfcdaf72503f2a0
//http://save.raidrush.ws/?id=e7ccb3ee67daff310402e5e629ab8a91
//http://save.raidrush.ws/?id=c17ce92bc6154713f66b151b8f55684

public class SaveRaidrushWs extends PluginForDecrypt {

    static private final String host             = "save.raidrush.ws";

    private String              version          = "1.1.0";
    private Pattern             patternSupported = getSupportPattern("http://[*]save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+");
    private Pattern             patternCount     = Pattern.compile("\',\'FREE\',\'");

    public SaveRaidrushWs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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
        return "Save.Raidrush.ws-1.0.0.";
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
    			
    			RequestInfo reqinfo = getRequest(new URL(parameter));

    			progress.setRange( countOccurences(reqinfo.getHtmlCode(), patternCount));
    			ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "get('°','FREE','°');");
    			
    			for(int i=0; i<links.size(); i++) {
    				
    				ArrayList<String> help = links.get(i);
    				reqinfo = getRequest(new URL("http://save.raidrush.ws/404.php.php?id=" + help.get(0) + "&key=" + help.get(1)));
    				progress.increase(1);
    				decryptedLinks.add(this.createDownloadlink("http://"+reqinfo.getHtmlCode().trim()));
    				
    			}
    			
    			step.setParameter(decryptedLinks);
    			
    		} catch(IOException e) {
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