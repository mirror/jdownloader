package org.jdownloader.gui.sponsor;

import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.AccountUpOrDowngradeEvent;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeVisitor;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.premium.OpenURLAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;

public class BannerRotation implements Sponsor, AccountControllerListener {
    private final List<AvailableBanner> allBanners = new CopyOnWriteArrayList<AvailableBanner>();

    private class AvailableBanner implements DownloadControllerListener, LinkCollectorListener, DownloadWatchdogListener, AccountControllerListener {
        private volatile boolean    hasDownloadLinks        = false;
        private volatile boolean    hasCrawledLinks         = false;
        private volatile boolean    hasLinks                = false;
        private volatile boolean    hasAccounts             = false;
        private volatile long       lastFreeDownloadSeen    = -1;
        private volatile long       lastPremiumDownloadSeen = -1;
        private volatile long       lastMultiDownloadSeen   = -1;
        private volatile long       lastAccountChangeSeen   = -1;
        private final AtomicBoolean hasChanges              = new AtomicBoolean(false);
        private long                lastUpdateTimestamp     = -1;
        private final DomainInfo    domainInfo;

        protected AvailableBanner(final DomainInfo domainInfo) {
            this.domainInfo = domainInfo;
            DownloadController.getInstance().getEventSender().addListener(this, true);
            LinkCollector.getInstance().getEventsender().addListener(this, true);
            DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
            AccountController.getInstance().getEventSender().addListener(this, true);
        }

        public String getHost() {
            return domainInfo.getTld();
        }

        public Icon getIcon() {
            return getIcon(TranslationFactory.getDesiredLanguage());
        }

        public Icon getIcon(String lng) {
            if (StringUtils.isEmpty(lng)) {
                final String iconKey = "banner/" + getHost() + "_en";
                return loadIcon(iconKey);
            } else {
                final String iconKey = "banner/" + getHost() + "_" + lng;
                final Icon ret = loadIcon(iconKey);
                if (ret != null) {
                    return ret;
                } else {
                    return getIcon(null);
                }
            }
        }

        public String getURL() {
            return getHost();
        }

        protected Icon loadIcon(final String relPath) {
            if (NewTheme.getInstance().hasIcon(relPath)) {
                try {
                    final URL iconURL = NewTheme.getInstance().getIconURL(relPath);
                    return new ImageIcon(ImageIO.read(iconURL));
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
            return null;
        }

        public DomainInfo getDomainInfo() {
            return domainInfo;
        }

        public void onClick(final MouseEvent e) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    new OpenURLAction(getDomainInfo(), "Banner12072017").actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
                }
            };
        }

        public String getPreSelectedInAddAccountDialog() {
            return getHost();
        }

        private boolean updateData() {
            if (System.currentTimeMillis() - lastUpdateTimestamp > 60 * 1000l) {
                final DomainInfo domainInfo = getDomainInfo();
                final boolean hasDownloadLinks = this.hasDownloadLinks;
                final boolean hasCrawledLinks = this.hasCrawledLinks;
                final boolean hasLinks = this.hasLinks;
                boolean hasRunningDownloadWithAccount = false;
                if (!hasDownloadLinks || lastDownloadLinkEnabled < lastUpdateTimestamp) {
                    boolean hasRunningDownload = false;
                    for (final SingleDownloadController singleDownloadController : DownloadWatchDog.getInstance().getRunningDownloadLinks()) {
                        final DownloadLinkCandidate candidate = singleDownloadController.getDownloadLinkCandidate();
                        if (domainInfo == candidate.getLink().getDomainInfo()) {
                            hasRunningDownload = true;
                            switch (candidate.getCachedAccount().getType()) {
                            case ORIGINAL:
                                switch (candidate.getCachedAccount().getAccount().getType()) {
                                case LIFETIME:
                                case PREMIUM:
                                    hasRunningDownloadWithAccount = true;
                                    break;
                                default:
                                    break;
                                }
                                break;
                            default:
                                break;
                            }
                        }
                    }
                    if (hasRunningDownload) {
                        this.hasDownloadLinks = true;
                    } else {
                        this.hasDownloadLinks = contains(DownloadController.getInstance(), domainInfo, Boolean.TRUE);
                    }
                }
                if (!hasCrawledLinks || lastCrawledLinkEnabled < lastUpdateTimestamp) {
                    this.hasCrawledLinks = contains(LinkCollector.getInstance(), domainInfo, Boolean.TRUE);
                }
                if (this.hasDownloadLinks || this.hasCrawledLinks) {
                    this.hasLinks = true;
                } else if (!hasLinks || lastDownloadLinkDisabled < lastUpdateTimestamp || lastCrawledLinkDisabled < lastUpdateTimestamp) {
                    this.hasLinks = contains(DownloadController.getInstance(), domainInfo, null) || contains(LinkCollector.getInstance(), domainInfo, null);
                }
                final boolean hasAccounts = this.hasAccounts;
                if (hasRunningDownloadWithAccount) {
                    this.hasAccounts = true;
                } else {
                    if (!hasAccounts || lastAccountChangeSeen < lastUpdateTimestamp) {
                        this.hasAccounts = false;
                        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
                        if (accounts != null) {
                            for (Account account : accounts) {
                                switch (account.getType()) {
                                case PREMIUM:
                                case LIFETIME:
                                    this.hasAccounts = true;
                                    break;
                                default:
                                    break;
                                }
                            }
                        }
                    }
                }
                lastUpdateTimestamp = System.currentTimeMillis();
                return hasAccounts != this.hasAccounts || hasLinks != this.hasLinks || hasDownloadLinks != this.hasDownloadLinks || hasCrawledLinks != this.hasCrawledLinks;
            } else {
                return false;
            }
        }

