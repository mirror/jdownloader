//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ziddu.com" }, urls = { "http://[\\w\\.]*?ziddu\\.com/((download(file)?/\\d+/.+)|(download\\.php\\?uid=.+))" }, flags = { 0 })
public class ZidduCom extends PluginForHost {

    private static final String FILEOFFLINE = "(may be deleted by the user or by the Administrator|src=images/oops\\.gif|The requested URL  was not found on this server)";

    public ZidduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ziddu.com/termsandconditions.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        String Url = downloadLink.getDownloadURL();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(Url);
        if (br.containsHTML(FILEOFFLINE) || br.getURL().contains("msg=File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("top\\.document\\.title=\"Download (.*?) in Ziddu\"").getMatch(0);
        if (filename == null) filename = br.getRegex("download/\\d+/(.*?)\\.html").getMatch(0);
        String filesize = br.getRegex("File Size.*?class=\"fontfamilyverdana normal12black\">([^<>\"]*?)</tr>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename));
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form form = br.getFormbyProperty("name", "dfrm");
        this.sleep(500, downloadLink);
        br.submitForm(form);
        if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Is too Busy,Please <br>")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
        form = br.getFormbyProperty("name", "securefrm");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String capurl = form.getRegex("(/CaptchaSecurityImages\\.php\\?width=\\d+&height=\\d+&characters=\\d)").getMatch(0);
        if (capurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode("http://downloads.ziddu.com" + capurl, downloadLink);
        form.put("securitycode", code);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
        /*
         * Folgendes nicht optimal da bei .isContentDisposition == false immer angenommen wird dass das Captcha falsch war.
         */
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("FILEOFFLINE")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!");
            try {
                invalidateLastChallengeResponse();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
        }
        try {
            validateLastChallengeResponse();
        } catch (final Throwable e) {
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}