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
package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video.yandex.ru" }, urls = { "http://video\\.yandex\\.ru/(iframe/[A-Za-z0-9]+/[A-Za-z0-9]+\\.\\d+|users/[A-Za-z0-9]+/view/\\d+)" })
public class VideoYandexRu extends PluginForHost {
    public VideoYandexRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://yandex.ru/";
    }

    private static final String NORESUME        = "NORESUME";
    private static final String TYPE_VIDEO_USER = "https?://video\\.yandex\\.ru/users/[A-Za-z0-9]+/view/\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        br.setFollowRedirects(true);
        String filename;
        long filesize_long = -1;
        String final_filename = link.getStringProperty("plain_filename", null);
        if (final_filename == null) {
            final_filename = link.getFinalFileName();
        }
        getPage(link.getPluginPatternMatcher());
        if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_USER)) {
            /* offline|empty|enything else (e.g. abuse) */
            if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Ролик не найден</title>|>Здесь пока пусто<|class=\"error\\-container\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String iframe_url = br.getRegex("property=\"og:video:ifrаme\" content=\"(https?://video\\.yandex\\.ru/iframe/[^<>\"]*?)\"").getMatch(0);
            if (iframe_url == null) {
                iframe_url = br.getRegex("class=\"video\\-frame\"><iframe src=\"(//video\\.yandex\\.ru/[^<>\"]*?)\"").getMatch(0);
            }
            if (iframe_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!iframe_url.startsWith("http:") && !iframe_url.startsWith("https:")) {
                iframe_url = "http:" + iframe_url;
            }
            link.setUrlDownload(iframe_url);
            getPage(iframe_url);
        }
        if (br.containsHTML("<title>Яндекс\\.Видео</title>") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = br.getRegex("<title>([^<>]*?) — Яндекс\\.Видео</title>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
        filename = encodeUnicode(filename);
        link.setName(filename);
        if (filesize_long > -1) {
            link.setDownloadSize(filesize_long);
        }
        return AvailableStatus.TRUE;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://disk.yandex.com/", "ys", "");
        br.setAllowedResponseCodes(new int[] { 429 });
        return br;
    }

    private void getPage(final String url) throws IOException {
        getPage(this.br, url);
    }

    public static void getPage(final Browser br, final String url) throws IOException {
        br.getPage(url);
        /* 2017-03-30: New */
        final String jsRedirect = br.getRegex("(https?://[^<>\"]+force_show=1[^<>\"]*?)").getMatch(0);
        if (jsRedirect != null) {
            br.getPage(jsRedirect);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        doFree(link, null);
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account);
        handleDownloadVideo(link);
    }

    @SuppressWarnings("deprecation")
    private void handleDownloadVideo(final DownloadLink link) throws Exception {
        final String linkpart = new Regex(link.getDownloadURL(), "/iframe/(.+)").getMatch(0);
        final String width = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        final String height = br.getRegex("width\\&quot;:\\&quot;(\\d+)\\&quot;").getMatch(0);
        String file = br.getRegex("\\&quot;file\\&quot;:\\&quot;([a-z0-9]+)\\&quot;").getMatch(0);
        if (file == null) {
            file = br.getRegex("name=\"twitter:image\" content=\"https?://static\\.video\\.yandex.ru/get/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_\\.]+/([A-Za-z0-9]+)\\.jpg\"").getMatch(0);
        }
        if (file == null && (width != null && height != null)) {
            file = "m" + width + "x" + height + ".flv";
            link.setFinalFileName(link.getName().replace(".mp4", ".flv"));
        } else if (file == null) {
            file = "0.flv";
            link.setFinalFileName(link.getName().replace(".mp4", ".flv"));
        } else {
            file += ".mp4";
            link.setFinalFileName(link.getName().replace(".flv", ".mp4"));
        }
        getPage("http://static.video.yandex.net/get-token/" + linkpart + "?nc=0." + System.currentTimeMillis());
        final String token = br.getRegex("<token>([^<>\"]*?)</token>").getMatch(0);
        if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage("http://streaming.video.yandex.ru/get-location/" + linkpart + "/" + file + "?token=" + token + "&ref=video.yandex.ru");
        String dllink = br.getRegex("<video\\-location>(https?://[^<>\"]*?)</video\\-location>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            handleServerErrors(link);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private void handleServerErrors(final DownloadLink link) throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            link.setChunksProgress(null);
            link.setProperty(VideoYandexRu.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    public static String getHashLongFromHTML(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "public_key");
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    /** Gets new 'SK' value via '/auth/status' request. */
    public static String getNewSK(final Browser br, final String domain, final String sourceURL) throws IOException {
        br.getPage("https://" + domain + "/auth/status?urlOrigin=" + Encoding.urlEncode(sourceURL) + "&source=album_web_signin");
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}