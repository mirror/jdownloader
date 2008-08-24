//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Gwarezcc extends PluginForDecrypt {
    static private final String host = "gwarez.cc";
    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details", Pattern.CASE_INSENSITIVE);

    private static final Pattern patternLink_Details_Mirror_Check = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/check/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Parts = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/parts/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Download_DLC = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternLink_Details_Download.pattern() + "|" + patternLink_Details_Mirror_Check.pattern() + "|" + patternLink_Details_Mirror_Parts.pattern() + "|" + patternLink_Download_DLC.pattern(), Pattern.CASE_INSENSITIVE);
    private static final String PREFER_DLC = "PREFER_DLC";

    public Gwarezcc() {
        super();
        setConfigElements();
    }

    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo;
            boolean dlc_found = false;

            if (cryptedLink.matches(patternLink_Details_Mirror_Check.pattern())) {
                /* Link aus der Mirror Check Seite */
                String downloadid = url.getFile().replaceAll("check", "parts");
                /* weiterleiten zur Mirror Parts Seite */
                decryptedLinks.add(createDownloadlink("http://gwarez.cc" + downloadid));
            } else if (cryptedLink.matches(patternLink_Details_Download.pattern())) {
                /* Link auf die Download Info Seite */
                requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                String downloadid = new Regex(url.getFile(), "\\/([\\d].*)").getMatch(0);

                if (getPluginConfig().getBooleanProperty(PREFER_DLC, false) == true) {
                    /* DLC Suchen */
                    String dlc[] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"gfx/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download/dlc/" + downloadid + "/\" onmouseover", Pattern.CASE_INSENSITIVE)).getColumn(-1);
                    if (dlc.length == 1) {
                        decryptedLinks.add(createDownloadlink("http://www.gwarez.cc/download/dlc/" + downloadid + "/"));
                        dlc_found = true;
                    } else {
                        logger.severe("Please Update Gwarez Plugin(DLC Pattern)");
                    }
                }

                if (dlc_found == false) {
                    /* Mirrors suchen (Verschlüsselt) */
                    String mirror_pages[] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"gfx/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"mirror/" + downloadid + "/check/(.*)/\" onmouseover", Pattern.CASE_INSENSITIVE)).getColumn(0);
                    for (int i = 0; i < mirror_pages.length; i++) {
                        /* Mirror Page zur weiteren Verarbeitung adden */
                        decryptedLinks.add(createDownloadlink("http://gwarez.cc/mirror/" + downloadid + "/parts/" + mirror_pages[i] + "/"));
                    }
                }

            } else if (cryptedLink.matches(patternLink_Details_Mirror_Parts.pattern())) {
                /* Link zu den Parts des Mirrors (Verschlüsselt) */
                requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                String downloadid = new Regex(url.getFile(), "\\/mirror/([\\d].*)/parts/([\\d].*)/").getMatch(0);
                /* Parts suchen */
                String parts[] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"redirect\\.php\\?to=([^\"]*?)(\" target|\n)", Pattern.CASE_INSENSITIVE)).getColumn(-1);
                /* Passwort suchen */
                url = new URL("http://gwarez.cc/" + downloadid + "#details");
                requestInfo = HTTP.getRequest(url, null, url.toString(), false);
                String password = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"gfx/icons/passwort\\.png\"> <b>Passwort:</b>.*?class=\"up\">(.*?)<\\/td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                if (password == null) {
                    logger.severe("Please Update Gwarez Plugin(PW Pattern)");
                } else {
                    password = password.trim();
                }

                for (int ii = 0; ii < parts.length; ii++) {
                    /* Parts decrypten und adden */
                    DownloadLink link = createDownloadlink(gwarezdecrypt(parts[ii]));
                    link.setSourcePluginComment("gwarez.cc - load and play your favourite game");
                    link.addSourcePluginPassword(password);
                    decryptedLinks.add(link);
                }
            } else if (cryptedLink.matches(patternLink_Download_DLC.pattern())) {
                /* DLC laden */
                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                HTTPConnection dlc_con = new HTTPConnection(url.openConnection());
                dlc_con.setRequestProperty("Referer", cryptedLink);
                if (Browser.download(container, dlc_con)) {
                    decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                    container.delete();
                } else {
                    return null;
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    public String getCoder() {
        return "JD-Team";
    }

    public String getHost() {
        return host;
    }

    public String getPluginName() {
        return host;
    }

    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    private String gwarezdecrypt(String link) {
        HashMap<String, String> replace = new HashMap<String, String>();
        replace.put("JAC\\|", "1");
        replace.put("IBD\\|", "2");
        replace.put("HCE\\|", "3");
        replace.put("GDF\\|", "4");
        replace.put("FEG\\|", "5");
        replace.put("EFH\\|", "6");
        replace.put("DGI\\|", "7");
        replace.put("CHJ\\|", "8");
        replace.put("BIK\\|", "9");
        replace.put("AJL\\|", "0");

        replace.put("\\|JQD\\|", "a");
        replace.put("\\|GRE\\|", "b");
        replace.put("\\|JKF\\|", "c");
        replace.put("\\|VHG\\|", "d");
        replace.put("\\|NDH\\|", "e");
        replace.put("\\|YKI\\|", "f");
        replace.put("\\|ZBJ\\|", "g");
        replace.put("\\|FJK\\|", "h");
        replace.put("\\|FKL\\|", "i");
        replace.put("\\|ZDM\\|", "j");
        replace.put("\\|ZSN\\|", "k");
        replace.put("\\|KIO\\|", "l");
        replace.put("\\|GIP\\|", "m");
        replace.put("\\|SIQ\\|", "n");
        replace.put("\\|KAR\\|", "o");
        replace.put("\\|SUS\\|", "p");
        replace.put("\\|POT\\|", "q");
        replace.put("\\|OPU\\|", "r");
        replace.put("\\|YXV\\|", "s");
        replace.put("\\|SXW\\|", "t");
        replace.put("\\|UYX\\|", "u");
        replace.put("\\|UMY\\|", "v");
        replace.put("\\|QSZ\\|", "w");
        replace.put("\\|AKA\\|", "x");
        replace.put("\\|VPB\\|", "y");
        replace.put("\\|YYC\\|", "z");

        replace.put("\\|DDA\\|", ":");
        replace.put("\\|SSB\\|", "/");
        replace.put("\\|OOC\\|", ".");

        for (String key : replace.keySet()) {
            String with = replace.get(key);
            link = link.replaceAll(key, with);
        }
        return link;
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_DLC, JDLocale.L("plugins.decrypt.gwarezcc.preferdlc", "Prefer DLC Container")).setDefaultValue(false));
    }

}
