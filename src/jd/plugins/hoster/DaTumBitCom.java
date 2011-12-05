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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datumbit.com" }, urls = { "http://(www\\.)?datumbit\\.com/file/.*?/" }, flags = { 0 })
public class DaTumBitCom extends PluginForHost {

    public DaTumBitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://datumbit.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        long timeBefore = System.currentTimeMillis();
        String action = br.getRegex("<form id=\"captcha_form\" method=\"POST\" action=\"(/.*?)\"").getMatch(0);
        if (action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        action = "http://datumbit.com" + action;
        boolean failed = true;
        Form dlForm = new Form();
        dlForm.setMethod(MethodType.POST);
        dlForm.setAction(action);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
        rc.setId(id);
        rc.setForm(dlForm);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("\"error\"")) {
                rc.reload();
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("\"url\":\"(/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://datumbit.com" + dllink;
        String waittime = br.getRegex("onclick=\"startTimer\\((\\d+)\\);").getMatch(0);
        if (waittime == null) waittime = br.getRegex("id=\"pause\">(\\d+)</strong> seconds").getMatch(0);
        int wait = 60;
        if (waittime != null) wait = Integer.parseInt(waittime);
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        wait -= passedTime;
        logger.info("Waittime detected, waiting " + wait + " - " + passedTime + " seconds from now on...");
        if (wait > 0) sleep(wait * 1000l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://datumbit.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("id=\"filename\">(.*?)</strong></span>").getMatch(0);
        if (filename == null) filename = br.getRegex("<span>File: <strong title=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("<span>Size:[\t\n\r ]+<strong>(.*?)</strong></span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
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