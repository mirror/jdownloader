package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

import jd.http.Browser;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;

public class Captcha9kwSolverPuzzle extends AbstractCaptcha9kwSolver<String> {
    private static Captcha9kwSolverPuzzle INSTANCE = new Captcha9kwSolverPuzzle();

    public static Captcha9kwSolverPuzzle getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    private Captcha9kwSolverPuzzle() {
        super();
        NineKwSolverService.getInstance().setPuzzleSolver(this);
    }

    @Override
    protected boolean isChallengeSupported(Challenge<?> c) {
        return c instanceof KeyCaptchaPuzzleChallenge;
    }

    @Override
    protected void solveCES(CESSolverJob<String> solverJob) throws InterruptedException, SolverException {
        checkInterruption();
        KeyCaptchaPuzzleChallenge captchaChallenge = (KeyCaptchaPuzzleChallenge) solverJob.getChallenge();
        RequestOptions options = prepare(solverJob);
        if (options.getMoreoptions().containsKey("userconfirm")) {
            options.getMoreoptions().remove("userconfirm");
        } else {
            options.setConfirm(config.isconfirm());
        }
        try {
            UrlQuery qi = createQueryForUpload(solverJob, options, null);
            qi.appendEncoded("puzzle", "1");
            KeyCaptchaImages images = captchaChallenge.getHelper().getPuzzleData().getImages();
            LinkedList<BufferedImage> piecesAll = new LinkedList<BufferedImage>(images.pieces);
            qi.appendEncoded("file-upload-01", Base64.encodeToString(IconIO.toJpgBytes(images.backgroundImage), false));
            qi.appendEncoded("file-upload-02", Base64.encodeToString(IconIO.toJpgBytes(images.sampleImage), false));
            String allfiledata = "";
            for (int c = 0; c < piecesAll.size(); c++) {
                BufferedImage image = piecesAll.get(c);
                int x = c + 3;
                qi.appendEncoded("file-upload-0" + x, Base64.encodeToString(IconIO.toJpgBytes(image), false));
            }
            UrlQuery queryPoll = createQueryForPolling().appendEncoded("puzzle", "1");
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
    public boolean isEnabled() {
        return config.ispuzzle() && config.isEnabledGlobally();
    }

    @Override
    protected void parseResponse(CESSolverJob<String> solverJob, Challenge<String> challenge, String captchaID, String ret) throws IOException {
        final String points;
        if (StringUtils.startsWithCaseInsensitive(ret, "OK-answered-")) {
            points = ret.substring("OK-answered-".length());
        } else {
            points = ret;
        }
        final String token = ((KeyCaptchaPuzzleChallenge) challenge).getHelper().sendPuzzleResult(null, points);
        counterSolved.incrementAndGet();
        solverJob.setAnswer(new Captcha9kwPuzzleResponse((challenge), this, token, 95, captchaID));
    }
}
