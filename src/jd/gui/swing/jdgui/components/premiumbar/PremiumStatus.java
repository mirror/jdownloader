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

import jd.Main;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.accountchecker.AccountChecker;
import jd.gui.UserIF;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;

public class PremiumStatus extends JPanel implements MouseListener {

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

        this.setOpaque(false);
        updateGUI(org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getDefaultValue());
        org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                updateGUI(newValue);
            }

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {

                    @Override
                    public void run() {
                        IOEQ.TIMINGQUEUE.scheduleWithFixedDelay(new Runnable() {

                            public void run() {
                                /*
                                 * this scheduleritem checks all enabled
                                 * accounts every 30 mins
                                 */
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
                                redraw();
                            }

                        };
                        redrawTimer.run();
                        AccountController.getInstance().addListener(new AccountControllerListener() {

                            public void onAccountControllerEvent(AccountControllerEvent event) {
                                switch (event.getEventID()) {
                                case AccountControllerEvent.ACCOUNT_ADDED:
                                case AccountControllerEvent.ACCOUNT_UPDATE:
                                case AccountControllerEvent.ACCOUNT_REMOVED:
                                case AccountControllerEvent.ACCOUNT_EXPIRED:
                                case AccountControllerEvent.ACCOUNT_INVALID:
                                    if (org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) redrawTimer.run();
                                    break;
                                default:
                                    break;
                                }
                            }

                        });
                    }

                }.start();
            }

        });
    }

    private void updateGUI(final boolean enabled) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (bars == null) return;
                for (int i = 0; i < BARCOUNT; i++) {
                    bars[i].setEnabled(enabled);
                }
            }
        };
    }

    private void refreshAccountStats() {

        for (LazyHostPlugin wrapper : HostPluginController.getInstance().list()) {
            String host = wrapper.getDisplayName();
            // if (wrapper.isLoaded()) {
            ArrayList<Account> accs = new ArrayList<Account>(AccountController.getInstance().getAllAccounts(host));
            for (Account acc : accs) {
                if (acc.isEnabled()) {
                    /*
                     * we do not force update here, the internal timeout will
                     * make sure accounts get fresh checked from time to time
                     */
                    AccountChecker.getInstance().check(acc, false);
                }
                // }
            }
        }
    }

    private void redraw() {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                int ii = 0;
                try {
                    for (int i = 0; i < BARCOUNT; i++) {
                        bars[i].setVisible(false);
                    }
                    boolean enabled = false;
                    for (LazyHostPlugin wrapper : HostPluginController.getInstance().list()) {
                        String host = wrapper.getDisplayName();

                        ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(host);
                        if (accs.size() > 0) {
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
                                             * only increase data if not temp.
                                             * disabled
                                             */
                                            max += ai.getTrafficMax();
                                            if (left != -1) {
                                                /*
                                                 * only add TrafficLeft if no
                                                 * unlimted account available
                                                 * TODO: seperate normal and
                                                 * premium accs
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
                            PluginForHost plugin = null;
                            try {
                                plugin = wrapper.getPrototype();
                                if (plugin == null) throw new WTFException();
                            } catch (final Throwable e) {
                                /*
                                 * in case something went wrong with prototype,
                                 * we disable the accounts
                                 */
                                Log.exception(e);
                                for (Account a : accs) {
                                    a.setEnabled(false);
                                }
                                continue;
                            }
                            bars[ii].setVisible(true);
                            bars[ii].setIcon(plugin.getHosterIcon());
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
                if (tpb.getPlugin().hasConfig()) {
                    UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, tpb.getPlugin().getConfig());
                }
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

}