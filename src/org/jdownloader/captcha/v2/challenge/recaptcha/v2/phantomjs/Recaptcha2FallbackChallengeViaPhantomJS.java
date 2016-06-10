package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.utils.ImagePHash;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.phantomjs.DebugWindow;
import org.jdownloader.phantomjs.PhantomJS;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;
import org.jdownloader.webcache.CachedRequest;
import org.jdownloader.webcache.WebCache;

import jd.captcha.gui.BasicWindow;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;

public final class Recaptcha2FallbackChallengeViaPhantomJS extends AbstractRecaptcha2FallbackChallenge {
    public static final String DYNAMIC    = "dynamic";

    public static final String TILESELECT = "tileselect";

    private static LogSource   LOGGER;

    static {
        LOGGER = LogController.getInstance().getLogger("PHANTOMJS");

    }

    private LogInterface             logger;

    private PhantomJS                phantom;

    private boolean                  errorAnotherOneRequired;

    private boolean                  errorDynamicTileMore;

    private boolean                  errorIncorrect;

    private HashMap<String, Payload> payloads;
    private TileContent[][]          tileGrid;

    protected String                 mainImageUrl;

    private String                   challengeType;

    private DebugWindow              debugger;

    protected String                 verificationResponse;

    protected String                 error;

    private boolean                  doRunAntiDdos;

    private int                      reloadCounter = 0;

    private String                   reloadErrorMessage;

    private boolean                  googleLoggedIn;

    @Override
    protected boolean isSlotAnnotated(int xslot, int yslot) {
        if (!DYNAMIC.equals(challengeType)) {
            return true;
        }
        return tileGrid == null || !tileGrid[xslot][yslot].isNoMatch();
    }

    public Recaptcha2FallbackChallengeViaPhantomJS(RecaptchaV2Challenge challenge) {
        super(challenge);
        doRunAntiDdos = false;
        this.logger = LogController.getRebirthLogger();
        if (this.logger == null) {
            this.logger = LOGGER;
        }
        this.load();

    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (phantom != null) {
            phantom.kill();
        }
        if (debugger != null) {
            debugger.dispose();
            debugger = null;
        }
        long started = System.currentTimeMillis();

    }

    @Override
    protected void load() {
        try {

            StatsManager.I().track("challenge", CollectionName.PJS);
            final String dummyUrl = "http://" + this.owner.getSiteDomain() + "/rc2";

            if (phantom != null) {
                try {
                    // debug only63

                    phantom.kill();
                } catch (Throwable e) {

                }
            }

            payloads = new HashMap<String, Payload>();
            phantom = new PhantomJS() {
                @Override
                protected WebCache initWebCache() {
                    return new WebCache("Recaptcha2") {
                        @Override
                        protected void putToDisk(CachedRequest cachedRequest) {
                            if (cachedRequest.getUrl().contains("payload?")) {
                                return;
                            }
                            super.putToDisk(cachedRequest);
                        }
                    };
                }

                @Override
                protected void onDisposed() {
                    super.onDisposed();
                    killSession();
                }

                @Override
                protected byte[] onRequestDone(String url, boolean getMethod, Request newRequest, byte[] bytes) throws Exception {
                    // if (url.startsWith("https://www.google.com/recaptcha/api2/frame?")) {
                    // String html = newRequest.getHtmlCode();
                    // // if (html.contains("\\x22dynamic\\x22")) {
                    // // html = html.replace("\\x22dynamic\\x22", "\\x22nocaptcha\\x22");
                    // // }
                    // return html.getBytes("UTF-8");
                    //
                    // }
                    if (url.startsWith("https://www.google.com/recaptcha/api2/payload?")) {

                        handlePayload(bytes, url);
                    } else if (url.startsWith("https://www.google.com/recaptcha/api2/userverify?")) {
                        verificationResponse = new String(bytes, "UTF-8").substring(")]}'".length());

                    }

                    return bytes;

                };

                @Override
                protected void onWebProxy(HttpRequest request, HttpResponse response, String url, String requestID, boolean getMethod) throws IOException {

                    if (dummyUrl.equals(url)) {

                        response.setResponseCode(ResponseCode.SUCCESS_OK);
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
                        String html = IO.readURLToString(Recaptcha2FallbackChallengeViaPhantomJS.class.getResource("recaptcha_simple.html"));
                        html = html.replace("%%%sitekey%%%", owner.getSiteKey());
                        String stoken = owner.getSecureToken();
                        if (StringUtils.isNotEmpty(stoken)) {
                            html = html.replace("%%%optionals%%%", "data-stoken=\"" + stoken + "\"");
                        } else {
                            html = html.replace("%%%optionals%%%", "");

                        }
                        byte[] bytes = html.getBytes("UTF-8");
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, "" + bytes.length));
                        response.getOutputStream(true).write(bytes);

                    } else {

                        super.onWebProxy(request, response, url, requestID, getMethod);
                    }
                }

            };
            phantom.setLogger(LogController.getInstance().getLogger("PhantomJS"));
            Browser br = owner.getPluginBrowser();
            phantom.setBr(br.cloneBrowser());

