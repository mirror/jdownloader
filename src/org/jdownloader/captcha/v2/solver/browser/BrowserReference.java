package org.jdownloader.captcha.v2.solver.browser;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
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
import org.jdownloader.captcha.v2.challenge.areyouahuman.AreYouAHumanChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
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

    public BrowserReference(AbstractBrowserChallenge challenge) {
        this.challenge = challenge;
        id = new UniqueAlltimeID();
        // this should get setter in advanced.
        this.port = 12345;
    }

    public void open() throws IOException {
        handlerInfo = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(port, true, this);
        openURL("http://127.0.0.1:" + port + "/" + Hash.getMD5(this.challenge.getPlugin().getClass().getName()) + "?id=" + id.getID());
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

        if (!StringUtils.equals(request.getRequestedURL(), "/" + Hash.getMD5(this.challenge.getPlugin().getClass().getName()) + "?id=" + id.getID()) && !StringUtils.contains(request.getRequestedURL(), "?do=loaded")) {
            return false;
        }

        try {
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            String pDo = request.getParameterbyKey("do");
            if ("loaded".equals(pDo)) {

                browserWindow = new BrowserWindow(Integer.parseInt(request.getParameterbyKey("x")), Integer.parseInt(request.getParameterbyKey("y")), Integer.parseInt(request.getParameterbyKey("w")), Integer.parseInt(request.getParameterbyKey("h")), Integer.parseInt(request.getParameterbyKey("vw")), Integer.parseInt(request.getParameterbyKey("vh")));
                if (BrowserSolverService.getInstance().getConfig().isAutoClickEnabled()) {

                    this.viewport = challenge.getBrowserViewport(browserWindow);
                    viewport.onLoaded();
                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

                    response.getOutputStream(true).write("ok".getBytes("UTF-8"));

                }
                return true;
            }

            response.getOutputStream(true).write(challenge.getHTML().getBytes("UTF-8"));
            return true;
        } catch (Throwable e) {
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
        if (!StringUtils.equals(request.getRequestedURL(), "/" + Hash.getMD5(this.challenge.getPlugin().getClass().getName()) + "?id=" + id.getID())) {
            return false;
        }
        try {
            if (challenge instanceof RecaptchaV1Challenge) {
                String challenge = request.getParameterbyKey("recaptcha_challenge_field");
                String responseString = request.getParameterbyKey("recaptcha_challenge_field");
                onResponse(JSonStorage.serializeToJson(new String[] { challenge, responseString }));

                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

                response.getOutputStream(true).write("You can close the browser now".getBytes("UTF-8"));

                // Close Browser Tab
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_W);

                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_W);
                return true;
            } else if (challenge instanceof AreYouAHumanChallenge) {

                // recaptch2
                String parameter = request.getParameterbyKey("session_secret");
                if (parameter == null) {
                    throw new WTFException("No Response");
                }
                onResponse(parameter);

                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

                response.getOutputStream(true).write("You can close the browser now".getBytes("UTF-8"));

                // Close Browser Tab
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_W);

                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_W);
                return true;

            } else if (challenge instanceof RecaptchaV2Challenge) {

                // recaptch2
                String parameter = request.getParameterbyKey("g-recaptcha-response");
                if (parameter == null) {
                    throw new WTFException("No Response");
                }
                onResponse(parameter);

                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

                response.getOutputStream(true).write("You can close the browser now".getBytes("UTF-8"));

                // Close Browser Tab
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_W);

                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_W);
                return true;
            } else {
                return false;
            }
        } catch (Throwable e) {
            error(response, e);
            return true;
        }

    }

    abstract void onResponse(String request);

}
