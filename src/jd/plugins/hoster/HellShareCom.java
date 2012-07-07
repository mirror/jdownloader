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

import jd.PluginWrapper;
import jd.gui.UserIO;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hellshare.com" }, urls = { "http://[\\w\\.]*?(download\\.)?(sk|cz|en)?hellshare\\.(com|sk|hu|de|cz|pl)/((.+/[0-9]+)|(/[0-9]+/.+/.+))" }, flags = { 2 })
public class HellShareCom extends PluginForHost {

    private static final String LIMITREACHED       = "(You have exceeded today´s free download limit|You exceeded your today\\'s limit for free download|<strong>Dnešní limit free downloadů jsi vyčerpal\\.</strong>)";

    // edt: added 2 constants for testing, in normal work - waittime when server
    // is 100% load should be 2-5 minutes
    private static final long   WAITTIME100PERCENT = 60 * 1000l;
    private static final long   WAITTIMEDAILYLIMIT = 4 * 3600 * 1000l;

    public HellShareCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.en.hellshare.com/register");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final String numbers = new Regex(link.getDownloadURL(), "hellshare\\.com/(\\d+)").getMatch(0);
        if (numbers == null) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("http.*?//.*?/", "http://download.en.hellshare.com/"));
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();

        try {
            login(account);
        } catch (final PluginException e) {
            ai.setStatus("Login failed");
            UserIO.getInstance().requestMessageDialog(0, "Hellshare.com Premium Error", "Login failed!\r\nPlease check your Username and Password!");
            account.setValid(false);
            return ai;
        }
        final String hostedFiles = br.getRegex(">Number of your files:</label></th>.*?<td id=\"info_files_counter\"><strong>(\\d+)</strong></td>").getMatch(0);
        if (hostedFiles != null) {
            ai.setFilesNum(Long.parseLong(hostedFiles));
        }

        // edt
        // searching for known account plans
        // Free Registered

        String premiumActive = br.getRegex("<div class=\"icon-timecredit icon\">[\n\t\r]+<h4>Premium account</h4>[\n\t\r]+(.*)[\n\t\r]+<br />[\n\t\r]+<a href=\"/credit/time\">Buy</a>").getMatch(0);

        if (premiumActive == null) {
            // Premium User
            premiumActive = br.getRegex("<div class=\"icon-timecredit icon\">[\n\t\r]+<h4>Premium account</h4>[\n\t\r]+(.*)<br />[\n\t\r]+<a href=\"/credit/time\">Buy</a>").getMatch(0);
        }

        if (premiumActive == null) {
            // User with Credits
            premiumActive = br.getRegex("<div class=\"icon-credit icon\">[\n\t\r]+<h4>(.*)</h4>[\n\t\r]+<table>+[\n\t\r]+<tr>[\n\t\r]+<th>Current:</th>[\n\t\r]+<td>(.*?)</td>[\n\t\r]+</tr>").getMatch(0);
        }

        if (premiumActive == null) {
            ai.setStatus("Invalid/Unknown");
            account.setValid(false);
        } else if (premiumActive.contains("Inactive")) {
            ai.setStatus("Free User");
            // for inactive - set traffic left to 0
            ai.setTrafficLeft(0l);
            account.setValid(true);
        } else if (premiumActive.contains("Active")) {

            String validUntil = premiumActive.substring(premiumActive.indexOf(":") + 1);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd.MM.yyyy", null));
            ai.setStatus("Premium User");
            account.setValid(true);

        } else if (premiumActive.contains("Download credit")) {
            final String trafficleft = br.getRegex("id=\"info_credit\" class=\"va-middle\">[\n\t\r ]+<strong>(.*?)</strong>").getMatch(0);
            if (trafficleft != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            }
            ai.setStatus("User with Credits");
            account.setValid(true);

        }

        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.en.hellshare.com/terms";
    }

    private String getDownloadOverview(final String fileID) {
        String freePage = br.getRegex("\"(/[^/\"\\'<>]+/[^/\"\\'<>]+/" + fileID + "/\\?do=relatedFileDownloadButton\\-" + fileID + ".*?)\"").getMatch(0);
        if (freePage == null) freePage = br.getRegex("\"(/([^/\"\\'<>]+/)?[^/\"\\'<>]+/" + fileID + "/\\?do=relatedFileDownloadButton\\-" + fileID + ".*?)\"").getMatch(0);
        if (freePage == null) {
            freePage = br.getRegex("\"(/[^/\"\\'<>]+/" + fileID + "/\\?do=fileDownloadButton\\-showDownloadWindow)\"").getMatch(0);
            if (freePage == null) {
                freePage = br.getRegex("\\s+<a href=\"([^\\s]+)\" class=\"ajax button button-devil\"").getMatch(0);
            }
        }
        if (freePage != null) {
            if (!freePage.startsWith("http")) {
                freePage = "http://download.hellshare.com" + freePage;
            }
        }
        return freePage;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // edt: added more logging info
        if (br.containsHTML(LIMITREACHED)) {
            // edt: to support bug when server load = 100% and daily limit
            // reached are simultaneously displayed
            if (br.containsHTML("Current load 100%") || br.containsHTML("Server load: 100%")) {
                // throw new
                // PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
                // JDL.L("plugins.hoster.HellShareCom.error.CurrentLoadIs100Percent",
                // "The current serverload is 100%"), 10 * WAITTIME100PERCENT);
                // } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT);
            }
        }
        br.setFollowRedirects(false);
        final String changetocz = br.getRegex("lang=\"cz\" xml:lang=\"cz\" href=\"(http://download\\.cz\\.hellshare\\.com/.*?/\\d+)\"").getMatch(0);
        if (changetocz == null) {
            // Do NOT throw an exeption here as this part isn't that important
            // but it's bad that the plugin breaks just because of this regex
            logger.warning("Language couldn't be changed. This will probably cause trouble...");
        } else {
            br.getPage(changetocz);
            if (br.containsHTML("No htmlCode read")) {
                br.getPage(downloadLink.getDownloadURL());
            }
        }
        br.setDebug(true);
        // edt: new string for server 100% load
        if (br.containsHTML("Current load 100%") || br.containsHTML("Server load: 100%")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.CurrentLoadIs100Percent", "The current serverload is 100%"), WAITTIME100PERCENT); }
        // edt: added more logging info
        if (br.containsHTML(LIMITREACHED)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT); }
        final String fileId = new Regex(downloadLink.getDownloadURL(), "/(\\d+)(/)?$").getMatch(0);
        if (fileId == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        boolean secondWay = true;
        String freePage = getDownloadOverview(fileId);
        if (freePage == null) {
            // edt: seems that URL for download is already final for all the
            // links, so br.getURL doesn't need to be changed, secondWay always
            // works good
            freePage = br.getURL(); // .replace("hellshare.com/serialy/",
                                    // "hellshare.com/").replace("/pop/",
                                    // "/").replace("filmy/", "");
            secondWay = false;
        }

        // edt: if we got response then secondWay works for all the links
        // br.getPage(freePage);
        if (!br.containsHTML("No htmlCode read")) {// (br != null) {
            secondWay = true;
        }
        if (br.containsHTML("The server is under the maximum load")) {
            logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), 10 * 60 * 1000l);
        }
        if (br.containsHTML("You are exceeding the limitations on this download")) {
            logger.info("You are exceeding the limitations on this download");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        }
        // edt: added more logging info
        if (br.containsHTML(LIMITREACHED)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.HellShareCom.error.DailyLimitReached", "Daily Limit for free downloads reached"), WAITTIMEDAILYLIMIT); }
        if (br.containsHTML("<h1>File not found</h1>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        if (secondWay) {
            Form captchaForm = null;
            final Form[] allForms = br.getForms();
            if (allForms != null && allForms.length != 0) {
                for (final Form aForm : allForms) {
                    if (aForm.containsHTML("captcha-img")) {
                        captchaForm = aForm;
                        break;
                    }
                }
            }
            if (captchaForm == null) {
                logger.warning("captchaform equals null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            String captchaLink = captchaForm.getRegex("src=\"(.*?)\"").getMatch(0);
            if (captchaLink == null) {
                captchaLink = br.getRegex("\"(http://(www\\.)?hellshare\\.com/captcha\\?sv=.*?)\"").getMatch(0);
            }
            if (captchaLink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            final String code = getCaptchaCode(Encoding.htmlDecode(captchaLink), downloadLink);
            captchaForm.put("captcha", code);
            br.setFollowRedirects(true);
            br.setReadTimeout(120 * 1000);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, false, 1);
        } else {
            Form form = br.getForm(1);
            if (form == null) {
                form = br.getForm(0);
            }
            final String captcha = "http://www.en.hellshare.com/antispam.php?sv=FreeDown:" + fileId;
            final String code = getCaptchaCode(captcha, downloadLink);
            form.put("captcha", Encoding.urlEncode(code));
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, false, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getURL().contains("errno=404")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.HellShareCom.error.404", "404 Server error. File might not be available for your country!")); }
            if (br.containsHTML("<h1>File not found</h1>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            // edt: new string for server 100% load
            if (br.containsHTML("The server is under the maximum load") || br.containsHTML("Server load: 100%")) {
                logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), WAITTIME100PERCENT);
            }
            if (br.containsHTML("(Incorrectly copied code from the image|Opište barevný kód z obrázku)") || br.getURL().contains("error=405")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (br.containsHTML("You are exceeding the limitations on this download")) {
                logger.info("You are exceeding the limitations on this download");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);

        // edt
        // checking if the account is "Free User", if so then download as Free
        AccountInfo ai = account.getAccountInfo();
        if (ai.getStatus().equals("Free User") & ai.getTrafficLeft() == 0l) {
            handleFree(downloadLink);
            return;
        }

        br.getPage(downloadLink.getDownloadURL());
        final String fileId = new Regex(downloadLink.getDownloadURL(), "/(\\d+)(/)?$").getMatch(0);
        if (fileId == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String downloadOverview = getDownloadOverview(fileId);
        if (downloadOverview != null) {
            br.getPage(downloadOverview);
        }
        String dllink = br.getRegex("launchFullDownload\\(\\'.*?(http:.*?)(\\&|\"|\\')").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://data\\d+\\.helldata\\.com/[a-z0-9]+/\\d+/.*?)(\\&|\"|\\')").getMatch(0);
        }
        if (dllink == null) {
            if (br.containsHTML("button\\-download\\-full\\-nocredit")) {
                logger.info("not enough credits to download");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            logger.warning("dllink (premium) is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replaceAll("\\\\", "");

        // edt
        // Premium download handling

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

        // edt
        // not used anymore, because there is no plan with traffic limit
        /*
         * set max chunks to 1 because each range request counts as download,
         * reduces traffic very fast ;)
         */
        /*
         * int retry = 2; while (retry > 0) { dl =
         * jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink,
         * true, 1); if (!dl.getConnection().isContentDisposition()) {
         * br.followConnection(); if
         * (br.containsHTML("<h1>File not found</h1>")) { throw new
         * PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); } if
         * (br.containsHTML("The server is under the maximum load")) { throw new
         * PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE,
         * "Server is under maximum load", 10 * 60 * 1000l); } if
         * (br.containsHTML("Incorrectly copied code from the image")) { throw
         * new PluginException(LinkStatus.ERROR_CAPTCHA); } if
         * (br.containsHTML("You are exceeding the limitations on this download"
         * )) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 *
         * 1000l); } if (br.containsHTML(
         * "This file you downloaded already and re-download is for free")) {
         * dllink = br.getRegex(
         * "\"(http://data\\d+\\.helldata\\.com/[a-z0-9]+/\\d+/.*?)(\\&|\"|\\')"
         * ).getMatch(0); retry--; continue; } throw new
         * PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
         * dl.startDownload(); return; } throw new
         * PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
         */
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        /* to prefer english page */
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(false);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.postPage("http://www.hellshare.com/login?do=loginForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login=odeslat&DownloadRedirect=");

        /*
         * this will change account language to eng,needed because language is
         * saved in profile
         */
        final String changetoeng = br.getRegex("\"(http://www\\.en\\.hellshare\\.com/--.*?profile.*?)\"").getMatch(0);
        if (changetoeng == null) {
            // Do NOT throw an exeption here as this part isn't that important
            // but it's bad that the plugin breaks just because of this regex
            logger.warning("Language couldn't be changed. This will probably cause trouble...");
        } else {
            br.getPage(changetoeng);
        }
        if (br.containsHTML("Wrong user name or wrong password.") || !br.containsHTML("credit for downloads") || br.containsHTML("Špatně zadaný login nebo heslo uživatele")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        /* to prefer english page UPDATE: English does not work anymore */
        br.setCustomCharset("utf-8");
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(true);
        try {
            try {
                /* not available in 0.9.* && nightly */
                br.setAllowedResponseCodes(new int[] { 502 });
            } catch (final Throwable e) {
            }
            br.getPage(link.getDownloadURL());
            if (this.br.containsHTML(">We are sorry, but HellShare is unavailable in \\w+\\.?<")) {
                logger.warning(">We are sorry, but HellShare is unavailable in your Country");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.HellShareCom.error.CountryBlock", "We are sorry, but HellShare is unavailable in your Country"));
            }
        } catch (final Exception e) {
            if (e instanceof PluginException) throw (PluginException) e;
            // for stable: we assume that 502 == country block.
            if (e.getMessage().contains("502 Bad Gateway")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.HellShareCom.error.CountryBlock", "We are sorry, but HellShare is unavailable in your Country")); }
        }
        if (br.containsHTML("<h1>File not found</h1>") || br.containsHTML("<h1>Soubor nenalezen</h1>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filesize = br.getRegex("FileSize_master\">(.*?)</strong>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\"The content.*?with a size of (.*?) has been uploaded").getMatch(0);
        }
        String filename = br.getRegex("\"FileName_master\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"The content (.*?) with a size").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) – Download").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("hidden\">Downloading file (.*?)</h1>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("keywords\" content=\"HellShare, (.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("&nbsp;", "")));
        link.setUrlDownload(br.getURL());
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}