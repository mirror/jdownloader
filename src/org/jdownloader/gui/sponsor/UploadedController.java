package org.jdownloader.gui.sponsor;

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
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.TopRightPainter;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;

public class UploadedController implements AccountControllerListener, Sponsor {

    // private final AtomicBoolean enabledByAPI = new AtomicBoolean(false);
    private final AtomicBoolean             enabledInAdvancedConfig = new AtomicBoolean(false);
    private boolean                         mouseover;
    private AbstractIcon                    icon;
    private AbstractIcon                    close;
    private Rectangle                       closeBounds;

    private HashMap<String, Long>           expireNotifies;

    private static final UploadedController INSTANCE                = new UploadedController();

    /**
     * get the only existing instance of OboomController. This is a singleton
     * 
     * @return
     */
    public static UploadedController getInstance() {
        return UploadedController.INSTANCE;
    }

    /**
     * Create a new instance of OboomController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private UploadedController() {
        close = new AbstractIcon("close", -1);
        expireNotifies = CFG_GUI.CFG.getPremiumExpireWarningMapV2();
        if (expireNotifies == null) {
            expireNotifies = new HashMap<String, Long>();
        }
        String key = "uploaded/get_premium_" + TranslationFactory.getDesiredLocale().getLanguage().toLowerCase(Locale.ENGLISH);
        if (!NewTheme.I().hasIcon(key)) {
            key = "uploaded/get_premium_en";
        }
        icon = new AbstractIcon(key, -1);

        CFG_GUI.SPONSOR_UPLOADED_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

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
        enabledInAdvancedConfig.set(CFG_GUI.SPONSOR_UPLOADED_ENABLED.isEnabled());
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                AccountController.getInstance().getBroadcaster().addListener(UploadedController.this, true);
                onAccountControllerEvent(null);
            }
        });

    }

    @Override
    public Rectangle paint(Graphics2D g) {
        final MainTabbedPane pane = MainTabbedPane.getInstance();
        if (isVisible()) {

            if (icon != null && pane != null && close != null) {
                icon.paintIcon(pane, g, pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight() + 2);
                final Rectangle specialDealBounds = new Rectangle(pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight() + 2);
                if (mouseover) {
                    g.setColor(Color.GRAY);
                    g.drawLine(specialDealBounds.x + 50, specialDealBounds.y + specialDealBounds.height - 1 - 3, specialDealBounds.x + specialDealBounds.width - 2, specialDealBounds.y + specialDealBounds.height - 1 - 3);
                    g.setColor(Color.WHITE);
                    g.fillRect(pane.getWidth() - close.getIconWidth() - 3, 25 - icon.getIconHeight() - 2, 9, 11);
                    closeBounds = new Rectangle(pane.getWidth() - close.getIconWidth() - 3, 25 - icon.getIconHeight() - 2, 9, 11);
                    close.paintIcon(pane, g, pane.getWidth() - close.getIconWidth() - 2, 25 - icon.getIconHeight());
                }
                return specialDealBounds;
            }
        }

        return null;
    }

    @Override
    public boolean isVisible() {
        return enabledInAdvancedConfig.get();
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
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.Sponsor_run_hide_title(), _GUI._.Sponsor_run_hide_msg(), null, _GUI._.lit_yes(), null);
                        UploadedController.track("TabbedHideClick/YES");
                        CFG_GUI.SPONSOR_UPLOADED_ENABLED.setValue(false);
                    } catch (DialogNoAnswerException e) {
                        UploadedController.track("TabbedHideClick/NO");
                    }

                }
            }.start();
            return;
        }

        new Thread("OSR") {
            public void run() {
                CrossSystem.openURL("http://ul.to/ref/12859436");
                UploadedController.track("TabbedClick");
            }
        }.start();

    }

    public TopRightPainter start() {

        return this;
    }

    public static void track(final String string) {
        StatsManager.I().track("specialdeals/uploaded/" + string + "/jd2");
    }

    public static void writeRegistry(long value) {
        try {
            final Process p = Runtime.getRuntime().exec("reg add \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal2\" /t REG_DWORD /d 0x" + Long.toHexString(value) + " /f");
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
            final String iconResult = IO.readInputStreamToString(Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\JDownloader\" /v \"deal2\"").getInputStream());
            final Matcher matcher = Pattern.compile("deal2\\s+REG_DWORD\\s+0x(.*)").matcher(iconResult);
            matcher.find();
            final String value = matcher.group(1);
            return Integer.parseInt(value, 16);
        } catch (Throwable e) {
            // e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        try {
            // if (event == null || event.getAccount() != null && "uploaded.to".equals(event.getAccount().getHoster())) {

            // boolean hasOtherAccountToRenewAlreadyExpired = false;
            // boolean hasOtherAccountToRenew = false;
            // boolean hasPremium = false;
            // boolean hasDealAccount = false;
            // Account renew = null;
            // Account renewDeal = null;
            // for (Account acc : AccountController.getInstance().list("uploaded.to")) {
            //
            // long dealTime = acc.getLongProperty("DEAL", -1l);
            // if (dealTime > 0) {
            // hasDealAccount = true;
            // AccountInfo accountInfo = acc.getAccountInfo();
            // if (accountInfo != null) {
            // long validUntil = accountInfo.getValidUntil();
            // if (validUntil <= 0) {
            // validUntil = acc.getLongProperty("PREMIUM_UNIX", -1);
            // }
            // long restPremium = validUntil - System.currentTimeMillis();
            // if (restPremium < 24 * 60 * 60 * 1000l) {
            // if (renewDeal == null) {
            // renewDeal = acc;
            // }
            // if (restPremium < 0) {
            // // hasDealAccountToRenewAlreadyExpired = true;
            // }
            // // on day or less valid
            //
            // } else {
            // hasPremium = true;
            // }
            // }
            // } else {
            //
            // AccountInfo accountInfo = acc.getAccountInfo();
            // if (accountInfo != null) {
            // long validUntil = accountInfo.getValidUntil();
            // if (validUntil <= 0) {
            // validUntil = acc.getLongProperty("PREMIUM_UNIX", -1);
            // }
            // long restPremium = validUntil - System.currentTimeMillis();
            // if (restPremium < 24 * 60 * 60 * 1000l) {
            // if (renew == null) {
            // renew = acc;
            // }
            // if (restPremium < 0) {
            // hasOtherAccountToRenewAlreadyExpired = true;
            // }
            // // on day or less valid
            // hasOtherAccountToRenew = true;
            // } else {
            // hasPremium = true;
            // }
            // }
            // }
            //
            // }
            // if (renewDeal != null) {
            // renew = renewDeal;
            // }

            // this.accountToRenew = renew;
            // this.hasOtherAccountToRenewAlreadyExpired = hasOtherAccountToRenewAlreadyExpired;
            //
            // this.hasOtherAccountToRenew = hasOtherAccountToRenew;
            // this.hasDealAccountToRenew = hasDealAccountToRenew;
            // boolean showPro = !isOfferActiveByAPI() && (hasOtherAccountToRenew || hasDealAccountToRenew) && !hasPremium;
            // if (!isOfferActiveByAPI() && !hasPremium && !hasOtherAccountToRenew && !hasDealAccountToRenew) {
            // showPro = true;
            // }
            // if (getProMode.compareAndSet(!showPro, showPro)) {
            // new EDTRunner() {
            //
            // @Override
            // protected void runInEDT() {
            // MainTabbedPane.getInstance().repaint();
            // }
            // };
            // }

            if (event != null && event.getAccount() != null && event.getAccount().getPlugin() != null && AccountControllerEvent.Types.ACCOUNT_CHECKED.equals(event.getType()) && CFG_GUI.CFG.isPremiumExpireWarningEnabled()) {
                long premiumUntil = -1;
                premiumUntil = event.getAccount().getValidPremiumUntil();
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

                // }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
    }

    private void notify(final Account account, String title, String msg) {
        final PluginForHost plugin = account.getPlugin();
        String url = null;
        if (plugin == null || StringUtils.isEmpty(url = plugin.getBuyPremiumUrl())) {
            return;
        }
        final Icon fav = DomainInfo.getInstance(account.getHoster()).getFavIcon();
        final ExtMergedIcon hosterIcon = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_REFRESH, 32)).add(fav, 32 - fav.getIconWidth(), 32 - fav.getIconHeight());
        final ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, msg, hosterIcon, _GUI._.lit_continue(), _GUI._.lit_close()) {
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
            CrossSystem.openURL("http://ul.to/ref/12859436");
        } catch (DialogNoAnswerException e) {
            e.printStackTrace();
            StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/CANCELED");
        }
        if (d.isDontShowAgainSelected()) {
            StatsManager.I().track("PremiumExpireWarning/" + account.getHoster() + "/DONT_SHOW_AGAIN");
        }
    }

    @Override
    public String getPreSelectedInAddAccountDialog() {
        return "uploaded.to";
    }
}
