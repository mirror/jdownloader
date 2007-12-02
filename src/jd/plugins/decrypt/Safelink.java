package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;

public class Safelink extends PluginForDecrypt {
    static private final String host             = "safelink.in";

    private String              version          = "1.0.0.0";

    private Pattern             patternSupported = Pattern.compile("http://(safelink.in|85.17.177.195)/rc-[/|.|a-zA-Z0-9|_|-]*");

    private Pattern             Formp            = Pattern.compile(".FORM ACTION..http:..rapidshare.com.files.(.*)..METHOD..post. ID..postit", Pattern.CASE_INSENSITIVE);

    public Safelink() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        return "DwD aka James";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Salfelink-1.0.0.";
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
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String> decryptedLinks = new Vector<String>();
                try {
                    progress.setRange(1);
                    String html = getRequest(new URL(cryptedLink)).getHtmlCode();
                    String id = getFirstMatch(html, Formp, 1);
                    if (id != null) {
                        String link = "http://rapidshare.com/files/" + getFirstMatch(html, Formp, 1);
                        decryptedLinks.add(link);
                    }
                    progress.increase(1);
                    // veraltet: firePluginEvent(new PluginEvent(this,
                    // PluginEvent.PLUGIN_PROGRESS_FINISH, null));
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
