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
                final int index = decoded.indexOf(":");
                if (index == -1 || index + 1 >= decoded.length()) {
                    throw new InvalidBasicAuthFormatException(basicAuthString);
                }
                setUsername(decoded.substring(0, index));
                setPassword(decoded.substring(index + 1));
            } else {
                String decoded = Encoding.Base64Decode(basicAuthString);
                final int index = decoded.indexOf(":");
                if (index == -1 || index + 1 >= decoded.length()) {
                    throw new InvalidBasicAuthFormatException(basicAuthString);
                }
                setUsername(decoded.substring(0, index));
                setPassword(decoded.substring(index + 1));
            }
        } catch (InvalidBasicAuthFormatException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidBasicAuthFormatException(e);
        }
    }

    public String toAuthString() {
        return BASIC + Encoding.Base64Encode(getUsername() + ":" + getPassword());
    }

}
