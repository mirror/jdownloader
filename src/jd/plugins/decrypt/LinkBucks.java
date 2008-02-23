package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;

public class LinkBucks extends PluginForDecrypt {

    static private String host = "linkbucks.com";

    private String version = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://.*?linkbucks.com/link/.*", Pattern.CASE_INSENSITIVE);

    public LinkBucks() {
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

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                progress.setRange(1);

                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                String link = new Regexp(reqinfo.getHtmlCode(), "<a href=\"(.*?)\" id=\"[^\"]*?\">Click here to Continue</a></p>").getFirstMatch();
                progress.increase(1);
                decryptedLinks.add(this.createDownloadlink(link));

                // Decrypt abschliessen

                step.setParameter(decryptedLinks);
            } catch (IOException e) {
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