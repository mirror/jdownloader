package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Locale;
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
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;

public class OboomController implements TopRightPainter, AccountControllerListener {

    protected boolean                    visible;
    private boolean                      enabled         = false;
    private boolean                      mouseover;
    private AbstractIcon                 icon;
    private LogSource                    logger;
    private AbstractIcon                 close;
    private Rectangle                    closeBounds;
    private boolean                      getProMode      = false;
    private AbstractIcon                 getproIcon;
    private boolean                      hasPremium;
    private boolean                      hasOtherAccountToRenew;
    private boolean                      hasDealAccountToRenew;
    private boolean                      hasOtherAccountToRenewAlreadyExpired;
    private boolean                      hasDealAccountToRenewAlreadyExpired;
    private Account                      accountToRenew;
    public static boolean                OFFER_IS_ACTIVE = OboomController.readOfferActive();
    private static final OboomController INSTANCE        = new OboomController();

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
        logger = LogController.getInstance().getLogger("OboomDeal");

        close = new AbstractIcon("close", -1);
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
                enabled = newValue;
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
        enabled = CFG_GUI.SPECIAL_DEALS_ENABLED.getValue();
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
            if (getProMode) {
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
                } else {

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
                } else {

                }
                return specialDealBounds;
            }

        }

        return null;
    }

    @Override
    public boolean isVisible() {
        if (!enabled) {
            return false;
        }
        if (isOfferActive() && !getProMode) {
            // user did not participate yet
            return true;
        }
        if (getProMode) {
            return true;
        }
        return false;
    }

    // private boolean isPaintSpecialDealReminder() {
    // return (OboomDialog.isOfferActive() && CFG_GUI.CFG.isSpecialDealsEnabled() && SPECIAL_DEALS_ENABLED.get());
    // }
    //
    // private boolean isPaintSpecialDeal() {
    // return (OboomDialog.isOfferActive() && CFG_GUI.CFG.isSpecialDealsEnabled() && SPECIAL_DEALS_ENABLED.get());
    // }

    @Override
    public void onMouseOver(MouseEvent e) {
        MainTabbedPane pane = MainTabbedPane.getInstance();
        mouseover = true;
        pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        System.out.println("in");
    }

    @Override
    public void onMouseOut(MouseEvent e) {
        MainTabbedPane pane = MainTabbedPane.getInstance();
        mouseover = false;
        pane.setCursor(null);
        System.out.println("out");
    }

    @Override
    public void onClicked(MouseEvent e) {
        if (getProMode) {
            if (closeBounds != null && closeBounds.contains(e.getPoint())) {
                new Thread("DEAL_1") {
                    public void run() {
                        CrossSystem.openURL("https://www.oboom.com/ref/501C81");
                        new Thread("DEAL_HIDE") {
                            public void run() {
                                try {
                                    Dialog.getInstance().showConfirmDialog(0, _GUI._.OboomController_run_hide_title(), _GUI._.OboomController_run_hide_msg(), null, _GUI._.lit_yes(), null);
                                    OboomController.track("GETPRO_HIDE_YES");
                                    CFG_GUI.SPECIAL_DEALS_ENABLED.setValue(false);
                                } catch (DialogNoAnswerException e) {
                                    OboomController.track("GETPRO_HIDE_NO");
                                }
                            }
                        }.start();

                        OboomController.track("GETPRO_HIDE");

                    }
                }.start();
            } else {
                new Thread("OSR") {
                    public void run() {

                        OboomController.track("GETPRO");
                        ConfirmDialog d = null;
                        if ((hasDealAccountToRenewAlreadyExpired || hasOtherAccountToRenewAlreadyExpired) && accountToRenew != null) {
                            d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.OboomController_run_renew_title_expired(), _GUI._.OboomController_run_renew_msg_expired(accountToRenew.getUser()), new AbstractIcon("logo_oboom_small", 32), _GUI._.lit_continue(), null) {
                                @Override
                                public ModalityType getModalityType() {
                                    return ModalityType.MODELESS;
                                }
                            };
                        } else if ((hasOtherAccountToRenew || hasDealAccountToRenew) && accountToRenew != null) {
                            d = new ConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.OboomController_run_renew_title(), _GUI._.OboomController_run_renew_msg(accountToRenew.getUser()), new AbstractIcon("logo_oboom_small", 32), _GUI._.lit_continue(), null) {
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
                            OboomController.track("GETPRO_DIALOG_OK");
                        } catch (DialogClosedException e) {
                            OboomController.track("GETPRO_DIALOG_CLOSED");
                        } catch (DialogCanceledException e) {
                            OboomController.track("GETPRO_DIALOG_CANCELED");
                        }

                        CrossSystem.openURL("https://www.oboom.com/ref/501C81");

                    }
                }.start();
            }
        } else {
            if (closeBounds != null && closeBounds.contains(e.getPoint())) {
                new Thread("DEAL_1") {
                    public void run() {

                        OboomDialog d = new OboomDialog("tabclick_hide") {
                            protected void packed() {
                                new Thread("DEAL_HIDE") {
                                    public void run() {
                                        try {
                                            Dialog.getInstance().showConfirmDialog(0, _GUI._.OboomController_run_hide_title(), _GUI._.OboomController_run_hide_msg(), null, _GUI._.lit_yes(), null);
                                            OboomController.track("TabbedHideClick_YES");
                                            CFG_GUI.SPECIAL_DEALS_ENABLED.setValue(false);
                                        } catch (DialogNoAnswerException e) {
                                            OboomController.track("TabbedHideClick_NO");
                                        }
                                    }
                                }.start();

                            };
                        };

                        UIOManager.I().show(null, d);
                        OboomController.track("TabbedHideClick");

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
    }

    public TopRightPainter start() {

        Thread thread = new Thread("Ask StatServ") {
            public void run() {

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (true) {
                    Browser br = new Browser();
                    try {
                        br.getPage("http://stats.appwork.org/data/db/getDealStatus");
                        boolean newValue = false;
                        if (br.containsHTML("true") || !Application.isJared(null)) {
                            newValue = true;
                        }

                        visible = newValue;
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                MainTabbedPane pane = MainTabbedPane.getInstance();
                                pane.repaint();
                            }
                        };
                        if (CFG_GUI.CFG.isSpecialDealOboomDialogVisibleOnStartup()) {
                            Thread.sleep(10000);

                            OboomDialog d = new OboomDialog("autopopup");
                            OboomController.track("Popup_10000");
                            UIOManager.I().show(null, d);
                            CFG_GUI.CFG.setSpecialDealOboomDialogVisibleOnStartup(false);

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

    public static boolean isOfferActive() {
        return OFFER_IS_ACTIVE;
    }

    public static void track(final String string) {
        StatsManager.I().track("specialdeals/oboom/" + string);
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
        } catch (final IOException e) {
            // TODO Auto-generated catch block
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
            e.printStackTrace();
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
        boolean hasDealAccountToRenewAlreadyExpired = false;
        boolean hasOtherAccountToRenewAlreadyExpired = false;
        boolean hasDealAccountToRenew = false;
        boolean hasOtherAccountToRenew = false;
        boolean hasOboomPremium = false;
        Account renew = null;
        Account renewDeal = null;
        for (Account acc : AccountController.getInstance().list("oboom.com")) {
            long dealTime = acc.getLongProperty("DEAL", -1l);
            if (acc.isEnabled() && dealTime > 0) {

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
            } else if (acc.isEnabled()) {
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

        this.accountToRenew = renew;
        this.hasPremium = hasOboomPremium;
        this.hasOtherAccountToRenewAlreadyExpired = hasOtherAccountToRenewAlreadyExpired;
        this.hasDealAccountToRenewAlreadyExpired = hasDealAccountToRenewAlreadyExpired;
        this.hasOtherAccountToRenew = hasOtherAccountToRenew;
        this.hasDealAccountToRenew = hasDealAccountToRenew;
        boolean showPro = isOfferActive() && (hasOtherAccountToRenew || hasDealAccountToRenew) && !hasOboomPremium;
        if (showPro != getProMode) {
            getProMode = showPro;
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    MainTabbedPane pane = MainTabbedPane.getInstance();
                    pane.repaint();
                }
            };
        }

    }
}
