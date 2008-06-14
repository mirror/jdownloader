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

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class NetfolderIn extends PluginForDecrypt {
    static private String host             = "netfolder.in";
    private String        version          = "1.0.0.0";
    //netfolder.in/folder.php?folder_id=b8b3b0b 
    static private final Pattern patternSupported = Pattern.compile("http://.*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}", Pattern.CASE_INSENSITIVE);

    public NetfolderIn() {
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
        return "Netfolder.in-1.0.0.";
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
    			String password = "";
    			
    			while (true) {
                 	int check = SimpleMatches.countOccurences(reqinfo.getHtmlCode(), Pattern.compile("input type=\"password\" name=\"password\""));
                 	
                 	if (check > 0) {
                 		password = JDUtilities.getController().getUiInterface().showUserInputDialog("Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:");
                 		
                 		if(password == null) {
                 			step.setParameter(decryptedLinks);
                 			return null;
                 		}
                 		
                 		reqinfo = HTTP.postRequest(url, "password=" + password + "&save=Absenden");
                 	} else {
                 		break;
                 	}
     			}
    			
    			ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"http://netload.in/°\"");
    			
    			progress.setRange( links.size());
    			
    			// Link der Liste hinzufügen
    			for(int i=0; i<links.size(); i++) {
    				decryptedLinks.add(this.createDownloadlink("http://netload.in/" + links.get(i).get(0)));
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