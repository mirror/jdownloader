package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.HostPluginWrapper;
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

public class PremiumStatus extends JPanel implements ControlListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final String DEBUG = "";
    private static final int BARCOUNT = 8;
    private TreeMap<HostPluginWrapper, ArrayList<AccountInfo>> map;
    private TreeMap<HostPluginWrapper, Long> mapSize;
    private JDProgressBar[] bars;

    private long trafficTotal = 0l;
    private Thread refresher;
    private JLabel lbl;
    private Thread cacheWait;
    private Logger logger;

    public PremiumStatus() {
        super();
        bars = new JDProgressBar[BARCOUNT];
        logger = JDLogger.getLogger();
        this.map = new TreeMap<HostPluginWrapper, ArrayList<AccountInfo>>();
        this.mapSize = new TreeMap<HostPluginWrapper, Long>();
        this.setLayout(new MigLayout(DEBUG + "ins 0", "[]", "[]"));
        add(lbl = new JLabel("Load Premiumstatus..."), "hidemode 3");
        JDController.getInstance().addControlListener(this);
        for (int i = 0; i < BARCOUNT; i++) {
            JDProgressBar pg = new JDProgressBar();
            bars[i] = pg;
            bars[i].addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent arg0) {

                    for (int i = 0; i < BARCOUNT; i++) {
                        if (bars[i] == arg0.getSource()) {
                            for (Iterator<HostPluginWrapper> it = getMap().keySet().iterator(); it.hasNext();) {
                                HostPluginWrapper wrapper = it.next();

                                if (i == 0) {
                                    showDetails(wrapper);
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
        TreeMap<HostPluginWrapper, ArrayList<AccountInfo>> map = new TreeMap<HostPluginWrapper, ArrayList<AccountInfo>>();
        TreeMap<HostPluginWrapper, Long> mapSize = new TreeMap<HostPluginWrapper, Long>();
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
                                    if (!map.containsKey(wrapper)) {
                                        mapSize.put(wrapper, 0l);
                                        map.put(wrapper, new ArrayList<AccountInfo>());
                                    }

                                    map.get(wrapper).add(ai);
                                    mapSize.put(wrapper, mapSize.get(wrapper) + (ai.getTrafficLeft() > 0 ? ai.getTrafficLeft() : UNLIMITED));

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
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                redraw();
                return null;
            }

        }.start();
    }

    private void redraw() {
        lbl.setVisible(false);
        for (int i = 0; i < BARCOUNT; i++) {
            bars[i].setVisible(false);
        }

        int i = 0;
        for (Iterator<HostPluginWrapper> it = getMap().keySet().iterator(); it.hasNext();) {
            HostPluginWrapper wrapper = it.next();
            ArrayList<AccountInfo> list = this.getMap().get(wrapper);

            long max = 0l;
            long left = 0l;

            for (AccountInfo ai : list) {

                max += ai.getTrafficMax();
                left += ai.getTrafficLeft();
            }
            bars[i].setVisible(true);
            bars[i].setIcon(JDImage.getScaledImageIcon(wrapper.getPlugin().getHosterIcon(), 12, 12));
            bars[i].setMaximum(max);
            bars[i].setValue(left);

            if (left >= 0) {
                bars[i].setString(JDUtilities.formatKbReadable(left / 1024));
                bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.traffic.tooltip", "%s - %s account(s) -- You can download up to %s today.", wrapper.getHost(), list.size(), JDUtilities.formatKbReadable(left / 1024)));
            } else {
                bars[i].setMaximum(10);
                bars[i].setValue(10);
                bars[i].setString("");
                bars[i].setToolTipText(JDLocale.LF("gui.premiumstatus.unlimited_traffic.tooltip", "%s -- Unlimited traffic! You can download as much as you want to.", wrapper.getHost()));

            }
            i++;
            if (i >= BARCOUNT) break;
        }

        this.invalidate();

    }

    public synchronized void setMap(TreeMap<HostPluginWrapper, ArrayList<AccountInfo>> map) {
        this.map = map;
    }

    public synchronized void setMapSize(TreeMap<HostPluginWrapper, Long> mapSize) {
        this.mapSize = mapSize;
    }

    public synchronized void setTrafficTotal(long trafficTotal) {
        this.trafficTotal = trafficTotal;
    }

    public synchronized TreeMap<HostPluginWrapper, ArrayList<AccountInfo>> getMap() {
        return map;
    }

    public synchronized TreeMap<HostPluginWrapper, Long> getMapSize() {
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
        } else if (event.getParameter() == PluginForHost.PROPERTY_PREMIUM) {
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

}
