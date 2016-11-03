//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 20515 $", interfaceVersion = 3, names = { "kissanime.com", "kissasian.com", "kisscartoon.me" }, urls = { "https?://(?:www\\.)?kissanime\\.(?:com|to)/anime/[a-zA-Z0-9\\-\\_]+/[a-zA-Z0-9\\-\\_]+(?:\\?id=\\d+)?", "http://kissasian\\.com/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?", "http://kisscartoon\\.me/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?" })
public class KisAmeCm extends antiDDoSForDecrypt {

    public KisAmeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        handleHumanCheck(this.br);
        String title = br.getRegex("<title>\\s*(.*?)\\s*- Watch\\s*\\1[^<]*</title>").getMatch(0);
        if (title == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        title = title.replaceAll("\\s+", " ");
        // we have two things we need to base64decode
        final String[][] quals = getQuals(this.br);
        if (quals != null) {
            for (final String qual[] : quals) {
                String decode = decodeSingleURL(qual[1]);
                final String quality = qual[2];
                final DownloadLink dl = createDownloadlink(decode);
                /* md5 of "kissanime.com" */
                dl.setProperty("source_plugin_b64", "a2lzc2FuaW1lLmNvbQ==");
                dl.setProperty("source_url", parameter);
                dl.setProperty("source_quality", quality);
                dl.setFinalFileName(title + "-" + quality + ".mp4");
                dl.setAvailableStatus(AvailableStatus.TRUE);
                decryptedLinks.add(dl);
            }
        } else {
            // iframed.. seen openload.. but maybe others
            final String link = br.getRegex("\\$\\('#divContentVideo'\\)\\.html\\('<iframe\\s+[^>]* src=\"(.*?)\"").getMatch(0);
            if (link != null) {
                decryptedLinks.add(createDownloadlink(link));
            }

        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String[][] getQuals(final Browser br) {
        String[][] quals = null;
        final String qualityselection = br.getRegex("<select id=\"selectQuality\">.*?</select").getMatch(-1);
        if (qualityselection != null) {
            quals = new Regex(qualityselection, "<option [^>]*value\\s*=\\s*('|\"|)(.*?)\\1[^>]*>(\\d+p)").getMatches();
        }
        return quals;
    }

    public static String decodeSingleURL(final String encodedString) throws IOException {
        String decode = Encoding.Base64Decode(encodedString);
        if (StringUtils.contains(decode, "blogspot.com/")) {
            // this is redirect bullshit
            final Browser test = new Browser();
            test.getPage(decode);
            decode = test.getRedirectLocation();
        }
        return decode;
    }

    public static boolean isOffline(final Browser br) {
        return br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404;
    }

    private void handleHumanCheck(final Browser br) throws IOException, PluginException, InterruptedException, DecrypterException {
        final Form ruh = br.getFormbyAction("/Special/AreYouHuman");
        // recaptchav2 event can happen here
        if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || ruh != null) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            ruh.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(ruh);
        }
    }

    public final String refreshDirecturl(final DownloadLink dl, final Browser br) throws Exception {
        String directlink = null;
        final String source_url = dl.getStringProperty("source_url", null);
        final String source_quality = dl.getStringProperty("source_quality", null);
        if (source_url == null || source_quality == null) {
            return null;
        }
        getPage(br, source_url);
        if (isOffline(br)) {
            return null;
        }
        handleHumanCheck(br);
        /* Find new directlink for original quality */
        final String[][] quals = getQuals(br);
        if (quals != null) {
            for (final String qual[] : quals) {
                final String quality = qual[2];
                if (!quality.equalsIgnoreCase(source_quality)) {
                    continue;
                }
                directlink = decodeSingleURL(qual[1]);
                break;
            }
        }
        return directlink;
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}