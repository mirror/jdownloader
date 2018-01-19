package jd.plugins;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.Label;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.components.TextPane;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountEntry;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.PremiumAccountTableModel;
import jd.nutils.Formatter;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.storage.config.swing.models.ConfigLongSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.uio.UIOManager;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.AccountJsonConfig;
import org.jdownloader.plugins.config.CustomUI;
import org.jdownloader.plugins.config.Group;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginConfigPanelEventSenderEvent;
import org.jdownloader.plugins.config.PluginConfigPanelEventSenderEventSender;
import org.jdownloader.plugins.config.PluginConfigPanelEventSenderListener;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.premium.BuyAndAddPremiumAccount;
import org.jdownloader.premium.BuyAndAddPremiumDialogInterface;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class PluginConfigPanelNG extends AbstractConfigPanel implements AccountControllerListener {
    private List<Group> groups = new ArrayList<Group>();
    private boolean     seperatorRequired;

    public void addGroup(Group g) {
        this.groups.add(g);
    }

    public void addGroup(String title, String regex) {
        addGroup(new Group(title, regex, null));
    }

    private static final String                     ACCOUNT  = "ACCOUNT";
    private boolean                                 updateAccounts;
    private HashSet<Component>                      accountSettingsComponents;
    private Plugin                                  plugin;
    private Object                                  translation;
    private String                                  gapleft  = "0";
    private String                                  gapright = "";
    private int                                     headerHight;
    private boolean                                 added;
    private PluginConfigPanelEventSenderEventSender eventSender;
    private HashMap<JLabel, Account>                accountMap;

    public synchronized PluginConfigPanelEventSenderEventSender getEventSender() {
        if (eventSender == null) {
            eventSender = new PluginConfigPanelEventSenderEventSender();
        }
        return eventSender;
    }

    public PluginConfigPanelNG() {
        super(0);
    }

    @Override
    public void onAccountControllerEvent(final AccountControllerEvent event) {
        if (plugin == null || !StringUtils.equals(plugin.getClass().getSimpleName(), event.getAccount().getPlugin().getClass().getSimpleName())) {
            return;
        }
        switch (event.getType()) {
        case ACCOUNT_CHECKED:
        case ACCOUNT_PROPERTY_UPDATE:
        case ACCOUNT_UP_OR_DOWNGRADE:
        case ADDED:
        case REMOVED:
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    updateAccountSettings(event.getAccount().getPlugin());
                    final Container parent = getParent();
                    if (parent != null && parent instanceof JComponent) {
                        ((JComponent) parent).revalidate();
                    }
                }
            };
            break;
        default:
            break;
        }
    }

    @Override
    public String getRightGap() {
        return gapright;
    }

    public String getLeftGap() {
        return gapleft;
    }

    @Override
    public Header addHeader(String name, Icon icon) {
        Header header;
        add(header = new Header(name, icon), "gapleft " + getLeftGap() + ",spanx,newline,growx,pushx" + getRightGap());
        header.setLayout(new MigLayout("ins 0", "[35!,align left]5[]10[grow,fill]"));
        return header;
    }

    public JLabel addStartDescription(String description) {
        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        // txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft 0,gapbottom 5,wmin 10");
        add(new JSeparator(), "gapleft 0,spanx,growx,pushx,gapbottom 5");
        return txt;
    }

    @Override
    public JLabel addDescription(String description) {
        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        txt.setEnabled(false);
        // txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10" + getRightGap());
        add(new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5");
        return txt;
    }

    public JLabel addDescriptionPlain(String description) {
        if (StringUtils.isEmpty(description)) {
            return null;
        }
        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        txt.setEnabled(false);
        // txt.setEnabled(false);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10" + getRightGap());
        return txt;
    }

    public void reset() {
        for (ConfigInterface cfg : interfaces) {
            for (KeyHandler m : cfg._getStorageHandler().getKeyHandler()) {
                m.setValue(m.getDefaultValue());
            }
        }
        getEventSender().fireEvent(new PluginConfigPanelEventSenderEvent() {
            @Override
            public void callListener(PluginConfigPanelEventSenderListener listener) {
                listener.onConfigPanelReset(plugin, PluginConfigPanelNG.this, interfaces);
            }
        });
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                updateContents();
            }
        };
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    public void initLayout(Plugin protoType) {
        this.plugin = protoType;
        if (accountSettingsComponents != null) {
            updateAccountSettings(protoType);
            return;
        }
        initPluginSettings(protoType);
        updateAccountSettings(protoType);
    }

    private void updateAccountSettings(Plugin protoType) {
        if (accountSettingsComponents != null) {
            for (Component c : accountSettingsComponents) {
                remove(c);
            }
        }
        accountSettingsComponents = new HashSet<Component>();
        updateAccounts = true;
        try {
            initAccountSettings(protoType, AccountController.getInstance().list(protoType.getHost()));
        } finally {
            updateAccounts = false;
        }
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (updateAccounts) {
            accountSettingsComponents.add(comp);
        }
        super.addImpl(comp, constraints, index);
    }

    private List<DomainInfo> getDomainInfos(Account acc) {
        HashSet<DomainInfo> domains = new HashSet<DomainInfo>();
        AccountInfo ai = acc.getAccountInfo();
        if (ai != null) {
            final List<String> supported = ai.getMultiHostSupport();
            if (supported != null) {
                /*
                 * synchronized on list because plugins can change the list in runtime
                 */
                for (String sup : supported) {
                    LazyHostPlugin plg = HostPluginController.getInstance().get(sup);
                    if (plg != null) {
                        domains.add(DomainInfo.getInstance(plg.getHost()));
                    }
                }
            }
        }
        ArrayList<DomainInfo> ret = new ArrayList<DomainInfo>(domains);
        Collections.sort(ret, new Comparator<DomainInfo>() {
            @Override
            public int compare(DomainInfo o1, DomainInfo o2) {
                return o1.getTld().compareToIgnoreCase(o2.getTld());
            }
        });
        return ret;
    }

    @Override
    public void paint(Graphics g) {
        // Paint Border around and account entry. I do not want to use a wrapper panel, because this would fuck up the layout.
        int start = -1;
        int end = -1;
        Account acc = null;
        for (Component c : getComponents()) {
            if (c.getName() == ACCOUNT) {
                acc = accountMap.get(c);
                if (start >= 0) {
                    g.fillRect(0, start, getWidth(), headerHight);
                }
                if (acc != null) {
                    if (!acc.isValid()) {
                        g.setColor(LAFOptions.getInstance().getColorForTableAccountErrorRowBackground());
                    } else if (acc.isTempDisabled()) {
                        g.setColor(LAFOptions.getInstance().getColorForTableAccountTempErrorRowBackground());
                    } else {
                        g.setColor(LAFOptions.getInstance().getColorForPanelHeaderBackground());
                    }
                    if (!acc.isEnabled()) {
                        g.setColor(Color.LIGHT_GRAY);
                    }
                } else {
                    g.setColor(LAFOptions.getInstance().getColorForTableAccountTempErrorRowBackground());
                }
                start = c.getLocation().y - 2;
                end = c.getLocation().y + c.getHeight();
            } else {
                end = c.getLocation().y + c.getHeight();
            }
        }
        if (start >= 0) {
            g.fillRect(0, start, getWidth(), headerHight);
        }
        super.paint(g);
        g.setColor(LAFOptions.getInstance().getColorForPanelBorders());
        start = -1;
        end = -1;
        for (Component c : getComponents()) {
            if (c.getName() == ACCOUNT) {
                if (start >= 0) {
                    if (acc != null && !acc.isEnabled()) {
                        g.setColor(ColorUtils.getAlphaInstance(Color.LIGHT_GRAY, 50));
                        g.fillRect(0, start, getWidth() - 1, end - start - 1);
                        g.setColor(LAFOptions.getInstance().getColorForPanelBorders());
                    }
                    g.drawLine(0, start + headerHight, getWidth(), start + headerHight);
                    g.drawRect(0, start, getWidth() - 1, end - start - 1);
                }
                acc = accountMap.get(c);
                start = c.getLocation().y - 2;
                end = c.getLocation().y + c.getHeight();
            } else {
                end = c.getLocation().y + c.getHeight();
            }
        }
        if (start >= 0) {
            if (acc != null && !acc.isEnabled()) {
                g.setColor(ColorUtils.getAlphaInstance(Color.LIGHT_GRAY, 50));
                g.fillRect(0, start, getWidth() - 1, end - start - 1);
                g.setColor(LAFOptions.getInstance().getColorForPanelBorders());
            }
            g.drawLine(0, start + headerHight, getWidth(), start + headerHight);
            g.drawRect(0, start, getWidth() - 1, end - start - 1);
        }
    }

    protected String getTrafficString(Account acc, long current, long total) {
        AccountInfo ai = acc.getAccountInfo();
        long timeout = -1;
        if (!acc.isValid()) {
            return null;
        } else if (acc.isEnabled() && acc.isTempDisabled() && ((timeout = acc.getTmpDisabledTimeout() - System.currentTimeMillis()) > 0)) {
            return null;
        } else if (ai == null) {
            return "";
        } else {
            if (ai.isUnlimitedTraffic()) {
                return _GUI.T.premiumaccounttablemodel_column_trafficleft_unlimited();
            } else {
                return _GUI.T.premiumaccounttablemodel_column_trafficleft_left_(Formatter.formatReadable(ai.getTrafficLeft()), Formatter.formatReadable(ai.getTrafficMax()));
            }
        }
    }

    protected void initAccountSettings(final Plugin plugin, ArrayList<Account> accounts) {
        String gapbefore = gapleft;
        String gaprightBefore = getRightGap();
        if (accounts != null) {
            Collections.sort(accounts, new Comparator<Account>() {
                public int compare(boolean x, boolean y) {
                    return (x == y) ? 0 : (x ? 1 : -1);
                }

                @Override
                public int compare(Account o1, Account o2) {
                    final boolean e1 = o1.isEnabled();
                    final boolean e2 = o2.isEnabled();
                    int ret = compare(e2, e1);
                    if (ret == 0) {
                        boolean error1 = o1.getError() == null;
                        boolean error2 = o2.getError() == null;
                        ret = compare(error2, error1);
                    }
                    return ret;
                }
            });
        }
        try {
            if (plugin instanceof PluginForHost) {
                final PluginForHost plgh = ((PluginForHost) plugin);
                if (plgh.isPremiumEnabled()) {
                    AccountController.getInstance().getEventSender().removeListener(this);
                    AccountController.getInstance().getEventSender().addListener(this, true);
                    addHeader(_GUI.T.lit_your_accounts(plugin.getHost()), new AbstractIcon(IconKey.ICON_PREMIUM, 18));
                    addDescriptionPlain(_GUI.T.account_settings_description());
                    added = accounts != null && accounts.size() > 0;
                    if (!added) {
                        addDescriptionPlain(_GUI.T.description_accountmanager_button());
                    }
                    JButton bt = new JButton(ConfigurationView.getInstance().getSubPanel(AccountManagerSettings.class).getTitle());
                    bt.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            switchToAccountManager(null);
                            if (!added) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            final BuyAndAddPremiumAccount dia;
                                            UIOManager.I().show(BuyAndAddPremiumDialogInterface.class, dia = new BuyAndAddPremiumAccount(DomainInfo.getInstance(plgh.getHost()), "pluginsettings"));
                                            dia.throwCloseExceptions();
                                        } catch (DialogClosedException e1) {
                                            e1.printStackTrace();
                                        } catch (DialogCanceledException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    });
                    add(bt, "spanx,pushx,growx");
                    gapleft = "5";
                    gapright = ",gapright 5";
                    headerHight = 0;
                    accountMap = new HashMap<JLabel, Account>();
                    if (accounts != null) {
                        for (final Account acc : accounts) {
                            Class<? extends AccountConfigInterface> confinf = plgh.getAccountConfigInterface(acc);
                            // addHeader(acc.getUser(), (Icon) null);
                            TextPane status;
                            JLabel accountHeader = new JLabel(_GUI.T.plugin_account_header(acc.getUser()));
                            SwingUtils.toBold(accountHeader);
                            add(accountHeader, "gaptop 2,gapbottom 2, gapleft " + gapleft + "" + getRightGap());
                            headerHight = Math.max(headerHight, accountHeader.getPreferredSize().height + 4);
                            accountHeader.setName(ACCOUNT);
                            accountMap.put(accountHeader, acc);
                            final ExtCheckBox enabledBox = new ExtCheckBox();
                            enabledBox.setSelected(acc.isEnabled());
                            enabledBox.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    acc.setEnabled(enabledBox.isSelected());
                                    repaint();
                                }
                            });
                            enabledBox.setText(acc.isEnabled() ? _GUI.T.lit_enabled() : _GUI.T.lit_disabled());
                            accountHeader.setEnabled(acc.isEnabled());
                            add(enabledBox, "spanx,gapright " + gapleft + ", alignx right" + getRightGap());
                            addPair(_GUI.T.lit_status(), null, new Label(PremiumAccountTableModel.accountToStatusString(acc), PremiumAccountTableModel.accountToStatusIcon(acc)));
                            // addPair(_GUI.T.lit_type(), null, new Label(acc.getType().getLabel()));
                            AccountInfo ai = acc.getAccountInfo();
                            if (ai != null) {
                                String expire = getExpireDateString(acc, ai);
                                if (StringUtils.isNotEmpty(expire)) {
                                    addPair(_GUI.T.premiumaccounttablemodel_column_expiredate(), null, new Label(expire));
                                }
                                long addedTs = acc.getLongProperty("added", -1);
                                if (addedTs > 0) {
                                    addPair(_GUI.T.lit_added(), null, new Label(formatDate(new Date(addedTs))));
                                }
                                String traffic = getTrafficString(acc, ai.getTrafficLeft(), ai.getTrafficMax());
                                if (StringUtils.isNotEmpty(traffic)) {
                                    addPair(_GUI.T.lit_download_traffic(), null, new Label(traffic));
                                }
                                if (ai.getUsedSpace() != -1) {
                                    addPair(_GUI.T.lit_used_space(), null, new Label(Formatter.formatReadable(ai.getUsedSpace())));
                                }
                                if (ai.getPremiumPoints() != -1) {
                                    addPair(_GUI.T.lit_premium_points(), null, new Label(ai.getPremiumPoints() + ""));
                                }
                            }
                            if (acc.isMultiHost() && acc.getPlugin().getLazyP().hasFeature(FEATURE.MULTIHOST)) {
                                initMultiHosterInfo(acc);
                            }
                            Class<? extends AccountConfigInterface> accountConfig = plgh.getAccountConfigInterface(acc);
                            if (accountConfig != null) {
                                initAccountConfig(plgh, acc, accountConfig);
                            }
                            plgh.extendAccountSettingsPanel(acc, this);
                        }
                    }
                }
            }
        } finally {
            gapleft = gapbefore;
            gapright = gaprightBefore;
        }
    }

    private String getExpireDateString(Account acc, AccountInfo ai) {
        final long validUntil = ai.getValidUntil();
        if (validUntil <= 0) {
            return null;
        }
        final Date date = new Date(validUntil);
        final long left = validUntil - System.currentTimeMillis();
        if (left <= 0) {
            return formatDate(date) + " (" + _GUI.T.PremiumAccountTableModel_getStringValue_status_expired() + ")";
        } else {
            return formatDate(date) + " (" + TimeFormatter.formatMilliSeconds(left, TimeFormatter.HIDE_SECONDS) + ")";
        }
    }

    protected String formatDate(Date date) {
        String custom = CFG_GUI.CFG.getDateTimeFormatAccountManagerExpireDateColumn();
        if (StringUtils.isEmpty(custom)) {
            DateFormat sd = SimpleDateFormat.getDateTimeInstance();
            if (sd instanceof SimpleDateFormat) {
                custom = ((SimpleDateFormat) sd).toPattern();
            }
        }
        if (StringUtils.isEmpty(custom)) {
            custom = _GUI.T.PremiumAccountTableModel_getDateFormatString_();
        }
        return new SimpleDateFormat(custom).format(date);
    }

    protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> accountConfig) {
        Header header = addHeader(_GUI.T.account_settings_header(), new AbstractIcon(IconKey.ICON_SETTINGS, 16));
        build(AccountJsonConfig.get(acc));
        if (getComponents()[getComponentCount() - 1] == header) {
            remove(header);
        } else {
            add(Box.createGlue(), "gapbottom 5,pushx,growx,spanx" + getRightGap());
        }
    }

    protected void initMultiHosterInfo(final Account acc) {
        String gapbefore = gapleft;
        String gapbeforeright = getRightGap();
        try {
            gapleft = "5";
            gapright = ",gapright 5";
            // JLabel label = new JLabel(_GUI.T.AccountTooltip_AccountTooltip_supported_hosters());
            // add(label, "gapleft " + gapleft + ",gapright 3,pushx,growx,spanx" + getRightGap());
            addHeader(_GUI.T.multihoster_account_settings_header(), new AbstractIcon(IconKey.ICON_LIST, 18));
            addDescription(_GUI.T.multihoster_account_settings_description());
            List<DomainInfo> dis = getDomainInfos(acc);
            final JList list = new JList(dis.toArray(new DomainInfo[] {}));
            list.setLayoutOrientation(JList.VERTICAL_WRAP);
            final ListCellRenderer org = list.getCellRenderer();
            list.setCellRenderer(new ListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DomainInfo di = (DomainInfo) value;
                    JLabel ret = (JLabel) org.getListCellRendererComponent(list, "", index, false, cellHasFocus);
                    ret.setText(di.getTld());
                    ret.setIcon(di.getFavIcon());
                    ret.setOpaque(false);
                    ret.setBackground(null);
                    return ret;
                }
            });
            list.setVisibleRowCount(dis.size() / 5);
            // list.setFixedCellHeight(22);
            // list.setFixedCellWidth(22);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setOpaque(false);
            list.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    DomainInfo domainInfo = (DomainInfo) list.getSelectedValue();
                    if (domainInfo != null) {
                        LazyHostPlugin plg = HostPluginController.getInstance().get(domainInfo.getTld());
                        if (plg != null) {
                            try {
                                StatsManager.I().openAfflink(plg.getPrototype(null), null, "MultiHostPanel");
                                return;
                            } catch (UpdateRequiredClassNotFoundException e1) {
                                e1.printStackTrace();
                            }
                        }
                        CrossSystem.openURL(AccountController.createFullBuyPremiumUrl("http://" + domainInfo.getTld(), "MultiHostPanel"));
                    }
                };
            });
            add(list, "gapleft " + gapleft + ",gapright " + gapleft + ",pushx,growx,spanx" + getRightGap());
            add(Box.createGlue(), "gapbottom 5,pushx,growx,spanx" + getRightGap());
        } finally {
            gapleft = gapbefore;
            gapright = gapbeforeright;
        }
    }

    protected void initPluginSettings(Plugin plugin) {
        Class<? extends PluginConfigInterface> inf = plugin.getConfigInterface();
        if (inf != null) {
            build(PluginJsonConfig.get(inf));
        }
    }

    public Rectangle getAccountRectangle(Account account) {
        return null;
    }

    protected void switchToAccountManager(AccountEntry obj) {
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(AccountManagerSettings.class);
        ConfigurationView.getInstance().getSubPanel(AccountManagerSettings.class).getAccountManager().setTab(0);
        if (obj != null) {
            ConfigurationView.getInstance().getSubPanel(AccountManagerSettings.class).getAccountManager().selectAccount(obj.getAccount());
        }
    }

    public void addHandler(ConfigInterface cfg, KeyHandler m) {
        DescriptionForConfigEntry anno = (DescriptionForConfigEntry) m.getAnnotation(DescriptionForConfigEntry.class);
        System.out.println("Add " + m.getReadableName());
        String description = getTranslation(m, "_description");
        if (description != null) {
            addDescriptionPlain(description);
        } else if (anno != null) {
            addDescriptionPlain(anno.value());
        }
        String label = getTranslation(m, "_label");
        if (label == null) {
            label = m.getReadableName();
        }
        if (m instanceof BooleanKeyHandler) {
            addPair(label, null, new Checkbox((BooleanKeyHandler) m));
        } else if (m instanceof StringKeyHandler) {
            addPair(label, null, new TextInput((StringKeyHandler) m));
        } else if (m instanceof IntegerKeyHandler) {
            addPair(label, null, new Spinner(new ConfigIntSpinnerModel((IntegerKeyHandler) m)));
        } else if (m instanceof LongKeyHandler) {
            addPair(label, null, new Spinner(new ConfigLongSpinnerModel((LongKeyHandler) m)));
        } else if (m instanceof EnumKeyHandler) {
            addPair(label, null, null, new ComboBox<Enum>(m, ((EnumKeyHandler) m).values(), null));
        } else {
            UIOManager.I().showException("Unsupported Type: " + m, new Exception());
        }
    }

    private String getTranslation(KeyHandler m, String ext) {
        if (translation == null) {
            return null;
        }
        String caseSensitiveGetter = m.getGetMethod().getName();
        if (caseSensitiveGetter.startsWith("is")) {
            caseSensitiveGetter = "get" + caseSensitiveGetter.substring(2);
        }
        try {
            Method method;
            method = translation.getClass().getMethod(caseSensitiveGetter + ext, new Class[] {});
            method.setAccessible(true);
            return (String) method.invoke(translation, new Object[] {});
        } catch (NoSuchMethodException e) {
            if ("_label".equals(ext)) {
                System.out.println("Missing Translation: " + translation.getClass().getName() + "." + caseSensitiveGetter + ext + "(){return ....}");
            }
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    public JSeparator addSeperator() {
        if (getComponent(getComponentCount() - 1) instanceof JSeparator) {
            return null;
        }
        JSeparator sep;
        add(sep = new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5" + getRightGap());
        return sep;
    }

    private final CopyOnWriteArraySet<ConfigInterface> interfaces = new CopyOnWriteArraySet<ConfigInterface>();

    protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
        return keyHandler != null;
    }

    public void build(ConfigInterface cfg) {
        interfaces.add(cfg);
        ArrayList<Group> interfaceGroups = new ArrayList<Group>();
        interfaceGroups.addAll(groups);
        Field groupField;
        try {
            groupField = cfg._getStorageHandler().getConfigInterface().getField("GROUPS");
            groupField.setAccessible(true);
            for (Group p : (Group[]) groupField.get(null)) {
                interfaceGroups.add(p.createClone());
            }
        } catch (NoSuchFieldException e) {
            // ok
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
            UIOManager.I().showException("Bad Plugin Config Interface. Contact Support.", e);
        }
        try {
            Field field = cfg._getStorageHandler().getConfigInterface().getField("TRANSLATION");
            field.setAccessible(true);
            translation = field.get(null);
        } catch (NoSuchFieldException e) {
            // ok
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
            UIOManager.I().showException("Bad Plugin Config Interface. Contact Support.", e);
        }
        Group defGroup = null;
        ArrayList<Group> finalGroups = new ArrayList<Group>();
        HashSet<Group> groupsAdded = new HashSet<Group>();
        List<KeyHandler<?>> list = cfg._getStorageHandler().getKeyHandler();
        Collections.sort(list, new Comparator<KeyHandler<?>>() {
            @Override
            public int compare(KeyHandler<?> o1, KeyHandler<?> o2) {
                Order orderAn1 = o1.getAnnotation(Order.class);
                Order orderAn2 = o2.getAnnotation(Order.class);
                int order1 = orderAn1 == null ? Integer.MAX_VALUE : orderAn1.value();
                int order2 = orderAn2 == null ? Integer.MAX_VALUE : orderAn2.value();
                int ret = CompareUtils.compare(order1, order2);
                if (ret == 0) {
                    return o1.getKey().compareToIgnoreCase(o2.getKey());
                } else {
                    return ret;
                }
            }
        });
        parent: for (KeyHandler<?> m : list) {
            if (!showKeyHandler(m)) {
                continue;
            }
            for (Group g : interfaceGroups) {
                if (g.matches(m)) {
                    g.add(m);
                    if (groupsAdded.add(g)) {
                        finalGroups.add(g);
                    }
                    defGroup = null;
                    continue parent;
                }
            }
            if (defGroup == null) {
                defGroup = new Group();
                finalGroups.add(defGroup);
            }
            defGroup.add(m);
        }
        seperatorRequired = false;
        for (Group p : finalGroups) {
            if (StringUtils.isNotEmpty(p.getTitle())) {
                addHeader(p.getTitle(), new AbstractIcon(p.getIconKey(), 20));
                seperatorRequired = false;
            } else if (seperatorRequired) {
                addSeperator();
                seperatorRequired = false;
            }
            for (KeyHandler h : p.handler) {
                if (useCustomUI(h)) {
                    continue;
                }
                addHandler(cfg, h);
                if (p.getTitle() != null) {
                    seperatorRequired = true;
                }
            }
        }
    }

    protected boolean useCustomUI(KeyHandler h) {
        return h.getAnnotation(CustomUI.class) != null;
    }

    public void addStringPair(String key, Object value) {
        if (value != null) {
            addPair(key, null, new Label(String.valueOf(value)));
        }
    }
}
