package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class AnimeLoadsorg extends PluginForDecrypt {

    static private String host = "anime-loads.org";

    private String version = "1.0.0.0";
    final static private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?anime-loads\\.org/Crypt-it/([^/]*)/[a-zA-Z0-9]+\\.html", Pattern.CASE_INSENSITIVE);

    public AnimeLoadsorg() {
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
        ////if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo reqinfo = HTTP.getRequest(url);
                String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), "src=\"(.*?)\"").getFirstMatch());
                if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                //step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}