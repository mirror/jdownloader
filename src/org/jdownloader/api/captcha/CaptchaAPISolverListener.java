package org.jdownloader.api.captcha;

import java.util.EventListener;

import org.jdownloader.captcha.v2.solverjob.SolverJob;

public interface CaptchaAPISolverListener extends EventListener {

    void onAPIJobDone(SolverJob<?> job);

    void onAPIJobStarted(SolverJob<Object> job);

}