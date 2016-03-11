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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drawcrowd.com" }, urls = { "http://(?:www\\.)?drawcrowd\\.com/projects/[A-Za-z0-9]+" }, flags = { 0 })
public class DrawcrowdCom extends PluginForHost {

    public DrawcrowdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://drawcrowd.com/";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        setHeaders(br);
        /* This time we'll get a json answer. */
        br.getPage(downloadLink.getDownloadURL());
        LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final String full_name = downloadLink.getStringProperty("full_name", null);
        json = (LinkedHashMap<String, Object>) json.get("project");
        String filename = (String) json.get("title");
        if (inValidate(filename)) {
            filename = fid;
        } else {
            filename = filename + " - " + fid;
        }
        dllink = (String) json.get("original_image");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (full_name != null) {
            filename = full_name + " - " + filename;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        /* Make sure that we get a correct extension */
        final String ext = getFileNameExtensionFromString(dllink, ".jpg");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /** Sets correct json headers */
    public static void setHeaders(final Browser br) {
        // should have X-CSRF-Token and X-NewRelic-ID
        final String token = br.getRegex("name=\"authenticity_token\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        if (token != null) {
            br.getHeaders().put("X-CSRF-Token", token);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
