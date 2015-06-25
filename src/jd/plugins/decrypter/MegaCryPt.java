//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.controlling.FileCreationManager;

/**
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 30118 $", interfaceVersion = 3, names = { "megacry.pt" }, urls = { "http://(?:www\\.)?megacry\\.pt/([A-Za-z0-9]{12})" }, flags = { 0 })
public class MegaCryPt extends PluginForDecrypt {

    public MegaCryPt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String uid = null;
    private String a   = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();
        final String parameter = param.toString();
        uid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        a = ">\\s*Einen Augenblick bitte, der Ordner wird geöffnet\\.\\s*<|window\\.location\\.replace\\(\"/" + uid + "\"\\)";
        br.setFollowRedirects(false);
        // note: solve url in browser then in jd.. copy the cookie over from browser and you don't get captcha, but only for that id.

        // currently ALL response are within 404
        br.setAllowedResponseCodes(404);
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.getPage(parameter);
        br.setCookie(this.getHost(), "devicePixelRatio", "1");
        if (br.containsHTML(">Page not found</h1>|<title>Nothing found for")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // form takes a while to find, no doubt to bad regex and the html present.
        Form[] forms = br.getForms();
        Form captcha = getCaptchaForm(forms);
        if (captcha != null) {
            int i = -1;
            final int repeat = 4;
            while (true) {
                if (++i > repeat) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                final String imageHash = br.getRegex("/wp-content/plugins/securimage-wp/lib/siwp_captcha\\.php\\?id=([a-f0-9]{40})").getMatch(0);
                if (imageHash == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unexpected captcha type?");
                }
                final Browser cap = br.cloneBrowser();
                cap.getHeaders().put("Cache-Control", null);
                cap.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                final File captchaFile = this.getLocalCaptchaFile();
                String code = null;
                try {
                    cap.getDownload(captchaFile, "/wp-content/plugins/securimage-wp/lib/siwp_captcha.php?id=" + imageHash);
                    code = getCaptchaCode(getHost(), captchaFile, param);
                } finally {
                    if (captchaFile != null) {
                        FileCreationManager.getInstance().delete(captchaFile, null);
                    }
                }
                if ("".equals(code)) {
                    // this will effectively change the hash value without doing another request
                    br.getRequest().setHtmlCode(br.toString().replace(imageHash, JDHash.getSHA1(System.currentTimeMillis() + "")));
                    continue;
                }
                captcha.put("siwp_captcha_value", Encoding.urlEncode(code));
                br.submitForm(captcha);
                forms = br.getForms();
                captcha = getCaptchaForm(forms);
                if (captcha == null || br.containsHTML(a)) {
                    br.getPage(br.getURL());
                    forms = br.getForms();
                    break;
                }
            }
        }

        // contains clicknload/dlc/links (latter behind redirects for each one!! so lets just use cnl
        Form cnl = null;
        for (final Form f : forms) {
            if (f.getAction().contains("127.0.0.1:9666/")) {
                cnl = f;
                break;
            }
        }
        if (cnl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "clicknload can not be found.");
        }
        final HashMap<String, String> infos = new HashMap<String, String>();
        infos.put("crypted", Encoding.urlDecode(cnl.getInputField("crypted").getValue(), false));
        infos.put("jk", Encoding.urlDecode(cnl.getInputField("jk").getValue(), false));
        String source = cnl.getInputField("source").getValue();
        if (StringUtils.isEmpty(source)) {
            source = parameter.toString();
        } else {
            infos.put("source", source);
        }
        infos.put("source", source);
        final String json = JSonStorage.toString(infos);
        final DownloadLink dl = createDownloadlink("http://dummycnl.jdownloader.org/" + HexFormatter.byteArrayToHex(json.getBytes("UTF-8")));
        decryptedLinks.add(dl);

        return decryptedLinks;

    }

    private Form getCaptchaForm(Form[] forms) {
        final String[] p = { "name", "frm-captcha" };
        for (final Form f : forms) {
            if (f.getStringProperty(p[0]) != null && f.getStringProperty(p[0]).equalsIgnoreCase(p[1])) {
                return f;
            }
        }
        return null;
    }

}
