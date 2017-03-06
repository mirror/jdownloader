package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.utils.ImagePHash;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ValidationResult;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.SubChallenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.phantomjs.DebugWindow;
import org.jdownloader.phantomjs.PhantomJS;
import org.jdownloader.plugins.components.google.GoogleAccountConfig;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.config.AccountJsonConfig;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;
import org.jdownloader.webcache.CachedRequest;
import org.jdownloader.webcache.WebCache;

import jd.captcha.gui.BasicWindow;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.JDGui;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.plugins.Account;

public class Recaptcha2FallbackChallengeViaPhantomJS extends AbstractRecaptcha2FallbackChallenge {
    private static LogSource LOGGER;
    static {
        LOGGER = LogController.getInstance().getLogger("PHANTOMJS");
    }

    @Override
    protected ArrayList<String> addAnnotationLines() {
        ArrayList<String> lines = new ArrayList<String>();
        SubChallenge sc = getSubChallenge();
        if (sc.getGridHeight() * sc.getGridWidth() < 10) {
            lines.add("Example answer: 2,5,6 or 256");
        } else {
            lines.add("Example answer: 2,5,6 Use , deliminator");
        }
        if (sc.getChallengeType() == ChallengeType.DYNAMIC && sc.getDynamicRoundCount() > 1) {
            lines.add("If there is no match, answer with \"0\"");
        }
        return lines;
    }

    private LogInterface      logger;
    private PhantomJS         phantom;
    private DebugWindow       debugger;
    protected String          verificationResponse;
    protected volatile String error;
    private boolean           doRunAntiDdos;
    private boolean           googleLoggedIn;
    protected volatile String reloadResponse;

    @Override
    protected boolean isSlotAnnotated(int xslot, int yslot) {
        return getSubChallenge().isSlotAnnotated(xslot, yslot);
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
        if (!googleLoggedIn) {
            synchronized (GOOGLE) {
                final Cookies google = phantom.getBr().getCookies("https://www.google.com");
                GOOGLE.add(google);
            }
        }
        if (phantom != null) {
            phantom.kill();
        }
        if (debugger != null) {
            debugger.dispose();
            debugger = null;
        }
    }

    private final static Cookies GOOGLE = new Cookies();

