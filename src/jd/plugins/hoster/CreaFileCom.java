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

    @Override
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

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String downarea = "http://creafile.com/handlers.php?h=getdownloadarea";
        br.setFollowRedirects(true);
        String dllink = null;
        boolean slowdownload = false;
        Form dlForm = br.getFormbyKey("s_pair");
        if (dlForm == null) dlForm = br.getForm(1);
        dlForm.remove("Buy+VIP");
        // Old Captcha handling
        // String[] letters = br.getRegex("captcha/(\\d+)").getColumn(0);
        // if (letters == null || letters.length == 0 || dlForm == null) throw
        // new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // StringBuilder code = new StringBuilder();
        // for (String value : letters) {
        // code.append(value);
        // }
        // logger.info("Captchacode to enter is " + code.toString());
        //***New Captcha handling***
        String captchaurl = "http://creafile.com//codeimg.php";
        if (!br.containsHTML("codeimg.php")) captchaurl = null;
        if (dlForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode(captchaurl, downloadLink);
        String hash = new Regex(downloadLink.getDownloadURL(), "/download/(.*)").getMatch(0);
        dlForm.put("hash", hash);
        dlForm.put("captcha", code.toString());
        br.submitForm(dlForm);
        br.getPage(downarea);
        String dllink0 = br.getRegex("href=\"(http://creafile.com/d/.*?)\"").getMatch(0);
        Form faster = br.getFormbyProperty("id", "fasters");
        if (faster == null) faster = br.getForm(0);
        if (faster != null) {
            br.submitForm(faster);
            br.getPage(downarea);
            dllink = br.getRegex("href=\"(http://creafile.com/d/.*?)\"").getMatch(0);
        }
        if (dllink == null && dllink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink == null && dllink0 != null) {
            logger.warning("Downloading using slow download link, plugin is not working as it should!, Slow link = " + dllink0);
            dllink = dllink0;
            slowdownload = true;
        }
        // Downloading using the slow download link the server doesn't allow
        // more than 1 connection per file
        if (slowdownload == false) {
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        } else {
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}