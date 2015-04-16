package org.jdownloader.captcha.v2.solver.browser;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import jd.nutils.Colors;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.utils.Exceptions;
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
    private int                      x;
    private int                      y;
    private int                      width;
    private int                      height;
    private Process                  process;

    public BrowserReference(AbstractBrowserChallenge challenge) {
        this.challenge = challenge;
        id = new UniqueAlltimeID();
        this.port = 12345;

    }

    public void open() throws IOException {
        handlerInfo = DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(port, true, this);
        openURL("http://127.0.0.1:" + port + "/" + id.getID());
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
            for (int i = 0; i < browserCmd.length; i++) {
                browserCmd[i] = browserCmd[i].replace("%%%url%%%", url);

            }

            ProcessBuilder pb = ProcessBuilderFactory.create(browserCmd);
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

        if (!StringUtils.equals(request.getRequestedPath(), "/" + id.getID())) {
            return false;
        }

        try {
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));

            String pDo = request.getParameterbyKey("do");
            if ("loaded".equals(pDo)) {
                x = Integer.parseInt(request.getParameterbyKey("x"));
                y = Integer.parseInt(request.getParameterbyKey("y"));
                width = Integer.parseInt(request.getParameterbyKey("w"));
                height = Integer.parseInt(request.getParameterbyKey("h"));
                if (BrowserSolverService.getInstance().getConfig().isAutoClickEnabled()) {
                    Robot robot = new Robot();
                    BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, Math.min(500, width), Math.min(500, height)));
                    // Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(image), null, null);
                    int color = 0xff9900;
                    int dx = Integer.MAX_VALUE;
                    int dy = Integer.MAX_VALUE;
                    int dwidth = -1;
                    int dheight = -1;
                    for (int x = 0; x < Math.min(500, width); x += 3) {
                        for (int y = 0; y < Math.min(500, height); y += 3) {
                            try {
                                int localColor = image.getRGB(x, y);
                                double dif = Colors.getColorDifference(color, localColor);
                                if (dif < 1d) {
                                    dx = Math.min(dx, x);
                                    dy = Math.min(dy, y);
                                    dwidth = Math.max(dwidth, x);
                                    dheight = Math.max(dheight, y);
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // Graphics2D g = (Graphics2D) image.getGraphics();
                    // g.setColor(Color.RED);
                    // g.drawRect(dx, dy, dwidth - dx, dheight - dy);

                    image = image.getSubimage(dx, dy, dwidth - dx + 1, dheight - dy + 1);
                    // Dialog.getInstance().showConfirmDialog(0, "", "", new ImageIcon(image), null, null);

                    response.getOutputStream(true).write("ok".getBytes("UTF-8"));
                    Point oldloc = MouseInfo.getPointerInfo().getLocation();
                    int clickX = x + dx + 22 + (int) (Math.random() * 20);
                    int clickY = y + dy + 32 + (int) (Math.random() * 20);

                    robot.mouseMove(clickX, clickY);

                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);

                    robot.mouseMove(oldloc.x, oldloc.y);
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
        if (!StringUtils.equals(request.getRequestedPath(), "/" + id.getID())) {
            return false;
        }
        try {
            String parameter = request.getParameterbyKey("g-recaptcha-response");
            onResponse(parameter);
            if (parameter == null) {
                throw new WTFException("No Response");
            }
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
        } catch (Throwable e) {
            error(response, e);
            return true;
        }

    }

    abstract void onResponse(String parameter);

}
