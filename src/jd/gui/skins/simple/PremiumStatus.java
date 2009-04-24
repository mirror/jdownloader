package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountListener;
import jd.controlling.AccountManager;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements ControlListener, AccountListener, ActionListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final String DEBUG = "";
    private static final int BARCOUNT = 9;
    private TreeMap<String, ArrayList<AccountInfo>> map;
    private TreeMap<String, Long> mapSize;
    private TinyProgressBar[] bars;

    private long trafficTotal = 0l;
    private Timer refresher;
    private JLabel lbl;
    private Logger logger;
    private SubConfiguration config;
    private String MAP_PROP = "MAP2";
    private String MAPSIZE_PROP = "MAPSIZE2";
    private Object Lock = new Object();

    @SuppressWarnings("unchecked")
    public PremiumStatus() {
        super();
        bars = new TinyProgressBar[BARCOUNT];
        logger = JDLogger.getLogger();

        refresher = new Timer(1000 * 60 * 15, this);
        refresher.setInitialDelay(1000 * 60 * 15);
        refresher.setRepeats(true);

        this.setLayout(new MigLayout(DEBUG + "ins 0", "[]", "[]"));
        add(lbl = new JLabel("Load Premiumstatus..."), "hidemode 3");
        JDController.getInstance().addControlListener(this);
        AccountManager.getInstance().addAccountListener(this);
        for (int i = 0; i < BARCOUNT; i++) {
            TinyProgressBar pg = new TinyProgressBar();
            bars[i] = pg;
            bars[i].addMouseListener(new MouseListener() {

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

                public void mouseEntered(MouseEvent arg0) {

                }

                public void mouseExited(MouseEvent arg0) {

                }

                public void mousePressed(MouseEvent arg0) {

                }

                public void mouseReleased(MouseEvent arg0) {

                }

            });

            pg.setVisible(false);
            add(pg, "hidemode 3");

        }
        config = SubConfiguration.getConfig("PREMIUMSTATUS");
        this.map = (TreeMap<String, ArrayList<AccountInfo>>) config.getProperty(MAP_PROP, new TreeMap<String, ArrayList<AccountInfo>>());
        this.mapSize = (TreeMap<String, Long>) config.getProperty(MAPSIZE_PROP, new TreeMap<String, Long>());
        this.onUpdate();
    }

    protected void showDetails(PluginForHost plugin) {
        ConfigEntriesPanel panel = new ConfigEntriesPanel(plugin.getConfig());

        Component comp = panel.getComponent(0);
        if (comp instanceof JTabbedPane) {
            ((JTabbedPane) comp).setSelectedIndex(((JTabbedPane) comp).getTabCount() - 1);
        }
        SimpleGUI.CURRENTGUI.getContentPane().display(panel);
    }

    private synchronized void updatePremium() {
        synchronized (Lock) {
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
        }
        onUpdate();
    }

    private void save() {
        config.setProperty(MAP_PROP, map);
        config.setProperty(MAPSIZE_PROP, mapSize);
        config.save();

    }

    private synchronized void redraw() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                System.out.println("redraw");
                synchronized (Lock) {
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
                            //bars[i].setString(JDUtilities.formatKbReadable(left
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
        onUpdate();
    }

    public void onUpdate() {
        refresher.stop();
        refresher.setDelay(2000);
        refresher.setInitialDelay(2000);
        refresher.restart();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == this.refresher) {
            refresher.stop();
            if (refresher.getDelay() == 1000 * 60 * 15) {
                updatePremium();
            } else {
                redraw();
            }
            refresher.setDelay(1000 * 60 * 15);
            refresher.setInitialDelay(1000 * 60 * 15);
            refresher.restart();
        }
    }

}
