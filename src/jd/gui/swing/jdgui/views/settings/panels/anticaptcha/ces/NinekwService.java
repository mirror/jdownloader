package jd.gui.swing.jdgui.views.settings.panels.anticaptcha.ces;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import jd.controlling.ClipboardMonitoring;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESGenericConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CESService;
import jd.http.Browser;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;

public class NinekwService implements CESService {
    private ExtButton          btnRegister;
    private ExtButton          btnApi;
    private ExtButton          btnPlugins;
    private ExtButton          btnFAQ;
    private ExtButton          btnSupport;
    private ExtButton          btnHelp;
    private ExtButton          btnUserhistory;
    private ExtButton          btnUserCheck;
    private ExtButton          btnUserBuy;
    private ExtButton          btnUserDebug1;
    private ExtButton          btnUserDebug1clipboard;
    private ExtButton          btnUserDebug1file;
    private ExtButton          btnUserDebug2;
    private ExtButton          btnUserDebug3;
    private ExtButton          btnUserDebug3hoster;
    private ExtButton          btnUserDebug3crawler;
    private Captcha9kwSettings config;
    public static final String DEBUGEXT = ".txt";

    @Override
    public ImageIcon getIcon(int i) {
        return NewTheme.I().getIcon(IconKey.ICON_9KW, i);
    }

    @Override
    public String getDisplayName() {
        return "9kw.eu";
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(true);
        return ret;
    }

    public String getAPIROOT() {
        config = JsonConfig.create(Captcha9kwSettings.class);
        if (config.ishttps()) {
            return "https://www.9kw.eu/";
        } else {
            return "http://www.9kw.eu/";
        }
    }

    @Override
    public CESGenericConfigPanel createPanel() {
        CESGenericConfigPanel ret = new CESGenericConfigPanel(this) {
            /**
             * 
             */
            private static final long serialVersionUID = -1805335184795063609L;
            private TextInput         apiKey;
            private TextInput         blacklist;
            private TextInput         whitelist;
            private TextInput         blacklistprio;
            private TextInput         whitelistprio;
            private TextInput         blacklisttimeout;
            private TextInput         whitelisttimeout;

            @Override
            public String getPanelID() {
                return "CES_" + getDisplayName();
            }

            {
                addHeader(getDisplayName(), NewTheme.I().getIcon(IconKey.ICON_9KW, 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 8804949739472915394L;

                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT());

                    }
                }), "gapleft 37,spanx,pushx,growx");

                MigPanel toolbar1 = new MigPanel("ins 0", "[][][][]", "[]");
                MigPanel toolbar2 = new MigPanel("ins 0", "[][][][]", "[]");
                btnRegister = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 7195034001951861669L;

