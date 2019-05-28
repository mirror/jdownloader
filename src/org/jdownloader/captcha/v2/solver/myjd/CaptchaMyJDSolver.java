package org.jdownloader.captcha.v2.solver.myjd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.plugins.Account;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64OutputStream;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1CaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge.TYPE;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class CaptchaMyJDSolver extends CESChallengeSolver<String> {
    private final CaptchaMyJDSolverConfig  config;
    private final LogSource                logger;
    private final ArrayList<Request>       lastChallenge;
    private static final CaptchaMyJDSolver INSTANCE = new CaptchaMyJDSolver();

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<String> job, BasicCaptchaChallenge challenge) throws SolverException {
        // not used solveCES Overwritten
    }

    public static CaptchaMyJDSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private class Request {
        public Request(String id2) {
            this.id = id2;
            timestamp = System.currentTimeMillis();
        }

        public long   timestamp;
        public String id;
    }

    @Override
    public CaptchaMyJDSolverService getService() {
        return (CaptchaMyJDSolverService) super.getService();
    }

    private CaptchaMyJDSolver() {
        super(new CaptchaMyJDSolverService(), 5);
        getService().setSolver(this);
        logger = LogController.getInstance().getLogger(CaptchaMyJDSolver.class.getName());
        config = JsonConfig.create(CaptchaMyJDSolverConfig.class);
        lastChallenge = new ArrayList<Request>();
    }

    private final boolean enabled = true;

    private Plugin getPluginFromThread() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof AccountCheckerThread) {
            final AccountCheckJob job = ((AccountCheckerThread) thread).getJob();
            if (job != null) {
                final Account account = job.getAccount();
                return account.getPlugin();
            }
        } else if (thread instanceof LinkCheckerThread) {
            final PluginForHost plg = ((LinkCheckerThread) thread).getPlugin();
            if (plg != null) {
                return plg;
            }
        } else if (thread instanceof SingleDownloadController) {
            return ((SingleDownloadController) thread).getDownloadLinkCandidate().getCachedAccount().getPlugin();
        } else if (thread instanceof LinkCrawlerThread) {
            final Object owner = ((LinkCrawlerThread) thread).getCurrentOwner();
            if (owner instanceof Plugin) {
                return (Plugin) owner;
            }
        }
        return null;
    }

    public boolean isEnabled() {
        if (!CFG_GENERAL.CFG.isMyJDownloaderCaptchaSolverEnabled()) {
            return false;
        }
        return getService().getConfig().isEnabled();
    }

    public boolean canHandle() {
        if (!CFG_GENERAL.CFG.isMyJDownloaderCaptchaSolverEnabled()) {
            return false;
        }
        final boolean myEn = MyJDownloaderController.getInstance().isRemoteCaptchaServiceEnabled();
        return myEn && isEnabled() && validateLogins();
    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        try {
            final boolean myEn = MyJDownloaderController.getInstance().isRemoteCaptchaServiceEnabled();
            logger.info("Is Enabled:" + c);
            if (myEn) {
                if (c instanceof BasicCaptchaChallenge && super.canHandle(c)) {
                    final boolean validLogins = validateLogins();
                    logger.info("Is Valid:" + validLogins);
                    if (validLogins) {
                        if (c instanceof RecaptchaV1CaptchaChallenge) {
                            logger.info("is RC1");
                        }
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable e) {
            logger.log(e);
            return false;
        }
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
        job.getLogger().info(this + ": Start. GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        // int timeoutthing = (JsonConfig.create(CaptchaSettings.class).getCaptchaDialogMyJDCESTimeout() / 1000);
        job.getLogger().info(this + ": Upload Captcha. GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        try {
            // job.showBubble(this);
            checkInterruption();
            // Browser br = new Browser();
            // br.setAllowedResponseCodes(new int[] { 500 });
            job.showBubble(this, 0);
            String ret = "";
            synchronized (lastChallenge) {
                if (job.getChallenge() instanceof ImageCaptchaChallenge) {
                    final Plugin plg = ((ImageCaptchaChallenge) job.getChallenge()).getPlugin();
                    if (plg != null) {
                        String id = plg.getHost();
                        lastChallenge.add(new Request(id));
                    }
                }
            }
            job.getChallenge().sendStatsSolving(this);
            job.setStatus(SolverStatus.UPLOADING);
            MyCaptchaChallenge ch = new MyCaptchaChallenge();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
            final Base64OutputStream b64os = new Base64OutputStream(bos);
            b64os.write(IO.readFile(challenge.getImageFile()));
            b64os.close();
            ch.setDataURL("data:image/" + Files.getExtension(challenge.getImageFile().getName()) + ";base64," + new String(bos.toByteArray(), "UTF-8"));
            ch.setSource(challenge.getPlugin().getHost());
            if (!StringUtils.equals(challenge.getTypeID(), ch.getSource())) {
                ch.setMethod(challenge.getTypeID());
            }
            ch.setType(TYPE.TEXT);
            MyCaptchaSolution id = MyJDownloaderController.getInstance().pushChallenge(ch);
            if (id == null) {
                throw new SolverException("Unknown Connection problems");
            }
            long startTime = System.currentTimeMillis();
            // Encoding.urlEncode(challenge.getTypeID())
            // ret = br.postPage(getAPIROOT() + "index.cgi", "&oldsource=" + Encoding.urlEncode(challenge.getTypeID()) + "&apikey=" +
            // Encoding.urlEncode(config.getApiKey()) + "&captchaSource=jdPlugin&maxtimeout=" + timeoutthing +
            // "&version=1.2&base64=1&file-upload-01=" + Encoding.urlEncode(org.appwork.utils.encoding.Base64.encodeToString(data, false)));
            job.setStatus(SolverStatus.SOLVING);
            try {
                Thread.sleep(3000);
                while (true) {
                    job.getLogger().info(this + "my.jdownloader.org Ask " + id);
                    MyCaptchaSolution solution = MyJDownloaderController.getInstance().getChallengeResponse(id.getId());
                    if (solution == null) {
                        throw new SolverException("Unknown Connection problems");
                    }
                    switch (solution.getState()) {
                    case NOT_AVAILABLE:
                        throw new SolverException("Not Available");
                    case PROCESSING:
                    case QUEUED:
                        job.getLogger().info(this + "my.jdownloader.org NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                        break;
                    case SOLVED:
                        job.getLogger().info(this + "my.jdownloader.org Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                        job.setAnswer(new CaptchaMyJDCESResponse(challenge, this, solution.getResponse(), 99, solution));
                        return;
                    }
                    checkInterruption();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                try {
                    MyJDownloaderController.getInstance().sendChallengeFeedback(id.getId(), MyCaptchaSolution.RESULT.ABORT);
                } catch (Throwable e1) {
                    logger.log(e1);
                }
                throw e;
            }
            // polling
        } catch (IOException e) {
            job.getChallenge().sendStatsError(this, e);
            job.getLogger().log(e);
            throw new SolverException(e);
        } catch (MyJDownloaderException e) {
            job.getChallenge().sendStatsError(this, e);
            throw new SolverException(e);
        }
    }

    @Override
    public boolean setValid(final AbstractResponse<?> response) {
        if (response instanceof CaptchaMyJDCESResponse) {
            try {
                MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.CORRECT);
                return true;
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        return false;
    }

    @Override
    public boolean setUnused(final AbstractResponse<?> response) {
        if (response instanceof CaptchaMyJDCESResponse) {
            try {
                MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.ABORT);
                return true;
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        return false;
    }

    @Override
    public boolean setInvalid(final AbstractResponse<?> response) {
        if (response instanceof CaptchaMyJDCESResponse) {
            try {
                MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.WRONG);
                return true;
            } catch (Throwable e) {
                logger.log(e);
            }
        }
        return false;
    }

    @Override
    protected boolean validateLogins() {
        return enabled && isMyJDownloaderAccountValid() && isEnabled();
    }

    boolean isMyJDownloaderAccountValid() {
        switch (MyJDownloaderController.getInstance().getConnectionStatus()) {
        case CONNECTED:
        case PENDING:
            return true;
        }
        return false;
    }

    public MyJDCESInfo loadInfo() {
        MyJDCESInfo ret = new MyJDCESInfo();
        ret.setConnected(isMyJDownloaderAccountValid());
        ret.setStatus(MyJDCESStatus.ENABLED);
        return ret;
    }
}
