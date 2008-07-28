package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Myupcc extends PluginForDecrypt {

    static private final String host = "myup.cc";

    private String version = "1.0.0.0";    
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?myup\\.cc/link-[a-zA-Z0-9]+\\.html", Pattern.CASE_INSENSITIVE);

    public Myupcc() {
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
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo requestInfo = HTTP.getRequest(url);
                String link = requestInfo.getRegexp("<a href=\\\"(.*?)\\\" target").getFirstMatch();
                if (link != null) decryptedLinks.add(this.createDownloadlink(link));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
