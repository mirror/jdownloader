package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Gwarezcc extends PluginForDecrypt {
    static private final String host = "Gwarez Decrypter";
    private String version = "1.0.0.0";

    private static final Pattern patternLink_Details_Main = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/game_\\d{1,}_download\\#details", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternLink_Details_Main.pattern() + "|" + patternLink_Details_Download.pattern(), patternLink_Details_Main.flags() | patternLink_Details_Download.flags());
    private static final String USE_RSDF = "USE_RSDF";
    private static boolean load_rsdf = false;

    public Gwarezcc() {
        super();
        this.setConfigEelements();
        load_rsdf = getProperties().getBooleanProperty(USE_RSDF, false);
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(cryptedLink);
                RequestInfo requestInfo = HTTP.getRequest(url, null, null, false);

                if (cryptedLink.matches(patternLink_Details_Main.pattern())) {
                    /* Link aus der Übersicht */
                    String downloadid = url.getFile().substring(1);
                    /* weiterleiten zur Download Info Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/game_" + downloadid + "_download#details"));
                } else if (cryptedLink.matches(patternLink_Details_Download.pattern())) {
                    /* Download Info Seite */
                    String downloadid = new Regex(url.getFile(), "\\/game_([\\d].*)_download").getFirstMatch();
                    /* Passwort suchen */
                    ArrayList<String> password = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<td width=\"150\" height=\"20\" style=\"background\\-image\\:url\\(img\\/\\/table_ad920f_bg\\.jpg\\)\\;\">\n(.*?)<\\/td>", Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() == 1) {
                        /* Passwort gefunden */
                        default_password.add(JDUtilities.htmlDecode(password.get(0)));
                    } else
                        logger.severe("Please Update Gwarez Plugin(PW Pattern)");
                    if (load_rsdf == true) {
                        /* RSDF Suchen */
                        ArrayList<String> rsdf = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"img\\/dl\\.gif\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download_" + downloadid + "_5.html\"><b>.rsdf</b>", Pattern.CASE_INSENSITIVE), 0);
                        if (rsdf.size() == 1) {
                            /* RSDF gefunden */
                            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
                            URL rsdf_url = new URL("http://gwarez.cc/download_" + downloadid + "_5.html");
                            HTTPConnection rsdf_con = new HTTPConnection(rsdf_url.openConnection());
                            rsdf_con.setRequestProperty("Referer", cryptedLink);
                            JDUtilities.download(container, rsdf_con);
                            JDUtilities.getController().loadContainerFile(container);
                        } else
                            logger.severe("Please Update Gwarez Plugin(Download Pattern)");
                    }
                    /* Mirrors suchen */
                    ArrayList<String> mirror_pages = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"img\\/dl\\.gif\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download_" + downloadid + "_(.*)_check.html\" onmouseover", Pattern.CASE_INSENSITIVE), 1);
                    for (int i = 0; i < mirror_pages.size(); i++) {
                        /* Mirror Page verarbeiten */
                        URL mirror = new URL("http://gwarez.cc/download_" + downloadid + "_" + mirror_pages.get(i) + "_parts.html");
                        RequestInfo mirrorInfo = HTTP.getRequest(mirror, null, null, false);
                        /* Parts suchen */
                        ArrayList<String> parts = SimpleMatches.getAllSimpleMatches(mirrorInfo.getHtmlCode(), Pattern.compile("<a href=\"redirect\\.php\\?to=([^\"]*?)(\" target|\n)", Pattern.CASE_INSENSITIVE), 1);
                        for (int ii = 0; ii < parts.size(); ii++) {
                            /* Parts decrypten und adden */
                            decryptedLinks.add(this.createDownloadlink(gwarezdecrypt(parts.get(ii))));
                        }
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

    private String gwarezdecrypt(String link) {
        HashMap<String, Integer> replace = new HashMap<String, Integer>();
        replace.put("JA", 1);
        replace.put("IB", 2);
        replace.put("HC", 3);
        replace.put("GD", 4);
        replace.put("FE", 5);
        replace.put("EF", 6);
        replace.put("DG", 7);
        replace.put("CH", 8);
        replace.put("BI", 9);
        replace.put("AJ", 0);

        for (Iterator<String> it = replace.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            link = link.replaceAll(key + "\\|", replace.get(key) + "");
        }
        return link;
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
        return "Gwarez Decrypter";
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
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_RSDF, JDLocale.L("plugins.decrypt.gwarezcc.usersdf", "Use RSDF Container")).setDefaultValue(false));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
