//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl.free.fr" }, urls = { "http://[\\w\\.]*?dl\\.free\\.fr/(getfile\\.pl\\?file=/[\\w]+|[\\w]+/?)" }, flags = { 0 })
public class DlFreeFr extends PluginForHost {

    public DlFreeFr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dl.free.fr/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.isContentDisposition()) {
            downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
            downloadLink.setDownloadSize(con.getLongContentLength());
            con.disconnect();
            return AvailableStatus.TRUE;
        } else {
            br.followConnection();
        }
        String filename = br.getRegex(Pattern.compile("Fichier:</td>.*?<td.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("Taille:</td>.*?<td.*?>(.*?)soit", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll("o", "byte").replaceAll("Ko", "Kb").replaceAll("Mo", "Mb").replaceAll("Go", "Gb")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        if (dl.getConnection().isContentDisposition()) {
            /* directdownload */
        } else {
            br.followConnection();
            if (br.containsHTML("Trop de slots utilis")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
            String filename = br.getRegex(Pattern.compile("Fichier:</td>.*?<td.*?>(.*?)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            String filesize = br.getRegex(Pattern.compile("Taille:</td>.*?<td.*?>(.*?)soit", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String dlLink = br.getRegex("<tr><td colspan=\"2\" style=\"text-align: center; border: 1px solid #ffffff\"><a style=\"text-decoration: underline\" href=\"(.*?free.*?fr.*?)\">").getMatch(0);
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                if (br.getURL().contains("overload")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
