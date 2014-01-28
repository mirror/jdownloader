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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

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

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FreeWayMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap             = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem                        = new AtomicInteger(1);
    private final String                                   ALLOWRESUME                    = "ALLOWRESUME";
    private final String                                   BETAUSER                       = "FREEWAYBETAUSER";
    private static final String                            NORESUME                       = "NORESUME";

    public final String                                    ACC_PROPERTY_CONNECTIONS       = "parallel";
    public final String                                    ACC_PROPERTY_TRAFFIC_REDUCTION = "ACC_TRAFFIC_REDUCTION";

    public FreeWayMe(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
        setConfigElements();
        this.enablePremium("https://www.free-way.me/premium");
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOWRESUME, "Enable resume of stopped downloads (Warning: This can cause CRC errors)").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), BETAUSER, "Enable beta service (Requires free-way beta account)").setDefaultValue(false));
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
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = Encoding.urlTotalEncode(account.getUser());
        String pass = Encoding.urlTotalEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("https://www.free-way.me/ajax/jd.php?id=1&user=" + username + "&pass=" + pass + "&encoded");
        final String lang = System.getProperty("user.language");
        // "Invalid login" / "Banned" / "Valid login"
        if (br.toString().equalsIgnoreCase("Valid login")) {
            logger.info("{fetchAccInfo} Account " + username + " is valid");
        } else if (br.toString().equalsIgnoreCase("Invalid login")) {
            account.setError(AccountError.INVALID, "Invalid login");
            logger.info("{fetchAccInfo} Account " + username + " is invalid");
            logger.info("{fetchAccInfo} Request result: " + br.toString());
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (br.toString().equalsIgnoreCase("Banned")) {
            logger.info("{fetchAccInfo} Account banned by free-way! -> advise to contact free-way support");
            logger.info("{fetchAccInfo} Request result: " + br.toString());
            account.setError(AccountError.INVALID, "Account banned");

            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account banned!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else {
            logger.severe("{fetchAccInfo} Unknown ERROR!");
            logger.severe("{fetchAccInfo} Add to error parser: " + br.toString());
            // unknown error
            account.setError(AccountError.INVALID, "Unknown error");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unbekannter Accountstatus (deaktiviert)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unknown account status (deactivated)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
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
            ac.setStatus("Account valid: 0 Hosts via free-way.me available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via free-way.me available");
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

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        String user = Encoding.urlTotalEncode(acc.getUser());
        String pw = Encoding.urlTotalEncode(acc.getPass());
        final String url = Encoding.urlTotalEncode(link.getDownloadURL());

        logger.info("{handleMultiHost} Try download with account " + acc.getUser() + " file: " + link.getDownloadURL());

        String dllink = "https://www.free-way.me/load.php?multiget=2&user=" + user + "&pw=" + pw + "&url=" + url + "&encoded";

        /* Begin workaround for wrong encoding while redirect */
        br.setFollowRedirects(false);
        String page = br.getPage(dllink);
        if (page.contains("Invalid login")) {
            logger.info("{handleMultiHost} Invalid Login for account: " + acc.getUser());
            acc.setError(AccountError.TEMP_DISABLED, "Invalid login");
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
                acc.setError(AccountError.TEMP_DISABLED, "Invalid login");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (error.contains("ltige URL")) { // Ungültige URL
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l, "Invalid URL");
            } else if (error.contains("Sie haben nicht genug Traffic, um diesen Download durchzuf")) { // ühren
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l, "Traffic limit");
            } else if (error.contains("nnen nicht mehr parallele Downloads durchf")) { // Sie kö... ...ühren
                acc.setUpdateTime(30 * 1000); // 30s
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 1 * 60 * 1000l);
            } else if (error.contains("ltiger Hoster")) { // Ungü...
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l, "Invalid host link");
            } else if (error.equalsIgnoreCase("Dieser Hoster ist aktuell leider nicht aktiv.")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l, "Host temporary disabled");
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
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Retry in few secs" + msg, 20 * 1000l);
            } else if (error.startsWith("Die Datei darf maximal")) {
                logger.info("{handleMultiHost} File download limit");
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l, error);
            } else if (error.equalsIgnoreCase("Mehrere Computer haben in letzter Zeit diesen Account genutzt")) {
                logger.info("{handleMultiHost} free-way ip ban");
                acc.setError(AccountError.TEMP_DISABLED, "IP ban");
                throw new PluginException(LinkStatus.ERROR_RETRY, "IP ban");
            }
            logger.severe("{handleMultiHost} Unhandled download error on free-way.me: " + br.toString());
            int timesFailed = link.getIntegerProperty("timesfailedfreewayme_unknown", 0);
            if (timesFailed <= 2) {
                timesFailed++;
                link.setProperty("timesfailedfreewayme_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            } else {
                link.setProperty("timesfailedfreewayme_unknown", Property.NULL);
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
                final String lang = System.getProperty("user.language");
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

                if ("de".equalsIgnoreCase(lang)) {
                    windowTitleLangText = "Account Zusatzinformationen";

                    panelGenerator.addCategory("Account");
                    panelGenerator.addEntry("Account Name:", account.getUser());
                    panelGenerator.addEntry("Account Typ:", accType);

                    if (maxSimultanDls != null) panelGenerator.addEntry("Gleichzeitige Downloads:", maxSimultanDls);

                    panelGenerator.addEntry("Fullspeedvolumen verbraucht:", (trafficUsage == -1) ? "unbekannt" : ((trafficUsage >= 100) ? "gedrosselt" : trafficUsage + "%"));

                    panelGenerator.addEntry("Benachrichtigungen:", notifications);

                    panelGenerator.addCategory("Unterstützte Hoster");
                    panelGenerator.addEntry("Anzahl: ", Integer.toString(hostList.size()));

                } else {
                    windowTitleLangText = "Account Information";

                    panelGenerator.addCategory("Account");
                    panelGenerator.addEntry("Account Name:", account.getUser());
                    panelGenerator.addEntry("Account Type:", accType);

                    if (maxSimultanDls != null) panelGenerator.addEntry("Simultaneous Downloads:", maxSimultanDls);

                    panelGenerator.addEntry("Fullspeed traffic:", (trafficUsage == -1) ? "unknown" : ((trafficUsage >= 100) ? "reduced" : trafficUsage + "%"));

                    panelGenerator.addEntry("Notifications:", notifications);

                    panelGenerator.addCategory("Supported Hosts");
                    panelGenerator.addEntry("Amount: ", Integer.toString(hostList.size()));

                }
                panelGenerator.addTable(hostList);

                panelGenerator.addEntry("Plugin Revision:", revision);

                ContainerDialog dialog = new ContainerDialog(UIOManager.BUTTONS_HIDE_CANCEL + UIOManager.LOGIC_COUNTDOWN, windowTitleLangText, panelGenerator.getPanel(), null, "Close", "");
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

    private ArrayList<String> getDisabledHosts(Account account) {
        final Object disabledHostsObject = account.getAccountInfo().getProperty("disabledHosts", Property.NULL);
        if (disabledHostsObject.equals(Property.NULL)) return new ArrayList<String>();
        return (ArrayList<String>) disabledHostsObject;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout, String msg) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
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
        if (getDisabledHosts(account).contains(downloadLink.getHost())) return false;
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

            this.addColumn(new ExtCheckColumn<MultihostContainer>(_GUI._.premiumaccounttablemodel_column_enabled()) {

                private static final long serialVersionUID = 1515656228974789237L;

                public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                    final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                        private static final long serialVersionUID = 3224931991570756349L;

                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            setIcon(NewTheme.I().getIcon("ok", 14));
                            setHorizontalAlignment(CENTER);
                            setText(null);
                            return this;
                        }

                    };

                    return ret;
                }

                @Override
                public int getMaxWidth() {
                    return 30;
                }

                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                protected boolean getBooleanValue(MultihostContainer value) {
                    return value.isEnabled();
                }

                @Override
                public boolean isEditable(MultihostContainer obj) {
                    return true;
                }

                @Override
                protected void setBooleanValue(boolean value, final MultihostContainer container) {
                    if (value) {
                        container.enable();
                    } else {
                        container.disable();
                    }
                }
            });

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

        public void enable() {
            ArrayList<String> disabledHosts = getDisabledHosts();
            if (disabledHosts.contains(host)) {
                disabledHosts.remove(host);
                account.getAccountInfo().setProperty("disabledHosts", disabledHosts);
            }
        }

        public void disable() {
            ArrayList<String> disabledHosts = getDisabledHosts();
            if (!disabledHosts.contains(host)) {
                disabledHosts.add(host);
                account.getAccountInfo().setProperty("disabledHosts", disabledHosts);
            }
        }

        public boolean isEnabled() {
            return !getDisabledHosts().contains(host);
        }

        private ArrayList<String> getDisabledHosts() {
            final Object disabledHostsObject = account.getAccountInfo().getProperty("disabledHosts", Property.NULL);
            ArrayList<String> disabledHosts;
            if (disabledHostsObject.equals(Property.NULL)) return new ArrayList<String>();
            return (ArrayList<String>) disabledHostsObject;
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