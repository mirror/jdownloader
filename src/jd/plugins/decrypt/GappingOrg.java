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

public class GappingOrg extends PluginForDecrypt {
    final static String host             = "gapping.org";
    private String      version          = "0.1.0";
    private Pattern     patternSupported = getSupportPattern(
    	 "(http://gapping\\.org/index\\.php\\?folderid=[0-9]+)"+
    	 "|(http://gapping\\.org/file\\.php\\?id=[+])");
    
    public GappingOrg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "jD-Team";
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
    			
    			if ( parameter.indexOf("index.php") != -1 ) {
    			
    				RequestInfo request = getRequest(new URL(parameter));
    				ArrayList<ArrayList<String>> ids = getAllSimpleMatches(
    						request.getHtmlCode(), "href=\"http://gapping.org/file.php?id=°\" >");
    				
    				progress.setRange(ids.size());
    				
    				for ( int i=0; i<ids.size(); i++ ) {
    					
    					request = getRequest(new URL("http://gapping.org/decry.php?fileid="+ids.get(i).get(0)));
    					String link = getBetween(request.getHtmlCode(),"src=\"","\"");
    					decryptedLinks.add(this.createDownloadlink(link));
    					progress.increase(1);
    					
	    			}
    				
    			} else if ( parameter.indexOf("file.php") != -1 ) {
    				
    				parameter = parameter.replace("file.php?id=","decry.php?fileid=");
    				RequestInfo request = getRequest(new URL(parameter));
    				String link = getBetween(request.getHtmlCode(),"src=\"","\"");
    				progress.setRange(1);
    				decryptedLinks.add(this.createDownloadlink(link));
    				progress.increase(1);
    				
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