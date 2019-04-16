package org.jdownloader.captcha.v2;

import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.controlling.UniqueAlltimeID;

public class AbstractResponse<T> {
    private final UniqueAlltimeID id         = new UniqueAlltimeID();
    private int                   priority;
    private ValidationResult      validation = null;                  ;

    public ValidationResult getValidation() {
        return validation;
    }

    public boolean setValidation(final ValidationResult validation) {
        try {
            // only validate once
            if (this.validation != null) {
                return false;
            } else {
                this.validation = validation;
                final Object solver = getSolver();
                if (solver != null && solver instanceof ChallengeSolver) {
                    final Challenge<T> c = getChallenge();
                    switch (validation) {
                    case INVALID:
                        if (c != null) {
                            c.sendStatsValidation(((ChallengeSolver) solver), "false");
                        }
                        return ((ChallengeSolver<?>) solver).setInvalid(this);
                    case UNUSED:
                        if (c != null) {
                            c.sendStatsValidation(((ChallengeSolver) solver), "unused");
                        }
                        return ((ChallengeSolver<?>) solver).setUnused(this);
                    case VALID:
                        if (c != null) {
                            c.sendStatsValidation(((ChallengeSolver) solver), "true");
                        }
                        return ((ChallengeSolver<?>) solver).setValid(this);
                    }
                }
                return true;
            }
        } catch (Throwable e) {
            this.validation = null;
            LoggerFactory.getDefaultLogger().log(e);
            return false;
        }
    }

    public int getPriority() {
        return priority;
    }

    public UniqueAlltimeID getId() {
        return id;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    private T                  value;
    private final Object       solver;
    private final Challenge<T> challenge;

    public Object getSolver() {
        return solver;
    }

    public AbstractResponse(Challenge<T> challenge, Object solver, int priority, T responseData) {
        this.solver = solver;
        this.priority = priority;
        this.value = responseData;
        this.challenge = challenge;
    }

    public String toString() {
        return getClass().getSimpleName() + ": Value:" + value + " Priority: " + priority + " Solved By: " + solver;
    }

    public Challenge<T> getChallenge() {
        return challenge;
    }
}
