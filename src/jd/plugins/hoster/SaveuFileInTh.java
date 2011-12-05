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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "saveufile.in.th" }, urls = { "http://(www\\.)?saveufile\\.(in\\.th|com)/car\\.php\\?file=[a-z0-9]+" }, flags = { 0 })
public class SaveuFileInTh extends PluginForHost {

    public SaveuFileInTh(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.saveufile.com/service.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form dlForm = br.getFormbyProperty("id", "form_download");
        if (dlForm == null) dlForm = br.getForm(0);
        String captchalink = br.getRegex("</h3><div><img src=\"(s.*?)\"").getMatch(0);
        if (captchalink == null) captchalink = br.getRegex("\"(securimage/securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
        if (dlForm == null || captchalink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 0;
        String waittime = br.getRegex("var bnyme = (\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        final String code = getCaptchaCode(captchalink, downloadLink);
        dlForm.put("code", code);
        br.submitForm(dlForm);
        if (br.containsHTML("IP นี้เป็นของต่างประเทศ ไม่สามารถดาวน์โหลดไฟล์ได้ ติดต่อสอบถามได้ทาง")) throw new PluginException(LinkStatus.ERROR_FATAL, "Download only possible through tai IP adress.");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.contains("saveufile.in.th/car.php?file=")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    /**
     * Important: Hoster is (sometimes) only accessible in Thailand (ur using a
     * working proxy)
     */
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(File not found\\.|<title>ฝากไฟล์ , อัพโหลด  โดย </title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fInfo = br.getRegex("<title>ฝากไฟล์ (.*?), อัพโหลด (.*?) โดย ");
        if (fInfo.getMatch(0) == null || fInfo.getMatch(1) == null) fInfo = br.getRegex("<h3 style=\"font\\-size: 14px; margin\\-bottom: 10px;\">ฝากไฟล์ (.*?), อัพโหลด (.*?) โดย ");
        String filename = fInfo.getMatch(0);
        String filesize = fInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /** Set final filename here because server sometimes sends bad names */
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}