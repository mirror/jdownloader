package org.jdownloader.captcha.v2.solver.twocaptcha;

import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.logging.LogController;

public abstract class AbstractTwoCaptchaSolver<T> extends CESChallengeSolver<T> {
    private String                            accountStatusString;
    protected final TwoCaptchaConfigInterface config;
    AtomicInteger                             counter            = new AtomicInteger();
    AtomicInteger                             counterInterrupted = new AtomicInteger();
    AtomicInteger                             counterNotOK       = new AtomicInteger();
    AtomicInteger                             counterOK          = new AtomicInteger();
    AtomicInteger                             counterSend        = new AtomicInteger();
    AtomicInteger                             counterSendError   = new AtomicInteger();
    AtomicInteger                             counterSolved      = new AtomicInteger();
    AtomicInteger                             counterUnused      = new AtomicInteger();
    private String                            long_debuglog      = "";
    protected final LogSource                 logger;

    public AbstractTwoCaptchaSolver() {
        super(new TwoCaptchaSolverService(), Math.max(1, Math.min(25, JsonConfig.create(TwoCaptchaConfigInterface.class).getThreadpoolSize())));
        config = JsonConfig.create(TwoCaptchaConfigInterface.class);
        logger = LogController.getInstance().getLogger(TwoCaptchaSolver.class.getName());
        threadPool.allowCoreThreadTimeOut(true);
    }

    public synchronized void dellong_debuglog() {
        this.long_debuglog = "";
    }

    @Override
    public String getAccountStatusString() {
        return accountStatusString;
    }

    public synchronized String getlong_debuglog() {
        return this.long_debuglog;
    }

    @Override
    public SolverService getService() {
        return super.getService();
    }

    public synchronized void setlong_debuglog(String long_debuglog) {
        this.long_debuglog += long_debuglog + "\n";
    }

    protected void showMessageAndQuit(String title, String msg) throws SolverException {
        MessageDialogImpl d = new MessageDialogImpl(0, title, msg, null, null);
        UIOManager.I().show(MessageDialogInterface.class, d);
        throw new SolverException(title);
    }

    protected UrlQuery createQueryForPolling() {
        UrlQuery queryPoll = new UrlQuery();
        queryPoll.appendEncoded("key", config.getApiKey() + "");
        queryPoll.appendEncoded("action", "get");
        return queryPoll;
    }

    protected UrlQuery createQueryForUpload(CESSolverJob<T> job, RequestOptions options, final byte[] data) throws SolverException {
        UrlQuery qi = new UrlQuery();
        qi.appendEncoded("key", config.getApiKey() + "");
        qi.appendEncoded("method", "base64");
        qi.appendEncoded("soft_id", "2969704");
        qi.appendEncoded("json", "1");
        if (data != null) {
            qi.appendEncoded("body", org.appwork.utils.encoding.Base64.encodeToString(data, false));
        }
        return qi;
    }

    protected RequestOptions prepare(CESSolverJob<T> solverJob) throws SolverException, InterruptedException {
        RequestOptions options = new RequestOptions();
        validateApiKey(solverJob);
        return options;
    }

    protected void validateApiKey(CESSolverJob<T> job) throws SolverException {
        if (!config.getApiKey().matches("^[a-f0-9]+$")) {
            showMessageAndQuit("2Captcha.com Error", "API Key is not correct!" + "\n" + "Only a-f and 0-9");
        }
    }

    @Override
    protected void solveBasicCaptchaChallenge(CESSolverJob<T> job, BasicCaptchaChallenge challenge) throws SolverException {
        // not used. solveCEs is overwritten
    }
}
