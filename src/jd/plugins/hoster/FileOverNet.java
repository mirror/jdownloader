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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileover.net" }, urls = { "http(s)?://(www\\.)?fileover\\.net/\\d+" }, flags = { 0 })
public class FileOverNet extends PluginForHost {

    public FileOverNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://fileover.net/contacts/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/deleted/") || br.containsHTML("(<title>No such file \\| Fileover\\.Net\\! \\- Error</title>|>No such file<|The following file is unavailable\\.|>Not found or deleted by a user\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 style=\"text\\-align: center; padding: 0 50px 10px 50px; word\\-wrap: break\\-word;\">(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| Fileover\\.Net\\! \\- Download</title>").getMatch(0);
        String filesize = br.getRegex("<h3 id=\"filesize\">File Size: (.*?)</h3>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        // Filesize is only shown if no limits are reached
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        int minutes = 0, seconds = 0;
        Regex limits = br.getRegex(">You have to wait: (\\d+) minutes (\\d+) seconds");
        String mins = limits.getMatch(0);
        String secs = limits.getMatch(1);
        if (secs == null) secs = br.getRegex(">You have to wait: (\\d+) seconds").getMatch(0);
        if (mins != null) minutes = Integer.parseInt(mins);
        if (secs != null) seconds = Integer.parseInt(secs);
        int waittime = ((60 * minutes) + seconds + 1) * 1000;
        if (waittime > 1000) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String fileID = new Regex(downloadLink.getDownloadURL(), "fileover\\.net/(\\d+)").getMatch(0);
        br.getPage("https://fileover.net/ax/timereq.flo?" + fileID);
        String hash = br.getRegex("hash\":\"(.*?)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("https://fileover.net/ax/timepoll.flo?file=" + fileID + "&hash=" + hash);
        Form recaptchaForm = new Form();
        recaptchaForm.setMethod(MethodType.POST);
        recaptchaForm.setAction("https://fileover.net/ax/timepoll.flo");
        recaptchaForm.put("file", fileID);
        recaptchaForm.put("hash", hash);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(recaptchaForm);
            rc.findID();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("google\\.com/recaptcha/api/")) {
                rc.reload();
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("</style></head><body><a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://\\d+\\.pool\\.fileover\\.net/\\d+/[a-z0-9]+/.*?)\"").getMatch(0);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}