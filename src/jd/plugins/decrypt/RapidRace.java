package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class RapidRace extends PluginForDecrypt {
    static private final String HOST = "rapidrace.org";

    static private final String CODER = "TheBlindProphet";

    static private final String VERSION = "$Revision$";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidrace\\.org/rel\\.php\\?ID=.+", Pattern.CASE_INSENSITIVE);

    public RapidRace() {
        super();
    }

    public String getCoder() {
        return CODER;
    }

    public String getHost() {
        return HOST;
    }

    public String getPluginID() {
        return HOST + " " + VERSION;
    }

    public String getPluginName() {
        return HOST;
    }

    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public String getVersion() {
        return VERSION;
    }

    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            String finalUrl = "";
            String quellcode = "";            
            RequestInfo reqinfo = HTTP.getRequest(url, null, null, false);
            quellcode = reqinfo.getHtmlCode();
            while (quellcode.indexOf("http://www.rapidrace.org/load.php?ID") != -1) {
                finalUrl = "";
                quellcode = quellcode.substring(quellcode.indexOf("http://www.rapidrace.org/load.php?ID"));
                String tmp = quellcode.substring(0, quellcode.indexOf("\""));                
                reqinfo = HTTP.getRequest(new URL(tmp), null, null, true);
                tmp = reqinfo.getHtmlCode().substring(reqinfo.getHtmlCode().indexOf("document.write(fu('") + 19);
                tmp = tmp.substring(0, tmp.indexOf("'"));
                for (int i = 0; i < tmp.length(); i += 2) {
                    finalUrl = finalUrl + (char) (Integer.parseInt(tmp.substring(i, i + 2), 16) ^ (i / 2));
                }
                decryptedLinks.add(this.createDownloadlink(finalUrl));
                quellcode = quellcode.substring(20);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    public boolean doBotCheck(File file) {
        return false;
    }
}
