package org.jdownloader.captcha.v2.solver.dbc;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanelExtender;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.TextInput;
import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.dbc.api.Captcha;
import org.jdownloader.captcha.v2.solver.dbc.api.Client;
import org.jdownloader.captcha.v2.solver.dbc.api.SocketClient;
import org.jdownloader.captcha.v2.solver.dbc.api.User;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.settings.staticreferences.CFG_DBC;

public class DeathByCaptchaSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation, SolverService, ServicePanelExtender {
    public static final String                ID         = "dbc";
    private DeathByCaptchaSettings            config;
    private static final DeathByCaptchaSolver INSTANCE   = new DeathByCaptchaSolver();
    private ThreadPoolExecutor                threadPool = new ThreadPoolExecutor(0, 1, 30000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory());
    private LogSource                         logger;
    private SocketClient                      client;

    protected int getDefaultWaitForOthersTimeout() {
        return 30000;
    }

    @Override
    public Map<String, Integer> getWaitForOthersDefaultMap() {
        HashMap<String, Integer> ret = new HashMap<String, Integer>();
        // ret.put(Captcha9kwSolverClick.ID, 60000);
        // ret.put(DialogClickCaptchaSolver.ID, 60000);
        // ret.put(DialogBasicCaptchaSolver.ID, 60000);
        // ret.put(CaptchaAPISolver.ID, 60000);
        ret.put(JACSolver.ID, 30000);
        // ret.put(Captcha9kwSolver.ID, 60000);
        // ret.put(CaptchaMyJDSolver.ID, 60000);
        // ret.put(CBSolver.ID, 60000);
        // ret.put(DeathByCaptchaSolver.ID, 60000);

        return ret;
    }

    public static DeathByCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private DeathByCaptchaSolver() {
        super(Math.max(1, Math.min(25, JsonConfig.create(DeathByCaptchaSettings.class).getThreadpoolSize())));
        config = JsonConfig.create(DeathByCaptchaSettings.class);
        logger = LogController.getInstance().getLogger(DeathByCaptchaSolver.class.getName());
        AdvancedConfigManager.getInstance().register(config);
        threadPool.allowCoreThreadTimeOut(true);
        if (!Application.isHeadless()) {
            initServicePanel();
        }

    }

    /**
     * 
     */
    public void initServicePanel() {
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                ServicePanel.getInstance().addExtender(DeathByCaptchaSolver.this);

                CFG_DBC.USER_NAME.getEventSender().addListener(new GenericConfigEventListener<String>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
                CFG_DBC.PASSWORD.getEventSender().addListener(new GenericConfigEventListener<String>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });

                CFG_DBC.ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                    @Override
                    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                    }