    @Override
    protected void load() {
        try {
            StatsManager.I().track("challenge", CollectionName.PJS);
            final String dummyUrl;
            if (owner.getPluginBrowser().getRequest() != null) {
                dummyUrl = URLHelper.getURL(owner.getPluginBrowser().getRequest().getURL(), false, false, false).toString();
            } else {
                dummyUrl = "http://" + owner.getSiteDomain() + "/rc" + UniqueAlltimeID.create();
            }
            if (phantom != null) {
                try {
                    // debug only63
                    phantom.kill();
                } catch (Throwable e) {
                }
            }
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
                    } else if (url.startsWith("https://www.google.com/recaptcha/api2/reload?k=")) {
                        reloadResponse = new String(bytes, "UTF-8").substring(")]}'".length());
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
                try {
                    for (Account acc : AccountController.getInstance().list("recaptcha.google.com")) {
                        if (acc.isEnabled()) {
                            final GoogleAccountConfig cfg = (GoogleAccountConfig) AccountJsonConfig.get(acc);
                            if (cfg.isUsageRecaptchaV2Enabled()) {
                                final GoogleHelper helper = new GoogleHelper(br);
                                helper.setCacheEnabled(true);
                                googleLoggedIn = helper.login(acc);
                                if (googleLoggedIn) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.log(e);
                }
                if (!googleLoggedIn) {
                    synchronized (GOOGLE) {
                        if (GOOGLE.isEmpty()) {
                            final Browser google = br.cloneBrowser();
                            google.setFollowRedirects(true);
                            google.setCookie("https://google.com", "PREF", "hl=en-GB");
                            google.getPage("https://www.google.com");
                            google.getPage("https://consent.google.com/status?continue=https://www." + google.getHost() + "&pc=s&timestamp=" + System.currentTimeMillis());
                            GOOGLE.add(google.getCookies("https://www.google.com"));
                        } else {
                            br.getCookies("https://www.google.com").add(GOOGLE);
                        }
                    }
                }
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
                waitFor(30000, null, new Condition() {
                    @Override
                    public boolean breakIfTrue() throws InterruptedException, IOException {
                        return hasSubChallenge() && StringUtils.isNotEmpty(reloadResponse);
                    }
                });
                phantom.switchFrameToMain();
                // String mainHtml = (String) phantom.get("page.frameContent");
                phantom.switchFrameToChild(0);
                // String childHtml = (String) phantom.get("page.frameContent");
                final String token = (String) phantom.evalInPageContext("document.getElementById('g-recaptcha-response').value");
                if (StringUtils.isNotEmpty(token)) {
                    Recaptcha2FallbackChallengeViaPhantomJS.this.token = token;
                    StatsManager.I().track("direct", CollectionName.PJS);
                    logger.info("Wow?!");
                    return;
                }
                phantom.switchFrameToMain();
                phantom.switchFrameToChild(1);
                phantom.evalInPageContext(read("basicsPage.js"));
                phantom.execute(" _global['reloadResponse']=" + reloadResponse + ";");
                Map<String, Object> initData = (Map<String, Object>) phantom.get(read("extractInitDataFromReloadResponse.js"));
                handleInitData(initData);
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
            if (el.getFileName() != null && el.getFileName().contains(Recaptcha2FallbackChallengeViaPhantomJS.class.getSimpleName() + ".java")) {
                src = el;
                break;
            }
        }
        StatsManager.I().track(0, null, "exception/" + src.getFileName() + ":" + src.getLineNumber() + "/" + e.getMessage(), infos, CollectionName.PJS);
    }

    protected synchronized void handlePayload(byte[] bytes, String url) {
        try {
            logger.info("New Payload: " + bytes.length + " - " + url.contains("&id") + " - " + url);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (getSubChallenge() == null || (!url.contains("&id=") && getSubChallenge().getMainPayload() != null)) {
                createNewSubChallenge();
            }
            getSubChallenge().putPayload(url, new Payload(img, url));
            if (!url.contains("&id=")) {
                // main image
                // mainImageUrl = url;
                ImageIO.write(img, "png", getImageFile());
            } else if (!Application.isJared(null)) {
                saveTile(bytes, url);
            }
            synchronized (getSubChallenge()) {
                getSubChallenge().notifyAll();
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
            file = Application.getResource("tmp/rc2/" + getSubChallenge().getChallengeType() + "/" + getSubChallenge().getSearchKey() + "/" + Hash.getMD5(bytes) + "_" + i + ".png");
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
        file = Application.getResource("tmp/rc2/" + getSubChallenge().getChallengeType() + "/" + getSubChallenge().getSearchKey() + "/logs.txt");
        IO.writeStringToFile(file, Hash.getMD5(bytes) + "," + new Regex(url, "\\&id=([^\\&]+)").getMatch(0) + "," + phash + "\r\n", true);
    }

    @Override
    protected void onNewChallenge(SubChallenge sc) {
        ArrayList<SubChallenge> all = getSubChallenges();
        for (int i = 0; i < all.size() - 2; i++) {
            for (Response r : all.get(i).getResponses()) {
                r.getResponse().setValidation(ValidationResult.INVALID);
            }
        }
    }

    protected void handleInitData(Map<String, Object> initData) throws InterruptedException, IOException, TimeoutException {
        try {
            for (int i = 0; i < 25; i++) {
                boolean reloadChallenge = "unknown".equals(initData.get("challengeType"));
                // reloadChallenge |= "dynamic".equals(initData.get("challengeType"));
                // reloadChallenge |= "ImageSelectStoreFront".equals(initData.get("contentType"));
                reloadChallenge |= "multicaptcha".equals(initData.get("challengeType"));
                if (reloadChallenge) {
                    reloadResponse = null;
                    createNewSubChallenge();
                    phantom.evalInPageContext("console.log(clickReload)");
                    phantom.evalInPageContext("clickReload();");
                    waitFor(30000, null, new Condition() {
                        @Override
                        public boolean breakIfTrue() throws InterruptedException, IOException {
                            return hasSubChallenge() && getSubChallenge().hasPayload() && StringUtils.isNotEmpty(reloadResponse);
                        }
                    });
                    phantom.execute(" _global['reloadResponse']=" + reloadResponse + ";");
                    initData = (Map<String, Object>) phantom.get(read("extractInitDataFromReloadResponse.js"));
                } else {
                    break;
                }
            }
            final SubChallenge subChallenge = getSubChallenge();
            subChallenge.initGrid(((Number) initData.get("x")).intValue(), ((Number) initData.get("y")).intValue());
            doRunAntiDdos = true;
            subChallenge.setType((String) initData.get("contentType"));
            try {
                subChallenge.setChallengeType(ChallengeType.valueOf(String.valueOf(initData.get("challengeType")).toUpperCase(Locale.ENGLISH)));
            } catch (Throwable e) {
                logger.info("Unknown Challenge Type: " + JSonStorage.serializeToJson(initData));
                throw new WTFException(e);
            }
            final AtomicReference<HashMap<String, String>> explain = new AtomicReference<HashMap<String, String>>();
            phantom.switchFrameToMain();
            phantom.switchFrameToChild(1);
            // wait for page
            waitFor(10000, null, new Condition() {
                @Override
                public boolean breakIfTrue() throws InterruptedException, IOException {
                    explain.set(JSonStorage.convert(phantom.evalInPageContext(read("extractExplanation.js")), TypeRef.HASHMAP_STRING));
                    if (explain.get() != null) {
                        return true;
                    }
                    return false;
                }
            });
            logger.info(JSonStorage.serializeToJson(explain.get()));
            String decs = explain.get().get("description").replace("Click verify once there are none left.", "").trim();
            ;
            StatsManager.I().track("challengeType/" + subChallenge.getChallengeType(), CollectionName.PJS);
            if (subChallenge.getChallengeType() == ChallengeType.TILESELECT) {
                decs += "<br>" + _GUI.T.RECAPTCHA_2_Dialog_help_tile_selection(explain.get().get("tag"));
            }
            if (subChallenge.getChallengeType() == ChallengeType.DYNAMIC && !googleLoggedIn) {
                if (Application.isHeadless()) {
                    UIOManager.I().show(ConfirmDialogInterface.class, new ConfirmDialog(UIOManager.BUTTONS_HIDE_OK, _GUI.T.phantomjs_recaptcha_google_account_title(), _GUI.T.phantomjs_recaptcha_google_account_msg(), new AbstractIcon(IconKey.ICON_OCR, 32), null, _GUI.T.lit_close()));
                } else {
                    JDGui.help(_GUI.T.phantomjs_recaptcha_google_account_title(), _GUI.T.phantomjs_recaptcha_google_account_msg(), new AbstractIcon(IconKey.ICON_OCR, 32));
                }
            }
            setExplain(decs);
            subChallenge.setSearchKey(explain.get().get("tag"));
            // String exampleDataUrl = (String) initData.get("explainUrl");
            // String html = phantom.getFrameHtml();
            final Object mainPayloadUrl = phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-wrapper')[0].getElementsByTagName('img')[0].src");
            // wait until the payload image has been loaded
            logger.info("Wait until Payload has been loaded: " + mainPayloadUrl);
            waitFor(60000, subChallenge, new Condition() {
                @Override
                public boolean breakIfTrue() {
                    return subChallenge.containsPayload(mainPayloadUrl);
                }
            });
            subChallenge.fillGrid((String) mainPayloadUrl);
            if (!Application.isJared(null)) {
                BufferedImage img = subChallenge.getPayloadByUrl(subChallenge.getMainImageUrl()).image;
                if (subChallenge.getChallengeType() == ChallengeType.TILESELECT) {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(img, "png", bos);
                    saveTile(bos.toByteArray(), subChallenge.getMainImageUrl());
                } else {
                    double tileWidth = (double) img.getWidth() / subChallenge.getGridWidth();
                    double tileHeight = (double) img.getHeight() / subChallenge.getGridHeight();
                    for (int x = 0; x < subChallenge.getGridWidth(); x++) {
                        for (int y = 0; y < subChallenge.getGridHeight(); y++) {
                            BufferedImage imgnew = IconIO.createEmptyImage((int) Math.ceil(tileWidth), (int) Math.ceil(tileHeight));
                            Graphics2D g2d = (Graphics2D) imgnew.getGraphics();
                            g2d.drawImage(img, 0, 0, (int) Math.ceil(tileWidth), (int) Math.ceil(tileHeight), (int) (x * tileWidth), (int) (y * tileHeight), (int) (x * tileWidth) + (int) Math.ceil(tileWidth), (int) (y * tileHeight) + (int) Math.ceil(tileHeight), null);
                            g2d.dispose();
                            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ImageIO.write(imgnew, "png", bos);
                            saveTile(bos.toByteArray(), subChallenge.getMainImageUrl());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new WTFException(e);
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

    private String reloadErrorMessage;

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        try {
            if (response.getPriority() <= 0) {
                killSession();
                return false;
            }
            final SubChallenge sc = getSubChallenge();
            sc.resetErrors();
            reloadErrorMessage = null;
            // BasicWindow.showImage(phantom.getScreenShot());
            phantom.switchFrameToMain();
            // String htmlMain = phantom.getFrameHtml();
            phantom.switchFrameToChild(1);
            // String htmlFrame = phantom.getFrameHtml();
            final Response rc2Resp = new Response(response, sc);
            sc.addResponse(rc2Resp);
            verificationResponse = null;
            reloadResponse = null;
            phantom.switchFrameToMain();
            phantom.switchFrameToChild(1);
            if (sc.isDynamicAndHasZerosInARow(2)) {
                // reload, if to 0s want a verification, but rc claims that there is still an image missing
                final SubChallenge newSC = createNewSubChallenge();
                phantom.evalInPageContext("clickReload();");
                waitFor(30000, newSC, new Condition() {
                    @Override
                    public boolean breakIfTrue() throws InterruptedException, IOException {
                        return newSC.hasPayload() && StringUtils.isNotEmpty(reloadResponse);
                    }
                });
                phantom.execute(" _global['reloadResponse']=" + reloadResponse + ";");
                Map<String, Object> initData = (Map<String, Object>) phantom.get(read("extractInitDataFromReloadResponse.js"));
                handleInitData(initData);
                return true;
            }
            for (Integer num : rc2Resp.getClickedIndices()) {
                if (num >= 0) {
                    phantom.evalInPageContext("clickBox(" + (num) + ");");
                    sc.getTile(num).setAsyncJsStuffInProgress(true);
                }
            }
            switch (sc.getChallengeType()) {
            case DYNAMIC:
                if (rc2Resp.getSize() > 0 && rc2Resp.getClickedIndices().get(0) != -1) {
                    waitFor(60000, sc, new Condition() {
                        @Override
                        public boolean breakIfTrue() throws InterruptedException, IOException {
                            for (final Integer num : rc2Resp.getClickedIndices()) {
                                TileContent tile = sc.getTile(num);
                                if (tile.isAsyncJsStuffInProgress()) {
                                    String tileUrl;
                                    tileUrl = (String) phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-target')[" + num + "].getElementsByTagName('img')[0].src");
                                    if (!StringUtils.equals(tileUrl, tile.getPayload().url)) {
                                        Payload newPayload = sc.getPayloadByUrl(tileUrl);
                                        if (newPayload != null) {
                                            tile.setPayload(newPayload);
                                            tile.setAsyncJsStuffInProgress(false);
                                        }
                                    }
                                }
                            }
                            for (int x = 0; x < sc.getGridWidth(); x++) {
                                for (int y = 0; y < sc.getGridHeight(); y++) {
                                    if (sc.getTile(x, y).isAsyncJsStuffInProgress()) {
                                        return false;
                                    }
                                }
                            }
                            return true;
                        }
                    });
                    return true;
                } else {
                    // int ret = 0;
                    // sc.get
                    // for (int x = 0; x < sc.getGridWidth(); x++) {
                    // for (int y = 0; y < sc.getGridHeight(); y++) {
                    // if (sc.getTile(x, y).isNoMatch()) {
                    // ret++;
                    // }
                    // }
                    // }
                    // if (ret == 0) {
                    // response.setValidation(ValidationResult.INVALID);
                    // return false;
                    // } else {
                    // // empty. and we have a preselection
                    // }
                    // continue
                }
                break;
            case IMAGESELECT:
            case TILESELECT:
                if (rc2Resp.getSize() > 0) {
                    waitFor(15000, null, new Condition() {
                        @Override
                        public boolean breakIfTrue() throws InterruptedException, IOException {
                            for (final Integer num : rc2Resp.getClickedIndices()) {
                                TileContent tile = sc.getTile(num);
                                if (tile.isAsyncJsStuffInProgress()) {
                                    String tdElementClassname = (String) phantom.evalInPageContext("document.getElementsByClassName('rc-image-tile-target')[" + num + "].parentNode.className");
                                    if (StringUtils.isNotEmpty(tdElementClassname)) {
                                        tile.setAsyncJsStuffInProgress(false);
                                    }
                                }
                            }
                            for (int x = 0; x < sc.getGridWidth(); x++) {
                                for (int y = 0; y < sc.getGridHeight(); y++) {
                                    if (sc.getTile(x, y).isAsyncJsStuffInProgress()) {
                                        return false;
                                    }
                                }
                            }
                            return true;
                        }
                    });
                } else {
                    response.setValidation(ValidationResult.INVALID);
                    return false;
                }
            }
            phantom.evalInPageContext("clickVerify();");
            waitFor(10000, null, new Condition() {
                @Override
                public boolean breakIfTrue() throws InterruptedException, IOException {
                    phantom.switchFrameToMain();
                    // String mainHtml = (String) phantom.get("page.frameContent");
                    phantom.switchFrameToChild(1);
                    // String childHtml = (String) phantom.get("page.frameContent");
                    sc.setErrorAnotherOneRequired(phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-error-select-more')[0].style.display!='none'") == Boolean.TRUE);
                    if (sc.isErrorAnotherOneRequired()) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_MORE_REQUIRED();
                        return true;
                    }
                    sc.setErrorDynamicTileMore(phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-error-dynamic-more')[0].style.display!='none'") == Boolean.TRUE);
                    if (sc.isErrorDynamicTileMore()) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_TILE_MORE();
                        return true;
                    }
                    sc.setErrorIncorrect(phantom.evalInPageContext("document.getElementsByClassName('rc-imageselect-incorrect-response')[0].style.display!='none'") == Boolean.TRUE);
                    if (sc.isErrorIncorrect()) {
                        reloadErrorMessage = _GUI.T.RECAPTCHA_2_VERIFICATION_ERROR_ANOTHER_CHALLENGE();
                        return true;
                    }
                    phantom.switchFrameToMain();
                    // mainHtml = (String) phantom.get("page.frameContent");
                    final String token = (String) phantom.evalInPageContext("document.getElementById('g-recaptcha-response').value");
                    if (StringUtils.isNotEmpty(token)) {
                        Recaptcha2FallbackChallengeViaPhantomJS.this.token = token;
                        HashMap<String, String> infos = new HashMap<String, String>();
                        infos.put("reloadCount", sc.getReloudCounter() + "");
                        StatsManager.I().track("solved", infos, CollectionName.PJS);
                        return true;
                    } else {
                        Recaptcha2FallbackChallengeViaPhantomJS.this.token = null;
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
                response.setValidation(ValidationResult.INVALID);
                killSession();
                return false;
            }
            if (sc.isErrorDynamicTileMore() || sc.isErrorAnotherOneRequired()) {
                response.setValidation(ValidationResult.INVALID);
            }
            if (sc.isErrorIncorrect()) {
                // if (sc.getChallengeType() != ChallengeType.DYNAMIC && getReloadCounter() > 0) {
                // response.setValidation(ValidationResult.INVALID);
                // }
                phantom.execute(" _global['verificationResponse']=" + verificationResponse + ";_global['verificationResponse']=_global['verificationResponse'][7];");
                Map<String, Object> initData = (Map<String, Object>) phantom.get(read("extractInitDataFromReloadResponse.js"));
                handleInitData(initData);
            }
            return true;
        } catch (Throwable e1) {
            this.logger.log(e1);
            trackException(e1);
            killSession();
            return false;
        } finally {
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
        SubChallenge sc = getSubChallenge();
        sc.reload();
        HashMap<String, String> infos = new HashMap<String, String>();
        infos.put("counter", sc.getReloudCounter() + "");
        StatsManager.I().track(100, "reloadCount", "reload", infos, CollectionName.PJS);
        getImageFile().delete();
        ImageIO.write(sc.paintImage(), "png", getImageFile());
    }
}