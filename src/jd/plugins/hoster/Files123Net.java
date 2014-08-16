//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;

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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files123.net" }, urls = { "http://(www\\.)?files123\\.net/[A-Z0-9]+/download\\.html" }, flags = { 0 })
public class Files123Net extends PluginForHost {

    public Files123Net(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.files123.net/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setName(new Regex(link.getDownloadURL(), "files123\\.net/([A-Z0-9]+)/").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("id=\"error_note\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<title>([^<>\"]*?) \\| files123\\.net</title>").getMatch(0);
        final String filesize = br.getRegex("<div>File size <b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final String continuelink = br.getRegex("align=\"center\"><a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (continuelink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(continuelink);
            br.setFollowRedirects(false);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.findID();
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                br.postPage(br.getURL(), "download=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br.containsHTML("id=\"error_note\">Please Enter Correct")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("id=\"error_note\">Please Enter Correct")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
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

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}