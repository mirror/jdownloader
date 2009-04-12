package jd.gui.skins.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.HostPluginWrapper;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements ControlListener {
    private static final int GAP = 5;
    private static final long UNLIMITED = 10l * 1024l * 1024l * 1024l;
    private static final String DEBUG = "";
    private static final int BARCOUNT = 5;
    private HashMap<HostPluginWrapper, ArrayList<AccountInfo>> map;
    private HashMap<HostPluginWrapper, Long> mapSize;
    private JDProgressBar[] bars;

    private long trafficTotal = 0l;
    private Thread refresher;
    private JLabel lbl;

    public PremiumStatus() {
        super();
        bars = new JDProgressBar[BARCOUNT];
        this.map = new HashMap<HostPluginWrapper, ArrayList<AccountInfo>>();
        this.mapSize = new HashMap<HostPluginWrapper, Long>();
        this.setLayout(new MigLayout(DEBUG + "ins 0", "[]", "[]"));
        add(lbl = new JLabel("Load Premiumstatus..."), "hidemode 3");
        JDController.getInstance().addControlListener(this);
        for (int i = 0; i < BARCOUNT; i++) {
            JDProgressBar pg = new JDProgressBar();
            bars[i] = pg;
            pg.setStringPainted(true);
            pg.setVisible(false);
            add(pg, "width 70!,height 16!,hidemode 3");

        }
        refresher = new Thread() {
            public void run() {

                while (true) {
                    updatePremium();
                    try {
                        Thread.sleep(15 * 60 * 1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

        };

        refresher.start();
    }

    private void updatePremium() {
        long trafficTotal = 0;
        HashMap<HostPluginWrapper, ArrayList<AccountInfo>> map = new HashMap<HostPluginWrapper, ArrayList<AccountInfo>>();
        HashMap<HostPluginWrapper, Long> mapSize = new HashMap<HostPluginWrapper, Long>();
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

            bars[i].setMaximum(max);
            bars[i].setValue(left);

            if (left >= 0) {
                bars[i].setString(JDUtilities.formatKbReadable(left / 1024));
                bars[i].setToolTipText(wrapper.getHost() + ": " + JDUtilities.formatKbReadable(left / 1024) + " in " + list.size() + " accounts");
            } else {
                bars[i].setString(JDUtilities.formatKbReadable(left / 1024));
                bars[i].setToolTipText(wrapper.getHost() + ": unlimited traffic");

            }
            i++;
            if (i >= BARCOUNT) break;
        }

        this.invalidate();

    }

    public synchronized void setMap(HashMap<HostPluginWrapper, ArrayList<AccountInfo>> map) {
        this.map = map;
    }

    public synchronized void setMapSize(HashMap<HostPluginWrapper, Long> mapSize) {
        this.mapSize = mapSize;
    }

    public synchronized void setTrafficTotal(long trafficTotal) {
        this.trafficTotal = trafficTotal;
    }

    public synchronized HashMap<HostPluginWrapper, ArrayList<AccountInfo>> getMap() {
        return map;
    }

    public synchronized HashMap<HostPluginWrapper, Long> getMapSize() {
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

}
