package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class FlyLoadnet extends PluginForDecrypt {
    static private final String host = "Flyloadnet Decrypter";
    private String version = "1.0.0.0";
    private static final Pattern patternSupported_Download = Pattern.compile("http://[\\w\\.]*?flyload\\.net/download\\.php\\?view\\.(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Request = Pattern.compile("http://[\\w\\.]*?flyload\\.net/request_window\\.php\\?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Safe = Pattern.compile("http://[\\w\\.]*?flyload\\.net/safe\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_Safe.pattern() + "|" + patternSupported_Request.pattern() + "|" + patternSupported_Download.pattern(), Pattern.CASE_INSENSITIVE);

    public FlyLoadnet() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        // //if (step.getStep() == PluginStep.STEP_DECRYPT) {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo;
            if (new Regex(cryptedLink, patternSupported_Download).matches()) {
                String id = new Regex(cryptedLink, patternSupported_Download).getFirstMatch();
                decryptedLinks.add(this.createDownloadlink("http://flyload.net/request_window.php?" + id));
            } else if (new Regex(cryptedLink, patternSupported_Request).matches()) {
                String id = new Regex(cryptedLink, patternSupported_Request).getFirstMatch();
                requestInfo = HTTP.getRequest(new URL("http://flyload.net/download.php?view." + id));
                String pw = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<td color:red;' class='forumheader3'>(?!<b>)(.*?)</td>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                requestInfo = HTTP.getRequest(url);
                String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("value='(.*?)' readonly onclick", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < links.length; i++) {
                    DownloadLink link = this.createDownloadlink(links[i][0]);
                    if (!pw.matches("-") && !pw.matches("Kein Passwort")) link.addSourcePluginPassword(pw);
                    decryptedLinks.add(link);
                }
            } else if (new Regex(cryptedLink, patternSupported_Safe).matches()) {
                requestInfo = HTTP.getRequest(url);
                String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("onclick='popup\\(\"([a-zA-Z0-9]+)\",\"([a-zA-Z0-9]+)\"\\);", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < links.length; i++) {
                    requestInfo = HTTP.getRequest(new URL("http://flyload.net/safe.php?link_id=" + links[i][0] + "&link_hash=" + links[i][1]));
                    if (requestInfo.getLocation() != null) decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        //// step.setParameter(decryptedLinks);

        return decryptedLinks;
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
