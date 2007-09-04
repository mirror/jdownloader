package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.event.PluginEvent;

public class Safelink extends PluginForDecrypt {
    private String host = "safelink.in";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://safelink.in/rc-[/|.|a-zA-Z0-9|_|-]*");
    private Pattern Formp = Pattern.compile(
            ".FORM ACTION..http:..rapidshare.com.files.(.*)..METHOD..post. ID..postit",
            Pattern.CASE_INSENSITIVE);

    public Safelink() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override public String getCoder() { return "DwD aka James"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "Salfelink-1.0.0.";    }
    @Override public String getPluginName() {return host;    }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
    @Override public boolean isClipboardEnabled() { return true; }
    @Override public PluginStep getNextStep(Object parameter) { return currentStep; }
    @Override
    public Vector<String> decryptLink(String cryptedLink) {
        Vector<String> decryptedLinks = new Vector<String>();
        try {
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, 1));
            String html=getRequest(new URL(cryptedLink)).getHtmlCode();
            String link="http://rapidshare.com/files/" + getFirstMatch(html, Formp, 1);
            decryptedLinks .add(link);
            firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
            logger.info(decryptedLinks.size() + " download decrypted");
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
            currentStep = null;
        } 
        catch (MalformedURLException e) { e.printStackTrace(); } 
        catch (IOException e) { e.printStackTrace(); }

        return decryptedLinks;
    }
    @Override
    public boolean doBotCheck(File file) {        
        return false;
    }
}
