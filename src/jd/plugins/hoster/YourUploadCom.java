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

import java.net.ConnectException;

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourupload.com" }, urls = { "http://((www\\.)?(yourupload\\.com|yucache\\.net)/(file|embed(_ext/\\w+)?|watch)/[a-z0-9]+|embed\\.(yourupload\\.com|yucache\\.net)/[A-Za-z0-9]+)" }, flags = { 0 })
public class YourUploadCom extends PluginForHost {

    private String dllink = null;

    public YourUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://yourupload.com/index.php?act=pages&page=terms-of-service";
    }

    public void correctDownloadLink(DownloadLink link) {
        // you can not convert embed formats back! will always show up offline!
        if (!link.getDownloadURL().matches(".+(/embed_ext/|embed\\.(yourupload\\.com|yucache\\.net)/).+")) link.setUrlDownload("http://yourupload.com/file/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        // Correct old links
        correctDownloadLink(link);
        br.getPage(link.getDownloadURL());
        if (link.getDownloadURL().matches(".+(/embed_ext/|embed\\.(yourupload\\.com|yucache\\.net)/).+")) {
            if (br.containsHTML("<h1>Error</h1>") || br.containsHTML("Embed\\+entry\\+doesnt\\+exist") || br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\" />").getMatch(0);
            dllink = br.getRegex("'file':\\s+'(https?://.*?)'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("<meta property=\"og:video\" content=\"(https?://.*?)\"/>").getMatch(0);
            if (dllink == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openGetConnection(dllink);
                } catch (final ConnectException e) {
                    return AvailableStatus.TRUE;
                }
                // only way to check for made up links... or offline is here
                if (con.getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                return AvailableStatus.TRUE;
            } catch (Exception e) {
                throw e;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (br.containsHTML(">System Error<|>could not find file|>File not found<|Array doesn\\'t have key named|File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Name</b>[\r\n\t ]+</td>[\r\n\t ]+<td>([^<>\"]+)</td>").getMatch(0);
        final String filesize = br.getRegex(">Size</b>[\r\n\t ]+</td>[\r\n\t ]+<td>([^<>\"]+)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        String ext = null;
        if (filename.contains(".")) ext = filename.substring(filename.lastIndexOf("."));
        if (ext != null && ext.length() > 5) ext = null;
        if (br.containsHTML("<td>video/mp4</td>") && ext == null) ext = ".mp4";
        if (ext != null && !filename.endsWith(ext)) {
            link.setFinalFileName(filename + ext);
        } else {
            link.setName(filename);
        }
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink == null) dllink = br.getRegex("(http://download\\.(yourupload\\.com|yucache\\.net)/[a-f0-9]{32}[^\"]+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}