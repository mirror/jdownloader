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

import java.net.URL;
import java.util.ArrayList;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.AventertainmentsCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "aventertainments.com" }, urls = { "https?://(?:www\\.)?aventertainments\\.com/.+" })
public class AventertainmentsComCrawler extends PluginForDecrypt {
    public AventertainmentsComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        AventertainmentsCom.prepBR(br);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final AventertainmentsCom plg = (AventertainmentsCom) this.getNewPluginForHostInstance(this.getHost());
        if (plg.canHandle(param.getCryptedUrl())) {
            /* URL needs to be processed by host plugin. */
            final DownloadLink forHostPlugin = this.createDownloadlink(param.getCryptedUrl());
            ret.add(forHostPlugin);
            return ret;
        }
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            /* Login whenever possible */
            plg.login(account, false);
        }
        br.setFollowRedirects(true);
        /* Allow for any direct-URLs added by user. */
        final URLConnectionAdapter con = br.openGetConnection(param.getCryptedUrl());
        if (this.looksLikeDownloadableContent(con)) {
            try {
                con.disconnect();
            } catch (final Throwable ignore) {
            }
            final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(con.getRequest(), con);
            ret.add(direct.getDownloadLink());
            return ret;
        } else {
            br.followConnection();
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("/login.aspx")) {
            /**
             * This can even happen if account is given because: </br>
             * - maybe it's only a free account </br>
             * - maybe content needs to be purchased separately
             */
            throw new AccountRequiredException();
        } else if (br.getURL().contains("ppv/Streaming.aspx")) {
            logger.info("DRM protected paid content (encrypted HLS)");
            final DownloadLink offline = this.createOfflinelink(param.getCryptedUrl(), "DRM_PROTECTED_" + br._getURL().getFile(), "DRM protected content is not supported");
            ret.add(offline);
            return ret;
        }
        String fpName;
        final String title_part1 = this.br.getRegex("class=\"top\\-title\">Item #:([^<>\"]+)</div>").getMatch(0);
        final String title_part2 = this.br.getRegex("<h2>([^<>\"]+)(?:\\&nbsp;)?<").getMatch(0);
        if (title_part1 != null && title_part2 != null) {
            fpName = title_part1.trim() + " - " + title_part2.trim();
        } else {
            fpName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        }
        if (fpName != null) {
            /* Clean packagename */
            final String rubbish = new Regex(fpName, "(?i)((\\s*? \\|)?\\s*?AVEntertainments?(\\s*:.+|\\.com))").getMatch(0);
            if (rubbish != null) {
                fpName = fpName.replace(rubbish, "");
            }
        }
        boolean foundScreenshot = false;
        final String screenshot_url_part = this.br.getRegex("imgs\\.aventertainments\\.com/new/bigcover/([A-Za-z0-9\\-_]+\\.jpg)\"").getMatch(0);
        /*
         * 2021-07-21: Removed this screenshot RegEx as it will pickup screenshots of "related" videos:
         * (https?://imgs\\d*\\.aventertainments\\.com/[^/]+/screen_shot/[^<>\"\\']+\\.jpg)
         */
        final String allowedImageFileExtensions = "(jpg|webp)";
        final String[] screenshotRegexes = { "(https?://imgs\\d*\\.aventertainments\\.com/(?:[a-z0-9]+)?/?vodimages/screenshot/large/[^<>\"\\']+\\." + allowedImageFileExtensions + ")" };
        final String[] galleryRegexes = { "(https?://imgs\\d*\\.aventertainments\\.com/(?:[a-z0-9]+)?/vodimages/gallery/large/[^<>\"\\']+\\." + allowedImageFileExtensions + ")" };
        final String[] coverRegexes = { "\"(https?://imgs\\d*\\.aventertainments\\.com/(?:[a-z0-9]+)?/bigcover/[^/]+\\." + allowedImageFileExtensions + ")\"" };
        for (final String screenshotRegex : screenshotRegexes) {
            final String[] screenshots = br.getRegex(screenshotRegex).getColumn(0);
            if (screenshots != null && screenshots.length > 0) {
                foundScreenshot = true;
                for (final String singleLink : screenshots) {
                    final DownloadLink dl = createDownloadlink(singleLink);
                    final String filename_url = getFileNameFromURL(new URL(singleLink));
                    if (filename_url != null) {
                        final String filenamePrefix = new Regex(singleLink, "/large/([^/]+)/").getMatch(0);
                        final String filename;
                        if (filenamePrefix != null) {
                            /* 2021-07-26: Add extra prefix to prevent duplicated filenames for different files. */
                            filename = "screenshot_" + filenamePrefix + "_" + filename_url;
                        } else {
                            filename = "screenshot_" + filename_url;
                        }
                        dl.setFinalFileName(filename);
                    }
                    dl.setProperty("type", "screenshot");
                    dl.setAvailable(true);
                    ret.add(dl);
                }
            }
        }
        for (final String galleryRegex : galleryRegexes) {
            final String[] galleryImages = br.getRegex(galleryRegex).getColumn(0);
            if (galleryImages != null && galleryImages.length > 0) {
                for (final String singleLink : galleryImages) {
                    final DownloadLink dl = createDownloadlink(singleLink);
                    final String filename_url = getFileNameFromURL(new URL(singleLink));
                    if (filename_url != null) {
                        final String filename = "gallery_" + filename_url;
                        dl.setFinalFileName(filename);
                    }
                    dl.setProperty("type", "gallery");
                    dl.setAvailable(true);
                    ret.add(dl);
                }
            }
        }
        for (final String coverRegex : coverRegexes) {
            final String[] coverImages = br.getRegex(coverRegex).getColumn(0);
            if (coverImages != null && coverImages.length > 0) {
                for (final String singleLink : coverImages) {
                    final DownloadLink dl = createDownloadlink(singleLink);
                    final String filename_url = getFileNameFromURL(new URL(singleLink));
                    if (filename_url != null) {
                        final String filename = "cover_" + filename_url;
                        dl.setFinalFileName(filename);
                    }
                    dl.setProperty("type", "cover");
                    dl.setAvailable(true);
                    ret.add(dl);
                }
            }
        }
        if (!foundScreenshot && screenshot_url_part != null) {
            /* E.g. for DVDs these screenshots are officially (without logging in) not available --> We can work around this limitation */
            final String screenshot_directurl = "http://imgs.aventertainments.com/new/screen_shot/" + screenshot_url_part;
            final DownloadLink dl = createDownloadlink(screenshot_directurl);
            final String filename_url = getFileNameFromURL(new URL(screenshot_directurl));
            if (filename_url != null) {
                final String filename = "screenshot_" + filename_url;
                dl.setFinalFileName(filename);
            }
            dl.setProperty("type", "screenshot");
            dl.setAvailable(true);
            ret.add(dl);
        }
        /*
         * 2021-07-16: User has to enter a captcha to download those URLs now. This captcha does not even work via browser --> Prefer
         * stream-download over official download
         */
        final String[] officialVideoDownloads = br.getRegex("(https?://(?:www\\.)?aventertainments\\.com/newdlsample\\.aspx[^<>\"\\']+\\.mp4)").getColumn(0);
        String urlStream = this.br.getRegex("src[\t\n\r ]*?:[\t\n\r ]*?\"(http[^\"]*?\\.m3u8[^\"]*?)\"").getMatch(0);
        if (urlStream == null) {
            /* 2021-07-16 */
            urlStream = this.br.getRegex("<source src=\"(https://[^\"]+\\.m3u8)\"").getMatch(0);
        }
        if (urlStream != null) {
            logger.info("Stream download available");
            /* Replace '.m3u8' with '.m3u9' to prevent generic HLS decrypter from picking this up first! */
            final DownloadLink dl = this.createDownloadlink(urlStream.replace(".m3u8", ".m3u9"));
            dl.setContentUrl(urlStream);
            dl.setProperty("type", "video_stream");
            dl.setFinalFileName(fpName + ".mp4");
            dl.setAvailable(true);
            ret.add(dl);
        } else if (officialVideoDownloads.length > 0) {
            logger.info("Found " + officialVideoDownloads.length + " official video download(s)");
            for (final String singleLink : officialVideoDownloads) {
                final DownloadLink dl = createDownloadlink(singleLink);
                dl.setProperty("type", "video");
                ret.add(dl);
            }
        } else {
            /* Plugin broken or content offline or maybe no video available at all but only covers/images */
            logger.warning("Failed to find any results");
        }
        /* Add some plugin properties */
        for (final DownloadLink result : ret) {
            result.setProperty("mainlink", param.getCryptedUrl());
            result.setContentUrl(param.getCryptedUrl());
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }
}
