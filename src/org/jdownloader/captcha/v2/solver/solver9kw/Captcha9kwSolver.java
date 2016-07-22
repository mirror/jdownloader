package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;

import jd.http.Browser;

import org.appwork.utils.IO;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs.Recaptcha2FallbackChallengeViaPhantomJS;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.plugins.SkipReason;

public class Captcha9kwSolver extends AbstractCaptcha9kwSolver<String> {

    private static final Captcha9kwSolver INSTANCE = new Captcha9kwSolver();

    public static Captcha9kwSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolver() {
        super();

        NineKwSolverService.getInstance().setTextSolver(this);

    }

    @Override
    public boolean canHandle(Challenge<?> c) {
        if (c instanceof RecaptchaV2Challenge || c instanceof AbstractRecaptcha2FallbackChallenge) {
            try {
                checkForEnoughCredits();
            } catch (SolverException e) {
                return false;
            }
            return true;
        }

        if (c instanceof BasicCaptchaChallenge && super.canHandle(c)) {
            try {
                checkForEnoughCredits();
            } catch (SolverException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void solveCES(CESSolverJob<String> solverJob) throws InterruptedException, SolverException {
        Challenge<String> captchaChallenge = getChallenge(solverJob);
        if (captchaChallenge instanceof RecaptchaV2Challenge) {
            if (((RecaptchaV2Challenge) captchaChallenge).createBasicCaptchaChallenge() == null) {
                throw new SolverException(SkipReason.PHANTOM_JS_MISSING.getExplanation(null));
            }
        }
        checkInterruption();
        RequestOptions options = prepare(solverJob);
        if (options.getMoreoptions().containsKey("userconfirm")) {
            options.getMoreoptions().remove("userconfirm");
        } else {
            options.setConfirm(config.isconfirm());
        }
        try {
            final byte[] data = captchaChallenge instanceof Recaptcha2FallbackChallengeViaPhantomJS ? ((Recaptcha2FallbackChallengeViaPhantomJS) captchaChallenge).getAnnotatedImageBytes() : IO.readFile(((ImageCaptchaChallenge) captchaChallenge).getImageFile());
            UrlQuery qi = createQueryForUpload(solverJob, options, data);
            UrlQuery queryPoll = createQueryForPolling();

            Browser br = new Browser();
            br.setAllowedResponseCodes(new int[] { 500 });
            String captchaID = upload(br, solverJob, qi);

            poll(br, options, solverJob, captchaID, queryPoll);

        } catch (IOException e) {
            solverJob.getChallenge().sendStatsError(this, e);
            setdebug(solverJob, "Interrupted: " + e);
            counterInterrupted.incrementAndGet();
            solverJob.getLogger().log(e);
        } finally {
            System.out.println(1);
        }

    }

    @Override
    protected void parseResponse(CESSolverJob<String> solverJob, Challenge<String> captchaChallenge, String captchaID, String ret) {
        final AbstractResponse<String> answer = captchaChallenge.parseAPIAnswer(ret, this);
        solverJob.setAnswer(new Captcha9kwResponse(captchaChallenge, this, answer.getValue(), answer.getPriority(), captchaID));
    }

}
