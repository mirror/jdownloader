package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.jdgui.oboom.OboomDialog;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;

public class OboomController implements TopRightPainter, AccountControllerListener {

    private final AtomicBoolean          enabledByAPI            = new AtomicBoolean(false);
    private final AtomicBoolean          enabledInAdvancedConfig = new AtomicBoolean(false);
    private boolean                      mouseover;
    private AbstractIcon                 icon;
    private AbstractIcon                 close;
    private Rectangle                    closeBounds;
    private final AtomicBoolean          getProMode              = new AtomicBoolean(false);
    private AbstractIcon                 getproIcon;
    private boolean                      hasOtherAccountToRenew;
    private boolean                      hasDealAccountToRenew;
    private boolean                      hasOtherAccountToRenewAlreadyExpired;
    private boolean                      hasDealAccountToRenewAlreadyExpired;
    private volatile Account             accountToRenew;
    private boolean                      hasDealAccount          = false;
    private HashMap<String, Long>        expireNotifies;
    public static boolean                OFFER_IS_ACTIVE         = OboomController.readOfferActive();
    private static final OboomController INSTANCE                = new OboomController();

    /**
     * get the only existing instance of OboomController. This is a singleton
     * 
     * @return
     */
    public static OboomController getInstance() {
        return OboomController.INSTANCE;
    }

