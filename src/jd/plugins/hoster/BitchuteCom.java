//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitchute.com" }, urls = { "https?://(?:www\\.|old\\.)?bitchute\\.com/video/([A-Za-z0-9\\-_]+)" })
public class BitchuteCom extends PluginForHost {
    public BitchuteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    @Override
    public String getAGBLink() {
        return "https://www.bitchute.com/";
    }

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        final String fid = this.getFID(link);
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPageRaw("https://api.bitchute.com/api/beta9/video", "{\"video_id\":\"" + fid + "\"}");
        if (br.getHttpConnection().getResponseCode() == 403) {
            // {"errors":[{"context":"AUTH","message":"Forbidden - Cannot perform this action"},{"context":"reason","message":"Content
            // access is restricted based on the users location"}]}
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Content access is restricted based on the users location");
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503", 10 * 60 * 1000l);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String title = entries.get("video_name").toString();
        final String description = (String) entries.get("description");
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        final String finalName = this.applyFilenameExtension(title, extDefault);
        link.setFinalFileName(finalName);
        if (!(Thread.currentThread() instanceof SingleDownloadController) && !link.isSizeSet()) {
            final String dllink = getMP4Link(br, link);
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, finalName, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    private String getMP4Link(final Browser br, final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("video_id", fid);
        final PostRequest req = br.createJSonPostRequest("https://api.bitchute.com/api/beta/video/media", postdata);
        req.getHeaders().put("Accept", "*/*");
        req.getHeaders().put("Content-Type", "application/json");
        req.getHeaders().put("Origin", "https://www.bitchute.com");
        req.getHeaders().put("Priority", "u=1, i");
        req.getHeaders().put("Referer", "https://www.bitchute.com/");
        br.getPage(req);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String ret = (String) entries.get("media_url");
        if (StringUtils.isEmpty(ret)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return ret;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = getMP4Link(br, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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
