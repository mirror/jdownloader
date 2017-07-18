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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "telvi.de" }, urls = { "https?://embed\\.telvi\\.de/\\d+/clip/\\d+|decrypted://telvi\\.de/[a-zA-Z0-9_/\\+\\=\\-%]+" })
public class TelviDe extends PluginForHost {
    public TelviDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: This is a CMS used by some website. This is a general host plugin for their (embed) video urls.
    // Structure of embed urls: https?://embed\\.telvi\\.de/<portal_ID>/clip/<clip_ID>
    private static final String  TYPE_EMBED        = "https?://embed\\.telvi\\.de/\\d+/clip/\\d+";
    private static final String  TYPE_ENCRYPTED    = "decrypted://telvi\\.de/[a-zA-Z0-9_/\\+\\=\\-%]+";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.telvi.de/agb.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String videoid;
        if (downloadLink.getDownloadURL().matches(TYPE_ENCRYPTED)) {
            final String b64 = Encoding.htmlDecode(new Regex(downloadLink.getDownloadURL(), "telvi\\.de/(.+)").getMatch(0));
            final String b64_decrypted = Encoding.Base64Decode(b64);
            if (b64_decrypted == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex inforegex = new Regex(b64_decrypted, "(\\d+)\\|(\\d+)\\|");
            final String portal_ID = inforegex.getMatch(0);
            final String clip_ID = inforegex.getMatch(1);
            if (portal_ID == null || clip_ID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage("http://embed.telvi.de/" + portal_ID + "/clip/" + clip_ID);
            videoid = portal_ID + "_" + clip_ID;
        } else {
            br.getPage(downloadLink.getDownloadURL());
            final Regex vinfo = new Regex(downloadLink.getDownloadURL(), "telvi\\.de/(\\d+)/clip/(\\d+)");
            videoid = vinfo.getMatch(0) + "_" + vinfo.getMatch(1);
        }
        final String key = PluginJSonUtils.getJsonValue(br, "key");
        if (key == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (key.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.getHeaders().put("SOAPAction", "");
        this.br.postPageRaw("http://v.telvi.de/", "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">  <SOAP-ENV:Body SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">    <impl:getVideoData xmlns:impl=\"http://v.telvi.de/\">      <key xsi:type=\"xsd:string\">" + key + "</key>    </impl:getVideoData>  </SOAP-ENV:Body></SOAP-ENV:Envelope>");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* First check if a filename was set before via decrypter */
        String filename = downloadLink.getStringProperty("decryptedfilename", null);
        dllink = br.getRegex("<video xsi:type=\"xsd:string\">(\\d+/[^<>\"]*?)</video>").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ext;
        if (dllink != null) {
            dllink = "http://video.telvi.de/" + Encoding.htmlDecode(dllink);
            if (filename == null) {
                filename = new Regex(dllink, "([a-z0-9]+)\\.[a-z0-9]{2,5}$").getMatch(0);
            }
            ext = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            ext = ".mp4";
        }
        if (filename == null) {
            /* Last chance fallback */
            filename = videoid;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
                downloadLink.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
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
