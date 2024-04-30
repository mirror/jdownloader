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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DliveTv extends PluginForHost {
    public DliveTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://dlive.tv/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dlive.tv" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/p/(?:dlive-)?([^\\+]+\\+[A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String fid = this.getFID(link);
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String hash = "7e87892585655973d4a7659b3bc3cbdcbbbc2e2bd0b45367e2d42245ea9a1184";
        br.postPageRaw("https://graphigo.prd.dlive.tv/", "{\"operationName\":\"PastBroadcastPage\",\"variables\":{\"permlink\":\"" + fid + "\",\"commentsFirst\":20,\"topContributionsFirst\":10,\"isLoggedIn\":false},\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + hash + "\"}}}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object errorsO = root.get("errors");
        if (errorsO != null) {
            /* E.g. {"errors":[{"message":"PersistedQueryNotFound"}],"data":null} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> data = (Map<String, Object>) root.get("data");
        final Map<String, Object> video = (Map<String, Object>) data.get("pastBroadcastV2");
        if (video == null) {
            /* E.g. {"data":{"pastBroadcast":null}} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> creator = (Map<String, Object>) video.get("creator");
        final String title = (String) video.get("title");
        if (!StringUtils.isEmpty(title)) {
            final SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
            final Date dateTmp = new Date(Long.parseLong(video.get("createdAt").toString()));
            link.setFinalFileName(creator.get("username") + "_" + sd.format(dateTmp) + "_" + title + extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String hlsMaster = PluginJSonUtils.getJson(br, "playbackUrl");
        if (StringUtils.isEmpty(hlsMaster)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(hlsMaster);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        final String url_hls = hlsbest.getStreamURL();
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}