            try {

                // phantom.run(loadScript(getClass().getResource("phantomScript.js")));

                phantom.init();
                GoogleHelper helper = new GoogleHelper(br);
                helper.setCacheEnabled(true);
                googleLoggedIn = helper.login("google.com (Recaptcha)");

                for (Entry<String, Cookies> es : br.getCookies().entrySet()) {
                    for (Cookie c : es.getValue().getCookies()) {
                        if (c.getExpireDate() > 0) {
                            String date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK).format(new Date(c.getExpireDate()));
                            phantom.eval(" phantom.addCookie({ 'expires': '" + date + "', 'path': '" + c.getPath() + "','name': '" + c.getKey() + "', 'value': '" + c.getValue() + "',  'domain': '" + c.getDomain() + "'});");

                        } else {
                            phantom.eval(" phantom.addCookie({  'path': '" + c.getPath() + "','name': '" + c.getKey() + "', 'value': '" + c.getValue() + "',  'domain': '" + c.getDomain() + "'});");

                        }
                    }
                }
                phantom.eval("console.log(phantom.cookies);");
                phantom.eval("console.log(JSON.stringify(phantom.cookies));");
                phantom.eval(read("basics.js"));

                phantom.eval(" page.customHeaders = { 'Accept-Language': '" + (useEnglish ? "en" : TranslationFactory.getDesiredLanguage()) + "' };");
                phantom.loadPage(dummyUrl);
                if (System.getProperty("phantomjsdebug") != null) {
                    debugger = DebugWindow.show(phantom);
                }
                phantom.evalInPageContext("console.log(document.getElementsByTagName('iframe')+' - '+document.getElementsByTagName('iframe').length);");
                phantom.waitUntilDOM("document.getElementsByTagName('iframe').length>0;");
                phantom.switchFrameToChild(0);
                // long jobID = new UniqueAlltimeID().getID();
                // phantom.execute(jobID, "page.switchToChildFrame(0); endJob(" + jobID + ",null);");
                // phantom.evalInPageContext("console.log(document.getElementsByClassName('recaptcha-checkbox-checkmark').length);");
                phantom.waitUntilDOM(read("waitForCheckbox.js"));
                phantom.evalInPageContext(read("clickCheckbox.js"));
                String initScript = null;
                while (true) {
                    Thread.sleep(500);
                    phantom.switchFrameToMain();
                    phantom.switchFrameToChild(0);
                    token = (String) phantom.evalInPageContext("document.getElementById('g-recaptcha-response').value");
                    if (StringUtils.isNotEmpty(token)) {
                        StatsManager.I().track("direct", CollectionName.PJS);
                        logger.info("Wow");
                        return;
                    }
                    phantom.switchFrameToMain();
                    if (phantom.evalInPageContext("document.getElementsByTagName('iframe').length>1") == Boolean.TRUE) {
                        phantom.switchFrameToChild(1);
                        initScript = (String) phantom.evalInPageContext(read("getInitScript.js"));
                        if (StringUtils.isNotEmpty(initScript)) {
                            break;
                        }
                    }
                }

