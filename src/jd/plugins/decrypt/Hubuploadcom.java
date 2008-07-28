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

public class Hubuploadcom extends PluginForDecrypt {

    static private String host = "hubupload.com";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?hubupload\\.com/files/[a-zA-Z0-9]+/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);

    public Hubuploadcom() {
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
                String Cookie = reqinfo.getCookie();
                String links[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<form action=\"(.*?)\"><input type=\"submit\" class=\"dlbutton\"", Pattern.CASE_INSENSITIVE)).getMatches(1);

                progress.setRange(links.length);
                for (int i = 0; i < links.length; i++) {
                    reqinfo = HTTP.getRequest(new URL(links[i]), Cookie, cryptedLink, false);
                    String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), Pattern.compile("<iframe src=\"(.*?)\" id=\"hub\"", Pattern.CASE_INSENSITIVE)).getFirstMatch());
                    if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                    progress.increase(1);
                }
                // Decrypt abschliessen
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