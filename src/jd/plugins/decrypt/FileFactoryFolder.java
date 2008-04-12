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

// http://www.filefactory.com//f/ef45b5179409a229/

public class FileFactoryFolder extends PluginForDecrypt {

    final static String host             = "filefactory.com";
    final static String name             = "filefactory.com Folder";
    private String      version          = "0.1.0";

    private Pattern     patternSupported = getSupportPattern(
    		"http://[*]filefactory\\.com(/|//)f/[a-zA-Z0-9]+");
    
    public FileFactoryFolder() {
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
        return "filefactory.com/f-"+version;
    }

    @Override
    public String getPluginName() {
        return name;
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
    			
    			ArrayList<ArrayList<String>> ids = getAllSimpleMatches(reqinfo.getHtmlCode(), "href=\"http://www.filefactory.com/file/°\"");
    			progress.setRange(ids.size());
    			
    			for ( int i=0; i<ids.size(); i++ ) {
    				
    				decryptedLinks.add(this.createDownloadlink("http://www.filefactory.com/file/"+ids.get(i).get(0)));
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