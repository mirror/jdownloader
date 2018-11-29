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

@DecrypterPlugin(revision = "$Revision: 40115 $", interfaceVersion = 3, names = { "stream.to" }, urls = { "https?://(?:www\\.)?stream\\.to/(?:[a-z]+/)?(?:series|episode|movie)/.*" })
public class StreamTo extends PluginForDecrypt {
    private String[] videoDetail;

    public StreamTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String itemID = br.getRegex("https?://(?:www\\.)?stream\\.to/[a-z]+/[a-z]+/([0-9]+).*").getMatch(0);
        String fpName = br.getRegex("<title>([^<]+) auf [a-zA-Z]+\\.[a-zA-Z]+").getMatch(0);
        String[][] links = br.getRegex("<a title=\"[^\"]+\" href=\"([^\"]+)\" id=\"[^\"]+\" class=\"btn-eps ep-item\">").getMatches();
        // If there's no episode links, we'Re dealing with a single episode or movie and need to harvest the player embed HTML.
        if (links != null && links.length > 0) {
            for (String[] link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link[0])));
            }
        } else {
            String playerURL = "https://stream.to/ajax/load_player/" + itemID;
            Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            String playerHTML = br2.getPage(playerURL);
            String[][] videoDetails = br2.getRegex("<a title=\"[^\"]+\"[^\"]+href=\"[^\"]+\"[^\"]+id=\"([^\"]+)\"[^\"]+data-id=\"([^\"]+)\"[^\"]+data-server=\"([^\"]+)\"[^\"]+data-index=\"([^\"]+)\"[^\"]+class=\"btn-eps ep-item[^\"]*\">").getMatches();
            for (String[] videoDetail : videoDetails) {
                String videoOutURL = "/en/out/" + videoDetail[0];
                String videoHTML = br2.getPage(videoOutURL);
                if (br.getRedirectLocation() != null) {
                    links[links.length][0] = br.getRedirectLocation();
                } else {
                    videoHTML = videoHTML;
                    Form captcha = br2.getForm(0);
                    String sitekey = new Regex(videoHTML, "grecaptcha.execute\\('([^']+)'").getMatch(0);
                    if (sitekey != null) {
                        String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, sitekey) {
                            @Override
                            public TYPE getType() {
                                return TYPE.INVISIBLE;
                            }
                        }.getToken();
                        captcha.put("typ", "v3");
                        captcha.put("token", Encoding.urlEncode(recaptchaV2Response));
                        videoHTML = br2.submitForm(captcha);
                        captcha = br2.getForm(0);
                        String sitekey2 = new Regex(videoHTML, "'sitekey'\\: '([^']+)'").getMatch(0);
                        if (sitekey2 != null) {
                            recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, sitekey2) {
                                @Override
                                public TYPE getType() {
                                    return TYPE.INVISIBLE;
                                }
                            }.getToken();
                            captcha.put("typ", "v2");
                            captcha.put("token", Encoding.urlEncode(recaptchaV2Response));
                            videoHTML = br2.submitForm(captcha);
                            String videoURL = new Regex(videoHTML, "name=\"og:url\" content=\"([^\"]+)\">").getMatch(0);
                            if (videoURL != null) {
                                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(videoURL)));
                            }
                        }
                    }
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}