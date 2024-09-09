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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "biqle.ru" }, urls = { "biqledecrypted://.+" })
public class BiqleRu extends PluginForHost {
    public BiqleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("biqledecrypted://", "https://"));
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: Basically this plugin only exists to refresh URLs on timeout
    /* Extension which will be used if no correct extension is found */
    /* Connection stuff */
    private static final boolean free_resume    = true;
    private static final int     free_maxchunks = 0;
    private String               dllink         = null;
    private boolean              server_issues  = false;

    @Override
    public String getAGBLink() {
        return "https://biqle.ru/legal/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = link.getPluginPatternMatcher();
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (StringUtils.isEmpty(dllink)) {
            return AvailableStatus.UNCHECKABLE;
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put(OPEN_RANGE_REQUEST);
            con = brc.openHeadConnection(dllink);
            if (!this.looksLikeDownloadableContent(con)) {
                con.disconnect();
                dllink = getFreshDirecturl(link);
                if (dllink == null) {
                    logger.info("Failed to refresh directurl");
                    return AvailableStatus.UNCHECKABLE;
                }
                con = brc.openHeadConnection(dllink);
            }
            this.handleConnectionErrors(brc, con);
            if (con.getCompleteContentLength() > 0) {
                if (con.isContentDecoded()) {
                    link.setDownloadSize(con.getCompleteContentLength());
                } else {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getFreshDirecturl(final DownloadLink link) throws PluginException {
        logger.info("Trying to find fresh directurl");
        final PluginForDecrypt plg = getNewPluginForDecryptInstance("noodlemagazine.com");
        /* Match variants via filename */
        final String target_filename = link.getFinalFileName();
        try {
            final CryptedLink forDecrypter = new CryptedLink(link.getContainerUrl(), link);
            final ArrayList<DownloadLink> ret = plg.decryptIt(forDecrypter, null);
            for (final DownloadLink dl : ret) {
                correctDownloadLink(dl);
                final String filenameTmp = dl.getFinalFileName();
                if (filenameTmp != null && filenameTmp.equals(target_filename)) {
                    return dl.getPluginPatternMatcher();
                }
            }
        } catch (final Throwable e) {
            if (plg.getBrowser().getHttpConnection() != null && plg.getBrowser().getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
            } else {
                logger.log(e);
            }
        } finally {
            plg.clean();
        }
        return null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(DirectHTTP.PROPERTY_ServerComaptibleForByteRangeRequest, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        this.handleConnectionErrors(br, dl.getConnection());
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
