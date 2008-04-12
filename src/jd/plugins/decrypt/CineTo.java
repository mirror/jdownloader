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
import java.util.Arrays;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.DownloadLink;

public class CineTo extends PluginForDecrypt {
    final static String host             = "cine.to";
    private String      version          = "1.2.0";
    private Pattern     patternSupported = getSupportPattern("http://[*]cine.to/index.php\\?do=(protect|show_download)\\&id=[a-zA-Z0-9]+");
    
    public CineTo() {
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
            ArrayList<ArrayList<String>> mirrors = new ArrayList<ArrayList<String>>();
        	
        	try {
        		
        		RequestInfo reqinfo = getRequest(new URL(parameter));
        		boolean direct = false;
        		
        		if ( parameter.contains("do=show_download") ) {
        			
        			mirrors = getAllSimpleMatches(reqinfo.getHtmlCode(),"href=\"index.php?do=protect&id=°\"");
        			
        			if ( reqinfo.getHtmlCode().contains("<strong>Passwort:</strong>") ) {
        				
        				String password = getBetween(reqinfo.getHtmlCode(),
        						"<td><strong>Passwort:</strong></td>\n                        <td style=\"color: red\">","</td>");
        				default_password.add(password);
                        
        			} else {
        				default_password.add("cine.to");
        			}
        			
        			
        		} else {
        			
        			ArrayList<String> temp = new ArrayList<String>();
        			temp.add(parameter);
            		mirrors.add(temp);
            		direct = true;
            		
        		}
        		
        		if (!direct) progress.setRange(mirrors.size());
        		
        		for ( int i=0; i<mirrors.size(); i++ ) {
        			
        			reqinfo = getRequest(new URL("http://cine.to/index.php?do=protect&id="+mirrors.get(i).get(0)));
    				logger.info(reqinfo.getLocation());
    				ArrayList<ArrayList<String>> captcha = getAllSimpleMatches(reqinfo.getHtmlCode(),"span class=\"°\"");
    				
    				String capText = "";
    				if ( captcha.size() == 80 ) {
    					
    					for ( int j=1; j<5; j++ ) {
    						capText = capText + extractCaptcha(captcha, j);
    					}
    					
    				}
    				
    				reqinfo = postRequest(new URL("http://cine.to/index.php?do=protect&id="+mirrors.get(i).get(0)), reqinfo.getCookie(), parameter, null,
    						"captcha=" + capText + "&submit=Senden", true);
                	
    				ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "window.open(\'°\'");
    				if (direct) progress.setRange(links.size());
    				
    				for ( int j=0; j<links.size(); j++ ) {
    					decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
    					if (direct) progress.increase(1);
    				}
            		
    				if (!direct) progress.increase(1);
    				
        		}
        		
				step.setParameter(decryptedLinks);
        		
			} catch(IOException e) {
				e.printStackTrace();
			}
            
    	}
    	
    	return null;
    	
    }
    
    private String extractCaptcha(ArrayList<ArrayList<String>> source, int captchanumber) {
    	
    	String[] erg = new String[15];
    	
    	erg[0] = source.get((captchanumber*4)-4).get(0);
    	erg[1] = source.get((captchanumber*4)-3).get(0);
    	erg[2] = source.get((captchanumber*4)-2).get(0);
    	
    	erg[3] = source.get((captchanumber*4)+12).get(0);
    	erg[4] = source.get((captchanumber*4)+13).get(0);
    	erg[5] = source.get((captchanumber*4)+14).get(0);
    	
    	erg[6] = source.get((captchanumber*4)+28).get(0);
    	erg[7] = source.get((captchanumber*4)+29).get(0);
    	erg[8] = source.get((captchanumber*4)+30).get(0);
    	
    	erg[9] = source.get((captchanumber*4)+44).get(0);
    	erg[10] = source.get((captchanumber*4)+45).get(0);
    	erg[11] = source.get((captchanumber*4)+46).get(0);
    	
    	erg[12] = source.get((captchanumber*4)+60).get(0);
    	erg[13] = source.get((captchanumber*4)+61).get(0);
    	erg[14] = source.get((captchanumber*4)+62).get(0);
    	
    	String[] wert0 = {"s", "s", "s", "s", "w", "s", "s", "w", "s", "s", "s", "w", "s", "s", "s"};
    	if(Arrays.equals(erg, wert0))
    		return "0";
    	
    	String[] wert1 = {"w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s", "w", "w", "s"};
    	if(Arrays.equals(erg, wert1))
    		return "1";
    	
    	String[] wert2 = {"s", "s", "s", "w", "w", "s", "w", "s", "s", "s", "w", "w", "s", "s", "s"};
    	if(Arrays.equals(erg, wert2))
    		return "2";
    	
    	String[] wert3 = {"s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "s", "s", "s"};
    	if(Arrays.equals(erg, wert3))
    		return "3";
    	
    	String[] wert4 = {"s", "w", "w", "s", "w", "s", "s", "s", "s", "w", "w", "s", "w", "w", "s"};
    	if(Arrays.equals(erg, wert4))
    		return "4";
    	
    	String[] wert5 = {"s", "s", "s", "s", "w", "w", "s", "s", "s", "w", "w", "s", "s", "s", "s"};
    	if(Arrays.equals(erg, wert5))
    		return "5";
    	
    	String[] wert6 = {"s", "s", "s", "s", "w", "w", "s", "s", "s", "s", "w", "s", "s", "s", "s"};
    	if(Arrays.equals(erg, wert6))
    		return "6";
    	
    	String[] wert7 = {"s", "s", "s", "w", "w", "s", "w", "s", "s", "w", "w", "s", "w", "w", "s"};
    	if(Arrays.equals(erg, wert7))
    		return "7";
    	
    	String[] wert8 = {"s", "s", "s", "s", "w", "s", "s", "s", "s", "s", "w", "s", "s", "s", "s"};
    	if(Arrays.equals(erg, wert8))
    		return "8";
    	
    	String[] wert9 = {"s", "s", "s", "s", "w", "s", "s", "s", "s", "w", "w", "s", "s", "s", "s"};
    	if(Arrays.equals(erg, wert9))
    		return "9";
    	
    	return "0";
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
}