//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "url-protection.com" }, urls = { "http://(www\\.)?url\\-protection\\.com/(linkidwoc|)\\.php\\?linkid=[a-z0-9]+" }, flags = { 0 })
public class UrlProtectionCom extends PluginForDecrypt {

    public UrlProtectionCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DecrypterScript_linkid=_linkcheck.php Version 0.2 */

    private static final String  RECAPTCHATEXT = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/challenge";
    private static final boolean SKIP_CAPTCHA  = false;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("linkidwoc.php", "linkcheck.php");
        final String domain = new Regex(parameter, "http://(www\\.)?([^<>\"/]*?)/").getMatch(1);
        final String linkid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        /* In case there is a captcha: Try to prefer captcha by adding "&c=1" to the url */
        br.getPage(parameter);

        if (!SKIP_CAPTCHA && br.containsHTML(RECAPTCHATEXT)) {
            boolean failed = true;
            for (int i = 0; i <= 5; i++) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                final String postData = "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&x=" + Integer.toString(new Random().nextInt(100)) + "&y=" + Integer.toString(new Random().nextInt(100)) + "&captcha=recaptcha&mon_captcha=" + linkid;
                br.postPage("http://" + domain + "/showlinks.php", postData);
                if (br.containsHTML(">Captcha erroné vous allez être rediriger")) {
                    /* In case there is a captcha: Try to prefer captcha by adding "&c=1" to the url */
                    br.getPage(parameter);
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }

        br.postPage("http://" + domain + "/linkid.php", "linkid=" + linkid + "&x=" + Integer.toString(new Random().nextInt(100)) + "&y=" + Integer.toString(new Random().nextInt(100)));
        String fpName = br.getRegex("<div align=\"center\">([^<>\"]*?)</div>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("(Titre|Title):[\t\n\r ]+</td>[\t\n\r ]+<td style='[^<>\"]+'>([^<>\"]*?)</td>").getMatch(1);
        }
        if (fpName == null) {
            fpName = linkid;
        }
        final String tabletext = br.getRegex("class='all_liens'(.*?)</table>").getMatch(0);
        if (tabletext == null) {
            logger.warning("Could not find link-table: " + parameter);
            return null;
        }
        String[] links = new Regex(tabletext, "href=(http[^<>\"]*?)>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            if (!singleLink.contains(domain)) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}