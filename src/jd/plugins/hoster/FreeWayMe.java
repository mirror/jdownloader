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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.images.NewTheme;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FreeWayMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap               = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem                          = new AtomicInteger(1);
    private final String                                   ALLOWRESUME                      = "ALLOWRESUME";
    private final String                                   BETAUSER                         = "FREEWAYBETAUSER";

    private final String                                   NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE = "NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE";
    private final String                                   NOTIFY_ON_FULLSPEED_LIMIT_DIALOG = "NOTIFY_ON_FULLSPEED_LIMIT_DIALOG";

    private static final String                            NORESUME                         = "NORESUME";
    private static final String                            PREVENTSPRITUSAGE                = "PREVENTSPRITUSAGE";

    public final String                                    ACC_PROPERTY_CONNECTIONS         = "parallel";
    public final String                                    ACC_PROPERTY_TRAFFIC_REDUCTION   = "ACC_TRAFFIC_REDUCTION";
    public final String                                    ACC_PROPERTY_UNKOWN_FAILS        = "timesfailedfreewayme_unknown";

    public FreeWayMe(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2 * 1000l);
        setConfigElements();
        this.enablePremium("https://www.free-way.me/premium");
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_RESUME", "Enable resume of stopped downloads (Warning: This can cause CRC errors)");
                                                      put("SETTING_BETA", "Enable beta service (Requires free-way beta account)");
                                                      put("SETTING_SPRITUSAGE", "Stop download if sprit would be used");
                                                      put("ERROR_INVALID_LOGIN", "Invalid username/password!");
                                                      put("ERROR_BAN", "Account banned");
                                                      put("ERROR_UNKNOWN", "Unknown error");
                                                      put("ERROR_UNKNOWN_FULL", "Unknown account status (deactivated)!");
                                                      put("CHECK_ZERO_HOSTERS", "Account valid: 0 Hosts via free-way.me available");
                                                      put("SUPPORTED_HOSTS_1", "Account valid: ");
                                                      put("SUPPORTED_HOSTS_2", " Hosts via free-way.me available");
                                                      put("ERROR_INVALID_URL", "Invalid URL");
                                                      put("ERROR_RETRY_SECONDS", "Error: Retry in few secs");
                                                      put("ERROR_SERVER", "Server error");
                                                      put("ERROR_UNKNWON_CODE", "Unable to handle this errorcode!");
                                                      put("ERROR_HOST_TMP_DISABLED", "Host temporary disabled");
                                                      put("ERROR_INAVLID_HOST_URL", "Invalid host link");
                                                      put("ERROR_CONNECTIONS", "Too many simultan downloads");
                                                      put("ERROR_TRAFFIC_LIMIT", "Traffic limit");
                                                      put("DETAILS_TITEL", "Account information");
                                                      put("DETAILS_CATEGORY_ACC", "Account");
                                                      put("DETAILS_ACCOUNT_NAME", "Account name:");
                                                      put("DETAILS_ACCOUNT_TYPE", "Account type:");
                                                      put("DETAILS_SIMULTAN_DOWNLOADS", "Simultaneous Downloads:");
                                                      put("DETAILS_FULLSPEED_TRAFFIC", "Fullspeed traffic:");
                                                      put("DETAILS_FULLSPEED_UNKOWN", "unknown");
                                                      put("DETAILS_FULLSPEED_REDUCED", "reduced");
                                                      put("DETAILS_NOTIFICATIONS", "Notifications:");
                                                      put("DETAILS_CATEGORY_HOSTS", "Supported Hosts");
                                                      put("DETAILS_HOSTS_AMOUNT", "Amount: ");
                                                      put("DETAILS_REVISION", "Plugin Revision:");
                                                      put("CLOSE", "Close");
                                                      put("ERROR_PREVENT_SPRIT_USAGE", "Sprit usage prevented!");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION", "Fullspeedlimit");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_MSG", "You used your todays free-way.me fullspeed traffic. Your speed is limited until midnight.");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE", "Enable <u>bubble notification</u> if fullspeed limit is reached");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG", "Enable <u>dialog notification</u> if fullspeed limit is reached");
                                                  }
                                              };

    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_RESUME", "Aktiviere das Fortsetzen von gestoppen Downloads (Warnung: Kann CRC-Fehler verursachen)");
                                                      put("SETTING_BETA", "Aktiviere Betamodus (Erfordert einen free-way Beta-Account)");
                                                      put("SETTING_SPRITUSAGE", "Nicht Downloaden, falls Sprit verwendet wird (Spender-Account)");
                                                      put("ERROR_INVALID_LOGIN", "Ungültiger Benutzername oder ungültiges Passwort!");
                                                      put("ERROR_BAN", "Account gesperrt!");
                                                      put("ERROR_UNKNOWN", "Unbekannter Fehler");
                                                      put("ERROR_UNKNOWN_FULL", "Unbekannter Accountstatus (deaktiviert)!");
                                                      put("CHECK_ZERO_HOSTERS", "Account gültig: 0 Hosts via free-way.me verfügbar");
                                                      put("SUPPORTED_HOSTS_1", "Account gültig: ");
                                                      put("SUPPORTED_HOSTS_2", " Hoster über free-way.me verfügbar");
                                                      put("ERROR_INVALID_URL", "Ungültige URL");
                                                      put("ERROR_RETRY_SECONDS", "Fehler: Erneuter Versuch in wenigen sek.");
                                                      put("ERROR_SERVER", "Server Fehler");
                                                      put("ERROR_UNKNWON_CODE", "Unbekannter Fehlercode!");
                                                      put("ERROR_HOST_TMP_DISABLED", "Hoster temporär deaktiviert!");
                                                      put("ERROR_INAVLID_HOST_URL", "Ungültiger Hoster Link");
                                                      put("ERROR_CONNECTIONS", "Zu viele parallele Downloads");
                                                      put("ERROR_TRAFFIC_LIMIT", "Traffic Begrenzung");
                                                      put("DETAILS_TITEL", "Account Zusatzinformationen");
                                                      put("DETAILS_CATEGORY_ACC", "Account");
                                                      put("DETAILS_ACCOUNT_NAME", "Account Name:");
                                                      put("DETAILS_ACCOUNT_TYPE", "Account Typ:");
                                                      put("DETAILS_SIMULTAN_DOWNLOADS", "Gleichzeitige Downloads:");
                                                      put("DETAILS_FULLSPEED_TRAFFIC", "Fullspeedvolumen verbraucht:");
                                                      put("DETAILS_FULLSPEED_UNKOWN", "unbekannt");
                                                      put("DETAILS_FULLSPEED_REDUCED", "gedrosselt");
                                                      put("DETAILS_NOTIFICATIONS", "Benachrichtigungen:");
                                                      put("DETAILS_CATEGORY_HOSTS", "Unterstützte Hoster");
                                                      put("DETAILS_HOSTS_AMOUNT", "Anzahl: ");
                                                      put("DETAILS_REVISION", "Plugin Version:");
                                                      put("CLOSE", "Schließen");
                                                      put("ERROR_PREVENT_SPRIT_USAGE", "Spritverbrauch verhindert!");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION", "Fullspeed-Limit");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_MSG", "Das heutige Fullspeedvolumen für free-way.me wurde aufgebraucht. Die Geschwindigkeit ist bis Mitternacht gedrosselt.");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE", "Aktiviere <u>Bubble-Benachrichtigung</u> wenn das Fullspeedlimit ausgeschöpft ist");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG", "Aktiviere <u>Dialog-Benachrichtigung</u> wenn das Fullspeedlimit ausgeschöpft ist");
                                                  }
                                              };

    /**
     * Returns a germen/english translation of a phrase - we don't use the JDownloader translation framework since we need only germen and
     * english (provider is german)
     * 
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) { return phrasesEN.get(key); }
        return "Translation not found!";
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOWRESUME, getPhrase("SETTING_RESUME")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREVENTSPRITUSAGE, getPhrase("SETTING_SPRITUSAGE")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), BETAUSER, getPhrase("SETTING_BETA")).setDefaultValue(false));

        boolean isBeta = this.getPluginConfig().getBooleanProperty(BETAUSER, false);
        if (isBeta) {
            getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE, getPhrase("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE")).setDefaultValue(false));
            getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NOTIFY_ON_FULLSPEED_LIMIT_DIALOG, getPhrase("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG")).setDefaultValue(false));
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.free-way.me/agb";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        logger.info("{fetchAccInfo} Update free-way account: " + account.getUser());
        AccountInfo ac = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.setConnectTimeout(40 * 1000);
        br.setReadTimeout(40 * 1000);
        String username = Encoding.urlTotalEncode(account.getUser());
        String pass = Encoding.urlTotalEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("https://www.free-way.me/ajax/jd.php?id=1&user=" + username + "&pass=" + pass + "&encoded");
        // "Invalid login" / "Banned" / "Valid login"
        if (br.toString().equalsIgnoreCase("Valid login")) {
            logger.info("{fetchAccInfo} Account " + username + " is valid");
        } else if (br.toString().equalsIgnoreCase("Invalid login")) {
            account.setError(AccountError.INVALID, getPhrase("ERROR_INVALID_LOGIN"));
            logger.info("{fetchAccInfo} Account " + username + " is invalid");
            logger.info("{fetchAccInfo} Request result: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + getPhrase("ERROR_INVALID_LOGIN"), PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.toString().equalsIgnoreCase("Banned")) {
            logger.info("{fetchAccInfo} Account banned by free-way! -> advise to contact free-way support");
            logger.info("{fetchAccInfo} Request result: " + br.toString());
            account.setError(AccountError.INVALID, getPhrase("ERROR_BAN"));
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("ERROR_BAN"), PluginException.VALUE_ID_PREMIUM_DISABLE);

        } else {
            logger.severe("{fetchAccInfo} Unknown ERROR!");
            logger.severe("{fetchAccInfo} Add to error parser: " + br.toString());
            // unknown error
            account.setError(AccountError.INVALID, getPhrase("ERROR_UNKNOWN"));
            throw new PluginException(LinkStatus.ERROR_PREMIUM, getPhrase("ERROR_UNKNOWN_FULL"), PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        // account should be valid now, let's get account information:
        br.getPage("https://www.free-way.me/ajax/jd.php?id=4&user=" + username + "&pass=" + pass + "&encoded");

        int maxPremi = 1;
        final String maxPremApi = getJson("parallel", br.toString());
        if (maxPremApi != null) {
            maxPremi = Integer.parseInt(maxPremApi);
            account.setProperty(ACC_PROPERTY_CONNECTIONS, maxPremApi);
        }
        maxPrem.set(maxPremi);

        int trafficPerc = -1;
        final String trafficPercApi = getJson("perc", br.toString());
        if (trafficPercApi != null) {
            trafficPerc = Integer.parseInt(trafficPercApi);

            if (trafficPerc > 95) {
                // todays traffic limit reached...
                String lastNotification = account.getStringProperty("LAST_SPEEDLIMIT_NOTIFICATION", null);
                if (lastNotification != null) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    String today = dateFormat.format(new Date());
                    if (!today.equals(lastNotification)) {
                        // last notification is at least one day ago...
                        // save that we try to notify the user...
                        account.setProperty("LAST_SPEEDLIMIT_NOTIFICATION", today);
                        // search whether we have to notify by bubble
                        boolean bubbleNotify = this.getPluginConfig().getBooleanProperty(NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE, false);
                        if (bubbleNotify) {
                            BasicNotify no = new BasicNotify(getPhrase("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION"), getPhrase("FULLSPEED_TRAFFIC_NOTIFICATION_MSG"), NewTheme.I().getIcon("info", 32));
                            BubbleNotify.getInstance().show(no);
                        }
                        // search whether we have to notify by dialog
                        boolean dialogNotify = this.getPluginConfig().getBooleanProperty(NOTIFY_ON_FULLSPEED_LIMIT_DIALOG, false);
                        if (dialogNotify) {
                            MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, getPhrase("FULLSPEED_TRAFFIC_NOTIFICATION_MSG"));
                            try {
                                Dialog.getInstance().showDialog(dialog);
                            } catch (DialogNoAnswerException e) {
                            }
                        }
                    }
                }
            }
        }

        account.setProperty(ACC_PROPERTY_TRAFFIC_REDUCTION, trafficPerc);

        try {
            Long guthaben = Long.parseLong(getRegexTag(br.toString(), "guthaben").getMatch(0));
            ac.setTrafficLeft(guthaben * 1024 * 1024);
            logger.info("{fetchAccInfo} Limited traffic: " + guthaben * 1024 * 1024);
        } catch (Exception e) {
            logger.info("{fetchAccInfo} Unlimited traffic, api response: " + br.toString());
            ac.setUnlimitedTraffic(); // workaround
        }
        try {
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }
        String accountType = getRegexTag(br.toString(), "premium").getMatch(0);
        ac.setValidUntil(-1);
        if (accountType != null) {
            if (accountType.equalsIgnoreCase("Flatrate")) {
                logger.info("{fetchAccInfo} Flatrate Account");
                ac.setUnlimitedTraffic();
                long validUntil = Long.parseLong(getRegexTag(br.toString(), "Flatrate").getMatch(0));
                ac.setValidUntil(validUntil * 1000);
            } else if (accountType.equalsIgnoreCase("Spender")) {
                logger.info("{fetchAccInfo} Spender Account");
                ac.setUnlimitedTraffic();
            }
        }
        account.setProperty("notifications", br.getRegex("\"notis\":(\\d+)").getMatch(0));
        account.setProperty("acctype", accountType);
        // check if beta-account is enabled
        String hostsUrl = "https://www.free-way.me/ajax/jd.php?id=3";
        if (this.getPluginConfig().getBooleanProperty(BETAUSER, false)) {
            hostsUrl += "&user=" + username + "&pass=" + pass + "&encoded&beta=1";
            logger.info("{fetchAccInfo} free-way beta account enabled");
        }

        // now let's get a list of all supported hosts:
        br.getPage(hostsUrl);
        hosts = br.getRegex("\"([^\"]*)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }

        if (supportedHosts.size() == 0) {
            ac.setStatus(getPhrase("CHECK_ZERO_HOSTERS"));
        } else {
            ac.setStatus(getPhrase("SUPPORTED_HOSTS_1") + supportedHosts.size() + getPhrase("SUPPORTED_HOSTS_2"));
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private Regex getRegexTag(String content, String tag) {
        return new Regex(content, "\"" + tag + "\":\"([^\"]*)\"");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    private boolean prevetSpritUsage(Account acc) {
        boolean doPrevent = this.getPluginConfig().getBooleanProperty(PREVENTSPRITUSAGE, false);
        boolean unlimitedTraffic = acc.getAccountInfo().isUnlimitedTraffic();
        return doPrevent && !unlimitedTraffic;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        if (prevetSpritUsage(acc)) {
            // we stop if the user won't lose sprit
            acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_PREVENT_SPRIT_USAGE"));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }

        String user = Encoding.urlTotalEncode(acc.getUser());
        String pw = Encoding.urlTotalEncode(acc.getPass());
        final String url = Encoding.urlTotalEncode(link.getDownloadURL());

        logger.info("{handleMultiHost} Try download with account " + acc.getUser() + " file: " + link.getDownloadURL());

        String dllink = "https://www.free-way.me/load.php?multiget=2&user=" + user + "&pw=" + pw + "&url=" + url + "&encoded";

        // set timeout
        br.setConnectTimeout(40 * 1000);
        br.setReadTimeout(40 * 1000);

        /* Begin workaround for wrong encoding while redirect */
        br.setFollowRedirects(false);
        String page = br.getPage(dllink);
        if (page.contains("Invalid login")) {
            logger.info("{handleMultiHost} Invalid Login for account: " + acc.getUser());
            acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_INVALID_LOGIN"));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        String location = br.getRedirectLocation();
        dllink = location.substring(0, location.indexOf("?")) + dllink.substring(dllink.indexOf("?"), dllink.length()) + "&s=" + location.substring(location.length() - 1, location.length());
        /* end workaround for wrong encoding while redirect */

        boolean resume = this.getPluginConfig().getBooleanProperty(ALLOWRESUME, false);
        if (link.getBooleanProperty(FreeWayMe.NORESUME, false)) {
            resume = false;
            link.setProperty(FreeWayMe.NORESUME, Boolean.valueOf(false));
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, 1);

        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 416) {
                logger.info("Resume impossible, disabling it for the next try");
                link.setChunksProgress(null);
                link.setProperty(FreeWayMe.NORESUME, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            br.followConnection();
            String error = "";
            try {
                error = (new Regex(br.toString(), "<p id=\\'error\\'>([^<]*)</p>")).getMatch(0);
            } catch (Exception e) {
                // we handle this few lines later
            }
            if (error.contains("ltiger Login")) { // Ungü
                acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_INVALID_LOGIN"));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (error.contains("ltige URL")) { // Ungültige URL
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l, getPhrase("ERROR_INVALID_URL"));
            } else if (error.contains("Sie haben nicht genug Traffic, um diesen Download durchzuf")) { // ühren
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l, getPhrase("ERROR_TRAFFIC_LIMIT"));
            } else if (error.contains("nnen nicht mehr parallele Downloads durchf")) { // Sie kö... ...ühren
                int attempts = link.getIntegerProperty("CONNECTIONS_RETRY_COUNT", 0);
                // first attempt -> update acc information
                if (attempts == 0) acc.setUpdateTime(-1); // force update acc next try (to get new information about simultan connections)
                link.setProperty("CONNECTIONS_RETRY_COUNT", attempts + 1);
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, getPhrase("ERROR_CONNECTIONS"), (12 + 20 * attempts) * 1000l);
            } else if (error.contains("ltiger Hoster")) { // Ungü...
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l, getPhrase("ERROR_INAVLID_HOST_URL"));
            } else if (error.equalsIgnoreCase("Dieser Hoster ist aktuell leider nicht aktiv.")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l, getPhrase("ERROR_HOST_TMP_DISABLED"));
            } else if (error.equalsIgnoreCase("Diese Datei wurde nicht gefunden.")) {
                tempUnavailableHoster(acc, link, 1 * 60 * 1000l, "File not found");
            } else if (error.equalsIgnoreCase("Unbekannter Fehler #3") || error.equalsIgnoreCase("Unbekannter Fehler #2") || error.equals("Es ist ein unbekannter Fehler aufgetreten (#1)")) {
                /*
                 * "Unbekannter Fehler #3" -> free-way has internal routing problem "Es ist ein unbekannter Fehler aufgetreten (#1)" ->
                 * free-way has internally no traffic for host
                 */

                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 2) {
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000l, error);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("ERROR_RETRY_SECONDS") + msg, 20 * 1000l);
            } else if (error.startsWith("Die Datei darf maximal")) {
                logger.info("{handleMultiHost} File download limit");
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l, error);
            } else if (error.equalsIgnoreCase("Mehrere Computer haben in letzter Zeit diesen Account genutzt")) {
                logger.info("{handleMultiHost} free-way ip ban");
                acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_BAN"));
                throw new PluginException(LinkStatus.ERROR_RETRY, getPhrase("ERROR_BAN"));
            }
            logger.severe("{handleMultiHost} Unhandled download error on free-way.me: " + br.toString());
            int timesFailed = link.getIntegerProperty(ACC_PROPERTY_UNKOWN_FAILS, 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty(ACC_PROPERTY_UNKOWN_FAILS, timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, getPhrase("ERROR_SERVER"));
            } else {
                link.setProperty(ACC_PROPERTY_UNKOWN_FAILS, Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

        }
        dl.startDownload();
    }

    public void showAccountDetailsDialog(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            final Object supported = ai.getProperty("multiHostSupport", Property.NULL);
            if (supported != null) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                Set<String> unavailableHosts = new HashSet<String>();
                if (unavailableMap != null) {
                    unavailableHosts.addAll(unavailableMap.keySet());
                }
                String windowTitleLangText = null;
                ArrayList<String> supportedHosts = (ArrayList<String>) supported;
                final String accType = account.getStringProperty("acctype", null);
                final String maxSimultanDls = account.getStringProperty(ACC_PROPERTY_CONNECTIONS, null);
                final String notifications = account.getStringProperty("notifications", "0");
                final int trafficUsage = account.getIntegerProperty(ACC_PROPERTY_TRAFFIC_REDUCTION, -1);

                Set<MultihostContainer> hostList = new HashSet<MultihostContainer>();

                for (String host : supportedHosts) {
                    if (host.equals("uploaded.net") || host.equals("ul.to")) {
                        host = "uploaded.to";
                    }
                    MultihostContainer container = new MultihostContainer(host, account);
                    if (unavailableHosts != null && unavailableHosts.contains(host)) container.setIsWorking(false);
                    hostList.add(container);
                }

                /* it manages new panel */
                PanelGenerator panelGenerator = new PanelGenerator();

                JLabel hostLabel = new JLabel("<html><b>" + account.getHoster() + "</b></html>");
                hostLabel.setIcon(DomainInfo.getInstance(account.getHoster()).getFavIcon());
                panelGenerator.addLabel(hostLabel);

                String revision = "$Revision$";
                try {
                    String[] revisions = revision.split(":");
                    revision = revisions[1].replace('$', ' ').trim();
                } catch (Exception e) {
                    logger.info("free-way.me revision number error: " + e);
                }

                windowTitleLangText = getPhrase("DETAILS_TITEL");

                panelGenerator.addCategory(getPhrase("DETAILS_CATEGORY_ACC"));
                panelGenerator.addEntry(getPhrase("DETAILS_ACCOUNT_NAME"), account.getUser());
                panelGenerator.addEntry(getPhrase("DETAILS_ACCOUNT_TYPE"), accType);

                if (maxSimultanDls != null) panelGenerator.addEntry(getPhrase("DETAILS_SIMULTAN_DOWNLOADS"), maxSimultanDls);

                panelGenerator.addEntry(getPhrase("DETAILS_FULLSPEED_TRAFFIC"), (trafficUsage == -1) ? getPhrase("DETAILS_FULLSPEED_UNKOWN") : ((trafficUsage >= 100) ? getPhrase("DETAILS_FULLSPEED_REDUCED") : trafficUsage + "%"));

                panelGenerator.addEntry(getPhrase("DETAILS_NOTIFICATIONS"), notifications);

                panelGenerator.addCategory(getPhrase("DETAILS_CATEGORY_HOSTS"));
                panelGenerator.addEntry(getPhrase("DETAILS_HOSTS_AMOUNT"), Integer.toString(hostList.size()));

                panelGenerator.addTable(hostList);

                panelGenerator.addEntry(getPhrase("DETAILS_REVISION"), revision);

                ContainerDialog dialog = new ContainerDialog(UIOManager.BUTTONS_HIDE_CANCEL + UIOManager.LOGIC_COUNTDOWN, windowTitleLangText, panelGenerator.getPanel(), null, getPhrase("CLOSE"), "");
                try {
                    Dialog.getInstance().showDialog(dialog);
                } catch (DialogNoAnswerException e) {
                }
            }
        }

    }

    public class PanelGenerator {
        private JPanel panel = new JPanel();
        private int    y     = 0;

        public PanelGenerator() {
            panel.setLayout(new GridBagLayout());
            panel.setMinimumSize(new Dimension(270, 200));
        }

        public void addLabel(JLabel label) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(label, c);
            y++;
        }

        public void addCategory(String categoryName) {
            JLabel category = new JLabel("<html><u><b>" + categoryName + "</b></u></html>");

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(10, 5, 0, 5);
            panel.add(category, c);
            y++;
        }

        public void addEntry(String key, String value) {
            GridBagConstraints c = new GridBagConstraints();
            JLabel keyLabel = new JLabel(key);
            // keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD));
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0.9;
            c.gridx = 0;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(keyLabel, c);

            JLabel valueLabel = new JLabel(value);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            panel.add(valueLabel, c);

            y++;
        }

        public void addTable(Set<MultihostContainer> hostList) {
            MultihostTableModel tableModel = new MultihostTableModel();
            tableModel.addAllElements(hostList);
            MultihostTable table = new MultihostTable(tableModel);

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0.9;
            c.gridx = 0;
            c.gridy = y;
            c.insets = new Insets(1, 0, 8, 0);
            c.gridwidth = 2;
            y++;

            JScrollPane spTable = new JScrollPane(table);
            spTable.setPreferredSize(new Dimension(180, 150));
            panel.add(spTable, c);
        }

        public JPanel getPanel() {
            return panel;
        }

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout, String msg) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("ERROR_UNKNWON_CODE"));
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
            account.setProperty("unavailablemap", unavailableMap);
        }
        throw new PluginException(LinkStatus.ERROR_RETRY, msg);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
        // we stop if the user won't lose sprit
        if (prevetSpritUsage(account)) return false;

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                }
            }
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public class MultihostTable extends BasicJDTable<MultihostContainer> {

        private static final long serialVersionUID = 3954591041479889404L;

        public MultihostTable(ExtTableModel<MultihostContainer> tableModel) {
            super(tableModel);
            setSearchEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    public class MultihostTableModel extends ExtTableModel<MultihostContainer> {

        private static final long serialVersionUID = 1170104165705748962L;

        public MultihostTableModel() {
            super("multihostTable");
        }

        @Override
        protected void initColumns() {

            this.addColumn(new ExtTextColumn<MultihostContainer>("Host") {

                private static final long serialVersionUID = -8070328156326837828L;

                @Override
                protected Icon getIcon(MultihostContainer value) {
                    return value.getIcon();
                }

                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                public int getMaxWidth() {
                    return 240;
                }

                @Override
                public int getDefaultWidth() {
                    return getMinWidth();
                }

                @Override
                public int getMinWidth() {
                    return 150;
                }

                @Override
                public boolean isEditable(MultihostContainer obj) {
                    return false;
                }

                @Override
                public String getStringValue(MultihostContainer value) {
                    return value.getHost();
                }
            });

            this.addColumn(new ExtTextColumn<MultihostContainer>("Working?") {

                @Override
                protected Icon getIcon(MultihostContainer value) {
                    return value.isWorking ? NewTheme.I().getIcon("ok", 14) : NewTheme.I().getIcon("cancel", 14);
                }

                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                public int getMaxWidth() {
                    return 30;
                }

                @Override
                public int getDefaultWidth() {
                    return getMinWidth();
                }

                @Override
                public boolean isEditable(MultihostContainer obj) {
                    return false;
                }

                @Override
                public String getStringValue(MultihostContainer value) {
                    return "";
                }
            });
        }
    }

    public class MultihostContainer {

        private String  host;

        private Account account;

        private boolean isWorking = true;

        public MultihostContainer(String host, Account account) {
            this.host = host;
            this.account = account;
        }

        public void setIsWorking(boolean working) {
            this.isWorking = working;
        }

        public String getHost() {
            return host;
        }

        public Icon getIcon() {
            try {
                return DomainInfo.getInstance(host).getFavIcon();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            MultihostContainer other = (MultihostContainer) obj;
            if (!getOuterType().equals(other.getOuterType())) return false;
            if (host == null) {
                if (other.host != null) return false;
            } else if (!host.equals(other.host)) return false;
            return true;
        }

        private FreeWayMe getOuterType() {
            return FreeWayMe.this;
        }
    }
}
