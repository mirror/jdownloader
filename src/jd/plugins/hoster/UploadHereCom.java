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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

/**
 * Hoster belongs to multiupload.com & uploadking.com, uses similar code for
 * some parts
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadhere.com" }, urls = { "http://(www\\.)?uploadhere\\.com/[A-Z0-9]+" }, flags = { 0 })
public class UploadHereCom extends PluginForHost {

    private static final String TEMPORARYUNAVAILABLE         = "(>Unfortunately, this file is temporarily unavailable|> \\- The server the file is residing on is currently down for maintenance)";

    private static final String TEMPORARYUNAVAILABLEUSERTEXT = "This file is temporary unavailable!";

    public UploadHereCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadhere.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.uploadherecom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(>Unfortunately, this file is unavailable|> \\- Invalid link|> \\- The file has been deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infoWhileLimitReached = br.getRegex(">You are currently downloading (.*?) \\((\\d+.*?)\\)\\. Please ");
        String filename = br.getRegex("\">Filename:</font><font style=\"font\\-size:\\d+px; color:#[A-Z0-9]+; font\\-weight:bold;\">(.*?)</div>").getMatch(0);
        if (filename == null) filename = infoWhileLimitReached.getMatch(0);
        String filesize = br.getRegex(">Filesize:</font><font style=\"font\\-size:\\d+px; font\\-weight:bold;\">(.*?)</font>").getMatch(0);
        if (filesize == null) filesize = infoWhileLimitReached.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setReadTimeout(2 * 60 * 1000);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.uploadherecom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 60 * 60 * 1000l);
            if (br.containsHTML("(>You are currently downloading|this download, before starting another\\.</font>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
            final String rcID = br.getRegex("Recaptcha\\.create\\(\"([^/<>\"]+)\"").getMatch(0);
            if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            final Form cForm = new Form();
            cForm.setMethod(MethodType.POST);
            final String fid = new Regex(downloadLink.getDownloadURL(), "uploadhere\\.com/(.+)").getMatch(0);
            final String action = "http://www.uploadhere.com/" + fid + "?c=" + fid;
            cForm.setAction(action);
            br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(cForm);
            rc.setId(rcID);
            for (int i = 0; i <= 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                br.postPage(action, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (br.containsHTML("\"response\":\"0\"")) continue;
                break;
            }
            dllink = br.getRegex("href\":\"(http:.*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = dllink.replace("\\", "");
        }
        // More connections possible but doesn't work for all links
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -12);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}