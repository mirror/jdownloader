//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GuardLink extends PluginForDecrypt {
    public GuardLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "guard.link" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/[a-f0-9]{8}");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (this.getCaptchaForm() != null) {
            boolean captchaSuccess = false;
            int counter = 0;
            do {
                final Form captchaForm = this.getCaptchaForm();
                final String captchaURL = captchaForm.getRegex("(/captcha\\.php[^<>\"\\']+)").getMatch(0);
                final String code = this.getCaptchaCode(captchaURL, param);
                captchaForm.put("captcha_code", Encoding.urlEncode(code));
                this.br.submitForm(captchaForm);
                if (this.getCaptchaForm() == null) {
                    captchaSuccess = true;
                    break;
                }
                counter += 1;
            } while (!this.isAbort() && counter <= 3);
            if (!captchaSuccess) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        String passCode = null;
        if (this.getPasswordForm() != null) {
            boolean passwordSuccess = false;
            int counter = 0;
            do {
                final Form passwordForm = this.getPasswordForm();
                passCode = getUserInput("Password?", param);
                passwordForm.put("password", Encoding.urlEncode(passCode));
                this.br.submitForm(passwordForm);
                if (this.getPasswordForm() == null) {
                    passwordSuccess = true;
                    break;
                }
                counter += 1;
            } while (!this.isAbort() && counter <= 3);
            if (!passwordSuccess) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String[] sources = br.getRegex("<textarea(.*?)</textarea>").getColumn(0);
        String source = "";
        for (final String sourceTmp : sources) {
            source += sourceTmp;
        }
        if (StringUtils.isEmpty(source)) {
            /* Fallback */
            source = br.toString();
        }
        String fpName = br.getRegex("<h2>Folder name</h2>\\s*<h3>([^<>\"]+)</h3>").getMatch(0);
        final String[] urls = HTMLParser.getHttpLinks(source, br.getURL());
        if (urls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String url : urls) {
            if (!this.canHandle(url)) {
                final DownloadLink link = createDownloadlink(url);
                if (passCode != null) {
                    link.setDownloadPassword(passCode);
                }
                decryptedLinks.add(link);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private Form getCaptchaForm() {
        /* 2021-02-15: Form is always present but hidden */
        if (br.containsHTML("<div style=\"display:block;\">\\s*<form action=\"\" accept-charset=\"utf-8\" method=\"post\">\\s*<input name=\"captcha\" value=\"captcha\" type=\"hidden\">")) {
            return br.getFormbyKey("captcha");
        }
        return null;
    }

    private Form getPasswordForm() {
        /* 2021-02-15: Form is always present but hidden */
        if (br.containsHTML("<div class=\"panel-body\" style=\"display:block;\">\\s*<div class=\"signup_form\">")) {
            return br.getFormbyKey("password");
        }
        return null;
    }
}
