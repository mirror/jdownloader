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

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Pattern;


public class DreiDlAm extends PluginForDecrypt {

    final static String host             = "3dl.am";

    private String      version          = "0.6.1";

    private Pattern     patternSupported = getSupportPattern(
    	 "(http://[*]3dl\\.am/link/[a-zA-Z0-9]+)" +
    	// ohne abschliessendes "/" gehts nicht (auch im Browser)!
    	"|(http://[*]3dl\\.am/download/start/[0-9]+/)" +
    	"|(http://[*]3dl\\.am/download/[0-9]+/[+].html)");
    
    public DreiDlAm() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "jD-Team|b0ffed";
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
    			
    			if ( parameter.indexOf("3dl.am/download/start/") != -1 ) {
    				ArrayList<String> links = decryptFromStart(parameter);
    				progress.setRange(links.size());
    				String link = new String();
    				
    				for ( int i=0; i<links.size(); i++ ) {
        				progress.increase(1);
        				link = decryptFromLink(links.get(i));
        				decryptedLinks.add(this.createDownloadlink(link));
        			}
    				
        			step.setParameter(decryptedLinks);
        			
    			} else if ( parameter.indexOf("3dl.am/link/") != -1 ) {

    				progress.setRange(1);
    				
    				String link = decryptFromLink(parameter);
    				decryptedLinks.add(this.createDownloadlink(link));
    				
        			progress.increase(1);
        			step.setParameter(decryptedLinks);
    				
    			} else if ( parameter.indexOf("3dl.am/download/") != -1 ) {
    				
    				String link1 = decryptFromDownload(parameter);
    				ArrayList<String> links = decryptFromStart(link1);
    				progress.setRange(links.size());
    				String link2 = new String();
    				
    				for ( int i=0; i<links.size(); i++ ) {
        				progress.increase(1);
        				link2 = decryptFromLink(links.get(i));
        				decryptedLinks.add(this.createDownloadlink(link2));
        			}
    				
        			step.setParameter(decryptedLinks);
    			}
    	}
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
    private String decryptFromDownload(String parameter) {
    	String link = new String();
    	
    	try {
    		parameter.replace("&quot;", "\"");
    		
    		RequestInfo request = getRequest(new URL(parameter));
			String layer = getBetween(request.getHtmlCode(),
				"<form action=\"http://3dl.am/download/start/", "/\"");
			link = "http://3dl.am/download/start/"+layer+"/";
			
			// passwort auslesen
			if ( request.getHtmlCode().indexOf(
				"<b>Passwort:</b></td><td><input type='text' value='") != -1 ) {
				
				String password = getBetween(request.getHtmlCode(),
					"<b>Passwort:</b></td><td><input type='text' value='", "'");
				
				if ( !password.contains("kein") && !password.contains("kein P") ) default_password.add(password);
				
			}
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	
    	return link;
    	
    }
    
    private ArrayList<String> decryptFromStart(String parameter) {
    	ArrayList<ArrayList<String>> links = new ArrayList<ArrayList<String>>();
    	ArrayList<String> linksReturn = new ArrayList<String>();
    	
    	try {
    		RequestInfo request = getRequest(new URL(parameter));
    		links = getAllSimpleMatches(request.getHtmlCode(),
				"value='http://3dl.am/link/°/'");
    		
    		for(int i=0; i<links.size(); i++) {
				linksReturn.add("http://3dl.am/link/"+links.get(i).get(0)+"/");
			}
			
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	
    	return linksReturn;
    }
    
    private String decryptFromLink(String parameter) {
		String link = new String();
		
    	try {
    		RequestInfo request = getRequest(new URL(parameter));
			String layer = getBetween(request.getHtmlCode(),
				"<frame src=\"", "\" width=\"100%\"");
			link = layer;
			
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	return link;
    }
}