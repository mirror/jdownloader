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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

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
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

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
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
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
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getFreshDirecturl(final DownloadLink link) throws PluginException {
        logger.info("Trying to find fresh directurl");
        String freshDirecturl = null;
        final PluginForDecrypt decrypterplugin = getNewPluginForDecryptInstance(getHost());
        /* Match variants via filename */
        final String target_filename = link.getFinalFileName();
        try {
            final CryptedLink forDecrypter = new CryptedLink(link.getContainerUrl(), link);
            final ArrayList<DownloadLink> ret = decrypterplugin.decryptIt(forDecrypter, null);
            for (final DownloadLink dl : ret) {
                correctDownloadLink(dl);
                final String filenameTmp = dl.getFinalFileName();
                if (filenameTmp != null && filenameTmp.equals(target_filename)) {
                    freshDirecturl = dl.getPluginPatternMatcher();
                    break;
                }
            }
        } catch (final Throwable e) {
            if (decrypterplugin.getBrowser().getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
            } else {
                logger.log(e);
            }
        } finally {
            decrypterplugin.clean();
        }
        return freshDirecturl;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("ServerComaptibleForByteRangeRequest", true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
