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
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.nutils.JDHash;
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
import jd.utils.locale.JDL;

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
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.NewTheme;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FreeWayMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap                  = new HashMap<Account, HashMap<String, Long>>();
    private final String                                   ALLOWRESUME                         = "ALLOWRESUME";
    private final String                                   BETAUSER                            = "FREEWAYBETAUSER";

    private final String                                   NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE    = "NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE";
    private final String                                   NOTIFY_ON_FULLSPEED_LIMIT_DIALOG    = "NOTIFY_ON_FULLSPEED_LIMIT_DIALOG";

    private final String                                   SETTING_2FA_ALIAS                   = "SETTING_2FA_ALIAS";
    private final String                                   SETTING_SHOW_TRAFFICLEFT            = "SETTING_SHOW_TRAFFICLEFT";

    private static final String                            NORESUME                            = "NORESUME";
    private static final String                            PREVENTSPRITUSAGE                   = "PREVENTSPRITUSAGE";
    private static final String                            MAX_RETRIES_UNKNOWN_ERROR           = "MAX_RETRIES_UNKNOWN_ERROR";
    private static final long                              max_retries_unknown_error_default   = 10;
    private static final long                              traffic_max_static                  = 5 * 1024 * 1024 * 1024l;

    public final String                                    ACC_PROPERTY_CONNECTIONS            = "parallel";
    public final String                                    ACC_PROPERTY_TRAFFIC_REDUCTION      = "ACC_TRAFFIC_REDUCTION";
    public final String                                    ACC_PROPERTY_DROSSEL_ACTIVE         = "ACC_PROPERTY_DROSSEL_ACTIVE";
    public final String                                    ACC_PROPERTY_REST_FULLSPEED_TRAFFIC = "ACC_PROPERTY_REST_FULLSPEED_TRAFFIC";
    public final String                                    ACC_PROPERTY_UNKOWN_FAILS           = "timesfailedfreewayme_unknown";
    public final String                                    ACC_PROPERTY_CURL_FAIL_RESOLVE_HOST = "timesfailedfreewayme_curl_resolve_host";

    /**
     * @author flubshi
     * */
    public FreeWayMe(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
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
                                                      put("ERROR_NO_STABLE_ACCOUNTS", "Found no stable accounts");
                                                      put("SUPPORTED_HOSTS_1", "Account valid");
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
                                                      put("DETAILS_FULLSPEED_TRAFFIC", "Fullspeed traffic used:");
                                                      put("DETAILS_FULLSPEED_REST_TRAFFIC", "Fullspeed traffic left:");
                                                      put("DETAILS_FULLSPEED_UNKOWN", "unknown");
                                                      put("DETAILS_FULLSPEED_REDUCED", "reduced");
                                                      put("DETAILS_NOTIFICATIONS", "Notifications:");
                                                      put("DETAILS_CATEGORY_HOSTS", "Supported Hosts");
                                                      put("DETAILS_HOSTS_AMOUNT", "Amount: ");
                                                      put("DETAILS_REVISION", "Plugin Revision:");
                                                      put("CLOSE", "Close");
                                                      put("ERROR_PREVENT_SPRIT_USAGE", "Sprit usage prevented!");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION", "Fullspeedlimit");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE", "Show bubble notification if fullspeed limit is reached");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG", "Show dialog notification if fullspeed limit is reached");
                                                      put("SETTING_MAXRETRIES_UNKNOWN_ERROR", "Max retries on unknown errors");
                                                      put("SETTING_2FA_ALIAS", "Device name (Two-Factor Authentication)");
                                                      put("POPUP_2FA_TITLE", "Free-Way 2-Factor Authentication");
                                                      put("POPUP_2FA_DESCRIPTION", "Please authenticate this device or disable 2-factor authentication on free-way.me\n\n" + "Device: ");
                                                      put("SETTING_SHOW_TRAFFICLEFT", "Show remaining fullspeed traffic as 'traffic left'?\r\nNOTE: In case you have less than 10 GB fullspeed traffic left, it will be shown as 'Unlimited' again due to technical reasons!");
                                                      put("SETTINGSTEXT_SETTINGS_DOWNLOAD", "Download settings:");
                                                      put("SETTINGSTEXT_SETTINGS_GUI", "User interface settings:");
                                                      put("SETTINGSTEXT_SETTINGS_ACCOUNT", "Advanced account settings:");
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
                                                      put("ERROR_NO_STABLE_ACCOUNTS", "Keine stabilen Accounts verfügbar");
                                                      put("SUPPORTED_HOSTS_1", "Account gültig");
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
                                                      put("DETAILS_FULLSPEED_REST_TRAFFIC", "Restliches Fullspeedvolumen:");
                                                      put("DETAILS_FULLSPEED_UNKOWN", "unbekannt");
                                                      put("DETAILS_FULLSPEED_REDUCED", "gedrosselt");
                                                      put("DETAILS_NOTIFICATIONS", "Benachrichtigungen:");
                                                      put("DETAILS_CATEGORY_HOSTS", "Unterstützte Hoster");
                                                      put("DETAILS_HOSTS_AMOUNT", "Anzahl: ");
                                                      put("DETAILS_REVISION", "Plugin Revision:");
                                                      put("CLOSE", "Schließen");
                                                      put("ERROR_PREVENT_SPRIT_USAGE", "Spritverbrauch verhindert!");
                                                      put("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION", "Fullspeed-Limit");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE", "Zeige Bubble-Benachrichtigung wenn das Fullspeedlimit ausgeschöpft ist");
                                                      put("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG", "Zeige Dialog-Benachrichtigung wenn das Fullspeedlimit ausgeschöpft ist");
                                                      put("SETTING_MAXRETRIES_UNKNOWN_ERROR", "Maximale Neuversuche bei unbekannten Fehlerfällen");
                                                      put("SETTING_2FA_ALIAS", "Gerätename (Zwei-Faktor Authentifizierung)");
                                                      put("POPUP_2FA_TITLE", "Free-Way 2-Faktor Authentifizierung");
                                                      put("POPUP_2FA_DESCRIPTION", "Bitte autorisiere das folgende Gerät oder deaktiviere die 2-Faktor Authent-\n" + "entifizierung auf der Free-Way Seite.\n\nGerät: ");
                                                      put("SETTING_SHOW_TRAFFICLEFT", "Zeige verbleibendes Fullspeedvolumen in der Accountübersicht bei 'Downloadtraffic übrig'?\r\nWICHTIG: Solltest du weniger als 10 GB Fullspeedvolumen haben, wird aus technischen Gründen wieder 'Unlimitiert' angezeigt!");
                                                      put("SETTINGSTEXT_SETTINGS_DOWNLOAD", "Downloadeinstellungen:");
                                                      put("SETTINGSTEXT_SETTINGS_GUI", "Benutzeroberflächen-Einstellungen:");
                                                      put("SETTINGSTEXT_SETTINGS_ACCOUNT", "Erweiterte Account Einstellungen:");
                                                  }
                                              };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     * 
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    /**
     * Gets a unique device id for 2FA (fallback: device alias)
     * 
     * @return
     */
    private String get2FADevID() {
        String nInterfaceName = this.getPluginConfig().getStringProperty("FW_2FA_DEV_IF", "");

        String devID = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nI = interfaces.nextElement();
                if (nI == null) {
                    continue;
                }
                if (devID == null || nI.getName().equals(nInterfaceName)) {
                    if (devID == null) {
                        this.getPluginConfig().setProperty("FW_2FA_DEV_IF", nI.getName());
                    }
                    devID = "JD" + JDHash.getSHA1(Arrays.toString(nI.getHardwareAddress()));
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return devID == null ? this.getPluginConfig().getStringProperty(SETTING_2FA_ALIAS) : devID;
    }

    /**
     * gets a page and shows 2FA input if necessary
     * 
     * @param url
     * @return
     * @throws IOException
     * @throws PluginException
     */
    private String getPage2FA(String url) throws IOException, PluginException {
        String twoFactorDeviceID = get2FADevID();
        br.getHeaders().put("User-Agent", twoFactorDeviceID);
        String page = br.getPage(url);
        if (page.contains("Advanced authentification needed")) {
            return getPageForce2FA(url);
        }
        return page;
    }

    private String getPageForce2FA(String url) throws PluginException, IOException {
        String twoFactorDeviceID = get2FADevID();
        br.getHeaders().put("User-Agent", twoFactorDeviceID);
        String alias = this.getPluginConfig().getStringProperty(SETTING_2FA_ALIAS);
        // page require 2FA
        String authcode = JOptionPane.showInputDialog(null, getPhrase("POPUP_2FA_DESCRIPTION") + alias, getPhrase("POPUP_2FA_TITLE"), JOptionPane.PLAIN_MESSAGE);

        // If a string was returned, say so.
        if (authcode == null || authcode.length() <= 0) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM);
        }
        String page = br.getPage(url + "&2fa_action=login&2fa_alias=" + Encoding.urlTotalEncode(alias) + "&2fa_auth=" + Encoding.urlTotalEncode(authcode));
        if (page.contains("auth invalid")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM);
        }
        return page;
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTINGSTEXT_SETTINGS_DOWNLOAD")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOWRESUME, getPhrase("SETTING_RESUME")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREVENTSPRITUSAGE, getPhrase("SETTING_SPRITUSAGE")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTINGSTEXT_SETTINGS_GUI")));
        /* settings for notification on empty fullspeed traffic */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE, getPhrase("SETTINGS_FULLSPEED_NOTIFICATION_BUBBLE")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NOTIFY_ON_FULLSPEED_LIMIT_DIALOG, getPhrase("SETTINGS_FULLSPEED_NOTIFICATION_DIALOG")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_SHOW_TRAFFICLEFT, getPhrase("SETTING_SHOW_TRAFFICLEFT")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTINGSTEXT_SETTINGS_ACCOUNT")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), BETAUSER, getPhrase("SETTING_BETA")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), FreeWayMe.MAX_RETRIES_UNKNOWN_ERROR, JDL.L("plugins.hoster.FreeWayMe.maxRetriesOnUnknownErrors", getPhrase("SETTING_MAXRETRIES_UNKNOWN_ERROR")), 3, 50, 1).setDefaultValue(max_retries_unknown_error_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), SETTING_2FA_ALIAS, getPhrase("SETTING_2FA_ALIAS")).setDefaultValue("JDownloader - " + System.getProperty("user.name")));
    }

    @Override
    public String getAGBLink() {
        return "https://www.free-way.me/agb";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null) {
            boolean drosselActive = account.getBooleanProperty(ACC_PROPERTY_DROSSEL_ACTIVE, false);
            int connections = account.getIntegerProperty(ACC_PROPERTY_CONNECTIONS, 1);

            if (!drosselActive) {
                return connections;
            }
            // else it is limited
            if (link != null) {
                List<String> limitedHosts = Arrays.asList("uploaded.to", "ul.to", "uploaded.net", "share-online.biz", "freakshare.com", "datei.to", "uploading.com", "rapidgator.net", "oboom.com", "filepost.com", "depositfiles.com", "keep2share.cc", "k2c.cc", "k2share.cc");

                if (limitedHosts.contains(link.getHost().toLowerCase(Locale.ENGLISH))) {
                    return Math.min(connections, 2);
                }
            }
            // or not a limited host
            return connections;
        }
        return 0;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        logger.info("{fetchAccInfo} Update free-way account: " + account.getUser());
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(70 * 1000);
        br.setReadTimeout(65 * 1000);
        String username = Encoding.urlTotalEncode(account.getUser().trim());
        String pass = Encoding.urlTotalEncode(account.getPass().trim());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        getPage2FA("https://www.free-way.me/ajax/jd.php?id=1&user=" + username + "&pass=" + pass + "&encoded");
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
        String accInfoAPIResp = getPage2FA("https://www.free-way.me/ajax/jd.php?id=4&user=" + username + "&pass=" + pass + "&encoded");

        int maxPremi = 1;
        final String maxPremApi = getJson(accInfoAPIResp, "parallel");
        if (maxPremApi != null) {
            maxPremi = Integer.parseInt(maxPremApi);
        }
        account.setProperty(ACC_PROPERTY_CONNECTIONS, maxPremi);

        // get available fullspeed traffic
        account.setProperty(ACC_PROPERTY_REST_FULLSPEED_TRAFFIC, this.getJson(accInfoAPIResp, "restgb"));

        // get percentage usage of fullspeed traffic
        float trafficPerc = -1f;
        final String trafficPercApi = getJson(accInfoAPIResp, "perc");
        if (trafficPercApi != null) {
            trafficPerc = Float.parseFloat(trafficPercApi);

            if (trafficPerc > 95.0f) {
                // todays traffic limit reached...
                account.setProperty(ACC_PROPERTY_DROSSEL_ACTIVE, Boolean.TRUE);
                String lastNotification = account.getStringProperty("LAST_SPEEDLIMIT_NOTIFICATION", "default");
                if (lastNotification != null) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    String today = dateFormat.format(new Date());
                    if (!today.equals(lastNotification)) {
                        // last notification is at least one day ago...
                        // save that we try to notify the user...
                        account.setProperty("LAST_SPEEDLIMIT_NOTIFICATION", today);
                        // search whether we have to notify by bubble
                        boolean bubbleNotify = this.getPluginConfig().getBooleanProperty(NOTIFY_ON_FULLSPEED_LIMIT_BUBBLE, false);
                        // search whether we have to notify by dialog
                        boolean dialogNotify = this.getPluginConfig().getBooleanProperty(NOTIFY_ON_FULLSPEED_LIMIT_DIALOG, true);

                        if (bubbleNotify) {
                            /**
                             * we get the msg twice (see below), because we need it final. A conditional get in this case is not possible.
                             * But it shouldn't matter, because no one should enable dialog- and bubblenotify...
                             */
                            final String msg = br.getPage("https://www.free-way.me/ajax/jd.php?id=8");
                            BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {

                                @Override
                                public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                                    return new BasicNotify(getPhrase("FULLSPEED_TRAFFIC_NOTIFICATION_CAPTION"), msg, NewTheme.I().getIcon("info", 32));
                                }
                            });
                        }

                        if (dialogNotify) {
                            final String msg = br.getPage("https://www.free-way.me/ajax/jd.php?id=8");
                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, msg);
                                    try {
                                        Dialog.getInstance().showDialog(dialog);
                                    } catch (DialogNoAnswerException e) {
                                    }
                                }
                            });
                            t.start();
                        }
                    }
                }
            } else {
                // 'drossel' not active
                account.setProperty(ACC_PROPERTY_DROSSEL_ACTIVE, Boolean.FALSE);
            }
        }
        account.setProperty(ACC_PROPERTY_TRAFFIC_REDUCTION, ((int) trafficPerc * 100));
        account.setConcurrentUsePossible(true);

        final String accountType = getRegexTag(accInfoAPIResp, "premium").getMatch(0);
        final double remaining_gb = Double.parseDouble(this.getJson(accInfoAPIResp, "restgb"));
        if (accountType.equalsIgnoreCase("Flatrate") || accountType.equalsIgnoreCase("Spender")) {
            logger.info("{fetchAccInfo} Flatrate Account");
            /* TODO: Remove if statement in case this works fine for Spender-Accounts */
            final String expireDate_str = getRegexTag(accInfoAPIResp, "Flatrate").getMatch(0);
            if (expireDate_str != null) {
                long validUntil = Long.parseLong(getRegexTag(accInfoAPIResp, "Flatrate").getMatch(0));
                ac.setValidUntil(validUntil * 1000);
            }
            /* Obey users' setting */
            if (this.getPluginConfig().getBooleanProperty(this.SETTING_SHOW_TRAFFICLEFT, false) && remaining_gb > 10) {
                logger.info("User has traffic_left in GUI ACTIVE");
                /* TODO: Use (upcoming) API response for this to make it dynamic */
                ac.setTrafficMax(Long.parseLong(getJson(accInfoAPIResp, "drossel-max")) * 1024 * 1024 * 1024);
                ac.setTrafficLeft((long) remaining_gb * 1024 * 1024 * 1024);
            } else {
                logger.info("User has traffic_left in GUI IN_ACTIVE");
                ac.setUnlimitedTraffic();
            }
        } else if (accountType.equals("Free")) {
            logger.info("{fetchAccInfo} Free Account");
            /* Free accounts have a normal trafficlimit - once the traffic is gone, there is no way to continue downloading via free account */
            final long traffic_left_free = Long.parseLong(getJson(accInfoAPIResp, "guthaben")) * 1024 * 1024l;
            ac.setTrafficLeft(Long.parseLong(getJson(accInfoAPIResp, "guthaben")) * 1024 * 1024);
            /* TODO: Ask for a better API-way to get the traffic-max for free accounts */
            if (traffic_left_free <= traffic_max_static) {
                ac.setTrafficMax(traffic_max_static);
            }
        }
        account.setProperty("notifications", (new Regex(accInfoAPIResp, "\"notis\":(\\d+)")).getMatch(0));
        account.setProperty("acctype", accountType);
        // check if beta-account is enabled
        String hostsUrl = "https://www.free-way.me/ajax/jd.php?id=3";
        if (this.getPluginConfig().getBooleanProperty(BETAUSER, false)) {
            hostsUrl += "&user=" + username + "&pass=" + pass + "&encoded&beta=1";
            logger.info("{fetchAccInfo} free-way beta account enabled");
        }

        getPage2FA(hostsUrl);
        hosts = br.getRegex("\"([^\"]*)\"").getColumn(0);
        ArrayList<String> supportedHosts = new ArrayList<String>();
        for (String host : hosts) {
            if (!host.isEmpty()) {
                supportedHosts.add(host.trim());
            }
        }
        // set
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus(accountType + " Account");
        return ac;
    }

    private String getJson(final String source, final String parameter) {
        String result = new Regex(source, "\"" + parameter + "\":([0-9\\.]+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
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

        String user = Encoding.urlTotalEncode(acc.getUser().trim());
        String pw = Encoding.urlTotalEncode(acc.getPass().trim());
        String dlPw = link.getDownloadPassword();
        if (dlPw != null) {
            dlPw = Encoding.urlTotalEncode(dlPw);
        } else {
            dlPw = "";
        }
        final String url = Encoding.urlTotalEncode(link.getDownloadURL());

        logger.info("{handleMultiHost} Try download with account " + acc.getUser() + " file: " + link.getDownloadURL());

        String dllink = "https://www.free-way.me/load.php?multiget=2&user=" + user + "&pw=" + pw + "&dl_pw=" + dlPw + "&url=" + url + "&encodedJD";

        if (this.getPluginConfig().getBooleanProperty(BETAUSER, false)) {
            dllink += "&beta=1";
        }

        // set timeout
        br.setConnectTimeout(100 * 1000);
        br.setReadTimeout(95 * 1000);

        br.setFollowRedirects(false);
        String page = getPage2FA(dllink);
        if (page.contains("Invalid login")) {
            logger.info("{handleMultiHost} Invalid Login for account: " + acc.getUser());
            acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_INVALID_LOGIN"));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dllink = br.getRedirectLocation();

        if (dllink == null) {
            handleError(acc, link);
            // if above error handling fails... ie unknown error
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

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
            handleError(acc, link);
            logger.severe("{handleMultiHost} Unhandled download error on free-way.me: " + br.toString());
            errorMagic(acc, link, "unknown_dl_error", this.getPluginConfig().getLongProperty(MAX_RETRIES_UNKNOWN_ERROR, max_retries_unknown_error_default));

        }
        dl.startDownload();
    }

    private void handleError(final Account acc, final DownloadLink link) throws PluginException {
        String error = "";
        try {
            error = (new Regex(br.toString(), "<p id='error'>([^<]*)</p>")).getMatch(0);
        } catch (Exception e) {
            // we handle this few lines later
        }
        if (error == null) {
            errorMagic(acc, link, getPhrase("ERROR_SERVER"), 5);
        } else if (error.contains("Download password required")) {
            // file requires pw to download
            String newDLpw = getUserInput(null, link);
            link.setDownloadPassword(newDLpw);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (error.contains("No more traffic for this host")) {
            // this happens if there is a limit for a spcific host of this multi host
            tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l, error);
        } else if (error.contains("ltiger Login")) { // Ungü
            acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_INVALID_LOGIN"));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (error.contains("ltige URL")) { // Ungültige URL
            tempUnavailableHoster(acc, link, 1 * 60 * 1000l, getPhrase("ERROR_INVALID_URL"));
        } else if (error.contains("File offline.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (error.contains("Sie haben nicht genug Traffic, um diesen Download durchzuf")) { // ühren
            acc.setUpdateTime(-1);
            tempUnavailableHoster(acc, link, 10 * 60 * 1000l, getPhrase("ERROR_TRAFFIC_LIMIT"));
        } else if (error.contains("nnen nicht mehr parallele Downloads durchf")) { // Sie kö... ...ühren
            int attempts = link.getIntegerProperty("CONNECTIONS_RETRY_COUNT_PARALLEL", 0);
            // first attempt -> update acc information
            if (attempts == 0) {
                acc.setUpdateTime(-1); // force update acc next try (to get new information about simultan connections)
            }
            link.setProperty("CONNECTIONS_RETRY_COUNT_PARALLEL", attempts + 1);
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, getPhrase("ERROR_CONNECTIONS"), (12 + 20 * attempts) * 1000l);
        } else if (error.contains("ltiger Hoster")) { // Ungü...
            tempUnavailableHoster(acc, link, 8 * 60 * 1000l, getPhrase("ERROR_INAVLID_HOST_URL"));
        } else if (error.equalsIgnoreCase("Dieser Hoster ist aktuell leider nicht aktiv.")) {
            tempUnavailableHoster(acc, link, 8 * 60 * 1000l, getPhrase("ERROR_HOST_TMP_DISABLED"));
        } else if (error.equalsIgnoreCase("Diese Datei wurde nicht gefunden.")) {
            tempUnavailableHoster(acc, link, 1 * 60 * 1000l, "File not found");
        } else if (error.equalsIgnoreCase("Es ist ein unbekannter Fehler aufgetreten (#1)") //
                || error.equalsIgnoreCase("Unbekannter Fehler #2") //
                || error.equalsIgnoreCase("Unbekannter Fehler #3") //
                || error.equalsIgnoreCase("Unbekannter Fehler #5") // internal
        ) {
            /*
             * after x retries we disable this host and retry with normal plugin
             */
            //
            int attempts = link.getIntegerProperty("CONNECTIONS_RETRY_COUNT", 0);
            if (attempts >= 3) {
                /* reset retrycounter */
                link.setProperty("CONNECTIONS_RETRY_COUNT", 0);
                tempUnavailableHoster(acc, link, 4 * 60 * 1000l, error);
            }
            link.setProperty("CONNECTIONS_RETRY_COUNT", attempts + 1);
            String msg = "(" + (attempts + 1) + "/ 3)";
            throw new PluginException(LinkStatus.ERROR_RETRY, getPhrase("ERROR_RETRY_SECONDS") + msg, 15 * 1000l);
        } else if (error.startsWith("Die Datei darf maximal")) {
            logger.info("{handleMultiHost} File download limit");
            tempUnavailableHoster(acc, link, 2 * 60 * 1000l, error);
        } else if (error.equalsIgnoreCase("Mehrere Computer haben in letzter Zeit diesen Account genutzt")) {
            logger.info("{handleMultiHost} free-way ip ban");
            acc.setError(AccountError.TEMP_DISABLED, getPhrase("ERROR_BAN"));
            throw new PluginException(LinkStatus.ERROR_RETRY, getPhrase("ERROR_BAN"), 16 * 60 * 1000l);
        } else if (br.containsHTML(">Interner Fehler bei Findung eines stabilen Accounts<")) {
            /*
             * after x retries we disable this host and retry with normal plugin --> This error means that the free-way system cannot find
             * any working accounts for the current host at the moment
             */
            int attempts = link.getIntegerProperty("CONNECTIONS_RETRY_COUNT", 0);
            if (attempts >= 3) {
                /* reset retrycounter */
                link.setProperty("CONNECTIONS_RETRY_COUNT", 0);
                logger.info(getPhrase("ERROR_NO_STABLE_ACCOUNTS") + " --> Disabling current host for 15 minutes");
                tempUnavailableHoster(acc, link, 5 * 60 * 1000l, getPhrase("ERROR_NO_STABLE_ACCOUNTS"));
            }
            link.setProperty("CONNECTIONS_RETRY_COUNT", attempts + 1);
            String msg = "(" + (attempts + 1) + "/ 3  )";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, getPhrase("ERROR_NO_STABLE_ACCOUNTS") + msg);
        } else if (br.containsHTML("cURL-Error: Couldn't resolve host")) {
            errorMagic(acc, link, getPhrase("ERROR_SERVER"), 10);
        } else if (br.containsHTML("Advanced authentification needed")) {
            // 2FA auth required => do it during acc check
            acc.setUpdateTime(-1); // force update acc next try
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private void errorMagic(final Account acc, final DownloadLink link, final String error, final long maxretries) throws PluginException {
        int timesFailed = link.getIntegerProperty(ACC_PROPERTY_UNKOWN_FAILS, 0);
        if (timesFailed <= maxretries) {
            timesFailed++;
            link.setProperty(ACC_PROPERTY_CURL_FAIL_RESOLVE_HOST, timesFailed);
            logger.info(error + ":  -> Retrying");
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            link.setProperty(ACC_PROPERTY_UNKOWN_FAILS, Property.NULL);
            logger.info(error + ":  -> Disabling current host");
            tempUnavailableHoster(acc, link, 60 * 60 * 1000l, error);
        }
    }

    @SuppressWarnings({ "unused" })
    public void showAccountDetailsDialog(final Account account) {
        final AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            List<String> supportedHosts = ai.getMultiHostSupport();
            if (supportedHosts != null) {
                final HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                Set<String> unavailableHosts = new HashSet<String>();
                if (unavailableMap != null) {
                    unavailableHosts.addAll(unavailableMap.keySet());
                }
                String windowTitleLangText = null;

                final String accType = account.getStringProperty("acctype", null);
                final Integer maxSimultanDls = account.getIntegerProperty(ACC_PROPERTY_CONNECTIONS, 1);
                final String notifications = account.getStringProperty("notifications", "0");
                final float trafficUsage = account.getIntegerProperty(ACC_PROPERTY_TRAFFIC_REDUCTION, -1) / 100f;
                String restFullspeedTraffic = account.getStringProperty(ACC_PROPERTY_REST_FULLSPEED_TRAFFIC, "?");

                /* Make sure to show the correct fullspeed trafficleft for Free accounts */
                if (accType.equals("Free") && !restFullspeedTraffic.equals("?")) {
                    final long fullspeedtraffic_long = (long) Double.parseDouble(restFullspeedTraffic) * 1024 * 1024 * 1024l;
                    final long real_traffic = account.getAccountInfo().getTrafficLeft();
                    /*
                     * Show real traffic as fullspeed traffic if we got more fullspeedtraffic than real traffic, else show fullspeed
                     * traffic.
                     */
                    if (fullspeedtraffic_long >= real_traffic) {
                        restFullspeedTraffic = Double.toString((double) real_traffic / 1024 / 1024 / 1024);
                        restFullspeedTraffic = String.format("%.2f", (double) real_traffic / 1024 / 1024 / 1024);
                        restFullspeedTraffic.replace(",", ".");
                    }
                }

                Set<MultihostContainer> hostList = new HashSet<MultihostContainer>();

                for (String host : supportedHosts) {
                    if (host.equals("uploaded.net") || host.equals("ul.to")) {
                        host = "uploaded.to";
                    }
                    MultihostContainer container = new MultihostContainer(host, account);
                    if (unavailableHosts != null && unavailableHosts.contains(host)) {
                        container.setIsWorking(false);
                    }
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

                panelGenerator.addEntry(getPhrase("DETAILS_SIMULTAN_DOWNLOADS"), maxSimultanDls.toString());

                panelGenerator.addEntry(getPhrase("DETAILS_FULLSPEED_TRAFFIC"), (trafficUsage == -1) ? getPhrase("DETAILS_FULLSPEED_UNKOWN") : ((trafficUsage >= 100) ? getPhrase("DETAILS_FULLSPEED_REDUCED") : trafficUsage + "%"));

                panelGenerator.addEntry(getPhrase("DETAILS_FULLSPEED_REST_TRAFFIC"), restFullspeedTraffic + " GB");

                // panelGenerator.addEntry(getPhrase("DETAILS_NOTIFICATIONS"), notifications);

                panelGenerator.addCategory(getPhrase("DETAILS_CATEGORY_HOSTS"));
                panelGenerator.addEntry(getPhrase("DETAILS_HOSTS_AMOUNT"), Integer.toString(hostList.size()));

                panelGenerator.addTable(hostList);
                // TO-DO: Add log...
                // panelGenerator.addCategory("Error Log");
                // JTextArea tf = new JTextArea("Test\n123\n Test");
                // panelGenerator.addTextField(tf);

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

        public void addTextField(JTextArea textfield) {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridwidth = 2;
            c.gridy = y;
            c.insets = new Insets(0, 5, 0, 5);
            panel.add(textfield, c);
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
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, getPhrase("ERROR_UNKNWON_CODE"));
        }
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
        if (prevetSpritUsage(account)) {
            return false;
        }

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    return false;
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(downloadLink.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
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
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MultihostContainer other = (MultihostContainer) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (host == null) {
                if (other.host != null) {
                    return false;
                }
            } else if (!host.equals(other.host)) {
                return false;
            }
            return true;
        }

        private FreeWayMe getOuterType() {
            return FreeWayMe.this;
        }
    }
}
