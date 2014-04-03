//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://(x-art\\.com/members/videos/.+|([a-z0-9]+\\.)?x-art\\.com/.+\\.(mov|mp4|wmv).+)" }, flags = { 2 })
public class XArtCom extends PluginForHost {

    // DEVNOTES
    // links are now session based, we will need to add some sort of handling to get new token if/when token expires.

    private static Object LOCK   = new Object();
    private boolean       useRUA = true;

    public XArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://x-art.com/join/");
    }

    @Override
    public String getAGBLink() {
        return "http://x-art.com/legal/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {

        String name = new Regex(parameter.getDownloadURL(), "([^/]+\\.(mov|mp4|wmv))").getMatch(0);
        if (name == null) name = new Regex(parameter.getDownloadURL(), "x\\-art.com/(.+)").getMatch(0);

        parameter.setName(name);

        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("x-art.com");
        Account account = null;
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }

        // final check to prevent non account holders, else NPE happen!
        if (account == null) {
            parameter.getLinkStatus().setStatusText("Account required!");
            logger.warning("You're required to have an Account with this provider in order to use this plugin!");
            return AvailableStatus.UNCHECKABLE;
        }

        this.setBrowserExclusive();
        login(account, br, false);
        br.setFollowRedirects(true);

        URLConnectionAdapter urlcon = null;
        try {
            urlcon = br.openGetConnection(parameter.getDownloadURL());
            if (urlcon.getContentType().contains("text/html")) {
                br.followConnection();
            }

            int res_code = urlcon.getResponseCode();
            long dlsize = urlcon.getCompleteContentLength();

            if (res_code == 200) {
                parameter.setDownloadSize(dlsize);
                return AvailableStatus.TRUE;
            } else if (res_code == 404) {
                return AvailableStatus.FALSE;
            } else if (res_code == 401) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM);
            } else {
                return AvailableStatus.UNCHECKABLE;
            }
        } finally {
            try {
                urlcon.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "X-Art members only!");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) return false;
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            // check if it's time for the next full login.
            if (account.getStringProperty("nextFullLogin") != null && (System.currentTimeMillis() <= Long.parseLong(account.getStringProperty("nextFullLogin"))))
                login(account, br, false);
            else
                login(account, br, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, Browser lbr, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean redirect = lbr.isFollowingRedirects();
            try {
                /** Load cookies */
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            lbr.setCookie(this.getHost(), key, value);
                        }
                        agent.string = account.getStringProperty("userAgent");
                        prepBrowser(lbr);
                        return;
                    }
                }
                prepBrowser(lbr);
                lbr.setFollowRedirects(true);
                lbr.getPage("http://x-art.com/members/");
                Form loginform = br.getForm(0);
                if (loginform == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                doThis(lbr);

                Browser br2 = lbr.cloneBrowser();
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br2.getHeaders().put("Accept-Charset", null);
                br2.getHeaders().put("Cache-Control", null);
                br2.getHeaders().put("Pragma", null);
                br2.postPage("/includes/ajax_process.php", "action=remember_login");

                loginform.put("uid", Encoding.urlEncode(account.getUser()));
                loginform.put("pwd", Encoding.urlEncode(account.getPass()));

                lbr.submitForm(loginform);
                if (lbr.getCookie(this.getHost(), "sd_session_id") == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }

                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = lbr.getCookies(this.getHost());
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);

                // logic to randomise the next login attempt, to prevent issues with static login detection
                long ran2 = 0;
                // between 2 hours && 6 hours
                while (ran2 == 0 || (ran2 <= 7200000 && ran2 >= 21600000)) {
                    // generate new ran1 for each while ran2 valuation.
                    long ran1 = 0;
                    while (ran1 <= 1)
                        ran1 = new Random().nextInt(7);
                    // random of 1 hour, times ran1
                    ran2 = new Random().nextInt(3600000) * ran1;
                }
                account.setProperty("nextFullLogin", System.currentTimeMillis() + ran2);
                account.setProperty("lastFullLogin", System.currentTimeMillis());
                account.setProperty("userAgent", agent.string);
                // end of logic
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                account.setProperty("nextFullLogin", Property.NULL);
                account.setProperty("lastFullLogin", Property.NULL);
                account.setProperty("userAgent", Property.NULL);
                throw e;
            } finally {
                lbr.setFollowRedirects(redirect);
            }
        }
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        login(account, br, false);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getResponseCode() == 401) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void doThis(Browser dbr) {
        ArrayList<String> grabThis = new ArrayList<String>();
        grabThis.add("/css/login_new.css");
        grabThis.add("/js/login_new.js");
        grabThis.add("/cptcha.jpg");
        for (String url : grabThis) {
            Browser br2 = dbr.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(url);
            } catch (final Throwable e) {
            } finally {
                try {
                    con.disconnect();
                } catch (final Exception e) {
                }
            }
        }
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // prepBr.setCookie(this.getHost(), "lang", "english");
        if (useRUA) {
            if (agent.string == null) {
                /* we first have to load the plugin, before we can reference it */
                JDUtilities.getPluginForHost("mediafire.com");
                agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
            }
            prepBr.getHeaders().put("User-Agent", agent.string);
        }
        return prepBr;
    }

    private static StringContainer agent = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

}
