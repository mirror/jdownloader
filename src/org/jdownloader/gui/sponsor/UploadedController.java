package org.jdownloader.gui.sponsor;

import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.TopRightPainter;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.crypto.Crypto;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.statistics.StatsManager;

public class UploadedController implements AccountControllerListener, Sponsor {
    static {

        CFG_GUI.ULBANNER_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (Boolean.FALSE == newValue) {
                    StatsManager.I().track("various/UlBANNER/disabled");
                } else {
                    StatsManager.I().track("various/UlBANNER/enabled");
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
    }
    // private static final String HTTP_BASE = "http://nas:81/thomas/fcgi";
    private static final String             HTTP_BASE               = "http://update3.jdownloader.org/jdserv";
    // private final AtomicBoolean enabledByAPI = new AtomicBoolean(false);
    private final AtomicBoolean             enabledInAdvancedConfig = new AtomicBoolean(false);
    private boolean                         mouseover;
    private Icon                            icon;
    private final AbstractIcon              close;
    private Rectangle                       closeBounds;

    private HashMap<String, Long>           expireNotifies;
    private DelayedRunnable                 delayer;

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

        delayer = new DelayedRunnable(1000, 15000) {

            @Override
            public void delayedrun() {
                updateIcon();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MainTabbedPane.getInstance().repaint();
                    }
                };
            }
        };
        CFG_GUI.ULBANNER_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

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
        enabledInAdvancedConfig.set(CFG_GUI.ULBANNER_ENABLED.isEnabled());
        if (Application.getResource("cfg/donation_0.json").exists()) {
            enabledInAdvancedConfig.set(false);
        }
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                AccountController.getInstance().getBroadcaster().addListener(UploadedController.this, true);
                onAccountControllerEvent(null);

            }
        });

    }

    public static class UlBannerData implements Storable {
        public static final TypeRef<UlBannerData> TYPREF = new TypeRef<UlBannerData>() {
                                                         };

        public UlBannerData(/* Storable */) {
        }

        private String md5;

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        private String banner;

        public String getBanner() {
            return banner;
        }

        public void setBanner(String banner) {
            this.banner = banner;
        }

        public String getPromotion() {
            return promotion;
        }

        public void setPromotion(String promotion) {
            this.promotion = promotion;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        private String promotion;
        private String target;
        private long   updateTime;

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }

    private static final HashSet<String> EXCLUDES = new HashSet<String>();
    static {
        EXCLUDES.add("my.mail.ru");
        EXCLUDES.add("redfile.eu");
        EXCLUDES.add("flashx.tv");
        EXCLUDES.add("video.yandex.ru");
        EXCLUDES.add("facebook.com");
        EXCLUDES.add("massengeschmack.tv");
        EXCLUDES.add("deviantart.com");
        EXCLUDES.add("gloria.tv");
        EXCLUDES.add("disk.yandex.net");
        EXCLUDES.add("dailymotion.com");
        EXCLUDES.add("videozer.us");
        EXCLUDES.add("mangatraders.org");
        EXCLUDES.add("video.tt");
        EXCLUDES.add("wankoz.com");
        EXCLUDES.add("whatboyswant.com");
        EXCLUDES.add("met-art.com");
        EXCLUDES.add("junkyvideo.com");
        EXCLUDES.add("twitch.tv");
        EXCLUDES.add("videopremium.tv");
        EXCLUDES.add("boosterking.com");
        EXCLUDES.add("motherless.com");
        EXCLUDES.add("drtuber.com");
        EXCLUDES.add("nowvideo.ch");
        EXCLUDES.add("soundcloud.com");
        EXCLUDES.add("vidto.me");
        EXCLUDES.add("evilangel.com");
        EXCLUDES.add("nowvideo.co");
        EXCLUDES.add("issuu.com");
        EXCLUDES.add("playvid.com");
        EXCLUDES.add("nowvideo.eu");
        EXCLUDES.add("xboxisozone.com");
        EXCLUDES.add("abbywinters.com");
        EXCLUDES.add("livemixtapes.com");
        EXCLUDES.add("put.io");
        EXCLUDES.add("videobb.com");
        EXCLUDES.add("vimeo.com");
        EXCLUDES.add("worldclips.ru");
        EXCLUDES.add("x-art.com");
        EXCLUDES.add("7thsky.es");
        EXCLUDES.add("veehd.com");
        EXCLUDES.add("realitykings.com");
        EXCLUDES.add("boyztube.com");
        EXCLUDES.add("scribd.com");
        EXCLUDES.add("acapellas4u.co.uk");
        EXCLUDES.add("nk.pl");
        EXCLUDES.add("parellisavvyclub.com");
        EXCLUDES.add("manga.animea.net");
        EXCLUDES.add("vip.animea.net");
        EXCLUDES.add("videobox.com");
        EXCLUDES.add("tube8.com");
        EXCLUDES.add("hardsextube.com");
        EXCLUDES.add("vkontakte.ru");
        EXCLUDES.add("eroprofile.com");
        EXCLUDES.add("video.fc2.com");
        EXCLUDES.add("pinterest.com");
        EXCLUDES.add("fernsehkritik.tv");
        EXCLUDES.add("nicovideo.jp");
        EXCLUDES.add("shutterstock.com");
        EXCLUDES.add("yahoo.com");
        EXCLUDES.add("newgamex.com");
        EXCLUDES.add("xhamster.com");
        EXCLUDES.add("save.tv");
        EXCLUDES.add("ps3gameroom.net");
        EXCLUDES.add("5fantastic.pl");
        EXCLUDES.add("videopremium.net");
        EXCLUDES.add("babes.com");
        EXCLUDES.add("youtube.com");
        EXCLUDES.add("dropbox.com");
        EXCLUDES.add("uploadhero.co");
        EXCLUDES.add("otr.datenkeller.at");
        EXCLUDES.add("flickr.com");
        EXCLUDES.add("trilulilu.ro");
        EXCLUDES.add("online.nolife-tv.com");
        EXCLUDES.add("onlinetvrecorder.com");
    }

    private void updateIcon() {
        try {

            boolean[] b = aggregateAccounts();
            boolean hasUploaded = b[0];
            boolean hasOther = b[1];

            String lng = TranslationFactory.getDesiredLanguage();
            Browser br = new Browser();
            File png = Application.getResource("tmp/ul_" + lng + "_" + hasOther + "_" + hasUploaded + ".png");
            if (png.exists() && System.currentTimeMillis() - png.lastModified() < 24 * 60 * 60 * 1000l) {
                icon = new ImageIcon(ImageIO.read(png));
                return;
            }

            String md5 = png.exists() ? Hash.getMD5(png) : null;

            String uid;
            StringBuilder sb = createID();

            uid = sb.toString();
            String pid = Crypto.decrypt(HexFormatter.hexToByteArray(System.getProperty("pid")), HexFormatter.hexToByteArray("9ed709f3c87dfa6eee9bcd2897123bf6"));
            String uls = Crypto.decrypt(HexFormatter.hexToByteArray(System.getProperty("uls")), HexFormatter.hexToByteArray("9ed709f3c87dfa6eee9bcd2897123bf6"));
            String sig = Hash.getSHA1(uls + pid + uid);

            br.setAllowedResponseCodes(200, 204);
            URLConnectionAdapter conn = br.openGetConnection(HTTP_BASE + "/RedirectInterface/banner?jd2&" + URLEncode.encodeRFC2396(sig) + "&" + URLEncode.encodeRFC2396(uid) + "&" + URLEncode.encodeRFC2396(pid) + "&" + md5 + "&" + URLEncode.encodeRFC2396(lng) + "&" + hasUploaded + "&" + hasOther);
            try {
                if (conn.getResponseCode() == 200) {
                    png.delete();
                    Browser.download(png, conn);

                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            if (png.exists()) {
                icon = new ImageIcon(ImageIO.read(png));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean[] aggregateAccounts() {
        boolean hasUploaded = false;
        boolean hasOther = false;
        for (Account acc : AccountController.getInstance().list()) {
            if ("uploaded.to".equals(acc.getHoster())) {
                if (acc.getType() == AccountType.PREMIUM && acc.isEnabled()) {

                    hasUploaded = true;

                }
                continue;
            }
            if (acc.isValid() && acc.getLastValidTimestamp() > 0) {

                if (!EXCLUDES.contains(acc.getHoster().toLowerCase(Locale.ENGLISH))) {
                    hasOther = true;
                }
                if (hasUploaded && hasOther) {
                    break;
                    // }
                }
            }
        }
        return new boolean[] { hasUploaded, hasOther };
    }

    @Override
    public Rectangle paint(Graphics2D g) {
        final MainTabbedPane pane = MainTabbedPane.getInstance();
        if (isVisible()) {

            if (icon != null && pane != null && close != null) {
                icon.paintIcon(pane, g, pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight() + 2);
                final Rectangle specialDealBounds = new Rectangle(pane.getWidth() - icon.getIconWidth() - 2, 25 - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight() + 2);
                if (mouseover) {
                    // g.setColor(Color.GRAY);
                    // g.drawLine(specialDealBounds.x + 50, specialDealBounds.y + specialDealBounds.height - 1 - 3, specialDealBounds.x +
                    // specialDealBounds.width - 2, specialDealBounds.y + specialDealBounds.height - 1 - 3);
                    // g.setColor(Color.WHITE);
                    // g.fillRect(pane.getWidth() - close.getIconWidth() - 3, 25 - icon.getIconHeight() - 2, 9, 11);
                    // closeBounds = new Rectangle(pane.getWidth() - close.getIconWidth() - 3, 25 - icon.getIconHeight() - 2, 9, 11);
                    // close.paintIcon(pane, g, pane.getWidth() - close.getIconWidth() - 2, 25 - icon.getIconHeight());
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
                        CFG_GUI.ULBANNER_ENABLED.setValue(false);
                    } catch (DialogNoAnswerException e) {
                        UploadedController.track("TabbedHideClick/NO");
                    }

                }
            }.start();
            return;
        }

        new Thread("OSR") {
            public void run() {
                try {
                    String uid;
                    StringBuilder sb = createID();

                    uid = sb.toString();
                    String pid = Crypto.decrypt(HexFormatter.hexToByteArray(System.getProperty("pid")), HexFormatter.hexToByteArray("9ed709f3c87dfa6eee9bcd2897123bf6"));
                    String uls = Crypto.decrypt(HexFormatter.hexToByteArray(System.getProperty("uls")), HexFormatter.hexToByteArray("9ed709f3c87dfa6eee9bcd2897123bf6"));
                    String sig = Hash.getSHA1(uls + pid + uid);
                    boolean[] ag = aggregateAccounts();
                    boolean hasUploaded = ag[0];
                    boolean hasOther = ag[1];

                    CrossSystem.openURL(HTTP_BASE + "/RedirectInterface/ul?jd2&" + URLEncode.encodeRFC2396(sig) + "&" + URLEncode.encodeRFC2396(uid) + "&" + URLEncode.encodeRFC2396(pid) + "&" + hasUploaded + "&" + hasOther);

                    UploadedController.track("TabbedClick");

                } catch (Throwable e) {
                    CrossSystem.openURL("http://ul.to/ref/12859436");
                    UploadedController.track("TabbedClick_Fallback_" + e.getMessage());
                }
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
            delayer.resetAndStart();

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
            if ("uploaded.to".equals(plugin.getHost())) {
                CrossSystem.openURL("http://ul.to/ref/12859436");
            } else {
                CrossSystem.openURL(AccountController.createFullBuyPremiumUrl(url, "controller/notify"));
            }
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

    public StringBuilder createID() throws UnsupportedEncodingException {
        String uid = System.getProperty(new String(new byte[] { (byte) 117, (byte) 105, (byte) 100 }, new String(new byte[] { 85, 84, 70, 45, 56 }, "UTF-8")));
        if (StringUtils.isEmpty(uid)) {
            uid = "empty";
        }
        uid = Hash.getSHA256(uid);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uid.length(); i++) {
            if (i > 0 && i % 3 != 0) {
                sb.append(uid.charAt(i));
            }
        }
        return sb;
    }
}
