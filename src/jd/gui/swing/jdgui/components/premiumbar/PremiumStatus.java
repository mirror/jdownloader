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

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.Main;
import jd.config.ConfigGroup;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.MenuAction;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.menu.HosterMenu;
import jd.gui.swing.menu.Menu;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class PremiumStatus extends JPanel implements AccountControllerListener, ActionListener, MouseListener, ControlListener {

    private static final long serialVersionUID = 7290466989514173719L;
    private static final int BARCOUNT = 15;
    private static final long ACCOUNT_UPDATE_DELAY = 15 * 60 * 1000;
    private TinyProgressBar[] bars;
    private JLabel lbl;

    private boolean redrawinprogress = false;
    private JToggleButton premium;
    private boolean updating = false;
    private Timer updateIntervalTimer;
    private boolean updateinprogress = false;
    private JPopupMenu popup;
    private ArrayList<HostPluginWrapper> hosterplugins;
    private boolean guiInitComplete = false;

    public PremiumStatus() {
        super();
        hosterplugins = JDUtilities.getPluginsForHost();
        bars = new TinyProgressBar[BARCOUNT];
        lbl = new JLabel(JDL.L("gui.statusbar.premiumloadlabel", "< Add Accounts"));
        setName(JDL.L("quickhelp.premiumstatusbar", "Premium statusbar"));
        this.setLayout(new MigLayout("ins 0", "", "[::20, center]"));
        premium = new JToggleButton();
        premium.setToolTipText(JDL.L("gui.menu.action.premium.desc", "Enable Premiumusage globally"));

        premium.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!premium.isSelected()) {
                    int answer = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.NO_COUNTDOWN, JDL.L("dialogs.premiumstatus.global.title", "Disable Premium?"), JDL.L("dialogs.premiumstatus.global.message", "Do you really want to disable all premium accounts?"), JDTheme.II("gui.images.warning", 32, 32), JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
                    if (JDFlags.hasAllFlags(answer, UserIO.RETURN_CANCEL) && !JDFlags.hasAllFlags(answer, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                        premium.setSelected(true);
                        return;
                    }
                }

                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                JDUtilities.getConfiguration().save();
            }

        });
        premium.setFocusPainted(false);
        premium.setContentAreaFilled(false);
        premium.setBorderPainted(false);
        updatePremiumButton();
        add(premium, "hmax 20");
        premium.addMouseListener(this);
        add(new JSeparator(JSeparator.VERTICAL), "growy");
        add(lbl, "hidemode 3");

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

    private synchronized void updatePremium() {
        updating = true;
        for (HostPluginWrapper wrapper : hosterplugins) {
            String host = wrapper.getHost();
            if (wrapper.isLoaded() && wrapper.usePlugin()) {
                ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(host);
                for (Account a : accs) {
                    if (a.isEnabled()) {
                        AccountController.getInstance().updateAccountInfo(host, a, false);
                    }
                }
            }
        }
        updating = false;
    }

    private synchronized void redraw() {
        if (redrawinprogress) return;
        redrawinprogress = true;
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                int ii = 0;
                try {
                    lbl.setVisible(false);
                    for (int i = 0; i < BARCOUNT; i++) {
                        bars[i].setVisible(false);
                    }
                    boolean enabled = false;
                    for (HostPluginWrapper wrapper : hosterplugins) {
                        String host = wrapper.getHost();
                        if (wrapper.isLoaded() && wrapper.usePlugin()) {
                            ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(host);
                            if (accs.size() > 0) {
                                PluginForHost plugin = wrapper.getPlugin();
                                long max = 0l;
                                long left = 0l;
                                long workingaccs = 0;
                                enabled = false;
                                for (Account a : accs) {
                                    if (a.isEnabled()) {
                                        enabled = true;
                                        AccountInfo ai = a.getAccountInfo();
                                        if (ai == null) continue;
                                        max += ai.getTrafficMax();
                                        left += ai.getTrafficLeft();
                                        if (!a.isTempDisabled()) workingaccs++;
                                    }
                                }
                                if (!enabled) continue;
                                bars[ii].setVisible(true);
                                bars[ii].setIcon(plugin.getHosterIcon());
                                bars[ii].setAlignmentX(RIGHT_ALIGNMENT);
                                bars[ii].setPlugin(plugin);

                                if (left == 0 || workingaccs == 0) {
                                    bars[ii].setMaximum(10);
                                    bars[ii].setValue(0);
                                    bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.expired_traffic.tooltip", "%s - %s account(s) -- At the moment no premium traffic is available.", host, accs.size()));
                                } else if (left > 0) {
                                    bars[ii].setMaximum(max);
                                    bars[ii].setValue(left);
                                    bars[ii].setToolTipText(JDL.LF("gui.premiumstatus.traffic.tooltip", "%s - %s account(s) -- You can download up to %s today.", host, accs.size(), Formatter.formatReadable(left)));
                                } else {
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
            public void run() {
                this.setName("PremiumStatus: update");
                updateinprogress = true;
                if (!updating && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) updatePremium();
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

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == premium) {
            if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

                if (popup == null) {
                    popup = new JPopupMenu();
                    HosterMenu.update(popup);
                }
                popup.show(premium, e.getPoint().x, e.getPoint().y);

            }
            return;
        }
        for (int i = 0; i < BARCOUNT; i++) {
            if (bars[i] == e.getSource()) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

                    JPopupMenu popup = new JPopupMenu();

                    ArrayList<MenuAction> entries = bars[i].getPlugin().createMenuitems();
                    for (MenuAction next : entries) {
                        JMenuItem mi = Menu.getJMenuItem(next);
                        if (mi == null) {
                            popup.addSeparator();
                        } else {
                            popup.add(mi);
                        }
                    }
                    popup.show(bars[i], e.getPoint().x, e.getPoint().y);

                } else {
                    bars[i].getPlugin().getConfig().setGroup(new ConfigGroup(bars[i].getPlugin().getHost(), JDTheme.II("gui.images.taskpanes.premium", 24, 24)));

                    UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, bars[i].getPlugin().getConfig());
                }
                return;
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
