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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alphatv.gr" }, urls = { "https?://(?:www\\.)?alphatv\\.gr/shows/.+/[A-Za-z0-9\\-_]+" })
public class AlphatvGr extends PluginForDecrypt {

    public AlphatvGr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    // private boolean fastlinkcheck = true;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<String> urlsToDecrypt = new ArrayList<String>();
        final String parameter = param.toString();
        jd.plugins.hoster.AlphatvGr.prepBR(this.br);
        // fastlinkcheck = JDUtilities.getPluginForHost(this.getHost()).getPluginConfig().getBooleanProperty("FAST_LINKCHECK", true);
        br.getPage(parameter);

        final String linkpart = new Regex(parameter, "(/shows.+/)[^/]+").getMatch(0);
        final String main_url_title = jd.plugins.hoster.AlphatvGr.getFilenameFromUrl(this.br.getURL());

        short page_max = 0;
        final String[] pages = this.br.getRegex("\\?page=(\\d+)\">\\d+</a>").getColumn(0);
        for (final String page_temp_str : pages) {
            final short page_temp = Short.parseShort(page_temp_str);
            if (page_temp > page_max) {
                page_max = page_temp;
            }
        }

        final Browser br2 = this.br.cloneBrowser();
        final String base_url = this.br.getURL();
        short addedUrlsNum;
        int currentPage = 0;
        do {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            logger.info(String.format("Crawling urls from page %d of  (?) %d", currentPage, page_max));
            if (currentPage > 0) {
                br2.getPage(base_url + "?page=" + currentPage);
            }
            final String[] otherVideos = br2.getRegex("<article><a[^<>]*?class=\"link\"[^<>]*?href=\"(" + linkpart + "[^<>\"]+)\">").getColumn(0);
            for (String otherVideoUrl : otherVideos) {
                final String otherVideoUrlTitle = jd.plugins.hoster.AlphatvGr.getFilenameFromUrl(otherVideoUrl);
                if (main_url_title != null && otherVideoUrlTitle != null && otherVideoUrlTitle.equalsIgnoreCase(main_url_title)) {
                    /* Prevent re-adding the url which the user has just added. */
                    continue;
                }
                otherVideoUrl = "http://www." + this.getHost() + otherVideoUrl;
                /* This will go back into the decrypter. */
                urlsToDecrypt.add(otherVideoUrl);
            }
            addedUrlsNum = (short) otherVideos.length;
            currentPage++;
            /*
             * Important! Stop once a page has less than the max number of videos because if we go to far the website will just show more
             * random other videos and we can go until page ~2000.
             */
        } while (addedUrlsNum >= 12);

        /* First crawl the video the user initially added. */
        crawlSingleVideo();

        for (final String url : urlsToDecrypt) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            this.br.getPage(url);
            crawlSingleVideo();
        }

        return decryptedLinks;
    }

    private void crawlSingleVideo() {
        /* First check for embedded content e.g. YouTube. */
        final String externID = this.br.getRegex("<embed\\s*?src=\"(http[^<>\"]*?)\"").getMatch(0);
        final String otherVideoUrlTitle = jd.plugins.hoster.AlphatvGr.getFilenameFromUrl(this.br.getURL());
        final DownloadLink dl;
        if (externID != null) {
            dl = createDownloadlink(externID);
        } else {
            dl = this.createDownloadlink(this.br.getURL().replace("alphatv.gr/", "alphatvdecrypted.gr/"));
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            if (jd.plugins.hoster.AlphatvGr.isOffline(this.br)) {
                dl.setAvailable(false);
                if (otherVideoUrlTitle != null) {
                    dl.setName(otherVideoUrlTitle);
                }
            } else {
                // if (fastlinkcheck) {
                // dl.setAvailable(true);
                // }
                dl.setName(jd.plugins.hoster.AlphatvGr.getFilename(this.br));
                dl.setAvailable(true);
            }
        }
        decryptedLinks.add(dl);
        distribute(dl);
    }

}
