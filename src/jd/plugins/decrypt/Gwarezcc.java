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

    private static final Pattern patternLink_Top10 = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/game_\\d{1,}.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Main = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d{1,}(\\#details){0,1}", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/game_\\d{1,}_download(\\#details){0,1}", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Check = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download_\\d{1,}_\\d{1,}_check.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Parts = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download_\\d{1,}_\\d{1,}_parts.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download_\\d{1,}_\\d{1,}.html", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternLink_Details_Main.pattern() + "|" + patternLink_Details_Download.pattern() + "|" + patternLink_Details_Mirror_Check.pattern() + "|" + patternLink_Details_Mirror_Parts.pattern() + "|" + patternLink_Top10.pattern() + "|" + patternLink_Details_Mirror_Download.pattern(), patternLink_Details_Main.flags() | patternLink_Details_Download.flags() | patternLink_Details_Mirror_Check.flags() | patternLink_Details_Mirror_Parts.flags() | patternLink_Top10.flags() | patternLink_Details_Mirror_Download.flags());
    private static final String USE_RSDF = "USE_RSDF";

    public Gwarezcc() {
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
                RequestInfo requestInfo;

                if (cryptedLink.matches(patternLink_Top10.pattern())) {
                    /* Link aus den Top10 */
                    String downloadid = new Regex(url.getFile(), "\\/game_([\\d].*).html").getFirstMatch();
                    /* weiterleiten zur Download Info Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/game_" + downloadid + "_download#details"));
                } else if (cryptedLink.matches(patternLink_Details_Main.pattern())) {
                    /* Link aus der Übersicht */
                    String downloadid = url.getFile().substring(1);
                    /* weiterleiten zur Download Info Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/game_" + downloadid + "_download#details"));
                } else if (cryptedLink.matches(patternLink_Details_Mirror_Check.pattern())) {
                    /* Link aus der Mirror Check Seite */
                    String downloadid = url.getFile().replaceAll("check", "parts");
                    /* weiterleiten zur Mirror Parts Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc" + downloadid));
                } else if (cryptedLink.matches(patternLink_Details_Download.pattern())) {
                    /* Link auf die Download Info Seite */
                    requestInfo = HTTP.getRequest(url, null, null, false);
                    String downloadid = new Regex(url.getFile(), "\\/game_([\\d].*)_download").getFirstMatch();
                    if (getProperties().getBooleanProperty(USE_RSDF, false) == true) {
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
                    /* Mirrors suchen (Verschlüsselt) */
                    ArrayList<String> mirror_pages = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"img\\/dl\\.gif\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download_" + downloadid + "_(.*)_check.html\" onmouseover", Pattern.CASE_INSENSITIVE), 1);
                    for (int i = 0; i < mirror_pages.size(); i++) {
                        /* Mirror Page zur weiteren Verarbeitung adden */
                        decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/download_" + downloadid + "_" + mirror_pages.get(i) + "_parts.html"));
                    }
                    /* Downloads suchen (Nicht Verschlüsselt) */
                    ArrayList<String> download_pages = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"download_" + downloadid + "_(.*).html\" target=\"\\_blank\">", Pattern.CASE_INSENSITIVE), 1);
                    for (int i = 0; i < download_pages.size(); i++) {
                        /* Download Page zur weiteren Verarbeitung adden */
                        decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/download_" + downloadid + "_" + download_pages.get(i) + ".html"));                        
                    }

                } else if (cryptedLink.matches(patternLink_Details_Mirror_Parts.pattern())) {
                    /* Link zu den Parts des Mirrors (Verschlüsselt) */
                    requestInfo = HTTP.getRequest(url, null, null, false);
                    String downloadid = new Regex(url.getFile(), "\\/download_([\\d].*)_([\\d].*)_parts").getFirstMatch();
                    /* Parts suchen */
                    ArrayList<String> parts = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"redirect\\.php\\?to=([^\"]*?)(\" target|\n)", Pattern.CASE_INSENSITIVE), 1);
                    for (int ii = 0; ii < parts.size(); ii++) {
                        /* Parts decrypten und adden */
                        decryptedLinks.add(this.createDownloadlink(gwarezdecrypt(parts.get(ii))));
                    }
                    /* Passwort suchen */
                    url = new URL("http://gwarez.cc/" + downloadid + "#details");
                    requestInfo = HTTP.getRequest(url, null, null, false);
                    ArrayList<String> password = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<td width=\"150\" height=\"20\" style=\"background\\-image\\:url\\(img\\/\\/table_ad920f_bg\\.jpg\\)\\;\">\n(.*?)<\\/td>", Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() == 1) {
                        /* Passwort gefunden */
                        default_password.add(JDUtilities.htmlDecode(password.get(0)));
                    } else
                        logger.severe("Please Update Gwarez Plugin(PW Pattern)");
                } else if (cryptedLink.matches(patternLink_Details_Mirror_Download.pattern())) {
                    /* Link zu den Parts des Downloads (Nicht verschlüsselt) */
                    String downloadid = new Regex(url.getFile(), "\\/download_([\\d].*)_([\\d].*).html").getFirstMatch();
                    /* Passwort suchen */
                    Vector<String> downloadlink_passwds=new Vector<String>();
                    url = new URL("http://gwarez.cc/" + downloadid + "#details");
                    requestInfo = HTTP.getRequest(url, null, null, false);
                    ArrayList<String> password = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<td width=\"150\" height=\"20\" style=\"background\\-image\\:url\\(img\\/\\/table_ad920f_bg\\.jpg\\)\\;\">\n(.*?)<\\/td>", Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() == 1) {
                        /* Passwort gefunden */
                        downloadlink_passwds.add(JDUtilities.htmlDecode(password.get(0)));                        
                    } else
                        logger.severe("Please Update Gwarez Plugin(PW Pattern)");
                    /*Download Link folgen*/
                    url = new URL(cryptedLink);
                    requestInfo = HTTP.getRequest(url, null, "http://gwarez.cc/game_"+downloadid+"_download#details", true);
                    String downloadlink_url = new Regex(requestInfo.getHtmlCode(), "http\\:\\/\\/(.*)\">").getFirstMatch();
                    DownloadLink downloadlink_link=createDownloadlink(downloadlink_url);
                    downloadlink_link.setSourcePluginPasswords(downloadlink_passwds);
                    decryptedLinks.add(downloadlink_link);
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
