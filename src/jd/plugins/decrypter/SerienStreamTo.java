//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40121 $", interfaceVersion = 3, names = { "s.to" }, urls = { "https?://(?:www\\.)?s\\.to/[^/]+/.*" })
public class SerienStreamTo extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public SerienStreamTo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:").replaceAll("[/]+$", "");
        br.setFollowRedirects(true);
        final String page = br.getPage(parameter);
        final String[][] titleDetail = br.getRegex("<meta property=\"og:title\" content=\"(Episode \\d+\\s|Staffel \\d+\\s|von+\\s)+([^\"]+)\"/>").getMatches();
        final String title = (titleDetail.length > 0) ? (titleDetail[0][titleDetail[0].length - 1]) : null;
        final String itemTitle = new Regex(parameter, "https?://(?:www\\.)?s\\.to/[^/]+/[^/]+/(.*)").getMatch(0);
        // If we're on a show site, add the seasons, if we're on a season page, add the episodes and so on ...
        String[][] itemLinks = br.getRegex("href=\"([^\"]+" + Regex.escape(itemTitle) + "[^\"]+)\"").getMatches();
        for (String[] itemLink : itemLinks) {
            decryptedLinks.add(createDownloadlink(br.getURL(Encoding.htmlDecode(itemLink[0])).toString()));
        }
        // Videos are on external sites (not in embeds), so harvest those if we can get our hands on them.
        String[][] videoLinks = br.getRegex("itemprop=\"url\" href=\"([^\"]+redirect[^\"]+)\" target=\"_blank\"").getMatches();
        for (String[] videoLink : videoLinks) {
            final Browser br2 = br.cloneBrowser();
            String videoURL = br.getURL(Encoding.htmlDecode(videoLink[0])).toString();
            br2.setFollowRedirects(true);
            String redirectPage = br2.getPage(videoURL);
            if (br2.getRedirectLocation() != null) {
                videoURL = br2.getRedirectLocation();
            } else if (br2.containsHTML("grecaptcha")) {
                Form captcha = br2.getForm(0);
                String sitekey = new Regex(redirectPage, "grecaptcha.execute\\('([^']+)'").getMatch(0);
                String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, sitekey) {
                    @Override
                    public TYPE getType() {
                        return TYPE.INVISIBLE;
                    }
                }.getToken();
                captcha.put("original", "");
                captcha.put("token", Encoding.urlEncode(recaptchaV2Response));
                redirectPage = br2.submitForm(captcha);
                videoURL = br2.getURL().toString();
            }
            decryptedLinks.add(createDownloadlink(videoURL));
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title);
            filePackage.setProperty("ALLOW_MERGE", true);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}