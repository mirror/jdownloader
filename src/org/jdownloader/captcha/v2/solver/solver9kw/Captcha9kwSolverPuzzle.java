package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import jd.http.Browser;

import org.appwork.utils.encoding.Base64;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaResponse;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;

public class Captcha9kwSolverPuzzle extends AbstractCaptcha9kwSolver<String> {

    private LinkedList<Integer>           mouseArray = new LinkedList<Integer>();
    private static Captcha9kwSolverPuzzle INSTANCE   = new Captcha9kwSolverPuzzle();

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
    public boolean canHandle(Challenge<?> c) {
        return c instanceof KeyCaptchaPuzzleChallenge && super.canHandle(c);
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

            KeyCaptchaImages images = captchaChallenge.getHelper().getPuzzleData().getImages();
            LinkedList<BufferedImage> piecesAll = new LinkedList<BufferedImage>(images.pieces);

            String allfiledata = "";
            for (int c = 0; c < piecesAll.size(); c++) {
                BufferedImage image = piecesAll.get(c);
                int x = c + 1;
                qi.appendEncoded("file-upload-0" + x, Base64.encodeToString(IconIO.toJpgBytes(image), false));

            }

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

        }

    }

    @Override
    public boolean isEnabled() {
        return config.ispuzzle() && config.isEnabledGlobally();
    }

    @Override
    protected void parseResponse(CESSolverJob<String> solverJob, Challenge<String> challenge, String captchaID, String ret) throws IOException {
        // Special answer (x + 465? y + 264?)
        // Example: 622.289.683.351.705.331.734.351.713.264.734.281.488.275.784.281 (4 coordinates like x1,y1 to x2,y2)
        mouseArray.clear();

        boolean changemousexy9kw = true;
        ArrayList<Integer> marray = new ArrayList<Integer>();
        marray.addAll(mouseArray);
        for (String s : ret.substring("OK-answered-".length()).split("\\|")) {
            if (changemousexy9kw == true) {
                mouseArray.add(Integer.parseInt(s));// x+465?
                changemousexy9kw = false;
            } else {
                mouseArray.add(Integer.parseInt(s));// y+264?
                changemousexy9kw = true;
            }
        }
        mouseArray.clear();

        String token;
        token = ((KeyCaptchaPuzzleChallenge) challenge).getHelper().sendPuzzleResult(marray, ret.substring("OK-answered-".length()));

        counterSolved.incrementAndGet();
        solverJob.setAnswer(new KeyCaptchaResponse((challenge), this, token, 95));
    }

}
