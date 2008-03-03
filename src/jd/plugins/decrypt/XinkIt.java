package jd.plugins.decrypt;
  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class XinkIt extends PluginForDecrypt {

    final static String host             = "xink.it";

    private String      version          = "0.1.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]xink\\.it/f-[a-zA-Z0-9]+");
    
    public XinkIt() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
    
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
    	
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
    		
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		
    		try {
    			
    			RequestInfo reqinfo = getRequest(new URL(parameter));
    			File captchaFile = null;
    			String capTxt = "";
    			String session = "PHPSESSID=" + new Regexp(reqinfo.getHtmlCode(), "\\?PHPSESSID=(.*?)\"").getFirstMatch();
        		
                while ( true ) { // läuft bis kein Captcha mehr abgefragt wird
                	
                	if ( reqinfo.getHtmlCode().indexOf("captcha_send") != -1 ) {
                        
                        logger.info("Captcha Protected");
                        
                		if(captchaFile!=null && capTxt != null){
                			JDUtilities.appendInfoToFilename(this, captchaFile,capTxt, false);
                		}
                		
                		String captchaAdress = "http://xink.it/captcha-" +
                				getBetween(reqinfo.getHtmlCode(), "src=\"captcha-", "\"");
                		captchaAdress += "?"+session;
                		
                		captchaFile = getLocalCaptchaFile(this);
                		JDUtilities.download(captchaFile, captchaAdress);
                		
                		capTxt = JDUtilities.getCaptcha(this, "xink.it", captchaFile, false);
                		
                		String post = "captcha=" + capTxt.toUpperCase() + "&x=70&y=11&" + session;
                		
                		HashMap<String,String> requestHeaders = new HashMap<String,String>();
                		requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
                		
                		reqinfo = postRequest(new URL(parameter),
                				null,
                				parameter,
                				requestHeaders,
                				post,
                				true);
                        
    				} else {
    					
    					if (captchaFile!=null && capTxt != null) {
    						JDUtilities.appendInfoToFilename(this, captchaFile,capTxt, true);
    					}
    					
    					break;
    					
    				}
                		
                }
    			
    			Vector<Vector<String>> ids = getAllSimpleMatches(
    					reqinfo.getHtmlCode(), "startDownload('°');");
    			
    			progress.setRange(ids.size());
    			
    			for ( int i=0; i<ids.size(); i++ ) {
    				
    				reqinfo = getRequest(new URL("http://xink.it/encd_"+ids.get(i).get(0)));
    				decryptedLinks.add(this.createDownloadlink(XinkItDecodeLink(reqinfo.getHtmlCode())));
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
    
    /**
     * 
     * Nachbau der Javascript Entschlüsselung auf xink.it (u.a. http.xink.it/lcv1.js)
     * 
     * @param source codierte Zeichenkette
     * @return entschlüsselter Link
     * 
     */
    private String XinkItDecodeLink(String source) {
    	
    	// implementiert von js vorlage http.xink.it/lcv1.js
    	// l001011l10110101l11010101l101l01l( decodiert Base64
    	
    	String evalCode = JDUtilities.Base64Decode(source);
    	
    	String l010 = JDUtilities.Base64Decode(new Regexp(evalCode,
    			"l010 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getFirstMatch());
    	String gt = new Regexp(evalCode,
				"gt\\=\"(.*?)\";").getFirstMatch();
    	String l011 = JDUtilities.Base64Decode(new Regexp(evalCode,
				"l011 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getFirstMatch());
    	String l012 = JDUtilities.Base64Decode(gt);
    	
    	String r = l012;
        String ar = r;
        String re = "";
        
        for(int a = 2; a < r.length(); a = a+4) {
        	
        	String temp1 = "";
        	
        	int temp2 = a;
        	if ( temp2 > ar.length() ) temp2 = ar.length();
        	else if ( temp2 < 0 ) temp2 = 0;
        	
        	int temp3 = a+2;
        	if ( temp3 > ar.length() ) temp3 = ar.length();
        	
        	int temp4 = (a+2)+(ar.length()-a);
        	if ( temp4 > ar.length() ) temp4 = ar.length();
        	else if ( temp4 < 0 ) temp4 = temp3;
        	
        	
        	temp1 += ar.substring(0,temp2);
        	temp1 += ar.substring(temp3,temp4);
        	
        	ar = temp1;
        	
        }
        
        for ( int a = 0; a < ar.length(); a = a+2 ) {
        	
        	for ( int i = 0; i < l011.length(); i = i+2 ) {
            	
            	int temp5 = a+2;
            	if ( temp5 > ar.length() ) temp5 = ar.length();
            	
            	int temp6 = i+2;
            	if ( temp6 > l011.length() ) temp6 = l011.length();
            	
        		if ( ar.substring(a,temp5).equals( l011.substring(i,temp6) ) ) {

                	re += l010.substring( (int)Math.floor(i/2), ((int)Math.floor(i/2))+1 );
                	
        		}
        		
        	}
        	
        }
        
    	return re;
        
    }
    
}