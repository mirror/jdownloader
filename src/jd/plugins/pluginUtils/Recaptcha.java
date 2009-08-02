package jd.plugins.pluginUtils;

import java.io.File;
import java.io.IOException;

import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * LIttle helper calss to make recaptcha easier.
 * 
 * Examplecode: try{ Recaptcha rc = new Recaptcha(br); rc.parse(); rc.load();
 * File cf = rc.downloadCaptcha(getLocalCaptchaFile());
 * 
 * 
 * String c = getCaptchaCode(cf, param);
 * 
 * rc.setCode(c);
 * 
 * }catch(Exception e){
 * 
 * }
 * 
 * 
 * @author Coalado
 * 
 */
public class Recaptcha {

    private Browser br;
    private String challenge;
    private String server;

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getCaptchaAddress() {
        return captchaAddress;
    }

    public void setCaptchaAddress(String captchaAddress) {
        this.captchaAddress = captchaAddress;
    }

    private String captchaAddress;
    private String id;
    private Browser rcBr;
    private Form form;

    public Recaptcha(Browser br) {
        this.br = br;
    }

    public void parse() throws IOException, PluginException {

        Form[] forms = br.getForms();
        form = null;
        for (Form f : forms) {
            if (f.getInputField("recaptcha_challenge_field") != null) {
                form = f;
                break;
            }
        }
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        id = form.getRegex("k=(.*?)\"").getMatch(0);

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void load() throws IOException {
        rcBr = br.cloneBrowser();
        rcBr.getPage("http://api.recaptcha.net/challenge?k=" + id);
        challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
        server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
        captchaAddress = server + "image?c=" + challenge;
    }

    public File downloadCaptcha(File captchaFile) throws IOException {
        Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
        return captchaFile;
    }

    public Browser setCode(String code) throws Exception {
        // <textarea name="recaptcha_challenge_field" rows="3"
        // cols="40"></textarea>\n <input type="hidden"
        // name="recaptcha_response_field" value="manual_challenge"/>
        form.put("recaptcha_challenge_field", challenge);
        form.put("recaptcha_response_field", code);
        br.submitForm(form);
        return br;

    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

}
