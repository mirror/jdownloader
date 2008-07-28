package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UploadJockeycom extends PluginForDecrypt {

    static private final String HOST = "UploadJockey Decrypter";

    private String VERSION = "1.0.0";

    private String CODER = "JD-Team";
    
    static private final Pattern patternSupported_1 = Pattern.compile("http://[\\w\\.]*?uploadjockey\\.com/download/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_1.pattern(), Pattern.CASE_INSENSITIVE);

    public UploadJockeycom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            URL url;
            RequestInfo requestInfo;
            try {
                url=new URL(cryptedLink);
                requestInfo = HTTP.getRequest(url, null, url.toString(), false);                
                ArrayList<String> links = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"http\\:\\/\\/www\\.uploadjockey\\.com\\/redirect\\.php\\?url=([a-zA-Z0-9=]+)\"", Pattern.CASE_INSENSITIVE), 1);
                for (int i = 0; i < links.size(); i++) {
                    decryptedLinks.add(this.createDownloadlink(JDUtilities.Base64Decode(links.get(i))));
                }
                
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
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
