//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.pluginUtils;

import java.io.File;
import java.io.IOException;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;
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

    private final Browser br;
    private String challenge;
    private String server;
    private String captchaAddress;
    private String id;
    private Browser rcBr;
    private Form form;

    public Recaptcha(final Browser br) {
        this.br = br;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(final String challenge) {
        this.challenge = challenge;
    }

    public String getServer() {
        return server;
    }

    public void setServer(final String server) {
        this.server = server;
    }

    public String getCaptchaAddress() {
        return captchaAddress;
    }

    public void setCaptchaAddress(final String captchaAddress) {
        this.captchaAddress = captchaAddress;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(final Form form) {
        this.form = form;
    }

    public void parse() throws IOException, PluginException {
        final Form[] forms = br.getForms();
        form = null;
        for (final Form f : forms) {
            if (f.getInputField("recaptcha_challenge_field") != null) {
                form = f;
                break;
            }
        }
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            id = form.getRegex("k=(.*?)\"").getMatch(0);
        }
    }

    public void load() throws IOException {
        rcBr = br.cloneBrowser();
        rcBr.getPage("http://api.recaptcha.net/challenge?k=" + id);
        challenge = rcBr.getRegex("challenge : '(.*?)',").getMatch(0);
        server = rcBr.getRegex("server : '(.*?)',").getMatch(0);
        captchaAddress = server + "image?c=" + challenge;
    }

    public File downloadCaptcha(final File captchaFile) throws IOException {
        Browser.download(captchaFile, rcBr.openGetConnection(captchaAddress));
        return captchaFile;
    }

    public Browser setCode(final String code) throws Exception {
        // <textarea name="recaptcha_challenge_field" rows="3"
        // cols="40"></textarea>\n <input type="hidden"
        // name="recaptcha_response_field" value="manual_challenge"/>
        form.put("recaptcha_challenge_field", challenge);
        form.put("recaptcha_response_field", Encoding.urlEncode(code));
        br.submitForm(form);
        return br;

    }
}
