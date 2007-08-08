package jd.plugins.decrypt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
/**
 * http://stealth.to/?id=7oj9t3vb0u316a8sjd66b43w1q16ku
 * 
 * @author a
 *
 */
public class Stealth extends PluginForDecrypt{
    private String  host    = "Stealth.to";
    private String  version = "1.0.0.0";
    private String addressForPopupPost="http://stealth.to/get1.php?id=%s&h=%s";
    private Pattern patternSupported = Pattern.compile("http://stealth\\.to/\\?id=[^\\s\"]*");
    /**
     * Dieses Pattern erkennt einen Parameter für eine Downloadadress
     * 
     * popup *\( *\"([0-9]*)\" *, *\"([0-9]*)\" *\)
     */
    private Pattern patternLinkParameter = Pattern.compile("popup *\\( *\\\"([0-9]*)\\\" *, *\\\"([0-9]*)\\\" *\\)");
    
    public Stealth(){ 
        super();    
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
    @Override public String getCoder()                { return "Astaldo";        }
    @Override public String getPluginName()           { return host;             }
    @Override public Pattern getSupportedLinks()      { return patternSupported; }
    @Override public String getHost()                 { return host;             }
    @Override public boolean isClipboardEnabled()     { return true;             }
    @Override public String getVersion()              { return version;          }

    @Override 
    public Vector<String> decryptLinks(Vector<String> cryptedLinks )     {
        Vector<String> decryptedLinks = new Vector<String>();

        int countHits=0;
        //Zählen aller verfügbaren Treffer
        for(int i=0;i<cryptedLinks.size();i++){
            try {
                URL url = new URL(cryptedLinks.elementAt(i));
                RequestInfo requestInfo = getRequest(url);
                countHits += countOccurences(requestInfo.getHtmlCode(), patternLinkParameter);
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX,countHits));
        
        //Hier werden alle verschlüsselten Links behandelt
        for(int i=0;i<cryptedLinks.size();i++){
            try {
                URL url = new URL(cryptedLinks.elementAt(i));
                RequestInfo requestInfo = getRequest(url, null, null, false);
                Matcher matcher = patternLinkParameter.matcher(requestInfo.getHtmlCode());
                int position =0;
                while(matcher.find(position)){
                    position = matcher.start()+matcher.group().length();
                    String address = String.format(addressForPopupPost, new Object[]{matcher.group(1),matcher.group(2)});
                    requestInfo = postRequest(new URL(address), null);
                    if(requestInfo != null){
                        firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE,null));
                        decryptedLinks.add(requestInfo.getLocation());
                    }
                }
                firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH,null));
                currentStep = null;
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        return decryptedLinks;
    }
    @Override
    public PluginStep getNextStep(Object parameter) {
        return currentStep;
    }    
    
}
