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
import java.util.regex.Matcher;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentai.animestigma.com" }, urls = { "https?://(?:www\\.)?hentai\\.animestigma\\.com/([a-z0-9\\-]{2,})/?" })
public class HentaiAnimestigmaComCrawler extends antiDDoSForDecrypt {
    public HentaiAnimestigmaComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected DownloadLink createDownloadlink(String url, String title) {
        final String ext = ".mp4";
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setName(title + ext);
        dl.setAvailable(true);
        dl.setProperty("mainlink", br.getURL());
        return dl;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (handleEmbedSingle(ret, contenturl) == false) {
            if (contenturl.contains("hentai-list-a-z") || contenturl.contains("3d-hentai-list-a-z")) {
                handleEmbedList(ret);
            } else if (br.toString().contains("You must be login to submit genre tags")) {
                handleEmbedFinal(ret);
            } else {
                logger.warning("Decrypter broken for link: " + contenturl);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (ret.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private boolean handleEmbedSingle(final ArrayList<DownloadLink> ret, String link) {
        String downloadtitle = br.getRegex("rel=\"bookmark\" title=\"([^\"]+)\">").getMatch(0);
        String downloadlink = br.getRegex("<iframe src=\"([^\"]+)\" frameborder=\"0\" scrolling=\"no\"").getMatch(0);
        if (downloadlink == null || downloadtitle == null || link == null) {
            return false;
        }
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            for (int i = 0; i <= 3; i++) {
                con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(downloadlink));
                if (con.getResponseCode() == 503) {
                    logger.info("Retry on error 503");
                    continue;
                } else {
                    /* Success */
                    break;
                }
            }
            final String contentType = con.getContentType();
            if (con.isOK() && StringUtils.containsIgnoreCase(contentType, "text/html")) {
                brc.followConnection();// follow/finish head request
                brc.getPage(downloadlink);
                final String realdownloadlink = brc.getRegex("<source src=\"([^\"]+)\" type=\"video/mp4\">").getMatch(0);
                if (realdownloadlink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                downloadlink = realdownloadlink;
            }
        } catch (Exception e) {
            logger.log(e);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        ret.add(this.createDownloadlink(Encoding.htmlOnlyDecode(downloadlink) + "#hentai.animestigma.com-direct", Encoding.htmlOnlyDecode(downloadtitle)));
        return true;
    }

    private void handleEmbedList(final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        Matcher nextpage2 = br.getRegex("<a href=\"([^\"]+)\">([^<]+)</a>").getMatcher();
        while (nextpage2.find()) {
            if (nextpage2.group(1) != null && nextpage2.group(2) != null) {
                getPage(nextpage2.group(1));
                handleEmbedFinal(decryptedLinks);
            }
        }
        return;
    }

    private void handleEmbedFinal(final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        Matcher nextpage3 = br.getRegex("<a href=\"([^\"]+)\" rel=\"bookmark\" title=\"([^\"]+)\">").getMatcher();
        while (nextpage3.find()) {
            if (nextpage3.group(1) != null && nextpage3.group(2) != null) {
                getPage(nextpage3.group(1));
                handleEmbedSingle(decryptedLinks, nextpage3.group(1));
            }
        }
        return;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2024-05-27: Prevent running into http response 503 */
        return 1;
    }
}
