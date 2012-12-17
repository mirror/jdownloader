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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myupload.dk" }, urls = { "http://(www\\.)?myupload\\.(dk|uk)/showfile/[^<>\"/]+" }, flags = { 0 })
public class MyuploadDK extends PluginForHost {

    public MyuploadDK(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("myupload.uk/", "myupload.dk/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.myupload.dk/rules/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL().replace("/showfile/", "/download/"), false, 1);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.myupload.dk", "lang", "en");
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex(">File name:</td><td class=\"downloadTblRight\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download ([^<>\"]*?) \\| myUpload\\.dk</title>").getMatch(0);
        String filesize = br.getRegex(">Size:</td><td class=\"downloadTblRight\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        parameter.setName(filename);
        String md5 = br.getRegex(">MD5 value:</td><td class=\"downloadTblRight\">([a-z0-9]{32})</td>").getMatch(0);
        if (md5 != null) parameter.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}