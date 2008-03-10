package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class SeCurNet extends PluginForDecrypt {

    final 	static	String	HOST             	= "se-cur.net";
    private 		String	VERSION          	= "0.1.0";
    private 		String	CODER            	= "eXecuTe";
    private 		Pattern	SUPPORT_PATTERN		= getSupportPattern("http://[*]se-cur\\.net/q\\.php\\?d=[+]");
    
    private 		String  LINK_OUT_PATTERN	= "href=\"http://se-cur.net/out.php?d=°\"";
    private 		String  LINK_OUT_TEMPLATE	= "http://se-cur.net/out.php?d=";
    private 		String  FRAME	= "src=\"°\"";
    
    public SeCurNet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return SUPPORT_PATTERN;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            
    		Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            
    		try {

    			URL url = new URL(parameter);
    			RequestInfo requestInfo = getRequest(url);
    			Vector<Vector<String>> layerLinks = getAllSimpleMatches(requestInfo.getHtmlCode(), LINK_OUT_PATTERN);
    			progress.setRange(layerLinks.size());
    			
    			for ( int i = 0; i < layerLinks.size(); i++ ) {
    				
    				requestInfo = getRequest( new URL(LINK_OUT_TEMPLATE + layerLinks.get(i).get(0)) );
    				String link = getSimpleMatch(requestInfo.getHtmlCode(), FRAME, 0);
    				link = JDUtilities.htmlDecode(link);
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