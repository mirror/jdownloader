package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class DownloadsPesArenacom extends PluginForDecrypt {

    static private String host = "downloads.pes-arena.com";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported = Pattern.compile("http://downloads\\.pes-arena\\.com/\\?id=(\\d+)", Pattern.CASE_INSENSITIVE);

    public DownloadsPesArenacom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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
        return host + "-" + version;
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

    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                String id = new Regex(cryptedLink, patternSupported).getFirstMatch();
                if (id != null) {
                    URL url = new URL("http://downloads.pes-arena.com/content.php?id=" + id);
                    RequestInfo reqinfo = HTTP.getRequest(url);
                    String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), "<iframe src=\"(.*?)\"").getFirstMatch());
                    decryptedLinks.add(this.createDownloadlink(link));
                }
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
