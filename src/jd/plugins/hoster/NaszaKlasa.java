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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nk.pl" }, urls = { "http://(www\\.)?nk\\.decryptednaszaplasa/profile/\\d+/gallery/album/\\d+/\\d+\\?naszaplasalink" }, flags = { 2 })
public class NaszaKlasa extends PluginForHost {

    private static Object       LOCK     = new Object();
    private static final String MAINPAGE = "nk.pl";
    private static final String USERTEXT = "Only downloadable for registered users!";

    public NaszaKlasa(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://nk.pl/main");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decryptednaszaplasa", "pl").replace("?naszaplasalink", ""));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://nk.pl/nk_policy";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.naszaklasa.only4registered", USERTEXT));
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        String finallink = br.getRegex("<img id=\"photo_img\" alt=\"zdjęcie\" src=\"(http://.*?)\"").getMatch(0);
        if (finallink == null || finallink.contains("other/std")) {
            finallink = br.getRegex("\"(http://photos\\.nasza-klasa\\.pl/\\d+/\\d+/main/.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            logger.warning("finallink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) {
                acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            }
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("remember_me") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            Form dlForm = new Form();
            String loginPage = "https://nk.pl/login?captcha=0";
            boolean captcha = false;
            int tryouts = 0;

            while (true) {
                br.getPage(loginPage);
                dlForm = br.getFormbyKey("form_name", "login_form");

                dlForm.put("target", "");
                dlForm.put("login", Encoding.urlEncode(account.getUser()));
                dlForm.put("password", Encoding.urlEncode(account.getPass()));
                dlForm.put("remember", "1");
                if (captcha) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", "nk.pl", "http://nk.pl", true);

                    String captchaCode = getCaptchaCode("http://nk.pl/captcha?" + Math.floor(Math.random() * 10000), dummyLink);
                    // dlForm.setProperty("__captcha", captchaCode);
                    dlForm.put("__captcha", captchaCode);

                }
                br.submitForm(dlForm);

                // br.postPage("https://nk.pl/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" +
                // Encoding.urlEncode(account.getPass()) + "&remember=1&form_name=login_form&target=&manual=1");
                if ((!captcha && br.containsHTML("https://nk\\.pl/login\\?captcha=1")) || (captcha && br.containsHTML("<strong>Kod z obrazka: nieprawidłowy kod</strong>"))) {
                    loginPage = "https://nk.pl/login?captcha=1";
                    captcha = true;
                    tryouts++;
                    if (tryouts > 3) break;
                } else {
                    break;
                }
            }

            if (br.getCookie(MAINPAGE, "remember_me") == null || br.getCookie(MAINPAGE, "lltkck") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}