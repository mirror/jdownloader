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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "focus.de" }, urls = { "https?://(?:www\\.)?focus\\.de/.+" })
public class FocusDe extends PluginForHost {
    public FocusDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.focus.de/intern/agb-der-focus-online-group-gmbh-agb-focus-online-group-gmbh_id_10064693.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String asset_id = br.getRegex("asset_id\\s*:\\s*'(\\d+)").getMatch(0);
        if (asset_id != null) {
            this.setLinkID(link, "focus.de://" + asset_id);
        }
        final String videoplayerLicenseKey = br.getRegex("videoplayer\\.focus\\.de/p/(?:abtest|player)/latest/tfa\\.js\\?key=([a-f0-9]+)").getMatch(0);
        String vms_id = br.getRegex("\"vms_id\"\\s*:\\s*\"([^\"]+)").getMatch(0);
        if (vms_id == null) {
            /*
             * Fallback for rare cases e.g.
             * https://www.focus.de/regional/muenchen/unglaubliche-preisvorstellung-dank-eines-tricks-verlangt-muenchnerin-fuer-ihre-2-
             * zimmerwohnung-2300-euro_id_188929977.html
             */
            vms_id = br.getRegex("contentId\\s*:\\s*'([^\\']+)").getMatch(0);
        }
        String title = br.getRegex("customizeHeadlinesType\\(\'(.*?)\',").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        long filesize = -1;
        if (videoplayerLicenseKey != null && vms_id != null) {
            /**
             * Prefer information from this API over information from html. </br>
             * Also for some items, the streaming URL inside html code is broken so this is the better source.
             */
            final Browser brc = new Browser();
            brc.getHeaders().put("Accept", "*/*");
            brc.getHeaders().put("Origin", "https://www.focus.de");
            brc.getHeaders().put("Referer", "https://www.focus.de/");
            brc.getHeaders().put("X-Dl8-Licensekey", videoplayerLicenseKey);
            brc.getPage("https://media-api-prod.delight-vr.com/api/v1/content/" + vms_id);
            if (brc.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> result = (Map<String, Object>) entries.get("result");
            if (Boolean.TRUE.equals(result.get("isLiveVideo"))) {
                logger.info("Livestreams are not supported");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            title = result.get("title").toString();
            long dummyQualitySortValueMax = -1;
            long filesizeMax = -1;
            final List<Map<String, Object>> videoRenditions = (List<Map<String, Object>>) result.get("videoRenditions");
            for (final Map<String, Object> videoRendition : videoRenditions) {
                /* Filesize is not always given */
                final Number filesizeTmp = (Number) videoRendition.get("size");
                final long dummyQualitySortValueTmp;
                if (filesizeTmp != null) {
                    dummyQualitySortValueTmp = filesizeTmp.longValue();
                } else {
                    /* No filesize given -> We need another way to find best quality. */
                    final String renditionName = videoRendition.get("name").toString();
                    if (renditionName.equalsIgnoreCase("HD")) {
                        dummyQualitySortValueTmp = 10000;
                    } else if (renditionName.equalsIgnoreCase("SD")) {
                        dummyQualitySortValueTmp = 1000;
                    } else {
                        logger.info("Found unknown renditionName: " + renditionName);
                        dummyQualitySortValueTmp = 100;
                    }
                }
                if (StringUtils.isEmpty(this.dllink) || dummyQualitySortValueTmp > dummyQualitySortValueMax) {
                    dummyQualitySortValueMax = dummyQualitySortValueTmp;
                    if (filesizeTmp != null) {
                        filesizeMax = filesizeTmp.longValue();
                    }
                    this.dllink = videoRendition.get("src").toString();
                }
            }
            if (filesizeMax != -1) {
                filesize = filesizeMax;
            }
        } else {
            logger.warning("Cannot obtain information from API -> Download may fail");
        }
        if (StringUtils.isEmpty(dllink)) {
            final String[] qualities = { "hdurl", "sdurl" };
            for (final String quality : qualities) {
                dllink = br.getRegex(quality + "[\t\n\r ]+=[\t\n\r ]+\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink != null) {
                    break;
                }
            }
            if (dllink == null) {
                dllink = br.getRegex("videourl\"\\s*:\\s*\"(http[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("videoUrl\\'\\s*:\\s*\\'([^<>\"]*?)\\'").getMatch(0);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Looks like no video content");
        } else if (dllink.startsWith("rtmp")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Livestreams are not supported");
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + ".mp4");
        }
        if (filesize <= 0 && !isDownload) {
            /* Obtain filesize via header if needed. */
            final Browser br2 = br.cloneBrowser();
            prepBrDownload(br2);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    filesize = con.getCompleteContentLength();
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        return AvailableStatus.TRUE;
    }

    private void prepBrDownload(final Browser br) {
        br.getHeaders().put("Referer", "https://videoplayer.focus.de/");
        br.getHeaders().put("Accept-Encoding", "identity;q=1, *;q=0");
        /*
         * 2023-06-21: Some items will fail without this header e.g.:
         * https://www.focus.de/digital/startup-schickt-300-satelliten-ins-all-bluetooth-revolution-reichweite-bald-1000-
         * kilometer_id_195778115.html
         */
        br.getHeaders().put("Range", "bytes=0-");
        // In case the link redirects to the finallink
        br.setFollowRedirects(true);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        prepBrDownload(br);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
