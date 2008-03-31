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

public class RapidRace extends PluginForDecrypt {
    // http://www.rapidrace.org/rel.php?ID=1332
    static private final String  HOST             = "rapidrace.org";

    static private final String  CODER            = "TheBlindProphet";

    static private final String  VERSION          = "1.2";

    private Pattern              patternSupported = getSupportPattern("http://(www.)?rapidrace\\.org/rel\\.php\\?ID=[+]");

    private Vector<DownloadLink> decryptedLinks   = new Vector<DownloadLink>();

    public RapidRace() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    public String getCoder() {
        return CODER;
    }

    public String getHost() {
        return HOST;
    }

    public String getPluginID() {
        return HOST + " " + VERSION;
    }

    public String getPluginName() {
        return HOST;
    }

    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public String getVersion() {
        return VERSION;
    }

    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            try {
                URL url = new URL(parameter);
                String finalUrl = "";
                String quellcode = "";
                progress.setRange(1);
                RequestInfo reqinfo = getRequest(url, null, null, false);
                quellcode = reqinfo.getHtmlCode();
                while (quellcode.indexOf("http://www.rapidrace.org/load.php?ID") != -1) {
                    finalUrl = "";
                    quellcode = quellcode.substring(quellcode.indexOf("http://www.rapidrace.org/load.php?ID"));
                    String tmp = quellcode.substring(0, quellcode.indexOf("\""));
                    progress.increase(1);
                    reqinfo = getRequest(new URL(tmp), null, null, true);
                    tmp = reqinfo.getHtmlCode().substring(reqinfo.getHtmlCode().indexOf("document.write(fu('") + 19);
                    tmp = tmp.substring(0, tmp.indexOf("'"));
                    for (int i = 0; i < tmp.length(); i += 2) {
                        finalUrl = finalUrl + (char) (Integer.parseInt(tmp.substring(i, i + 2), 16) ^ (i / 2));
                    }
                    decryptedLinks.add(this.createDownloadlink(finalUrl));
                    quellcode = quellcode.substring(20);
                }
                step.setParameter(decryptedLinks);

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean doBotCheck(File file) {
        return false;
    }
}
