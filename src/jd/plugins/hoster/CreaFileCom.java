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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "creafile.com" }, urls = { "http://[\\w\\.]*?creafile\\.com/download/[a-z0-9]+" }, flags = { 0 })
public class CreaFileCom extends PluginForHost {

    public CreaFileCom(PluginWrapper wrapper) {
        super(wrapper);
        // this host blocks if there is no timegap between the simultan
        // downloads so waittime is 5,5 sec right now, works good!
        this.setStartIntervall(15000l);
    }

    @Override
    public String getAGBLink() {
        return "http://creafile.com/useragree.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://creafile.com", "creafile_lang", "en");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name :</strong></td>.*?<td colspan=\".*?>(.*?)</td>").getMatch(0).trim();
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("File size :</strong></td>.*?<td colspan=\"[0-9]\">(.*?)</td>").getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replaceAll("Г", "G");
        filesize = filesize.replaceAll("М", "M");
        filesize = filesize.replaceAll("к", "k");
        filesize = filesize + "b";
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String hash = new Regex(downloadLink.getDownloadURL(), "/download/(.*)").getMatch(0);
        Form DLForm = br.getForm(1);
        DLForm.setAction("http://creafile.com/handlers.php?h=loadiframe");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        DLForm.put("hash", hash);
        br.submitForm(DLForm);
        br.getPage("http://creafile.com/handlers.php?h=getdownloadarea");
        String dllink = br.getRegex("href=\"(http://creafile.com/d/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}