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

package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import jd.HostPluginWrapper;
import jd.Main;
import jd.config.Configuration;
import jd.controlling.AccountChecker;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIF;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.gui.translate._GUI;

public class PremiumStatus extends JPanel implements MouseListener, ControlListener {

    private static final long       serialVersionUID = 7290466989514173719L;
    private static final int        BARCOUNT         = 15;
    private final TinyProgressBar[] bars;
    private DelayedRunnable         redrawTimer;
    private static PremiumStatus    INSTANCE         = new PremiumStatus();

    public static PremiumStatus getInstance() {
        return INSTANCE;
    }

    private PremiumStatus() {
        super(new MigLayout("ins 0 3 0 0", "", "[20!]"));

        bars = new TinyProgressBar[BARCOUNT];
        for (int i = 0; i < BARCOUNT; i++) {
            bars[i] = new TinyProgressBar();
            bars[i].setOpaque(false);
            bars[i].addMouseListener(this);
            bars[i].setVisible(false);
            add(bars[i], "hidemode 3");
        }
        updateGUI(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));

        this.setOpaque(false);

        JDUtilities.getController().addControlListener(this);

        IOEQ.TIMINGQUEUE.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                /* this scheduleritem checks all enabled accounts every 30 mins */
                try {
                    refreshAccountStats();
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }

        }, 1, 30, TimeUnit.MINUTES);
        redrawTimer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 5000) {

            @Override
            public void delayedrun() {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) redraw();
            }

        };
    }

    public void updateGUI(boolean enabled) {
        if (bars == null) return;
        for (int i = 0; i < BARCOUNT; i++) {
            bars[i].setEnabled(enabled);
        }
    }

    private void refreshAccountStats() {
        for (HostPluginWrapper wrapper : HostPluginWrapper.getHostWrapper()) {
            String host = wrapper.getHost();
            if (wrapper.isLoaded()) {
                ArrayList<Account> accs = new ArrayList<Account>(AccountController.getInstance().getAllAccounts(host));
                for (Account acc : accs) {
                    if (acc.isEnabled()) {
                        /*
                         * we do not force update here, the internal timeout
                         * will make sure accounts get fresh checked from time
                         * to time
                         */
                        AccountChecker.getInstance().check(acc, false);
                    }
                }
            }
        }
    }

    private void redraw() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                int ii = 0;
                try {
                    for (int i = 0; i < BARCOUNT; i++) {
                        bars[i].setVisible(false);
                    }
                    boolean enabled = false;
                    for (HostPluginWrapper wrapper : HostPluginWrapper.getHostWrapper()) {
                        String host = wrapper.getHost();
                        if (wrapper.isLoaded()) {
                            ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(host);
                            if (accs.size() > 0) {
                                PluginForHost plugin = wrapper.getPlugin();
                                long max = 0l;
                                long left = 0l;
                                enabled = false;
                                boolean special = false;
                                for (Account a : accs) {
                                    if (a.isEnabled()) {
                                        enabled = true;
                                        AccountInfo ai = a.getAccountInfo();
                                        if (ai == null) continue;
                                        if (ai.isExpired()) continue;
                                        if (!ai.isUnlimitedTraffic()) {
                                            /* no unlimited traffic */
                                            if (!a.isTempDisabled()) {
                                                /*
                                                 * only increase data if not
                                                 * temp. disabled
                                                 */
                                                max += ai.getTrafficMax();
                                                if (left != -1) {
                                                    /*
                                                     * only add TrafficLeft if
                                                     * no unlimted account
                                                     * available TODO: seperate
                                                     * normal and premium accs
                                                     */
                                                    left += ai.getTrafficLeft();
                                                    if (ai.isSpecialTraffic()) special = true;
                                                }
                                            }
                                        } else {
                                            /* left < 0 for unlimited */
                                            left = -1;
                                        }
                                    }
                                }
                                if (!enabled) continue;
                                bars[ii].setVisible(true);
                                bars[ii].setIcon(plugin.getHosterIconScaled());
                                bars[ii].setPlugin(plugin);

                                if (left == 0) {
                                    bars[ii].setMaximum(10);
                                    bars[ii].setValue(0);
                                    if (special) {
                                        bars[ii].setToolTipText(_GUI._.gui_premiumstatus_expired_maybetraffic_tooltip(host, accs.size()));
                                    } else {
                                        bars[ii].setToolTipText(_GUI._.gui_premiumstatus_expired_traffic_tooltip(host, accs.size()));
                                    }
                                } else if (left > 0) {
                                    bars[ii].setMaximum(max);
                                    bars[ii].setValue(left);
                                    bars[ii].setToolTipText(_GUI._.gui_premiumstatus_traffic_tooltip(host, accs.size(), Formatter.formatReadable(left)));
                                } else {
                                    /* left < 0 for unlimited */
                                    bars[ii].setMaximum(10);
                                    bars[ii].setValue(10);
                                    bars[ii].setToolTipText(_GUI._.gui_premiumstatus_unlimited_traffic_tooltip(host));
                                }
                                ii++;
                                if (ii >= BARCOUNT) {
                                    invalidate();
                                    return null;
                                }
                            }
                        }
                    }
                    invalidate();
                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
                return null;
            }
        }.start();
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() instanceof TinyProgressBar) {
            TinyProgressBar tpb = (TinyProgressBar) e.getSource();
            if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();
                ArrayList<MenuAction> entries = tpb.getPlugin().createMenuitems();
                for (MenuAction next : entries) {
                    JMenuItem mi = next.toJMenuItem();
                    if (mi == null) {
                        popup.addSeparator();
                    } else {
                        popup.add(mi);
                    }
                }
                popup.show(tpb, e.getPoint().x, e.getPoint().y);
            } else {
                UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, tpb.getPlugin().getConfig());
            }
        }

    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getCaller() instanceof Main) {
            JDUtilities.getController().removeControlListener(this);
            /* once gui init is complete we can add the listener */
            AccountController.getInstance().addListener(new AccountControllerListener() {

                public void onAccountControllerEvent(AccountControllerEvent event) {
                    switch (event.getEventID()) {
                    case AccountControllerEvent.ACCOUNT_ADDED:
                    case AccountControllerEvent.ACCOUNT_UPDATE:
                    case AccountControllerEvent.ACCOUNT_REMOVED:
                    case AccountControllerEvent.ACCOUNT_EXPIRED:
                    case AccountControllerEvent.ACCOUNT_INVALID:
                        redrawTimer.run();
                        break;
                    default:
                        break;
                    }
                }

            });
        }
    }

}