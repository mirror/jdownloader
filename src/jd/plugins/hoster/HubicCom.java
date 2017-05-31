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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hubic.com" }, urls = { "https?://[a-z0-9\\-_]+\\.hubic\\.ovh\\.net/.+" })
public class HubicCom extends PluginForHost {
    public HubicCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://hubic.com/de/contracts/Contrat_hubiC_2014.pdf";
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 401 });
        return br;
    }

    public static Browser prepBRAjax(final Browser br) {
        final String token = br.getRegex("\"url\":\"[^\"]+\",\"token\":\"([a-f0-9]{32})\"").getMatch(0);
        if (token != null) {
            br.getHeaders().put("X-Token", token);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // final String url_filename = new Regex(link.getDownloadURL(), "/([^/]+)\\?").getMatch(0);
        dllink = link.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.getResponseCode() == 401) {
                /* Refresh directurl - they usually last ~2 weeks */
                final String ruid_b64 = link.getStringProperty("ruid", null);
                final String hash = link.getStringProperty("hash", null);
                if (ruid_b64 == null || hash == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.getPage("https://" + this.getHost() + "/home/pub/?ruid=" + ruid_b64);
                prepBRAjax(this.br);
                jd.plugins.decrypter.HubicCom.accessRUID(this.br, ruid_b64);
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                if (jd.plugins.decrypter.HubicCom.isOffline(this.br, entries)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                boolean success = false;
                final ArrayList<Object> ressourcelist = jd.plugins.decrypter.HubicCom.getList(entries);
                for (final Object fileo : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) fileo;
                    final String hash_temp = (String) entries.get("hash");
                    final String url = (String) entries.get("url");
                    if (StringUtils.isEmpty(hash_temp) || StringUtils.isEmpty(url) || !url.startsWith("http")) {
                        continue;
                    }
                    if (hash_temp.equalsIgnoreCase(hash)) {
                        success = true;
                        dllink = url;
                        break;
                    }
                }
                if (!success) {
                    logger.warning("Failed to refresh directurl --> Offline??");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                con = br.openHeadConnection(dllink);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
