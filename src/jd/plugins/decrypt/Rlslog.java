package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Rlslog extends PluginForDecrypt {
    static private final String host = "Rlslog Comment Parser";
    private String version = "1.0.0.0";
    private static final String HOSTER_LIST = "HOSTER_LIST";
    private String[] hosterList;

    private Pattern patternSupported = getSupportPattern("(http://[*]rlslog.net(/[+]/[+]/#comments|/[+]/#comments|/[+]/))");

    public Rlslog() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        this.setConfigEelements();
        this.hosterList = JDUtilities.splitByNewline(getProperties().getStringProperty(HOSTER_LIST, ""));
    }

    private boolean checkLink(String link) {

        for (String hoster : hosterList) {
            if (hoster.trim().length() > 2 && link.toLowerCase().contains(hoster.toLowerCase().trim())) return true;
        }
        return false;
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
        logger.info("Rlslog Comment Parser");
        String followcomments = "";

        if (parameter.contains("/comment-page")) {
            followcomments = parameter.substring(0, parameter.indexOf("/comment-page"));
        }
        if (!parameter.contains("#comments")) {
            parameter += "#comments";
            followcomments = parameter.substring(0, parameter.indexOf("/#comments"));
        } else {
            followcomments = parameter.substring(0, parameter.indexOf("/#comments"));
        }
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);
                String[] Links = Plugin.getHttpLinks(reqinfo.getHtmlCode(), null);
                Vector<String> passs = findPasswords(reqinfo.getHtmlCode());
                for (int i = 0; i < Links.length; i++) {

                    if (checkLink(Links[i])) {
                        /*
                         * links adden, die in der hosterlist stehen
                         */
                        decryptedLinks.add(this.createDownloadlink(Links[i]));
                        decryptedLinks.lastElement().addSourcePluginPasswords(passs);
                    }
                    if (Links[i].contains(followcomments) == true) {
                        /* weitere comment pages abrufen */
                        URL url2 = new URL(Links[i]);
                        RequestInfo reqinfo2 = getRequest(url2);
                        String[] Links2 = Plugin.getHttpLinks(reqinfo2.getHtmlCode(), null);
                        Vector<String> passs2 = findPasswords(reqinfo2.getHtmlCode());
                        for (int j = 0; j < Links2.length; j++) {

                            if (checkLink(Links2[j])) {
                                /*
                                 * links adden, die in der hosterlist stehen
                                 */
                                decryptedLinks.add(this.createDownloadlink(Links2[j]));
                                decryptedLinks.lastElement().addSourcePluginPasswords(passs2);
                            }
                        }
                    }
                }
                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void setConfigEelements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, getProperties(), HOSTER_LIST, JDLocale.L("plugins.decrypt.rlslog.hosterlist", "Liste der zu suchenden Hoster(Ein Hoster/Zeile)")));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}