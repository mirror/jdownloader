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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.http.Request;
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
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DepositFiles extends PluginForHost {

    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    private static final String PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";

    private static final String PATTERN_PREMIUM_REDIRECT = "window\\.location\\.href = \"(.*?)\";";

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr.*?: <b>(.*?)</b>");

    // Rechtschreibfehler übernommen
    private String PASSWORD_PROTECTED = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    public DepositFiles(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    @Override
    public void handleFree(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();

        DownloadLink downloadLink = parameter;

        this.setBrowserExclusive();

        String link = downloadLink.getDownloadURL().replaceAll("/\\w{2}/files/", "/de/files/");

        br.setCookie("http://depositfiles.com", "lang_current", "de");

        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");

            br.getPage(link);

        }
        br.forceDebug(true);
        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) {
            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) {
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        Form form = br.getFormbyValue("Kostenlosen download");

        if (form != null) {
            br.submitForm(form);

            String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(wait));

            }
        } else {
            String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(wait));

            }
        }
        if (br.containsHTML(PASSWORD_PROTECTED)) {
            // MUss wohl noch angepasst werden
            String password = Plugin.getUserInput(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), downloadLink);
            br.postPage(link, "go=1&gateway_result=1&file_password=" + password);
        }

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
            logger.severe("Unknown error. Retry in 20 seconds");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        form = br.getFormbyValue("Die Datei downloaden");
        Request r = br.createFormRequest(form);
        dl = RAFDownload.download(downloadLink, r, false, 1);
        HTTPConnection con = dl.connect(br);

        if (con == null) {
            if (br.containsHTML("IP-Addresse werden schoneinige Files")) {

                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                linkStatus.setValue(30000l);
                return;

            }
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }

        if (con.getHeaderField("Location") != null && con.getHeaderField("Location").indexOf("error") > 0) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        dl.startDownload();

    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setAcceptLanguage("en, en-gb;q=0.8");

        br.getPage("http://depositfiles.com/en/");

        Form login = br.getFormbyValue("enter");

        login.put("login", account.getUser());
        login.put("password", account.getPass());

        br.submitForm(login);
        if (br.containsHTML("Your password or login is incorrect") || br.containsHTML("Benutzername-Passwort-Kombination")) {

            ai.setValid(false);
            return ai;
        }
        br.getPage("http://depositfiles.com/en/gold/");
        String expire = br.getRegex("You have Gold access until: <b>(.*?)</b>").getMatch(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
        if (expire == null) {
            ai.setStatus("Account expired or logins not valid");
            ai.setValid(false);
            return ai;
        }
        ai.setStatus("Account is ok");
        // logger.info(dateFormat.format(new Date())+"");
        Date date;
        try {
            date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setCookie("http://depositfiles.com", "lang_current", "de");
        String link = downloadLink.getDownloadURL().replaceAll("/\\w{2}/files/", "/de/files/");

        downloadLink.setUrlDownload(link);

        br.getPage(link);

        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");

            br.getPage(link);

        }
        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) {
            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) {
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        Form login = br.getFormbyValue("einloggen");

        login.put("login", account.getUser());
        login.put("password", account.getPass());

        br.submitForm(login);

        if (br.containsHTML("Your password or login is incorrect") || br.containsHTML("Benutzername-Passwort-Kombination")) {
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            downloadLink.getLinkStatus().setErrorMessage("Your password or login is incorrect");
            return;

        }

        link = br.getRegex(PATTERN_PREMIUM_REDIRECT).getMatch(0);
        br.getPage(link);
        if (br.containsHTML(PASSWORD_PROTECTED)) {
            String password = Plugin.getUserInput(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), downloadLink);
            br.postPage(link, "go=1&gateway_result=1&file_password=" + password);
        } else {
            // logger.info(br + "");
        }
        link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);

        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM,LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        
        }

        dl = RAFDownload.download(downloadLink, br.createGetRequest(link), true, 0);
        HTTPConnection con = dl.connect(br);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("\\.com[/..]?/files", ".com/de/files"));
        String link = downloadLink.getDownloadURL();

        br.setFollowRedirects(true);
        br.setCookie("http://depositfiles.com", "lang_current", "de");
        br.setFollowRedirects(false);
        br.getPage(link);

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) { return false; }

        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        downloadLink.setName(fileName);
        String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        long length = Regex.getSize(fileSizeString);
        downloadLink.setDownloadSize(length);

        return true;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
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
