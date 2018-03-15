package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jd.captcha.utils.RecaptchaTypeTester;
import jd.captcha.utils.RecaptchaTypeTester.RecaptchaType;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents;

import org.appwork.utils.StringUtils;

public class Recaptcha {
    private static final int MAX_TRIES    = 5;
    private final Browser    br;
    private String           challenge;
    private String           server;
    private String           captchaAddress;
    private String           id;
    private Browser          rcBr;
    private Form             form;
    private int              tries        = 0;
    private boolean          clearReferer = true;
    private String           helperID;
    private final Plugin     plg;

    public Recaptcha(final Browser br, Plugin plg) {
        this.br = br;
        this.plg = plg;
        track("challenge/");
    }

    public File downloadCaptcha(final File captchaFile) throws IOException, PluginException {
        /* follow redirect needed as google redirects to another domain */
        if (this.getTries() > 0) {
            this.reload();
        }
        // this.rcBr could be null at this stage, if we are specifying challenge id and image ourselves.
        prepRcBr();
        this.rcBr.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            track("download/" + helperID);
            Browser.download(captchaFile, con = this.rcBr.openGetConnection(this.captchaAddress));
            FileInputStream is = null;
            try {
                is = new FileInputStream(captchaFile);
                RecaptchaType type = RecaptchaTypeTester.getType(captchaFile);
                track("imagetype/" + type + "/" + helperID);
            } catch (IOException e) {
                track("imagetype/" + e.getMessage() + "/" + helperID);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (final IOException e) {
            captchaFile.delete();
            throw e;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return captchaFile;
    }

    public void findID() throws PluginException {
        this.id = this.br.getRegex("(challenge|noscript|fallback)\\?k=([A-Za-z0-9%_\\+\\- ]+)(?:\"|\\&)").getMatch(1);
        if (this.id == null) {
            this.id = this.br.getRegex("Recaptcha\\.create\\((\"|\\')([A-Za-z0-9%_\\+\\- ]+)\\1").getMatch(1);
        }
        if (this.id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public String getCaptchaAddress() {
        return this.captchaAddress;
    }

    public String getChallenge() {
        return this.challenge;
    }

    public Form getForm() {
        return this.form;
    }

    public String getId() {
        return this.id;
    }

    public String getServer() {
        return this.server;
    }

    public int getTries() {
        return this.tries;
    }

    public boolean isSolved() throws PluginException {
        if (this.tries > Recaptcha.MAX_TRIES) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        try {
            this.parse();
            this.findID();
            // recaptcha still found. so it is not solved yet
            return false;
        } catch (final Exception e) {
            if (plg != null) {
                plg.getLogger().log(e);
            } else {
                e.printStackTrace();
            }
            return true;
        }
    }

    private void prepRcBr() {
        // only run if this.rcBr == null
        if (this.rcBr == null) {
            this.rcBr = this.br.cloneBrowser();
            // recaptcha works off API key, and javascript. The imported browser session isn't actually needed.
            /*
             * Randomise user-agent to prevent tracking by google, each time we load(). Without this they could make the captchas images
             * harder read, the more a user requests captcha'. Also algos could track captcha requests based on user-agent globally, which
             * means JD default user-agent been very old (firefox 3.x) negatively biased to JD clients! Tracking takes place on based on IP
             * address, User-Agent, and APIKey of request (site of APIKey), cookies session submitted, and combinations of those.
             * Effectively this can all be done with a new browser, with regex tasks from source browser (ids|keys|submitting forms).
             */
            /* we first have to load the plugin, before we can reference it */
            this.rcBr.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
            // this prevents google/recaptcha group from seeing referrer
            if (this.clearReferer) {
                this.rcBr.setRequest(null);
            }
            try {
                org.jdownloader.captcha.v2.solver.service.BrowserSolverService.fillCookies(rcBr);
                if (rcBr.getCookie("http://google.com", "SID") != null) {
                    if (StringUtils.isNotEmpty(org.jdownloader.captcha.v2.solver.service.BrowserSolverService.getInstance().getConfig().getGoogleComCookieValueSID()) && StringUtils.isNotEmpty(org.jdownloader.captcha.v2.solver.service.BrowserSolverService.getInstance().getConfig().getGoogleComCookieValueHSID())) {
                        helperID = "SID";
                    } else {
                        helperID = "ACC";
                    }
                }
            } catch (Throwable e) {
                if (plg != null) {
                    plg.getLogger().log(e);
                } else {
                    e.printStackTrace();
                }
            }
            // end of privacy protection
        }
    }

    private final boolean isSupported(final String url) {
        return url != null && !StringUtils.containsIgnoreCase(url, "v1_unsupported.png");
    }

    public void load() throws IOException, PluginException {
        runDdosProtection();
        prepRcBr();
        String challenge = null;
        try {
            challenge = org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1Handler.load(rcBr, id);
            if (isSupported(challenge)) {
                setChallenge(challenge);
                helperID = "BrowserLoop";
                server = "https://www.google.com/recaptcha/api/";
                this.captchaAddress = this.server + "image?c=" + getChallenge();
            }
        } catch (Throwable e) {
            if (plg != null) {
                plg.getLogger().log(e);
            } else {
                e.printStackTrace();
            }
        }
        if (!isSupported(challenge)) {
            /* follow redirect needed as google redirects to another domain */
            this.rcBr.setFollowRedirects(true);
            // new primary. 20141211
            this.rcBr.getPage("https://www.google.com/recaptcha/api/challenge?k=" + this.id);
            // old
            // this.rcBr.getPage("http://api.recaptcha.net/challenge?k=" + this.id);
            challenge = this.rcBr.getRegex("challenge\\s*:\\s*'([^']*)'").getMatch(0);
            this.server = this.rcBr.getRegex("server\\s*:\\s*'(https?://[^']*)'").getMatch(0);
            if (!isSupported(challenge)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (this.server == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            setChallenge(challenge);
            this.captchaAddress = this.server + "image?c=" + getChallenge();
        }
    }

    /**
     * @throws PluginException
     */
    protected void runDdosProtection() throws PluginException {
        if (plg != null) {
            try {
                plg.runCaptchaDDosProtection("recaptcha");
            } catch (InterruptedException e) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private void track(String string) {
        // HashMap<String, String> info = new HashMap<String, String>();
        // info.put("host", sourceHost);
        // StatsManager.I().track(100, "rc_track", string, info, CollectionName.RECAPTCHA);
    }

    public void parse() throws IOException, PluginException {
        this.form = null;
        this.id = null;
        if (this.br.containsHTML("Recaptcha\\.create\\(\".*?\"\\,\\s*\".*?\"\\,.*?\\)")) {
            this.id = this.br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
            final String div = this.br.getRegex("Recaptcha\\.create\\(\"(.*?)\"\\,\\s*\"(.*?)\"").getMatch(1);
            // find form that contains the found div id
            if (div == null || this.id == null) {
                System.out.println("reCaptcha ID or div couldn't be found...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final Form f : this.br.getForms()) {
                if (f.containsHTML("id\\s*?=\\s*?\"" + div + "\"")) {
                    this.form = f;
                    break;
                }
            }
        } else {
            final Form[] forms = this.br.getForms();
            this.form = null;
            for (final Form f : forms) {
                if (f.getInputField("recaptcha_challenge_field") != null) {
                    this.form = f;
                    break;
                }
            }
            if (this.form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                this.id = this.form.getRegex("k=(.*?)\"").getMatch(0);
                if (this.id == null || this.id.equals("") || this.id.contains("\\")) {
                    this.findID();
                }
                if (this.id == null || this.id.equals("")) {
                    System.out.println("reCaptcha ID couldn't be found...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    this.id = this.id.replace("&amp;error=1", "");
                }
            }
        }
        if (this.id == null || this.id.equals("")) {
            System.out.println("reCaptcha ID couldn't be found...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.form == null) {
            System.out.println("reCaptcha form couldn't be found...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /* do not use for plugins at the moment */
    private void prepareForm(final String code) throws PluginException {
        final String challenge = getChallenge();
        if (!isSupported(challenge)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (code == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.form.put("recaptcha_challenge_field", challenge);
        this.form.put("recaptcha_response_field", Encoding.urlEncode(code));
    }

    public void reload() throws IOException, PluginException {
        runDdosProtection();
        String newChallenge = null;
        try {
            newChallenge = org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1Handler.load(rcBr, id);
        } catch (Throwable e) {
            if (plg != null) {
                plg.getLogger().log(e);
            } else {
                e.printStackTrace();
            }
        }
        if (!isSupported(newChallenge)) {
            this.rcBr.getPage("https://www.google.com/recaptcha/api/reload?c=" + getChallenge() + "&k=" + this.id + "&reason=r&type=image&lang=en");
            newChallenge = this.rcBr.getRegex("Recaptcha\\.finish\\_reload\\(\\'(.*?)\\'\\, \\'image\\'").getMatch(0);
            if (!isSupported(newChallenge)) {
                System.out.println("Recaptcha Module fails: " + this.rcBr.getHttpConnection());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        setChallenge(newChallenge);
        this.captchaAddress = this.server + "image?c=" + getChallenge();
    }

    public void setCaptchaAddress(final String captchaAddress) {
        this.captchaAddress = captchaAddress;
    }

    public void setChallenge(final String challenge) {
        this.challenge = challenge;
    }

    public void setClearReferer(final boolean clearReferer) {
        this.clearReferer = clearReferer;
    }

    public Browser setCode(final String code) throws Exception {
        // <textarea name="recaptcha_challenge_field" rows="3"
        // cols="40"></textarea>\n <input type="hidden"
        // name="recaptcha_response_field" value="manual_challenge"/>
        this.prepareForm(code);
        this.br.submitForm(this.form);
        this.tries++;
        return this.br;
    }

    public void setForm(final Form form) {
        this.form = form;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setServer(final String server) {
        this.server = server;
    }
}
