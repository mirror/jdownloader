package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements AccountControllerListener, ActionListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final int BARCOUNT = 9;
    private static final long ACCOUNT_UPDATE_DELAY = 15 * 60 * 1000;
    private TreeMap<String, ArrayList<AccountInfo>> map;
    private TreeMap<String, Long> mapSize;
    private TinyProgressBar[] bars;

    private long trafficTotal = 0l;

    private JLabel lbl;
    private SubConfiguration config;
    private String MAP_PROP = "MAP2";
    private String MAPSIZE_PROP = "MAPSIZE2";

    private boolean redrawinprogress = false;
    private JToggleButton premium;
    private boolean updating = false;
    private Timer updateIntervalTimer;
    private boolean updateinprogress = false;

    @SuppressWarnings("unchecked")
    public PremiumStatus() {
        super();
        bars = new TinyProgressBar[BARCOUNT];
        lbl = new JLabel(JDLocale.L("gui.statusbar.premiumloadlabel", "< Add Accounts"));

        this.setLayout(new MigLayout("ins 0", "[]", "[]"));
        premium = new JToggleButton();
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
            premium.setSelected(true);
            premium.setIcon(JDTheme.II("gui.images.premium_enabled", 16, 16));
        } else {
            premium.setSelected(false);
            premium.setIcon(JDTheme.II("gui.images.premium_disabled", 16, 16));
        }
        premium.setToolTipText(JDLocale.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));

        premium.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                if (JDUtilities.getConfiguration().isChanges()) {
                    if (premium.isSelected()) {
                        premium.setIcon(JDTheme.II("gui.images.premium_enabled", 16, 16));
                    } else {
                        premium.setIcon(JDTheme.II("gui.images.premium_disabled", 16, 16));
                        // JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM,
                        // false);
                    }

                    for (int i = 0; i < BARCOUNT; i++) {
                        if (bars[i] != null) {
                            bars[i].setEnabled(premium.isSelected());
                        }
                    }
                    JDUtilities.getConfiguration().save();
                }

            }

        });
        premium.setFocusPainted(false);
        premium.setContentAreaFilled(false);
        premium.setBorderPainted(false);
        add(premium);
        premium.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    for (HostPluginWrapper wrapper : HostPluginWrapper.getHostWrapper()) {
                        if (!wrapper.isLoaded()) continue;
                        if (!wrapper.isPremiumEnabled()) continue;
                        JMenu pluginPopup = new JMenu(wrapper.getHost());
                        ArrayList<MenuItem> entries = wrapper.getPlugin().createMenuitems();
                        for (MenuItem next : entries) {
                            JMenuItem mi = JDMenu.getJMenuItem(next);
                            if (mi == null) {
                                pluginPopup.addSeparator();
                            } else {
                                pluginPopup.add(mi);
                            }
                        }
                        popup.add(pluginPopup);

                    }

                    popup.show(premium, e.getPoint().x, e.getPoint().y);
                }
            }

        });
        add(new JSeparator(JSeparator.VERTICAL), "height 16,aligny center");
        add(lbl, "hidemode 3");

        for (int i = 0; i < BARCOUNT; i++) {
            TinyProgressBar pg = new TinyProgressBar();
            pg.setOpaque(false);
            bars[i] = pg;
            bars[i].addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent arg0) {

                    for (int i = 0; i < BARCOUNT; i++) {
                        if (bars[i] == arg0.getSource()) {
                            for (Iterator<String> it = getMap().keySet().iterator(); it.hasNext();) {
                                String host = it.next();

                                if (i == 0) {
                                    showDetails(JDUtilities.getPluginForHost(host));
                                    return;
                                }
                                i--;
                            }

                        }
                    }
                }

            });

            pg.setVisible(false);
            add(pg, "hidemode 3");

        }
        for (int i = 0; i < BARCOUNT; i++) {
            bars[i].setEnabled(premium.isSelected());
        }
        this.setOpaque(false);
        config = SubConfiguration.getConfig("PREMIUMSTATUS");
        this.map = (TreeMap<String, ArrayList<AccountInfo>>) config.getProperty(MAP_PROP, new TreeMap<String, ArrayList<AccountInfo>>());
        this.mapSize = (TreeMap<String, Long>) config.getProperty(MAPSIZE_PROP, new TreeMap<String, Long>());
        updateIntervalTimer = new Timer(5000, this);
        updateIntervalTimer.setInitialDelay(5000);
        updateIntervalTimer.setRepeats(false);
        updateIntervalTimer.stop();

        Thread updateTimer = new Thread("PremiumStatusUpdateTimer") {
            public void run() {
                requestUpdate();
                while (true) {
                    try {
                        Thread.sleep(ACCOUNT_UPDATE_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                    requestUpdate();
                }
            }
        };
        updateTimer.start();

        AccountController.getInstance().addListener(this);
    }

    private void showDetails(PluginForHost plugin) {
        SimpleGUI.displayConfig(plugin.getConfig(), 1);
    }

    private synchronized void updatePremium() {
        updating = true;
        long trafficTotal = 0;
        TreeMap<String, ArrayList<AccountInfo>> map = new TreeMap<String, ArrayList<AccountInfo>>();
        TreeMap<String, Long> mapSize = new TreeMap<String, Long>();
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = wrapper.getPlugin();
                if (helpPlugin.getPremiumAccounts().size() > 0) {
                    ArrayList<Account> alist = new ArrayList<Account>(helpPlugin.getPremiumAccounts());
                    for (Account a : alist) {
                        if (a.isEnabled()) {
                            try {
                                AccountInfo ai = null;

                                if (a.getProperty(AccountInfo.PARAM_INSTANCE) != null) {
                                    ai = (AccountInfo) a.getProperty(AccountInfo.PARAM_INSTANCE);

                                    if ((System.currentTimeMillis() - ai.getCreateTime()) >= ACCOUNT_UPDATE_DELAY) {

                                        ai = null;
                                    }
                                }
                                if (ai == null) {
                                    int to = helpPlugin.getBrowser().getConnectTimeout();
                                    helpPlugin.getBrowser().setConnectTimeout(5000);

                                    ai = helpPlugin.getAccountInformation(a);

                                    helpPlugin.getBrowser().setConnectTimeout(to);
                                }
                                if (ai == null) continue;
                                if (ai.isValid() && !ai.isExpired()) {
                                    if (!map.containsKey(wrapper.getHost())) {
                                        mapSize.put(wrapper.getHost(), 0l);
                                        map.put(wrapper.getHost(), new ArrayList<AccountInfo>());
                                    }

                                    map.get(wrapper.getHost()).add(ai);
                                    mapSize.put(wrapper.getHost(), mapSize.get(wrapper.getHost()) + (ai.getTrafficLeft() > 0 ? ai.getTrafficLeft() : UNLIMITED));

                                    trafficTotal += (ai.getTrafficLeft() > 0 ? ai.getTrafficLeft() : UNLIMITED);

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                        // try {
                        // Thread.sleep(10000000);
                        // } catch (InterruptedException e) {
                        // // TODO Auto-generated catch block
                        // e.printStackTrace();
                        // }
                    }
                }
            }

        }
        setTrafficTotal(trafficTotal);
        setMap(map);
        setMapSize(mapSize);
        save();
        updating = false;
    }

    private void save() {
        config.setProperty(MAP_PROP, map);
        config.setProperty(MAPSIZE_PROP, mapSize);
        config.save();
    }

    private synchronized void redraw() {
        if (redrawinprogress) return;
        redrawinprogress = true;
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                try {
                    lbl.setVisible(false);
                    for (int i = 0; i < BARCOUNT; i++) {
                        bars[i].setVisible(false);
                    }

                    int i = 0;
                    for (Iterator<String> it = getMap().keySet().iterator(); it.hasNext();) {
                        String host = it.next();
                        ArrayList<AccountInfo> list = getMap().get(host);

                        PluginForHost plugin = JDUtilities.getPluginForHost(host);
                        long max = 0l;
                        long left = 0l;

                        for (AccountInfo ai : list) {

                            max += ai.getTrafficMax();
                            left += ai.getTrafficLeft();
                        }
                        bars[i].setVisible(true);
                        bars[i].setIcon(plugin.getHosterIcon());
                        bars[i].setMaximum(max);
                        bars[i].setAlignmentX(RIGHT_ALIGNMENT);
                        bars[i].setValue(left);

                        if (left >= 0) {
                            // bars[i].setString(JDUtilities.formatKbReadable
                            // (left
                            // /
                            // 1024));
                            bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.traffic.tooltip", "%s - %s account(s) -- You can download up to %s today.", host, list.size(), Formatter.formatReadable(left)));
                        } else {
                            bars[i].setMaximum(10);
                            bars[i].setValue(10);

                            bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.unlimited_traffic.tooltip", "%s -- Unlimited traffic! You can download as much as you want to.", host));

                        }
                        i++;
                        if (i >= BARCOUNT) break;
                    }
                    invalidate();
                    redrawinprogress = false;
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    redrawinprogress = false;
                    return null;
                }
            }
        }.start();
    }

    public synchronized void setMap(TreeMap<String, ArrayList<AccountInfo>> map) {
        this.map = map;
    }

    public synchronized void setMapSize(TreeMap<String, Long> mapSize) {
        this.mapSize = mapSize;
    }

    public synchronized void setTrafficTotal(long trafficTotal) {
        this.trafficTotal = trafficTotal;
    }

    public synchronized TreeMap<String, ArrayList<AccountInfo>> getMap() {
        return map;
    }

    public synchronized TreeMap<String, Long> getMapSize() {
        return mapSize;
    }

    public synchronized long getTrafficTotal() {
        return trafficTotal;
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
            requestUpdate();
            break;
        default:
            break;
        }
    }

    public void requestUpdate() {
        updateIntervalTimer.restart();
    }

    private void doUpdate() {
        if (updateinprogress) return;
        new Thread() {
            public void run() {
                this.setName("PremiumStatus: update");
                updateinprogress = true;
                if (!updating) updatePremium();
                redraw();
                updateinprogress = false;
            }
        }.start();
    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        // TODO Auto-generated method stub
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.updateIntervalTimer) {
            doUpdate();
        }
    }

}
