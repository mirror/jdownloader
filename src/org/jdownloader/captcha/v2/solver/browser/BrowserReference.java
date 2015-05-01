package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.captcha.v2.solver.service.BrowserSolverService;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class BrowserReference implements HttpRequestHandler {

    private HttpHandlerInfo          handlerInfo;
    private AbstractBrowserChallenge challenge;
    private UniqueAlltimeID          id;
    private int                      port;

    private Process                  process;
    private double                   scale;
    private BrowserWindow            browserWindow;
    private BrowserViewport          viewport;
    private HashMap<String, URL>     resourceIds;
    private HashMap<String, String>  types;
    {
        resourceIds = new HashMap<String, URL>();
        resourceIds.put("style.css", BrowserReference.class.getResource("html/style.css"));
        resourceIds.put("plax-1.png", BrowserReference.class.getResource("html/plax-1.png"));
        resourceIds.put("plax-2.png", BrowserReference.class.getResource("html/plax-2.png"));
        resourceIds.put("plax-3.png", BrowserReference.class.getResource("html/plax-3.png"));
        resourceIds.put("plax-4.png", BrowserReference.class.getResource("html/plax-4.png"));
        resourceIds.put("plax-5.png", BrowserReference.class.getResource("html/plax-5.png"));
        resourceIds.put("plax-6.png", BrowserReference.class.getResource("html/plax-6.png"));
        resourceIds.put("script.min.js", BrowserReference.class.getResource("html/script.min.js"));
        resourceIds.put("teaser.png", BrowserReference.class.getResource("html/teaser.png"));
        resourceIds.put("body-bg.jpg", BrowserReference.class.getResource("html/body-bg.jpg"));
        resourceIds.put("header-bg.jpg", BrowserReference.class.getResource("html/header-bg.jpg"));
        resourceIds.put("logo.png", BrowserReference.class.getResource("html/logo.png"));
        resourceIds.put("mediumblue-bg.jpg", BrowserReference.class.getResource("html/mediumblue-bg.jpg"));
        resourceIds.put("social.png", BrowserReference.class.getResource("html/social.png"));
        resourceIds.put("twitterbird.png", BrowserReference.class.getResource("html/twitterbird.png"));
        resourceIds.put("fuuuu.png", BrowserReference.class.getResource("html/fuuuu.png"));
        resourceIds.put("favicon.ico", BrowserReference.class.getResource("html/favicon.ico"));
        resourceIds.put("browserCaptcha.js", BrowserReference.class.getResource("html/browserCaptcha.js"));
        resourceIds.put("jquery-1.9.1-min.js", BrowserReference.class.getResource("html/jquery-1.9.1-min.js"));

        types = new HashMap<String, String>();
        types.put("html", "text/html; charset=utf-8");
        types.put("css", "text/css; charset=utf-8");
        types.put("png", "image/png");
        types.put("js", "text/javascript; charset=utf-8");
        types.put("jpg", "image/jpeg");
        types.put("ico", "image/x-icon");
    }

    public BrowserReference(AbstractBrowserChallenge challenge) {
        this.challenge = challenge;
        id = new UniqueAlltimeID();
        // this should get setter in advanced.
        this.port = BrowserSolverService.getInstance().getConfig().getLocalHttpPort();

    }

    public void open() throws IOException {
        handlerInfo = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(port, true, this);
        openURL("http://127.0.0.1:" + port + "/" + challenge.getHttpPath() + "/?id=" + id.getID());
    }

    private void openURL(String url) {

        String[] browserCmd = BrowserSolverService.getInstance().getConfig().getBrowserCommandline();

        if (browserCmd == null || browserCmd.length == 0) {
            // if (CrossSystem.isWindows()) {
            //
            // try {
            // // Get registry where we find the default browser
            //
            // ProcessOutput result = ProcessBuilderFactory.runCommand("REG", "QUERY", "HKEY_CLASSES_ROOT\\http\\shell\\open\\command");
            // String string = result.getStdOutString("UTF-8");
            // String pathToExecutable = new Regex(string, "\"([^\"]+)").getMatch(0);
            // File file = new File(pathToExecutable);
            // if (file.exists() && file.getName().toLowerCase(Locale.ENGLISH).endsWith(".exe")) {
            // browserCmd = new String[] { file.getAbsolutePath(), "%%%url%%%" };
            // if (file.getName().toLowerCase(Locale.ENGLISH).endsWith("chrome.exe")) {
            // browserCmd = new String[] { file.getAbsolutePath(), "--app=%%%url%%%", "--window-position=%%%x%%%,%%%y%%%",
            // "--window-size=%%%width%%%,%%%height%%%" };
            // }
            //
            // }
            //
            // } catch (Exception e) {
            // e.printStackTrace();
            //
            // }
            // }
        }
        if (browserCmd == null || browserCmd.length == 0) {
            CrossSystem.openURL(url);
        } else {
            // Point pos = getPreferredBrowserPosition();
            // Dimension size = getPreferredBrowserSize();
            String[] cmds = new String[browserCmd.length];
            for (int i = 0; i < browserCmd.length; i++) {
                cmds[i] = browserCmd[i].replace("%s", url);

            }

            ProcessBuilder pb = ProcessBuilderFactory.create(cmds);
            pb.redirectErrorStream(true);
            try {
                process = pb.start();

                // String str = IO.readInputStreamToString(process.getInputStream());
                // System.out.println(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //
    // private Dimension getPreferredBrowserSize() {
    // return new Dimension(300, 600);
    // }
    //
    // private Point getPreferredBrowserPosition() {
    // return new Point(0, 0);
    // }

    public void dispose() {

        if (handlerInfo != null) {
            DeprecatedAPIHttpServerController.getInstance().unregisterRequestHandler(handlerInfo);
        }

        if (process != null) {

            process.destroy();
            process = null;
        }
    }

    @Override
    public boolean onGetRequest(GetRequest request, HttpResponse response) throws BasicRemoteAPIException {

        try {
            if ("/resource".equals(request.getRequestedPath())) {
                String resourceID = request.getRequestedURLParameters().get(0).value;
                URL resource = resourceIds.get(resourceID);
                if (resource != null) {
                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, types.get(Files.getExtension(resourceID))));
                    response.getOutputStream(true).write(IO.readURL(resource));

                    return true;
                }
            }
            if (request.getRequestedPath() != null && !request.getRequestedPath().matches("^/" + Pattern.quote(challenge.getHttpPath()) + "/.*$")) {
                return false;
            }

            // custom
            boolean custom = challenge.onRawGetRequest(this, request, response);
            if (custom) {
                return true;
            }

            String pDo = request.getParameterbyKey("do");
            String id = request.getParameterbyKey("id");
            if (!StringUtils.equals(id, this.id.getID() + "")) {
                return false;
            }
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            if ("loaded".equals(pDo)) {

                HTTPHeader ua = request.getRequestHeaders().get("User-Agent");

                browserWindow = new BrowserWindow(ua == null ? null : ua.getValue(), (int) Double.parseDouble(request.getParameterbyKey("x")), (int) Double.parseDouble(request.getParameterbyKey("y")), (int) Double.parseDouble(request.getParameterbyKey("w")), (int) Double.parseDouble(request.getParameterbyKey("h")), (int) Double.parseDouble(request.getParameterbyKey("vw")), (int) Double.parseDouble(request.getParameterbyKey("vh")));
                if (BrowserSolverService.getInstance().getConfig().isAutoClickEnabled()) {
                    Rectangle elementBounds = null;
                    try {
                        elementBounds = new Rectangle((int) Double.parseDouble(request.getParameterbyKey("eleft")), (int) Double.parseDouble(request.getParameterbyKey("etop")), (int) Double.parseDouble(request.getParameterbyKey("ew")), (int) Double.parseDouble(request.getParameterbyKey("eh")));
                    } catch (Throwable e) {

                    }
                    this.viewport = challenge.getBrowserViewport(browserWindow, elementBounds);
                    if (viewport != null) {
                        viewport.onLoaded();
                    }

                    response.getOutputStream(true).write("Thanks".getBytes("UTF-8"));

                }
                return true;
            } else if ("canClose".equals(pDo)) {
                response.getOutputStream(true).write("false".getBytes("UTF-8"));
            } else if (pDo == null) {
                response.getOutputStream(true).write(challenge.getHTML().getBytes("UTF-8"));
            } else {
                return challenge.onGetRequest(this, request, response);

            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            error(response, e);
            return true;
        }

    }

    private void error(HttpResponse response, Throwable e) {
        try {
            response.setResponseCode(ResponseCode.SERVERERROR_INTERNAL);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            response.getOutputStream(true).write(Exceptions.getStackTrace(e).getBytes("UTF-8"));
        } catch (Throwable e1) {
            throw new WTFException(e1);
        }

    }

    @Override
    public boolean onPostRequest(PostRequest request, HttpResponse response) throws BasicRemoteAPIException {
        if (request.getRequestedPath() != null && !request.getRequestedPath().matches("^/" + Pattern.quote(challenge.getHttpPath()) + "/.*$")) {
            return false;
        }

        try {
            // custom
            boolean custom = challenge.onRawPostRequest(this, request, response);
            if (custom) {
                return true;
            }

            String pDo = request.getParameterbyKey("do");
            String id = request.getParameterbyKey("id");
            if (!StringUtils.equals(id, this.id.getID() + "")) {
                return false;
            }
            return challenge.onPostRequest(this, request, response);

        } catch (Throwable e) {
            error(response, e);
            return true;
        }

    }

    public abstract void onResponse(String request);

}