                    {
                        setName("Register");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "register.html");

                    }
                });
                btnApi = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 7443478587786398670L;

                    {
                        setName("API management");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "userapi.html");

                    }
                });
                btnPlugins = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 535406846478413287L;

                    {
                        setName("Plugins");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "plugins.html");

                    }
                });
                btnFAQ = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -93146391722269060L;

                    {
                        setName("FAQ");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "faq.html");

                    }
                });
                btnHelp = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -8623650782355847927L;

                    {
                        setName("Help");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "hilfe.html");

                    }
                });
                btnSupport = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 36350110842819194L;

                    {
                        setName("Support");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "kontakt.html");

                    }
                });
                btnUserhistory = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 8334258034515555683L;

                    {
                        setName("History");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL(getAPIROOT() + "userhistory.html");

                    }
                });
                toolbar1.add(btnRegister, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnPlugins, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnFAQ, "pushx,growx,sg 1,height 26!");
                toolbar1.add(btnHelp, "pushx,growx,sg 1,height 26!");
                add(toolbar1, "gapleft 37,spanx,pushx,growx");

                toolbar2.add(btnApi, "pushx,growx,sg 1,height 26!");
                toolbar2.add(btnUserhistory, "pushx,growx,sg 1,height 26!");
                toolbar2.add(btnSupport, "pushx,growx,sg 1,height 26!");
                add(toolbar2, "gapleft 37,spanx,pushx,growx");

                apiKey = new TextInput(CFG_9KWCAPTCHA.API_KEY);
                blacklist = new TextInput(CFG_9KWCAPTCHA.BLACKLIST);
                whitelist = new TextInput(CFG_9KWCAPTCHA.WHITELIST);
                blacklistprio = new TextInput(CFG_9KWCAPTCHA.BLACKLISTPRIO);
                whitelistprio = new TextInput(CFG_9KWCAPTCHA.WHITELISTPRIO);
                blacklisttimeout = new TextInput(CFG_9KWCAPTCHA.BLACKLISTTIMEOUT);
                whitelisttimeout = new TextInput(CFG_9KWCAPTCHA.WHITELISTTIMEOUT);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.NinekwService_createPanel_logins_());

                MigPanel toolbar3 = new MigPanel("ins 0", "[][][]", "[]");
                // toolbar3.add(btnHelp, "sg 1,height 26!");
                // add(toolbar3, "gapleft 37,spanx,pushx,growx");
                toolbar3.add(label(_GUI._.NinekwService_createPanel_enabled()), "width 135!");
                toolbar3.add(new Checkbox(CFG_9KWCAPTCHA.ENABLED));
                toolbar3.add(label("Text captchas"));
                add(toolbar3, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_enabled(), null, );

                MigPanel toolbar4 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar4.add(label(_GUI._.NinekwService_createPanel_apikey()), "width 135!");
                toolbar4.add(apiKey, "pushx,growx");

                btnUserCheck = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -103695205004891917L;

                    {
                        setName("Check");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Browser br = new Browser();
                            String accountcheck = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&okcheck=1&apikey=" + CFG_9KWCAPTCHA.API_KEY);
                            String errorcheck = br.getRegex("^([0-9]+ .*)").getMatch(0);
                            if (accountcheck.startsWith("OK-")) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw message ", "Account OK\nCredits: " + accountcheck.substring("OK-".length()));
                            } else if (errorcheck != null) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "Account error\n" + accountcheck);
                            } else if (CFG_9KWCAPTCHA.API_KEY != null) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "No api key.");
                            } else if (accountcheck.length() > 5) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "Unknown error or incorrect api key.");
                            } else {
                                jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(1) ", "Unknown error.");
                            }
                        } catch (IOException e9kw) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(2) ", "No connection, unknown error or incorrect api key.");
                        }
                    }
                });
                toolbar4.add(btnUserCheck);
                btnUserBuy = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 8334258034515555683L;

                    {
                        setName("Buy credits");

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.9kw.eu/buy.html");

                    }
                });
                toolbar4.add(btnUserBuy);
                add(toolbar4, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_apikey(), null, apiKey);

                // addPair(_GUI._.NinekwService_createPanel_mouse(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSE));
                MigPanel toolbar5 = new MigPanel("ins 0", "[][][]", "[]");
                toolbar5.add(label(_GUI._.NinekwService_createPanel_mouse()), "width 135!");
                toolbar5.add(new Checkbox(CFG_9KWCAPTCHA.MOUSE));
                toolbar5.add(label("Mouse captchas"));
                toolbar5.add(new Checkbox(CFG_9KWCAPTCHA.LOWCREDITS));
                toolbar5.add(label(_GUI._.NinekwService_createPanel_lowcredits()));
                add(toolbar5, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar6 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar6.add(label(" "), "width 135!");
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.FEEDBACK));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_feedback()));
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.HTTPS));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_https()));
                toolbar6.add(new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE));
                toolbar6.add(label(_GUI._.NinekwService_createPanel_selfsolve()));
                add(toolbar6, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_feedback(), null, new Checkbox(CFG_9KWCAPTCHA.FEEDBACK));
                // addPair(_GUI._.NinekwService_createPanel_https(), null, new Checkbox(CFG_9KWCAPTCHA.HTTPS));
                // addPair(_GUI._.NinekwService_createPanel_selfsolve(), null, new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE));

                MigPanel toolbar7 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar7.add(label(" "), "width 135!");
                toolbar7.add(new Checkbox(CFG_9KWCAPTCHA.CONFIRM));
                toolbar7.add(label(_GUI._.NinekwService_createPanel_confirm()));
                toolbar7.add(new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM));
                toolbar7.add(label(_GUI._.NinekwService_createPanel_mouseconfirm()));
                add(toolbar7, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_confirm(), null, new Checkbox(CFG_9KWCAPTCHA.CONFIRM));
                // addPair(_GUI._.NinekwService_createPanel_mouseconfirm(), null, new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM));

                MigPanel toolbar8 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar8.add(label(" "), "width 135!");
                toolbar8.add(new Spinner(CFG_9KWCAPTCHA.PRIO));
                toolbar8.add(label(_GUI._.NinekwService_createPanel_prio()));
                toolbar8.add(new Spinner(CFG_9KWCAPTCHA.HOUR));
                toolbar8.add(label(_GUI._.NinekwService_createPanel_hour()));
                add(toolbar8, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_prio(), null, new Spinner(CFG_9KWCAPTCHA.PRIO));
                // addPair(_GUI._.NinekwService_createPanel_hour(), null, new Spinner(CFG_9KWCAPTCHA.HOUR));

                MigPanel toolbar9 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar9.add(label(" "), "width 135!");
                toolbar9.add(new Spinner(CFG_9KWCAPTCHA.THREADPOOL));
                toolbar9.add(label(_GUI._.NinekwService_createPanel_threadsizepool()));
                toolbar9.add(new Spinner(CFG_9KWCAPTCHA.Timeout9kw));
                toolbar9.add(label(_GUI._.NinekwService_createPanel_9kwtimeout()));
                add(toolbar9, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_threadsizepool(), null, new Spinner(CFG_9KWCAPTCHA.THREADPOOL));
                // addPair(_GUI._.NinekwService_createPanel_9kwtimeout(), null, new Spinner(CFG_9KWCAPTCHA.Timeout9kw));

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_blackwhitelist_(), NewTheme.I().getIcon(IconKey.ICON_SELECT, 32));

                MigPanel toolbar10 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar10.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
                toolbar10.add(label(_GUI._.NinekwService_createPanel_blacklist()));
                toolbar10.add(blacklist, "pushx,growx");
                add(toolbar10, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_blacklistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
                // addPair(_GUI._.NinekwService_createPanel_blacklist(), null, blacklist);

                MigPanel toolbar11 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar11.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
                toolbar11.add(label(_GUI._.NinekwService_createPanel_whitelist()));
                toolbar11.add(whitelist, "pushx,growx");
                add(toolbar11, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_whitelistcheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
                // addPair(_GUI._.NinekwService_createPanel_whitelist(), null, whitelist);

                MigPanel toolbar13t3 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13t3.add(label(" "));
                add(toolbar13t3, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar12 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar12.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
                toolbar12.add(label(_GUI._.NinekwService_createPanel_blacklistprio()));
                toolbar12.add(blacklistprio, "pushx,growx");
                add(toolbar12, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_blacklistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
                // addPair(_GUI._.NinekwService_createPanel_blacklistprio(), null, blacklistprio);

                MigPanel toolbar13 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
                toolbar13.add(label(_GUI._.NinekwService_createPanel_whitelistprio()));
                toolbar13.add(whitelistprio, "pushx,growx");
                add(toolbar13, "gapleft 33,spanx,pushx,growx");
                // addPair(_GUI._.NinekwService_createPanel_whitelistpriocheck(), null, new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
                // addPair(_GUI._.NinekwService_createPanel_whitelistprio(), null, whitelistprio);

                MigPanel toolbar13t0 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13t0.add(label(" "));
                add(toolbar13t0, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar13t = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13t.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTTIMEOUTCHECK));
                toolbar13t.add(label(_GUI._.NinekwService_createPanel_whitelisttimeout()));
                toolbar13t.add(whitelisttimeout, "pushx,growx");
                add(toolbar13t, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar13t1 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar13t1.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTTIMEOUTCHECK));
                toolbar13t1.add(label(_GUI._.NinekwService_createPanel_blacklisttimeout()));
                toolbar13t1.add(blacklisttimeout, "pushx,growx");
                add(toolbar13t1, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbar9a = new MigPanel("ins 0", "[][][][]", "[]");
                toolbar9a.add(label(" "), "width 130!");
                toolbar9a.add(label(_GUI._.NinekwService_createPanel_9kwtimeoutother()));
                toolbar9a.add(new Spinner(CFG_9KWCAPTCHA.Timeout9kwNew));
                toolbar9a.add(label("ms"));
                add(toolbar9a, "gapleft 33,spanx,pushx,growx");

                this.addHeader(_GUI._.NinekwService_createPanel_debug(), NewTheme.I().getIcon(IconKey.ICON_EVENT, 32));
                MigPanel toolbardebug1 = new MigPanel("ins 0", "[][][][]", "[]");
                toolbardebug1.add(new Checkbox(CFG_9KWCAPTCHA.DEBUG));
                toolbardebug1.add(label("Debug"));

                btnUserDebug1 = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 1700532687116057633L;

                    {
                        setName("Debuglog");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().length() > 1) {
                            try {
                                Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, "9kw debuglog", null, org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog(), NewTheme.getInstance().getIcon("proxy", 32), null, null);
                            } catch (DialogClosedException e2) {
                                e2.printStackTrace();
                            } catch (DialogCanceledException e2) {
                                e2.printStackTrace();
                            }
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", "no data");
                        }
                    }
                });
                toolbardebug1.add(btnUserDebug1);

                btnUserDebug1clipboard = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 1700532687116057633L;

                    {
                        setName("Log to clipboard");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().length() > 1) {
                            ClipboardMonitoring.getINSTANCE().setCurrentContent(org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog());
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", "no data");
                        }
                    }
                });
                toolbardebug1.add(btnUserDebug1clipboard);

                btnUserDebug1file = new ExtButton(new AppAction() {
                    /**
                     * Save debuglog as file
                     */
                    private static final long serialVersionUID = 1700542687116057633L;

                    {
                        setName("Log to file");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().length() > 1) {
                            ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.SaveAsProxyProfileAction_actionPerformed_choose_file(), null, null);
                            d.setFileFilter(new FileFilter() {

                                @Override
                                public String getDescription() {

                                    return "*" + DEBUGEXT;
                                }

                                @Override
                                public boolean accept(File f) {
                                    return f.isDirectory() || f.getName().endsWith(DEBUGEXT);
                                }
                            });

                            d.setFileSelectionMode(FileChooserSelectionMode.FILES_AND_DIRECTORIES);
                            d.setMultiSelection(false);

                            d.setStorageID(DEBUGEXT);
                            d.setType(FileChooserType.SAVE_DIALOG);
                            try {
                                Dialog.getInstance().showDialog(d);

                                File saveTo = d.getSelectedFile();
                                if (!saveTo.getName().endsWith(DEBUGEXT)) {
                                    saveTo = new File(saveTo.getAbsolutePath() + DEBUGEXT);
                                }
                                IO.secureWrite(saveTo, org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().getBytes("UTF-8"));
                            } catch (DialogClosedException e1) {
                                e1.printStackTrace();
                            } catch (DialogCanceledException e1) {
                                e1.printStackTrace();
                            } catch (Exception e1) {
                                Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
                            }
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", "no data");
                        }
                    }
                });
                toolbardebug1.add(btnUserDebug1file);

                btnUserDebug2 = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -4020410143121908004L;

                    {
                        setName("Clear log");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().dellong_debuglog();
                        jd.gui.UserIO.getInstance().requestMessageDialog("9kw debug ", "Debuglog cleared");
                    }
                });
                toolbardebug1.add(btnUserDebug2);
                add(toolbardebug1, "gapleft 33,spanx,pushx,growx");

                MigPanel toolbardebug1ex = new MigPanel("ins 0", "[][][][]", "[]");
                btnUserDebug3 = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -622574297401313782L;

                    {
                        setName("Errorlog from your api key");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Browser br = new Browser();
                            String accountcheck = br.getPage(getAPIROOT() + "index.cgi?action=userapilog&jd2=1&user=" + CFG_9KWCAPTCHA.API_KEY + "&apikey=" + CFG_9KWCAPTCHA.API_KEY);
                            if (accountcheck.length() > 5) {
                                jd.gui.UserIO.getInstance().requestMessageDialog("Errorlog from your api key ", accountcheck);
                            } else {
                                jd.gui.UserIO.getInstance().requestMessageDialog("Errorlog from your api key ", "no data");
                            }
                        } catch (IOException e9kw) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "No connection or incorrect api key.");
                        }
                    }
                });
                toolbardebug1ex.add(btnUserDebug3);

                btnUserDebug3hoster = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 1700532687116057633L;

                    {
                        setName("List hoster");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String crazylonglist_hoster = "";
                        crazylonglist_hoster += "Hoster:\n";
                        for (LazyHostPlugin plg : HostPluginController.getInstance().list()) {
                            crazylonglist_hoster += plg.getDisplayName() + "\n";// plg.getAverageParseRuntime()
                        }

                        try {
                            Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, "9kw list", null, crazylonglist_hoster, NewTheme.getInstance().getIcon("proxy", 32), null, null);
                        } catch (DialogClosedException e2) {
                            e2.printStackTrace();
                        } catch (DialogCanceledException e2) {
                            e2.printStackTrace();
                        }
                    }
                });
                toolbardebug1ex.add(btnUserDebug3hoster);

                btnUserDebug3crawler = new ExtButton(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 1300532687116057633L;

                    {
                        setName("List crawler");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String crazylonglist_hoster = "";
                        crazylonglist_hoster += "Crawler:\n";
                        for (LazyCrawlerPlugin plg : CrawlerPluginController.getInstance().list()) {
                            crazylonglist_hoster += plg.getDisplayName() + "\n";// plg.getAverageCrawlRuntime()
                        }

                        try {
                            Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, "9kw list", null, crazylonglist_hoster, NewTheme.getInstance().getIcon("proxy", 32), null, null);
                        } catch (DialogClosedException e2) {
                            e2.printStackTrace();
                        } catch (DialogCanceledException e2) {
                            e2.printStackTrace();
                        }
                    }
                });
                toolbardebug1ex.add(btnUserDebug3crawler);
                add(toolbardebug1ex, "gapleft 33,spanx,pushx,growx");
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
    public String getDescription() {
        return _GUI._.NinekwService_getDescription_tt_();
    }

    // http://www.9kw.eu/hilfe.html#jdownloader-tab

}
