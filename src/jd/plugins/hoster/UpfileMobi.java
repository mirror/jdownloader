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

import java.io.IOException;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "upfile.mobi" }, urls = { "http://(?:www\\.)?upfile\\.mobi/(\\d+(\\.[a-f0-9]{32})?|index\\.php\\?page=file\\&f=\\d+|[a-zA-Z0-9]{6,12})" })
public class UpfileMobi extends PluginForHost {
    private final String password_required = "Enter password:<br/>";

    public UpfileMobi(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://upfile.mobi/index.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No file with id <")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(password_required)) {
            link.getLinkStatus().setStatusText("Requires Pre-Download Password!");
            return AvailableStatus.UNCHECKABLE;
        }
        final String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("Download (File)?(</a>)?\\s*?([^<>\"]+)").getMatch(2);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fid = getFUID(downloadLink);
        if (br.containsHTML(password_required)) {
            Form password_form = br.getForm(0);
            for (Form f : br.getForms()) {
                if (f.containsHTML(password_required)) {
                    password_form = f;
                    break;
                }
            }
            if (password_form != null) {
                String passCode = downloadLink.getStringProperty("pass", null);
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                }
                if (passCode == null) {
                    logger.info("Pre-Download Password: User has entered blank password, exiting.");
                    passCode = null;
                    downloadLink.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                logger.info("Pre-Download Password: User entered: " + passCode);
                password_form.put("key", Encoding.urlEncode(passCode));
                br.submitForm(password_form);
                if (br.containsHTML(password_required)) {
                    logger.info("Incorrect Pre-Download Password!");
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.cloneBrowser().openGetConnection("//upfile.mobi/index.php?page=screen&id=" + fid + "&p=");
        final String ad_sht = br.getRegex("\"(ga\\.php[^<>\"]*?)\"").getMatch(0);
        if (ad_sht != null) {
            br.cloneBrowser().openGetConnection("//upfile.mobi/" + ad_sht);
        }
        String dllink = br.getRegex("\"(https?://(www\\.)?upfile\\.mobi/download/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/download/\\?page=download[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Download key is incorrect")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Download key is incorrect'", 10 * 30 * 1000l);
            }
            if (br.containsHTML("file and needs moderation, after moderation")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'This is a video file and needs moderation, after moderation you can download this file'", 30 * 30 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFUID(final DownloadLink downloadLink) {
        if (downloadLink == null) {
            return null;
        }
        String s = new Regex(downloadLink.getDownloadURL(), "https?://(?:www\\.)?upfile\\.mobi/(\\d+)").getMatch(0);
        if (s == null) {
            s = new Regex(downloadLink.getDownloadURL(), "https?://(?:www\\.)?upfile\\.mobi/index\\.php\\?page=file&f=(\\d+)").getMatch(0);
            if (s == null) {
                s = new Regex(downloadLink.getDownloadURL(), "upfile\\.mobi/([a-zA-Z0-9]{6,12})").getMatch(0);
            }
        }
        return s;
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