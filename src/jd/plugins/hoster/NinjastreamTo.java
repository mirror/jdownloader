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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.segment.Segment;
import org.jdownloader.downloader.segment.SegmentDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class NinjastreamTo extends PluginForHost {
    public NinjastreamTo(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://ninjastream.to/tos";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ninjastream.to" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:download|watch)/([A-Za-z0-9]+)(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    private static final String  API_BASE          = "https://api.ninjastream.to";
    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFallbackFilename(final DownloadLink link) {
        String fallbackFilename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        if (fallbackFilename == null) {
            fallbackFilename = this.getFID(link);
        }
        return fallbackFilename;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* 2020-09-23: They're hosting video content-ONLY. */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(API_BASE + "/api/file/get", "id=" + Encoding.urlEncode(this.getFID(link)));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String status = (String) entries.get("status");
        if (!status.equals("success")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (Map<String, Object>) entries.get("result");
        String filename = (String) entries.get("name");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String md5 = (String) entries.get("hash");
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        } else if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFallbackFilename(link));
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS);
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        br.getPage("https://" + this.getHost() + "/download/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() != 200) {
            // Fallback through the watch page
            br.getPage("https://" + this.getHost() + "/watch/" + this.getFID(link));
        }
        // Check for Errors
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
        }
        String json = br.getRegex("v-bind:stream\\s*=\\s*\"([^\"]+)").getMatch(0);
        if (Encoding.isHtmlEntityCoded(json)) {
            json = Encoding.htmlDecode(json);
        }
        final String host = PluginJSonUtils.getJson(json, "host");
        final String hash = PluginJSonUtils.getJson(json, "hash");
        if (StringUtils.isEmpty(host) || StringUtils.isEmpty(hash)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String playlist = br.getRegex("v-bind:playlist\\s*=\\s*\"([^\"]+)\"").getMatch(0);
        if (playlist != null && !playlist.isEmpty()) {
            // If there is an available playlist, fetch the files from there
            if (Encoding.isHtmlEntityCoded(playlist)) {
                playlist = Encoding.htmlDecode(playlist);
            }
            final String[] segments = PluginJSonUtils.getJsonResultsFromArray(playlist);
            if (segments == null || segments.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new SegmentDownloader(this, link, null, br, new URL(host + hash + "/"), segments) {
                protected InputStream getInputStream(Segment segment, URLConnectionAdapter connection) throws IOException, PluginException {
                    final InputStream ret = super.getInputStream(segment, connection);
                    new DataInputStream(ret).readFully(new byte[0x78]);// Skip static PNG data
                    return ret;
                }
            };
            //
        } else if (false) {
            // Otherwise, use the m3u8 playlist
            // This only works if M3U8Playlist.X_BYTERANGE_SUPPORT is available (and therefore disabled for now)
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, host + hash + "/2_720p.m3u8");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}