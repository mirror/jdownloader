/*
 * Source: http://deathbycaptcha.eu/user/api
 * Slightly modified to work without json and base64 dependencies
 */
package org.jdownloader.captcha.v2.solver.dbc;

public class ServiceOverloadException extends Exception {
    public ServiceOverloadException(String message) {
        super(message);
    }
}
