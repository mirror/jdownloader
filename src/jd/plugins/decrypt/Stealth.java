package jd.plugins.decrypt;

import java.io.File;
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
 * http://stealth.to/?id=13wz0z8lds3nun4dihetpsqgzte4t2
 * 
 * http://stealth.to/?id=ol0fhnjxpogioavfnmj3aub03s10nt
 * 
 * @author astaldo
 * 
 */
public class Stealth extends PluginForDecrypt {
    static private final String   host                 = "Stealth.to";

    private String  version              = "1.0.0.1";

    private String  addressForPopupPost  = "http://stealth.to/get1.php?id=%s&h=%s";

    private Pattern patternSupported     = getSupportPattern("http://stealth.to/\\?id=[+]");

    /**
     * Dieses Pattern erkennt einen Parameter für eine Downloadadress
     * 
     * popup *\( *\"([0-9]*)\" *, *\"([0-9]*)\" *\)
     */
    private Pattern patternLinkParameter = Pattern.compile("popup *\\( *\\\"([0-9]*)\\\" *, *\\\"([0-9]*)\\\" *\\)");

    public Stealth() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "Astaldo";
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
    public String getHost() {
        return host;
    }


    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "STEALTH-1.0.0.";
    }


 

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String> decryptedLinks = new Vector<String>();
             // Zählen aller verfügbaren Treffer
                try {
                    URL url = new URL(cryptedLink);
                    RequestInfo requestInfo = getRequest(url, null, null, false);
                    int countHits = countOccurences(requestInfo.getHtmlCode(), patternLinkParameter);

                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, countHits));

                    // Hier werden alle verschlüsselten Links behandelt

                    Matcher matcher = patternLinkParameter.matcher(requestInfo.getHtmlCode());
                    int position = 0;
                    while (matcher.find(position)) {
                        position = matcher.start() + matcher.group().length();
                        String address = String.format(addressForPopupPost, new Object[] { matcher.group(1), matcher.group(2) });
                        requestInfo = postRequestWithoutHtmlCode((new URL(address)), null, null, null, false);
                        if (requestInfo != null) {
                            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                            decryptedLinks.add(requestInfo.getLocation());
                        }
                    }
                    logger.info(decryptedLinks.size() + " downloads decrypted");
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                    currentStep = null;
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                step.setParameter(decryptedLinks);
                break;

        }
        return null;

    }
}
