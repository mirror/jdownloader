package org.jdownloader.captcha.v2.solver.myjd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.components.premiumbar.ServiceCollection;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;
import jd.plugins.Plugin;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64OutputStream;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectionStatus;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.BlackOrWhitelist;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.SolverStatus;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge.TYPE;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CaptchaMyJDSolver extends CESChallengeSolver<String> implements ChallengeResponseValidation, MyJDownloaderListener {
    private final MyJDownloaderSettings    config;

    private final LogSource                logger;

    private final ArrayList<Request>       lastChallenge;

    private static final CaptchaMyJDSolver INSTANCE = new CaptchaMyJDSolver();

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

    private CaptchaMyJDSolver() {
        super(5);
        logger = LogController.getInstance().getLogger(CaptchaMyJDSolver.class.getName());
        config = JsonConfig.create(MyJDownloaderSettings.class);

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                if (!Application.isHeadless()) {
                    ServicePanel.getInstance().addExtender(CaptchaMyJDSolver.this);
                }

                MyJDownloaderController.getInstance().getEventSender().addListener(CaptchaMyJDSolver.this);

            }

        });
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().requestUpdate(true);
        }

        lastChallenge = new ArrayList<Request>();
    }

    private final boolean enabled = true;

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (validateLogins() && c instanceof BasicCaptchaChallenge && MyJDownloaderController.getInstance().isChallengeExchangeEnabled() && CFG_CAPTCHA.CAPTCHA_EXCHANGE_SERVICES_ENABLED.isEnabled() && super.canHandle(c)) {

            Plugin plg = ((BasicCaptchaChallenge) c).getPlugin();
            if (plg != null) {
                final String id = plg.getHost();
                int counter = 0;
                synchronized (lastChallenge) {

                    final ArrayList<Request> remove = new ArrayList<Request>();
                    for (int i = lastChallenge.size() - 1; i >= 0; i--) {
                        final Request r = lastChallenge.get(i);
                        if (System.currentTimeMillis() > r.timestamp + 30 * 60 * 1000l) {
                            remove.add(r);
                            continue;
                        }
                        if (r.id.equals(id)) {
                            counter++;
                        }
                    }
                    lastChallenge.removeAll(remove);
                }
                // max 2 captchas per plugin and 30 minutes.
                if (counter >= 10) {
                    return false;
                }

            }
            return true;
        }
        return false;
    }

    @Override
    public Icon getIcon(int size) {
        return new AbstractIcon(IconKey.ICON_MYJDOWNLOADER, size);
    }

    @Override
    protected void solveCES(CESSolverJob<String> job) throws InterruptedException, SolverException {
        BasicCaptchaChallenge challenge = (BasicCaptchaChallenge) job.getChallenge();
        job.getLogger().info(this.getName() + ": Start. GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());

        // int timeoutthing = (JsonConfig.create(CaptchaSettings.class).getCaptchaDialogMyJDCESTimeout() / 1000);

        BlackOrWhitelist type = config.getCESBlackOrWhitelistType();
        if (type != null) {

            switch (type) {
            case BLACKLIST:
                String[] blacklist = config.getCESBlacklist();
                if (blacklist != null) {
                    for (String s : blacklist) {
                        if (s.equalsIgnoreCase(challenge.getTypeID())) {
                            job.getLogger().info(this.getName() + ": Did not solve because of blacklist entry");
                            return;
                        }
                    }
                }
                break;
            case WHITELIST:
                String[] whitelist = config.getCESWhitelist();
                boolean allowed = false;
                if (whitelist != null) {
                    for (String s : whitelist) {
                        if (s.equalsIgnoreCase(challenge.getTypeID())) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (!allowed) {
                    job.getLogger().info(this.getName() + ": Did not solve because of missing whitlist entry");
                    return;
                }
                break;
            default:

            }
        }

        job.getLogger().info(this.getName() + ": Upload Captcha. GetTypeID: " + challenge.getTypeID() + " - Plugin: " + challenge.getPlugin());
        try {

            // job.showBubble(this);
            checkInterruption();

            byte[] data = IO.readFile(challenge.getImageFile());
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
            job.setStatus(SolverStatus.UPLOADING);
            MyCaptchaChallenge ch = new MyCaptchaChallenge();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
            final Base64OutputStream b64os = new Base64OutputStream(bos);
            b64os.write(IO.readFile(challenge.getImageFile()));
            b64os.close();

            ch.setDataURL("data:image/" + Files.getExtension(challenge.getImageFile().getName()) + ";base64," + new String(bos.toByteArray(), "UTF-8"));
            ch.setSource(challenge.getTypeID());
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
                    job.getLogger().info(this.getName() + "my.jdownloader.org Ask " + id);
                    MyCaptchaSolution solution = MyJDownloaderController.getInstance().getChallengeResponse(id.getId());
                    if (solution == null) {
                        throw new SolverException("Unknown Connection problems");
                    }
                    switch (solution.getState()) {
                    case NOT_AVAILABLE:
                        throw new SolverException("Not Available");
                    case PROCESSING:
                    case QUEUED:
                        job.getLogger().info(this.getName() + "my.jdownloader.org NO answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s ");
                        break;
                    case SOLVED:
                        job.getLogger().info(this.getName() + "my.jdownloader.org Answer after " + ((System.currentTimeMillis() - startTime) / 1000) + "s: " + ret);
                        job.setAnswer(new CaptchaMyJDCESResponse(challenge, this, solution.getResponse(), 100, solution));
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
            job.getLogger().log(e);
            throw new SolverException(e);
        } catch (MyJDownloaderException e) {
            throw new SolverException(e);
        }

    }

    @Override
    public void setValid(final AbstractResponse<?> response, SolverJob<?> job) {
        if (response instanceof CaptchaMyJDCESResponse) {
            try {
                MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.CORRECT);
            } catch (Throwable e) {
                logger.log(e);
            }
        }

    }

    @Override
    public void setUnused(final AbstractResponse<?> response, SolverJob<?> job) {
        try {
            MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.ABORT);
        } catch (Throwable e) {
            logger.log(e);
        }
    }

    @Override
    public void setInvalid(final AbstractResponse<?> response, SolverJob<?> job) {
        try {
            MyJDownloaderController.getInstance().sendChallengeFeedback(((CaptchaMyJDCESResponse) response).getSolution().getId(), MyCaptchaSolution.RESULT.WRONG);
        } catch (Throwable e) {
            logger.log(e);
        }
    }

    @Override
    public String getName() {
        return "MY.JDownloader Captcha Solver";
    }

    @Override
    public void extendServicePabel(List<ServiceCollection<?>> services) {
        if (isMyJDownloaderAccountValid() && enabled) {

            services.add(new ServiceCollection<CaptchaMyJDSolver>() {

                /**
                 * 
                 */
                private static final long serialVersionUID = 5569965026755271172L;

                @Override
                public Icon getIcon() {
                    return new AbstractIcon(IconKey.ICON_MYJDOWNLOADER, 18);
                }

                @Override
                public boolean isEnabled() {
                    return config.isCESEnabled();
                }

                @Override
                protected long getLastActiveTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                protected String getName() {
                    return CaptchaMyJDSolver.this.getName();
                }

                @Override
                public ExtTooltip createTooltip(ServicePanel owner) {
                    return new ServicePanelMyJDCESTooltip(owner, CaptchaMyJDSolver.this);
                }

            });
        }
    }

    @Override
    protected boolean validateLogins() {
        return enabled && isMyJDownloaderAccountValid() && config.isCESEnabled();
    }

    private boolean isMyJDownloaderAccountValid() {
        switch (MyJDownloaderController.getInstance().getConnectionStatus()) {
        case CONNECTED:
        case PENDING:
            return true;
        }
        return false;
    }

    @Override
    public void onMyJDownloaderConnectionStatusChanged(MyJDownloaderConnectionStatus status, int connections) {
        if (!Application.isHeadless()) {
            ServicePanel.getInstance().requestUpdate(true);
        }
    }

    public MyJDCESInfo loadInfo() {
        MyJDCESInfo ret = new MyJDCESInfo();
        ret.setConnected(isMyJDownloaderAccountValid());
        ret.setStatus(MyJDCESStatus.ENABLED);
        return ret;
    }
}
