package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.ThrowingRunnable;

import org.appwork.utils.IO;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.jac.KeyCaptchaAutoSolver;
import org.jdownloader.images.NewTheme;
import org.jdownloader.scripting.JavaScriptEngineFactory;

public class KeyCaptcha {
    public static enum KeyCaptchaType {
        PUZZLE,
        CATEGORY;
    }

    static Object LOCK = new Object();

    public void prepareBrowser(final Browser kc, final String a) {
        kc.getHeaders().put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        kc.getHeaders().put("Referer", downloadUrl);
        kc.getHeaders().put("Pragma", null);
        kc.getHeaders().put("Cache-Control", null);
        kc.getHeaders().put("Accept", a);
        kc.getHeaders().put("Accept-Charset", null);
        kc.getHeaders().put("Accept-Language", "en-EN");
        kc.getHeaders().put("Cache-Control", null);
    }

    private final Browser                br;
    private Form                         FORM;
    private HashMap<String, String>      PARAMS;
    private Browser                      rcBr;
    private String                       SERVERSTRING;
    private String                       downloadUrl;
    private String[]                     stImgs;
    private String[]                     sscStc;
    private LinkedHashMap<String, int[]> fmsImg;
    private KeyCaptcha.KeyCaptchaType    type;
    private Plugin                       plugin;
    private DownloadLink                 link;
    private PuzzleData                   puzzleData;
    private CategoryData                 categoryData;
    private String                       capJs;
    private Object                       endParameter;
    static ReentrantLock                 LOCKDIALOG = new ReentrantLock();

    public KeyCaptcha(Plugin plg, final Browser br, DownloadLink link) {
        this.br = br;
        this.plugin = plg;
        this.link = link;
        downloadUrl = link.getPluginPatternMatcher();
    }

    private String getGjsParameter() {
        final String[] pars = { "s_s_c_user_id", "src", "s_s_c_session_id", "s_s_c_captcha_field_id", "s_s_c_submit_button_id", "s_s_c_web_server_sign", "s_s_c_web_server_sign2", "s_s_c_web_server_sign3", "s_s_c_web_server_sign4" };
        String result = "";
        for (final String key : pars) {
            result = result != "" ? result + "|" : result;
            if (PARAMS.containsKey(key)) {
                result += PARAMS.get(key);
            }
        }
        return result;
    }

    private String getAdditionalQuery(String query) {
        query = Encoding.htmlDecode(query).replaceAll("[\" ]", "");
        String js = rcBr.toString().replaceAll("[\n\r]+", "");
        String result = "|";
        for (String s : query.split("\\|")) {
            String t;
            if (s.length() > 1) {
                if (s.endsWith("()")) {
                    String regex = "function\\s+" + s.replace("(", "\\(\\s*").replace(")", "\\)") + "\\s*\\{\\s*(.*?)\\s*\\}\\s*;";
                    t = new Regex(js, regex).getMatch(0);
                } else {
                    t = new Regex(js, s + "=\\s+(.*?)\\s+;").getMatch(0);
                }
                if (t == null) {
                    result += "1";
                    continue;
                }
                String ret = new Regex(t, "\"([0-9a-f]+)\"").getMatch(0);
                if (ret == null) {
                    result += "error";
                }
                result += "|" + ret;
            }
        }
        return result;
    }

    private String getCapsUrl(String string) throws ScriptException {
        final ScriptEngine engine = getScriptEngine();
        /* creating pseudo functions: document.location */
        engine.eval("var document = { loc : function() { return \"" + downloadUrl + "\";}}");
        engine.eval("document.location = document.loc();");
        engine.put("s_s_c_user_id", PARAMS.get("s_s_c_user_id"));
        engine.eval(string);
        return engine.get("_13").toString();
    }

