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

public class URLCash extends PluginForDecrypt {

    static private String host = "urlcash.net";

    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://[a-zA-Z0-9\\-]{5,16}\\.(urlcash\\.net|urlcash\\.org|clb1\\.com|urlgalleries\\.com|celebclk\\.com|smilinglinks\\.com|peekatmygirlfriend\\.com|looble\\.net)", Pattern.CASE_INSENSITIVE);

    public URLCash() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "GforE";
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
                String link = new Regexp(reqinfo.getHtmlCode(), "<META HTTP-EQUIV=\"Refresh\" .*? URL=(.*?)\">").getFirstMatch();
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