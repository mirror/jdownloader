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

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "biguz.net" }, urls = { "https?://(?:www\\.)?biguz\\.net/(?:watch\\.php\\?id=\\d+|video/\\?id=\\d+\\&name=[a-z0-9\\-]+)" })
public class BiguzNet extends PluginForHost {
    public BiguzNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    /* Connection stuff */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://biguz.net/contactus.php";
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
        try {
            final UrlQuery query = UrlQuery.parse(link.getPluginPatternMatcher());
            return query.get("id");
        } catch (final Throwable ignore) {
        }
        return null;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        final String extDefault = ".mp4";
        String title = null;
        final String urlName = UrlQuery.parse(this.br.getURL()).get("name");
        if (urlName != null) {
            /* 2020-10-20: Prefer file title from URL. */
            title = urlName.replace("-", " ").trim();
        }
        if (!link.isNameSet()) {
            /* Set fallback filename */
            if (title != null) {
                link.setName(this.applyFilenameExtension(title, extDefault));
            } else {
                link.setName(this.getFID(link) + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String jsAntiBotHash = br.getRegex("escape\\(\"([a-f0-9]+)").getMatch(0);
        if (jsAntiBotHash != null) {
            /* 2024-07-22 */
            logger.info("Entered anti-anti bot handling");
            final String ct_headless = jsAntiBotHash + ":false";
            final String ct_headless_b64 = Encoding.Base64Encode(ct_headless);
            br.setCookie(br.getHost(), "ct_headless", Encoding.urlEncode(ct_headless_b64));
            br.setCookie(br.getHost(), "verify", jsAntiBotHash);
            /* Reload page */
            br.getPage(br.getURL());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("(?i)This video was suspended")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (title == null) {
            title = br.getRegex("<title>([^<>\"]+)\\- biguz\\.net</title>").getMatch(0);
            if (title == null) {
                title = br.getRegex("</div><h1>([^<>\"]+)</h1>").getMatch(0);
            }
        }
        dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(this.applyFilenameExtension(title, extDefault));
        }
        if (!StringUtils.isEmpty(dllink)) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // 2024-04-23: max chunks = 1
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 1);
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
