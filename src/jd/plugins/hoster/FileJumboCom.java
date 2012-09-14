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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filejumbo.com" }, urls = { "http://(www\\.)?filejumbo\\.com/Download/[A-Z0-9]+" }, flags = { 0 })
public class FileJumboCom extends PluginForHost {

    public FileJumboCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filejumbo.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file is either removed due to copyright claim") || br.getURL().contains("filejumbo.com/NotFound.aspx")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span id=\"ctl00_PagePlaceHolder_dlFiles_ctl00_lbFileName\">([^<>\"]*?)</span>").getMatch(0);
        String filesize = br.getRegex("<span id=\"ctl00_PagePlaceHolder_dlFiles_ctl00_lbSize\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0);
        final String urlencodedFilename = Encoding.urlEncode(downloadLink.getName());
        final String viewState = br.getRegex("name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (viewState == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://www.filejumbo.com/Download/DownloadDeleteTemplate.aspx?fileId=" + fid + "&topFolder=download&fileName=", "ctl00%24PagePlaceHolder%24scrMngr=ctl00%24PagePlaceHolder%24updatePan%7Cctl00%24PagePlaceHolder%24updatePan%24Download&__EVENTTARGET=ctl00%24PagePlaceHolder%24updatePan%24Download&__EVENTARGUMENT=" + fid + "%09&__VIEWSTATE=&__VIEWSTATEENCRYPTED=&ctl00%24UserName=&ctl00%24Password=&ctl00%24RememberMe=on&ctl00%24PagePlaceHolder%24dlFiles%24ctl00%24hiddenIsPassword=0&shareURL=http%3A%2F%2Fwww.filejumbo.com%2FDownload%2F" + fid + "&shareHTML=%3Ca%20href%3D%22http%3A%2F%2Fwww.filejumbo.com%2FDownload%2F" + fid + "%22%20target%3D_blank%3E" + urlencodedFilename + "%3C%2Fa%3E&shareForumHTML=%5BURL%3Dhttp%3A%2F%2Fwww.filejumbo.com%2FDownload%2F" + fid + "%5D" + urlencodedFilename + "%5B%2FURL%5D&__ASYNCPOST=true&");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
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