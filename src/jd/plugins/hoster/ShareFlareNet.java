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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareflare.net" }, urls = { "http://[\\w\\.]*?shareflare\\.net/download/.*?/.*?\\.html" }, flags = { 2 })
public class ShareFlareNet extends PluginForHost {

    public ShareFlareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        enablePremium("http://shareflare.net/page/premium.php");
    }

    private static final String NEXTPAGE = "http://shareflare.net/tmpl/tmpl_frame_top.php?link=";

    @Override
    public String getAGBLink() {
        return "http://shareflare.net/page/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://shareflare.net", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        if (br.containsHTML("(File not found|deleted for abuse or something like this)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("id=\"file-info\">(.*?)<small").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("name=\"name\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("name=\"realname\" value=\"(.*?)\"").getMatch(0);
            }
        }
        String filesize = br.getRegex("name=\"sssize\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        Form dlform = br.getFormbyProperty("id", "dvifree");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(dlform);
        String linkframe = null;
        for (int i = 0; i <= 3; i++) {
            br.setFollowRedirects(false);
            Form captchaform = br.getFormbyProperty("id", "dvifree");
            String captchaUrl = br.getRegex("<div class=\"cont c2\" align=\"center\">.*?<img src='(http.*?)'").getMatch(0);
            if (captchaUrl == null) {
                captchaUrl = br.getRegex("('|\")(http://letitbit\\.net/cap\\.php\\?jpg=.*?\\.jpg)('|\")").getMatch(1);
                if (captchaUrl == null) {
                    String capid = br.getRegex("name=\"(uid2|uid)\" value=\"(.*?)\"").getMatch(1);
                    if (capid != null) captchaUrl = "http://letitbit.net/cap.php?jpg=" + capid + ".jpg";
                }
            }
            if (captchaform == null || captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            captchaform.put("cap", code);
            br.submitForm(captchaform);
            linkframe = br.getRegex("<frame src=\"(.*?)\"").getMatch(0);
            if (linkframe == null) linkframe = br.getRegex("\"(http://s[0-9]+\\.shareflare\\.net/tmpl/tmpl_frame_top\\.php\\?link=)\"").getMatch(0);
            if (linkframe == null) {
                // Get back to the previous page
                br.submitForm(dlform);
                continue;
            }
            break;
        }
        if (linkframe == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage(linkframe);
        br.getPage(NEXTPAGE);
        String wait = br.getRegex("y =.*?(\\d+);").getMatch(0);
        int tt = 60;
        if (wait != null) {
            logger.info("Regexed waittime is found...");
            tt = Integer.parseInt(wait);
        }
        sleep(tt * 1001, downloadLink);
        br.getPage(NEXTPAGE);
        String dllink = br.getRegex("DownloadClick\\(\\);\" href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://[0-9]+\\.[0-9]+\\..*?/download[0-9]+/.*?/.*?/shareflare\\.net/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("title>Error</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        Form premForm = null;
        Form allForms[] = br.getForms();
        if (allForms == null || allForms.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (Form aForm : allForms) {
            if (aForm.containsHTML("\"pass\"")) {
                premForm = aForm;
                break;
            }
        }
        if (premForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        premForm.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(premForm);
        if (br.containsHTML("<b>Given password does not exist")) {
            logger.info("Downloadpassword seems to be wrong, disabeling account now!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String url = Encoding.htmlDecode(br.getRegex(Pattern.compile("valign=\"middle\"><br><span style=\"font-size:12px;\"><a href='(http://.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (url == null) url = br.getRegex("('|\")(http://\\d+\\.\\d+\\.\\d+\\.\\d+/downloadp\\d+/.*?\\..*?/\\d+/shareflare\\.net/.*?)('|\")").getMatch(1);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}