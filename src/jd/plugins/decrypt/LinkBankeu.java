package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
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
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo requestInfo = HTTP.getRequest(url);
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("onclick='posli\\(\"([\\d]+)\",\"([\\d]+)\"\\);'", Pattern.CASE_INSENSITIVE));
                ArrayList<String> mirrors = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("onclick='mirror\\(\"(.*?)\"\\);'", Pattern.CASE_INSENSITIVE), 1);
                for (int i = 0; i < links.size(); i++) {
                    url = new URL("http://www.linkbank.eu/posli.php?match=" + links.get(i).get(0) + "&id=" + links.get(i).get(1));
                    requestInfo = HTTP.getRequestWithoutHtmlCode(url, null, cryptedLink, false);
                    decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                }
                if (getProperties().getBooleanProperty(CHECK_MIRRORS, false) == true) {
                    for (int i = 0; i < mirrors.size(); i++) {
                        decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(mirrors.get(i))));
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
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "LinkBankeu Decrypter";
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
