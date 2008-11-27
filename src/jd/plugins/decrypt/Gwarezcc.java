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
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.io.JDIO;

public class Gwarezcc extends PluginForDecrypt {

    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d{1,}\\#details", Pattern.CASE_INSENSITIVE);

    private static final Pattern patternLink_Details_Mirror_Check = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/check/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Parts = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d{1,}/parts/\\d{1,}/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Download_DLC = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d{1,}/", Pattern.CASE_INSENSITIVE);

    private static final String PREFER_DLC = "PREFER_DLC";

    public Gwarezcc(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        boolean dlc_found = false;

        if (parameter.matches(patternLink_Details_Mirror_Check.pattern())) {
            /* weiterleiten zur Mirror Parts Seite */
            parameter = parameter.replaceAll("check", "parts");
        }

        if (parameter.matches(patternLink_Details_Download.pattern())) {
            /* Link auf die Download Info Seite */
            br.getPage(parameter);
            String downloadid = new Regex(parameter, "\\/(\\d+)").getMatch(0);

            if (getPluginConfig().getBooleanProperty(PREFER_DLC, false) == true) {
                /* DLC Suchen */
                String dlc[] = br.getRegex(Pattern.compile("<img src=\"gfx/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download/dlc/" + downloadid + "/\" onmouseover", Pattern.CASE_INSENSITIVE)).getColumn(-1);
                if (dlc.length == 1) {
                    decryptedLinks.add(createDownloadlink("http://www.gwarez.cc/download/dlc/" + downloadid + "/"));
                    dlc_found = true;
                } else {
                    logger.severe("Please Update Gwarez Plugin(DLC Pattern)");
                }
            }

            if (dlc_found == false) {
                /* Mirrors suchen (Verschlüsselt) */
                String mirror_pages[] = br.getRegex(Pattern.compile("<img src=\"gfx/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"mirror/" + downloadid + "/check/(.*)/\" onmouseover", Pattern.CASE_INSENSITIVE)).getColumn(0);
                for (int i = 0; i < mirror_pages.length; i++) {
                    /* Mirror Page zur weiteren Verarbeitung adden */
                    decryptedLinks.add(createDownloadlink("http://gwarez.cc/mirror/" + downloadid + "/parts/" + mirror_pages[i] + "/"));
                }
            }

        } else if (parameter.matches(patternLink_Details_Mirror_Parts.pattern())) {
            /* Link zu den Parts des Mirrors (Verschlüsselt) */
            br.getPage(parameter);
            String downloadid = new Regex(parameter, "\\/mirror/([\\d].*)/parts/([\\d].*)/").getMatch(0);
//            /* Parts suchen */
//            String parts[] = br.getRegex(Pattern.compile("<a href=\"redirect\\.php\\?to=([^\"]*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);

            Form[] forms = br.getForms();

            /* Passwort suchen */
            br.getPage("http://gwarez.cc/" + downloadid + "#details");
            String password = br.getRegex(Pattern.compile("<img src=\"gfx/icons/passwort\\.png\"> <b>Passwort:</b>.*?class=\"up\">(.*?)<\\/td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password == null) {
                logger.severe("Please Update Gwarez Plugin(PW Pattern)");
            } else {
                password = password.trim();
            }
            progress.setRange(forms.length);
            for (int ii = 0; ii < forms.length; ii++) {
                /* Parts decrypten und adden */
                if (forms[ii].action.trim().startsWith("redirect")) {

                    br.submitForm(forms[ii]);

                    String linkString = null;

                    for (int i = 0; i < 10; i++) {
                        // viele links werden auch ohne recaptcha angeboten.
                        // deshalb wird der check zuerst gemacht.
                        linkString = br.getRegex("<meta http-equiv=\"refresh\".*?URL=(.*?)\">").getMatch(0);
                        if (linkString != null) break;
                        String k = br.getRegex("<script type=\"text/javascript\" src=\"http://api.recaptcha.net/challenge\\?k=(.*?)\"></script>").getMatch(0);
                        Browser rcBr = br.cloneBrowser();
                        rcBr.getPage("http://api.recaptcha.net/challenge?k=" + k);
                        String challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
                        String server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
                        String captchaAddress = server + "image?c=" + challenge;
                        File captchaFile = this.getLocalCaptchaFile(this);
                        Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
                        String code = Plugin.getCaptchaCode(captchaFile, this, param);
                        if (code == null) continue;
                        forms[ii].put("recaptcha_challenge_field", challenge);
                        forms[ii].put("recaptcha_response_field", code);
                        br.submitForm(forms[ii]);

                    }
                    if (linkString == null) linkString = br.getRegex("<meta http-equiv=\"refresh\".*?URL=(.*?)\">").getMatch(0);

                    progress.increase(1);
                    // String linkString = gWarezDecrypt(parts[ii]);
                    Vector<DownloadLink> links = new DistributeData(linkString).findLinks(false);
                    if (links.size() == 0) continue;
                    for (DownloadLink l : links) {
                        if (l.isAvailable()) {

                            l.setSourcePluginComment("gwarez.cc - load and play your favourite game");
                            l.addSourcePluginPassword(password);
                            decryptedLinks.add(l);
                        }
                    }
                }
            }
        } else if (parameter.matches(patternLink_Download_DLC.pattern())) {
            /* DLC laden */
            String downloadid = new Regex(parameter, "\\/download/dlc/([\\d].*)/").getMatch(0);
            /* Passwort suchen */
            br.getPage("http://gwarez.cc/" + downloadid + "#details");
            String password = br.getRegex(Pattern.compile("<img src=\"gfx/icons/passwort\\.png\"> <b>Passwort:</b>.*?class=\"up\">(.*?)<\\/td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password == null) {
                logger.severe("Please Update Gwarez Plugin(PW Pattern)");
            } else {
                password = password.trim();
            }
            File container = JDIO.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            Browser.download(container, br.cloneBrowser().openGetConnection("http://gwarez.cc/download/dlc/" + downloadid + "/"));
            for (DownloadLink dLink : JDUtilities.getController().getContainerLinks(container)) {
                dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
            }
            container.delete();

        }

        return decryptedLinks;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

//    private String gWarezDecrypt(String link) {
//        HashMap<String, String> replace = new HashMap<String, String>();
//        replace.put("JAC\\|", "1");
//        replace.put("IBD\\|", "2");
//        replace.put("HCE\\|", "3");
//        replace.put("GDF\\|", "4");
//        replace.put("FEG\\|", "5");
//        replace.put("EFH\\|", "6");
//        replace.put("DGI\\|", "7");
//        replace.put("CHJ\\|", "8");
//        replace.put("BIK\\|", "9");
//        replace.put("AJL\\|", "0");
//
//        replace.put("\\|JQD\\|", "a");
//        replace.put("\\|GRE\\|", "b");
//        replace.put("\\|JKF\\|", "c");
//        replace.put("\\|VHG\\|", "d");
//        replace.put("\\|NDH\\|", "e");
//        replace.put("\\|YKI\\|", "f");
//        replace.put("\\|ZBJ\\|", "g");
//        replace.put("\\|bFJK\\|", "h");
//        replace.put("\\|FKL\\|", "i");
//        replace.put("\\|ZDM\\|", "j");
//        replace.put("\\|ZSN\\|", "k");
//        replace.put("\\|KIO\\|", "l");
//        replace.put("\\|GIP\\|", "m");
//        replace.put("\\|SIQ\\|", "n");
//        replace.put("\\|KAR\\|", "o");
//        replace.put("\\|SUS\\|", "p");
//        replace.put("\\|POT\\|", "q");
//        replace.put("\\|hOPU\\|", "r");
//        replace.put("\\|qYXV\\|", "s");
//        replace.put("\\|SXW\\|", "t");
//        replace.put("\\|UYX\\|", "u");
//        replace.put("\\|UMY\\|", "v");
//        replace.put("\\|QSZ\\|", "w");
//        replace.put("\\|AKA\\|", "x");
//        replace.put("\\|VPB\\|", "y");
//        replace.put("\\|YYC\\|", "z");
//
//        replace.put("\\|DDA\\|", ":");
//        replace.put("\\|SSB\\|", "/");
//        replace.put("\\|OOC\\|", ".");
//
//        for (String key : replace.keySet()) {
//            String with = replace.get(key);
//            link = link.replaceAll(key, with);
//        }
//        return link;
//    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_DLC, JDLocale.L("plugins.decrypt.gwarezcc.preferdlc", "Prefer DLC Container")).setDefaultValue(false));
    }

}
