package org.jdownloader.captcha.v2.solver.gui;

import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;

import jd.controlling.captcha.BasicCaptchaDialogHandler;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.CBSolver;
import org.jdownloader.captcha.v2.solver.Captcha9kwSettings;
import org.jdownloader.captcha.v2.solver.Captcha9kwSolver;
import org.jdownloader.captcha.v2.solver.CaptchaBrotherHoodSettings;
import org.jdownloader.captcha.v2.solver.CaptchaResolutorCaptchaSettings;
import org.jdownloader.captcha.v2.solver.CaptchaResolutorCaptchaSolver;
import org.jdownloader.captcha.v2.solver.jac.JACSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.SoundSettings;

public class DialogBasicCaptchaSolver extends ChallengeSolver<String> {
    private CaptchaSettings                       config;
    private Captcha9kwSettings                    config9kw;
    private CaptchaBrotherHoodSettings            configcbh;
    private CaptchaResolutorCaptchaSettings       configresolutor;
    private static final DialogBasicCaptchaSolver INSTANCE = new DialogBasicCaptchaSolver();

    public static DialogBasicCaptchaSolver getInstance() {
        return INSTANCE;
    }

    @Override
    public Class<String> getResultType() {
        return String.class;
    }

    public void enqueue(SolverJob<String> job) {
        if (job.getChallenge() instanceof BasicCaptchaChallenge) {
            super.enqueue(job);
            captchaSound();
        }
    }

    public static void captchaSound() {
        final URL soundUrl = NewTheme.I().getURL("sounds/", "captcha", ".wav");

        if (soundUrl != null && JsonConfig.create(SoundSettings.class).isCaptchaSoundEnabled()) {
            new Thread("Captcha Sound") {
                public void run() {
                    AudioInputStream stream = null;
                    try {

                        AudioFormat format;
                        DataLine.Info info;

                        stream = AudioSystem.getAudioInputStream(soundUrl);
                        format = stream.getFormat();
                        info = new DataLine.Info(Clip.class, format);
                        final Clip clip = (Clip) AudioSystem.getLine(info);
                        clip.open(stream);
                        try {
                            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                            float db = (20f * (float) Math.log(JsonConfig.create(SoundSettings.class).getCaptchaSoundVolume() / 100f));

                            gainControl.setValue(Math.max(-80f, db));
                            BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                            muteControl.setValue(true);

                            muteControl.setValue(false);
                        } catch (Exception e) {
                            Log.exception(e);
                        }
                        clip.start();
                        clip.addLineListener(new LineListener() {

                            @Override
                            public void update(LineEvent event) {
                                if (event.getType() == Type.STOP) {
                                    clip.close();
                                }
                            }
                        });
                        while (clip.isRunning()) {
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        Log.exception(e);
                    } finally {
                        try {
                            stream.close();
                        } catch (Throwable e) {

                        }
                        // try {
                        // clip.close();
                        // } catch (Throwable e) {
                        //
                        // }
                    }
                }
            }.start();
        }
    }

    private DialogBasicCaptchaSolver() {
        super(1);
        config = JsonConfig.create(CaptchaSettings.class);
        config9kw = JsonConfig.create(Captcha9kwSettings.class);
        configcbh = JsonConfig.create(CaptchaBrotherHoodSettings.class);
        configresolutor = JsonConfig.create(CaptchaResolutorCaptchaSettings.class);
    }

    @Override
    public void solve(final SolverJob<String> job) throws InterruptedException, SkipException {
        synchronized (this) {

            if (job.getChallenge() instanceof BasicCaptchaChallenge) {
                job.getLogger().info("Waiting for JAC");
                job.waitFor(config.getCaptchaDialogJAntiCaptchaTimeout(), JACSolver.getInstance());

                if (config9kw.isEnabled() && config.getCaptchaDialog9kwTimeout() > 0) job.waitFor(config.getCaptchaDialog9kwTimeout(), Captcha9kwSolver.getInstance());
                if (configcbh.isEnabled() && config.getCaptchaDialogCaptchaBroptherhoodTimeout() > 0) job.waitFor(config.getCaptchaDialogCaptchaBroptherhoodTimeout(), CBSolver.getInstance());
                if (configresolutor.isEnabled() && config.getCaptchaDialogResolutorCaptchaTimeout() > 0) job.waitFor(config.getCaptchaDialogResolutorCaptchaTimeout(), CaptchaResolutorCaptchaSolver.getInstance());

                job.getLogger().info("JAC is done. Response so far: " + job.getResponse());
                ChallengeSolverJobListener jacListener = null;
                checkInterruption();
                BasicCaptchaChallenge captchaChallenge = (BasicCaptchaChallenge) job.getChallenge();
                // we do not need another queue
                final BasicCaptchaDialogHandler handler = new BasicCaptchaDialogHandler(captchaChallenge);
                job.getEventSender().addListener(jacListener = new ChallengeSolverJobListener() {

                    @Override
                    public void onSolverTimedOut(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverStarts(ChallengeSolver<?> parameter) {
                    }

                    @Override
                    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
                        ResponseList<String> resp = job.getResponse();
                        handler.setSuggest(resp.getValue());
                        job.getLogger().info("Received Suggestion: " + resp);

                    }

                    @Override
                    public void onSolverDone(ChallengeSolver<?> solver) {

                    }
                });
                try {
                    ResponseList<String> resp = job.getResponse();
                    if (resp != null) {
                        handler.setSuggest(resp.getValue());
                    }
                    checkInterruption();
                    if (!captchaChallenge.getImageFile().exists()) {

                        job.getLogger().info("Cannot solve. image does not exist");
                        return;
                    }

                    handler.run();

                    if (StringUtils.isNotEmpty(handler.getCaptchaCode())) {
                        job.addAnswer(new CaptchaResponse(captchaChallenge, this, handler.getCaptchaCode(), 100));
                    }
                } finally {
                    job.getLogger().info("Dialog closed. Response far: " + job.getResponse());
                    if (jacListener != null) job.getEventSender().removeListener(jacListener);
                }
            }
        }

    }

}
