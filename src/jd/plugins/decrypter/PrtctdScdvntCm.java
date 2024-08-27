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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protected.socadvnet.com" }, urls = { "https?://(?:www\\.)?protected\\.socadvnet\\.com/\\?[a-z0-9\\-]+" })
public class PrtctdScdvntCm extends antiDDoSForDecrypt {
    private Browser xhr = null;

    public PrtctdScdvntCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * At the moment this crawler only crawls: turbobit.net, hotfile.com links as "protected.socadvnet.com" only allows crypting links of
     * this host!
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        final String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(parameter);
        if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("<title>\\s*(?:Links protector:)?\\s*(.*?)\\s*</title>").getMatch(0);
        final FilePackage fp;
        if (title != null) {
            fp = FilePackage.getInstance();
            fp.setName(title);
        } else {
            fp = null;
        }
        final String cpPage = br.getRegex("\"(plugin/.*?)\"").getMatch(0);
        final String sendCaptcha = "/ksl.php";
        final String getList = "/llinks.php";
        xhrPostPage(getList, "LinkName=" + postvar);
        String[] linksCount = xhr.getRegex("(ten\\.selifylf//:s?ptth)").getColumn(0);
        if (linksCount.length == 0) {
            linksCount = xhr.getRegex("(moc\\.tenvdacos\\.detcetorp//:s?ptth)").getColumn(0);
        }
        if (linksCount.length == 0) {
            /* 2022-05-24 */
            linksCount = xhr.getRequest().getHtmlCode().split("\\|");
        }
        if (linksCount == null || linksCount.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int linkCounter = linksCount.length;
        if (cpPage != null) {
            Browser r = xhr;
            for (int i = 0; i <= 3; i++) {
                final String equals = this.getCaptchaCode("/" + cpPage, param);
                r = xhrPostPage(sendCaptcha, "res_code=" + equals);
                if (!r.toString().trim().equals("1") && !r.toString().trim().equals("0")) {
                    logger.warning("Error in doing the maths for link: " + parameter);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!r.toString().trim().equals("1")) {
                    continue;
                } else {
                    break;
                }
            }
            if (!xhr.toString().trim().equals("1")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else if (true) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            final Browser r = xhrPostPage(sendCaptcha, "recaptcha_code=" + Encoding.urlEncode(recaptchaV2Response));
            if (!r.toString().trim().equals("1")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        for (int i = 0; i <= linkCounter - 1; i++) {
            final Browser brc = br.cloneBrowser();
            final String actualPage = getList + "?out_name=" + postvar + "&&link_id=" + i;
            logger.info("Crawling item " + (i + 1) + "/" + linkCounter + " | " + actualPage);
            getPage(brc, actualPage);
            if (brc.containsHTML("(?i)This file is either removed due to copyright claim or is deleted by the uploader")) {
                logger.info("Found one offline link for link " + parameter + " linkid:" + i);
                continue;
            }
            String finallink = brc.getRegex("http-equiv\\s*=\\s*\"refresh\" content\\s*=\\s*\"0;url\\s*=\\s*(https?[^\"]+)\"").getMatch(0);
            if (finallink == null) {
                // Handlings for more hosters will come soon i think
                if (brc.containsHTML("turbobit\\.net")) {
                    final String singleProtectedLink = "/plugin/turbobit.net.free.php?out_name=" + postvar + "&link_id=" + i;
                    getPage(brc, singleProtectedLink);
                    if (brc.getRedirectLocation() == null) {
                        logger.warning("Redirect location for this link is null: " + parameter);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String turboId = new Regex(brc.getRedirectLocation(), "https?://turbobit\\.net/download/free/(.+)").getMatch(0);
                    if (turboId == null) {
                        logger.warning("There is a problem with the link: " + brc.getURL());
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    finallink = "https://turbobit.net/" + turboId + ".html";
                }
            }
            if (finallink == null) {
                logger.warning("Finallink for the following link is null: " + parameter);
            }
            final DownloadLink link = createDownloadlink(finallink);
            if (fp != null) {
                fp.add(link);
            }
            ret.add(link);
            distribute(link);
            if (this.isAbort()) {
                break;
            }
        }
        return ret;
    }

    private Browser xhrPostPage(String page, String param) throws Exception {
        xhr = br.cloneBrowser();
        postPage(xhr, page, param);
        return xhr;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-03-24 */
        return 1;
    }
}