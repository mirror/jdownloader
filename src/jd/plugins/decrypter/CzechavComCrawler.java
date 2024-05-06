//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.CzechavComConfigInterface;
import org.jdownloader.plugins.components.config.CzechavComConfigInterface.QualitySelectionFallbackMode;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.CzechavCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "czechav.com" }, urls = { "https?://czechav\\.com/members/gallery/([a-z0-9\\-]+)/" })
public class CzechavComCrawler extends PluginForDecrypt {
    public CzechavComCrawler(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    /* 2016-12-29: Prevent serverside IP ban. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        return crawlMedia(param, false);
    }

    public ArrayList<DownloadLink> crawlMedia(final CryptedLink param, final boolean crawlAll) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account == null) {
            throw new AccountRequiredException();
        }
        final CzechavCom hostPlugin = (CzechavCom) this.getNewPluginForHostInstance(this.getHost());
        hostPlugin.login(account, false);
        br.getPage(param.getCryptedUrl());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> selectedAndFoundItems = new ArrayList<DownloadLink>();
        final String fid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final CzechavComConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.CzechavComConfigInterface.class);
        String title = this.br.getRegex("<h1>([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            /* Fallback to id from inside url */
            title = fid.replace("-", " ").trim();
        }
        title = Encoding.htmlDecode(title).trim();
        final String[] videourls = br.getRegex("<option value=\"([^\"]+)\">[^<]*</option>").getColumn(0);
        final String[] videoheights = br.getRegex(">\\s*(\\d+)p[^<]*</option>").getColumn(0);
        final ArrayList<DownloadLink> allResults = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> bestResults = new ArrayList<DownloadLink>();
        final HashMap<String, List<DownloadLink>> qualities = new HashMap<String, List<DownloadLink>>();
        int index = -1;
        for (String videourl : videourls) {
            index++;
            videourl = br.getURL(Encoding.htmlOnlyDecode(videourl)).toExternalForm();
            String videoheightStr = new Regex(videourl, "x(\\d+)").getMatch(0);
            if (videoheightStr == null && videoheights != null && videoheights.length == videourls.length) {
                videoheightStr = videoheights[index];
            }
            if (videoheightStr == null) {
                logger.warning("Skipping unknown quality | URL: " + videourl);
                continue;
            }
            final String videoResolution = getVideoResolutionFromURL(videourl);
            final String qualityStringForFilename;
            if (videoResolution != null) {
                qualityStringForFilename = videoResolution;
            } else {
                qualityStringForFilename = videoheightStr;
            }
            final String ext = getFileNameExtensionFromURL(videourl, ".mp4");
            final DownloadLink link = new DownloadLink(hostPlugin, this.getHost(), this.getHost(), videourl, true);
            final String filenameFromURL = Plugin.extractFileNameFromURL(videourl);
            if (!StringUtils.isEmpty(filenameFromURL)) {
                link.setName(filenameFromURL);
            } else {
                link.setName(title + "_" + qualityStringForFilename + ext);
            }
            link.setProperty(CzechavCom.PROPERTY_VIDEO_HEIGHT, videoheightStr);
            link.setAvailable(true);
            allResults.add(link);
            List<DownloadLink> list = qualities.get(videoheightStr);
            if (list == null) {
                list = new ArrayList<DownloadLink>();
                qualities.put(videoheightStr, list);
            }
            list.add(link);
        }
        if (qualities.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (cfg.isCrawlImages() || crawlAll) {
            final String[] imageURLs = br.getRegex("class=\"open\" href=\"(https?://[^\"]+)").getColumn(0);
            if (imageURLs != null && imageURLs.length > 0) {
                int position = 0;
                for (final String imageURL : imageURLs) {
                    final DownloadLink image = new DownloadLink(hostPlugin, this.getHost(), this.getHost(), imageURL, true);
                    image.setAvailable(true);
                    image.setProperty(CzechavCom.PROPERTY_IMAGE_POSITION, position);
                    final String filenameFromURL = Plugin.extractFileNameFromURL(imageURL);
                    if (filenameFromURL != null) {
                        image.setName(Plugin.extractFileNameFromURL(imageURL));
                    }
                    selectedAndFoundItems.add(image);
                    allResults.add(image);
                    position++;
                }
            }
        }
        /* Pick qualities selected by user */
        if (qualities.containsKey("2160")) {
            if (cfg.isGrab2160pVideoEnabled()) {
                selectedAndFoundItems.addAll(qualities.get("2160"));
            }
            if (bestResults.isEmpty()) {
                bestResults.addAll(qualities.get("2160"));
            }
        }
        if (qualities.containsKey("1080")) {
            if (cfg.isGrab1080pVideoEnabled()) {
                selectedAndFoundItems.addAll(qualities.get("1080"));
            }
            if (bestResults.isEmpty()) {
                bestResults.addAll(qualities.get("1080"));
            }
        }
        if (qualities.containsKey("720")) {
            if (cfg.isGrab720pVideoEnabled()) {
                selectedAndFoundItems.addAll(qualities.get("720"));
            }
            if (bestResults.isEmpty()) {
                bestResults.addAll(qualities.get("720"));
            }
        }
        if (qualities.containsKey("540")) {
            if (cfg.isGrab540pVideoEnabled()) {
                selectedAndFoundItems.addAll(qualities.get("540"));
            }
            if (bestResults.isEmpty()) {
                bestResults.addAll(qualities.get("540"));
            }
        }
        if (qualities.containsKey("360")) {
            if (cfg.isGrab360pVideoEnabled()) {
                selectedAndFoundItems.addAll(qualities.get("360"));
            }
            if (bestResults.isEmpty()) {
                bestResults.addAll(qualities.get("360"));
            }
        }
        final ArrayList<DownloadLink> finalResults;
        if (crawlAll) {
            finalResults = allResults;
        } else {
            finalResults = new ArrayList<DownloadLink>();
            if (cfg.isGrabOtherResolutionsVideoEnabled()) {
                final Iterator<Entry<String, List<DownloadLink>>> it = qualities.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, List<DownloadLink>> next = it.next();
                    final int q = Integer.valueOf(next.getKey());
                    switch (q) {
                    case 2160:
                    case 1080:
                    case 720:
                    case 540:
                    case 360:
                        continue;
                    default:
                        selectedAndFoundItems.addAll(next.getValue());
                        break;
                    }
                }
            }
            if (cfg.isGrabBestVideoVersionEnabled()) {
                finalResults.addAll(bestResults);
            } else if (!selectedAndFoundItems.isEmpty()) {
                finalResults.addAll(selectedAndFoundItems);
            }
            if (finalResults.isEmpty()) {
                final QualitySelectionFallbackMode mode = cfg.getQualitySelectionFallbackMode();
                if (mode == QualitySelectionFallbackMode.BEST && bestResults.size() > 0) {
                    finalResults.addAll(bestResults);
                } else if (mode == QualitySelectionFallbackMode.ALL) {
                    finalResults.addAll(allResults);
                } else {
                    // QualitySelectionFallbackMode.NONE
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        for (final DownloadLink result : finalResults) {
            result._setFilePackage(fp);
            result.setContainerUrl(param.getCryptedUrl());
        }
        return finalResults;
    }

    public static String getVideoResolutionFromURL(final String url) {
        return new Regex(url, "(\\d+x\\d+)").getMatch(0);
    }

    @Override
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}