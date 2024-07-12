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
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "web.de" }, urls = { "https?://(?:www\\.)?web\\.de/magazine/.+" })
public class WebDe extends PluginForHost {
    public WebDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://web.de/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        br.getPage(link.getPluginPatternMatcher().replaceFirst("http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = br.getRegex("ui\\.tifConfig\\.coremid\\s*=\\s*\"(\\d+)\";").getMatch(0);
        if (contentID != null) {
            link.setLinkID("webde://video:" + contentID);
        } else {
            logger.warning("Failed to find contentID");
        }
        String filename = br.getRegex("(?i)<title>([^<>\"]*?) (?:\\-|\\|) WEB\\.DE</title>").getMatch(0);
        dllink = PluginJSonUtils.getJson(br, "contentUrl");
        if (StringUtils.isEmpty(dllink) || !dllink.toLowerCase(Locale.ENGLISH).endsWith(".mp4")) {
            /* No supported [video] content. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            /* Fallback */
            filename = br._getURL().getPath().replace("/", " ").replace("-", " ").trim();
        }
        filename = Encoding.htmlDecode(filename).trim();
        final String extDefault = ".mp4";
        link.setFinalFileName(this.applyFilenameExtension(filename, extDefault));
        final boolean isDownload = Thread.currentThread() instanceof SingleDownloadController;
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, filename, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        this.handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected void throwFinalConnectionException(jd.http.Browser br, jd.http.URLConnectionAdapter con) throws PluginException, IOException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    };

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
