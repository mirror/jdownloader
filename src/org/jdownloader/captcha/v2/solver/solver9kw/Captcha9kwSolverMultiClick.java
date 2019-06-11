package org.jdownloader.captcha.v2.solver.solver9kw;

import java.io.IOException;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;

public class Captcha9kwSolverMultiClick extends AbstractCaptcha9kwSolver<MultiClickedPoint> {
    private static final Captcha9kwSolverMultiClick INSTANCE = new Captcha9kwSolverMultiClick();

    public static Captcha9kwSolverMultiClick getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<MultiClickedPoint> getResultType() {
        return MultiClickedPoint.class;
    }

    private Captcha9kwSolverMultiClick() {
        super();
        NineKwSolverService.getInstance().setMultiClickSolver(this);
    }

    @Override
    public boolean isEnabled() {
        return config.ismouse() && config.isEnabledGlobally();
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof MultiClickCaptchaChallenge;
    }

    @Override
    protected void solveCES(CESSolverJob<MultiClickedPoint> solverJob) throws InterruptedException, SolverException {
        checkInterruption();
        MultiClickCaptchaChallenge captchaChallenge = (MultiClickCaptchaChallenge) solverJob.getChallenge();
        RequestOptions options = prepare(solverJob);
        if (options.getMoreoptions().containsKey("userconfirm")) {
            options.getMoreoptions().remove("userconfirm");
        } else {
            options.setConfirm(config.ismouseconfirm());
        }
        try {
            final byte[] data = IO.readFile(captchaChallenge.getImageFile());
            UrlQuery qi = createQueryForUpload(solverJob, options, data).appendEncoded("multimouse", "1").appendEncoded("textinstructions", captchaChallenge.getExplain());
            UrlQuery queryPoll = createQueryForPolling().appendEncoded("multimouse", "1");
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
        }
    }

    @Override
    protected boolean validateLogins() {
        return StringUtils.isNotEmpty(config.getApiKey()) && config.ismouse();
    }

    @Override
    protected void parseResponse(CESSolverJob<MultiClickedPoint> solverJob, Challenge<MultiClickedPoint> captchaChallenge, String captchaID, String antwort) {
        String jsonX = "";
        String jsonY = "";
        String[] splitResult = antwort.split(";");// Example: 68x149;81x192
        for (int i = 0; i < splitResult.length; i++) {
            String[] splitResultx = antwort.split("x");
            if (!jsonX.isEmpty()) {
                jsonX += ",";
            }
            jsonX += splitResultx[0];
            if (!jsonY.isEmpty()) {
                jsonY += ",";
            }
            jsonY += splitResultx[1];
        }
        String jsonInString = "{\"x\":[" + "],\"y\":[" + "]}";
        MultiClickedPoint res = JSonStorage.restoreFromString(jsonInString, new TypeRef<MultiClickedPoint>() {
        });
        solverJob.setAnswer(new Captcha9kwMultiClickResponse(captchaChallenge, this, res, 100, captchaID));
    }
}
