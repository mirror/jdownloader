//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2shared.com" }, urls = { "http://(www\\.)?2shared\\.com/(audio|file|video|photo|document)/.*?/[\\w\\-\\.]+" }, flags = { 0 })
public class TwoSharedCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.2shared.com";

    public TwoSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(audio|video|photo|document)/", "/file/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String finallink = null;
        String link = br.getRegex("(\'|\")(http://[0-9a-z]+\\.2shared\\.com/download/[0-9a-zA-Z]+/.*?\\?tsid=[\\w\\-]+)(\'|\")").getMatch(1);
        // special handling for picture-links
        if (br.containsHTML(">Loading image")) {
            br.getPage(MAINPAGE + link);
            finallink = br.toString();
        }
        link = link == null ? br.getRegex("<div style=\"display:none\" id=\"\\w+\">(.*?)</div>").getMatch(0) : link;
        finallink = finallink == null ? link : finallink;
        finallink = finallink == null ? br.getRegex("window\\.location ='(.*?)';").getMatch(0) : finallink;
        if (finallink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        finallink = finallink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getURL().getQuery() == null) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            if (dl.getConnection().getURL().getQuery().contains("ip-lim")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.2sharedcom.errors.sessionlimit", "Session limit reached! "), 10 * 60 * 1000l);
            } else if (dl.getConnection().getURL().getQuery().contains("MAX_SESSION")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.2sharedcom.errors.maxsession", "User downloading session limit is reached! "), 10 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The file link that you requested is not valid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form pwform = br.getForm(0);
        if (pwform != null && pwform.containsHTML("password") && !pwform.getAction().contains("paypal")) {
            String passCode = downloadLink.getStringProperty("pass", null);
            for (int i = 0; i <= 3; i++) {
                passCode = passCode == null ? Plugin.getUserInput(null, downloadLink) : passCode;
                pwform.put("userPass2", passCode);
                br.submitForm(pwform);
                pwform = br.getForm(0);
                if (br.containsHTML("The password you have entered is not valid")) {
                    logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                    passCode = null;
                } else {
                    downloadLink.setProperty("pass", passCode);
                    break;
                }
            }
            if (br.containsHTML("The password you have entered is not valid")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Wrong password entered"); }
        }
        String filename = br.getRegex("<h1>(.*?)</h1>\\s?download").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h2>(.*?)\\sdownload</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)\\s2shared\\s\\-\\sdownload</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        // filesize regex for picturelinks
        if (filesize == null) {
            filesize = br.getRegex("class=\"bodytitle\">Loading image \\((.*?)\\)\\.\\.\\. Please wait").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}