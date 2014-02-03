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

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freedisc.pl" }, urls = { "http://(www\\.)?freedisc\\.pl/(#\\!)?[a-z0-9]+,f\\-\\d+" }, flags = { 0 })
public class FreeDiscPl extends PluginForHost {

    public FreeDiscPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://freedisc.pl/regulations";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/#!", "/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            if (br.getRequest().getHttpConnection().getResponseCode() == 410) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw e;
        }
        if (br.containsHTML("Ten plik został usunięty przez użytkownika lub administratora")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Handle no public files as offline
        if (br.containsHTML("Ten plik nie jest publicznie dostępny")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        String filesize = br.getRegex("class=\\'frameFilesSize\\'>Rozmiar pliku</div>[\t\n\r ]+<div class=\\'frameFilesCountNumber\\'>([^<>\"]*?)</div>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("Rozmiar pliku</div>[\t\n\r ]+<div class=\\'frameFilesCountNumber\\'>([^<>\"]*?)</div>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("Rozmiar plików</div><div class=\\'menuFilesCountNumber\\'>([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = "http://freedisc.pl/download/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("freedisc.pl/pierrw,f-")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}