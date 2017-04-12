package org.jdownloader.captcha.v2.solver.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.captcha.v2.solver.browser.BrowserCaptchaSolverConfig;
import org.jdownloader.captcha.v2.solver.browser.CFG_BROWSER_CAPTCHA_SOLVER;
import org.jdownloader.captcha.v2.solver.cheapcaptcha.CheapCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaSolverService;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzSolverService;
import org.jdownloader.captcha.v2.solver.jac.JacSolverService;
import org.jdownloader.captcha.v2.solver.myjd.CaptchaMyJDSolverService;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.components.google.GoogleAccountConfig;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.config.AccountJsonConfig;

public class BrowserSolverService extends AbstractSolverService {
    public static final String                ID       = "browser";
    private static final BrowserSolverService INSTANCE = new BrowserSolverService();

    static {
        GenericConfigEventListener<String> cookiesTester = new GenericConfigEventListener<String>() {
            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                {
                    String sid = CFG_BROWSER_CAPTCHA_SOLVER.GOOGLE_COM_COOKIE_VALUE_SID.getValue();
                    String hsid = CFG_BROWSER_CAPTCHA_SOLVER.GOOGLE_COM_COOKIE_VALUE_HSID.getValue();
                    final AtomicReference<BufferedImage> niceOrg = new AtomicReference<BufferedImage>();
                    final AtomicReference<BufferedImage> badOrg = new AtomicReference<BufferedImage>();
                    if (StringUtils.isNotEmpty(sid) && StringUtils.isNotEmpty(hsid)) {
                        ConfirmDialog d = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_OK, _GUI.T.Recaptcha_cookie_help_title(), _GUI.T.Recaptcha_cookie_help_msg(), new AbstractIcon(IconKey.ICON_OCR, 32), null, _GUI.T.lit_close()) {
                            @Override
                            protected JComponent getIconComponent() {
                                URLConnectionAdapter con;
                                try {
                                    String siteKey = "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-";
                                    Browser br = new Browser();
                                    BrowserSolverService.fillCookies(br);
                                    br.getPage("http://www.google.com/recaptcha/api/challenge?k=" + siteKey);
                                    String challenge = br.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
                                    String server = br.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
                                    niceOrg.set(ImageIO.read(br.openGetConnection(server + "image?c=" + challenge).getInputStream()));
                                    BufferedImage niceImage = IconIO.toBufferedImage(niceOrg.get());
                                    br = new Browser();
                                    br.getPage("http://www.google.com/recaptcha/api/challenge?k=" + siteKey);
                                    challenge = br.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
                                    server = br.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
                                    badOrg.set(ImageIO.read(br.openGetConnection(server + "image?c=" + challenge).getInputStream()));
                                    BufferedImage badImage = IconIO.toBufferedImage(badOrg.get());
                                    Graphics2D niceGraphics = (Graphics2D) niceImage.getGraphics();
                                    Graphics2D badGraphics = (Graphics2D) badImage.getGraphics();
                                    Font font = new Font(ImageProvider.getDrawFontName(), Font.BOLD, 18);
                                    niceGraphics.setColor(Color.GREEN);
                                    niceGraphics.setFont(font);
                                    niceGraphics.drawString("Easy Captcha :-) ", 4, niceImage.getHeight() - 4);
                                    badGraphics.setColor(Color.RED);
                                    badGraphics.setFont(font);
                                    badGraphics.drawString("Hard Captcha :´( ", 4, badImage.getHeight() - 4);
                                    MigPanel ret = new MigPanel("ins 0,wrap 1", "[]", "[][]");
                                    ret.add(new JLabel(new ImageIcon(niceImage)));
                                    ret.add(new JLabel(new ImageIcon(badImage)));
                                    return ret;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return super.getIconComponent();
                            }

                            protected int getPreferredWidth() {
                                return 700;
                            };
                        };
                        d.setTimeout(120000);
                        UIOManager.I().show(ConfirmDialogInterface.class, d);
                        if (niceOrg.get() == null || niceOrg.get().getType() == 10) {
                            return;
                        }
                    } else {
                        UIOManager.I().showMessageDialog(_GUI.T.Recaptcha_cookie_help_msg_both_cookies());
                    }
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }
        };
        CFG_BROWSER_CAPTCHA_SOLVER.GOOGLE_COM_COOKIE_VALUE_HSID.getEventSender().addListener(cookiesTester);
        CFG_BROWSER_CAPTCHA_SOLVER.GOOGLE_COM_COOKIE_VALUE_SID.getEventSender().addListener(cookiesTester);
    }

    private static BrowserCaptchaSolverConfig config;

    public static BrowserSolverService getInstance() {
        config = JsonConfig.create(BrowserCaptchaSolverConfig.class);
        return INSTANCE;
    }

    @Override
    public String getType() {
        return _GUI.T.BrowserSolverService_getName();
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_OCR, size);
    }

    @Override
    public String getName() {
        return _GUI.T.BrowserSolverService_gettypeName();
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            {
                addHeader(getTitle(), BrowserSolverService.this.getIcon(32));
                addDescription(BrowserSolverService.this.getType());
                addBlackWhiteList(config);
            }

            @Override
            public Icon getIcon() {
                return BrowserSolverService.this.getIcon(32);
            }

            @Override
            public String getPanelID() {
                return "JAC_" + getTitle();
            }

            @Override
            public String getTitle() {
                return BrowserSolverService.this.getName();
            }

            @Override
            public void save() {
            }

            @Override
            public void updateContents() {
            }
        };
        return ret;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public BrowserCaptchaSolverConfig getConfig() {
        return JsonConfig.create(BrowserCaptchaSolverConfig.class);
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        // ret.put(DialogClickCaptchaSolver.ID, 0);
        // ret.put(DialogBasicCaptchaSolver.ID, 0);
        // ret.put(CaptchaAPISolver.ID, 0);
        ret.put(JacSolverService.ID, 30000);
        ret.put(NineKwSolverService.ID, 300000);
        ret.put(CaptchaMyJDSolverService.ID, 60000);
        ret.put(DeathByCaptchaSolverService.ID, 60000);
        ret.put(ImageTyperzSolverService.ID, 60000);
        ret.put(CheapCaptchaSolverService.ID, 60000);
        ret.put(EndCaptchaSolverService.ID, 60000);
        return ret;
    }

    @Override
    public String getID() {
        return ID;
    }

    public static void fillCookies(Browser rcBr) {
        if (StringUtils.isNotEmpty(getInstance().getConfig().getGoogleComCookieValueSID()) && StringUtils.isNotEmpty(getInstance().getConfig().getGoogleComCookieValueHSID())) {
            rcBr.setCookie("http://google.com", "SID", getInstance().getConfig().getGoogleComCookieValueSID());
            rcBr.setCookie("http://google.com", "HSID", getInstance().getConfig().getGoogleComCookieValueHSID());
        } else {
            try {
                for (Account acc : AccountController.getInstance().list("recaptcha.google.com")) {
                    if (acc.isEnabled()) {
                        GoogleAccountConfig cfg = (GoogleAccountConfig) AccountJsonConfig.get(acc);
                        if (cfg.isUsageRecaptchaV1Enabled()) {
                            GoogleHelper helper = new GoogleHelper(rcBr);
                            helper.setCacheEnabled(true);
                            if (helper.login(acc)) {
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
    }
}
