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
import java.util.List;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VideoFc2ComProfileCrawler extends PluginForDecrypt {
    public VideoFc2ComProfileCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "video.fc2.com" });
        ret.add(new String[] { "video.laxd.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:a/)?account/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String userID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl() + "/content");
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String username = br.getRegex("class=\"memberName\"[^<]*>([^<]+)<").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (username != null) {
            username = Encoding.htmlDecode(username).trim();
            fp.setName(username);
        } else {
            /* Fallback */
            fp.setName(userID);
        }
        final String expectedNumberofItemsStr = br.getRegex("class=\"each_btm_number\">(\\d+)</span>").getMatch(0);
        final int expectedNumberofItems;
        final String expectedNumberofItemsHumanReadable;
        if (expectedNumberofItemsStr != null) {
            expectedNumberofItemsHumanReadable = expectedNumberofItemsStr;
            expectedNumberofItems = Integer.parseInt(expectedNumberofItemsStr);
        } else {
            expectedNumberofItemsHumanReadable = "??";
            expectedNumberofItems = -1;
        }
        if (expectedNumberofItems == 0) {
            logger.info("Empty profile");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final HashSet<String> dupes = new HashSet<String>();
        int page = 1;
        do {
            final String[] videourls = br.getRegex("(https?://[^/]+/(a/)?content/[A-Za-z0-9]+)").getColumn(0);
            if (videourls == null || videourls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int numberofNewItemsOnThisPage = 0;
            for (final String videourl : videourls) {
                if (!dupes.add(videourl)) {
                    /* Skip dupes */
                    continue;
                }
                numberofNewItemsOnThisPage++;
                final DownloadLink link = createDownloadlink(videourl);
                String videoTitle = br.getRegex(Regex.escape(videourl) + "\"[^<]*class=\"c-boxList-111_video_ttl\">([^<]+)</a>").getMatch(0);
                if (videoTitle != null) {
                    videoTitle = Encoding.htmlDecode(videoTitle).trim();
                    link.setName(fp.getName() + "_" + videoTitle + ".mp4");
                }
                link.setAvailable(true);
                link._setFilePackage(fp);
                decryptedLinks.add(link);
                distribute(link);
            }
            if (numberofNewItemsOnThisPage == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            }
            logger.info("Crawled page " + page + " | Found items so far: " + decryptedLinks.size() + "/" + expectedNumberofItemsHumanReadable + " | On this page: " + numberofNewItemsOnThisPage);
            page++;
            final String nextPageURL = br.getRegex("\"([^\"]+\\?page=" + page + ")").getMatch(0);
            if (nextPageURL == null) {
                logger.info("Stopping because: Reached end");
                break;
            }
            br.getPage(nextPageURL);
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
