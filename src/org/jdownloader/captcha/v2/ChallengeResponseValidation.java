package org.jdownloader.captcha.v2;

public interface ChallengeResponseValidation {

    public void setValid(AbstractResponse<?> response);

    public void setUnused(AbstractResponse<?> response);

    public void setInvalid(AbstractResponse<?> response);
}
