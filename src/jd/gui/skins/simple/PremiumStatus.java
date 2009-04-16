package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.HostPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountListener;
import jd.controlling.AccountManager;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.nutils.JDImage;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements ControlListener, AccountListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final String DEBUG = "";
    private static final int BARCOUNT = 8;
    private TreeMap<String, ArrayList<AccountInfo>> map;
    private TreeMap<String, Long> mapSize;
    private JDProgressBar[] bars;

    private long trafficTotal = 0l;
    private Thread refresher;
    private JLabel lbl;
    private Thread cacheWait;
    private Logger logger;
    private SubConfiguration config;
    private String MAP_PROP = "MAP2";
    private String MAPSIZE_PROP = "MAPSIZE2";

    @SuppressWarnings("unchecked")
    public PremiumStatus() {
        super();
        bars = new JDProgressBar[BARCOUNT];
        logger = JDLogger.getLogger();

        this.setLayout(new MigLayout(DEBUG + "ins 0", "[]", "[]"));
        add(lbl = new JLabel("Load Premiumstatus..."), "hidemode 3");
        JDController.getInstance().addControlListener(this);
        AccountManager.getInstance().addAccountListener(this);
        for (int i = 0; i < BARCOUNT; i++) {
            JDProgressBar pg = new JDProgressBar();
            bars[i] = pg;
            bars[i].addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent arg0) {

                    for (int i = 0; i < BARCOUNT; i++) {
                        if (bars[i] == arg0.getSource()) {
                            for (Iterator<String> it = getMap().keySet().iterator(); it.hasNext();) {
                                String host = it.next();

                                if (i == 0) {
                                    showDetails((HostPluginWrapper) JDUtilities.getPluginForHost(host).getWrapper());
                                    return;
                                }
                                i--;
                            }

                        }
                    }
                }

                public void mouseEntered(MouseEvent arg0) {

                }

                public void mouseExited(MouseEvent arg0) {

                }

                public void mousePressed(MouseEvent arg0) {

                }

                public void mouseReleased(MouseEvent arg0) {

                }

            });
            pg.setStringPainted(true);
            pg.setVisible(false);
            add(pg, "width 80!,height 16!,hidemode 3");

        }
        config = SubConfiguration.getConfig("PREMIUMSTATUS");
        this.map = (TreeMap<String, ArrayList<AccountInfo>>) config.getProperty(MAP_PROP, new TreeMap<String, ArrayList<AccountInfo>>());
        this.mapSize = (TreeMap<String, Long>) config.getProperty(MAPSIZE_PROP, new TreeMap<String, Long>());
        refresher = new Thread() {
            public void run() {

                while (true) {
                    try {
                        Thread.sleep(15 * 60 * 1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    updatePremium();

                }
            }

        };

        refresher.start();
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                redraw();
                return null;
            }

        }.start();
    }

    protected void showDetails(HostPluginWrapper wrapper) {
        // System.out.println("Show detaoils to " + wrapper.getClassName());
        // SimpleGUI.showConfigDialog(SimpleGUI.CURRENTGUI,
        // wrapper.getPlugin().getConfig());
        ConfigEntriesPanel panel = new ConfigEntriesPanel(wrapper.getPlugin().getConfig());
        Component comp = panel.getComponent(0);
        if (comp instanceof JTabbedPane) {
            ((JTabbedPane) comp).setSelectedIndex(((JTabbedPane) comp).getTabCount() - 1);
        }
        SimpleGUI.CURRENTGUI.getContentPane().display(panel);
    }

    private void updatePremium() {
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
                                int to = helpPlugin.getBrowser().getConnectTimeout();
                                helpPlugin.getBrowser().setConnectTimeout(5000);

                                AccountInfo ai = helpPlugin.getAccountInformation(a);
                                helpPlugin.getBrowser().setConnectTimeout(to);
                                if (ai.isValid()) {
                                    if (!map.containsKey(wrapper.getHost())) {
                                        mapSize.put(wrapper.getHost(), 0l);
                                        map.put(wrapper.getHost(), new ArrayList<AccountInfo>());
                                    }

                                    map.get(wrapper.getHost()).add(ai);
                                    mapSize.put(wrapper.getHost(), mapSize.get(wrapper) + (ai.getTrafficLeft() > 0 ? ai.getTrafficLeft() : UNLIMITED));

                                    trafficTotal += (ai.getTrafficLeft() > 0 ? ai.getTrafficLeft() : UNLIMITED);

                                }
                            } catch (Exception e) {

                            }

                        }
                    }
                }
            }

        }
        this.setTrafficTotal(trafficTotal);
        this.setMap(map);
        this.setMapSize(mapSize);
        save();
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                redraw();
                return null;
            }

        }.start();
    }

    private void save() {
        config.setProperty(MAP_PROP, map);
        config.setProperty(MAPSIZE_PROP, mapSize);
        config.save();

    }

    private void redraw() {
        lbl.setVisible(false);
        for (int i = 0; i < BARCOUNT; i++) {
            bars[i].setVisible(false);
        }

        int i = 0;
        for (Iterator<String> it = getMap().keySet().iterator(); it.hasNext();) {
            String host = it.next();
            ArrayList<AccountInfo> list = this.getMap().get(host);

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
                bars[i].setString(JDUtilities.formatKbReadable(left / 1024));
                bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.traffic.tooltip", "%s - %s account(s) -- You can download up to %s today.", host, list.size(), JDUtilities.formatKbReadable(left / 1024)));
            } else {
                bars[i].setMaximum(10);
                bars[i].setValue(10);
                bars[i].setString("∞");
                bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.unlimited_traffic.tooltip", "%s -- Unlimited traffic! You can download as much as you want to.", host));

            }
            i++;
            if (i >= BARCOUNT) break;
        }

        this.invalidate();

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
        if (event.getID() == ControlEvent.CONTROL_PLUGIN_INACTIVE) {
            new GuiRunnable<Object>() {

                @Override
                public Object runSave() {
                    redraw();
                    return null;
                }

            }.start();
        }

    }

    public void onUpdate() {
        if (cacheWait != null) return;
        this.cacheWait = new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
                updatePremium();
                cacheWait = null;
            }
        };
        cacheWait.start();

    }

}
