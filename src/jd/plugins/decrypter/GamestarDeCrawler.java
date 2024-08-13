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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gamestar.de" }, urls = { "https?://(www\\.)?gamestar\\.de/videos/.+" })
public class GamestarDeCrawler extends PluginForDecrypt {
    public GamestarDeCrawler(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String videoURLs[] = br.getRegex("'(/videos/media.*?)'").getColumn(0);
        if (videoURLs == null || videoURLs.length == 0) {
            videoURLs = br.getRegex("\"(/videos/media.*?)\"").getColumn(0);
        }
        final String title = br.getRegex("og:title\"\\s*content=\"(.*?)\"").getMatch(0);
        int position = 0;
        if (videoURLs != null && videoURLs.length > 0) {
            final Browser brc = br.cloneBrowser();
            final HashSet<String> dupes = new HashSet<String>();
            for (final String videoURL : videoURLs) {
                if (!dupes.add(videoURL)) {
                    /* Skip duplicates */
                    continue;
                }
                position++;
                logger.info("Crawling item " + position + "/" + videoURLs.length);
                brc.setFollowRedirects(false);
                brc.getPage(videoURL);
                final String url = brc.getRedirectLocation();
                if (url != null) {
                    ret.add(createDownloadlink(url));
                } else {
                    final String contentURL = brc.getRegex("content url=\"(https?://.*?)\"\\s*type=\"video").getMatch(0);
                    if (contentURL != null) {
                        ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(contentURL)));
                    }
                }
                if (this.isAbort()) {
                    break;
                }
            }
        }
        final String[] dailymotionVideoIDs = br.getRegex("\"dmId\":\"(\\w+)").getColumn(0);
        if (dailymotionVideoIDs != null && dailymotionVideoIDs.length > 0) {
            for (final String dailymotionVideoID : dailymotionVideoIDs) {
                ret.add(this.createDownloadlink("https://www.dailymotion.com/video/" + dailymotionVideoID));
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}