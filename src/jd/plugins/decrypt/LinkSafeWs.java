package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class LinkSafeWs extends PluginForDecrypt {
	private static final String CODER			= "Bo0nZ";
	private static final String HOST			= "linksafe.ws";
	private static final String PLUGIN_NAME		= HOST;
	private static final String PLUGIN_VERSION	= "1.0.0.0";
	private static final String PLUGIN_ID		= PLUGIN_NAME + "-" + PLUGIN_VERSION;
	private static final Pattern PAT_SUPPORTED  = getSupportPattern("http://[*]linksafe.ws/files/[+]");
    
    /*
     * Suchmasken
     */
    private static final String FILES = "<input type='hidden' name='id' value='°' />°<input type='hidden' name='f' value='°' />";
    private static final String LINK = "<iframe frameborder=\"0\" height=\"100%\" width=\"100%\" src=\"http://anonym.us.to/?°\">";
   
    public LinkSafeWs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
    
    /*
     * Funktionen
     */
    @Override public String getCoder() { return CODER; }
    @Override public String getHost() { return HOST; }
    @Override public String getPluginID() { return PLUGIN_ID; }
    @Override public String getPluginName() { return HOST; }
    @Override public Pattern getSupportedLinks() { return PAT_SUPPORTED; }
    @Override public String getVersion() { return PLUGIN_VERSION; }
    
    @Override public PluginStep doStep(PluginStep step, String parameter) {
        if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
            try {
            	String strURL = parameter;
                URL url = new URL(strURL);
                RequestInfo reqinfo = getRequest(url); // Seite aufrufen
                
                // Im HTML-Code nach "files"/"Forms" suchen
                Vector<Vector<String>> files = getAllSimpleMatches(reqinfo.getHtmlCode(), FILES);
                firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, files.size()));
                
                for(int i=0; i<files.size(); i++) {
                    reqinfo = postRequest(new URL("http://www.linksafe.ws/go/"), reqinfo.getCookie(), strURL, null, "id=" + files.get(i).get(0) + "&f=" + files.get(i).get(2) + "&Download.x=5&Download.y=10&Download=Download", true);
                    
                    String newLink = getSimpleMatch(reqinfo.getHtmlCode(), LINK, 0);

                    decryptedLinks.add(newLink);
                    firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                }
                
                //Decrypt abschliessen
                firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    @Override public boolean doBotCheck(File file) {        
        return false;
    }

}
