/*
 * Source: http://deathbycaptcha.eu/user/api
 * Slightly modified to work without json and base64 dependencies
 */
package org.jdownloader.captcha.v2.solver.dbc.api;

/**
 * CAPTCHA details.
 * 
 */
public class Captcha {
    public int        id      = 0;
    public String     text    = "";

    protected boolean correct = false;

    public Captcha() {
    }

    public Captcha(DataObject src) {
        this();
        this.id = Math.max(0, src.optInt("captcha", 0));
        if (0 < this.id) {
            this.correct = src.optBoolean("is_correct", true);
            Object o = src.get("text");
            if (null != o && null != o) {
                this.text = o.toString();
            }
        }
    }

    public boolean isUploaded() {
        return 0 < this.id;
    }

    public boolean isSolved() {
        return !this.text.equals("");
    }

    public boolean isCorrect() {
        return this.isSolved() && this.correct;
    }

    public int toInt() {
        return this.id;
    }

    public String toString() {
        return this.text;
    }

    public boolean toBoolean() {
        return this.isCorrect();
    }
}
