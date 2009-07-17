//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.JDUtilities;

public class Gwarezcc extends PluginForDecrypt {

    private static final Pattern patternLink_Details_Download = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/\\d+", Pattern.CASE_INSENSITIVE);

    private static final Pattern patternLink_Details_Mirror_Check = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d+/checked/game/\\d+/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Details_Mirror_Parts = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/mirror/\\d+/parts/game/\\d+/", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Download_DLC = Pattern.compile("http://[\\w\\.]*?gwarez\\.cc/download/dlc/\\d+/", Pattern.CASE_INSENSITIVE);

    public Gwarezcc(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(patternLink_Details_Mirror_Check.pattern())) {
            /* weiterleiten zur Mirror Parts Seite */
            parameter = parameter.replaceAll("checked", "parts");
        }

        if (parameter.matches(patternLink_Details_Download.pattern())) {
            /* Link auf die Download Info Seite */
            br.getPage(parameter);
            String downloadid = new Regex(parameter, "\\/(\\d+)").getMatch(0);

            /* DLC Suchen */
            String dlc[] = br.getRegex(Pattern.compile("<img src=\"gfx/icons/dl\\.png\" style=\"vertical-align\\:bottom\\;\"> <a href=\"download/dlc/" + downloadid + "/\" onmouseover", Pattern.CASE_INSENSITIVE)).getColumn(-1);
            if (dlc.length == 1) {
                decryptedLinks.add(createDownloadlink("http://www.gwarez.cc/download/dlc/" + downloadid + "/"));
            } else {
                logger.severe("Please Update Gwarez Plugin(DLC Pattern)");
                /* Mirrors suchen (Verschlüsselt) */
                String mirror_pages[] = br.getRegex(Pattern.compile("<a href=\"mirror/(\\d+)/checked/game/" + downloadid + "/", Pattern.CASE_INSENSITIVE)).getColumn(0);
                for (String mirror_page : mirror_pages) {
                    /* Mirror Page zur weiteren Verarbeitung adden */
                    decryptedLinks.add(createDownloadlink("http://gwarez.cc/mirror/" + mirror_page + "/parts/game/" + downloadid + "/"));
                }
            }

        } else if (parameter.matches(patternLink_Details_Mirror_Parts.pattern())) {
            /* Link zu den Parts des Mirrors (Verschlüsselt) */
            br.getHeaders().put("Referer", parameter.replaceAll("parts", "check"));

            br.getPage(parameter);
            String downloadid = new Regex(parameter, "\\/mirror/\\d+/parts/game/(\\d+)/").getMatch(0);
            // /* Parts suchen */
            // String parts[] =
            // br.getRegex(Pattern.compile(
            // "<a href=\"redirect\\.php\\?to=([^\"]*?)\"",
            // Pattern.CASE_INSENSITIVE)).getColumn(0);

            Form[] forms = br.getForms();

            /* Passwort suchen */
            br.getPage("http://gwarez.cc/" + downloadid + "#details");
            String password = br.getRegex(Pattern.compile("<strong>Passwort:</strong>.*?<td align=.*?listdetail2.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password == null) {
                logger.severe("Please Update Gwarez Plugin(PW Pattern)");
            } else {
                password = password.trim();
            }
            progress.setRange(forms.length);
            Browser brc = br.cloneBrowser();
            for (Form form : forms) {
                /* Parts decrypten und adden */
                if (form.getAction().trim().startsWith("redirect")) {
                    brc = br.cloneBrowser();
                    brc.submitForm(form);

                    String linkString = null;

                    for (int i = 0; i < 10; i++) {
                        // viele links werden auch ohne recaptcha angeboten.
                        // deshalb wird der check zuerst gemacht.
                        linkString = brc.getRegex("<meta http-equiv=\"refresh\".*?URL=(.*?)\">").getMatch(0);
                        if (linkString != null) break;
                        
                        Recaptcha rc = new Recaptcha(brc);
                        rc.parse();
                        String k = brc.getRegex("<script type=\"text/javascript\" src=\"http://api.recaptcha.net/challenge\\?k=(.*?)\"></script>").getMatch(0);
                        if (k != null) {
                            /* recaptcha */

                            Browser rcBr = brc.cloneBrowser();
                            rcBr.getPage("http://api.recaptcha.net/challenge?k=" + k);
                            String challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
                            String server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
                            String captchaAddress = server + "image?c=" + challenge;
                            File captchaFile = this.getLocalCaptchaFile();
                            Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
                            String code = getCaptchaCode(captchaFile, param);
                            if (code == null) continue;
                            form.put("recaptcha_challenge_field", challenge);
                            form.put("recaptcha_response_field", code);
                            brc.submitForm(form);
                        } else {
                            String code = getCaptchaCode("captcha/captcha.php", param);
                            Form cap = br.getForm(0);
                            cap.put("sicherheitscode", code);
                            brc.submitForm(cap);
                        }

                    }
                    if (linkString == null) linkString = brc.getRegex("<meta http-equiv=\"refresh\".*?URL=(.*?)\">").getMatch(0);

                    progress.increase(1);

                    DownloadLink l = this.createDownloadlink(linkString);
                    l.setSourcePluginComment("gwarez.cc - load and play your favourite game");
                    l.addSourcePluginPassword(password);
                    decryptedLinks.add(l);
                }
            }
        } else if (parameter.matches(patternLink_Download_DLC.pattern())) {
            /* DLC laden */
            String downloadid = new Regex(parameter, "\\/download/dlc/(\\d+)/").getMatch(0);
            /* Passwort suchen */
            br.getPage("http://gwarez.cc/" + downloadid + "#details");
            String password = br.getRegex(Pattern.compile("<strong>Passwort:</strong>.*?<td align=.*?listdetail2.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password == null) {
                logger.severe("Please Update Gwarez Plugin(PW Pattern)");
            } else {
                password = password.trim();
            }
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            Browser.download(container, br.cloneBrowser().openGetConnection("http://gwarez.cc/download/dlc/" + downloadid + "/"));
            for (DownloadLink dLink : JDUtilities.getController().getContainerLinks(container)) {
                dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
            }
            container.delete();

        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
