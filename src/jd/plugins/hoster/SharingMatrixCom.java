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
import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharingmatrix.com" }, urls = { "http://[\\w\\.]*?sharingmatrix\\.com/file/[0-9]+" }, flags = { 2 })
public class SharingMatrixCom extends PluginForHost {

    public SharingMatrixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://sharingmatrix.com/contact";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:.*?<td>(.*?)</td>").getMatch(0);
        Regex reg = br.getRegex("<th>Size:.*?<td>(.*?)&nbsp;(.*?)</td>");
        String filesize = reg.getMatch(0) + reg.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // System.out.print(br.toString());
        // if
        // (br.containsHTML("You have got max allowed bandwidth size per hour"))
        // { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED); }
        String linkid = br.getRegex("link_id = '(\\d+)';").getMatch(0);
        if (linkid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        String freepage = "http://sharingmatrix.com/ajax_scripts/download.php?type_membership=free&link_id=" + linkid;
        br.getPage(freepage);
        String link_name = br.getRegex("link_name = '([^']+')").getMatch(0);
        // System.out.print(br.toString());
//        long time = System.currentTimeMillis();
        String linkurl = br.getRegex("<input\\.*document\\.location=\"(.*?)\";").getMatch(0);
        // String captchalink =
        // "http://sharingmatrix.com/include/crypt/cryptographp.php?cfg=0&";
        // br.cloneBrowser().getPage(captchalink);
        String captchalink = "http://sharingmatrix.com/include/crypt/cryptographp.inc.php?cfg=0&sn=PHPSESSID&";

        File captcha = getLocalCaptchaFile();

        Browser brc = br.cloneBrowser();
        brc.setCookiesExclusive(true);
        brc.setCookie("sharingmatrix.com", "cryptcookietest", "1");
        brc.getDownload(captcha, captchalink);
        // br.cloneBrowser().getPage("http://sharingmatrix.com/js/jquery-impromptu.1.5.js?_="
        // + System.currentTimeMillis());
        String code = getCaptchaCode(captcha, downloadLink);
        Browser br2 = br.cloneBrowser();
        // looks like wait time is js only (can be skipped)
        // long delay = 60000 - (System.currentTimeMillis() - time);
        // try {
        // delay =
        // (Long.parseLong(br.getRegex("current_time = '([\\d]+)';").getMatch(0))
        // * 1000) - (System.currentTimeMillis() - time);
        // } catch (Exception e) {
        // }
        // sleep(delay, downloadLink);
        br2.postPage("http://sharingmatrix.com/ajax_scripts/verifier.php", "?&code=" + code);
        if (Integer.parseInt(br2.toString().trim()) != 1) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dl_id = br2.getPage("http://sharingmatrix.com/ajax_scripts/dl.php").trim();
        // br2.getPage("http://sharingmatrix.com/ajax_scripts/update_dl.php?id="
        // + dl_id);
        br2.getPage("http://sharingmatrix.com/ajax_scripts/_get.php?link_id=" + linkid + "&link_name=" + link_name + "&dl_id=" + dl_id + "&password=");
        linkurl = br2.getRegex("serv:\"([^\"]+)\"").getMatch(0) + "/download/" + br2.getRegex("hash:\"([^\"]+)\"").getMatch(0) + "/" + dl_id.trim() + "/";
        // br2.getPage("http://sharingmatrix.com/ajax_scripts/update_dl.php?id="
        // + dl_id);
        // System.out.println(br2.getCookies("http://sharingmatrix.com"));
        // System.out.print(br2.toString());
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, linkurl, false, 1);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}