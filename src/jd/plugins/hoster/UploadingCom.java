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
import java.util.ArrayList;

import jd.PluginWrapper;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploading.com" }, urls = { "http://[\\w\\.]*?uploading\\.com/files/\\w+" }, flags = { 2 })
public class UploadingCom extends PluginForHost {
    private static int simultanpremium = 1;
    private static final Object PREMLOCK = new Object();
    private String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.0; chrome://global/locale/intl.properties; rv:1.8.1.12) Gecko/2008102920  Firefox/3.0.0";
    private boolean free = false;

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
        this.enablePremium("http://www.uploading.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    public boolean isPremium() throws IOException {
        boolean follow = br.isFollowingRedirects();
        br.setFollowRedirects(true);
        br.getPage("http://www.uploading.com/");
        br.setFollowRedirects(follow);
        if (br.containsHTML("UPGRADE TO PREMIUM")) return false;
        if (br.containsHTML("Membership: Premium")) return true;
        return false;
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", userAgent);
        br.setCookie("http://www.uploading.com/", "lang", "1");
        br.setCookie("http://www.uploading.com/", "language", "1");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.getPage("http://www.uploading.com/");
        br.postPage("http://uploading.com/general/login_form/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on");
        if (br.getCookie("http://www.uploading.com/", "remembered_user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        /* change language to english */
        br.postPage("http://uploading.com/general/select_language/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "language=1");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            account.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        account.setValid(true);
        String validUntil = br.getRegex("Valid Until:(.*?)<").getMatch(0);
        if (validUntil != null) {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil.trim(), "MMM dd, yyyy", null));
        } else {
            /* fallback */
            ai.setValidUntil(br.getCookies("http://www.uploading.com/").get("remembered_user").getExpireDate());
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        free = false;
        br.setDebug(true);
        synchronized (PREMLOCK) {
            login(account);
            if (!isPremium()) {
                simultanpremium = 1;
                free = true;
            } else {
                if (simultanpremium + 1 > 20) {
                    simultanpremium = 20;
                } else {
                    simultanpremium++;
                }
            }
            requestFileInformation(link);
        }
        br.getPage(link.getDownloadURL());
        if (free) {
            handleFree0(link);
            return;
        }
        String redirect = getDownloadUrl(link);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, true, 0);
        handleDownloadErrors();
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    public void checkErrors() throws PluginException {
        logger.info("Checking errors");
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.uploadingcom.errors.countrylimitreached", "You reached your country daily limit"), 60 * 60 * 1000l);
        if (br.containsHTML("you have reached your daily download limi")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
        if (br.containsHTML("Your IP address is currently downloading a file")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("Only Premium users can download files larger than")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
        if (br.containsHTML("You have reached the daily downloads limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
        if (br.containsHTML("you can download only one file per")) {
            int wait = 15;
            String time = br.getRegex("you can download only one file per (\\d+) minutes").getMatch(0);
            if (time != null) wait = Integer.parseInt(time.trim());
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 60 * 1000l);
        }
    }

    private void handleDownloadErrors() throws IOException, PluginException {
        logger.info("Handling errors");
        if (dl.getConnection().getResponseCode() == 416) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 1000l * 60 * 30);
        }
        if (!dl.getConnection().isContentDisposition()) {
            String error = dl.getConnection().getRequest().getCookies().get("error").getValue();
            br.followConnection();
            if (error != null && error.contains("wait")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 1000l * 15);
            logger.warning("dl isn't ContentDisposition, plugin must be broken!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 1000l * 60 * 30);
        }
    }

    public void handleFree0(DownloadLink link) throws Exception {
        checkErrors();
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        if (form == null) {
            logger.warning("The first form equals null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Submitting form");
        br.submitForm(form);
        checkErrors();
        String redirect = getDownloadUrl(link);
        br.setFollowRedirects(false);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, redirect, true, 1);
        handleDownloadErrors();
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", userAgent);
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "lang", "1");
        br.setCookie("http://www.uploading.com/", "language", "1");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
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
                br.postPage("http://uploading.com/files/checker/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", sb.toString());
                String correctedHTML = br.toString().replace("\\", "");
                for (DownloadLink dl : links) {
                    String fileid = new Regex(dl.getDownloadURL(), "uploading\\.com/files/(.+)").getMatch(0);
                    if (fileid == null) {
                        logger.warning("Uploading.com availablecheck is broken!");
                        return false;
                    }
                    String regexForThisLink = "(\">http://uploading\\.com/files/" + fileid + "/.*?/</a></td>ntttt<td>(Aktiv|active|Gelöscht|Deleted)</td>ntttt<td>.*?</td>)";
                    String theData = new Regex(correctedHTML, regexForThisLink).getMatch(0);
                    if (theData == null) {
                        if (br.containsHTML("\"js\": \\{ \"checker_result\": \"\" \\}, \"text\": \"\" \\}")) {
                            dl.setAvailable(false);
                            continue;
                        } else {
                            logger.warning("Uploading.com availablecheck is broken!");
                            return false;
                        }
                    }
                    Regex allMatches = new Regex(theData, "\">http://uploading\\.com/files/" + fileid + "/(.*?)/</a></td>ntttt<td>(Aktiv|active|Gelöscht|Deleted)</td>ntttt<td>(.*?)</td>");
                    String status = allMatches.getMatch(1);
                    String filename = allMatches.getMatch(0);
                    String filesize = allMatches.getMatch(2);
                    if (filename == null || filesize == null) {
                        logger.warning("Uploading.com availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.matches("(Aktiv|active)")) {
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    filename = Encoding.htmlDecode(filename);
                    filename = Encoding.urlDecode(filename, false);
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

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        handleFree0(downloadLink);
    }

    public String getDownloadUrl(DownloadLink downloadLink) throws Exception {
        String timead = br.getRegex("timead_counter\">(\\d+)<").getMatch(0);
        if (timead != null) {
            Form form = br.getForm(0);
            sleep(Integer.parseInt(timead) * 1000l, downloadLink);
            br.submitForm(form);
        }
        String varLink = br.getRegex("var file_link = '(http://.*?)'").getMatch(0);
        if (varLink != null) {
            sleep(2000, downloadLink);
            return varLink;
        }
        br.setFollowRedirects(false);
        String fileID = br.getRegex("file_id: (\\d+)").getMatch(0);
        if (fileID == null) fileID = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
        String code = br.getRegex("code: \"(.*?)\"").getMatch(0);
        if (code == null) code = br.getRegex("name=\"code\" value=\"(.*?)\"").getMatch(0);
        if (fileID == null || code == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String starttimer = br.getRegex("start_timer\\((\\d+)\\);").getMatch(0);
        String redirect = null;
        if (starttimer != null) {
            sleep((Long.parseLong(starttimer) + 2) * 1000l, downloadLink);
        }
        br.postPage("http://uploading.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "file_id=" + fileID + "&code=" + code + "&action=get_link&pass=");
        redirect = br.getRegex("link\": \"(http.*?)\"").getMatch(0);
        if (redirect != null) {
            redirect = redirect.replaceAll("\\\\/", "/");
        } else {
            if (br.containsHTML("Please wait")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
            if (br.containsHTML("Your download was not found or")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Your download was not found or has expired. Please try again later", 15 * 60 * 1000l);
            if (br.containsHTML("Your download has expired")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Your download was not found or has expired. Please try again later", 15 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return redirect;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 100;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        synchronized (PREMLOCK) {
            return simultanpremium;
        }
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * TODO: remove with next major update, DownloadWatchDog/AccountController
     * handle blocked accounts now
     */
    @Override
    public boolean isPremiumDownload() {
        /* free user accounts are no premium accounts */
        boolean ret = super.isPremiumDownload();
        if (ret && free) ret = false;
        return ret;
    }

}
