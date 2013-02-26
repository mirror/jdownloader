package org.jdownloader.captcha.v2.solverjob;

import org.appwork.utils.event.SimpleEvent;

public class ChallengeSolverJobEvent extends SimpleEvent<Object, Object, ChallengeSolverJobEvent.Type> {

    public static enum Type {
        SOLVER_DONE,
        SOLVER_START,
        SOLVER_TIMEOUT,
        NEW_ANSWER
    }

    public ChallengeSolverJobEvent(Object caller, Type type, Object... parameters) {
        super(caller, type, parameters);
    }
}