                    @Override
                    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                        ServicePanel.getInstance().requestUpdate(true);
                    }
                });
            }

        });
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        return c instanceof BasicCaptchaChallenge && super.canHandle(c);
    }

    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {

        solveBasicCaptchaChallenge(job, (BasicCaptchaChallenge) job.getChallenge());

    }

    private void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws InterruptedException {

        if (config.getWhiteList() != null) {
            if (config.getWhiteList().length() > 5) {
                if (config.getWhiteList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on WhiteList for deathbycaptcha.eu. - " + challenge.getTypeID());
                } else {
                    job.getLogger().info("Hoster not on WhiteList for deathbycaptcha.eu. - " + challenge.getTypeID());
                    return;
                }
            }
        }
        if (config.getBlackList() != null) {
            if (config.getBlackList().length() > 5) {
                if (config.getBlackList().contains(challenge.getTypeID())) {
                    job.getLogger().info("Hoster on BlackList for deathbycaptcha.eu. - " + challenge.getTypeID());
                    return;
                } else {
                    job.getLogger().info("Hoster not on BlackList for deathbycaptcha.eu. - " + challenge.getTypeID());
                }
            }
        }
        job.showBubble(this);
        checkInterruption();
        try {

            Client client = getClient();
            Captcha captcha = null;

            // Put your CAPTCHA image file, file object, input stream,
            // or vector of bytes here:
            job.setStatus(SolverStatus.UPLOADING);
            captcha = client.upload(challenge.getImageFile());
            if (null != captcha) {
                job.setStatus(new SolverStatus(_GUI._.DeathByCaptchaSolver_solveBasicCaptchaChallenge_solving(), NewTheme.I().getIcon(IconKey.ICON_WAIT, 20)));
                job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " uploaded: " + captcha.id);
                long startTime = System.currentTimeMillis();
                // Poll for the uploaded CAPTCHA status.
                while (captcha.isUploaded() && !captcha.isSolved()) {

                    job.getLogger().info("deathbycaptcha.eu NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");

                    Thread.sleep(Client.POLLS_INTERVAL * 1000);
                    captcha = client.getCaptcha(captcha);
                }

                if (captcha.isSolved()) {
                    job.getLogger().info("CAPTCHA " + challenge.getImageFile() + " solved: " + captcha.text);
                    job.setAnswer(new DeathByCaptchaResponse(challenge, this, captcha));

                } else {
                    job.getLogger().info("Failed solving CAPTCHA");
                    throw new SolverException("Failed:" + captcha.toString());
                }
            }

        } catch (Exception e) {
            job.getLogger().log(e);
        }

    }

    private synchronized Client getClient() {
        if (client != null) {
            return client;
        }
        client = new SocketClient(config.getUserName(), config.getPassword());
        client.isVerbose = true;

        return client;

    }

    protected boolean validateLogins() {
        if (!CFG_DBC.ENABLED.isEnabled()) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.USER_NAME.getValue())) {
            return false;
        }
        if (StringUtils.isEmpty(CFG_DBC.PASSWORD.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public void setUnused(AbstractResponse<?> response, SolverJob<?> job) {
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response, SolverJob<?> job) {
        if (config.isFeedBackSendingEnabled() && response instanceof DeathByCaptchaResponse) {
            threadPool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        Captcha captcha = ((DeathByCaptchaResponse) response).getCaptcha();
                        // Report incorrectly solved CAPTCHA if neccessary.
                        // Make sure you've checked if the CAPTCHA was in fact
                        // incorrectly solved, or else you might get banned as
                        // abuser.
                        Client client = getClient();
                        Challenge<?> challenge = response.getChallenge();
                        if (challenge instanceof BasicCaptchaChallenge) {
                            if (client.report(captcha)) {
                                logger.info("CAPTCHA " + challenge + " reported as incorrectly solved");
                            } else {
                                logger.info("Failed reporting incorrectly solved CAPTCHA. Disabled Feedback");
                                config.setFeedBackSendingEnabled(false);
                            }
                        }

                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
        }
    }

    @Override
    public String getType() {
        return _GUI._.DeathByCaptchaSolver_getName_();
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (validateLogins()) {
            services.add(new ServiceCollection<DeathByCaptchaSolver>() {

                @Override
                public Icon getIcon() {
                    return DomainInfo.getInstance("deathbycaptcha.eu").getFavIcon();
                }

                @Override
                public boolean isEnabled() {
                    return config.isEnabled();
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return "deathbycaptcha.eu";
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelDBCTooltip(owner, DeathByCaptchaSolver.this);
                }

            });
        }
    }

    public DBCAccount loadAccount() {

        DBCAccount ret = new DBCAccount();
        try {
            Client c = getClient();
            User user = c.getUser();
            ret.setBalance(user.getBalance());
            ret.setBanned(user.isBanned());
            ret.setId(user.getId());
            ret.setRate(user.getRate());
        } catch (Exception e) {
            logger.log(e);
            ret.setError(e.getMessage());
        }
        return ret;

    }

    @Override
    public void setValid(AbstractResponse<?> response, SolverJob<?> job) {
    }

    @Override
    public Icon getIcon(int size) {
        return NewTheme.I().getIcon(IconKey.ICON_DBC, size);
    }

    @Override
    public AbstractCaptchaSolverConfigPanel getConfigPanel() {
        AbstractCaptchaSolverConfigPanel ret = new AbstractCaptchaSolverConfigPanel() {
            private TextInput     username;
            private TextInput     blacklist;
            private TextInput     whitelist;
            private PasswordInput password;

            @Override
            public String getPanelID() {
                return "CES_" + getTitle();
            }

            {
                addHeader(getTitle(), NewTheme.I().getIcon(ID, 32));
                addDescription(_GUI._.AntiCaptchaConfigPanel_onShow_description_ces());

                add(new SettingsButton(new AppAction() {
                    {
                        setName(_GUI._.lit_open_website());

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openURL("http://www.deathbycaptcha.eu/");

                    }
                }), "gapleft 37,spanx,pushx,growx");
                username = new TextInput(CFG_DBC.USER_NAME);
                password = new PasswordInput(CFG_DBC.PASSWORD);
                blacklist = new TextInput(CFG_DBC.BLACK_LIST);
                whitelist = new TextInput(CFG_DBC.WHITE_LIST);

                this.addHeader(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_(), NewTheme.I().getIcon(IconKey.ICON_LOGINS, 32));
                // addPair(_GUI._.MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled(), null, checkBox);
                this.addDescriptionPlain(_GUI._.dbcService_createPanel_logins_());
                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_enabled(), null, new Checkbox(CFG_DBC.ENABLED, username, password));
                addPair(_GUI._.captchabrotherhoodService_createPanel_username(), null, username);
                addPair(_GUI._.captchabrotherhoodService_createPanel_password(), null, password);

                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_feedback(), null, new Checkbox(CFG_DBC.FEED_BACK_SENDING_ENABLED));

                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_blacklist(), null, blacklist);
                addPair(_GUI._.DeatchbyCaptcha_Service_createPanel_whitelist(), null, whitelist);

            }

            @Override
            public void save() {

            }

            @Override
            public void updateContents() {
            }

            @Override
            public Icon getIcon() {
                return DeathByCaptchaSolver.this.getIcon(32);
            }

            @Override
            public String getTitle() {
                return "deathbycaptcha.eu";
            }

        };
        return ret;
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getName() {
        return _GUI._.DeathByCaptchaSolver_gettypeName_();
    }

    @Override
    public ChallengeSolverConfig getConfig() {
        return config;
    }

}
