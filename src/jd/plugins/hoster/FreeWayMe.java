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
import javax.swing.ImageIcon;
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
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ContainerDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-way.me" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class FreeWayMe extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static AtomicInteger                           maxPrem            = new AtomicInteger(1);
    // we can switch to BETAENCODING next days if there are no problems, otherwise we have to switch back
    private final String                                   USEBETAENCODING    = "USEBETAENCODING";

    public FreeWayMe(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
        // setConfigElements();
        this.enablePremium("https://www.free-way.me/premium");
    }

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USEBETAENCODING, "Use beta encoding").setDefaultValue(false));
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
        boolean betaEncoding = true;// this.getPluginConfig().getBooleanProperty(USEBETAENCODING, false);

        AccountInfo ac = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        String username = (betaEncoding) ? Encoding.urlTotalEncode(account.getUser()) : Encoding.urlEncode(account.getUser());
        String pass = (betaEncoding) ? Encoding.urlTotalEncode(account.getPass()) : Encoding.urlEncode(account.getPass());
        String hosts[] = null;
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("https://www.free-way.me/ajax/jd.php?id=1&user=" + username + "&pass=" + pass + ((betaEncoding) ? "&encoded" : ""));
        // "Invalid login" / "Banned" / "Valid login"
        if (br.toString().equalsIgnoreCase("Valid login")) {
            account.setValid(true);
        } else if (br.toString().equalsIgnoreCase("Invalid login")) {
            account.setValid(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nFalscher Benutzername/Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (br.toString().equalsIgnoreCase("Banned")) {
            account.setValid(false);
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount banned!\r\nAccount gesperrt!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            // unknown error
            account.setValid(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnknown account status (deactivated)!\r\nUnbekannter Accountstatus (deaktiviert)!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        // account should be valid now, let's get account information:
        br.getPage("https://www.free-way.me/ajax/jd.php?id=4&user=" + username + "&pass=" + pass + ((betaEncoding) ? "&encoded" : ""));

        int maxPremi = 1;
        final String maxPremApi = getJson("parallel", br.toString());
        if (maxPremApi != null) {
            maxPremi = Integer.parseInt(maxPremApi);
            account.setProperty("parallel", maxPremApi);
        }
        maxPrem.set(maxPremi);
        try {
            Long guthaben = Long.parseLong(getRegexTag(br.toString(), "guthaben").getMatch(0));
            ac.setTrafficLeft(guthaben * 1024 * 1024);
        } catch (Exception e) {
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
                ac.setUnlimitedTraffic();
                long validUntil = Long.parseLong(getRegexTag(br.toString(), "Flatrate").getMatch(0));
                ac.setValidUntil(validUntil * 1000);
            } else if (accountType.equalsIgnoreCase("Spender")) {
                ac.setUnlimitedTraffic();
            }
        }
        ac.setProperty("acctype", accountType);
        // now let's get a list of all supported hosts:
        br.getPage("https://www.free-way.me/ajax/jd.php?id=3");
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
        boolean betaEncoding = true;// this.getPluginConfig().getBooleanProperty(USEBETAENCODING, false);
        String user = (betaEncoding) ? Encoding.urlTotalEncode(acc.getUser()) : Encoding.urlEncode(acc.getUser());
        String pw = (betaEncoding) ? Encoding.urlTotalEncode(acc.getPass()) : Encoding.urlEncode(acc.getPass());
        final String url = (betaEncoding) ? Encoding.urlTotalEncode(link.getDownloadURL()) : Encoding.urlEncode(link.getDownloadURL());

        String dllink = "https://www.free-way.me/load.php?multiget=2&user=" + user + "&pw=" + pw + "&url=" + url + ((betaEncoding) ? "&encoded" : "");

        if (betaEncoding) {
            /* Begin workaround for wrong encoding while redirect */
            br.setFollowRedirects(false);
            br.getPage(dllink);
            String location = br.getRedirectLocation();
            dllink = location.substring(0, location.indexOf("?")) + dllink.substring(dllink.indexOf("?"), dllink.length()) + "&s=" + location.substring(location.length() - 1, location.length());
            /* end workaround for wrong encoding while redirect */
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);

        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String error = "";
            try {
                error = (new Regex(br.toString(), "<p id=\\'error\\'>([^<]*)</p>")).getMatch(0);
            } catch (Exception e) {
                // we handle this few lines later
            }
            if (error.contains("ltiger Login")) { // Ungü
                acc.setTempDisabled(true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (error.contains("ltige URL")) { // Ungültige URL
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l);
            } else if (error.contains("Sie haben nicht genug Traffic, um diesen Download durchzuf")) { // ühren
                tempUnavailableHoster(acc, link, 10 * 60 * 1000l);
            } else if (error.contains("nnen nicht mehr parallele Downloads durchf")) { // Sie kö... ...ühren
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 1 * 60 * 1000l);
            } else if (error.contains("ltiger Hoster")) { // Ungü...
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Dieser Hoster ist aktuell leider nicht aktiv.")) {
                tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Diese Datei wurde nicht gefunden.")) {
                tempUnavailableHoster(acc, link, 1 * 60 * 1000l);
            } else if (error.contains("nnen nicht mehr parallele Downloads durchf")) { // Sie k&ouml;nnen nicht mehr parallele Downloads
                                                                                       // durchf&uuml;hren (>2 aufgrund von Drosslung).
                tempUnavailableHoster(acc, link, 1 * 60 * 1000l); // 1min
            } else if (error.equalsIgnoreCase("Unbekannter Fehler #2") || error.equals("Es ist ein unbekannter Fehler aufgetreten (#1)")) {
                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                if (link.getLinkStatus().getRetryCount() >= 2) {
                    /* reset retrycounter */
                    link.getLinkStatus().setRetryCount(0);
                    tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000l);
                }
                String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error: Retry in few secs" + msg, 20 * 1000l);
            } else if (error.startsWith("Die Datei darf maximal")) {
                tempUnavailableHoster(acc, link, 2 * 60 * 1000l);
            } else if (error.equalsIgnoreCase("Mehrere Computer haben in letzter Zeit diesen Account genutzt")) {
                acc.setTempDisabled(true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            logger.info("Unhandled download error on free-way.me: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

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
                final String accType = ai.getStringProperty("acctype", null);
                final String maxSimultanDls = account.getStringProperty("parallel", null);

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
                hostLabel.setIcon(account.getDomainInfo().getFavIcon());
                panelGenerator.addLabel(hostLabel);

                if ("de".equalsIgnoreCase(lang)) {
                    windowTitleLangText = "Account Zusatzinformationen";

                    panelGenerator.addCategory("Account");
                    panelGenerator.addEntry("Account Name:", account.getUser());
                    panelGenerator.addEntry("Account Typ:", accType);

                    if (maxSimultanDls != null) panelGenerator.addEntry("Gleichzeitige Downloads:", maxSimultanDls);

                    panelGenerator.addCategory("Unterstützte Hoster");
                    panelGenerator.addEntry("Anzahl: ", Integer.toString(hostList.size()));

                } else {
                    windowTitleLangText = "Account Information";

                    panelGenerator.addCategory("Account");
                    panelGenerator.addEntry("Account Name:", account.getUser());
                    panelGenerator.addEntry("Account Type:", accType);

                    if (maxSimultanDls != null) panelGenerator.addEntry("Simultaneous Downloads:", maxSimultanDls);

                    panelGenerator.addCategory("Supported Hosts");
                    panelGenerator.addEntry("Amount: ", Integer.toString(hostList.size()));

                }
                panelGenerator.addTable(hostList);

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
            c.gridy = y + 1;
            c.insets = new Insets(1, 0, 0, 0);
            c.gridwidth = 2;

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
        ArrayList<String> disabledHosts;
        if (disabledHostsObject.equals(Property.NULL)) return new ArrayList<String>();
        return (ArrayList<String>) disabledHostsObject;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout) throws PluginException {
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
        throw new PluginException(LinkStatus.ERROR_RETRY);
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

        public MultihostTable(ExtTableModel<MultihostContainer> tableModel) {
            super(tableModel);
            setSearchEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    public class MultihostTableModel extends ExtTableModel<MultihostContainer> {

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

        public ImageIcon getIcon() {
            try {
                return JDUtilities.getPluginForHost(host).getDomainInfo().getFavIcon();
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