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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files-save.com" }, urls = { "http://(www\\.)?files\\-save\\.com/(fr|en)/download\\-[a-z0-9]{32}\\.html" }, flags = { 0 })
public class FilesSaveCom extends PluginForHost {

    public FilesSaveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.files-save.com/fr/faq/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.files-save.com/en/download-" + new Regex(link.getDownloadURL(), "download\\-([a-z0-9]{32}\\.html)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.files-save.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The requested file does not exist<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename :</td><td class=\"td\\-haut\\-gd\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"title_top\"><h3>Download file : ([^<>\"]*?)</h3>").getMatch(0);
        final String extension = br.getRegex(">File Type :</td><td class=\"td\\-haut\\-gd1\">([^<>\"/]*?)</td>").getMatch(0);
        String filesize = br.getRegex(">Filesize :</td><td class=\"td\\-haut\\-gd1\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null || extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "." + extension);
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("Mo", "Mb")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String reconnectWait = br.getRegex("count=(\\d+);").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
        if (!br.containsHTML("/img_key_protect\\.png")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String code = getCaptchaCode("http://www.files-save.com/img_key_protect.png", downloadLink);
        if (code.length() != 6) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.postPage(br.getURL(), "verif_code=" + code);
        if (br.containsHTML("/img_key_protect\\.png")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("id=\"downloadlink\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://ftp\\d+\\.files\\-save\\.com/files/[^<>\"]*?)\"").getMatch(0);
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
        // Only possible when starting all at the same time, maybe even more possible but didn't have/find any more working example links
        return 3;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}