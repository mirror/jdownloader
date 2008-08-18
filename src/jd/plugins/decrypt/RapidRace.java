package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RapidRace extends PluginForDecrypt {
    static private final String HOST = "rapidrace.org";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidrace\\.org/rel\\.php\\?ID=.+", Pattern.CASE_INSENSITIVE);

    public RapidRace() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String finalUrl = "";
        String page = br.getPage(parameter);
        while (page.indexOf("http://www.rapidrace.org/load.php?ID") != -1) {
            finalUrl = "";
            page = page.substring(page.indexOf("http://www.rapidrace.org/load.php?ID"));
            String subPage = br.getPage(page.substring(0, page.indexOf("\"")));
            String tmp = subPage.substring(subPage.indexOf("document.write(fu('") + 19);
            tmp = tmp.substring(0, tmp.indexOf("'"));
            for (int i = 0; i < tmp.length(); i += 2) {
                finalUrl = finalUrl + (char) (Integer.parseInt(tmp.substring(i, i + 2), 16) ^ i / 2);
            }
            decryptedLinks.add(createDownloadlink(finalUrl));
            page = page.substring(20);
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "TheBlindProphet";
    }

    @Override
    public String getHost() {
        return HOST;
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
