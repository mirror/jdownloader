//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.jdownloader.plugins.components.RefreshSessionLink;

@HostPlugin(revision = "$Revision: 21813 $", interfaceVersion = 2, names = { "kissanime.to" }, urls = { "http://(www\\.)?51\\.15\\.\\d{1,3}\\.\\d{1,3}(?::\\d+)?/videoplayback\\?hash=[^\"'\\s<>]+" })
public class KissAnimeTo extends PluginForHost {
    // raztoki embed video player template.
    private String dllink = null;

    public KissAnimeTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.kissanime.to/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        // dllink = refreshDirectlink(downloadLink);
        dllink = downloadLink.getDownloadURL();
        // they segment and connection will close.
        while (true) {
            final long downloadCurrentRaw = downloadLink.getDownloadCurrentRaw();
            try {
                br.setCurrentURL(downloadLink.getStringProperty("source_url", null));
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
                if (!dl.getConnection().isOK()) {
                    dl.getConnection().disconnect();
                    dllink = refreshDirectlink(downloadLink);
                    // save this for resume events outside of while loop (GUI abort/disable/stop download)
                    downloadLink.setPluginPatternMatcher(dllink);
                    br = new Browser();
                    br.setCurrentURL(downloadLink.getStringProperty("source_url", null));
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
                    if (!dl.getConnection().isOK()) {
                        br.followConnection();
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl.startDownload();
                break;
            } catch (final Exception e) {
                if ("Download Incomplete".equals(e.getMessage()) || (e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE)) {
                    if (downloadLink.getDownloadCurrent() > downloadCurrentRaw && !isAbort()) {
                        dl.close();
                        continue;
                    }
                }
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    /**
     * Refresh directurls from external providers
     *
     * @throws Exception
     */
    private String refreshDirectlink(final DownloadLink downloadLink) throws Exception {
        final String refresh_url_plugin = downloadLink.getStringProperty("refresh_url_plugin", null);
        if (refresh_url_plugin != null) {
            return ((RefreshSessionLink) JDUtilities.getPluginForDecrypt(refresh_url_plugin)).refreshVideoDirectUrl(downloadLink);
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}