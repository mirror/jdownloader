package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkBankeu extends PluginForDecrypt {
    static private final String host = "LinkBankeu Decrypter";
    private String version = "1.0.0.0";

    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+", Pattern.CASE_INSENSITIVE);
    private static final String CHECK_MIRRORS = "CHECK_MIRRORS";

    public LinkBankeu() {
        super();
        this.setConfigEelements();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ////if (step.getStep() == PluginStep.STEP_DECRYPT) {

            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo requestInfo = HTTP.getRequest(url);
                String[][] links = new Regex(requestInfo.getHtmlCode(), Pattern.compile("onclick='posli\\(\"([\\d]+)\",\"([\\d]+)\"\\);'", Pattern.CASE_INSENSITIVE)).getMatches();
                String[] mirrors = new Regex(requestInfo.getHtmlCode(), Pattern.compile("onclick='mirror\\(\"(.*?)\"\\);'", Pattern.CASE_INSENSITIVE)).getMatches(1);
                for (int i = 0; i < links.length; i++) {
                    url = new URL("http://www.linkbank.eu/posli.php?match=" + links[i][0] + "&id=" + links[i][1]);
                    requestInfo = HTTP.getRequestWithoutHtmlCode(url, null, cryptedLink, false);
                    decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                }
                if (getProperties().getBooleanProperty(CHECK_MIRRORS, false) == true) {
                    for (int i = 0; i < mirrors.length; i++) {
                        decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(mirrors[i])));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //step.setParameter(decryptedLinks);
        
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

    private void setConfigEelements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), CHECK_MIRRORS, JDLocale.L("plugins.decrypt.linkbankeu", "Check Mirror Links")).setDefaultValue(false));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
