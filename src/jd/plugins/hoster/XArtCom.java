package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://x\\-art\\.com/members/(videos/.+)" }, flags = { 2 })
public class XArtCom extends PluginForHost {

    private static Object LOCK = new Object();

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
        String name = null;
        if (parameter.getDownloadURL().contains("/videos/")) {
            name = new Regex(parameter.getDownloadURL(), "/videos/.+/(.+)\\.(mov|mp4|wmv)").getMatch(0);
        }
        if (name == null) name = "Unknown Filename";
        String type = new Regex(parameter.getDownloadURL(), "/videos/.+/(.+)\\.(mov|mp4|wmv)").getMatch(1);
        if (type != null) {
            if ("wmv".equalsIgnoreCase(type)) {
                name = name + ".wmv";
            } else if ("mp4".equalsIgnoreCase(type)) {
                name = name + ".mp4";
            } else if ("mov".equalsIgnoreCase(type)) {
                name = name + ".mov";
            } else {
                name = name + "-" + type + ".mp4";
            }
        }
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
        login(account, false);
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
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getPage("http://x-art.com/members/");
                Form loginform = br.getForm(0);
                if (loginform == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("rpassword", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (br.getCookie(this.getHost(), "sd_usr") == null && br.getCookie(this.getHost(), "sd_pass") == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }

                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(this.getHost());
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

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        login(account, false);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getResponseCode() == 401) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setCookie(this.getHost(), "lang", "english");
        return prepBr;
    }

}
