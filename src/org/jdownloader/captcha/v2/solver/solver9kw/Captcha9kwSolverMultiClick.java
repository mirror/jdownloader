package org.jdownloader.captcha.v2.solver.solver9kw;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.captcha.v2.solver.jac.SolverException;

public class Captcha9kwSolverMultiClick extends AbstractCaptcha9kwSolver<MultiClickedPoint> {
    private static final Captcha9kwSolverMultiClick INSTANCE        = new Captcha9kwSolverMultiClick();
    private double                                  base_workaround = 2;

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
            byte[] data = null;
            final String ext = Files.getExtension(captchaChallenge.getImageFile().getName());
            if (StringUtils.equalsIgnoreCase("kissanime.to", solverJob.getChallenge().getTypeID())) {
                // why not add generic handling? check image size and resire if required?
                // too big for 9kw.eu
                // Width+Height=max.800px, width under 620px, height under 600px
                if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("gif")) {
                    BufferedImage image = ImageProvider.read(captchaChallenge.getImageFile());
                    double size = image.getWidth() + image.getHeight();
                    int basevalue = 800;
                    double toobig_size;
                    double round_width = 0;
                    double round_height = 0;
                    for (int i = 0; i < 100; i++) {
                        toobig_size = size / (basevalue - i);
                        this.base_workaround = toobig_size;
                        round_width = image.getWidth() / toobig_size;
                        round_height = image.getHeight() / toobig_size;
                        if (round_width < 620 && round_height < 600 && (round_height + round_width) < 800) {
                            break;
                        }
                    }
                    image = (BufferedImage) ImageProvider.scaleBufferedImage(image, (int) Math.round(round_width), (int) Math.round(round_height));
                    data = toByteArrayCaptcha(image, ext);
                } else {
                    data = IO.readFile(captchaChallenge.getImageFile());
                }
            } else {
                data = IO.readFile(captchaChallenge.getImageFile());
            }
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
        }
    }

    protected static byte[] toByteArrayCaptcha(BufferedImage image, String type) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, type, out);
        return out.toByteArray();
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
            String[] splitResultx = splitResult[i].split("x");
            if (!jsonX.isEmpty()) {
                jsonX += ",";
            }
            if (solverJob.getChallenge().getTypeID() == "kissanime.to") {
                jsonX += "" + (int) Math.round(Integer.parseInt(splitResultx[0]) * this.base_workaround);
            } else {
                jsonX += splitResultx[0];
            }
            if (!jsonY.isEmpty()) {
                jsonY += ",";
            }
            if (solverJob.getChallenge().getTypeID() == "kissanime.to") {
                jsonX += "" + (int) Math.round((Integer.parseInt(splitResultx[1]) * this.base_workaround));
            } else {
                jsonY += splitResultx[1];
            }
        }
        String jsonInString = "{\"x\":[" + "],\"y\":[" + "]}";
        MultiClickedPoint res = JSonStorage.restoreFromString(jsonInString, new TypeRef<MultiClickedPoint>() {
        });
        solverJob.setAnswer(new Captcha9kwMultiClickResponse(captchaChallenge, this, res, 100, captchaID));
    }
}
