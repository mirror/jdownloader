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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4upload.ru" }, urls = { "http://[\\w\\.]*?(4upload\\.ru|box4upload\\.com)/(file|wait)/[0-9a-z]+/.*?\\.html" }, flags = { 0 })
public class FourUploadRu extends PluginForHost {

    public FourUploadRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://4upload.ru/publications/rules/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("box4upload.com", "4upload.ru"));
        link.setUrlDownload(link.getDownloadURL().replace("/wait/", "/file/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Ошибка 404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"fileName\">.*?<p>(.*?)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("\"/(wait|file)/.*?/(.*?)\\.html\"").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        String filesize = br.getRegex("class=\"fileSize\">.*?<p>(.*?)</p>").getMatch(0);
        if (filesize != null) {
            filesize = filesize.trim();
            filesize = filesize.replaceAll("Г", "G");
            filesize = filesize.replaceAll("М", "M");
            filesize = filesize.replaceAll("к", "k");
            filesize = filesize.replace("б", "");
            filesize = filesize + "b";
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL().replace("/file/", "/wait/"));
        // This should only happen if the user tries to start multiple dls (if
        // one download is already running in the browser and he starts a 2nd
        // download in jd
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/file/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        for (int i = 0; i <= 5; i++) {
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.setCode(c);
            if (br.containsHTML("api.recaptcha.net")) continue;
            break;
        }
        if (br.containsHTML("api.recaptcha.net")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        // Ticket Time not needed till they check it serverside ;)
        // String ttt = br.getRegex("counter\">(\\d+)</p>").getMatch(0);
        // int tt = 90;
        // if (ttt != null) tt = Integer.parseInt(ttt);
        // sleep(tt * 1001, link);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}