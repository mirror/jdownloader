package org.jdownloader.captcha.v2.solver.browser;

public class NoBlockException extends Exception {

    private int x;
    private int y;

    public NoBlockException(int x, int y) {
        this.x = x;
        this.y = y;
    }

}
