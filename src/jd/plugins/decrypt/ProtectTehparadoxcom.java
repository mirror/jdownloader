package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ProtectTehparadoxcom extends PluginForDecrypt {

    static private final String host = "Protect.Tehparadox.com";

    private String version = "1.0.0.0";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?protect\\.tehparadox\\.com\\/[a-zA-Z0-9]+\\!", Pattern.CASE_INSENSITIVE);

    public ProtectTehparadoxcom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
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

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ////if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                String downloadid = new Regex(url.getFile(), "/([a-zA-Z0-9]+)\\!").getFirstMatch();
                url = new URL("http://protect.tehparadox.com/getdata.php");
                RequestInfo requestInfo = HTTP.postRequest(url, null, cryptedLink, null, "id=" + downloadid, false);
                String downloadlink = SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<iframe name=\"ifram\" src=\"", "\"");
                decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(downloadlink)));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //step.setParameter(decryptedLinks);
        
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
