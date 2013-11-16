/*
 * Source: http://deathbycaptcha.eu/user/api
 * Slightly modified to work without json and base64 dependencies
 */
package org.jdownloader.captcha.v2.solver.dbc.api;

public class InvalidCaptchaException extends Exception {
    public InvalidCaptchaException(String message) {
        super(message);
    }
}
