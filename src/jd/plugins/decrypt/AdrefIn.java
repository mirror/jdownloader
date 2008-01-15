package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;

public class AdrefIn extends PluginForDecrypt {

    final static String host = "adref.in";

    private String version = "2.0.0.0";

    private Pattern patternSupported = getSupportPattern("http://[+]adref.in/\\?[+]");

    public AdrefIn() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "Botzi|G4E v2";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "adRef.in-" + version;
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
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();

            progress.setRange(1);
            parameter=parameter.replaceFirst("http://.*?adref.in/\\?", "");
            if(!parameter.matches("^http://"))
                parameter="http://"+parameter;
            decryptedLinks.add(this.createDownloadlink(parameter));
            progress.increase(1);

            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}