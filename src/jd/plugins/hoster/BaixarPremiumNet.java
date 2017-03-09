//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "baixarpremium.net" }, urls = { "" })
public class BaixarPremiumNet extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(20);
    private static final String                            MAINPAGE           = "http://baixarpremium.net";

    private static final String                            NICE_HOST          = "baixarpremium.net";
    private static final String                            NICE_HOSTproperty  = NICE_HOST.replaceAll("(\\.|\\-)", "");

    private Account                                        currAcc            = null;
    private DownloadLink                                   currDownloadLink   = null;

    @SuppressWarnings("deprecation")
    public BaixarPremiumNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://baixarpremium.net/");
    }

    @Override
    public String getAGBLink() {
        return "http://baixarpremium.net/contato/";
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        return maxPrem.get();
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setConstants(account, null);
        return fetchAccountInfoBaixar(this, this.br, account);
    }

    public static AccountInfo fetchAccountInfoBaixar(final PluginForHost plugin, final Browser br, final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        if (!((jd.plugins.hoster.BaixarPremiumNet) JDUtilities.getPluginForHost("baixarpremium.net")).login(br, account, true)) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br.getPage("http://" + account.getHoster() + "/gerador/");
        final boolean is_premium = br.containsHTML("id=\"BaixarLinkstxt\"");
        br.getPage("/contas-ativas/");
        /* free accounts are not supported */
        final String hoststext = br.getRegex("premium aos servidores <span style=\"[^\"]+\">(.*?)<").getMatch(0);
        if (br.containsHTML(">\\s*Você não possui nenhum pacote de Conta Premium\\.\\s*<") || !is_premium) {
            account.setType(AccountType.FREE);
            ac.setStatus("Free Account");
            ac.setTrafficLeft(0);
        } else {
            /*
             * Important: Ignore expire date as it depends on every host - we could show the expire date that lasts longest but that's too
             * much effort for a website without API!
             */
            account.setType(AccountType.PREMIUM);
            ac.setStatus("Premium Account");
            ac.setUnlimitedTraffic();
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] possible_domains = { "com.br", "br", "to", "de", "com", "net", "co.nz", ".nz", "in", "co", "me", "biz", "ch", "pl", "us", "cc", "eu" };
        String[] crippledHosts;
        if (hoststext != null) {
            crippledHosts = hoststext.split(", ");
        } else {
            br.getPage("http://" + account.getHoster() + "/");
            crippledHosts = br.getRegex("theme/img/([A-Za-z0-9\\-]+)\\.(?:jpg|jpeg|png)\"").getColumn(0);
            if (crippledHosts.length == 0 || crippledHosts.length <= 5) {
                /* Especially for comprarpremium.com */
                crippledHosts = br.getRegex("/srv/([A-Za-z0-9\\-]+)\\-logo\\.(?:jpg|jpeg|png)\"").getColumn(0);
            }
        }
        for (String crippledhost : crippledHosts) {
            crippledhost = crippledhost.trim();
            crippledhost = crippledhost.toLowerCase();
            if (crippledhost.equals("shareonline")) {
                supportedHosts.add("share-online.biz");
            } else {
                /* Go insane */
                for (final String possibledomain : possible_domains) {
                    final String full_possible_host = crippledhost + "." + possibledomain;
                    supportedHosts.add(full_possible_host);
                }
            }
        }
        ac.setMultiHostSupport(plugin, supportedHosts);
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {

        setConstants(account, link);

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        login(this.br, account, false);
        final String dllink = getDllinkBaixar(this.br, this.currAcc, this.currDownloadLink);
        if (!dllink.startsWith("http")) {
            handleErrorRetries("dllinknull", 50, 2 * 60 * 1000l);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleDlErrors(this.br, this.currAcc);
            handleErrorRetries("unknowndlerror", 50, 2 * 60 * 1000l);
        }
        /* Now we know for sure that it is a premium account. */
        account.getAccountInfo().setStatus("Premium account");
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    public static String getDllinkBaixar(final Browser br, final Account account, final DownloadLink link) throws IOException, PluginException {
        final String additional_param;
        final String host = account.getHoster();
        if (host.equalsIgnoreCase("comprarpremium.com")) {
            additional_param = "&cp=1";
        } else if (host.equalsIgnoreCase("contacombo.com.br")) {
            additional_param = "&cc=1";
        } else {
            /* E.g. baixarpremium.net */
            additional_param = "";
        }
        final String keypass = br.getCookie(account.getHoster(), "utmhb");
        final String getdata = "?link=" + Encoding.base16Encode(link.getDownloadURL()) + "&keypass=" + keypass + additional_param;

        final String dllink = br.getPage("http://baixarpremium.net/api/index.php" + getdata);
        if (br.toString().equals("Arquivo-nao-encontrado")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.toString().equals("autenticacao")) {
            // account isn't logged in? or so they claim see: jdlog://4236969150841
            account.clearCookies("");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        return dllink;
    }

    public static void handleDlErrors(final Browser br, final Account account) throws PluginException {
        /* Free accounts are not supported */
        if (br.containsHTML("Erro 404 \\- Página Não encontrada")) {
            account.getAccountInfo().setTrafficLeft(0);
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    public boolean login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Workaround for static usage */
                if (this.br == null) {
                    this.br = br;
                }
                final String currenthost = account.getHoster();
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(currenthost, cookies);
                    final Browser test = br.cloneBrowser();
                    test.setFollowRedirects(true);
                    /* Avoid login captchas whenever possible! */
                    test.getPage("http://" + currenthost + "/contas-ativas/");
                    if (test.getURL().endsWith("/contas-ativas/")) {
                        /* Refresh cookie timestamp */
                        account.saveCookies(br.getCookies(currenthost), "");
                        return true;
                    }
                    br.clearCookies(account.getHoster());
                    /* Force full login! */
                }
                br.setFollowRedirects(false);
                br.getPage("http://" + currenthost + "/logar/");
                String postData = "login=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass());
                if (br.containsHTML("/captcha\\.php")) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", currenthost, "http://" + currenthost, true);
                    final String code = getCaptchaCode("/acoes/captcha.php", dummyLink);
                    postData += "&confirmacao=" + Encoding.urlEncode(code);
                }
                br.getHeaders().put("Accept", "*/*");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("/acoes/deslogado/logar.php", postData);
                if (br.getCookie(currenthost, "utmhb") == null || br.containsHTML("Login/E-mail ou senha inválida")) {
                    if (br.toString().equals("6")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nDeine IP ist gesperrt.\r\nÄndere deine IP und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nYour IP is banned.\r\nChange your IP and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(currenthost), "");
                return true;
            } catch (final PluginException e) {
                account.clearCookies("");
                return false;
            }
        }
    }

    private void tempUnavailableHoster(final long timeout) throws PluginException {
        if (this.currDownloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(this.currAcc);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(this.currAcc, unavailableMap);
            }
            unavailableMap.put(this.currDownloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before we temporarily remove the host
     * from the host list.
     *
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrorRetries(final String error, final int maxRetries, final long disableTime) throws PluginException {
        int timesFailed = this.currDownloadLink.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        this.currDownloadLink.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            this.currDownloadLink.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(disableTime);
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}