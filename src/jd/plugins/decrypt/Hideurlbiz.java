package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Hideurlbiz  extends PluginForDecrypt {

    static private final String host = "hideurl.biz";

    private String version = "1.0.0.0";    
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?hideurl\\.biz/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public Hideurlbiz() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo requestInfo = HTTP.getRequest(url);
                String links[][]=new Regex(requestInfo.getHtmlCode(),Pattern.compile("value=\"(Download|Free Download)\" onclick=\"openlink\\('(.*?)','.*?'\\);\"",Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i=0;i<links.length;i++){
                    requestInfo=HTTP.getRequest(new URL(links[i][1]));
                    if (requestInfo.getLocation()!=null){
                        /*direkt aus dem locationheader*/
                        decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                    }else{
                        /*aus dem htmlcode den link finden*/
                        String link=new Regex(requestInfo.getHtmlCode(),Pattern.compile("action=\"(.*?)\"",Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        if (link!=null) decryptedLinks.add(this.createDownloadlink(link));
                    }
                }
                
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
