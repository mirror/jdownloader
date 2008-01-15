package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Filer extends PluginForDecrypt {

    static private String        host             = "filer.net";

    private String               version          = "0.1";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?filer.net/folder/(.*)", Pattern.CASE_INSENSITIVE);

    static private final Pattern INFO             = Pattern.compile("(?s)<td><a href=\"\\/get\\/.*?.html\">(.*?)</a></td>", Pattern.CASE_INSENSITIVE);

    public Filer() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "G4E";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Filer-0.1";
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

                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                Vector<Vector<String>> matches = getAllSimpleMatches(reqinfo.getHtmlCode(), INFO);
                progress.setRange(matches.size());
                String link = getFirstMatch(parameter, patternSupported, 1);
                for (int i = 0; i < matches.size(); i++) {
                    decryptedLinks.add(this.createDownloadlink("http://www.filer.net/file" + i + "/" + link + "/filename/" + matches.get(i).get(0)));
                    progress.increase(1);
                }

                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
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