//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitshare.com" }, urls = { "http://[\\w\\.]*?bitshare\\.com/(files/[a-z0-9]{8}/(.*?\\.html)?|\\?f=[a-z0-9]{8})" }, flags = { 2 })
public class BitShareCom extends PluginForHost {

    // private static final String RECAPTCHA = "/recaptcha/";
    private static final String JSONHOST    = "http://bitshare.com/files-ajax/";
    private static final String AJAXIDREGEX = "var ajaxdl = \"(.*?)\";";
    private static final String FILEIDREGEX = "bitshare\\.com/files/([a-z0-9]{8})/";
    private static final String DLLINKREGEX = "SUCCESS#(http://.+)";

    private static final String agent       = RandomUserAgent.generate();

    public BitShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://bitshare.com/premium.html");
    }

    public void correctDownloadLink(DownloadLink link) {
        String fid = new Regex(link.getDownloadURL(), "bitshare\\.com/\\?f=(.+)").getMatch(0);
        if (fid != null) link.setUrlDownload("http://bitshare.com/files/" + fid + "/.html");
    }

    @Override
    public String getAGBLink() {
        return "http://bitshare.com/terms-of-service.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        /* switch language to english */
        br.getPage("http://bitshare.com/?language=EN");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>We are sorry, but the requested file was not found in our database|>Error - File not available<|The file was deleted either by the uploader, inactivity or due to copyright claim)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex nameAndSize = br.getRegex("<h1>Downloading (.*?) - ([0-9\\.]+ [A-Za-z]+)</h1>");
        String filename = nameAndSize.getMatch(0);
        String filesize = nameAndSize.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize.replace("yte", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Sorry, you cant download more then 1 files at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("(You reached your hourly traffic limit|Your Traffic is used up for today)")) {
            String wait = br.getRegex("id=\"blocktimecounter\">(\\d+) Seconds</span>").getMatch(0);
            if (wait == null) wait = br.getRegex("var blocktime = (\\d+);").getMatch(0);
            if (wait != null)
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            else
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        String fileID = new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0);
        String tempID = br.getRegex(AJAXIDREGEX).getMatch(0);
        if (fileID == null || tempID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(JSONHOST + fileID + "/request.html", "request=generateID&ajaxid=" + tempID);
        String rgexedWait = br2.getRegex("file:(\\d+):").getMatch(0);
        int wait = 45;
        if (rgexedWait != null) {
            wait = Integer.parseInt(rgexedWait);
            logger.info("Waittime-Regex worked, regexed waittime = " + wait);
        }
        wait += 3;
        sleep(wait * 1001l, downloadLink);
        String id = br.getRegex("http://api\\.recaptcha\\.net/challenge\\?k=(.*?)\"").getMatch(0);
        if (id != null) {
            Boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                id = br.getRegex("http://api\\.recaptcha\\.net/challenge\\?k=(.*?)\"").getMatch(0);
                if (id == null) {
                    logger.warning("id is null...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                id = id.trim();
                Form reCaptchaForm = new Form();
                reCaptchaForm.setMethod(Form.MethodType.POST);
                reCaptchaForm.setAction(JSONHOST + fileID + "/request.html");
                reCaptchaForm.put("request", "validateCaptcha");
                reCaptchaForm.put("ajaxid", tempID);
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setForm(reCaptchaForm);
                rc.setId(id);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                rc.getForm().put("recaptcha_response_field", c);
                rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
                br2.submitForm(rc.getForm());
                if (br2.containsHTML("ERROR:incorrect-captcha")) {
                    br.getPage(downloadLink.getDownloadURL());
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br2.postPage(JSONHOST + fileID + "/request.html", "request=getDownloadURL&ajaxid=" + tempID);
        String dllink = br2.getRegex(DLLINKREGEX).getMatch(0);
        if (dllink == null) {
            logger.warning("The dllink couldn't be found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Remove new line
        dllink = dllink.trim();
        logger.info("Fixed dllink...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<h1>404 Not Found</h1>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", agent);
        /* switch language to english */
        br.getPage("http://bitshare.com/?language=EN");
        br.postPage("http://bitshare.com/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (!br.containsHTML("\\(<b>Premium</b>\\)")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://bitshare.com/myaccount.html");
        String space = br.getRegex(">Used Space:</td>[\t\n\r ]+<td><b>(.*?)</b></td>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String points = br.getRegex(">BitShare Points:</td>[\t\n\r ]+<td><b>(.*?)</b></td>").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex(">Valid until:</td>[\t\n\r ]+<td><b>(.*?)</b></td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(expire, "yyyy-MM-dd hh:mm", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            String tempID = br.getRegex(AJAXIDREGEX).getMatch(0);
            String fileID = new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(JSONHOST + fileID + "/request.html", "request=generateID&ajaxid=" + tempID);
            br.postPage(JSONHOST + fileID + "/request.html", "request=getDownloadURL&ajaxid=" + tempID);
            dllink = br.getRegex(DLLINKREGEX).getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -15);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // Maximum allowed connections = 15
        return 1;
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