    private void load() throws Exception {
        rcBr = br.cloneBrowser();
        rcBr.setFollowRedirects(true);
        prepareBrowser(rcBr, "application/javascript, */*;q=0.8");
        String base = null;
        String capsUrl = null;
        if (PARAMS.containsKey("src")) {
            rcBr.setFollowRedirects(false);
            rcBr.getPage(PARAMS.get("src"));
            String redirect;
            while ((redirect = rcBr.getRedirectLocation()) != null) {
                base = new Regex(redirect, "(http.+/)swfs/").getMatch(0);
                rcBr.getHeaders().put("Referer", downloadUrl);
                rcBr.addAllowedResponseCodes(502);
                rcBr.getPage(redirect);
                if (rcBr.getHttpConnection().getResponseCode() == 502) {
                    continue;
                }
            }
            capsUrl = getCapsUrl(rcBr.getRegex("(var _13=[^;]+;)").getMatch(0));
            rcBr.getHeaders().put("Referer", downloadUrl);
            rcBr.getPage(base + "swfs/session.html?r=" + Math.random());
            Form form = rcBr.getFormbyKey("a");
            if (form != null) {
                rcBr.submitForm(form);
            }
            PARAMS.put("src", downloadUrl);
        }
        rcBr.getHeaders().put("Referer", downloadUrl);
        rcBr.getPage(capsUrl);
        rcBr.getHeaders().put("Referer", downloadUrl);
        rcBr.cloneBrowser().getPage(base + "js/keycaptcha-logo?r=" + Math.random());
        rcBr.getHeaders().put("Referer", downloadUrl);
        rcBr.cloneBrowser().getPage(base + "swfs/ckf");
        // rcBr.getPage(SERVERSTRING.replaceAll("/, replacement));
        this.capJs = rcBr.toString();
        SERVERSTRING = null;
        PARAMS.put("s_s_c_web_server_sign4", rcBr.getRegex("s_s_c_web_server_sign4\\s*=\\s*\"(.*?)\"").getMatch(0));
        String[] next = rcBr.getRegex("\\.setAttribute\\(\\s*\"src\"\\s*,\\s*\"(.*?)\"\\s*\\+\\s*(.*?)\\s*\\+").getRow(0);
        if (next == null) {
            throw new Exception("KeyCaptcha Module fails");
        }
        String gjsUrl = next[0];
        // var _34 = s_s_c_user_id + "|" + window.location.toString().split("#")[0] + "|" + s_s_c_session_id + "|" +
        // s_s_c_captcha_field_id + "|" + s_s_c_submit_button_id + "|" + s_s_c_web_server_sign + "|" + s_s_c_web_server_sign2 + "|" +
        // s_s_c_web_server_sign3 + "|" + s_s_c_web_server_sign4 + "|" + d719880867366dcd255394ad4fe3bec1() + "|" +
        // d719880867366dcd255394ad4fe3befs() + "|" + document.dn3iufwwoi4;
        String reg = "var\\s+" + next[1] + "\\s*=.*?s_s_c_web_server_sign4\\s*\\+\\s*(.*?);";
        String q = rcBr.getRegex(reg).getMatch(0);
        String endParameterName = new Regex(q, "(document\\.[\\w\\d]+)$").getMatch(0);
        endParameter = rcBr.getRegex(endParameterName.replace(".", "\\.") + "\\s*=\\s*\"([^\"]+)").getMatch(0);
        // additionalQuery = getAdditionalQuery(q);
        // String ads = getAdditionalQuery(true);
        // String ads2 = getAdditionalQuery(false);
        // System.out.println(ads2);
        rcBr.getHeaders().put("Referer", downloadUrl);
        gjsUrl = gjsUrl + Encoding.urlEncode(getGjsParameter() + getAdditionalQuery(false) + "|" + endParameter) + "&r=" + Math.random() + "&sr=1920.1080";
        rcBr.getPage(gjsUrl);
        rcBr.getHeaders().put("Referer", downloadUrl);
        rcBr.cloneBrowser().getPage(base + "swfs/ckf");
        // rcBr.getRequest().setHtmlCode(IO.readURLToString(getClass().getResource("LnkCrptWs.java.js")));
        // additionalQuery = additionalQuery.substring(0, additionalQuery.lastIndexOf("|"));
        PARAMS.put("s_s_c_web_server_sign3", rcBr.getRegex("s_s_c_setnewws\\(\"(.*?)\",").getMatch(0));
        String categoryImagesList = rcBr.getRegex("var imgs\\s*=\\s*new Array\\s*\\(\\s*(.+?)\\s*\\)").getMatch(0);
        if (categoryImagesList != null) {
            type = KeyCaptchaType.CATEGORY;
        } else {
            type = KeyCaptchaType.PUZZLE;
            stImgs = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(0);
            sscStc = rcBr.getRegex("\\(\'([0-9a-f]+)\',\'(http.*?\\.png)\',(.*?),(true|false)\\)").getRow(1);
        }
        String signFour = PARAMS.get("s_s_c_web_server_sign4");
        if (signFour.length() < 33) {
            // signFour = signFour.substring(0, 10) + "378" + signFour.substring(10);
            signFour = signFour.substring(0, 10) + "310" + signFour.substring(10);
            PARAMS.put("s_s_c_web_server_sign4", signFour);
        }
        if (type == KeyCaptchaType.CATEGORY) {
            String categoriesUrl = rcBr.getRegex("cnv\\.innerHTML\\s*=\\s*\\'<img\\s+style=\"background\\:none\\;\"\\s+src=\"(http[^\"]+\\.png)").getMatch(0);
            Browser picLoad = rcBr.cloneBrowser();
            prepareBrowser(picLoad, "image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5");
            CategoryData data = new CategoryData();
            // ArrayList<String> requests = new ArrayList<String>();
            data.setBackground(readImage(categoriesUrl, picLoad));
            // if (categoryImage != null) {
            //
            // imagesList.add(categoryImage);
            // requests.add(picLoad.getRequest() + "");
            // }
            String[] images = new Regex(categoryImagesList, "'(http[^']+)").getColumn(0);
            for (String im : images) {
                prepareBrowser(picLoad, "image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5");
                data.addImage(readImage(im, picLoad));
            }
            String resultUrl = rcBr.getRegex("resscript\\.setAttribute\\('src'\\s*\\,\\s*'([^\']+)").getMatch(0);
            data.setResultUrl(resultUrl);
            // SERVERSTRING += Encoding.urlEncode(getGjsParameter() + additionalQuery);
            final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + "Khd21M47");
            String mmUrlReq = base + "swfs/mm?pS=" + pS + "&cP=" + getGjsParameter() + getAdditionalQuery(false);
            mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
            // KeyCaptchaImageGetter imgGetter = new KeyCaptchaImageGetter(new String[] { stImgs[1], sscStc[1] }, fmsImg, rcBr,
            // mmUrlReq);
            //
            // KeyCaptchaSolver kcSolver = new KeyCaptchaSolver();
            // seems like timing is important
            Thread.sleep(1000);
            // is sent on first touch of an image
            rcBr.cloneBrowser().getPage(mmUrlReq);
            Thread.sleep(3000);
            // is sent 5112 ms afterwards
            String dh38 = base + "swfs/dh38?pS=" + sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + "KdEfOMM");
            dh38 += "&cP=" + getGjsParameter() + getAdditionalQuery(false);
            dh38 += "&mms=" + Math.random() + "&r=" + Math.random();
            rcBr.cloneBrowser().getPage(dh38);
            this.categoryData = data;
            //
            // KeyCaptchaCategoryChallenge challenge = new KeyCaptchaCategoryChallenge(imagesList, categoryImage);
            // ArrayList<Integer> marray = new ArrayList<Integer>();
            // marray.add(1);
            // marray.add(1);
            // marray.add(1);
            // marray.add(1);
            // marray.add(1);
            // marray.add(1);
            // marray.add(1);
            // String cOut = "";
            // for (Integer i : marray) {
            // if (cOut.length() > 1) {
            // cOut += ".";
            // }
            // cOut += String.valueOf(i);
            // }
            //
            // String out;
            // // SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
            // resultUrl += "&cOut=" + cOut + "&cP=" + getGjsParameter();
            // rcBr.clearCookies(rcBr.getHost());
            // out = rcBr.getPage(resultUrl);
            // out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
            // // validate final response
            // if
            // (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)"))
            // {
            // return;
            // }
            // throw new Exception("KeyCaptcha Module fails: Category Type not supported");
        } else {
            SERVERSTRING = rcBr.getRegex("\\.s_s_c_resurl=\'([^\']+)\'\\+").getMatch(0);
            SERVERSTRING += Encoding.urlEncode(getGjsParameter() + getAdditionalQuery(false));
            if (stImgs == null || sscStc == null || SERVERSTRING == null) {
                throw new Exception("KeyCaptcha Module fails");
            }
            //
            /* Bilderdownload und Verarbeitung */
            sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));// fragmentierte Puzzleteile
            sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));// fragmentiertes Hintergrundbild
            if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) {
                throw new PluginException(LinkStatus.ERROR_FATAL);
            }
            // String out = null;
            // ArrayList<Integer> marray = new ArrayList<Integer>();
            Thread.sleep(1000);
            final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + Encoding.Base64Decode("S2hkMjFNNDc="));
            String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
            mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
            rcBr.cloneBrowser().getPage(mmUrlReq);
            KeyCaptchaImageGetter imgGetter = new KeyCaptchaImageGetter(this, new String[] { stImgs[1], sscStc[1] }, fmsImg, rcBr, mmUrlReq);
            // KeyCaptchaAutoSolver kcSolver = new KeyCaptchaAutoSolver();
            KeyCaptchaImages imgs = imgGetter.getKeyCaptchaImage();
            // rcBr.cloneBrowser().getPage(mmUrlReq);
            puzzleData = new PuzzleData(fmsImg, imgs, mmUrlReq);
        }
    }

    public static class ScriptEnv {
        private ScriptEngine engine;

        public ScriptEnv(ScriptEngine engine) {
            this.engine = engine;
        }

        public void log(String log) {
            System.out.println(log);
        }

        public void eval(String eval) throws ScriptException {
            engine.eval(eval);
        }

        public String atob(String string) {
            String ret = Encoding.Base64Decode(string);
            return ret;
        }
    }

    private ScriptEngine getScriptEngine() {
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        return engine;
    }

    private String evalGHS(String arg0, String arg1) throws Exception {
        try {
            final ScriptEngine engine = getScriptEngine();
            JavaScriptEngineFactory.runTrusted(new ThrowingRunnable<ScriptException>() {
                @Override
                public void run() throws ScriptException {
                    ScriptEnv env = new ScriptEnv(engine);
                    // atob requires String to be loaded for its parameter and return type
                    engine.put("env", env);
                    engine.eval("var string=" + String.class.getName() + ";");
                    engine.eval("log=function(str){return env.log(str);};");
                    engine.eval("alert=function(str){return env.log(str);};");
                    engine.eval("eval=function(str){return env.eval(str);};");
                    engine.eval("atob=function(str){return env.atob(str);};");
                    // cleanup
                    engine.eval("delete java;");
                    engine.eval("delete jd;");
                    // load Env in Trusted Thread
                    engine.eval("log('Java Env Loaded');");
                }
            });
            String env = IO.readURLToString(getClass().getResource("env.js"));
            engine.eval(env);
            engine.eval(capJs);
            /* creating pseudo functions: document.location */
            engine.eval(rcBr.toString());
            String onLoadMethod = new Regex(capJs, "f\\.onload\\s*=\\s*function\\(\\)\\s*\\{\\s*([\\w\\d]+)\\(\\)").getMatch(0);
            engine.eval(onLoadMethod + "();");
            engine.put("arg1", arg0);
            engine.put("arg2", arg1);
            String ret;
            String signMethod = rcBr.getRegex("(\\w+\\.\\w+)\\(s_s_c_web_server_sign\\,\\s*s_s_c_web_server_sign").getMatch(0);
            ret = engine.eval(signMethod + "(arg1,arg2);").toString();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
            throw e;
        }
    }

    private BufferedImage readImage(String categoriesUrl, Browser picLoad) throws IOException {
        BufferedImage ret = ImageIO.read(picLoad.openGetConnection(categoriesUrl).getInputStream());
        if (ret == null) {
            ret = IconIO.toBufferedImage(NewTheme.I().getImage("help", 32));
        }
        return ret;
    }

    private String getAdditionalQuery(boolean inProcess) throws Exception {
        String[] next = new Regex(capJs, "\\.setAttribute\\(\\s*\"src\"\\s*,\\s*\"(.*?)\"\\s*\\+\\s*(.*?)\\s*\\+").getRow(0);
        if (next == null) {
            throw new Exception("KeyCaptcha Module fails");
        }
        String reg = "var\\s+" + next[1] + "\\s*=.*?s_s_c_web_server_sign4\\s*\\+\\s*(.*?);";
        String q = new Regex(capJs, reg).getMatch(0);
        String[] methods = new Regex(q, "([\\w\\d]+)\\(\\).*?([\\w\\d]+)\\(\\)").getRow(0);
        final ScriptEngine engine = getScriptEngine();
        String env = "s_s_c_get_form=function(){return null;};s_s_c_captcha_field_id=\"\";document={};" + "document.getElementById=function(){var obj={};obj.s_s_c_check_process=" + inProcess + ";return obj;};" + "document.s_s_c_popupmode=false;" + "document.s_s_c_do_not_auto_show=true;";
        engine.eval(env);
        /* creating pseudo functions: document.location */
        engine.eval(capJs);
        Object a = engine.eval(methods[0] + "();");
        if (a instanceof Number) {
            a = ((Number) a).intValue() + "";
        }
        String b = engine.eval(methods[1] + "();").toString();
        return "|" + a + "|" + b;
    }

    public Browser getBrowser() {
        return rcBr;
    }

    public PuzzleData getPuzzleData() {
        return puzzleData;
    }

    public CategoryData getCategoryData() {
        return categoryData;
    }

    private void parse() throws Exception {
        FORM = null;
        if (br.containsHTML("(KeyCAPTCHA|www\\.keycaptcha\\.com)")) {
            for (final Form f : br.getForms()) {
                if (f.containsHTML("var s_s_c_user_id = ('\\d+'|\"\\d+\")")) {
                    FORM = f;
                    break;
                }
            }
            if (FORM == null) {
                String st = br.getRegex("(<script type=\'text/javascript\'>var s_s_c_.*?\'></script>)").getMatch(0);
                if (st != null) {
                    Browser f = br.cloneBrowser();
                    FORM = new Form();
                    f.getRequest().setHtmlCode("<form>" + st + "</form>");
                    FORM = f.getForm(0);
                }
            }
            if (FORM == null) {
                throw new Exception("KeyCaptcha form couldn't be found");
            } else {
                PARAMS = new HashMap<String, String>();
                String[][] parameter = FORM.getRegex("(s_s_c_\\w+) = \'(.*?)\'").getMatches();
                if (parameter == null || parameter.length == 0) {
                    parameter = FORM.getRegex("(s_s_c_\\w+) = \"(.*?)\"").getMatches();
                }
                for (final String[] para : parameter) {
                    if (para.length != 2) {
                        continue;
                    }
                    PARAMS.put(para[0], para[1]);
                }
                if (PARAMS == null || PARAMS.size() == 0) {
                    throw new Exception("KeyCaptcha values couldn't be found");
                } else {
                    String src = FORM.getRegex("src=\'([^']+keycaptcha\\.com[^']+)\'").getMatch(0);
                    if (src == null) {
                        src = FORM.getRegex("src=\"([^\"]+keycaptcha\\.com[^\"]+)\"").getMatch(0);
                        if (src == null) {
                            throw new Exception("KeyCaptcha Module fails");
                        }
                    }
                    PARAMS.put("src", src);
                }
            }
        } else {
            throw new Exception("KeyCaptcha handling couldn't be found");
        }
    }

    /**
     * Handles a KeyCaptcha by trying to autosolve it first and use a dialog as fallback
     *
     * @param parameter
     *            the keycaptcha parameter as already used for showDialog
     * @param downloadLink
     *            downloadlink (for counting attempts)
     * @return
     * @throws Exception
     */
    // public String handleKeyCaptcha(final String parameter, DownloadLink downloadLink) throws Exception {
    // int attempt = downloadLink.getIntegerProperty("KEYCAPTCHA_ATTEMPT", 0);
    // downloadLink.setProperty("KEYCAPTCHA_ATTEMPT", attempt + 1);
    // if (attempt < 2) {
    // // less than x attempts -> try autosolve
    // return autoSolve(parameter);
    // } else {
    // // shows the dialog
    // return showDialog(parameter);
    // }
    // }
    /**
     * This methods just displays a dialog. You can use {@link #handleKeyCaptcha(String, DownloadLink) handleKeyCaptcha} instead, which
     * tries to autosolve it first. Or you can use {@link #autoSolve(String) autosolve}, which tries to solve the captcha directly.
     */
    // public synchronized String showDialog(final String parameter) throws Exception {
    // LOCKDIALOG.lock();
    // try {
    // downloadUrl = parameter;
    // try {
    // parse();
    // load();
    // } catch (final Throwable e) {
    // e.printStackTrace();
    // throw new Exception(e.getMessage());
    // } finally {
    // try {
    // rcBr.getHttpConnection().disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    //
    // /* Bilderdownload und Verarbeitung */
    // sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));// fragmentierte Puzzleteile
    // sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));// fragmentiertes Hintergrundbild
    //
    // if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) {
    // return "CANCEL";
    // }
    //
    // String out = null;
    // ArrayList<Integer> marray = new ArrayList<Integer>();
    //
    // final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") +
    // Encoding.Base64Decode("S2hkMjFNNDc="));
    // String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
    // mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
    //
    // final KeyCaptchaDialog vC = new KeyCaptchaDialog(0, "KeyCaptcha - " + br.getHost(), new String[] { stImgs[1], sscStc[1] }, fmsImg,
    // null, rcBr, mmUrlReq);
    //
    // // avoid imports here
    // jd.gui.swing.dialog.AbstractCaptchaDialog.playCaptchaSound();
    // try {
    // out = org.appwork.utils.swing.dialog.Dialog.getInstance().showDialog(vC);
    // } catch (final Throwable e) {
    // out = null;
    // }
    // if (out == null) {
    // throw new DecrypterException(DecrypterException.CAPTCHA);
    // }
    // if (vC.getReturnmask() == 4) {
    // out = "CANCEL";
    // }
    // marray.addAll(vC.mouseArray);
    //
    // if (out == null) {
    // return null;
    // }
    // if ("CANCEL".equals(out)) {
    // System.out.println("KeyCaptcha: User aborted captcha dialog.");
    // return out;
    // }
    //
    // String key = rcBr.getRegex("\\|([0-9a-zA-Z]+)\'\\.split").getMatch(0);
    // if (key == null) {
    // key = Encoding.Base64Decode("OTNodk9FZmhNZGU=");
    // }
    //
    // String cOut = "";
    // for (Integer i : marray) {
    // if (cOut.length() > 1) {
    // cOut += ".";
    // }
    // cOut += String.valueOf(i);
    // }
    //
    // SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
    // rcBr.clearCookies(rcBr.getHost());
    // out = rcBr.getPage(SERVERSTRING.substring(0, SERVERSTRING.lastIndexOf("%7C")));
    // out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
    // // validate final response
    // if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
    // return null;
    // }
    // return out;
    // } finally {
    // LOCKDIALOG.unlock();
    // }
    // }
    private String sscFsmCheckFour(String arg0, final String arg1) {
        try {
            if (arg0 == null || arg0.length() < 8 || arg1 == null) {
                return null;
            }
            String prand = "";
            for (int i = 0; i < arg1.length(); i++) {
                prand += arg1.codePointAt(i);
            }
            final int sPos = (int) Math.floor(prand.length() / 5);
            final int mult = Integer.parseInt(String.valueOf(prand.charAt(sPos) + "" + prand.charAt(sPos * 2) + "" + prand.charAt(sPos * 3) + "" + prand.charAt(sPos * 4) + "" + prand.charAt(sPos * 5 - 1)));
            final int incr = Math.round(arg1.length() / 3);
            final long modu = (int) Math.pow(2, 31);
            final int salt = Integer.parseInt(arg0.substring(arg0.length() - 8, arg0.length()), 16);
            arg0 = arg0.substring(0, arg0.length() - 8);
            prand += salt;
            while (prand.length() > 9) {
                prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
            }
            final String[] sburl = "https://back2.keycaptcha.com".split("\\.");
            if (sburl != null && sburl.length == 3 && "keycaptcha".equalsIgnoreCase(sburl[1]) && "com".equalsIgnoreCase(sburl[2])) {
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
            } else {
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu + 294710);
            }
            int enc_chr = 0;
            String enc_str = "";
            for (int i = 0; i < arg0.length(); i += 2) {
                enc_chr = Integer.parseInt(arg0.substring(i, i + 2), 16) ^ (int) Math.floor(Double.parseDouble(prand) / modu * 255);
                enc_str += String.valueOf((char) enc_chr);
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
            }
            return enc_str;
        } catch (final Throwable e) {
            return null;
        }
    }

    private String sscFsmCheckTwo(final String arg0, final String arg1) {
        try {
            // String org = evalGHS(arg0, arg1);
            if (arg1 == null) {
                return null;
            }
            String prand = "";
            for (int i = 0; i < arg1.length(); i++) {
                prand += arg1.codePointAt(i);
            }
            final int sPos = (int) Math.floor(prand.length() / 5);
            final int mult = Integer.parseInt(String.valueOf(prand.charAt(sPos) + "" + prand.charAt(sPos * 2) + "" + prand.charAt(sPos * 3) + "" + prand.charAt(sPos * 4) + "" + prand.charAt(sPos * 5 - 1)));
            final int incr = (int) Math.ceil(arg1.length() / 3d);
            final long modu = (int) Math.pow(2, 31);
            if (mult < 2) {
                return null;
            }
            int salt = (int) Math.round(Math.random() * 1000000000) % 100000000;
            prand += salt;
            while (prand.length() > 9) {
                prand = String.valueOf(Integer.parseInt(prand.substring(0, 9), 10) + Integer.parseInt(prand.substring(9, Math.min(prand.length(), 14)), 10)) + prand.substring(Math.min(prand.length(), 14), prand.length());
            }
            final String[] sburl = "https://back2.keycaptcha.com".split("\\.");
            if (sburl != null && sburl.length == 3 && "keycaptcha".equalsIgnoreCase(sburl[1]) && "com".equalsIgnoreCase(sburl[2])) {
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
            } else {
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu + 540);
            }
            int enc_chr = 0;
            String enc_str = "";
            for (int i = 0; i < arg0.length(); i++) {
                enc_chr = arg0.codePointAt(i) ^ (int) Math.floor(Double.parseDouble(prand) / modu * 255);
                if (enc_chr < 16) {
                    enc_str += "0" + String.valueOf(Integer.toHexString(enc_chr));
                } else {
                    enc_str += String.valueOf(Integer.toHexString(enc_chr));
                }
                prand = String.valueOf((mult * Long.parseLong(prand) + incr) % modu);
            }
            String saltStr = String.valueOf(Integer.toHexString(salt));
            while (saltStr.length() < 8) {
                saltStr = "0" + saltStr;
            }
            return enc_str + saltStr;
        } catch (final Throwable e) {
            return null;
        }
    }

    private void sscGetImagest(final String arg0, final String arg1, final String arg2, final boolean arg4) {
        final String outst = sscFsmCheckFour(arg0, arg1.substring(arg1.length() - 33, arg1.length() - 6));
        String[] parseOutst;
        int[] pOut = null;
        if (arg4) {
            parseOutst = outst.split(";");
        } else {
            parseOutst = outst.split(",");
            pOut = new int[parseOutst.length];
            for (int i = 0; i < pOut.length; i++) {
                pOut[i] = Integer.parseInt(parseOutst[i]);
            }
        }
        if (parseOutst != null && parseOutst.length > 0) {
            if (fmsImg == null || fmsImg.size() == 0) {
                fmsImg = new LinkedHashMap<String, int[]>();
            }
            if (arg4) {
                for (final String pO : parseOutst) {
                    final String[] tmp = pO.split(":");
                    if (tmp == null || tmp.length == 0) {
                        continue;
                    }
                    final String[] tmpOut = tmp[1].split(",");
                    pOut = new int[tmpOut.length];
                    for (int i = 0; i < pOut.length; i++) {
                        pOut[i] = Integer.parseInt(tmpOut[i]);
                    }
                    fmsImg.put(tmp[0], pOut);
                }
            } else {
                fmsImg.put("backGroundImage", pOut);
            }
        }
    }

    // ===== BEGIN autosolve stuff
    public String autoSolve(final String parameter) throws Exception {
        downloadUrl = parameter;
        try {
            parse();
            load();
        } catch (final Throwable e) {
            throw new Exception(e);
        } finally {
            try {
                rcBr.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        /* Bilderdownload und Verarbeitung */
        sscGetImagest(stImgs[0], stImgs[1], stImgs[2], Boolean.parseBoolean(stImgs[3]));// fragmentierte Puzzleteile
        sscGetImagest(sscStc[0], sscStc[1], sscStc[2], Boolean.parseBoolean(sscStc[3]));// fragmentiertes Hintergrundbild
        if (sscStc == null || sscStc.length == 0 || stImgs == null || stImgs.length == 0 || fmsImg == null || fmsImg.size() == 0) {
            return "CANCEL";
        }
        String out = null;
        ArrayList<Integer> marray = new ArrayList<Integer>();
        final String pS = sscFsmCheckTwo(PARAMS.get("s_s_c_web_server_sign"), PARAMS.get("s_s_c_web_server_sign") + Encoding.Base64Decode("S2hkMjFNNDc="));
        String mmUrlReq = SERVERSTRING.replaceAll("cjs\\?pS=\\d+&cOut", "mm\\?pS=" + pS + "&cP");
        mmUrlReq = mmUrlReq + "&mms=" + Math.random() + "&r=" + Math.random();
        KeyCaptchaImageGetter imgGetter = new KeyCaptchaImageGetter(this, new String[] { stImgs[1], sscStc[1] }, fmsImg, rcBr, mmUrlReq);
        KeyCaptchaAutoSolver kcSolver = new KeyCaptchaAutoSolver();
        rcBr.cloneBrowser().getPage(mmUrlReq);
        out = kcSolver.solve(imgGetter.getKeyCaptchaImage());
        marray.addAll(kcSolver.getMouseArray());
        if (out == null) {
            return null;
        }
        if ("CANCEL".equals(out)) {
            System.out.println("KeyCaptcha: User aborted captcha dialog.");
            return out;
        }
        String key = rcBr.getRegex("\\|([0-9a-zA-Z]+)\'\\.split").getMatch(0);
        if (key == null) {
            key = Encoding.Base64Decode("OTNodk9FZmhNZGU=");
        }
        String cOut = "";
        for (Integer i : marray) {
            if (cOut.length() > 1) {
                cOut += ".";
            }
            cOut += String.valueOf(i);
        }
        SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
        rcBr.clearCookies(rcBr.getHost());
        out = rcBr.getPage(SERVERSTRING.substring(0, SERVERSTRING.lastIndexOf("%7C")));
        out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
        // validate final response
        if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
            return null;
        }
        return out;
    }

    public Challenge<String> createChallenge(boolean noAutoSolver, Plugin plg) throws Exception {
        try {
            parse();
            load();
        } catch (final Throwable e) {
            throw new Exception(e);
        } finally {
            try {
                rcBr.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        if (type == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        switch (type) {
        case CATEGORY:
            return new KeyCaptchaCategoryChallenge(this, plg, noAutoSolver);
        case PUZZLE:
            return new KeyCaptchaPuzzleChallenge(this, plg, noAutoSolver);
        }
        return null;
    }

    public Challenge<String> createChallenge(Plugin plugin) throws Exception {
        return createChallenge(false, plugin);
    }

    public String sendPuzzleResult(ArrayList<Integer> marray, String out) throws IOException {
        String key = rcBr.getRegex("\\|([0-9a-zA-Z]+)\'\\.split").getMatch(0);
        if (key == null) {
            key = Encoding.Base64Decode("OTNodk9FZmhNZGU=");
        }
        if (marray == null || marray.size() == 0) {
            marray = new ArrayList<Integer>();
            String[] points = out.split("\\.");
            if (points.length % 2 == 0) {
                for (int i = 0; i < points.length; i += 2) {
                    int x = Integer.valueOf(points[i]);
                    int y = Integer.valueOf(points[i + 1]);
                    Point position = new Point(x, y);
                    Point randomPoint = new Point();
                    randomPoint.setLocation(position.x * Math.random(), position.y * Math.random());
                    marray(marray, randomPoint);
                    marray(marray, position);
                }
            }
        }
        String cOut = "";
        for (Integer i : marray) {
            if (cOut.length() > 1) {
                cOut += ".";
            }
            cOut += String.valueOf(i);
        }
        SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
        rcBr.clearCookies(rcBr.getHost());
        out = rcBr.getPage(SERVERSTRING.substring(0, SERVERSTRING.lastIndexOf("%7C")));
        out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
        // validate final response
        if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
            return null;
        }
        return out;
    }

    private void marray(ArrayList<Integer> mouseArray, Point loc) {
        if (loc != null) {
            if (mouseArray.size() == 0) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.get(mouseArray.size() - 2) != loc.x + 465 || mouseArray.get(mouseArray.size() - 1) != loc.y + 264) {
                mouseArray.add(loc.x + 465);
                mouseArray.add(loc.y + 264);
            }
            if (mouseArray.size() > 40) {
                ArrayList<Integer> tmpMouseArray = new ArrayList<Integer>();
                tmpMouseArray.addAll(mouseArray.subList(2, 40));
                mouseArray.clear();
                mouseArray.addAll(tmpMouseArray);
            }
        }
    }

    public void sendOnMousePressFeedback() {
    }

    public String sendCategoryResult(String cOut) throws Exception {
        String out;
        String resultUrl = categoryData.getResultUrl();
        // SERVERSTRING = SERVERSTRING.replace("cOut=", "cOut=" + sscFsmCheckTwo(out, key) + "..." + cOut + "&cP=");
        resultUrl += cOut + "&cP=" + Encoding.urlEncode(getGjsParameter() + getAdditionalQuery(true));
        // rcBr.clearCookies(rcBr.getHost());
        prepareBrowser(rcBr, null);
        out = rcBr.getPage(resultUrl);
        out = new Regex(out, "s_s_c_setcapvalue\\( \"(.*?)\" \\)").getMatch(0);
        // validate final response
        // setTimeout( function () { s_s_c_setcapvalue(
        // "32eb7dc714397e5173d2903bee1bddde|f196f739ec0ec7ed3c50397390487d66|http://back15.keycaptcha.com/swfs/ckc/bb3edba9f10c1cc04a9c5cedbbe221f2-|55ba196c8fd5d-3.4.0.001|1"
        // ); }, 800 );
        if (!out.matches("[0-9a-f]+\\|[0-9a-f]+\\|http://back\\d+\\.keycaptcha\\.com/swfs/ckc/[0-9a-f-]+\\|[0-9a-f-\\.]+\\|(0|1)")) {
            return null;
        }
        return out;
    }
}