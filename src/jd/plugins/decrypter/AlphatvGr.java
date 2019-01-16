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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alphatv.gr" }, urls = { "https?://(?:www\\.)?alphatv\\.gr/shows?/.+" })
public class AlphatvGr extends PluginForDecrypt {
    public AlphatvGr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    // private boolean fastlinkcheck = true;
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        if (parameter.contains("?")) {
            /* For hostplugin */
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        jd.plugins.hoster.AlphatvGr.prepBR(this.br);
        // fastlinkcheck = JDUtilities.getPluginForHost(this.getHost()).getPluginConfig().getBooleanProperty("FAST_LINKCHECK", true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String linkpart = new Regex(parameter, "(/shows?.+)").getMatch(0);
        final String main_url_title = jd.plugins.hoster.AlphatvGr.getFilenameFromUrl(this.br.getURL());
        // final ConfirmDialog confirm = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, parameter, "For this URL JDownloader can crawl the
        // single video only or all related videos. What would you like to do?", null, "All videos AND the single video?", "Single Video?")
        // {
        // @Override
        // public ModalityType getModalityType() {
        // return ModalityType.MODELESS;
        // }
        //
        // @Override
        // public boolean isRemoteAPIEnabled() {
        // return true;
        // }
        // };
        // boolean decryptRelatedVideos = false;
        // try {
        // UIOManager.I().show(ConfirmDialogInterface.class, confirm).throwCloseExceptions();
        // decryptRelatedVideos = true;
        // } catch (DialogCanceledException e) {
        // decryptRelatedVideos = false;
        // } catch (DialogClosedException e) {
        // decryptRelatedVideos = false;
        // }
        boolean decryptRelatedVideos = true;
        if (decryptRelatedVideos) {
            final String showID = br.getRegex("window.Episodes.ShowId = (\\d+);").getMatch(0);
            final ArrayList<String> yearsDupeCheck = new ArrayList<String>();
            final String[] years = br.getRegex("\"CategoryId\":(\\d+)").getColumn(0);
            if (years == null || years.length == 0 || showID == null) {
                logger.info("Probably no downloadable content");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            /* 2018-11-15: Website default = 12 */
            final int maxItemsPerPage = 50;
            FilePackage fp = null;
            if (main_url_title != null) {
                fp = FilePackage.getInstance();
                fp.setName(main_url_title);
            }
            for (final String year : years) {
                if (yearsDupeCheck.contains(year)) {
                    continue;
                }
                logger.info("Decrypting category: " + year);
                /* Their counter starts at 1 */
                int page = 1;
                String[] videoItems = null;
                do {
                    if (this.isAbort()) {
                        return decryptedLinks;
                    }
                    logger.info(String.format("Crawling urls from page %d", page));
                    br.getPage("https://www.alphatv.gr/ajax/Isobar.AlphaTv.Components.Shows.Show.episodeslist?Key=" + year + "&Page=" + page + "&PageSize=" + maxItemsPerPage + "&ShowId=" + showID);
                    videoItems = br.getRegex("<div class=\"episodeItem flexClm4\">(.*?)</div>\\s*?</div>").getColumn(0);
                    for (final String videoItem : videoItems) {
                        String videoID = new Regex(videoItem, "new episodesContext\\(\\),\\{ id :(\\d+)\\}").getMatch(0);
                        if (videoID == null) {
                            videoID = new Regex(videoItem, "WebTvVideoId\\&quot;:(\\d+)").getMatch(0);
                        }
                        if (videoID == null) {
                            return null;
                        }
                        final String url = "https://www.alphatv.gr" + linkpart + "?vtype=episodes&vid=" + videoID + "&year=" + year + "&showId=" + showID;
                        final String episodeInfo = new Regex(videoItem, "<a class=\"openVideoPopUp\" onclick=\"[^\"]+\">([^<>\"]+)<").getMatch(0);
                        String title = jd.plugins.hoster.AlphatvGr.getFilenameFromUrl(url);
                        if (episodeInfo != null) {
                            title += episodeInfo.trim();
                        }
                        final DownloadLink dl = this.createDownloadlink(url);
                        if (fp != null) {
                            dl._setFilePackage(fp);
                        }
                        dl.setLinkID(videoID);
                        dl.setName(title + ".mp4");
                        dl.setAvailable(true);
                        /* This will go back into the decrypter. */
                        distribute(dl);
                    }
                    page++;
                } while (videoItems.length >= maxItemsPerPage);
            }
        }
        // /* First crawl the video the user initially added. */
        // crawlSingleVideo();
        // for (final String url : urlsToDecrypt) {
        // if (this.isAbort()) {
        // return decryptedLinks;
        // }
        // this.br.getPage(url);
        // crawlSingleVideo();
        // }
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
