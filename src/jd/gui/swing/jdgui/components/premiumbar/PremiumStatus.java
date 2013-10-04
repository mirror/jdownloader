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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.accountchecker.AccountChecker;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.DomainInfo;
import org.jdownloader.settings.GeneralSettings;

public class PremiumStatus extends JPanel implements MouseListener {

    private static final long    serialVersionUID = 7290466989514173719L;

    private DelayedRunnable      redrawTimer;
    private static PremiumStatus INSTANCE         = new PremiumStatus();

    public static PremiumStatus getInstance() {
        return INSTANCE;
    }

    private PremiumStatus() {
        super(new MigLayout("ins 0 2 0", "0[]0[]0[]0[]0", "0[]0"));

        this.setOpaque(false);

        redraw();
        org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                redraw();
            }

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {

                    @Override
                    public void run() {
                        ScheduledExecutorService scheduler = DelayedRunnable.getNewScheduledExecutorService();
                        scheduler.scheduleWithFixedDelay(new Runnable() {

                            public void run() {
                                /*
                                 * this scheduleritem checks all enabled accounts every 5 mins
                                 */
                                try {
                                    refreshAccountStats();
                                } catch (Throwable e) {
                                    Log.exception(e);
                                }
                            }

                        }, 1, 5, TimeUnit.MINUTES);
                        redrawTimer = new DelayedRunnable(scheduler, 1000, 5000) {

                            @Override
                            public String getID() {
                                return "PremiumStatusRedraw";
                            }

                            @Override
                            public void delayedrun() {
                                redraw();
                            }

                        };
                        redrawTimer.run();
                        AccountController.getInstance().getBroadcaster().addListener(new AccountControllerListener() {

                            public void onAccountControllerEvent(AccountControllerEvent event) {

                                if (org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) redrawTimer.run();
                            }
                        });
                    }
                }.start();
            }
        });
    }

    // private void updateGUI(final boolean enabled) {
    //
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    // if (bars == null) return;
    // for (int i = 0; i < bars.length; i++) {
    // bars[i].setEnabled(enabled);
    // }
    // }
    // };
    // }

    private void refreshAccountStats() {
        for (Account acc : AccountController.getInstance().list()) {
            if (acc.isEnabled() && acc.refreshTimeoutReached()) {
                /*
                 * we do not force update here, the internal timeout will make sure accounts get fresh checked from time to time
                 */
                AccountChecker.getInstance().check(acc, false);
            }
        }
    }

    void redraw() {
        List<Account> accs = AccountController.getInstance().list();
        Collections.sort(accs, new Comparator<Account>() {

            @Override
            public int compare(Account o1, Account o2) {
                int ret = new Boolean(o2.isEnabled()).compareTo(new Boolean(o1.isEnabled()));
                if (ret == 0) {
                    if (o2.isEnabled()) {
                        // sort on name
                        ret = o1.getHoster().compareTo(o2.getHoster());
                    } else {

                        // last enabled one should be the first
                        ret = new Long(o2.getLastValidTimestamp()).compareTo(o1.getLastValidTimestamp());
                    }
                }
                return ret;
            }
        });
        HashMap<String, DomainInfo> domainInfos = new HashMap<String, DomainInfo>();
        final HashSet<DomainInfo> enabled = new HashSet<DomainInfo>();
        final LinkedList<DomainInfo> domains = new LinkedList<DomainInfo>();
        for (Account acc : accs) {
            AccountInfo ai = acc.getAccountInfo();
            // if (!acc.isEnabled() || !acc.isValid() || (ai != null && ai.isExpired())) continue;
            DomainInfo domainInfo = domainInfos.get(acc.getHoster());
            if (domainInfo == null) {
                PluginForHost plugin = JDUtilities.getPluginForHost(acc.getHoster());
                if (plugin != null) {
                    domainInfo = plugin.getDomainInfo(null);
                    domainInfos.put(acc.getHoster(), domainInfo);
                    domains.add(domainInfo);

                }

            }
            if (acc.isEnabled() && acc.isValid() && !(ai != null && ai.isExpired())) {

                enabled.add(domainInfo);
            }
            /* prefetch outside EDT */
            domainInfo.getFavIcon();
        }
        domainInfos = null;
        accs = null;
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                try {
                    removeAll();

                    int max = Math.min(domains.size(), JsonConfig.create(GeneralSettings.class).getMaxPremiumIcons());
                    StringBuilder sb = new StringBuilder();
                    sb.append("2");
                    for (int i = 0; i < max; i++) {
                        sb.append("[22!]0");
                    }
                    setLayout(new MigLayout("ins 0 2 0 0", sb.toString(), "[22!]"));
                    for (int i = 0; i < max; i++) {
                        DomainInfo di;
                        TinyProgressBar bar = new TinyProgressBar(PremiumStatus.this, di = domains.removeFirst());
                        add(bar, "gapleft 0,gapright 0");
                        bar.setEnabled(enabled.contains(di) && org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getValue());

                    }
                    revalidate();
                    repaint();
                } catch (final Throwable e) {
                    Log.exception(e);
                }
                invalidate();
                return null;
            }
        }.start();
    }

    public void mouseClicked(MouseEvent e) {
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