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
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LnkCrptWs;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bytesbox.com" }, urls = { "http://(www\\.)?bytesbox\\.com/\\!/[A-Za-z0-9]+" }, flags = { 0 })
public class BytesBoxCom extends PluginForHost {

    private static AtomicBoolean isWaittimeDetected = new AtomicBoolean(false);

    public BytesBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://bytesbox.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Download: ([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>BytesBox\\.com \\- Download ([^<>\"]*?)</title>").getMatch(0);
        if (filename != null) link.setName(Encoding.htmlDecode(filename.trim()));
        isWaittimeDetected.set(false);
        if (br.containsHTML("class=\"inactiveButton\">Wait<span")) isWaittimeDetected.set(true);
        if (isWaittimeDetected.get() == true) return AvailableStatus.TRUE;
        String filesize = br.getRegex(">Download<span style=\" font\\-weight:normal;\"><br>([^<>\"]*?)</span></a>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* waittime */
        if (br.containsHTML("class=\"inactiveButton\">Wait<span")) {
            isWaittimeDetected.set(true);
            long wait = 40 * 60 * 1000l;
            String waittime = br.getRegex("(\\d+:\\d+) Mins").getMatch(0);
            if (waittime != null) wait = (Integer.parseInt(waittime.split(":")[0]) * 60 + Integer.parseInt(waittime.split(":")[1])) * 1000l;
            if (wait > 30000) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
            } else {
                sleep(wait, downloadLink);
            }
        }
        /* captcha */
        final String downsess = br.getRegex("var downsess = \"([a-z0-9]+)\";").getMatch(0);
        if (downsess == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 0; i <= 3; i++) {
            final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
            jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((LnkCrptWs) solveplug).getSolveMedia(br);
            sm.setSecure(false);
            sm.setNoscript(true);
            final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
            final String code = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage("http://bytesbox.com/ajax.solvcaptcha.php", "downsess=" + downsess + "&adcopy_challenge=" + sm.verify(code) + "&adcopy_response=" + Encoding.urlEncode(code));
            if (ajaxBR.containsHTML("\"status\":\"ERROR\"")) continue;
            break;
        }
        if (ajaxBR.containsHTML("\"status\":\"ERROR\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.postPage("/getdownlink.php", "down_sess=" + downsess + "&file=" + getFileId(downloadLink));
        String dllink = br.getRegex("link\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFileId(DownloadLink downloadLink) {
        String dllink = downloadLink.getDownloadURL();
        return new Regex(dllink, "(?i)\\!/([0-9a-z]+)/?$").getMatch(0);
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