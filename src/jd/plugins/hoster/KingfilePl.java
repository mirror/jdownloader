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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kingfile.pl" }, urls = { "http://(?:www\\.)?kingfile\\.pl/download/[A-Za-z0-9]+" })
public class KingfilePl extends PluginForHost {

    public KingfilePl(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "http://kingfile.pl/contact";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = true;
    private final int     FREE_MAXCHUNKS    = 1;
    private final int     FREE_MAXDOWNLOADS = 1;

    private String        fid               = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        link.setLinkID(fid);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String secret = Encoding.unicodeDecode(br.getRegex("\\\\x3C\\\\x69\\\\x6D\\\\x67\\\\x3E\"\\,\"([^\"]+)").getMatch(0));
        if (secret != null) {
            br.openGetConnection("/googletagmanager/" + secret + ".png").disconnect();
            br.getPage(link.getDownloadURL());
        }
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML(">Plik który chcesz pobrać zmienił swój adres lub został usunięty<|>Nie znaleziono szukanego pliku<|>Nie znaleziono pliku|>Szukany plik zmienił swój adres lub")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"fileName\">([^<>\"]+)<").getMatch(0);
        String filesize = br.getRegex(">Rozmiar: ([^<>\"]+)<").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            this.br.getPage("/download/" + this.fid + "/?plan=n");
            long wait = 68;
            final String wait_str = PluginJSonUtils.getJsonValue(br, "timer");
            if (wait_str != null) {
                wait = Long.parseLong(wait_str);
            }
            boolean success = false;
            final long timetarget = System.currentTimeMillis() + (wait * 1001l);
            for (int i = 0; i <= 3; i++) {
                final String code = this.getCaptchaCode("/seccode/normal.png", downloadLink);
                if (i == 0) {
                    /* Waittime needed before the first captcha-try --> Minimize this waittime. */
                    if (System.currentTimeMillis() < timetarget) {
                        /* Wait if we still have to! */
                        wait = timetarget - System.currentTimeMillis();
                        this.sleep(wait, downloadLink);
                    }
                }
                this.br.getPage("/download/" + this.fid + "/?plan=n&captcha=" + Encoding.urlEncode(code));
                if (this.br.toString().replace("\\", "").contains("/seccode/normal.png")) {
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            dllink = PluginJSonUtils.getJsonValue(br, "url");
            if (dllink == null) {
                if (this.br.containsHTML("LIMIT POBIERANIA ZOSTA")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1001l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}