                readChallenge(initScript);
                phantom.evalInPageContext(read("basicsPage.js"));
                // BasicWindow.showImage(phantom.getScreenShot());
                // try {
                // this.waitWhile(30000, isDone);
                // } catch (InvalidDomainForSiteKeyException e) {
                // url = "http://localhost/rc2";
                //
                // this.browser.loadHTML(new LoadHTMLParams(html, "UTF-8", url));
                // this.waitWhile(30000, isDone);
                // }

                if (false) {
                    phantom.evalInPageContext(read("basicsPage.js"));
                    while (true) {
                        // Thread.sleep(1000);
                        String input = Dialog.getInstance().showInputDialog(0, "", "", "", null, null, null);
                        if (StringUtils.isNotEmpty(input)) {
                            if ("v".equals(input)) {
                                phantom.evalInPageContext("clickVerify();");
                            } else {
                                phantom.evalInPageContext("clickBox(" + (Integer.parseInt(input.trim()) - 1) + ");");
                            }
                        } else {
                            break;
                        }
                    }
                    BasicWindow.showImage(phantom.getScreenShot());
                }
            } finally {

                System.out.println("Loaded");
            }

        } catch (Throwable e) {
            this.logger.log(e);

            trackException(e);
            throw new WTFException(e);
        }

    }

    protected void trackException(Throwable e) {
        HashMap<String, String> infos = new HashMap<String, String>();
        infos.put("stack", Exceptions.getStackTrace(e));
        StackTraceElement[] stack = e.getStackTrace();
        StackTraceElement src = stack[0];
        for (StackTraceElement el : stack) {
            if (src == stack[0] && el.getLineNumber() > 0 && StringUtils.isNotEmpty(el.getFileName())) {
                src = el;
            }
            if (el.getFileName().contains(Recaptcha2FallbackChallengeViaPhantomJS.class.getSimpleName() + ".java")) {
                src = el;
                break;
            }
        }
        StatsManager.I().track(0, null, "exception/" + src.getFileName() + ":" + src.getLineNumber() + "/" + e.getMessage(), infos, CollectionName.PJS);
    }

    protected void handlePayload(byte[] bytes, String url) {
        try {
            System.out.println("New Payload: " + bytes.length + " - " + url.contains("&id") + " - " + url);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            synchronized (payloads) {
                payloads.put(url, new Payload(img, url));
                if (!url.contains("&id=")) {
                    // main image
                    mainImageUrl = url;
                    ImageIO.write(img, "png", getImageFile());

                } else if (!Application.isJared(null)) {
                    saveTile(bytes, url);

                }
                payloads.notifyAll();
            }

            // BasicWindow.showImage(payloadImage);
        } catch (IOException e) {
            trackException(e);
            throw new WTFException(e);
        }
    }

    protected void saveTile(byte[] bytes, String url) throws IOException {
        int i = 0;
        File file = null;
        do {
            file = Application.getResource("tmp/rc2/" + challengeType + "/" + highlightedExplain + "/" + Hash.getMD5(bytes) + "_" + i + ".png");

            i++;
        } while (file.exists());
        file.getParentFile().mkdirs();
        IO.writeToFile(file, bytes);
        if (i > 1) {
            Dialog.getInstance().showMessageDialog("DUPE: " + file);
        }
        String phash = null;

        try {
            phash = new ImagePHash(32, 8).getHash(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        file = Application.getResource("tmp/rc2/" + challengeType + "/" + highlightedExplain + "/logs.txt");
        IO.writeStringToFile(file, Hash.getMD5(bytes) + "," + new Regex(url, "\\&id=([^\\&]+)").getMatch(0) + "," + phash + "\r\n", true);
    }

    protected void readChallenge(String initScript) throws IOException, InterruptedException, TimeoutException {
        String json = new Regex(initScript, "recaptcha\\.frame\\.Main\\.init\\(\"(.*)\"\\)\\;").getMatch(0);
        phantom.setVariable("initScript", json);
        Map<String, Object> initData = (Map<String, Object>) phantom.get(read("extractInitData.js"));
        handleInitData(initData);
    }

    protected void handleInitData(Map<String, Object> initData) throws InterruptedException, IOException, TimeoutException {
        setSplitWidth(((Number) initData.get("x")).intValue());
        setSplitHeight(((Number) initData.get("y")).intValue());
        doRunAntiDdos = true;
        tileGrid = new TileContent[getSplitWidth()][getSplitHeight()];
        type = (String) initData.get("contentType");
        challengeType = (String) initData.get("challengeType");
        HashMap<String, String> explain = JSonStorage.convert(phantom.evalInPageContext(read("extractExplanation.js")), TypeRef.HASHMAP_STRING);

        String decs = explain.get("description");
        StatsManager.I().track("challengeType/" + challengeType, CollectionName.PJS);
        if (TILESELECT.equalsIgnoreCase(challengeType)) {
            decs += "<br>" + _GUI.T.RECAPTCHA_2_Dialog_help_tile_selection(explain.get("tag"));
        }
        if (DYNAMIC.equalsIgnoreCase(challengeType) && !googleLoggedIn) {
            JDGui.help(_GUI.T.phantomjs_recaptcha_google_account_title(), _GUI.T.phantomjs_recaptcha_google_account_msg(), new AbstractIcon(IconKey.ICON_OCR, 32));

        }
        setExplain(decs);

        this.highlightedExplain = explain.get("tag");

        String exampleDataUrl = (String) initData.get("explainUrl");
        explainIcon = null;
        if (exampleDataUrl != null) {
            try {
                explainIcon = IconIO.getIconFromDataUrl(exampleDataUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String html = phantom.getFrameHtml();
        final Object mainPayloadUrl = phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-wrapper')[0].getElementsByTagName('img')[0].src");
        // wait until the payload image has been loaded

        waitFor(60000, payloads, new Condition() {

            @Override
            public boolean breakIfTrue() {
                return payloads.containsKey(mainPayloadUrl);
            }

        });

        for (int x = 0; x < getSplitWidth(); x++) {
            for (int y = 0; y < getSplitHeight(); y++) {

                tileGrid[x][y] = new TileContent(payloads.get(mainPayloadUrl));
                // tileGrid.put(x*y, new Payload(main))
            }
        }
        if (!Application.isJared(null)) {
            BufferedImage img = payloads.get(mainImageUrl).image;
            if (TILESELECT.equalsIgnoreCase(challengeType)) {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                ImageIO.write(img, "png", bos);
                saveTile(bos.toByteArray(), mainImageUrl);
            } else {
                double tileWidth = (double) img.getWidth() / getSplitWidth();
                double tileHeight = (double) img.getHeight() / getSplitHeight();
                for (int x = 0; x < getSplitWidth(); x++) {
                    for (int y = 0; y < getSplitHeight(); y++) {

                        BufferedImage imgnew = IconIO.createEmptyImage((int) Math.ceil(tileWidth), (int) Math.ceil(tileHeight));
                        Graphics2D g2d = (Graphics2D) imgnew.getGraphics();
                        g2d.drawImage(img, 0, 0, (int) Math.ceil(tileWidth), (int) Math.ceil(tileHeight), (int) (x * tileWidth), (int) (y * tileHeight), (int) (x * tileWidth) + (int) Math.ceil(tileWidth), (int) (y * tileHeight) + (int) Math.ceil(tileHeight), null);
                        g2d.dispose();

                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                        ImageIO.write(imgnew, "png", bos);
                        saveTile(bos.toByteArray(), mainImageUrl);
                    }
                }
            }
        }
    }

    private void waitFor(int timeout, Object lockObject, Condition condition) throws TimeoutException, InterruptedException, IOException {
        long started = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - started > timeout && timeout > 0) {
                throw new TimeoutException();
            }
            if (lockObject == null) {
                if (condition.breakIfTrue()) {
                    return;
                }
                Thread.sleep(500);
            } else {
                synchronized (lockObject) {
                    if (condition.breakIfTrue()) {
                        return;
                    }
                    lockObject.wait(1000);
                    if (condition.breakIfTrue()) {
                        return;
                    }
                }
            }
        }
    }

    private String read(String string) throws IOException {
        return IO.readURLToString(Recaptcha2FallbackChallengeViaPhantomJS.class.getResource(string));
    }

    public boolean doRunAntiDDosProtection() {
        try {
            return doRunAntiDdos;
        } finally {
            doRunAntiDdos = false;
        }
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        try {
            if (response.getPriority() <= 0) {

                killSession();
                return false;
            }

            errorAnotherOneRequired = false;
            errorDynamicTileMore = false;
            errorIncorrect = false;
            reloadErrorMessage = null;
            // BasicWindow.showImage(phantom.getScreenShot());
            final HashSet<String> dupe = new HashSet<String>();
            phantom.switchFrameToMain();
            // String htmlMain = phantom.getFrameHtml();
            phantom.switchFrameToChild(1);
            // String htmlFrame = phantom.getFrameHtml();

            final String[] parts = response.getValue().split("[,]+");
            int clickedElements = 0;
            for (int i = 0; i < parts.length; i++) {
                try {
                    final int num = Integer.parseInt(parts[i]) - 1;

                    if (dupe.add(Integer.toString(num)) && num < getSplitHeight() * getSplitWidth()) {
                        phantom.evalInPageContext("clickBox(" + (num) + ");");
                        tileGrid[num % getSplitWidth()][num / getSplitWidth()].setNoMatch(false);
                        tileGrid[num % getSplitWidth()][num / getSplitWidth()].setAsyncJsStuffInProgress(true);
                        clickedElements++;

                    }
                } catch (NumberFormatException e) {

                }
            }

            for (int x = 0; x < getSplitWidth(); x++) {
                for (int y = 0; y < getSplitHeight(); y++) {
                    if (!dupe.contains(Integer.toString(x + y * getSplitWidth()))) {
                        tileGrid[x][y].setNoMatch(true);
                    }

                }
            }
            if (DYNAMIC.equals(challengeType)) {
                if (clickedElements > 0) {
                    waitFor(60000, payloads, new Condition() {

                        @Override
                        public boolean breakIfTrue() throws InterruptedException, IOException {

                            for (int i = 0; i < parts.length; i++) {
                                try {
                                    final int num = Integer.parseInt(parts[i]) - 1;

                                    if (dupe.contains(Integer.toString(num)) && num < getSplitHeight() * getSplitWidth()) {
                                        TileContent tile = tileGrid[num % getSplitWidth()][num / getSplitWidth()];
                                        if (tile.isAsyncJsStuffInProgress()) {
                                            String tileUrl;

                                            tileUrl = (String) phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-target')[" + num + "].getElementsByTagName('img')[0].src");

                                            if (!StringUtils.equals(tileUrl, tile.getPayload().url)) {

                                                Payload newPayload = payloads.get(tileUrl);

                                                if (newPayload != null) {

                                                    tile.setPayload(newPayload);
                                                    tile.setAsyncJsStuffInProgress(false);
                                                }
                                            }
                                        }
                                    }
                                } catch (NumberFormatException e) {

                                }
                            }

                            for (int x = 0; x < getSplitWidth(); x++) {
                                for (int y = 0; y < getSplitHeight(); y++) {

                                    if (tileGrid[x][y].isAsyncJsStuffInProgress()) {
                                        return false;
                                    }

                                }
                            }

                            return true;
                        }
                    });
                    return true;
                } else {
                    // continue
                }

            } else if ("imageselect".equals(challengeType) || TILESELECT.equals(challengeType)) {

                waitFor(15000, null, new Condition() {

                    @Override
                    public boolean breakIfTrue() throws InterruptedException, IOException {

                        for (int i = 0; i < parts.length; i++) {
                            try {
                                final int num = Integer.parseInt(parts[i]) - 1;

                                if (dupe.contains(Integer.toString(num)) && num < getSplitHeight() * getSplitWidth()) {
                                    TileContent tile = tileGrid[num % getSplitWidth()][num / getSplitWidth()];
                                    if (tile.isAsyncJsStuffInProgress()) {
                                        String tdElementClassname = (String) phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-target')[" + num + "].parentNode.className");
                                        if (StringUtils.isNotEmpty(tdElementClassname)) {
                                            tile.setAsyncJsStuffInProgress(false);

                                        }
                                    }
                                }
                            } catch (NumberFormatException e) {

                            }
                        }

                        for (int x = 0; x < getSplitWidth(); x++) {
                            for (int y = 0; y < getSplitHeight(); y++) {

                                if (tileGrid[x][y].isAsyncJsStuffInProgress()) {
                                    return false;
                                }

                            }
                        }

                        return true;
                    }
                });

            }
            // while (true) {
            // // Thread.sleep(1000);
            // String input = Dialog.getInstance().showInputDialog(0, "", "", "", null, null, null);
            // if (StringUtils.isNotEmpty(input)) {
            // phantom.evalInPageContext("clickBox(" + input + ");");
            // } else {
            // break;
            // }
            // }
            // BasicWindow.showImage(phantom.getScreenShot());

            phantom.evalInPageContext("clickVerify();");

            waitFor(10000, null, new Condition() {

                @Override
                public boolean breakIfTrue() throws InterruptedException, IOException {
                    phantom.switchFrameToMain();
                    phantom.switchFrameToChild(1);
                    errorAnotherOneRequired = phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-error-select-more')[0].style.display!='none'") == Boolean.TRUE;
                    if (errorAnotherOneRequired) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_MORE_REQUIRED();
                        return true;
                    }
                    errorDynamicTileMore = phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-error-dynamic-more')[0].style.display!='none'") == Boolean.TRUE;
                    if (errorDynamicTileMore) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_TILE_MORE();

                        return true;
                    }
                    errorIncorrect = phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-incorrect-response')[0].style.display!='none'") == Boolean.TRUE;
                    if (errorIncorrect) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_ANOTHER_CHALLENGE();

                        return true;
                    }
                    phantom.switchFrameToMain();

                    String html = (String) phantom.get("page.frameContent");
                    token = (String) phantom.evalInPageContext("document.getElementById('g-recaptcha-response').value");
                    if (StringUtils.isNotEmpty(token)) {
                        HashMap<String, String> infos = new HashMap<String, String>();
                        infos.put("reloadCount", reloadCounter + "");
                        StatsManager.I().track("solved", infos, CollectionName.PJS);

                        return true;
                    }
                    phantom.switchFrameToChild(0);
                    error = (String) phantom.evalInPageContext("document.getElementsByClassName('rc-anchor-error-msg')[0].innerText");
                    if (StringUtils.isNotEmpty(error)) {
                        return true;
                    }
                    return false;
                }

            });
            if (StringUtils.isNotEmpty(error)) {
                killSession();
                return false;
            }
            if (errorDynamicTileMore || errorAnotherOneRequired) {
                for (int x = 0; x < getSplitWidth(); x++) {
                    for (int y = 0; y < getSplitHeight(); y++) {

                        TileContent tile = tileGrid[x][y];
                        tile.setNoMatch(false);
                    }
                }
            }
            if (errorIncorrect) {

                phantom.execute(" _global['verificationResponse']=" + verificationResponse + ";");
                Map<String, Object> initData = (Map<String, Object>) phantom.get(read("extractInitDataFromVerificationResponse.js"));
                handleInitData(initData);
                // another
                // readChallenge(initScript);
            }
            // if (errorIncorrect) {
            // killSession();
            // return false;
            // } else {
            return true;
            // }
        } catch (Throwable e1) {
            this.logger.log(e1);
            trackException(e1);
            killSession();
            return false;
        } finally

        {

        }

    }

    public String getReloadErrorMessage() {
        return reloadErrorMessage;
    }
    // private void getVisibleError(String className) throws CancelException {
    //
    // boolean hidden = this.browser.executeJavaScriptAndReturnValue(this.frameFrame,
    // "window.getComputedStyle(document.getElementsByClassName('" + className + "')[0]).display==='none'").getBoolean();
    // if (!hidden) {
    // throw new CancelException(className);
    // }
    //
    // }

    @Override
    public void reload(int round) throws Throwable {
        if (!phantom.isRunning()) {
            throw new IOException("PhantomJS is not running");
        }
        reloadCounter++;

        HashMap<String, String> infos = new HashMap<String, String>();
        infos.put("counter", reloadCounter + "");

        StatsManager.I().track(100, "reloadCount", "reload", infos, CollectionName.PJS);
        Payload mainImage = payloads.get(mainImageUrl);

        BufferedImage imgnew = IconIO.createEmptyImage(mainImage.image.getWidth(), mainImage.image.getHeight());
        Graphics2D g2d = (Graphics2D) imgnew.getGraphics();

        if (DYNAMIC.equals(challengeType)) {
            double tileWidth = (double) mainImage.image.getWidth() / getSplitWidth();
            double tileHeight = (double) mainImage.image.getHeight() / getSplitHeight();

            BufferedImage grayOriginal = null;
            for (int x = 0; x < getSplitWidth(); x++) {
                for (int y = 0; y < getSplitHeight(); y++) {
                    int tileX = (int) (x * tileWidth);
                    int tileY = (int) (y * tileHeight);
                    TileContent tile = tileGrid[x][y];
                    if (tile.isNoMatch()) {
                        if (tile.getPayload().url.contains("&id=")) {
                            g2d.drawImage(ImageProvider.convertToGrayScale(tile.getPayload().image), (int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight, null);
                        } else {
                            if (grayOriginal == null) {
                                grayOriginal = ImageProvider.convertToGrayScale(mainImage.image);
                            }
                            g2d.drawImage(grayOriginal, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, null);

                        }
                        Composite c = g2d.getComposite();
                        try {
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect((int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight);
                        } finally {
                            g2d.setComposite(c);
                        }

                    } else {
                        if (tile.getPayload().url.contains("&id=")) {
                            g2d.drawImage(tile.getPayload().image, (int) (x * tileWidth), (int) (y * tileHeight), (int) tileWidth, (int) tileHeight, null);
                        } else {
                            g2d.drawImage(mainImage.image, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, tileX, tileY, tileX + (int) tileWidth, tileY + (int) tileHeight, null);

                        }

                    }
                }
            }

        } else {
            g2d.drawImage(mainImage.image, 0, 0, null);
        }
        if (!getExplain().contains(_GUI.T.RECAPTCHA_2_Dialog_help_dynamic())) {
            setExplain(getExplain() + " <br>" + _GUI.T.RECAPTCHA_2_Dialog_help_dynamic());
        }
        g2d.dispose();
        getImageFile().delete();
        ImageIO.write(imgnew, "png", getImageFile());

    }

    public int getReloadCounter() {
        return reloadCounter;
    }

}