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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://[\\w\\.]*?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 2 })
public class FileServeCom extends PluginForHost {

    public FileServeCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileserve.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    public String FILEIDREGEX = "fileserve\\.com/file/([a-zA-Z0-9]+)";
    private boolean isFree = false;

    @Override
    public void correctDownloadLink(DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        String fileId = new Regex(link.getDownloadURL(), FILEIDREGEX).getMatch(0);
        link.setUrlDownload("http://fileserve.com/file/" + fileId);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return link.getAvailableStatus();
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage("http://fileserve.com/login.php", "loginUserName=" + Encoding.urlEncode(account.getUser()) + "&loginUserPassword=" + Encoding.urlEncode(account.getPass()) + "&autoLogin=on&loginFormSubmit=Login");
        String accType = br.getRegex("<h5>Account type:</h5>[\r\n ]+<h3>(Premium|Free)</h3>").getMatch(0);
        if (accType == null) accType = br.getRegex("<h4>Account Type</h4></td> <td><h5 class=\"inline\">(Premium|Free)([ ]+)?</h5>").getMatch(0);
        if (br.getCookie("http://fileserve.com", "cookie") == null || accType == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!accType.equals("Premium")) {
            try {
                account.setMaxSimultanDownloads(1);
            } catch (Throwable e) {
                /* not available in 0.9xxx */
            }
            account.setProperty("type", "free");
            isFree = true;
        } else {
            account.setProperty("type", null);
        }
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
        String uploadedFiles = br.getRegex("<h5>Files uploaded:</h5>\r\n[ ]+<h3>(\\d+)<span>").getMatch(0);
        if (uploadedFiles != null) ai.setFilesNum(Integer.parseInt(uploadedFiles));
        ai.setUnlimitedTraffic();
        if (isFree)
            ai.setStatus("Registered (free) User");
        else
            ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        if (isFree) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            br.setFollowRedirects(false);
            br.postPage(link.getDownloadURL(), "download=premium");
            String dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                handleErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        int maxdls = -1;
        try {
            if (AccountController.getInstance().getValidAccount(this).getStringProperty("type").toString() != null) maxdls = 1;
        } catch (Exception e) {

        }
        return maxdls;
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML("File not available, please register as <a href=\"/login\\.php\">Premium</a> Member to download<br")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileServeCom.errors.only4premium", "This file is only downloadable for premium users"));
        if (br.containsHTML(">Your download link has expired\\.<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download link expired, contact fileserve support", 30 * 60 * 1000l);
        String fileId = br.getRegex("fileserve\\.com/file/([a-zA-Z0-9]+)").getMatch(0);
        br.setFollowRedirects(false);
        String captchaJSPage = br.getRegex("\"(/landing/.*?/download_captcha\\.js)\"").getMatch(0);
        if (captchaJSPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaJSPage = "http://fileserve.com" + captchaJSPage;
        Browser br2 = br.cloneBrowser();
        // It doesn't work without accessing this page!!
        br2.getPage(captchaJSPage);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (!br.containsHTML("<div id=\"captchaArea\" style=\"display:none;\">") || !br2.containsHTML("//showCaptcha\\(\\);")) {
            Boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                String id = br.getRegex("var reCAPTCHA_publickey='(.*?)';").getMatch(0);
                if (!br.containsHTML("api.recaptcha.net") || id == null || fileId == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                Form reCaptchaForm = new Form();
                reCaptchaForm.setMethod(Form.MethodType.POST);
                reCaptchaForm.setAction("http://www.fileserve.com/checkReCaptcha.php");
                reCaptchaForm.put("recaptcha_shortencode_field", fileId);
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
                if (br2.containsHTML("incorrect-captcha")) {
                    br.getPage(downloadLink.getDownloadURL());
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        String reconTime = br.getRegex("(\\d+)").getMatch(0);
        int tt = 60;
        if (reconTime != null) {
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime);
        }
        sleep(tt * 1001, downloadLink);
        br2.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        br.postPage(downloadLink.getDownloadURL(), "download=normal");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            String wait = br.getRegex("You (have to|need to) wait (\\d+) seconds to start another download").getMatch(1);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String wait = br.getRegex("You (have to|need to) wait (\\d+) seconds to start another download").getMatch(1);
            if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        doFree(downloadLink);
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("submit=Check+Urls&urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
                br.getPage("http://fileserve.com/link-checker.php");
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://fileserve.com/link-checker.php", sb.toString());
                for (DownloadLink dl : links) {
                    String fileid = new Regex(dl.getDownloadURL(), FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    String regexForThisLink = "(<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>))";
                    String theData = br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    Regex linkinformation = new Regex(theData, "<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>)");
                    String status = linkinformation.getMatch(3);
                    String filename = linkinformation.getMatch(1);
                    String filesize = linkinformation.getMatch(2);
                    if (filename == null || filesize == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("Available") || filename.equals("--") || filesize.equals("--")) {
                        filename = fileid;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    dl.setDownloadSize(Regex.getSize(filesize));
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("(<h1>404 - Page not found</h1>|<p>We are sorry...</p>|<p>The page you were trying to reach wasn't there\\.</p>|<p>You can only download 1 file at a time|URL=http://www\\.fileserve\\.com/landing-403\\.php\")")) throw new PluginException(LinkStatus.ERROR_FATAL, "FATAL Server error, contact fileserve support");
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