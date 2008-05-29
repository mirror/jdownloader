package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Wordpress extends PluginForDecrypt {
    static private final String host = "Wordpress Parser";
    private String version = "1.0.0.0";
    private String Supportpattern = "(http://[*]movie-blog.org/[+]/[+]/[+]/[+])"+"|(http://[*]xxx-blog.org/blog.php\\?id=[+])"+"|(http://[*]sky-porn.info/blog/\\?p=[+])";    
    private Pattern patternSupported = getSupportPattern(Supportpattern);

    public Wordpress() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));

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
        return "Rlslog Comment Parser";
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
        logger.info("Wordpress Comment Parser");

        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                String[] Links = Plugin.getHttpLinks(reqinfo.getHtmlCode(), null);
                Vector<String> passs = findPasswords(reqinfo.getHtmlCode());
                for (int i = 0; i < Links.length; i++) {

                    if (!Links[i].contains(url.getHost())) {
                        decryptedLinks.add(this.createDownloadlink(Links[i]));
                        decryptedLinks.lastElement().addSourcePluginPasswords(passs);
                    }
                }
                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}