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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "yadi.sk" }, urls = { "https://yadi\\.sk/a/[A-Za-z0-9\\-_]+/([a-f0-9]{24})" })
public class YandexAlbum extends PluginForHost {
    public YandexAlbum(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://yadi.sk/";
    }

    private static final String NORESUME                    = "NORESUME";
    /* Some constants which they used in browser */
    public static final String  CLIENT_ID                   = "2784000881524006529778";
    public static String        VERSION_YANDEX_PHOTO_ALBUMS = "77.3";
    public static final String  PROPERTY_HASH               = "hash_main";

    @Override
    public String getLinkID(final DownloadLink link) {
        return new Regex(this.getHost() + "://" + link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        br.setFollowRedirects(true);
        String final_filename = link.getStringProperty("plain_filename", null);
        if (final_filename == null) {
            final_filename = link.getFinalFileName();
        }
        br.getPage(link.getPluginPatternMatcher());
        if (jd.plugins.decrypter.DiskYandexNetFolder.isOfflineWebsite(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.isEmpty(getRawHash(link))) {
            /* For urls which have not been added via crawler, this value is not set but we might need it later. */
            final String hash_long = PluginJSonUtils.getJsonValue(br, "public_key");
            if (!StringUtils.isEmpty(hash_long)) {
                setRawHash(link, hash_long);
            }
        }
        /* Find json object for current link ... */
        final ArrayList<Object> modelObjects = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(jd.plugins.decrypter.DiskYandexNetFolder.regExJSON(this.br));
        Map<String, Object> entries = jd.plugins.decrypter.DiskYandexNetFolder.findModel(modelObjects, "resources");
        if (entries == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<Object> mediaObjects = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/resources");
        boolean foundObject = false;
        final String item_id = this.albumGetID(link);
        for (final Object mediao : mediaObjects) {
            entries = (Map<String, Object>) mediao;
            final String item_id_current = (String) entries.get("item_id");
            if (item_id.equalsIgnoreCase(item_id_current)) {
                foundObject = true;
                break;
            }
        }
        if (foundObject) {
            /* Great - we found our object and can display filename information. */
            return parseInformationAPIAvailablecheckAlbum(this, link, entries);
        } else {
            /* We failed to find the details but a download should be possible nontheless! */
            return AvailableStatus.TRUE;
        }
    }

    /** Parses file info for photo/video (ALBUM) urls e.g. /album/blabla:5fffffce1939c0c46ffffffe */
    public static AvailableStatus parseInformationAPIAvailablecheckAlbum(final Plugin plugin, final DownloadLink dl, final Map<String, Object> entries) throws Exception {
        Map<String, Object> entriesMeta = (Map<String, Object>) entries.get("meta");
        final List<Object> versions = (List<Object>) entriesMeta.get("sizes");
        final long filesize = JavaScriptEngineFactory.toLong(entriesMeta.get("size"), -1);
        final String error = (String) entries.get("error");
        String filename = (String) entries.get("name");
        if (error != null || StringUtils.isEmpty(filename) || filesize == -1) {
            /* Whatever - our link is probably offline! */
            return AvailableStatus.FALSE;
        }
        filename = plugin.encodeUnicode(filename);
        dl.setFinalFileName(filename);
        dl.setDownloadSize(filesize);
        String dllink = null, dllinkAlt = null;
        for (final Object versiono : versions) {
            entriesMeta = (Map<String, Object>) versiono;
            String url = (String) entriesMeta.get("url");
            final String versionName = (String) entriesMeta.get("name");
            if (StringUtils.isEmpty(url) || !url.startsWith("//") || StringUtils.isEmpty(versionName)) {
                continue;
            }
            url = "http:" + url;
            if (versionName.equalsIgnoreCase("ORIGINAL")) {
                dllink = url;
                break;
            } else if (StringUtils.isEmpty(dllinkAlt)) {
                /*
                 * 2018-08-09: List is sorted from best --> worst so let's use the highest one after ORIGINAL as alternative downloadurl.
                 */
                dllinkAlt = url;
            }
        }
        if (StringUtils.isEmpty(dllink) && !StringUtils.isEmpty(dllinkAlt)) {
            /* ORIGINAL not found? Download other version ... */
            dllink = dllinkAlt;
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty("directurl", dllink);
        }
        return AvailableStatus.TRUE;
    }

    public static boolean apiAvailablecheckIsOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setCookie("http://disk.yandex.com/", "ys", "");
        br.setAllowedResponseCodes(new int[] { 429 });
        return br;
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
        handleDownloadAlbum(link);
    }

    /** Download single album objects (photo/video) */
    private void handleDownloadAlbum(final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, "directurl");
        if (StringUtils.isEmpty(dllink)) {
            final String id0 = albumGetID0(link);
            final String clientID = albumGetIdClient();
            /* Rare case - probably the user tries to download a video. */
            String sk = jd.plugins.hoster.DiskYandexNet.getSK(this.br);
            if (StringUtils.isEmpty(sk)) {
                /** TODO: Maybe keep SK throughout sessions to save that one request ... */
                logger.info("Getting new SK value ...");
                sk = jd.plugins.hoster.DiskYandexNet.getNewSK(this.br, "disk.yandex.ru", br.getURL());
            }
            if (StringUtils.isEmpty(sk) || id0 == null) {
                logger.warning("Failed to get SK value");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("https://disk.yandex.ru/album-models/?_m=do-get-resource-url", "_model.0=do-get-resource-url&id.0=" + Encoding.urlEncode(id0) + "&idClient=" + clientID + "&version=" + jd.plugins.hoster.YandexAlbum.VERSION_YANDEX_PHOTO_ALBUMS + "&sk=" + sk);
            dllink = PluginJSonUtils.getJson(br, "file");
            if (!StringUtils.isEmpty(dllink) && dllink.startsWith("//")) {
                dllink = "http:" + dllink;
            } else if (!dllink.startsWith("http")) {
                /* This should never happen! */
                logger.info("WTF bad downloadlink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* More than 1 chunk is not necessary */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            handleServerErrors(link);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directurl", dl.getConnection().getURL().toString());
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
            link.setProperty(YandexAlbum.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private String albumGetID0(final DownloadLink dl) {
        final String hash_long = getRawHash(dl);
        final String id = albumGetID(dl);
        if (StringUtils.isEmpty(hash_long)) {
            /* This should never happen */
            return null;
        }
        return String.format("/album/%s:%s", hash_long, id);
    }

    private String albumGetID(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "/([a-f0-9]+)$").getMatch(0);
    }

    /** e.g. 'client365485934985' */
    public static String albumGetIdClient() {
        return "undefined" + System.currentTimeMillis();
    }

    public static String getHashLongFromHTML(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "public_key");
    }

    public static String getSK(final Browser br) {
        return PluginJSonUtils.getJsonValue(br, "sk");
    }

    private String getRawHash(final DownloadLink dl) {
        final String hash = dl.getStringProperty(PROPERTY_HASH, null);
        return hash;
    }

    public static void setRawHash(final DownloadLink dl, final String hash_long) {
        dl.setProperty(PROPERTY_HASH, hash_long);
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