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

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://(?:www\\.)?(x\\-art(decrypted)?\\.com/(members/)?(videos|galleries)/.+|([a-z0-9]+\\.)?x-art(decrypted)?\\.com/.+\\.(mov|mp4|wmv|zip).*)" })
public class XArtCom extends PluginForHost {

    public static interface XArtConfigInterface extends PluginConfigInterface {

        @DefaultBooleanValue(false)
        @Order(10)
        boolean isGrabBestVideoVersionEnabled();

        void setGrabBestVideoVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(20)
        boolean isGrab4KVideoEnabled();

        void setGrab4KVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrab1080pVideoEnabled();

        void setGrab1080pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(40)
        boolean isGrab720pVideoEnabled();

        void setGrab720pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(50)
        boolean isGrab540pVideoEnabled();

        void setGrab540pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(60)
        boolean isGrab360pVideoEnabled();

        void setGrab360pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(70)
        boolean isGrabBestImagesVersionEnabled();

        void setGrabBestImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(80)
        boolean isGrab1200pImagesVersionEnabled();

        void setGrab1200pImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrab2000pImagesVersionEnabled();

        void setGrab2000pImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrab4000pImagesVersionEnabled();

        void setGrab4000pImagesVersionEnabled(boolean b);

    }

    private static Object LOCK = new Object();

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("decrypted\\.com", ".com"));
    }

    public XArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.x-art.com/join/");
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return XArtConfigInterface.class;
    }

    @Override
    public String getAGBLink() {
        return "http://www.x-art.com/legal/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (parameter.getFinalFileName() == null) {
            String name = getFileNameFromURL(new URL(parameter.getPluginPatternMatcher()));
            if (name == null) {
                name = new Regex(parameter.getPluginPatternMatcher(), "x-art.com/(.+)").getMatch(0);
            }
            parameter.setName(name);
        }
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
        Account account = null;
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }
        this.setBrowserExclusive();
        if (account != null) {
            login(account, br, false);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter urlcon = null;
        try {
            urlcon = br.openHeadConnection(parameter.getPluginPatternMatcher());
            final int responseCode = urlcon.getResponseCode();
            if (urlcon.isOK() && !StringUtils.containsIgnoreCase(urlcon.getContentType(), "text")) {
                if (urlcon.getLongContentLength() > 0) {
                    parameter.setVerifiedFileSize(urlcon.getLongContentLength());
                }
                return AvailableStatus.TRUE;
            }
            if (account != null) {
                final String pageURL = parameter.getStringProperty("pageURL", null);
                String newURL = null;
                if (pageURL != null) {
                    final String videoID = parameter.getStringProperty("videoID", null);
                    final String imageID = parameter.getStringProperty("imageID", null);
                    final String quality = parameter.getStringProperty("quality", null);
                    final String ext = parameter.getStringProperty("ext", null);
                    final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
                    jd.plugins.decrypter.XArt.parseUrl(br, this, results, pageURL, true);
                    if (imageID != null) {
                        for (final DownloadLink result : results) {
                            if (StringUtils.equals(imageID, result.getStringProperty("imageID", null)) && StringUtils.equals(quality, result.getStringProperty("quality", null))) {
                                correctDownloadLink(result);
                                newURL = result.getPluginPatternMatcher();
                                break;
                            }
                        }
                    } else if (videoID != null) {
                        for (final DownloadLink result : results) {
                            if (StringUtils.equals(videoID, result.getStringProperty("videoID", null)) && StringUtils.equals(quality, result.getStringProperty("quality", null)) && StringUtils.equals(ext, result.getStringProperty("ext", null))) {
                                correctDownloadLink(result);
                                newURL = result.getPluginPatternMatcher();
                                break;
                            }
                        }
                    }
                }
                if (newURL == null) {
                    parameter.setPluginPatternMatcher(newURL);
                    return AvailableStatus.TRUE;
                }
            }
            if (responseCode == 401 && account != null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                if (urlcon != null) {
                    urlcon.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return account != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        // check if it's time for the next full login.
        if (account.getStringProperty("nextFullLogin") != null && (System.currentTimeMillis() <= Long.parseLong(account.getStringProperty("nextFullLogin")))) {
            login(account, br, false);
        } else {
            login(account, br, true);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    public void login(final Account account, Browser lbr, final boolean force) throws Exception {
        synchronized (LOCK) {
            final boolean redirect = lbr.isFollowingRedirects();
            try {
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    lbr.setCookies(this.getHost(), cookies);
                    prepBrowser(lbr);
                    lbr.setFollowRedirects(true);
                    lbr.getPage("http://www.x-art.com/members/");
                    if (lbr.containsHTML(">Logout</a>")) {
                        return;
                    }
                }
                prepBrowser(lbr);
                lbr.setFollowRedirects(true);
                lbr.getPage("http://www.x-art.com/members/");
                final Form loginform = lbr.getForm(0);
                if (loginform == null) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                doThis(lbr);
                final Form rememberLogin = new Form();
                rememberLogin.setMethod(MethodType.POST);
                rememberLogin.setAction(lbr.getURL("/includes/ajax_process.php").toString());
                rememberLogin.put("action", "remember_login");
                final PostRequest loginRequest = new PostRequest(rememberLogin.getAction());
                loginRequest.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                loginRequest.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                loginRequest.getHeaders().put("Accept-Charset", null);
                loginRequest.getHeaders().put("Cache-Control", null);
                loginRequest.getHeaders().put("Pragma", null);
                final Browser br2 = lbr.cloneBrowser();
                br2.submitForm(rememberLogin);

                loginform.put("uid", Encoding.urlEncode(account.getUser()));
                loginform.put("pwd", Encoding.urlEncode(account.getPass()));
                lbr.submitForm(loginform);
                if (!lbr.containsHTML(">Logout</a>")) {
                    String lang = System.getProperty("user.language");
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(lbr.getCookies(this.getHost()), "");
                // logic to randomise the next login attempt, to prevent issues with static login detection
                long ran2 = 0;
                // between 2 hours && 6 hours
                while (ran2 == 0 || (ran2 <= 7200000 && ran2 >= 21600000)) {
                    // generate new ran1 for each while ran2 valuation.
                    long ran1 = 0;
                    while (ran1 <= 1) {
                        ran1 = new Random().nextInt(7);
                    }
                    // random of 1 hour, times ran1
                    ran2 = new Random().nextInt(3600000) * ran1;
                }
                account.setProperty("nextFullLogin", System.currentTimeMillis() + ran2);
                account.setProperty("lastFullLogin", System.currentTimeMillis());
                // end of logic
            } catch (final PluginException e) {
                account.clearCookies("");
                account.setProperty("nextFullLogin", Property.NULL);
                account.setProperty("lastFullLogin", Property.NULL);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, -5);
        if (dl.getConnection().getResponseCode() == 401) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void doThis(Browser dbr) {
        final ArrayList<String> grabThis = new ArrayList<String>();
        grabThis.add("/css/zurb/common?v=2.1 ");
        grabThis.add("/js/zurb/common-login?v=2.1");
        grabThis.add("/cptcha.jpg");
        for (final String url : grabThis) {
            final Browser br2 = dbr.cloneBrowser();
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

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        return prepBr;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the x-art.com plugin.";
    }

}
