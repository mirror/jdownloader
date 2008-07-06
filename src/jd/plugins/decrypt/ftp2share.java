package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ftp2share extends PluginForDecrypt {
    static private final String host = "ftp2share Decrypter";
    private String version = "1.0.0.0";
    private static final Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/file/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public ftp2share() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url;
                RequestInfo requestInfo;

                if (cryptedLink.matches(patternSupported_Folder.pattern())) {
                    if (!cryptedLink.contains("?system")) cryptedLink=cryptedLink+"?system=*";
                    url = new URL(cryptedLink);
                    requestInfo = HTTP.getRequest(url);
                    ArrayList<String> links = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE), 1);
                    for (int i = 0; i < links.size(); i++) {
                        String link = JDUtilities.Base64Decode(JDUtilities.filterString(links.get(i), "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                        decryptedLinks.add(this.createDownloadlink(link));
                    }
                } else if (cryptedLink.matches(patternSupported_File.pattern())) {
                    url = new URL(cryptedLink);
                    requestInfo = HTTP.getRequest(url);
                    Form[] forms= requestInfo.getForms();
                    if (forms.length>1) requestInfo = forms[1].getRequestInfo();
                    ArrayList<String> links = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE), 1);
                    for (int i = 0; i < links.size(); i++) {
                        String link = JDUtilities.Base64Decode(JDUtilities.filterString(links.get(i), "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                        decryptedLinks.add(this.createDownloadlink(link));
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public String getCoder() {
        return "JD-Team,Scikes";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "ftp2share Decrypter";
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
    public boolean doBotCheck(File file) {
        return false;
    }
}
