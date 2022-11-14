//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zaycev.net" }, urls = { "https?://(?:www\\.)?zaycev\\.net/pages/[0-9]+/([0-9]+)\\.shtml" })
public class ZaycevNet extends PluginForHost {
    public ZaycevNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://zaycev.net/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME        = true;
    private static final int     FREE_MAXCHUNKS     = 0;
    private static final String  PROPERTY_DIRECTURL = "directurl";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.101 Safari/537.36");
        this.br.setAllowedResponseCodes(410);
        // br.setCookie(this.getHost(), "mm_cookie", "1");
        br.getPage(link.getPluginPatternMatcher());
        final int responsecode = this.br.getHttpConnection().getResponseCode();
        if (br.getRedirectLocation() != null || br.containsHTML("http\\-equiv=\"Refresh\"|>Данная композиция заблокирована, приносим извинения") || responsecode == 404 || responsecode == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("itemProp=\"availability\"/></span></section><h1[^>]*>([^<>\"]+)</h1>").getMatch(0);
        final String filesizeStr = br.getRegex("Б<meta content=\"(.*?)\" itemprop=\"contentSize\"/>").getMatch(0);
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        }
        if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        } else {
            /* Try to calculate filesize */
            final String bitrateStr = br.getRegex("data-qa=\"TrackPage-bitrate\"[^>]*>(\\d+) kbps</div>").getMatch(0);
            final Regex durationRegex = br.getRegex("itemProp=\"duration\"[^>]*>(\\d+):(\\d+)</time>");
            if (bitrateStr != null && durationRegex.matches()) {
                final int durationSeconds = Integer.parseInt(durationRegex.getMatch(0)) * 60 + Integer.parseInt(durationRegex.getMatch(1));
                link.setDownloadSize((durationSeconds * Integer.parseInt(bitrateStr) * 1024) / 8);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link);
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
            brc.postPageRaw("/api/external/track/filezmeta/", "{\"trackIds\":[\"" + getFID(link) + "\"],\"subscription\":false}");
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
            final Map<String, Object> track = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "tracks/{0}");
            final Object downloadIDOrStatus = track.get("download");
            final String dllink;
            if (downloadIDOrStatus instanceof String) {
                final String downloadID = downloadIDOrStatus.toString();
                brc.getPage("/api/external/track/download/" + downloadID);
                /* Answer = URL as plaintext */
                dllink = brc.getRequest().getHtmlCode();
            } else {
                /* "download":false --> Download stream */
                final String streamingID = track.get("streaming").toString();
                if (StringUtils.isEmpty(streamingID)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                brc.getPage("/api/external/track/play/" + streamingID);
                final Map<String, Object> streaming = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                dllink = streaming.get("url").toString();
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML(">In this country site zaycev\\.net is not")) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // id3 and packagename can be tag due to directlink imports
            // downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()).replaceAll("_?\\(zaycev\\.net\\)", ""));
            link.setProperty("savedlink", dl.getConnection().getURL().toString());
        }
        final String serverFilename = Plugin.getFileNameFromDispositionHeader(dl.getConnection());
        if (serverFilename != null) {
            link.setFinalFileName(serverFilename);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, FREE_RESUME, FREE_MAXCHUNKS);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(PROPERTY_DIRECTURL);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}