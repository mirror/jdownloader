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
    static private final String host = "gwarez.cc Decrypter";
    private String version = "1.0.0.0";

    private static final Pattern patternLink_Details_Main = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}\\#details", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Download_DLC = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Check = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/check/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Parts = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/parts/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternLink_Details_Main.pattern() + "|" + patternLink_Details_Download.pattern() + "|" + patternLink_Details_Mirror_Check.pattern() + "|" + patternLink_Details_Mirror_Parts.pattern() + "|" + patternLink_Download_DLC.pattern(), Pattern.CASE_INSENSITIVE);
    private static final String PREFER_DLC = "PREFER_DLC";

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
                boolean dlc_found = false;

                if (cryptedLink.matches(patternLink_Details_Main.pattern())) {
                    /* Link aus der Übersicht */
                    String downloadid = url.getFile().substring(1);
                    /* weiterleiten zur Download Info Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/mirror/" + downloadid + "#details"));
                } else if (cryptedLink.matches(patternLink_Details_Mirror_Check.pattern())) {
                    /* Link aus der Mirror Check Seite */
                    String downloadid = url.getFile().replaceAll("check", "parts");
                    /* weiterleiten zur Mirror Parts Seite */
                    decryptedLinks.add(this.createDownloadlink("http://gwarez.cc" + downloadid));
                } else if (cryptedLink.matches(patternLink_Details_Download.pattern())) {
                    /* Link auf die Download Info Seite */
                    requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                    String downloadid = new Regex(url.getFile(), "\\/mirror/([\\d].*)").getFirstMatch();

                    if (getProperties().getBooleanProperty(PREFER_DLC, false) == true) {
                        /* DLC Suchen */
                        ArrayList<String> dlc = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"img/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download/dlc/" + downloadid + "/\" onmouseover", Pattern.CASE_INSENSITIVE), 0);
                        if (dlc.size() == 1) {
                            decryptedLinks.add(this.createDownloadlink("http://www.gwarez.cc/download/dlc/" + downloadid + "/"));
                            dlc_found = true;
                        } else
                            logger.severe("Please Update Gwarez Plugin(DLC Pattern)");
                    }

                    if (dlc_found == false) {
                        /* Mirrors suchen (Verschlüsselt) */
                        ArrayList<String> mirror_pages = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"img/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"mirror/" + downloadid + "/check/(.*)/\" onmouseover", Pattern.CASE_INSENSITIVE), 1);
                        for (int i = 0; i < mirror_pages.size(); i++) {
                            /* Mirror Page zur weiteren Verarbeitung adden */
                            decryptedLinks.add(this.createDownloadlink("http://gwarez.cc/mirror/" + downloadid + "/parts/" + mirror_pages.get(i) + "/"));
                        }
                    }

                } else if (cryptedLink.matches(patternLink_Details_Mirror_Parts.pattern())) {
                    /* Link zu den Parts des Mirrors (Verschlüsselt) */
                    requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                    String downloadid = new Regex(url.getFile(), "\\/mirror/([\\d].*)/parts/([\\d].*)/").getFirstMatch();
                    /* Parts suchen */
                    ArrayList<String> parts = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"redirect\\.php\\?to=([^\"]*?)(\" target|\n)", Pattern.CASE_INSENSITIVE), 1);
                    /* Passwort suchen */
                    url = new URL("http://gwarez.cc/" + downloadid + "#details");
                    Vector<String> link_passwds = new Vector<String>();
                    requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                    ArrayList<String> password = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<td width=\"110\" height=\"20\" style=\"background\\-image\\:url\\(img\\/\\/table_ad920f_bg\\.jpg\\)\\;\">\n(.*?)<\\/td>", Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() == 1) {
                        /* Passwort gefunden */
                        link_passwds.add(JDUtilities.htmlDecode(password.get(0)));
                    } else
                        logger.severe("Please Update Gwarez Plugin(PW Pattern)");

                    for (int ii = 0; ii < parts.size(); ii++) {
                        /* Parts decrypten und adden */
                        DownloadLink link = this.createDownloadlink(gwarezdecrypt(parts.get(ii)));
                        link.setSourcePluginComment("gwarez.cc - load and play your favourite game");
                        link.setSourcePluginPasswords(link_passwds);
                        decryptedLinks.add(link);
                    }
                } else if (cryptedLink.matches(patternLink_Download_DLC.pattern())) {
                    /* DLC laden */
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                    HTTPConnection dlc_con = new HTTPConnection(url.openConnection());
                    dlc_con.setRequestProperty("Referer", cryptedLink);
                    JDUtilities.download(container, dlc_con);
                    JDUtilities.getController().loadContainerFile(container);
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
        HashMap<String, String> replace = new HashMap<String, String>();
        replace.put("JA\\|", "1");
        replace.put("IB\\|", "2");
        replace.put("HC\\|", "3");
        replace.put("GD\\|", "4");
        replace.put("FE\\|", "5");
        replace.put("EF\\|", "6");
        replace.put("DG\\|", "7");
        replace.put("CH\\|", "8");
        replace.put("BI\\|", "9");
        replace.put("AJ\\|", "0");
        
        replace.put("\\|JQ\\|", "a"); 
        replace.put("\\|GR\\|", "b"); 
        replace.put("\\|JK\\|", "c"); 
        replace.put("\\|VH\\|", "d"); 
        replace.put("\\|ND\\|", "e"); 
        replace.put("\\|YK\\|", "f"); 
        replace.put("\\|ZB\\|", "g"); 
        replace.put("\\|FJ\\|", "h"); 
        replace.put("\\|FK\\|", "i"); 
        replace.put("\\|ZD\\|", "j"); 
        replace.put("\\|ZS\\|", "k"); 
        replace.put("\\|KI\\|", "l"); 
        replace.put("\\|GI\\|", "m"); 
        replace.put("\\|SI\\|", "n"); 
        replace.put("\\|KA\\|", "o"); 
        replace.put("\\|SU\\|", "p"); 
        replace.put("\\|PO\\|", "q"); 
        replace.put("\\|OP\\|", "r"); 
        replace.put("\\|YX\\|", "s"); 
        replace.put("\\|SX\\|", "t"); 
        replace.put("\\|UY\\|", "u"); 
        replace.put("\\|UM\\|", "v"); 
        replace.put("\\|QS\\|", "w"); 
        replace.put("\\|AK\\|", "x"); 
        replace.put("\\|VP\\|", "y"); 
        replace.put("\\|YY\\|", "z"); 
         
        replace.put("\\|DD\\|", ":"); 
        replace.put("\\|SS\\|", "/"); 
        replace.put("\\|OO\\|", ".");

        for (Iterator<String> it = replace.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String with=replace.get(key);
            link = link.replaceAll(key , with);
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
        return "gwarez.cc Decrypter";
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
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PREFER_DLC, JDLocale.L("plugins.decrypt.gwarezcc.preferdlc", "Prefer DLC Container")).setDefaultValue(false));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
