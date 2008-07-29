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
import jd.utils.JDUtilities;

public class rapidsharknet extends PluginForDecrypt {

    static private final String host = "Rapidshark Decrypter";
    private String version = "1.0.0.0";

    private static final Pattern patternLink_direct = Pattern.compile("http://[\\w\\.]*?rapidshark\\.net/(?!safe\\.php\\?id=)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_safephp = Pattern.compile("http://[\\w\\.]*?rapidshark\\.net/safe\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternLink_direct.pattern() + "|" + patternLink_safephp.pattern(), Pattern.CASE_INSENSITIVE);

    public rapidsharknet() {
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

            if (cryptedLink.matches(patternLink_direct.pattern())) {
                String downloadid = url.getFile().substring(1);
                /* weiterleiten zur safephp Seite */
                decryptedLinks.add(this.createDownloadlink("http://rapidshark.net/safe.php?id=" + downloadid));
            } else if (cryptedLink.matches(patternLink_safephp.pattern())) {
                String downloadid = url.getFile().substring(13);
                requestInfo = HTTP.getRequest(url, null, "http://rapidshark.net/" + downloadid, false);
                downloadid = new Regex(requestInfo, "src=\"(.*)\"></iframe>").getFirstMatch();
                decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(downloadid)));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // step.setParameter(decryptedLinks);

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
        return "Rapidshark Decrypter";
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
