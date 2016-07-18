//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumy.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class PremiumyPl extends PluginForHost {
    private String                                         MAINPAGE           = "https://premiumy.pl/";
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();

    public PremiumyPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "login,process.html");
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private void login(Account account, boolean force) throws PluginException, IOException {
        synchronized (LOCK) {
            try {
                br.postPage(MAINPAGE + "login,signin.html", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("<div class=\"inputError\">Niepoprawny login i/lub hasło\\.</div>")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("ERROR_PREMIUM"), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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

    List<String> getSupportedHosts(AccountInfo ai) {
        final ArrayList<String> availableHosts = new ArrayList<String>(Arrays.asList("catshare.net", "rapidu.net", "uploaded.to", "fileshark.pl", "turbobit.net", "devilshare.net"));
        List<String> supportedHosts = new ArrayList<String>();
        String accountInfoDetails = new String();
        String tableOfHosts = new Regex(br, "div class=\"contentTitle\">Statystyki i limity</div>[ \t\n\r\f]+<table class=\"table\">[ \t\n\r\f]+<tr class=\"tableHeader\"><td>Hosting</td><td>Pobrano dzisiaj</td><td>Limit dzienny</td></tr>(.*)</table>").getMatch(0);
        String hosters[][] = null;
        if (tableOfHosts != null) {
            hosters = new Regex(tableOfHosts, "<tr><td style=\"color: (red|green)\">([^<>\"]+)</td><td>(\\d+[MTG]?B)</td><td>(\\d+[MTG]?B)</td></tr>").getMatches();
        }
        if (hosters.length > 0) {
            for (int i = 0; i < hosters.length; i++) {
                String hosterAvailable = hosters[i][0];
                String hoster = hosters[i][1];
                String hosterDownloaded = hosters[i][2];
                String hosterDailyLimit = hosters[i][3];
                if ("green".equals(hosterAvailable)) {
                    for (String availableHost : availableHosts) {
                        if (availableHost.contains(hoster.toLowerCase())) {
                            supportedHosts.add(availableHost);
                            if (accountInfoDetails.contains(getPhrase("HOSTER"))) {
                                accountInfoDetails += "\n" + getPhrase("HOSTER") + ": " + availableHost + ", " + getPhrase("DOWNLOADED") + ": " + hosterDownloaded + ", " + getPhrase("DAILYLIMIT") + ": " + hosterDailyLimit;
                            } else {
                                accountInfoDetails = getPhrase("HOSTER") + ": " + availableHost + ", " + getPhrase("DOWNLOADED") + ": " + hosterDownloaded + ", " + getPhrase("DAILYLIMIT") + ": " + hosterDailyLimit;
                            }
                            break;
                        }
                    }
                }
            }
            if (accountInfoDetails != null) {
                ai.setProperty("DETAILS", accountInfoDetails);
            }
            return supportedHosts;
        } else {
            return null;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        String packageInfo = "";
        String packageName = "";
        AccountInfo ai = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(true);
        login(account, true);
        br.getPage(MAINPAGE + "limits.html");
        List<String> supportedHosts = getSupportedHosts(ai);
        // hoster packages
        if (supportedHosts.size() > 0) {
            packageInfo = br.getRegex("<div class=\"contentTitleGreen\">Pakiety</div>[ \t\n\r\f]+<div class=\"packagesInfo\">[ \t\n\r\f]+(.*)<br /><div class=\"separator\"></div>[ \t\n\r\f]+</div>").getMatch(0);
            if (packageInfo == null) {
                packageInfo = br.getRegex("<div class=\"contentTitleGreen\">Pakiety</div>[ \t\n\r\f]+<div class=\"packagesInfo\">[ \t\n\r\f]+(.*)<br />[ \t\n\r\f]+</div>[ \t\n\r\f]+</div>").getMatch(0);
                if (packageInfo != null) {
                    if (packageInfo.contains("Multi:") && packageInfo.contains("Transfer:")) {
                        packageName = "Multi + Transfer";
                        ai.setProperty("DETAILS", (String) ai.getProperty("DETAILS") + "\n" + packageInfo.replace("<br /><div class=\"separator\"></div>", ", "));
                    } else {
                        packageName = "Transfer";
                        String trafficLeft = new Regex(packageInfo, "Transfer: (\\d+\\.?\\d?[MGT]?B)").getMatch(0);
                        ai.setTrafficLeft(trafficLeft);
                        ai.setProperty("DETAILS", (String) ai.getProperty("DETAILS") + "\n" + packageInfo);
                    }
                }
            } else {
                // hoster package
                if (packageInfo.contains("Multi")) {
                    packageName = "Multi";
                    ai.setProperty("DETAILS", (String) ai.getProperty("DETAILS") + "\n" + packageInfo);
                } else {
                    packageName = getPhrase("HOSTER");
                    String pattern = getPhrase("HOSTER") + ": [^\"<>,]+, " + getPhrase("DOWNLOADED") + ": (\\d+[MGT]?B), " + getPhrase("DAILYLIMIT") + ": (\\d+[MGT]?B)";
                    String trafficUsed = new Regex(ai.getProperty("DETAILS"), pattern).getMatch(0);
                    String trafficLeft = new Regex(ai.getProperty("DETAILS"), pattern).getMatch(1);
                    ai.setTrafficMax(trafficLeft);
                    long traffic = SizeFormatter.getSize(trafficLeft) - SizeFormatter.getSize(trafficUsed);
                    ai.setTrafficLeft(traffic);
                    ai.setProperty("DETAILS", (String) ai.getProperty("DETAILS") + "\n" + getPhrase("EXPIRE_DATE") + ": " + packageInfo.replaceFirst("Serwis [A-Za-z0-9^ ]+: ", ""));
                }
                String validUntil = new Regex(packageInfo, "[A-Za-z]+: (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
                long expireTime = TimeFormatter.getMilliSeconds(validUntil, "yyyy-MM-dd HH:mm", Locale.ENGLISH);
                ai.setValidUntil(expireTime);
            }
        }
        if (packageInfo == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("UNSUPPORTED_PREMIUM"), PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        ai.setStatus("PREMIUM " + packageName);
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "1,regulamin.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("HOSTER_UNAVAILABLE") + " " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        final String downloadUrl = link.getPluginPatternMatcher();
        boolean resume = true;
        showMessage(link, "Phase 1/3: Login");
        br.setFollowRedirects(true);
        login(account, true);
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        dl = null;
        String generatedLink = checkDirectLink(link, "generatedLink");
        if (generatedLink == null) {
            /* generate new downloadlink */
            String url = Encoding.urlEncode(downloadUrl);
            String postData = "links=" + url;
            showMessage(link, "Phase 2/3: Generating Link");
            br.postPage(MAINPAGE + "download,check.html", postData);
            if (br.containsHTML("<b class=\"inputError\">Błąd hostingu lub nieprawidłowy link</b>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String pattern = "<a href=\"(.*)\"[^\"<>]+title=\"";
            generatedLink = br.getRegex(pattern).getMatch(0);
            if (generatedLink == null) {
                logger.severe("premiumy.pl(Error): " + generatedLink);
                //
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("HOSTER_UNAVAILABLE"), 20 * 1000l);
            }
            link.setProperty("generatedLink", generatedLink);
        }
        // wait, workaround
        sleep(1 * 1000l, link);
        int chunks = 0;
        if (downloadUrl.contains("catshare.net")) {
            chunks = 4;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedLink, resume, chunks);
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) // unknown
        // error
        {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("PLUGIN_BROKEN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (dl.getConnection().getResponseCode() == 404) {
            /* file offline */
            dl.getConnection().disconnect();
            tempUnavailableHoster(account, link, 20 * 60 * 1000l);
        }
        showMessage(link, "Phase 3/3: Begin download");
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    // try redirected link
                    boolean resetGeneratedLink = true;
                    String redirectConnection = br2.getRedirectLocation();
                    if (redirectConnection != null) {
                        if (redirectConnection.contains("premiumy.pl")) {
                            con = br2.openGetConnection(redirectConnection);
                            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                                resetGeneratedLink = true;
                            } else {
                                resetGeneratedLink = false;
                            }
                        } else { // turbobit link is already redirected link
                            resetGeneratedLink = false;
                        }
                    }
                    if (resetGeneratedLink) {
                        downloadLink.setProperty(property, Property.NULL);
                        dllink = null;
                    }
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("UNKNOWN_ERROR"));
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("generatedLinkTb7", null);
    }

    @Override
    public void extendAccountSettingsPanel(Account acc, PluginConfigPanelNG panel) {
        AccountInfo ai = acc.getAccountInfo();
        String details = (String) (ai.getProperty("DETAILS"));
        if (StringUtils.isNotEmpty(details)) {
            panel.addDescription(details);
        }
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("ERROR_PREMIUM", "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                                                      put("HOSTER", "Hoster");
                                                      put("DOWNLOADED", "Downloaded");
                                                      put("DAILYLIMIT", "Daily Limit");
                                                      put("UNSUPPORTED_PREMIUM", "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.");
                                                      put("HOSTER", "Hoster");
                                                      put("PLUGIN_BROKEN", "\r\nPlugin broken, please contact the JDownloader Support!");
                                                      put("HOSTER_UNAVAILABLE", "Host is temporarily unavailable");
                                                      put("EXPIRE_DATE", "Expiration date");
                                                  }
                                              };
    private HashMap<String, String> phrasesPL = new HashMap<String, String>() {
                                                  {
                                                      put("ERROR_PREMIUM", "\r\nNieprawidłowy użytkownik/hasło!\r\nUpewnij się, że wprowadziłeś poprawnie użytkownika i hasło. Podpowiedzi:\r\n1. Jeśli w twoim haśle znajdują się znaki specjalne - usuń je/popraw i wprowadź ponownie hasło!\r\n2. Wprowadzając nazwę użytkownika i hasło - nie używaj operacji Kopiuj i Wklej.");
                                                      put("HOSTER", "Serwis");
                                                      put("DOWNLOADED", "Pobrano");
                                                      put("DAILYLIMIT", "Dzienny Limit");
                                                      put("UNSUPPORTED_PREMIUM", "\r\nNieobsługiwany typ konta!\r\nJesli uważasz, że informacja ta jest niepoprawna i chcesz aby dodac obsługę tego typu konta\r\nskontaktuj się z nami poprzez forum wsparcia.");
                                                      put("HOSTER", "Serwis");
                                                      put("PLUGIN_BROKEN", "\r\nProblem z wtyczką, skontaktuj się z zespołem wsparcia JDownloader!");
                                                      put("HOSTER_UNAVAILABLE", "Serwis jest niedostępny");
                                                      put("EXPIRE_DATE", "Data ważności");
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