    /**
     * Create a new instance of OboomController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private OboomController() {
        close = new AbstractIcon("close", -1);
        expireNotifies = CFG_GUI.CFG.getPremiumExpireWarningMapV2();
        if (expireNotifies == null) {
            expireNotifies = new HashMap<String, Long>();
        }
        String key = "oboom/jdbanner_free_" + TranslationFactory.getDesiredLocale().getLanguage().toLowerCase(Locale.ENGLISH);
        if (!NewTheme.I().hasIcon(key)) {
            key = "oboom/jdbanner_free_en";
        }
        icon = new AbstractIcon(key, -1);

        key = "oboom/jdbanner_getpro_" + TranslationFactory.getDesiredLocale().getLanguage().toLowerCase(Locale.ENGLISH);
        if (!NewTheme.I().hasIcon(key)) {
            key = "oboom/jdbanner_getpro_en";
        }
        getproIcon = new AbstractIcon(key, -1);
        CFG_GUI.SPECIAL_DEALS_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                enabledInAdvancedConfig.set(Boolean.TRUE.equals(newValue));
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MainTabbedPane.getInstance().repaint();
                    }
                };
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        enabledInAdvancedConfig.set(CFG_GUI.SPECIAL_DEALS_ENABLED.isEnabled());
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                AccountController.getInstance().getBroadcaster().addListener(OboomController.this, true);
                onAccountControllerEvent(null);
            }
        });

    }

    @Override
    public Rectangle paint(Graphics2D g) {
        MainTabbedPane pane = MainTabbedPane.getInstance();
        if (isVisible()) {
            if (getProMode.get()) {
                Icon icon = getproIcon;
                icon.paintIcon(pane, g, pane.getWidth() - icon.getIconWidth() - 2, 22 - icon.getIconHeight());
                Rectangle specialDealBounds = new Rectangle(pane.getWidth() - icon.getIconWidth() - 2, 22 - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight() + 2);
                if (mouseover) {
                    g.setColor(Color.GRAY);
                    g.drawLine(specialDealBounds.x, specialDealBounds.y + specialDealBounds.height - 1, specialDealBounds.x + specialDealBounds.width - 2, specialDealBounds.y + specialDealBounds.height - 1);
                    g.setColor(Color.WHITE);
                    g.fillRect(pane.getWidth() - close.getIconWidth() - 3, 22 - icon.getIconHeight() - 2, 9, 11);
                    closeBounds = new Rectangle(pane.getWidth() - close.getIconWidth() - 3, 22 - icon.getIconHeight() - 2, 9, 11);
                    close.paintIcon(pane, g, pane.getWidth() - close.getIconWidth() - 2, 22 - icon.getIconHeight());
                }
                return specialDealBounds;
            } else {
                icon.paintIcon(pane, g, pane.getWidth() - icon.getIconWidth() - 2, 22 - icon.getIconHeight());
                Rectangle specialDealBounds = new Rectangle(pane.getWidth() - icon.getIconWidth() - 2, 22 - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight() + 2);
                if (mouseover) {
                    g.setColor(Color.GRAY);
                    g.drawLine(specialDealBounds.x, specialDealBounds.y + specialDealBounds.height - 1, specialDealBounds.x + specialDealBounds.width - 2, specialDealBounds.y + specialDealBounds.height - 1);
                    g.setColor(Color.WHITE);
                    g.fillRect(pane.getWidth() - close.getIconWidth() - 3, 22 - icon.getIconHeight() - 2, 9, 11);
                    closeBounds = new Rectangle(pane.getWidth() - close.getIconWidth() - 3, 22 - icon.getIconHeight() - 2, 9, 11);
                    close.paintIcon(pane, g, pane.getWidth() - close.getIconWidth() - 2, 22 - icon.getIconHeight());
                }
                return specialDealBounds;
            }
        }
        return null;
    }

    @Override
    public boolean isVisible() {
        return enabledInAdvancedConfig.get() && ((!hasDealAccount && is2DaysOfferVisible() && enabledByAPI.get()) || getProMode.get());
    }

    @Override
    public void onMouseOver(MouseEvent e) {
        MainTabbedPane pane = MainTabbedPane.getInstance();
        mouseover = true;
        pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void onMouseOut(MouseEvent e) {
        MainTabbedPane pane = MainTabbedPane.getInstance();
        mouseover = false;
        pane.setCursor(null);
    }

    @Override
    public void onClicked(MouseEvent e) {
        if (closeBounds != null && closeBounds.contains(e.getPoint())) {
            new Thread("DEAL_1") {
                public void run() {
                    try {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.OboomController_run_hide_title(), _GUI._.OboomController_run_hide_msg(), null, _GUI._.lit_yes(), null);
                        OboomController.track("TabbedHideClick/YES");
                        CFG_GUI.SPECIAL_DEALS_ENABLED.setValue(false);
                    } catch (DialogNoAnswerException e) {
                        OboomController.track("TabbedHideClick/NO");
                    }

                }
            }.start();
            return;
        }
        if (getProMode.get()) {

            new Thread("OSR") {
                public void run() {

                    ConfirmDialog d = null;
                    final Account laccountToRenew = accountToRenew;
                    if ((hasDealAccountToRenewAlreadyExpired || hasOtherAccountToRenewAlreadyExpired) && laccountToRenew != null) {
                        d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.OboomController_run_renew_title_expired(), _GUI._.OboomController_run_renew_msg_expired(laccountToRenew.getUser()), new AbstractIcon("logo_oboom_small", 32), _GUI._.lit_continue(), null) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }
                        };
                    } else if ((hasOtherAccountToRenew || hasDealAccountToRenew) && laccountToRenew != null) {
                        d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.OboomController_run_renew_title(), _GUI._.OboomController_run_renew_msg(laccountToRenew.getUser()), new AbstractIcon("logo_oboom_small", 32), _GUI._.lit_continue(), null) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }
                        };
                    } else {
                        d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.OboomController_run_renew_title_noaccount(), _GUI._.OboomController_run_renew_noaccount(), new AbstractIcon("logo_oboom_small", 32), _GUI._.lit_continue(), null) {
                            @Override
                            public ModalityType getModalityType() {
                                return ModalityType.MODELESS;
                            }
                        };
                    }
                    try {
                        Dialog.getInstance().showDialog(d);
                        OboomController.track("GETPRO_DIALOG/OK");
                    } catch (DialogClosedException e) {
                        OboomController.track("GETPRO_DIALOG/CLOSED");
                    } catch (DialogCanceledException e) {
                        OboomController.track("GETPRO_DIALOG/CANCELED");
                    }
                    CrossSystem.openURL("https://www.oboom.com/ref/501C81");

                }
            }.start();

        } else {

            new Thread("OSR") {
                public void run() {

                    OboomDialog d = new OboomDialog("tabclick");

                    UIOManager.I().show(null, d);
                    OboomController.track("TabbedClick");
                }
            }.start();
        }

    }

    public TopRightPainter start() {

        Thread thread = new Thread("Ask StatServ") {
            public void run() {

                while (true) {
                    Browser br = new Browser();
                    try {
                        br.getPage("http://stats.appwork.org/data/db/getDealStatus");
                        boolean newValue = false;
                        if (br.containsHTML("true") || !Application.isJared(null)) {
                            newValue = true;
                        }
                        enabledByAPI.set(newValue);
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                MainTabbedPane pane = MainTabbedPane.getInstance();
                                pane.repaint();
                            }
                        };
                        if (newValue && CFG_GUI.CFG.isSpecialDealOboomDialogVisibleOnStartup() && is2DaysOfferVisible() && !hasDealAccount) {
                            Thread.sleep(10000);
                            if (CFG_GUI.CFG.isSpecialDealOboomDialogVisibleOnStartup()) {
                                OboomDialog d = new OboomDialog("autopopup");
                                OboomController.track("Popup_10000");
                                UIOManager.I().show(null, d);
                                CFG_GUI.CFG.setSpecialDealOboomDialogVisibleOnStartup(false);
                            }

                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(60 * 60 * 1000l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            };
        };
        thread.setDaemon(true);
        thread.start();
        return this;
    }

    public static boolean is2DaysOfferVisible() {
        return OFFER_IS_ACTIVE;
    }

    public static void track(final String string) {
        StatsManager.I().track("specialdeals/oboom/" + string + "/jd2");
    }

    public static void writeRegistry(long value) {
        try {
            final Process p = Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal1\" /t REG_DWORD /d 0x" + Long.toHexString(value) + " /f");
            IO.readInputStreamToString(p.getInputStream());
            final int exitCode = p.exitValue();
            if (exitCode == 0) {
            } else {
                throw new IOException("Reg add execution failed");
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static int readRegistry() {
        try {
            final String iconResult = IO.readInputStreamToString(Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal1\"").getInputStream());
            final Matcher matcher = Pattern.compile("deal1\\s+REG_DWORD\\s+0x(.*)").matcher(iconResult);
            matcher.find();
            final String value = matcher.group(1);
            return Integer.parseInt(value, 16);
        } catch (Throwable e) {
            // e.printStackTrace();
            return -1;
        }
    }

    public static boolean readOfferActive() {
        switch (CrossSystem.getOSFamily()) {
        case WINDOWS:
            return OboomController.readRegistry() <= 0 && !Application.getTempResource("oboom1").exists();
        default:
            return !Application.getResource("cfg/deals.json").exists() && !Application.getTempResource("oboom1").exists();
        }
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        if (event == null || event.getAccount() != null && "oboom.com".equals(event.getAccount().getHoster())) {
            boolean hasDealAccountToRenewAlreadyExpired = false;
            boolean hasOtherAccountToRenewAlreadyExpired = false;
            boolean hasDealAccountToRenew = false;
            boolean hasOtherAccountToRenew = false;
            boolean hasOboomPremium = false;
            boolean hasDealAccount = false;
            Account renew = null;
            Account renewDeal = null;
            for (Account acc : AccountController.getInstance().list("oboom.com")) {

                long dealTime = acc.getLongProperty("DEAL", -1l);
                if (dealTime > 0) {
                    hasDealAccount = true;
                    AccountInfo accountInfo = acc.getAccountInfo();
                    if (accountInfo != null) {
                        long validUntil = accountInfo.getValidUntil();
                        if (validUntil <= 0) {
                            validUntil = acc.getLongProperty("PREMIUM_UNIX", -1);
                        }
                        long restPremium = validUntil - System.currentTimeMillis();
                        if (restPremium < 24 * 60 * 60 * 1000l) {
                            if (renewDeal == null) {
                                renewDeal = acc;
                            }
                            if (restPremium < 0) {
                                hasDealAccountToRenewAlreadyExpired = true;
                            }
                            // on day or less valid
                            hasDealAccountToRenew = true;
                        } else {
                            hasOboomPremium = true;
                        }
                    }
                } else {

                    AccountInfo accountInfo = acc.getAccountInfo();
                    if (accountInfo != null) {
                        long validUntil = accountInfo.getValidUntil();
                        if (validUntil <= 0) {
                            validUntil = acc.getLongProperty("PREMIUM_UNIX", -1);
                        }
                        long restPremium = validUntil - System.currentTimeMillis();
                        if (restPremium < 24 * 60 * 60 * 1000l) {
                            if (renew == null) {
                                renew = acc;
                            }
                            if (restPremium < 0) {
                                hasOtherAccountToRenewAlreadyExpired = true;
                            }
                            // on day or less valid
                            hasOtherAccountToRenew = true;
                        } else {
                            hasOboomPremium = true;
                        }
                    }
                }

            }
            if (renewDeal != null) {
                renew = renewDeal;
            }
            this.hasDealAccount = hasDealAccount;
            this.accountToRenew = renew;
            this.hasOtherAccountToRenewAlreadyExpired = hasOtherAccountToRenewAlreadyExpired;
            this.hasDealAccountToRenewAlreadyExpired = hasDealAccountToRenewAlreadyExpired;
            this.hasOtherAccountToRenew = hasOtherAccountToRenew;
            this.hasDealAccountToRenew = hasDealAccountToRenew;
            boolean showPro = !is2DaysOfferVisible() && (hasOtherAccountToRenew || hasDealAccountToRenew) && !hasOboomPremium;
            if (!is2DaysOfferVisible() && !hasOboomPremium && !hasOtherAccountToRenew && !hasDealAccountToRenew) {
                showPro = true;
            }
            if (getProMode.compareAndSet(!showPro, showPro)) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MainTabbedPane.getInstance().repaint();
                    }
                };
            }
        }
        long premiumUntil = -1;
        if (event != null && event.getAccount() != null && event.getAccount().getPlugin() != null && (premiumUntil = event.getAccount().getValidPremiumUntil()) > 0 && AccountControllerEvent.Types.ACCOUNT_CHECKED.equals(event.getType()) && CFG_GUI.CFG.isPremiumExpireWarningEnabled()) {
            try {
                final Account account = event.getAccount();
                final long rest = premiumUntil - System.currentTimeMillis();
                if (rest > 0 && rest < 1 * 24 * 60 * 60 * 1000l) {
                    boolean notify = false;
                    synchronized (this) {
                        final Long lastNotify = expireNotifies.get(account.getHoster());
                        // ask at max once a month
                        if (lastNotify == null || System.currentTimeMillis() - lastNotify > 30 * 24 * 60 * 60 * 1000l) {
                            notify = true;
                            expireNotifies.put(account.getHoster(), System.currentTimeMillis());
                            CFG_GUI.CFG.setPremiumExpireWarningMapV2(expireNotifies);
                        }
                    }
                    if (notify) {
                        notify(account, _GUI._.OboomController_onAccountControllerEvent_premiumexpire_warn_still_premium_title(account.getHoster()), _GUI._.OboomController_onAccountControllerEvent_premiumexpire_warn_still_premium_msg(account.getUser(), account.getHoster()));
                    }
                } else if (rest < 0 && rest > -7 * 24 * 60 * 60 * 1000l) {
                    boolean notify = false;
                    synchronized (this) {
                        final Long lastNotify = expireNotifies.get(account.getHoster());
                        // ask at max once a month
                        if (lastNotify == null || System.currentTimeMillis() - lastNotify > 30 * 24 * 60 * 60 * 1000l) {
                            notify = true;
                            expireNotifies.put(account.getHoster(), System.currentTimeMillis());
                            CFG_GUI.CFG.setPremiumExpireWarningMapV2(expireNotifies);
                        }
                    }
                    if (notify) {
                        notify(account, _GUI._.OboomController_onAccountControllerEvent_premiumexpire_warn_expired_premium_title(account.getHoster()), _GUI._.OboomController_onAccountControllerEvent_premiumexpire_warn_expired_premium_msg(account.getUser(), account.getHoster()));
                    }
                }
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
    }

    private void notify(final Account account, String title, String msg) {
        final LazyHostPlugin plg = account.getPlugin().getLazyP();
        final Icon fav = DomainInfo.getInstance(account.getHoster()).getFavIcon();
        final ExtMergedIcon hosterIcon = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_REFRESH, 32)).add(fav, 32 - fav.getIconWidth(), 32 - fav.getIconHeight());
        final ConfirmDialog d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, hosterIcon, _GUI._.lit_continue(), null) {
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
            StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/OK");
            CrossSystem.openURL(AccountController.createFullBuyPremiumUrl(plg.getPremiumUrl(), "PremiumExpireWarning/JD2"));
        } catch (DialogNoAnswerException e) {
            e.printStackTrace();
            StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/CANCELED");
        }
        if (d.isDontShowAgainSelected()) {
            StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/DONT_SHOW_AGAIN");
        }
    }
}
