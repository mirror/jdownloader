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
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareflare.net" }, urls = { "http://(www\\.)?shareflare\\.net/download/.*?/.*?\\.html" }, flags = { 2 })
public class ShareFlareNet extends PluginForHost {

    private static final String FREEDOWNLOADPOSSIBLE = "download4";
    private static final Object LOCK                 = new Object();
    private static final String FREELIMIT            = ">Your limit for free downloads is over for today<";

    public ShareFlareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        enablePremium("http://shareflare.net/page/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://shareflare.net/page/terms.php";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\?", "%3F"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://shareflare.net", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        if (br.containsHTML("(File not found|deleted for abuse or something like this|\"http://up\\-file\\.com/find/)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (!br.containsHTML(FREEDOWNLOADPOSSIBLE) && !br.containsHTML(FREELIMIT)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.shareflarenet.nofreedownloadlink", "No free download link for this file"));
        return AvailableStatus.TRUE;
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (LOCK) {
            AccountInfo ai = new AccountInfo();
            if (account.getUser() != null && account.getUser().length() > 0) {
                ai.setStatus("Please leave username empty and enter premium code as password only!");
                account.setValid(false);
            } else {
                ai.setStatus("Status can only be checked while downloading!");
                account.setValid(true);
            }
            return ai;
        }
    }

    private String getDllink(final Browser br) {
        String dllink = br.getRegex("\"(http:[^<>\"]*?)\"").getMatch(0);
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String waittime = br.getRegex("You can wait download for ([\t\n\r0-9]+) minutes or upgrade to premium").getMatch(0);
        if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime.trim()) * 60 * 1001l);
        if (br.containsHTML("You reached your hourly traffic limit\\.")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
        if (br.containsHTML(FREELIMIT)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        if (br.containsHTML("(В бесплатном режиме вы можете скачивать только один файл|You are currently downloading|Free users are allowed to only one parallel download\\.\\.)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        br.setFollowRedirects(false);
        String debug = br.toString();
        Form dlform = br.getFormbyProperty("id", "dvifree");
        if (dlform == null) {
            if (!br.containsHTML(FREEDOWNLOADPOSSIBLE)) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.shareflarenet.nofreedownloadlink", "No free download link for this file"));
            logger.warning("dlform is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        debug = debug + br.submitForm(dlform);
        dlform = br.getFormbyProperty("id", "dvifree");
        if (dlform == null) {
            logger.warning("dlform#2 is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(dlform);
        // Waittime before captcha is skippable
        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 0; i <= 3; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            ajaxBR.postPage("http://shareflare.net/ajax/check_recaptcha.php", "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
            if (ajaxBR.containsHTML("error_wrong_captcha")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (ajaxBR.containsHTML("error_wrong_captcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = getDllink(ajaxBR);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("html") && con.getLongContentLength() < (downloadLink.getDownloadSize() / 2)) {
            logger.warning("the dllink doesn't seem to be a file, following the connection...");
            br.followConnection();
            if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l);
            if (br.containsHTML("title>Error</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (account.getUser() != null && account.getUser().length() > 0) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
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
        /** 1 point = 1 GB */
        String points = br.getRegex(">Points:</span>([0-9\\.]+)\\&nbsp;").getMatch(0);
        if (points == null) points = br.getRegex("<p>You have: ([0-9\\.]+) Points</p>").getMatch(0);
        if (points != null) {
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) {
                ai = new AccountInfo();
                account.setAccountInfo(ai);
            }
            ai.setTrafficLeft(SizeFormatter.getSize(points + "GB"));
        }
        if (br.containsHTML("(>The file is temporarily unavailable for download|Please try a little bit later\\.<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Servererror", 60 * 60 * 1000l);
        String url = Encoding.htmlDecode(br.getRegex(Pattern.compile("valign=\"middle\"><br><span style=\"font-size:12px;\"><a href='(http://.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (url == null) {
            url = br.getRegex("('|\")(http://\\d+\\.\\d+\\.\\d+\\.\\d+/[^<>\"\\']+\\d+/[^<>\"\\']+/" + downloadLink.getName() + ")('|\")").getMatch(1);
            if (url == null) url = br.getRegex("class=\"btn\\-corner\\-tl\"><a style=\\'font\\-size: 16px\\' href=\\'(http://[^<>\"\\']+)\\'").getMatch(0);
            if (url == null) url = br.getRegex("Link to the file download\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        }
        if (url == null) {
            if (br.containsHTML("The premium key you provided does not exist")) {
                logger.info("The premium key you provided does not exist");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}