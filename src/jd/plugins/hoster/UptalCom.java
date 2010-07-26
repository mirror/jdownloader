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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uptal.com" }, urls = { "http://[\\w\\.]*?uptal\\.com/\\?d=[A-Fa-f0-9]+" }, flags = { 0 })
public class UptalCom extends PluginForHost {

    public UptalCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uptal.com/faq.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("uptal.com", "uptal.org"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File size:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String getlink = null;
        String filename = downloadLink.getName();
        br.setDebug(true);
        br.setFollowRedirects(true);
        getlink = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
        if (getlink == null) getlink = br.getRegex("name=downloadurl value=\"(.*?)\"").getMatch(0);
        if (getlink == null && br.containsHTML("captcha.php")) {
            /* captcha? */
            String code = getCaptchaCode("http://www.uptal.com/captcha.php", downloadLink);
            Form form = br.getForm(1);
            form.put("captchacode", code);
            br.submitForm(form);
            if (!br.containsHTML("com/getfile.php")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            getlink = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);
            if (getlink == null) getlink = br.getRegex("name=downloadurl value=\"(.*?)\"").getMatch(0);
        }
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        getlink = getlink.replaceAll(" ", "%20");
        this.sleep(3000, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, false, 1);
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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
