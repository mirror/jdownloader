package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
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

public class Bm4uin extends PluginForDecrypt {
    static private final String host = "bm4u.in";
    private String version = "1.0.0.0";

    private static final Pattern patternSupported = getSupportPattern("http://[\\w\\.]*?bm4u\\.in/index\\.php\\?do=show_download&id=\\d+");

    public Bm4uin() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo requestInfo = HTTP.getRequest(url);
                String pass = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), "<strong>Password:</strong> <b><font color=red>°</font></b>", 0);
                ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("onClick=\"window\\.open\\('crypt\\.php\\?id=([\\d]+)&amp;mirror=([\\d\\w]+)&part=([\\d]+)", Pattern.CASE_INSENSITIVE));

                for (int i = 0; i < links.size(); i++) {
                    url = new URL("http://bm4u.in/crypt.php?id=" + links.get(i).get(0) + "&mirror=" + links.get(i).get(1) + "&part=" + links.get(i).get(2));
                    requestInfo = HTTP.getRequest(url);
                    DownloadLink link = createDownloadlink(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<iframe src=\"", "\" width"));
                    link.addSourcePluginPassword(pass);
                    decryptedLinks.add(link);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            step.setParameter(decryptedLinks);
        }
        return null;
    }

    @Override
    public String getCoder() {
        return "Greeny";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "bm4u.in Decrypter";
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
