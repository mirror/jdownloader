//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "facecast.net" }, urls = { "https?://(?:www\\.)?facecast\\.net/(?:v|w)/([A-Za-z0-9]+)" })
public class FacecastNet extends PluginForHost {
    public FacecastNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "http://www.servustv.com/Nutzungsbedingungen";
    }

    private long date_start = 0;

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

    private Map<String, Object> entries = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String extDefault = ".mp4";
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback filename */
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://" + this.getHost() + "/ajaj/get_servers?_t=" + System.currentTimeMillis());
        final List<HashMap<String, Object>> ressourcelist = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.LIST_HASHMAP);
        final Map<String, Object> randomServerMap = ressourcelist.get(new Random().nextInt(ressourcelist.size() - 1));
        final String webapidomain = randomServerMap.get("src").toString();
        br.getPage("https://" + webapidomain + "/eventdata?code=" + fid + "&ref=&_=" + System.currentTimeMillis());
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        if (br.getHttpConnection().getResponseCode() == 404 || entries.containsKey("error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        date_start = ((Number) entries.get("date_plan_start_ts")).longValue() * 1000;
        final String date_formatted = formatDate();
        final String title = (String) entries.get("name");
        if (title != null) {
            String filename = date_formatted;
            filename = filename + "_" + title;
            link.setFinalFileName(filename + extDefault);
        }
        final String description = (String) entries.get("description");
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final Object is_live = entries.get("is_live");
        if (this.date_start > System.currentTimeMillis()) {
            /* Seems like what the user wants to download hasn't aired yet --> Wait and retry later! */
            final long waitUntilStart = this.date_start - System.currentTimeMillis();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This video has not been broadcasted yet!", waitUntilStart);
        } else if (is_live instanceof Number && ((Number) is_live).intValue() == 1) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Livestreams are not supported");
        }
        if (true) {
            /* https://svn.jdownloader.org/issues/84276 */
            throw new PluginException(LinkStatus.ERROR_FATAL, "HLS streams with split video/audio are not yet supported");
        }
        final String eid = entries.get("id").toString();
        if (StringUtils.isEmpty(eid)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getPage("https://cdn-3.facecast.net/viewer_auth?eid=" + eid + "&sid=");
        this.br.getPage("/public/" + eid + ".m3u8?_=" + System.currentTimeMillis());
        if (br.getHttpConnection().getResponseCode() == 204) {
            /* 204 no content --> Most likely password protected content */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected videos are not yet supported");
        }
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
        dl.startDownload();
    }

    private String formatDate() {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date_start);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date_start);
        }
        return formattedDate;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}