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

public class Xailicom extends PluginForDecrypt {

    static private String host = "xaili.com";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?xaili\\.com/\\?site=protect\\&id=[0-9]+", Pattern.CASE_INSENSITIVE);

    public Xailicom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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

    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo reqinfo = HTTP.getRequest(url);

                String links[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("onClick='popuptt\\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE)).getMatches(1);
                progress.setRange(links.length);
                for (int i = 0; i < links.length; i++) {
                    reqinfo = HTTP.getRequest(new URL("http://www.xaili.com/include/get.php?link=" + links[i]));
                    String link = new Regex(reqinfo.getHtmlCode(), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    if (link != null) {
                        link = JDUtilities.htmlDecode(link);
                        decryptedLinks.add(this.createDownloadlink(link));
                    }
                    progress.increase(1);
                }

                //step.setParameter(decryptedLinks);
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