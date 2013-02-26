package org.jdownloader.captcha.event;

import org.appwork.utils.event.SimpleEvent;

public class ChallengeResponseEvent extends SimpleEvent<Object, Object, ChallengeResponseEvent.Type> {

    public static enum Type {
        NEW_JOB,
        JOB_DONE,
        SOLVER_END,
        JOB_ANSWER,
        SOLVER_START
    }

    public ChallengeResponseEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}