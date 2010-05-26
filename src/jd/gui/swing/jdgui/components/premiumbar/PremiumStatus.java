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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.Main;
import jd.config.Configuration;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIF;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements AccountControllerListener, ActionListener, MouseListener, ControlListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final int BARCOUNT = 15;
    private static final long ACCOUNT_UPDATE_DELAY = 30 * 60 * 1000;
    private TinyProgressBar[] bars;

    private boolean redrawinprogress = false;
    private JToggleButton premium;
    private boolean updating = false;
    private Timer updateIntervalTimer;
    private boolean updateinprogress = false;
    private boolean guiInitComplete = false;

    private static PremiumStatus INSTANCE = null;

    public static PremiumStatus getInstance() {
        if (INSTANCE == null) INSTANCE = new PremiumStatus();
        return INSTANCE;
    }

    private PremiumStatus() {
        super(new MigLayout("ins 0", "", "[::20, center]"));
        bars = new TinyProgressBar[BARCOUNT];
        setName("Premium Statusbar");
        premium = new JToggleButton(ActionController.getToolBarAction("premiumMenu.toggle"));
        premium.setHideActionText(true);
        premium.setFocusPainted(false);
        premium.setContentAreaFilled(false);
        premium.setBorderPainted(false);
        add(premium, "hmax 20");
        add(new JSeparator(JSeparator.VERTICAL), "growy");

        for (int i = 0; i < BARCOUNT; i++) {
            bars[i] = new TinyProgressBar();
            bars[i].setOpaque(false);
            bars[i].addMouseListener(this);
            bars[i].setEnabled(premium.isSelected());
            bars[i].setVisible(false);
            add(bars[i], "hidemode 3, hmax 20");
        }
        this.setOpaque(false);
        updateIntervalTimer = new Timer(5000, this);
        updateIntervalTimer.setInitialDelay(5000);
        updateIntervalTimer.setRepeats(false);
        updateIntervalTimer.stop();

        JDUtilities.getController().addControlListener(this);

        Thread updateTimer = new Thread("PremiumStatusUpdateTimer") {
            @Override
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

    public void updateGUI(boolean enabled) {
        for (int i = 0; i < BARCOUNT; i++) {
            if (bars[i] != null) {
                bars[i].setEnabled(enabled);
            }
        }
    }

    private synchronized void updatePremium() {
        updating = true;
        String id = JDController.requestDelayExit("updatePremium");
        for (HostPluginWrapper wrapper : HostPluginWrapper.getHostWrapper()) {
            String host = wrapper.getHost();
            if (wrapper.isLoaded() && wrapper.isEnabled()) {
                ArrayList<Account> accs = new ArrayList<Account>(AccountController.getInstance().getAllAccounts(host));
                for (Account a : accs) {
                    AccountController.getInstance().updateAccountInfo(host, a, false);
                }
            }
        }
        JDController.releaseDelayExit(id);
        updating = false;
    }

    private synchronized void redraw() {
        if (redrawinprogress) return;
        redrawinprogress = true;
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
                        if (wrapper.isLoaded() && wrapper.isEnabled()) {
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
                                            left = -1;/* left <0 for unlimited */
                                        }
                                    }
                                }
                                if (!enabled) continue;
                                bars[ii].setVisible(true);
                                bars[ii].setIcon(plugin.getHosterIconScaled());
                                bars[ii].setAlignmentX(RIGHT_ALIGNMENT);
                                bars[ii].setPlugin(plugin);

                                if (left == 0) {
                                    bars[ii].setMaximum(10);
                                    bars[ii].setValue(0);
                                    if (special) {
                                        bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.expired_maybetraffic.tooltip", "%s - %s account(s) -- At the moment it may be that no premium traffic is left.", host, accs.size()));
                                    } else {
                                        bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.expired_traffic.tooltip", "%s - %s account(s) -- At the moment no premium traffic is available.", host, accs.size()));
                                    }
                                } else if (left > 0) {
                                    bars[ii].setMaximum(max);
                                    bars[ii].setValue(left);
                                    bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.traffic.tooltip", "%s - %s account(s) -- You can download up to %s today.", host, accs.size(), Formatter.formatReadable(left)));
                                } else {
                                    /* left <0 for unlimited */
                                    bars[ii].setMaximum(10);
                                    bars[ii].setValue(10);
                                    bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.unlimited_traffic.tooltip", "%s -- Unlimited traffic! You can download as much as you want to.", host));
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
                    return null;
                } finally {
                    redrawinprogress = false;
                }
            }
        }.start();
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
        case AccountControllerEvent.ACCOUNT_EXPIRED:
        case AccountControllerEvent.ACCOUNT_INVALID:
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
        if (updateinprogress || !guiInitComplete) return;
        new Thread() {
            @Override
            public void run() {
                this.setName("PremiumStatus: update");
                updateinprogress = true;
                if (!updating && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) updatePremium();
                redraw();
                updateinprogress = false;
            }
        }.start();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.updateIntervalTimer) {
            doUpdate();
        }
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
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            guiInitComplete = true;
            JDUtilities.getController().removeControlListener(this);
            requestUpdate();
        }
    }

}
