package jd.gui.skins.simple;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.AccountListener;
import jd.controlling.AccountManager;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements ControlListener, AccountListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final int BARCOUNT = 9;
    private static final long ACCOUNT_UPDATE_DELAY = 15 * 60 * 1000;
    private TreeMap<String, ArrayList<AccountInfo>> map;
    private TreeMap<String, Long> mapSize;
    private TinyProgressBar[] bars;

    private long trafficTotal = 0l;

    private JLabel lbl;
    private Logger logger;
    private SubConfiguration config;
    private String MAP_PROP = "MAP2";
    private String MAPSIZE_PROP = "MAPSIZE2";

    // private boolean redrawinprogress = false;
    private JToggleButton premium;
    private Thread updater;
    private boolean updating = false;

    @SuppressWarnings("unchecked")
    public PremiumStatus() {
        super();
        bars = new TinyProgressBar[BARCOUNT];
        logger = JDLogger.getLogger();
        lbl = new JLabel(JDLocale.L("gui.statusbar.premiumloadlabel", "< Add Accounts"));
        Thread updateTimer = new Thread("PremiumStatusUpdateTimer") {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                    return;
                }
                updatePremium();
                redraw();
                while (true) {
                    try {
                        Thread.sleep(ACCOUNT_UPDATE_DELAY);
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                        return;
                    }
                    updatePremium();
                }
            }
        };

        this.setLayout(new MigLayout("ins 0", "[]", "[]"));

        JDController.getInstance().addControlListener(this);
        AccountManager.getInstance().addAccountListener(this);

        premium = new JToggleButton();
        premium.setIcon(JDTheme.II("gui.images.premium_disabled", 16, 16));
        premium.setSelectedIcon(JDTheme.II("gui.images.premium_enabled", 16, 16));
        premium.setToolTipText(JDLocale.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));

        premium.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                if (JDUtilities.getConfiguration().isChanges()) {
                    if (premium.isSelected()) {
                        premium.setIcon(JDTheme.II("gui.images.premium_enabled", 16, 16));

                    } else {
                        premium.setIcon(JDTheme.II("gui.images.premium_disabled", 16, 16));
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, false);

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

        premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
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

        config = SubConfiguration.getConfig("PREMIUMSTATUS");
        this.map = (TreeMap<String, ArrayList<AccountInfo>>) config.getProperty(MAP_PROP, new TreeMap<String, ArrayList<AccountInfo>>());
        this.mapSize = (TreeMap<String, Long>) config.getProperty(MAPSIZE_PROP, new TreeMap<String, Long>());

        updateTimer.start();
    }

    private void showDetails(PluginForHost plugin) {
        SimpleGUI.displayConfig(plugin.getConfig(), 1);
    }

    private void updatePremium() {
        updating = true;

        long trafficTotal = 0;
        logger.finer("Update Premiuminfo");
        TreeMap<String, ArrayList<AccountInfo>> map = new TreeMap<String, ArrayList<AccountInfo>>();
        TreeMap<String, Long> mapSize = new TreeMap<String, Long>();
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = wrapper.getPlugin();
                if (helpPlugin.getPremiumAccounts().size() > 0) {
                    for (Account a : helpPlugin.getPremiumAccounts()) {
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
                                if(ai==null)continue;
                                if (ai.isValid()&&!ai.isExpired()) {
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
        System.out.println("Update Premium finished");
        updating = false;
    }

    private void save() {
        config.setProperty(MAP_PROP, map);
        config.setProperty(MAPSIZE_PROP, mapSize);
        config.save();
    }

    private void redraw() {
        // if (redrawinprogress) return;
        // redrawinprogress = true;
        System.out.println("REQUEST DRAW");
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                System.out.println("DRAW");
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
                    // redrawinprogress = false;
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
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

    public void controlEvent(ControlEvent event) {
        // onUpdate();
    }

    public void onUpdate() {

     
                if (!updating) updatePremium();
                redraw();
       
    }

}
