package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

import jd.controlling.ClipboardMonitoring;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.CaptchaRegexListTextPane;
import jd.http.Browser;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_9KWCAPTCHA;
import org.jdownloader.updatev2.gui.LAFOptions;

public final class NineKwConfigPanel extends AbstractCaptchaSolverConfigPanel {
    private ExtButton                      btnRegister;
    private ExtButton                      btnApi;
    private ExtButton                      btnPlugins;
    private ExtButton                      btnFAQ;
    private ExtButton                      btnSupport;
    private ExtButton                      btnHelp;
    private ExtButton                      btnApiDocu;
    private ExtButton                      btnUserhistory;
    private ExtButton                      btnUserCheck;
    private ExtButton                      btnUserConfig;
    private ExtButton                      btnUserBuy;
    private ExtButton                      btnUserDebug1;
    private ExtButton                      btnUserDebug1clipboard;
    private ExtButton                      btnUserDebug1file;
    private ExtButton                      btnUserDebug2;
    private ExtButton                      btnUserDebug3;
    private ExtButton                      btnUserDebugStatReset;
    private ExtButton                      btnUserDebugBlacklistReset;
    private ExtButton                      btnUserDebugStatShow;
    private ExtButton                      btnUserDebugBlacklistShow;
    private ExtButton                      btnUserDebugBubbleShow;
    private Captcha9kwSettings             config;
    public static final String             DEBUGEXT         = ".txt";
    private static final long              serialVersionUID = -1805335184795063609L;
    private TextInput                      apiKey;
    private TextInput                      hosteroptions;
    private TextInput                      blacklist;
    private TextInput                      whitelist;
    private Pair<CaptchaRegexListTextPane> blacklistnew;
    private Pair<CaptchaRegexListTextPane> whitlistnew;
    private TextInput                      blacklistprio;
    private TextInput                      whitelistprio;
    private TextInput                      blacklisttimeout;
    private TextInput                      whitelisttimeout;
    private NineKwSolverService            service;

