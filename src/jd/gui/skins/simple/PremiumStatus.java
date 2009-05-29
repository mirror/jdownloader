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

package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.JDController;
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
        setName(JDLocale.L("quickhelp.premiumstatusbar", "Premium statusbar"));
        this.setLayout(new MigLayout("ins 0", "", "[center]"));
        premium = new JToggleButton();
        premium.setToolTipText(JDLocale.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));

        premium.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                if (JDUtilities.getConfiguration().isChanges()) {
                    updateGUI();
                    JDUtilities.getConfiguration().save();
                }

            }

        });
        premium.setFocusPainted(false);
        premium.setContentAreaFilled(false);
        premium.setBorderPainted(false);
        updatePremiumButton();
        add(premium);
        premium.addMouseListener(new JDMouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    SimpleGUI.CURRENTGUI.setWaiting(true);
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
                    SimpleGUI.CURRENTGUI.setWaiting(false);
                }
            }

        });
        add(new JSeparator(JSeparator.VERTICAL), "growy");
        add(lbl, "hidemode 3");

        for (int i = 0; i < BARCOUNT; i++) {
            TinyProgressBar pg = new TinyProgressBar();

            pg.setOpaque(false);
            bars[i] = pg;
            bars[i].addMouseListener(new JDMouseAdapter() {

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
        JDController.getInstance().addControlListener(new ConfigPropertyListener(Configuration.PARAM_USE_GLOBAL_PREMIUM) {

            @Override
            public void onPropertyChanged(Property source, String valid) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        updateGUI();
                    }

                });
            }

        });
    }

    private void updatePremiumButton() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
            premium.setSelected(true);
            premium.setIcon(JDTheme.II("gui.images.premium_enabled", 16, 16));
        } else {
            premium.setSelected(false);
            premium.setIcon(JDTheme.II("gui.images.premium_disabled", 16, 16));
        }
    }

    private void updateGUI() {
        updatePremiumButton();

        for (int i = 0; i < BARCOUNT; i++) {
            if (bars[i] != null) {
                bars[i].setEnabled(premium.isSelected());
            }
        }
    }

    private void showDetails(PluginForHost plugin) {
        SimpleGUI.displayConfig(plugin.getConfig(), 1);
    }

    public JToolTip createToolTip() {
        JToolTip toolTip = super.createToolTip();
        toolTip.setTipText("TEST...");
        return toolTip;
    }

    private synchronized void updatePremium() {
        updating = true;
        long trafficTotal = 0;
        TreeMap<String, ArrayList<AccountInfo>> map = new TreeMap<String, ArrayList<AccountInfo>>();
        TreeMap<String, Long> mapSize = new TreeMap<String, Long>();
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                final PluginForHost helpPlugin = (PluginForHost) wrapper.getNewPluginInstance();
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
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.updateIntervalTimer) {
            doUpdate();
        }
    }

}
