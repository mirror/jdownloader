package org.jdownloader.captcha.v2;

import org.jdownloader.captcha.v2.solverjob.SolverJob;

public interface ChallengeResponseValidation {

    public void setValid(AbstractResponse<?> response, SolverJob<?> job);

    public void setUnused(AbstractResponse<?> response, SolverJob<?> job);

    public void setInvalid(AbstractResponse<?> response, SolverJob<?> job);
}
