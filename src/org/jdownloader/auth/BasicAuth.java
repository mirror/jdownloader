package org.jdownloader.auth;

import jd.nutils.encoding.Encoding;

public class BasicAuth extends Login {

    private static final String BASIC = "Basic ";

    public BasicAuth(String username, String password) {
        super(username, password);
    }

    public BasicAuth(String basicAuthString) throws InvalidBasicAuthFormatException {
        super();
        try {
            if (basicAuthString.startsWith(BASIC)) {
                String decoded = Encoding.Base64Decode(basicAuthString.substring(BASIC.length()));
                int index = decoded.indexOf(":");
                setUsername(decoded.substring(0, index));
                setPassword(decoded.substring(index + 1));
            } else {
                String decoded = Encoding.Base64Decode(basicAuthString);
                int index = decoded.indexOf(":");
                setUsername(decoded.substring(0, index));
                setPassword(decoded.substring(index + 1));
            }
        } catch (RuntimeException e) {
            throw new InvalidBasicAuthFormatException(e);
        }
    }

    public String toAuthString() {
        return BASIC + Encoding.Base64Encode(getUsername() + ":" + getPassword());
    }

}
