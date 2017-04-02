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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ddl-music.to" }, urls = { "http://(?:www\\.)?ddl-music\\.(?:org|to)/(download/\\d+/.*?/|download/links/[a-z0-9]+/(mirror/\\d+/)?)" })
public class DDLMscrg extends antiDDoSForDecrypt {
    private static final String DECRYPTER_DDLMSC_MAIN  = "http://(www\\.)?ddl-music\\.to/download/\\d+/.*?/";
    private static final String DECRYPTER_DDLMSC_CRYPT = "http://(www\\.)?ddl-music\\.to/download/links/[a-z0-9]+/(mirror/\\d+/)?";
    private static final String CAPTCHATEXT            = "captcha\\.php\\?id=|class=\"g-recaptcha\"";

    public DDLMscrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("ddl-music.org/", "ddl-music.to/");
        if (parameter.matches(DECRYPTER_DDLMSC_CRYPT)) {
            logger.info("The user added a DECRYPTER_DDLMSC_CRYPT link...");
            int add = 0;
            for (int i = 1; i < 10; i++) {
                getPage(parameter);
                sleep(3000 + add, param);
                if (br.containsHTML("class=\"fehler\"")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                if (!br.containsHTML(CAPTCHATEXT)) {
                    return null;
                }
                if (br.containsHTML("class=(\"|')g-recaptcha\\1")) {
                    final Form captcha = br.getForm(0);
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(captcha);
                    break;
                } else {
                    String captchaUrl = "/captcha.php?id=" + new Regex(parameter, "/download/links/([a-z0-9]+)/").getMatch(0) + "&rand=" + new Random().nextInt(1000);
                    String code = getCaptchaCode(captchaUrl, param);
                    postPage(parameter, "sent=1&captcha=" + code);
                    if (!br.containsHTML(">Sicherheitscode nicht korrekt\\!<") && !br.containsHTML("captcha.php?id=")) {
                        break;
                    } else {
                        if (i >= 8) {
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        }
                    }
                    add += 500;
                }
            }
            String[] allLinks = br.getRegex("\"(https?://[^<>\"]*?)\" target=\"_blank\" style=\"border:0px;\">[\t\n\r ]+<img src=\"images/download_links_button\\.png\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Could not find the links...");
                return null;
            }
            for (String aLink : allLinks) {
                decryptedLinks.add(createDownloadlink(aLink));
            }
        } else if (parameter.matches(DECRYPTER_DDLMSC_MAIN)) {
            logger.info("The user added a DECRYPTER_DDLMSC_MAIN link...");
            getPage(parameter);
            String fpName = br.getRegex("<title>DDL\\-Music v3.0 // ([^<>\"]*?) // Download</title>").getMatch(0);
            if (br.containsHTML(">Download nicht gefunden")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String password = br.getRegex(Pattern.compile("<b>Passwort:</b> <i>(.*?)</i><br />", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password != null && password.contains("(kein Passwort)")) {
                password = null;
            }
            ArrayList<String> pwList = null;
            if (password != null) {
                pwList = new ArrayList<String>(Arrays.asList(new String[] { password.trim() }));
            }
            String allLinks[] = br.getRegex("\"(download/links/[a-z0-9]+/(mirror/\\d+/)?)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) {
                logger.warning("Couldn't find any links...");
                return null;
            }
            for (String singleLink : allLinks) {
                DownloadLink dLink = createDownloadlink("http://ddl-music.to/" + singleLink);
                if (pwList != null) {
                    dLink.setSourcePluginPasswordList(pwList);
                }
                decryptedLinks.add(dLink);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}