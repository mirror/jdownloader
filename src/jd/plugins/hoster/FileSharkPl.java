//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Base64;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshark.pl" }, urls = { "http://(www\\.)?fileshark\\.pl/pobierz/(\\d+)/(.+)" }, flags = { 2 })
public class FileSharkPl extends PluginForHost {

    public FileSharkPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileshark.pl/premium/kup");
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileshark.pl/strona/regulamin";
    }

    private static final String POLAND_ONLY = ">Strona jest dostępna wyłącznie dla użytkowników znajdujących się na terenie Polski<";

    private long checkForErrors() throws PluginException {
        if (br.containsHTML("Osiągnięto maksymalną liczbę sciąganych jednocześnie plików.")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        if (br.containsHTML("Plik nie został odnaleziony w bazie danych.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        if (br.containsHTML("<li>Trwa pobieranie pliku. Możesz pobierać tylko jeden plik w tym samym czasie.</li>")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Other file is downloading!", 5 * 50 * 1000l);
        }
        if (br.containsHTML("Kolejne pobranie możliwe za") || br.containsHTML("Proszę czekać. Pobieranie będzie możliwe za")) {

            String waitTime = br.getRegex("Kolejne pobranie możliwe za <span id=\"timeToDownload\">(\\d+)</span>").getMatch(0);
            if (waitTime == null) {
                waitTime = br.getRegex("Pobieranie będzie możliwe za <span id=\"timeToDownload\">(\\d+)</span>").getMatch(0);
            }

            if (waitTime != null) {
                return Long.parseLong(waitTime) * 1000l;
            }

        }
        return 0l;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        // bug at the server side:
        // if the user finished downloads then the next download link doesn't
        // display info about the link but message with the time for the next download
        // So checking the online stat simply requires login to the site! (just like
        // checking for Premium user)
        // This is stupid because after user puts the link from LinkGrabber -> Downloads
        // then again it is checked (for Premium user) and again log procedure is
        // required to set cookies for downloads...
        // So the correct names and filesizes are set at the download time...
        // Informed them about this bug, hope they will correct it, next download
        // time should be displayed just when the user tries to start the download (button)
        // not at the time when the link is displayed.

        // for Premium only ! Read description above!
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("fileshark.pl");
        final Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa != null) {
            try {
                login(aa, false);
            } catch (Exception e) {

            }
        }

        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());

        if (br.containsHTML(POLAND_ONLY)) {
            link.getLinkStatus().setStatusText("This service is only available in Poland");
            return AvailableStatus.UNCHECKABLE;
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String fileName = br.getRegex("<h2[ \n\t\t\f]+class=\"name-file\">([^<>\"]*?)</h2>").getMatch(0);
        String fileSize = br.getRegex("<p class=\"size-file\">Rozmiar: <strong>(.*?)</strong></p>").getMatch(0);

        if (fileName == null || fileSize == null) {
            long waitTime = checkForErrors();
            if (waitTime != 0) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, waitTime);
            }

        }

        link.setName(Encoding.htmlDecode(fileName.trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        link.setFinalFileName(Encoding.htmlDecode(fileName.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML(POLAND_ONLY)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This service is only available in Poland");
        }

        doFree(downloadLink);
    }

    public static void saveCaptchaImage(final File file, final byte[] data) throws IOException {
        if (file.isFile()) {
            if (file.exists() && !file.delete()) {
                throw new IOException("Could not overwrite file: " + file);
            }
        }
        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        file.createNewFile();
        FileOutputStream fos = null;
        InputStream input = null;
        boolean okay = false;
        try {
            fos = new FileOutputStream(file, false);

            final int length = data.length;
            fos.write(data, 0, length);
            okay = length > 0;
        } finally {
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (okay == false) {
                file.delete();
            }
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String downloadURL = downloadLink.getDownloadURL();
        String fileId = new Regex(downloadURL, "http://(www\\.)?fileshark.pl/pobierz/" + "(\\d+/[0-9a-zA-Z]+/?)").getMatch(1);

        br.getPage(MAINPAGE + "pobierz/normal/" + fileId);
        long waitTime = checkForErrors();
        if (waitTime != 0) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, waitTime);
        }

        Form dlForm = new Form();
        br.setCookie(downloadURL, "file", fileId);
        // captcha handling
        // the image is encoded into the page, so first we need to store
        // it to hdd and the display as challenge to solve
        for (int i = 0; i < 5; i++) {
            dlForm = br.getForm(0);
            String token = dlForm.getInputFieldByName("form%5B_token%5D").getValue();

            File cf = getLocalCaptchaFile();
            String imageDataEncoded = new Regex(dlForm.getHtmlCode(), "<img src=\"data:image/jpeg;base64,(.*)\" title=\"").getMatch(0);
            byte[] imageData = Base64.decode(imageDataEncoded);
            saveCaptchaImage(cf, imageData);
            String c = getCaptchaCode(cf, downloadLink);

            br.postPage(MAINPAGE + "pobierz/normal/" + fileId, "&form%5Bcaptcha%5D=" + c + "&form%5Bstart%5D=&form%5B_token%5D=" + token);
            logger.info("Submitted DLForm");
            if (br.containsHTML("class=\"error\">Błędny kod")) {
                continue;
            }
            waitTime = checkForErrors();
            if (waitTime != 0) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, waitTime);
            }
            break;
        }
        if (br.containsHTML("class=\"error\">Błędny kod")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wrong captcha!", 5 * 60 * 1000l);
        }
        String dllink = br.getRedirectLocation();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        boolean hours = false;
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus("Login failed or not Premium");
            UserIO.getInstance().requestMessageDialog(0, "FileShark Premium Error", "Login failed or not Premium!\r\nPlease check your Username and Password!");
            account.setValid(false);
            return ai;
        }

        final String dailyLimitLeftUsed = br.getRegex("<p>Pobrano dzisiaj</p>[\r\t\n ]+<p><strong>(.*)</strong> z 20 GB</p>").getMatch(0);
        if (dailyLimitLeftUsed != null) {
            long trafficLeft = SizeFormatter.getSize("20 GB") - SizeFormatter.getSize(dailyLimitLeftUsed);
            ai.setTrafficMax(SizeFormatter.getSize("20 GB"));
            ai.setTrafficLeft(trafficLeft);
        } else {
            ai.setUnlimitedTraffic();
        }

        String expire = br.getRegex(">Rodzaj konta <strong>Premium <span>\\(do (\\d{4}\\-\\d{2}\\-\\d{2})\\)").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
        account.setValid(true);
        try {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }

        ai.setStatus("Premium User");
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://fileshark.pl", key, value);
                        }
                        return;
                    }
                }
                br.getPage("http://fileshark.pl/zaloguj");
                Form login = br.getForm(0);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("_username", Encoding.urlEncode(account.getUser()));
                login.put("_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                br.getPage("/");
                if (!br.containsHTML("Rodzaj konta <strong>Premium")) {
                    logger.warning("Couldn't determine premium status or account is Free not Premium!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium Account is invalid: it's free or not recognized!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://fileshark.pl/");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(downloadLink);
        String downloadURL = downloadLink.getDownloadURL();
        br.getPage(downloadURL);
        String fileId = new Regex(downloadURL, "http://(www\\.)?fileshark.pl/pobierz/" + "(\\d+/[0-9a-zA-Z]+/?)").getMatch(1);
        if (fileId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        br.getPage("http://fileshark.pl/pobierz/start/" + fileId);
        String dllink = br.getRedirectLocation();

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    private static final String MAINPAGE = "http://fileshark.pl/";
    private static Object       LOCK     = new Object();

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}