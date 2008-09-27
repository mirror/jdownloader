//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.captcha.LetterComperator;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static final String CAPTCHA_WRONG = "verification code you entered was incorrect";
    private static final String DOWNLOAD_LIMIT = "exceeded the download limit";

    private static final String FILENAME = "<h1 style=\"width:370px;\">(.*)</h1>";
    private static final String FILESIZE = "Size: (.*?)(B|KB|MB)<br />";
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "downloading too many files";

    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/securimage/securimage_show.php\\?[^\"]*)\" alt=");
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";
    private static final String WAIT_TIME = "wait ([0-9]+ [minutes|seconds])";

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        if (parameter.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(parameter);
            return;
        }

        br.setFollowRedirects(true);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("there are currently no free download slots")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 3 * 60 * 1000l); }
        if (br.containsHTML(NOT_AVAILABLE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN) || br.containsHTML(NO_SLOT)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        br.getPage(Encoding.htmlDecode("http://www.filefactory.com" + br.getRegex(baseLink).getMatch(0)));

        br.setCookie(br.getURL(), "viewad11", "yes");
        br.getPage(Encoding.htmlDecode("http://www.filefactory.com/" + br.getRegex(frameForCaptcha).getMatch(0)));
        String captchaCode = null;
        int vp = JDUtilities.getSubConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 18);
        JDUtilities.getSubConfig("JAC").setProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 100);
        int i = 30;
        while (i-- > 0) {
            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, Encoding.htmlDecode("http://www.filefactory.com" + br.getRegex(patternForCaptcha).getMatch(0)));
     
            captchaCode = Plugin.getCaptchaCode(captchaFile, this, parameter);
         
            if (this.getLastCaptcha() == null) continue;
            double worst = 0.0;
            for (LetterComperator l : this.getLastCaptcha().getLetterComperators()) {
                if (l.getValityPercent() > worst) worst = l.getValityPercent();

            }
            logger.info("CAPTCHA: " + captchaCode + "(" + worst + "/" + this.getLastCaptcha().getValityPercent() + ")");
            if (captchaCode != null && worst < 30) {
                captchaCode = captchaCode.trim().replace("-", "");
                if (captchaCode.length() == 4) break;
            }
        }
        JDUtilities.getSubConfig("JAC").setProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, vp);
        Form captchaForm = br.getForm(0);
        captchaForm.put("captcha", captchaCode);
        br.submitForm(captchaForm);

        if (br.containsHTML(CAPTCHA_WRONG)) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }

        // Match die verbindung auf, Alle header werden ausgetauscht, aber keine
        // Daten geladen
       dl=RAFDownload.download(parameter, br.createPostRequest(Encoding.htmlDecode(br.getRegex(patternForDownloadlink).getMatch(0)), ""));
       dl.connect(br);
       // Prüft ob content disposition header da sind
        if (br.getHttpConnection().isContentDisposition()) {
          
            dl.startDownload();
        } else {
            // Falls nicht wird die html seite geladen
            br.followConnection();
            if (br.containsHTML(DOWNLOAD_LIMIT)) {
                logger.info("Traffic Limit for Free User reached");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(br.getRegex(WAIT_TIME).getMatch(0)));
            } else if (br.containsHTML(PATTERN_DOWNLOADING_TOO_MANY_FILES)) {
                logger.info("You are downloading too many files at the same time. Wait 10 seconds(or reconnect) and retry afterwards");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
            }
        }

    }
    public int getMaxRetries() {
        return 20;
    }
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);

        if (br.containsHTML("record of an account with that email")) {
            ai.setValid(false);
            ai.setStatus("No account with this email");
            return ai;
        }
        if (br.containsHTML("password you entered is incorrect")) {
            ai.setValid(false);
            ai.setStatus("Account found, but password is wrong");
            return ai;
        }
        br.getPage("http://filefactory.com/rewards/summary/");
        String expire = br.getMatch("subscription will expire on <strong>(.*?)</strong>");
        if (expire == null) {
            ai.setValid(false);
            return ai;
        }
        // 17 October, 2008 (in 66 days).
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));
        String pThisMonth = br.getMatch("\\(Usable next month\\)</td>.*?<td.*?>(.*?)</td>").replaceAll("\\,", "");
        String pUsable = br.getMatch("Usable Accumulated Points</h2></td>.*?<td.*?><h2>(.*?)</h2></td>").replaceAll("\\,", "");

        ai.setPremiumPoints(Integer.parseInt(pThisMonth) + Integer.parseInt(pUsable));

        br.getPage("http://www.filefactory.com/members/details/premium/usage/");

        String[] dat = br.getRegex("You have downloaded (.*?) in the last 24 hours.*?Your daily limit is (.*?), and your download usage will be reset ").getRow(0);
        long gone;
        if (dat == null && Regex.matches(br, "You have not downloaded anything")) {

            gone = 0;
        } else {

            gone = Regex.getSize(dat[0].replace(",", ""));
        }
        ai.setTrafficMax(12 * 1024 * 1024 * 1024l);
        ai.setTrafficLeft(12 * 1024 * 1024 * 1024l - gone);
        return ai;
    }

    // by eXecuTe
    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);

            return;
        }
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        if (user == null || pass == null) {
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            return;
        }
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getPage("http://filefactory.com");

        Form login = br.getFormbyValue("Log in");
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(true);
        String error = br.getRegex("<div class=\"box error\">.*?<p>(.*?)<").getMatch(0);
        if (error != null) {

        throw new PluginException(LinkStatus.ERROR_PREMIUM, error, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        dl=RAFDownload.download(downloadLink, br.createGetRequest(downloadLink.getDownloadURL()),true,0);
        HTTPConnection con = dl.connect(br);
        
        if (con.getHeaderField("Content-Disposition") == null) {
            br.followConnection();

            if (br.containsHTML(NOT_AVAILABLE)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            } else if (br.containsHTML(SERVER_DOWN)) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setValue(20 * 60 * 1000l);
                return;
            } else {
                String red = br.getRegex("Description: .*?p style=.*?><a href=\"(.*?)\".*?>.*?Click here to begin your download").getMatch(0);
                con = br.openGetConnection(red);

            }
        }
        dl.startDownload();
        return;
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return true;
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML("there are currently no free download slots")) {
            br.setFollowRedirects(false);
            return false;
        } else if (br.containsHTML(SERVER_DOWN)) {
            br.setFollowRedirects(false);
            return false;
        } else {
            if (br.containsHTML("there are currently no free download slots")) {
                downloadLink.getLinkStatus().setErrorMessage("No slots available atm");
                downloadLink.getLinkStatus().setStatusText("No slots available atm");
            } else {
                String fileName = Encoding.htmlDecode(new Regex(br.toString().replaceAll("\\&\\#8203\\;", ""), FILENAME).getMatch(0));

                String fileSize = new Regex(br.toString(), FILESIZE).getMatch(-1);

                downloadLink.setName(fileName);
                downloadLink.setDownloadSize(Regex.getSize(fileSize));

            }

        }
        br.setFollowRedirects(false);
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return super.getFileInformationString(downloadLink);
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void init() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
