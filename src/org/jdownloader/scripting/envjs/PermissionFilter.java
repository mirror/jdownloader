package org.jdownloader.scripting.envjs;

import jd.http.Request;

public interface PermissionFilter {

    String onBeforeExecutingInlineJavaScript(String type, String js);

    Request onBeforeXHRRequest(Request request);

    Request onBeforeLoadingExternalJavaScript(String type, String src, Request request);

    void onAfterXHRRequest(Request request, XHRResponse ret);

    String onAfterLoadingExternalJavaScript(String type, String src, String sourceCode, Request request);

}
