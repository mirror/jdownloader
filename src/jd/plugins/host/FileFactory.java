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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.reconnect.Reconnecter;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FileFactory extends PluginForHost {

    private static Pattern baseLink = Pattern.compile("<a class=\"download\" href=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    private static final String CAPTCHA_WRONG = "Download code was incorrect";
    private static final String DOWNLOAD_LIMIT = "(Thank you for waiting|exceeded the download limit)";

    private static final String FILENAME = "<h1>(.*)</h1>";
    private static final String FILESIZE = "<span>(.*? (B|KB|MB)) file";

    private static final String NO_SLOT = "no free download slots";
    private static final String NOT_AVAILABLE = "class=\"box error\"";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "currently downloading too many files at once";
    private static final String WAIT_TIME = "have exceeded the download limit for free users.  Please wait ([0-9]+) minutes to download more files";
    
    private static final String LOGIN_ERROR = "The email or password you have entered is incorrect";

    private static Pattern patternForCaptcha = Pattern.compile("<img class=\"captchaImage\" src=\"(.*?)\"");
    private static Pattern patternForDownloadlink = Pattern.compile("<p><a href=\"(.*?)\" class=\"download\">");

    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";

    public FileFactory(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filefactory.com/info/premium.php");
    }

    public int getTimegapBetweenConnections() {
        return 200;
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        try {
            handleFree0(parameter);
        } catch (IOException e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            if (e.getMessage() != null && e.getMessage().contains("502")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw e;
            }
        }
    }

    public void handleFree0(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("there are currently no free download slots")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 3 * 60 * 1000l); }
        if (br.containsHTML(NOT_AVAILABLE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN) || br.containsHTML(NO_SLOT)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        br.getPage(Encoding.htmlDecode("http://www.filefactory.com" + br.getRegex(baseLink).getMatch(0)));

        br.setCookie(br.getURL(), "viewad11", "yes");
        String captchaCode = null;
        int vp = SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 18);
        int i = 30;
        File captchaFile = null;

        while (i-- > 0) {

            int ii = 5;
            while (ii-- >= 0)
                try {
                    captchaFile = this.getLocalCaptchaFile(this);
                    Browser.download(captchaFile, Encoding.htmlDecode("http://www.filefactory.com" + br.getRegex(patternForCaptcha).getMatch(0)));
                    break;
                } catch (IOException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                    try {
                        Thread.sleep(200);
                    } catch (Exception e2) {
                        return;
                    }
                }
            try {
                parameter.getLinkStatus().setStatusText(JDLocale.L("plugin.filefactory.jac.running", "JAC running"));
                parameter.requestGuiUpdate();
                captchaCode = Plugin.getCaptchaCode(captchaFile, this, parameter);
            } catch (Exception e) {
                continue;
            }
            captchaCode = captchaCode.replaceAll("\\-", "");
            if (captchaCode.length() < 4) continue;
            parameter.getLinkStatus().setStatusText(JDLocale.LF("plugin.filefactory.jac.returned", "JAntiCaptcha: %s", captchaCode));
            parameter.requestGuiUpdate();

            if (captchaCode != null && captchaCode.length() == 4) break;

        }
        Thread.sleep(2000);
        parameter.getLinkStatus().setStatusText(JDLocale.LF("plugin.filefactory.jac.send", "JAC send: %s", captchaCode));
        parameter.requestGuiUpdate();
        SubConfiguration.getConfig("JAC").setProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, vp);
        Form captchaForm = br.getForm(1);
        captchaForm.put("captchaText", captchaCode);
        br.submitForm(captchaForm);

        if (br.containsHTML(CAPTCHA_WRONG)) {
            captchaFile.renameTo(new File(captchaFile.getParentFile(), captchaFile.getName() + "_BAD.png"));
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        captchaFile.renameTo(new File(captchaFile.getParentFile(), captchaFile.getName() + "_OK.png"));

        // Match die verbindung auf, Alle header werden ausgetauscht, aber keine
        // Daten geladen
        br.openDownload(parameter, Encoding.htmlDecode(br.getRegex(patternForDownloadlink).getMatch(0)));
        // dl = RAFDownload.download(parameter,
        // br.createPostRequest(Encoding.htmlDecode
        // (br.getRegex(patternForDownloadlink).getMatch(0)), ""));
        // dl.connect(br);
        // Prüft ob content disposition header da sind
        if (br.getHttpConnection().isContentDisposition()) {
            long cu = parameter.getDownloadCurrent();
            dl.startDownload();
            long loaded = parameter.getDownloadCurrent() - cu;
            if (loaded > 30 * 1024 * 1024l) {
                Reconnecter.requestReconnect();
            }
        } else {
            // Falls nicht wird die html seite geladen
            br.followConnection();
            if (br.containsHTML(DOWNLOAD_LIMIT)) {
                logger.info("Traffic Limit for Free User reached");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex(WAIT_TIME).getMatch(0)) * 60 * 1000l);
            } else if (br.containsHTML(PATTERN_DOWNLOADING_TOO_MANY_FILES)) {
                logger.info("You are downloading too many files at the same time. Wait 10 seconds(or reconnect) and retry afterwards");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
            }
        }

    }

    public int getMaxRetries() {
        return 20;
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
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

        if (br.containsHTML(LOGIN_ERROR)) {
            ai.setValid(false);
            ai.setStatus(LOGIN_ERROR);
            return ai;
        }
        br.getPage("http://www.filefactory.com/member/");
        String expire = br.getMatch("Your account is valid until the <strong>(.*?)</strong>");
        if (expire == null) {
            ai.setValid(false);
            return ai;
        }
        expire = expire.replace("th", "");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM, yyyy", Locale.UK));
        
        br.getPage("http://www.filefactory.com/reward/summary.php");
        String points = br.getMatch("Available reward points.*?class=\"amount\">(.*?) points").replaceAll("\\,", "");
        ai.setPremiumPoints(Long.parseLong(points.trim()));

        /*br.getPage("http://www.filefactory.com/members/details/premium/usage/");

        String[] dat = br.getRegex("You have downloaded (.*?) in the last 24 hours.*?Your daily limit is (.*?), and your download usage will be reset ").getRow(0);
        long gone;
        if (dat == null && Regex.matches(br, "You have not downloaded anything")) {

            gone = 0;
        } else {

            gone = Regex.getSize(dat[0].replace(",", ""));
        }
        ai.setTrafficMax(12 * 1024 * 1024 * 1024l);
        ai.setTrafficLeft(12 * 1024 * 1024 * 1024l - gone);*/
        
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
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

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(true);
        
        if (br.getRegex(LOGIN_ERROR).getMatch(0) != null)
        	throw new PluginException(LinkStatus.ERROR_PREMIUM, LOGIN_ERROR, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        
        br.setFollowRedirects(false);
        br.openGetConnection(downloadLink.getDownloadURL());
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 0);

        if (dl.getConnection().getHeaderField("Content-Disposition") == null) {
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
                logger.finer("Indirect download");
                dl = br.openDownload(downloadLink, red, true, 0);

            }
        } else {
            logger.finer("DIRECT download");
        }
        dl.startDownload();
        return;
    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws Exception, PluginException {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                return false;
            }
            try {
                br.getPage(downloadLink.getDownloadURL());
                break;
            } catch (Exception e) {
                if (i == 3) throw e;
            }
        }
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML("there are currently no free download slots")) {
            br.setFollowRedirects(false);
            return false;
        } else if (br.containsHTML(SERVER_DOWN)) {
            br.setFollowRedirects(false);
            return false;
        } else {
            if (br.containsHTML("there are currently no free download slots")) {
                downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("plugins.hoster.filefactorycom.errors.nofreeslots","No slots free available"));
                downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.hoster.filefactorycom.errors.nofreeslots","No slots free available"));
            } else {
                if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String fileName = Encoding.htmlDecode(new Regex(br.toString().replaceAll("\\&\\#8203\\;", ""), FILENAME).getMatch(0));
                String fileSize = new Regex(br.toString(), FILESIZE).getMatch(0);

                downloadLink.setName(fileName);
                downloadLink.setDownloadSize(Regex.getSize(fileSize));

            }

        }
        br.setFollowRedirects(false);
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatReadable(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
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
