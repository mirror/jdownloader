package org.jdownloader.extensions.jdanywhere;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIException;

public class CheckUser {

    String user;
    String pass;

    public boolean check(String username, String password) {
        if (username.equals(user) && password.equals(pass))
            return true;
        else {
            throw new RemoteAPIException(ResponseCode.ERROR_UNAUTHORIZED, "Username or Password missing or wrong");
        }
    }

    public CheckUser(String user, String pass) {

        this.user = user;
        this.pass = pass;
    }
}
