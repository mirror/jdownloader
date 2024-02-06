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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

/** Base class for xt7.pl and tb7.pl. */
public abstract class Tb7AndXt7PlCORE extends PluginForHost {
    public Tb7AndXt7PlCORE(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/rejestracja");
    }

    protected abstract MultiHosterManagement getMultiHosterManagement();

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/regulamin";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public boolean isResumeable(DownloadLink link, final Account account) {
        return true;
    }

    private void login(final Account account, final String checkURLPath, boolean validateCookies) throws PluginException, IOException {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!validateCookies) {
                    return;
                }
                logger.info("Validating cookies...");
                br.getPage("https://" + getHost() + checkURLPath);
                if (this.isLoggedIN(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + getHost() + "/login");
            final Form loginform = br.getFormbyProperty("id", "login-form");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find loginform");
            }
            loginform.put("login", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (!this.isLoggedIN(br)) {
                throw new AccountInvalidException(getPhrase("PREMIUM_ERROR"));
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            if (!br.getURL().endsWith(checkURLPath)) {
                br.getPage(checkURLPath);
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("href=\"/?wyloguj")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, "/mojekonto", true);
        final AccountInfo ai = new AccountInfo();
        if (br.containsHTML("Brak ważnego dostępu Premium")) {
            /* Not a premium account or expired premium account */
            ai.setExpired(true);
            ai.setStatus(getPhrase("EXPIRED"));
            account.setType(AccountType.FREE);
            return ai;
        } else if (br.containsHTML(">\\s*Brak ważnego dostępu Premium\\s*<")) {
            throw new AccountInvalidException(getPhrase("UNSUPPORTED_ACCOUNT_TYPE_NOT_PREMIUM"));
        }
        account.setType(AccountType.PREMIUM);
        String validUntilDateStr = br.getRegex("(\\d{2}\\.\\d{2}.\\d{4}\\s*/\\s*\\d{2}:\\d{2})").getMatch(0);
        if (validUntilDateStr == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find premium expire date");
        }
        validUntilDateStr = validUntilDateStr.replaceFirst("\\s*/\\s*", "");
        final long expireTimeMillis = TimeFormatter.getMilliSeconds(validUntilDateStr, "dd.MM.yyyyHH:mm", Locale.ENGLISH);
        ai.setValidUntil(expireTimeMillis, br);
        /* 2024-02-06: Is that the daily traffic-limit? [30GB] */
        String otherHostersLimitLeftStr = br.getRegex("Pozostały Limit Premium do wykorzystania:\\s*<b>([^<]+)</b></div>").getMatch(0);
        if (otherHostersLimitLeftStr == null) {
            otherHostersLimitLeftStr = br.getRegex("Pozostały limit na serwisy dodatkowe:\\s*<b>([^<]+)</b></div>").getMatch(0);
        }
        ai.setProperty("TRAFFIC_LEFT", otherHostersLimitLeftStr == null ? getPhrase("UNKNOWN") : SizeFormatter.getSize(otherHostersLimitLeftStr));
        String unlimited = br.getRegex("<br />(.*): <b>Bez limitu</b> \\|").getMatch(0);
        if (unlimited != null) {
            ai.setProperty("UNLIMITED", unlimited);
        }
        ai.setStatus("Premium" + " (" + getPhrase("TRAFFIC_LEFT") + ": " + (otherHostersLimitLeftStr == null ? getPhrase("UNKNOWN") : otherHostersLimitLeftStr) + (unlimited == null ? "" : ", " + unlimited + ": " + getPhrase("UNLIMITED")) + ")");
        if (otherHostersLimitLeftStr != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(otherHostersLimitLeftStr));
        }
        /* Find list of supported hosts */
        br.getPage("/jdhostingi.txt");
        /* Every line = one domain */
        final String[] lines = br.getRequest().getHtmlCode().split("\n");
        final List<String> supportedHosts = new ArrayList<String>();
        for (final String line : lines) {
            supportedHosts.add(line);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        /* This should never get called. */
        throw new AccountRequiredException();
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.getMultiHosterManagement().runCheck(account, link);
        final String directlinkproperty = this.getDikrectlinkproperty();
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
            /* Login without cookie validation */
            login(account, "/mojekonto/sciagaj", false);
        } else {
            /* Generate fresh downloadlink */
            login(account, "/mojekonto/sciagaj", true);
            final UrlQuery query1 = new UrlQuery();
            query1.add("step", "1");
            query1.add("content", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            br.postPage("/mojekonto/sciagaj", query1);
            this.checkErrors(br, link, account);
            final UrlQuery query2 = new UrlQuery();
            query2.add("step", "2");
            query2.add("0", "on");
            br.postPage("/mojekonto/sciagaj", query2);
            dllink = br.getRegex("<a[^>]*href=\"([^\"]+)\"[^>]*>\\s*Pobierz\\s*</a>").getMatch(0);
            if (dllink == null) {
                this.checkErrors(br, link, account);
                this.getMultiHosterManagement().handleErrorGeneric(account, link, "Failed to find final downloadlink", 50);
            }
            link.setProperty(directlinkproperty, dllink);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection();
                this.checkErrors(br, link, account);
                this.getMultiHosterManagement().handleErrorGeneric(account, link, "Final downloadlink did not lead to downloadable content", 50);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    private void checkErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException, InterruptedException, MalformedURLException {
        final String limitReachedErrormessage = br.getRegex("(Wymagane dodatkowe [0-9.]+ MB limitu)").getMatch(0);
        if (limitReachedErrormessage != null) {
            logger.info("Limit reached | Original errormessage: " + limitReachedErrormessage);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("DOWNLOAD_LIMIT"), 1 * 60 * 1000l);
        } else if (br.getBaseURL().contains("invaliduserpass")) {
            throw new AccountInvalidException(getPhrase("PREMIUM_ERROR"));
        } else if (br.getBaseURL().contains("notransfer")) {
            /* No traffic left */
            throw new AccountUnavailableException(getPhrase("NO_TRAFFIC"), 5 * 60 * 1000);
        } else if (br.getBaseURL().contains("serviceunavailable")) {
            this.getMultiHosterManagement().handleErrorGeneric(account, link, "serviceunavailable", 50);
        } else if (br.getBaseURL().contains("connecterror")) {
            this.getMultiHosterManagement().handleErrorGeneric(account, link, "connecterror", 50);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            getMultiHosterManagement().runCheck(account, link);
            return true;
        } else {
            /* Download without account is not possible */
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(getDikrectlinkproperty());
    }

    private String getDikrectlinkproperty() {
        return "directlink_" + getHost();
    }

    @Override
    public void extendAccountSettingsPanel(final Account account, final PluginConfigPanelNG panel) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai == null) {
            return;
        }
        if (AccountType.PREMIUM.equals(account.getType())) {
            final long otherHostersLimit = Long.parseLong(ai.getProperty("TRAFFIC_LEFT").toString(), 10);
            final String unlimited = (String) (ai.getProperty("UNLIMITED"));
            panel.addStringPair(_GUI.T.lit_traffic_left(), SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), otherHostersLimit) + (unlimited == null ? "" : "\n" + unlimited + ": " + getPhrase("UNLIMITED")));
        }
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("PREMIUM_ERROR", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                                                      put("UNSUPPORTED_ACCOUNT_TYPE_NOT_PREMIUM", "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.");
                                                      put("PLUGIN_BROKEN", "\r\nPlugin broken, please contact the JDownloader Support!");
                                                      put("TRAFFIC_LEFT", "Traffic left");
                                                      put("HOSTER_UNAVAILABLE", "Host is temporarily unavailable via");
                                                      put("DOWNLOAD_LIMIT", "Download limit exceeded!");
                                                      put("RETRY", "Retry in few secs");
                                                      put("LINK_INACTIVE", "Xt7 reports the link is as inactive!");
                                                      put("LINK_EXPIRED", "Previously generated Link expired!");
                                                      put("NO_TRAFFIC", "No traffic left");
                                                      put("UNKNOWN_ERROR", "Unable to handle this errorcode!");
                                                      put("ACCOUNT_TYPE", "Account type");
                                                      put("UNKNOWN", "Unknown");
                                                      put("UNLIMITED", "Unlimited");
                                                      put("FREE", "free");
                                                      put("EXPIRED", "Account expired/free");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("PREMIUM_ERROR", "\r\nNieprawidłowy użytkownik/hasło!\r\nUpewnij się, że wprowadziłeś poprawnie użytkownika i hasło. Podpowiedzi:\r\n1. Jeśli w twoim haśle znajdują się znaki specjalne - usuń je/popraw i wprowadź ponownie hasło!\r\n2. Wprowadzając nazwę użytkownika i hasło - nie używaj operacji Kopiuj i Wklej.");
                                                      put("UNSUPPORTED_ACCOUNT_TYPE_NOT_PREMIUM", "\r\nNieobsługiwany typ konta!\r\nJesli uważasz, że informacja ta jest niepoprawna i chcesz aby dodac obsługę tego typu konta\r\nskontaktuj się z nami poprzez forum wsparcia.");
                                                      put("PLUGIN_BROKEN", "\r\nProblem z wtyczką, skontaktuj się z zespołem wsparcia JDownloader!");
                                                      put("TRAFFIC_LEFT", "Pozostały transfer");
                                                      put("HOSTER_UNAVAILABLE", "Serwis jest niedostępny przez");
                                                      put("DOWNLOAD_LIMIT", "Przekroczono dostępny limit transferu!");
                                                      put("RETRY", "Ponawianie za kilka sekund");
                                                      put("LINK_INACTIVE", "Xt7 raportuje link jako nieaktywny!");
                                                      put("LINK_EXPIRED", "Poprzednio wygenerowany link wygasł!");
                                                      put("NO_TRAFFIC", "Brak dostępnego transferu");
                                                      put("UNKNOWN_ERROR", "Nieobsługiwany kod błędu!");
                                                      put("ACCOUNT_TYPE", "Typ konta");
                                                      put("UNKNOWN", "Nieznany");
                                                      put("UNLIMITED", "Bez limitu");
                                                      put("FREE", "darmowe");
                                                      put("EXPIRED", "Konto wygasło/darmowe");
                                                  }
                                              };

    /**
     * Returns a Polish/English translation of a phrase. We don't use the JDownloader translation framework since we need only Polish and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        String language = System.getProperty("user.language");
        if ("pl".equals(language) && phrasesPL.containsKey(key)) {
            return phrasesPL.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }
}