package jd.http.ext;

import jd.http.Request;

public interface AdBlockerInterface {

    boolean doBlockRequest(Request request);

    String prepareScript(String text, String source);

}
