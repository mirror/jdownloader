package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.Arrays;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class CineTo extends PluginForDecrypt {

    final static String host             = "cine.to";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]cine.to/protect.php\\?id=[+]");

      
    public CineTo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("cine.to");
    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Cine.to-1.0.0.";
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
    			
    			Vector<Vector<String>> captcha = getAllSimpleMatches(reqinfo.getHtmlCode(), "span class=\"°\"");
    			
    			String capText = "";
    			if(captcha.size() == 80) {
    				for(int i=1; i<5; i++) {
    					capText = capText + extractCaptcha(captcha, i);
    				}
    			}
    			reqinfo = postRequest(url, reqinfo.getCookie(), parameter, null, "captcha=" + capText + "&submit=Senden", true);
                
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "window.open(\'°\'");
    			
    			for(int i=0; i<links.size(); i++) {
    				decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
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
    
    private String extractCaptcha(Vector<Vector<String>> source, int captchanumber) {
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