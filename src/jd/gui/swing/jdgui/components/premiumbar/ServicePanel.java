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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JPanel;

import jd.SecondLevelLaunch;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.accountchecker.AccountChecker;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.PremiumStatusBarDisplay;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class ServicePanel extends JPanel implements MouseListener, AccountTooltipOwner {

    private static final long               serialVersionUID = 7290466989514173719L;

    private DelayedRunnable                 redrawTimer;

    private ArrayList<ServicePanelExtender> extender;
    private static ServicePanel             INSTANCE         = new ServicePanel();
    private AtomicBoolean                   redrawing        = new AtomicBoolean(false);

    public static ServicePanel getInstance() {
        return INSTANCE;
    }

    public void addExtender(ServicePanelExtender ex) {
        synchronized (extender) {
            extender.remove(ex);
            extender.add(ex);

        }
        redrawTimer.delayedrun();
    }

    public void requestUpdate(boolean immediately) {
        if (immediately) {
            redrawTimer.delayedrun();
        } else {
            redraw();
        }
    }

    public void removeExtender(ServicePanelExtender ex) {
        synchronized (extender) {
            extender.remove(ex);
        }
        redrawTimer.delayedrun();
    }

    private ServicePanel() {
        super(new MigLayout("ins 0 2 0", "0[]0[]0[]0[]0", "0[]0"));

        extender = new ArrayList<ServicePanelExtender>();
        this.setOpaque(false);
        final ScheduledExecutorService scheduler = DelayedRunnable.getNewScheduledExecutorService();

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
        redraw();

        CFG_GUI.PREMIUM_STATUS_BAR_DISPLAY.getEventSender().addListener(new GenericConfigEventListener<Enum>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                redraw();
            }
        });
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
                        redrawTimer.run();
                        AccountController.getInstance().getBroadcaster().addListener(new AccountControllerListener() {

                            public void onAccountControllerEvent(AccountControllerEvent event) {

                                if (org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.isEnabled()) redrawTimer.run();
                            }
                        });
                        redraw();
                    }
                }.start();
            }
        });
    }

    private void refreshAccountStats() {
        for (Account acc : AccountController.getInstance().list()) {
            if (acc.isEnabled() && acc.isValid() && acc.refreshTimeoutReached()) {
                /*
                 * we do not force update here, the internal timeout will make sure accounts get fresh checked from time to time
                 */
                AccountChecker.getInstance().check(acc, false);
            }
        }
    }

    public void redraw() {
        if (SecondLevelLaunch.ACCOUNTLIST_LOADED.isReached()) {
            if (redrawing.compareAndSet(false, true)) {
                try {
                    final LinkedList<ServiceCollection<?>> services = groupServices(CFG_GUI.CFG.getPremiumStatusBarDisplay(), true, null);
                    new EDTHelper<Object>() {
                        @Override
                        public Object edtRun() {
                            try {
                                try {
                                    removeAll();

                                    int max = services.size();
                                    // Math.min(, JsonConfig.create(GeneralSettings.class).getMaxPremiumIcons());
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("2");
                                    for (int i = 0; i < max; i++) {
                                        sb.append("[22!]0");
                                    }
                                    setLayout(new MigLayout("ins 0 2 0 0", sb.toString(), "[22!]"));
                                    for (int i = 0; i < max; i++) {
                                        JComponent c = services.removeFirst().createIconComponent(ServicePanel.this);
                                        if (c != null) {
                                            add(c, "gapleft 0,gapright 0");
                                        }

                                    }
                                    revalidate();
                                    repaint();
                                } catch (final Throwable e) {
                                    Log.exception(e);
                                }
                                invalidate();
                            } finally {
                                redrawing.compareAndSet(true, false);
                            }
                            return null;
                        }
                    }.start();
                } catch (final Throwable e) {
                    redrawing.compareAndSet(true, false);
                }
            }
        }
    }

    public LinkedList<ServiceCollection<?>> groupServices(PremiumStatusBarDisplay premiumStatusBarDisplay, boolean extend, String filter) {
        List<Account> accs = AccountController.getInstance().list();

        // final HashSet<DomainInfo> enabled = new HashSet<DomainInfo>();
        final HashMap<String, AccountServiceCollection> map = new HashMap<String, AccountServiceCollection>();
        final LinkedList<ServiceCollection<?>> services = new LinkedList<ServiceCollection<?>>();
        HashMap<String, LazyHostPlugin> plugins = new HashMap<String, LazyHostPlugin>();
        for (Account acc : accs) {
            AccountInfo ai = acc.getAccountInfo();

            if (acc.getLastValidTimestamp() < 0 && acc.getError() != null) continue;
            if ((System.currentTimeMillis() - acc.getLastValidTimestamp()) < 14 * 7 * 24 * 60 * 60 * 1000 && acc.getError() != null) continue;
            PluginForHost plugin = acc.getPlugin();
            DomainInfo domainInfo;
            if (plugin != null) {
                domainInfo = plugin.getDomainInfo(null);
                domainInfo.getFavIcon();

                AccountServiceCollection ac;
                switch (premiumStatusBarDisplay) {
                case DONT_GROUP:
                    if (filter != null && !filter.equals(domainInfo.getTld())) continue;
                    ac = new AccountServiceCollection(domainInfo);
                    ac.add(acc);
                    services.add(ac);
                    break;
                case GROUP_BY_ACCOUNT_TYPE:
                    if (filter != null && !filter.equals(domainInfo.getTld())) continue;
                    ac = map.get(domainInfo.getTld());
                    if (ac == null) {
                        ac = new AccountServiceCollection(domainInfo);
                        map.put(domainInfo.getTld(), ac);
                        services.add(ac);
                    }
                    ac.add(acc);
                    break;

                case GROUP_BY_SUPPORTED_HOSTS:

                    ai = acc.getAccountInfo();
                    if (ai == null) continue;
                    Object supported = null;
                    synchronized (ai) {
                        /*
                         * synchronized on accountinfo because properties are not threadsafe
                         */
                        supported = ai.getProperty("multiHostSupport", Property.NULL);
                    }
                    if (Property.NULL == supported || supported == null) {
                        // dedicated account
                        if (filter != null && !filter.equals(domainInfo.getTld())) continue;
                        ac = map.get(domainInfo.getTld());
                        if (ac == null) {
                            ac = new AccountServiceCollection(domainInfo);
                            map.put(domainInfo.getTld(), ac);
                            services.add(ac);
                        }
                        ac.add(acc);
                    } else {
                        synchronized (supported) {
                            /*
                             * synchronized on list because plugins can change the list in runtime
                             */

                            if (supported instanceof ArrayList) {
                                for (String sup : (java.util.List<String>) supported) {

                                    LazyHostPlugin plg = HostPluginController.getInstance().get((String) sup);

                                    if (plg != null) {
                                        LazyHostPlugin cached = plugins.get(plg.getClassname());
                                        if (cached != null) plg = cached;
                                        plugins.put(plg.getClassname(), plg);
                                        sup = plg.getHost();
                                    } else {
                                        //
                                        System.out.println(plg);
                                        continue;
                                    }
                                    if (filter != null && !filter.equals(sup)) continue;
                                    ac = map.get(sup);
                                    if (ac == null) {
                                        ac = new AccountServiceCollection(DomainInfo.getInstance(sup));
                                        map.put(sup, ac);
                                        services.add(ac);
                                    }
                                    ac.add(acc);
                                }
                            }
                        }
                    }

                    break;
                }

            }

            /* prefetch outside EDT */

        }
        if (extend) {
            synchronized (extender) {
                for (ServicePanelExtender bla : extender) {
                    bla.extendServicePabel(services);
                }

            }
        }

        Collections.sort(services);
        return services;
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