    public NineKwConfigPanel(NineKwSolverService nineKwSolverService) {
        this.service = nineKwSolverService;
        JTabbedPane tabbedPane = new JTabbedPane();
        // Tab 1
        JPanel Tab1_9kw = new JPanel(new MigLayout("ins 0"));
        Tab1_9kw.add(new Header(getTitle(), new AbstractIcon(IconKey.ICON_LOGO_9KW, 32)), "spanx,growx,pushx");
        JLabel txt = addDescriptionPlain9kw(_GUI.T.AntiCaptchaConfigPanel_onShow_description_ces());
        Tab1_9kw.add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        Tab1_9kw.add(new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5");
        Tab1_9kw.add(new SettingsButton(new AppAction() {
            private static final long serialVersionUID = 8804949739472915394L;
            {
                setName(_GUI.T.lit_open_website());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURL(getAPIROOT());
            }
        }), "gapleft 37,spanx,pushx,growx");
        MigPanel toolbar1 = new MigPanel("ins 0", "[][][][]", "[]");
        MigPanel toolbar2 = new MigPanel("ins 0", "[][][][]", "[]");
        btnRegister = addClickButton9kw(btnRegister, _GUI.T.NinekwService_createPanel_btnRegister(), getAPIROOT() + "register.html", _GUI.T.NinekwService_createPanel_btnRegister_tooltiptext());
        btnApi = addClickButton9kw(btnApi, _GUI.T.NinekwService_createPanel_btnApi(), getAPIROOT() + "userapi.html", _GUI.T.NinekwService_createPanel_btnApi_tooltiptext());
        btnPlugins = addClickButton9kw(btnPlugins, _GUI.T.NinekwService_createPanel_btnPlugins(), getAPIROOT() + "plugins.html", _GUI.T.NinekwService_createPanel_btnPlugins_tooltiptext());
        btnFAQ = addClickButton9kw(btnFAQ, _GUI.T.NinekwService_createPanel_btnFAQ(), getAPIROOT() + "faq.html", _GUI.T.NinekwService_createPanel_btnFAQ_tooltiptext());
        btnHelp = addClickButton9kw(btnHelp, _GUI.T.NinekwService_createPanel_btnHelp(), getAPIROOT() + "hilfe.html", _GUI.T.NinekwService_createPanel_btnHelp_tooltiptext());
        btnSupport = addClickButton9kw(btnSupport, _GUI.T.NinekwService_createPanel_btnSupport(), getAPIROOT() + "kontakt.html", _GUI.T.NinekwService_createPanel_btnSupport_tooltiptext());
        btnUserhistory = addClickButton9kw(btnUserhistory, _GUI.T.NinekwService_createPanel_btnUserhistory(), getAPIROOT() + "userhistory.html", _GUI.T.NinekwService_createPanel_btnUserhistory_tooltiptext());
        toolbar1.add(btnRegister, "pushx,growx,sg 1,height 26!");
        toolbar1.add(btnPlugins, "pushx,growx,sg 1,height 26!");
        toolbar1.add(btnFAQ, "pushx,growx,sg 1,height 26!");
        toolbar1.add(btnHelp, "pushx,growx,sg 1,height 26!");
        Tab1_9kw.add(toolbar1, "gapleft 37,spanx,pushx,growx");
        toolbar2.add(btnApi, "pushx,growx,sg 1,height 26!");
        toolbar2.add(btnUserhistory, "pushx,growx,sg 1,height 26!");
        toolbar2.add(btnSupport, "pushx,growx,sg 1,height 26!");
        Tab1_9kw.add(toolbar2, "gapleft 37,spanx,pushx,growx");
        Tab1_9kw.add(new Header(_GUI.T.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32)), "spanx,growx,pushx");
        JLabel txt_myaccount = addDescriptionPlain9kw(_GUI.T.NinekwService_createPanel_logins_());
        Tab1_9kw.add(txt_myaccount, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        apiKey = new TextInput(CFG_9KWCAPTCHA.API_KEY);
        apiKey.setHelpText(_GUI.T.NinekwService_createPanel_apikey_helptext());
        apiKey.setToolTipText(_GUI.T.NinekwService_createPanel_apikey_tooltipText());
        hosteroptions = new TextInput(CFG_9KWCAPTCHA.HOSTEROPTIONS);
        hosteroptions.setToolTipText(_GUI.T.NinekwService_createPanel_hosteroptions_tooltiptext());
        blacklist = new TextInput(CFG_9KWCAPTCHA.BLACKLIST);
        blacklist.setToolTipText(_GUI.T.NinekwService_createPanel_blacklist_tooltiptext());
        whitelist = new TextInput(CFG_9KWCAPTCHA.WHITELIST);
        whitelist.setToolTipText(_GUI.T.NinekwService_createPanel_whitelist_tooltiptext());
        blacklistprio = new TextInput(CFG_9KWCAPTCHA.BLACKLISTPRIO);
        blacklistprio.setToolTipText(_GUI.T.NinekwService_createPanel_blacklistprio_tooltiptext());
        whitelistprio = new TextInput(CFG_9KWCAPTCHA.WHITELISTPRIO);
        whitelistprio.setToolTipText(_GUI.T.NinekwService_createPanel_whitelistprio_tooltiptext());
        blacklisttimeout = new TextInput(CFG_9KWCAPTCHA.BLACKLISTTIMEOUT);
        blacklisttimeout.setToolTipText(_GUI.T.NinekwService_createPanel_blacklisttimeout_tooltiptext());
        whitelisttimeout = new TextInput(CFG_9KWCAPTCHA.WHITELISTTIMEOUT);
        whitelisttimeout.setToolTipText(_GUI.T.NinekwService_createPanel_whitelisttimeout_tooltiptext());
        MigPanel toolbar3 = new MigPanel("ins 0", "[][][]", "[]");
        toolbar3.add(label(_GUI.T.NinekwService_createPanel_enabled()), "width 135!");
        Checkbox textcaptchas = new Checkbox(CFG_9KWCAPTCHA.ENABLED);
        textcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_textcaptchas_tooltiptext());
        toolbar3.add(textcaptchas);
        toolbar3.add(label(_GUI.T.NinekwService_createPanel_textcaptchas()));
        Checkbox mousecaptchas = new Checkbox(CFG_9KWCAPTCHA.MOUSE);
        mousecaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_mousecaptchas_tooltiptext());
        toolbar3.add(mousecaptchas);
        toolbar3.add(label(_GUI.T.NinekwService_createPanel_mousecaptchas()));
        Checkbox puzzlecaptchas = new Checkbox(CFG_9KWCAPTCHA.PUZZLE);
        puzzlecaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_puzzlecaptchas_tooltiptext());
        puzzlecaptchas.setEnabled(true);
        toolbar3.add(puzzlecaptchas);
        toolbar3.add(label(_GUI.T.NinekwService_createPanel_puzzlecaptchas()));
        Tab1_9kw.add(toolbar3, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar4 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar4.add(label(_GUI.T.NinekwService_createPanel_apikey()), "width 135!");
        toolbar4.add(apiKey, "pushx,growx");
        btnUserCheck = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -103695205004891917L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserCheck());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (apiKey.getText().length() < 5) {
                    jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), "No api key.");
                } else if (!apiKey.getText().matches("^[a-zA-Z0-9]*$")) {
                    jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), _GUI.T.NinekwService_createPanel_errortext_wrongapikey1() + "\n" + _GUI.T.NinekwService_createPanel_errortext_wrongapikey2() + "\n");
                } else {
                    try {
                        Browser br = new Browser();
                        String accountcheck = br.getPage(getAPIROOT() + "index.cgi?action=usercaptchaguthaben&source=jd2&okcheck=1&apikey=" + CFG_9KWCAPTCHA.API_KEY.getValue());
                        String errorcheck = br.getRegex("^([0-9]+ .*)").getMatch(0);
                        if (accountcheck.startsWith("OK-")) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw message ", "Account OK\nCredits: " + accountcheck.substring("OK-".length()));
                        } else if (errorcheck != null) {
                            jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), "Account error\n" + accountcheck);
                        } else if (accountcheck.length() > 5) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error ", "Unknown error or incorrect api key.");
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(1) ", "Unknown error.");
                        }
                    } catch (IOException e9kw) {
                        if (config.ishttps()) {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(2) ", "No connection or unknown error. Please deactivate https und try it again.");
                        } else {
                            jd.gui.UserIO.getInstance().requestMessageDialog("9kw error(2) ", "No connection or unknown error.");
                        }
                    }
                }
            }
        });
        btnUserCheck.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserCheck_tooltiptext());
        toolbar4.add(btnUserCheck);
        btnUserBuy = addClickButton9kw(btnUserBuy, _GUI.T.NinekwService_createPanel_btnUserbuy(), getAPIROOT() + "buy.html", _GUI.T.NinekwService_createPanel_btnUserbuy_tooltiptext());
        toolbar4.add(btnUserBuy);
        Tab1_9kw.add(toolbar4, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar6 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_options_header()), "width 135!");
        Checkbox feedbackcaptchas = new Checkbox(CFG_9KWCAPTCHA.FEEDBACK);
        feedbackcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_feedback_tooltiptext());
        toolbar6.add(feedbackcaptchas);
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_feedback()));
        Checkbox httpscaptchas = new Checkbox(CFG_9KWCAPTCHA.HTTPS);
        httpscaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_https_tooltiptext());
        toolbar6.add(httpscaptchas);
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_https()));
        Checkbox selfsolvecaptchas = new Checkbox(CFG_9KWCAPTCHA.SELFSOLVE);
        selfsolvecaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_selfsolve_tooltiptext());
        toolbar6.add(selfsolvecaptchas);
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_selfsolve()));
        Checkbox lowcreditscaptchas = new Checkbox(CFG_9KWCAPTCHA.LOWCREDITS);
        lowcreditscaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_lowcredits_tooltiptext());
        toolbar6.add(lowcreditscaptchas);
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_lowcredits()));
        Checkbox highqueuecaptchas = new Checkbox(CFG_9KWCAPTCHA.HIGHQUEUE);
        highqueuecaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_highqueue_tooltiptext());
        toolbar6.add(highqueuecaptchas);
        toolbar6.add(label(_GUI.T.NinekwService_createPanel_highqueue()));
        Tab1_9kw.add(toolbar6, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar7 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar7.add(label(" "), "width 135!");
        Checkbox confirmcaptchas = new Checkbox(CFG_9KWCAPTCHA.CONFIRM);
        confirmcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_confirm_tooltiptext());
        toolbar7.add(confirmcaptchas);
        toolbar7.add(label(_GUI.T.NinekwService_createPanel_confirm()));
        Checkbox mouseconfirmcaptchas = new Checkbox(CFG_9KWCAPTCHA.MOUSECONFIRM);
        mouseconfirmcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_mouseconfirm_tooltiptext());
        toolbar7.add(mouseconfirmcaptchas);
        toolbar7.add(label(_GUI.T.NinekwService_createPanel_mouseconfirm()));
        Tab1_9kw.add(toolbar7, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar8 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar8.add(label(" "), "width 135!");
        Spinner priocaptchas = new Spinner(CFG_9KWCAPTCHA.PRIO);
        priocaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_prio_tooltiptext());
        toolbar8.add(priocaptchas);
        toolbar8.add(label(_GUI.T.NinekwService_createPanel_prio()));
        Spinner hourcaptchas = new Spinner(CFG_9KWCAPTCHA.HOUR);
        hourcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_hour_tooltiptext());
        toolbar8.add(hourcaptchas);
        toolbar8.add(label(_GUI.T.NinekwService_createPanel_hour()));
        Spinner mincaptchas = new Spinner(CFG_9KWCAPTCHA.MINUTE);
        mincaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_minute_tooltiptext());
        toolbar8.add(mincaptchas);
        toolbar8.add(label(_GUI.T.NinekwService_createPanel_minute()));
        toolbar8.add(label(_GUI.T.NinekwService_createPanel_unlimited()));
        Tab1_9kw.add(toolbar8, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar9 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar9.add(label(" "), "width 135!");
        Spinner poolcaptchas = new Spinner(CFG_9KWCAPTCHA.THREADPOOL_SIZE);
        poolcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_threadsizepool_tooltiptext());
        toolbar9.add(poolcaptchas);
        toolbar9.add(label(_GUI.T.NinekwService_createPanel_threadsizepool()));
        Tab1_9kw.add(toolbar9, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar9a = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar9a.add(label(" "), "width 135!");
        tabbedPane.addTab(_GUI.T.NinekwService_createPanel_general_header(), Tab1_9kw);
        // Tab 2
        JPanel Tab2_9kw = new JPanel(new MigLayout("ins 0"));
        Tab2_9kw.add(new Header(_GUI.T.captcha_settings_black_whitelist_header(), NewTheme.I().getIcon(IconKey.ICON_LIST, 32)), "spanx,growx,pushx");
        JLabel txt_blackwhitelist3 = addDescriptionPlain9kw(_GUI.T.captcha_settings_black_whitelist_description());
        Tab2_9kw.add(txt_blackwhitelist3, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        BooleanKeyHandler keyHandler = CFG_9KWCAPTCHA.CFG._getStorageHandler().getKeyHandler("BlackWhiteListingEnabled", BooleanKeyHandler.class);
        Pair<Checkbox> condition = null;
        if (keyHandler != null) {
            condition = addPair9kw(_GUI.T.captcha_settings_blacklist_enabled(), null, new jd.gui.swing.jdgui.views.settings.components.Checkbox(keyHandler), Tab2_9kw);
        }
        blacklistnew = addPair9kw(_GUI.T.captcha_settings_blacklist(), null, new CaptchaRegexListTextPane(), Tab2_9kw);
        whitlistnew = addPair9kw(_GUI.T.captcha_settings_whitelist(), null, new CaptchaRegexListTextPane(), Tab2_9kw);
        if (condition != null) {
            blacklistnew.setConditionPair(condition);
            whitlistnew.setConditionPair(condition);
        }
        blacklistnew.getComponent().setList(CFG_9KWCAPTCHA.CFG.getBlacklistEntries());
        whitlistnew.getComponent().setList(CFG_9KWCAPTCHA.CFG.getWhitelistEntries());
        blacklistnew.getComponent().addStateUpdateListener(new StateUpdateListener() {
            @Override
            public void onStateUpdated() {
                CFG_9KWCAPTCHA.CFG.setBlacklistEntries(new ArrayList<String>(blacklistnew.getComponent().getList()));
            }
        });
        whitlistnew.getComponent().addStateUpdateListener(new StateUpdateListener() {
            @Override
            public void onStateUpdated() {
                CFG_9KWCAPTCHA.CFG.setWhitelistEntries(new ArrayList<String>(whitlistnew.getComponent().getList()));
            }
        });
        Tab2_9kw.add(new Header(_GUI.T.NinekwService_createPanel_blackwhitelist_title(), NewTheme.I().getIcon(IconKey.ICON_SELECT, 32)), "spanx,growx,pushx");
        JLabel txt_blackwhitelist = addDescriptionPlain9kw(_GUI.T.NinekwService_createPanel_blackwhitelist_des());
        Tab2_9kw.add(txt_blackwhitelist, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        MigPanel toolbar10 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar10.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTCHECK));
        toolbar10.add(label(_GUI.T.NinekwService_createPanel_blacklist()));
        toolbar10.add(blacklist, "pushx,growx");
        Tab2_9kw.add(toolbar10, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar11 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar11.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTCHECK));
        toolbar11.add(label(_GUI.T.NinekwService_createPanel_whitelist()));
        toolbar11.add(whitelist, "pushx,growx");
        Tab2_9kw.add(toolbar11, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar13t3 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar13t3.add(label(" "));
        Tab2_9kw.add(toolbar13t3, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar12 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar12.add(new Checkbox(CFG_9KWCAPTCHA.BLACKLISTPRIOCHECK));
        toolbar12.add(label(_GUI.T.NinekwService_createPanel_blacklistprio()));
        toolbar12.add(blacklistprio, "pushx,growx");
        Tab2_9kw.add(toolbar12, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar13 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar13.add(new Checkbox(CFG_9KWCAPTCHA.WHITELISTPRIOCHECK));
        toolbar13.add(label(_GUI.T.NinekwService_createPanel_whitelistprio()));
        toolbar13.add(whitelistprio, "pushx,growx");
        Tab2_9kw.add(toolbar13, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar13t0 = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar13t0.add(label(" "));
        Tab2_9kw.add(toolbar13t0, "gapleft 33,spanx,pushx,growx");
        tabbedPane.addTab("Black-/Whitelist", Tab2_9kw);
        // Tab 3
        JPanel Tab3_9kw = new JPanel(new MigLayout("ins 0"));
        Tab3_9kw.add(new Header(_GUI.T.NinekwService_createPanel_options_header(), NewTheme.I().getIcon(IconKey.ICON_FOLDER_ADD, 32)), "spanx,growx,pushx");
        MigPanel toolbar9c = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar9c.add(label(_GUI.T.NinekwService_createPanel_hosteroptions()), "width 130!");
        toolbar9c.add(hosteroptions, "pushx,growx");
        Tab3_9kw.add(toolbar9c, "gapleft 33,spanx,pushx,growx");
        JLabel txt_blackwhitelist1 = addDescriptionPlain9kw(_GUI.T.NinekwService_createPanel_hosteroptions_description1());
        Tab3_9kw.add(txt_blackwhitelist1, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        JLabel txt_blackwhitelist2 = addDescriptionPlain9kw(_GUI.T.NinekwService_createPanel_hosteroptions_description2());
        Tab3_9kw.add(txt_blackwhitelist2, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        MigPanel toolbar9cA = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar9cA.add(label(_GUI.T.NinekwService_createPanel_hosteroptions_more()), "width 130!");
        btnApiDocu = addClickButton9kw(btnApiDocu, _GUI.T.NinekwService_createPanel_hosteroptions_api_button(), getAPIROOT() + "api.html", _GUI.T.NinekwService_createPanel_hosteroptions_api_button_tooltip());
        toolbar9cA.add(btnApiDocu, "gaptop 0,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        Tab3_9kw.add(toolbar9cA, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbar9cB = new MigPanel("ins 0", "[][][][]", "[]");
        toolbar9cB.add(label(_GUI.T.NinekwService_createPanel_hosteroptions_userconfig()), "width 130!");
        btnUserConfig = addClickButton9kw(btnUserConfig, _GUI.T.NinekwService_createPanel_hosteroptions_userconfig_button(), getAPIROOT() + "userconfig.html", _GUI.T.NinekwService_createPanel_hosteroptions_userconfig_button_tooltip());
        toolbar9cB.add(btnUserConfig, "gaptop 0,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        Tab3_9kw.add(toolbar9cB, "gapleft 33,spanx,pushx,growx");
        tabbedPane.addTab("Options", Tab3_9kw);
        // Tab 4
        JPanel Tab4_9kw = new JPanel(new MigLayout("ins 0"));
        Tab4_9kw.add(new Header(_GUI.T.NinekwService_createPanel_debug(), NewTheme.I().getIcon(IconKey.ICON_EVENT, 32)), "spanx,growx,pushx");
        MigPanel toolbardebug1 = new MigPanel("ins 0", "[][][][]", "[]");
        JLabel txt_debug1 = addDescriptionPlain9kw(_GUI.T.NinekwService_createPanel_debug_description());
        Tab4_9kw.add(txt_debug1, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        Checkbox debugcaptchas = new Checkbox(CFG_9KWCAPTCHA.DEBUG);
        debugcaptchas.setToolTipText(_GUI.T.NinekwService_createPanel_debugcaptchas_tooltiptext());
        toolbardebug1.add(debugcaptchas);
        toolbardebug1.add(label(_GUI.T.NinekwService_createPanel_debugcaptchas()));
        btnUserDebug1 = new ExtButton(new AppAction() {
            private static final long serialVersionUID = 1700532687116057633L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebug1());
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
                    jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", _GUI.T.NinekwService_createPanel_btnUserDebug1_text());
                }
            }
        });
        btnUserDebug1.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebug1_tooltiptext());
        toolbardebug1.add(btnUserDebug1);
        btnUserDebug1clipboard = new ExtButton(new AppAction() {
            private static final long serialVersionUID = 1700532687116057633L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebug1clipboard());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().length() > 1) {
                    ClipboardMonitoring.getINSTANCE().setCurrentContent(org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog());
                } else {
                    jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", _GUI.T.NinekwService_createPanel_btnUserDebug1clipboard_text());
                }
            }
        });
        btnUserDebug1clipboard.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebug1clipboard_tooltiptext());
        toolbardebug1.add(btnUserDebug1clipboard);
        btnUserDebug1file = new ExtButton(new AppAction() {
            /**
             * Save debuglog as file
             */
            private static final long serialVersionUID = 1700542687116057633L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebug1file());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().getlong_debuglog().length() > 1) {
                    ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.SaveAsProxyProfileAction_actionPerformed_choose_file(), null, null);
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
                        Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e1.getMessage(), e1);
                    }
                } else {
                    jd.gui.UserIO.getInstance().requestMessageDialog("9kw debuglog ", _GUI.T.NinekwService_createPanel_btnUserDebug1file_text());
                }
            }
        });
        btnUserDebug1file.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebug1file_tooltiptext());
        toolbardebug1.add(btnUserDebug1file);
        btnUserDebug2 = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -4020410143121908004L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebug2());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSolver.getInstance().dellong_debuglog();
                jd.gui.UserIO.getInstance().requestMessageDialog("9kw debug ", _GUI.T.NinekwService_createPanel_btnUserDebug2_text());
            }
        });
        btnUserDebug2.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebug2_tooltiptext());
        toolbardebug1.add(btnUserDebug2);
        Tab4_9kw.add(toolbardebug1, "gapleft 33,spanx,pushx,growx");
        MigPanel toolbardebug1ex = new MigPanel("ins 0", "[][][][]", "[]");
        btnUserDebug3 = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -622574297401313782L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebug3());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Browser br = new Browser();
                    String accountcheck = br.getPage(getAPIROOT() + "index.cgi?action=userapilog&jd2=1&user=" + CFG_9KWCAPTCHA.API_KEY.getValue() + "&apikey=" + CFG_9KWCAPTCHA.API_KEY.getValue());
                    if (accountcheck.length() > 5) {
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_btnUserDebug3(), accountcheck);
                    } else {
                        jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_btnUserDebug3(), _GUI.T.NinekwService_createPanel_btnUserDebug3_text());
                    }
                } catch (IOException e9kw) {
                    jd.gui.UserIO.getInstance().requestMessageDialog(_GUI.T.NinekwService_createPanel_error9kwtitle(), "No connection or incorrect api key.");
                }
            }
        });
        btnUserDebug3.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebug3());
        toolbardebug1ex.add(btnUserDebug3);
        Tab4_9kw.add(toolbardebug1ex, "gapleft 33,spanx,pushx,growx");
        btnUserDebugStatReset = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -4020410143121908004L;
            {
                setName(_GUI.T.NinekwService_createPanel_btnUserDebugStatReset());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Captcha9kwSolver.getInstance().counterSolved.set(0);
                Captcha9kwSolver.getInstance().counterInterrupted.set(0);
                Captcha9kwSolver.getInstance().counter.set(0);
                Captcha9kwSolver.getInstance().counterSend.set(0);
                Captcha9kwSolver.getInstance().counterSendError.set(0);
                Captcha9kwSolver.getInstance().counterOK.set(0);
                Captcha9kwSolver.getInstance().counterNotOK.set(0);
                Captcha9kwSolver.getInstance().counterUnused.set(0);
                Captcha9kwSolverClick.getInstance().counterSolved.set(0);
                Captcha9kwSolverClick.getInstance().counterInterrupted.set(0);
                Captcha9kwSolverClick.getInstance().counter.set(0);
                Captcha9kwSolverClick.getInstance().counterSend.set(0);
                Captcha9kwSolverClick.getInstance().counterSendError.set(0);
                Captcha9kwSolverClick.getInstance().counterOK.set(0);
                Captcha9kwSolverClick.getInstance().counterNotOK.set(0);
                Captcha9kwSolverClick.getInstance().counterUnused.set(0);
                jd.gui.UserIO.getInstance().requestMessageDialog("9kw debug ", _GUI.T.NinekwService_createPanel_btnUserDebugStatReset_text());
            }
        });
        btnUserDebugStatReset.setToolTipText(_GUI.T.NinekwService_createPanel_btnUserDebugStatReset_tooltiptext());
        toolbardebug1.add(btnUserDebugStatReset);
        btnUserDebugStatShow = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -4020410143121908004L;
            {
                setName("Show Stats");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                String crazylist = "";
                crazylist += "OK: " + (Captcha9kwSolverClick.getInstance().counterOK.get() + Captcha9kwSolver.getInstance().counterOK.get()) + "\n";
                crazylist += " NotOK: " + (Captcha9kwSolverClick.getInstance().counterNotOK.get() + Captcha9kwSolver.getInstance().counterNotOK.get()) + "\n";
                crazylist += " Solved: " + (Captcha9kwSolverClick.getInstance().counterSolved.get() + Captcha9kwSolver.getInstance().counterSolved.get()) + "\n";
                crazylist += " Unused: " + (Captcha9kwSolverClick.getInstance().counterUnused.get() + Captcha9kwSolver.getInstance().counterUnused.get()) + "\n";
                crazylist += " Interrupted: " + (Captcha9kwSolverClick.getInstance().counterInterrupted.get() + Captcha9kwSolver.getInstance().counterInterrupted.get()) + "\n";
                crazylist += " Send: " + (Captcha9kwSolverClick.getInstance().counterSend.get() + Captcha9kwSolver.getInstance().counterSend.get()) + "\n";
                crazylist += " SendError: " + (Captcha9kwSolverClick.getInstance().counterSendError.get() + Captcha9kwSolver.getInstance().counterSendError.get()) + "\n";
                crazylist += " All: " + (Captcha9kwSolverClick.getInstance().counter.get() + Captcha9kwSolver.getInstance().counter.get()) + "\n";
                try {
                    Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, "9kw stats", null, crazylist, NewTheme.getInstance().getIcon("proxy", 32), null, null);
                } catch (DialogClosedException e2) {
                    e2.printStackTrace();
                } catch (DialogCanceledException e2) {
                    e2.printStackTrace();
                }
            }
        });
        toolbardebug1.add(btnUserDebugStatShow);
        btnUserDebugBubbleShow = new ExtButton(new AppAction() {
            private static final long serialVersionUID = -4020410143121908004L;
            {
                setName("Show BubbleMap");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                String crazylist = config.getBubbleTimeoutByHostMap().toString();
                try {
                    Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | UIOManager.BUTTONS_HIDE_CANCEL, "9kw BubbleTimeoutByHostMap", null, crazylist, NewTheme.getInstance().getIcon("proxy", 32), null, null);
                } catch (DialogClosedException e2) {
                    e2.printStackTrace();
                } catch (DialogCanceledException e2) {
                    e2.printStackTrace();
                }
            }
        });
        toolbardebug1.add(btnUserDebugBubbleShow);
        tabbedPane.addTab("Debug", Tab4_9kw);
        int tabcount = tabbedPane.getTabCount();
        tabbedPane.setSelectedIndex(0);
        add(tabbedPane, "gapleft 37,spanx,pushx,growx");
    }

    private MigPanel addHelpText9kw(String descriptiontemp, String descriptionwidthtemp) {
        MigPanel tempmig = new MigPanel("ins 0", "[][][][]", "[]");
        if (descriptionwidthtemp.equalsIgnoreCase("105")) {
            tempmig.add(label(""), "width " + descriptionwidthtemp + "!");
        } else {
            tempmig.add(label(descriptiontemp), "width " + descriptionwidthtemp + "!");
        }
        if (descriptionwidthtemp.equalsIgnoreCase("105")) {
            JLabel txttemp = addDescriptionPlain9kw(descriptiontemp);
            tempmig.add(txttemp, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10");
        }
        return tempmig;
    }

    private JLabel addDescriptionPlain9kw(String description) {
        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "<html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        // txt.setEnabled(false);
        LAFOptions.getInstance().applyConfigDescriptionTextColor(txt);
        txt.setText(description);
        return txt;
    }

    private ExtButton addClickButton9kw(ExtButton btnTemp, final String title, final String url, String tooltext) {
        btnTemp = new ExtButton(new AppAction() {
            private static final long serialVersionUID = 7195034001951861669L;
            {
                setName(title);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                CrossSystem.openURL(url);
            }
        });
        btnTemp.setToolTipText(tooltext);
        return btnTemp;
    }

    private <T extends SettingsComponent> Pair<T> addPair9kw(String name, BooleanKeyHandler enabled, T comp, JPanel TabTemp) {
        String lblConstraints = "gapleft" + getLeftGap() + ",aligny " + (comp.isMultiline() ? "top" : "center");
        return addPair9kw(name, lblConstraints, enabled, comp, TabTemp);
    }

    private <T extends SettingsComponent> Pair<T> addPair9kw(String name, String lblConstraints, BooleanKeyHandler enabled, T comp, JPanel TabTemp) {
        JLabel lbl = createLabel(name);
        MigPanel toolbarTemp = new MigPanel("ins 0", "[][][]", "[]");
        toolbarTemp.add(lbl, lblConstraints);
        ExtCheckBox cb = null;
        String con = "pushx,grow";
        if (enabled != null) {
            cb = new ExtCheckBox(enabled, lbl, (JComponent) comp);
            SwingUtils.setOpaque(cb, false);
            toolbarTemp.add(cb, "width " + cb.getPreferredSize().width + "!, aligny " + (comp.isMultiline() ? "top" : "center"));
            cb.setToolTipText(_GUI.T.AbstractConfigPanel_addPair_enabled());
        }
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }
        toolbarTemp.add((JComponent) comp, con);
        TabTemp.add(toolbarTemp, "gapleft 33,spanx,pushx,growx");
        Pair<T> p = new Pair<T>(lbl, comp, cb);
        pairs.add(p);
        return p;
    }

    @Override
    public String getPanelID() {
        return "CES_" + getTitle();
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

    @Override
    public Icon getIcon() {
        return service.getIcon(32);
    }

    @Override
    public String getTitle() {
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

    public String getDescription() {
        return _GUI.T.NinekwService_getDescription_tt_();
    }
}