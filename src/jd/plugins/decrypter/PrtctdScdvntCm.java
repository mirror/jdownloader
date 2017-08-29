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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protected.socadvnet.com" }, urls = { "http://(www\\.)?protected\\.socadvnet\\.com/\\?[a-z0-9-]+" })
public class PrtctdScdvntCm extends antiDDoSForDecrypt {

    private String  MAINPAGE = "http://protected.socadvnet.com/";
    private Browser xhr      = null;

    public PrtctdScdvntCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * At the moment this decrypter only decrypts: turbobit.net, hotfile.com links as "protected.socadvnet.com" only allows crypting links
     * of this host!
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        final String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) {
            return null;
        }
        getPage(parameter);
        if (br._getURL().getPath().equals("/index.php")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String cpPage = br.getRegex("\"(plugin/.*?)\"").getMatch(0);
        final String sendCaptcha = "/ksl.php";
        final String getList = "/llinks.php";
        xhrPostPage(getList, "LinkName=" + postvar);
        final String[] linksCount = xhr.getRegex("(moc\\.tenvdacos\\.detcetorp//:ptth)").getColumn(0);
        if (linksCount == null || linksCount.length == 0) {
            return null;
        }
        final int linkCounter = linksCount.length;
        if (cpPage != null) {
            for (int i = 0; i <= 3; i++) {
                final String equals = this.getCaptchaCode(MAINPAGE + cpPage, param);
                xhrPostPage(sendCaptcha, "res_code=" + equals);
                if (!br.toString().trim().equals("1") && !br.toString().trim().equals("0")) {
                    logger.warning("Error in doing the maths for link: " + parameter);
                    return null;
                }
                if (!br.toString().trim().equals("1")) {
                    continue;
                }
                break;
            }
            if (!br.toString().trim().equals("1")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        } else if (true) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            xhrPostPage(sendCaptcha, "recaptcha_code=" + Encoding.urlEncode(recaptchaV2Response));
        }
        logger.info("Found " + linkCounter + " links, decrypting now...");
        br.setFollowRedirects(false);
        for (int i = 0; i <= linkCounter - 1; i++) {
            final Browser br = this.br.cloneBrowser();
            final String actualPage = getList + "?out_name=" + postvar + "&&link_id=" + i;
            getPage(br, actualPage);
            if (br.containsHTML("This file is either removed due to copyright claim or is deleted by the uploader")) {
                logger.info("Found one offline link for link " + parameter + " linkid:" + i);
                continue;
            }
            String finallink = br.getRegex("http-equiv=\"refresh\" content=\"0;url=(http.*?)\"").getMatch(0);
            if (finallink == null) {
                // Handlings for more hosters will come soon i think
                if (br.containsHTML("turbobit\\.net")) {
                    final String singleProtectedLink = "/plugin/turbobit.net.free.php?out_name=" + postvar + "&link_id=" + i;
                    getPage(br, singleProtectedLink);
                    if (br.getRedirectLocation() == null) {
                        logger.warning("Redirect location for this link is null: " + parameter);
                        return null;
                    }
                    final String turboId = new Regex(br.getRedirectLocation(), "http://turbobit\\.net/download/free/(.+)").getMatch(0);
                    if (turboId == null) {
                        logger.warning("There is a problem with the link: " + br.getURL());
                        return null;
                    }
                    finallink = "http://turbobit.net/" + turboId + ".html";
                }
            }
            if (finallink == null) {
                logger.warning("Finallink for the following link is null: " + parameter);
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private void xhrPostPage(String page, String param) throws Exception {
        xhr = br.cloneBrowser();
        postPage(xhr, page, param);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}