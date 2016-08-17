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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https://[a-z0-9\\-\\.]+\\.artstation\\.com/p/assets/images/.+" }) 
public class ArtstationCom extends PluginForHost {

    public ArtstationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    public static final String   default_Extension = ".jpg";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://artstation.com/";
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // filename NOT coming from decrypter wont have filename set!!! this is not a plugin defect! -raztoki20160202
        String filename = downloadLink.getStringProperty("decrypterfilename", null);
        dllink = downloadLink.getDownloadURL();
        if (filename != null) {
            downloadLink.setFinalFileName(filename);
        }
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                br2.getHeaders().put("Accept-Encoding", "identity");
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html") && con.isOK()) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                if (filename == null) {
                    filename = getFileNameFromHeader(con);
                    downloadLink.setFinalFileName(filename);
                }
                downloadLink.setProperty("directlink", dllink);
                return AvailableStatus.TRUE;
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        this.br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /** Sets correct json headers */
    public static void setHeaders(final Browser br) {
        final String token = br.getRegex("name=\"authenticity_token\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        if (token != null) {
            br.getHeaders().put("X-CSRF-Token", token);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* Not necessarily needed! */
        // br.getHeaders().put("X-NewRelic-ID", "VgMBWFFQGwEIUVFQAgE=");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    public static boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
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
