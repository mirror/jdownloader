package org.jdownloader.captcha.v2.challenge.keycaptcha;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;

public class KeyCaptchaPuzzleChallenge extends Challenge<String> {

    private KeyCaptcha helper;
    private boolean    noAutoSolver;
    private Plugin     plugin;

    public KeyCaptcha getHelper() {
        return helper;
    }

    public boolean isNoAutoSolver() {
        return noAutoSolver;
    }

    public KeyCaptchaPuzzleChallenge(KeyCaptcha keyCaptcha, Plugin plg, boolean noAutoSolver) {
        super("keyCaptchaPuzzle", null);
        this.helper = keyCaptcha;
        this.plugin = plg;

        this.noAutoSolver = noAutoSolver;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Used to validate result against expected pattern. <br />
     * This is different to AbstractBrowserChallenge.isSolved, as we don't want to throw the same error exception.
     *
     * @param result
     * @return
     * @author raztoki
     */
    protected final boolean isCaptchaResponseValid() {
        if (isSolved()) {
            return true;
        }
        return false;
    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }
}