        private volatile long lastDownloadLinkEnabled  = -1;
        private volatile long lastDownloadLinkDisabled = -1;

        @Override
        public void onDownloadControllerAddedPackage(final FilePackage pkg) {
            final boolean readL = pkg.getModifyLock().readLock();
            try {
                for (final DownloadLink link : pkg.getChildren()) {
                    if (link != null && !AvailableStatus.FALSE.equals(link.getAvailableStatus()) && getDomainInfo() == link.getDomainInfo()) {
                        hasLinks = true;
                        if (link.isEnabled()) {
                            hasDownloadLinks = true;
                            lastDownloadLinkEnabled = System.currentTimeMillis();
                        } else {
                            lastDownloadLinkDisabled = System.currentTimeMillis();
                        }
                        onChange();
                        break;
                    }
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }

        @Override
        public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        }

        @Override
        public void onDownloadControllerStructureRefresh() {
        }

        @Override
        public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        }

        @Override
        public void onDownloadControllerRemovedPackage(FilePackage pkg) {
        }

        @Override
        public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        }

        @Override
        public void onDownloadControllerUpdatedData(DownloadLink link, DownloadLinkProperty property) {
            if (link != null && !AvailableStatus.FALSE.equals(link.getAvailableStatus()) && getDomainInfo() == link.getDomainInfo()) {
                hasLinks = true;
                if (link.isEnabled()) {
                    hasDownloadLinks = true;
                    lastDownloadLinkEnabled = System.currentTimeMillis();
                } else {
                    lastDownloadLinkDisabled = System.currentTimeMillis();
                }
                onChange();
            }
        }

        @Override
        public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        }

        @Override
        public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        }

        @Override
        public void onDownloadControllerUpdatedData(FilePackage pkg) {
        }

        @Override
        public void onLinkCollectorAbort(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
            if (event != null && event.getParameter() instanceof CrawledLink) {
                final CrawledLink link = (CrawledLink) event.getParameter();
                if (link != null && !AvailableLinkState.OFFLINE.equals(link.getLinkState()) && getDomainInfo() == link.getDomainInfo()) {
                    hasLinks = true;
                    if (link.isEnabled()) {
                        hasCrawledLinks = true;
                        lastCrawledLinkEnabled = System.currentTimeMillis();
                    } else {
                        lastCrawledLinkDisabled = System.currentTimeMillis();
                    }
                    onChange();
                }
            }
        }

        @Override
        public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        }

        private volatile long lastCrawledLinkEnabled  = -1;
        private volatile long lastCrawledLinkDisabled = -1;

        @Override
        public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink link) {
            if (link != null && !AvailableLinkState.OFFLINE.equals(link.getLinkState()) && getDomainInfo() == link.getDomainInfo()) {
                hasLinks = true;
                if (link.isEnabled()) {
                    hasCrawledLinks = true;
                    lastCrawledLinkEnabled = System.currentTimeMillis();
                } else {
                    lastCrawledLinkDisabled = System.currentTimeMillis();
                }
                onChange();
            }
        }

        @Override
        public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
            if (link != null && !AvailableLinkState.OFFLINE.equals(link.getLinkState()) && getDomainInfo() == link.getDomainInfo()) {
                hasLinks = true;
                if (link.isEnabled()) {
                    hasCrawledLinks = true;
                    lastCrawledLinkEnabled = System.currentTimeMillis();
                } else {
                    lastCrawledLinkDisabled = System.currentTimeMillis();
                }
                onChange();
            }
        }

        @Override
        public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
        }

        @Override
        public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
        }

        @Override
        public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
        }

        @Override
        public void onLinkCrawlerFinished() {
        }

        @Override
        public void onLinkCrawlerNewJob(LinkCollectingJob job) {
        }

        @Override
        public void onDownloadWatchdogDataUpdate() {
        }

        @Override
        public void onDownloadWatchdogStateIsIdle() {
        }

        @Override
        public void onDownloadWatchdogStateIsPause() {
        }

        @Override
        public void onDownloadWatchdogStateIsRunning() {
        }

        @Override
        public void onDownloadWatchdogStateIsStopped() {
        }

        @Override
        public void onDownloadWatchdogStateIsStopping() {
        }

        @Override
        public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
            if (getDomainInfo() == candidate.getLink().getDomainInfo()) {
                hasDownloadLinks = true;
                hasLinks = true;
                switch (candidate.getCachedAccount().getType()) {
                case NONE:
                    lastFreeDownloadSeen = System.currentTimeMillis();
                    break;
                case ORIGINAL:
                    switch (candidate.getCachedAccount().getAccount().getType()) {
                    case FREE:
                    case UNKNOWN:
                        lastFreeDownloadSeen = System.currentTimeMillis();
                        break;
                    default:
                        lastPremiumDownloadSeen = System.currentTimeMillis();
                        break;
                    }
                    break;
                case MULTI:
                    lastMultiDownloadSeen = System.currentTimeMillis();
                    break;
                default:
                    break;
                }
                onChange();
            }
        }

        @Override
        public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        }

        @Override
        public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
        }

        @Override
        public void onAccountControllerEvent(AccountControllerEvent event) {
            final Account acc = event.getAccount();
            if (acc != null && StringUtils.equalsIgnoreCase(getDomainInfo().getTld(), acc.getHoster())) {
                switch (event.getType()) {
                case ADDED:
                case ACCOUNT_UP_OR_DOWNGRADE:
                case ACCOUNT_CHECKED:
                    switch (acc.getType()) {
                    case LIFETIME:
                    case PREMIUM:
                        if (acc.isEnabled() && acc.isValid()) {
                            hasAccounts = true;
                            lastAccountChangeSeen = System.currentTimeMillis();
                            onChange();
                        }
                        break;
                    default:
                        break;
                    }
                    break;
                default:
                    break;
                }
            }
        }

        public boolean hasChanges() {
            return updateData() || hasChanges.getAndSet(false);
        }

        protected void onChange() {
            hasChanges.set(true);
            BannerRotation.this.onChange(this);
        }
    }

    private class Banner {
        private final Icon            icon;
        private final AvailableBanner banner;
        private final long            timeStamp = System.currentTimeMillis();

        private Banner(AvailableBanner banner) {
            this.icon = banner.getIcon();
            this.banner = banner;
        }

        private final long getTimestamp() {
            return timeStamp;
        }

        private final Icon getIcon() {
            return icon;
        }

        private final AvailableBanner getBanner() {
            return banner;
        }
    }

    private final MainTabbedPane pane;
    private volatile Banner      current = null;

    private void rotateBanner(List<AvailableBanner> rotateBanners) {
        final Banner old = current;
        try {
            if (rotateBanners != null && rotateBanners.size() == 0) {
                current = null;
            } else {
                if (rotateBanners == null) {
                    rotateBanners = getAllBanners();
                }
                if (rotateBanners == null || rotateBanners.size() == 0) {
                    current = null;
                } else if (old == null) {
                    current = new Banner(rotateBanners.get(0));
                } else {
                    if (System.currentTimeMillis() - old.getTimestamp() > bannerMinimumShowtime) {
                        final int index = rotateBanners.indexOf(old.banner);
                        if (index == -1 || index + 1 == rotateBanners.size() || rotateBanners.size() == 1) {
                            current = new Banner(rotateBanners.get(0));
                        } else {
                            current = new Banner(rotateBanners.get(index + 1));
                        }
                    }
                }
            }
        } finally {
            if (old != current) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        pane.repaint();
                    }
                };
            }
        }
    }

    private final AtomicBoolean   isBannerEnabled       = new AtomicBoolean(false);
    private final int             refreshTimeout        = 60 * 1000;
    private final int             bannerMinimumShowtime = 5 * 60 * 1000;
    private final DelayedRunnable updateDelayer;

    private List<AvailableBanner> updateRotation() {
        final ArrayList<AvailableBanner> rotation = new ArrayList<AvailableBanner>();
        final ArrayList<AvailableBanner> fallback = new ArrayList<AvailableBanner>();
        for (final AvailableBanner banner : allBanners) {
            if (banner.hasChanges()) {
                // required to process changes
            }
            if (!banner.hasAccounts) {
                if (banner.hasDownloadLinks) {
                    rotation.add(0, banner);
                } else if (banner.hasLinks) {
                    rotation.add(banner);
                } else {
                    fallback.add(banner);
                }
            }
        }
        if (rotation.size() > 0) {
            return rotation;
        } else {
            return fallback;
        }
    }

    private List<AvailableBanner> getAllBanners() {
        return allBanners;
    }

    public BannerRotation() {
        pane = MainTabbedPane.getInstance();
        updateDelayer = new DelayedRunnable(1000, 15000) {
            @Override
            public void delayedrun() {
                rotateBanner(updateRotation());
            }
        };
        final Thread refreshThread = new Thread() {
            {
                setDaemon(true);
            }

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(refreshTimeout);
                    } catch (InterruptedException e) {
                    }
                    onChange(null);
                }
            };
        };
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {
                            @Override
                            public void run() {
                                AccountController.getInstance().getEventSender().addListener(BannerRotation.this, true);
                            }
                        });
                        CFG_GUI.BANNER_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
                            @Override
                            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                                final boolean isEnabled = Boolean.TRUE.equals(newValue);
                                if (isBannerEnabled.getAndSet(isEnabled) != isEnabled) {
                                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setBannerChangeTimestamp(System.currentTimeMillis());
                                    if (!isEnabled) {
                                        StatsManager.I().track("various/BANNER/disabled");
                                        new EDTRunner() {
                                            @Override
                                            protected void runInEDT() {
                                                current = null;
                                                pane.repaint();
                                            }
                                        };
                                    } else {
                                        StatsManager.I().track("various/BANNER/enabled");
                                        updateDelayer.resetAndStart();
                                    }
                                }
                            }

                            @Override
                            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                            }
                        });
                        isBannerEnabled.set(CFG_GUI.BANNER_ENABLED.isEnabled());
                        getAllBanners().add(new AvailableBanner(DomainInfo.getInstance("uploaded.to")));
                        getAllBanners().add(new AvailableBanner(DomainInfo.getInstance("keep2share.cc")));
                        getAllBanners().add(new AvailableBanner(DomainInfo.getInstance("rapidgator.net")));
                        getAllBanners().add(new AvailableBanner(DomainInfo.getInstance("share-online.biz")));
                        getAllBanners().add(new AvailableBanner(DomainInfo.getInstance("filejoker.net")));
                        updateDelayer.resetAndStart();
                        refreshThread.start();
                    }
                }.start();
            }
        });
    }

    private void onChange(AvailableBanner banner) {
        if (isEnabled()) {
            updateDelayer.resetAndStart();
        }
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        try {
            if (event != null && AccountControllerEvent.Types.ACCOUNT_UP_OR_DOWNGRADE.equals(event.getType()) && CFG_GUI.CFG.isPremiumExpireWarningEnabled()) {
                final AccountUpOrDowngradeEvent accountEvent = (AccountUpOrDowngradeEvent) event;
                final long DAY = 24 * 60 * 60 * 1000l;
                final Account account = event.getAccount();
                final String hoster = account.getHoster();
                final long expireTimeStamp = accountEvent.getExpireTimeStamp();
                if (accountEvent.isPremiumAccount() && !accountEvent.isPremiumExpired()) {
                    if (expireTimeStamp > 0 && (expireTimeStamp - System.currentTimeMillis() < DAY)) {
                        boolean notify;
                        synchronized (this) {
                            final String ID = "warn_" + hoster;
                            HashMap<String, Long> expireNotifies = CFG_GUI.CFG.getPremiumExpireWarningMapV2();
                            final Long lastNotify = expireNotifies != null ? expireNotifies.get(ID) : null;
                            if (lastNotify == null || (System.currentTimeMillis() - lastNotify > (30 * DAY))) {
                                notify = true;
                                if (expireNotifies == null) {
                                    expireNotifies = new HashMap<String, Long>();
                                }
                                expireNotifies.put(ID, System.currentTimeMillis());
                                CFG_GUI.CFG.setPremiumExpireWarningMapV2(expireNotifies);
                            } else {
                                notify = false;
                            }
                        }
                        if (notify) {
                            notify(account, _GUI.T.OboomController_onAccountControllerEvent_premiumexpire_warn_still_premium_title(hoster), _GUI.T.OboomController_onAccountControllerEvent_premiumexpire_warn_still_premium_msg(account.getUser(), hoster));
                        }
                    }
                } else if (accountEvent.isPremiumDowngraded() && !AccountController.getInstance().hasAccount(hoster, true, true, true, false)) {
                    boolean notify;
                    synchronized (this) {
                        final String ID = "expired_" + hoster;
                        HashMap<String, Long> expireNotifies = CFG_GUI.CFG.getPremiumExpireWarningMapV2();
                        final Long lastNotify = expireNotifies != null ? expireNotifies.get(ID) : null;
                        // ask at max once a month
                        if (lastNotify == null || System.currentTimeMillis() - lastNotify > 30 * DAY) {
                            notify = true;
                            if (expireNotifies == null) {
                                expireNotifies = new HashMap<String, Long>();
                            }
                            expireNotifies.put(ID, System.currentTimeMillis());
                            CFG_GUI.CFG.setPremiumExpireWarningMapV2(expireNotifies);
                        } else {
                            notify = false;
                        }
                    }
                    if (notify) {
                        notify(account, _GUI.T.OboomController_onAccountControllerEvent_premiumexpire_warn_expired_premium_title(account.getHoster()), _GUI.T.OboomController_onAccountControllerEvent_premiumexpire_warn_expired_premium_msg(account.getUser(), account.getHoster()));
                    }
                }
                // }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
    }

    private void notify(final Account account, String title, String msg) {
        if (account != null) {
            final PluginForHost plugin = account.getPlugin();
            final String customURL;
            if (plugin == null) {
                customURL = "http://" + account.getHoster();
            } else {
                customURL = null;
            }
            final Icon fav = DomainInfo.getInstance(account.getHoster()).getFavIcon();
            final ExtMergedIcon hosterIcon = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_REFRESH, 32)).add(fav, 32 - fav.getIconWidth(), 32 - fav.getIconHeight());
            final ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, hosterIcon, _GUI.T.lit_continue(), _GUI.T.lit_close()) {
                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }

                @Override
                public String getDontShowAgainKey() {
                    return "expireRenewNotification_" + account.getHoster();
                }

                @Override
                public long getCountdown() {
                    return 5 * 60 * 1000l;
                }
            };
            try {
                Dialog.getInstance().showDialog(d);
                StatsManager.I().openAfflink(plugin, customURL, "PremiumExpireWarning/" + account.getHoster() + "/OK");
            } catch (DialogNoAnswerException e) {
                e.printStackTrace();
                StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/CANCELED");
            }
            if (d.isDontShowAgainSelected()) {
                StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/DONT_SHOW_AGAIN");
            }
        }
    }

    @Override
    public Rectangle paint(Graphics2D g) {
        final Banner banner = this.current;
        if (isVisible() && isEnabled() && banner != null && banner.icon != null) {
            final Icon icon = banner.getIcon();
            icon.paintIcon(pane, g, pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight() + 2);
            final Rectangle bannerRectangle = new Rectangle(pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight() + 2);
            return bannerRectangle;
        }
        return null;
    }

    private boolean isEnabled() {
        return isBannerEnabled.get();
    }

    @Override
    public boolean isVisible() {
        return this.current != null;
    }

    @Override
    public void onMouseOver(MouseEvent e) {
        if (isEnabled()) {
            pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void onMouseOut(MouseEvent e) {
        pane.setCursor(null);
    }

    @Override
    public String getPreSelectedInAddAccountDialog() {
        final Banner banner = this.current;
        if (banner != null) {
            return banner.getBanner().getPreSelectedInAddAccountDialog();
        } else {
            return "rapidgator.net";
        }
    }

    @Override
    public void onClicked(MouseEvent e) {
        final Banner banner = this.current;
        if (banner != null && isEnabled()) {
            banner.getBanner().onClick(e);
        }
    }

    private static <P extends AbstractPackageNode<C, P>, C extends AbstractPackageChildrenNode<P>> boolean contains(PackageController<P, C> controller, final DomainInfo domainInfo, final Boolean enabledFlag) {
        final AtomicBoolean ret = new AtomicBoolean(false);
        controller.visitNodes(new AbstractNodeVisitor<C, P>() {
            @Override
            public Boolean visitPackageNode(P pkg) {
                if (ret.get()) {
                    return null;
                } else if (enabledFlag == null || enabledFlag.booleanValue() == pkg.isEnabled()) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }

            @Override
            public Boolean visitChildrenNode(C node) {
                if ((enabledFlag == null || node.isEnabled() == enabledFlag.booleanValue()) && node.getDomainInfo() == domainInfo) {
                    ret.set(true);
                    return null;
                } else {
                    return Boolean.TRUE;
                }
            }
        }, true);
        return ret.get();
    }
}
