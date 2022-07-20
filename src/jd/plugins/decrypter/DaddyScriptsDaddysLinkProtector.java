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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "protect.dmd247.com", "isra.click" }, urls = { "https?://(?:www\\.)?protect\\.dmd247\\.com/[^<>\"/]+", "https?://(?:www\\.)?isra\\.click/.+" })
public class DaddyScriptsDaddysLinkProtector extends antiDDoSForDecrypt {
    public DaddyScriptsDaddysLinkProtector(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: Daddy's Link Protector */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean captchaFail = false;
        boolean passwordFail = false;
        int counter = 0;
        Form confirmationForm = null;
        String passCode = null;
        do {
            counter++;
            confirmationForm = br.getForm(0);
            if (confirmationForm == null) {
                passwordFail = false;
                captchaFail = false;
                break;
            } else {
                /* 2017-01-30: Either captcha OR password */
                if (confirmationForm.hasInputFieldByName("security_code")) {
                    captchaFail = true;
                    final String captcha_method_name;
                    if (counter > 1) {
                        /* 3rd try, ask user and do not rely on auto-solver */
                        captcha_method_name = "ziddu.com_manualcaptcha";
                    } else {
                        captcha_method_name = "ziddu6.com";
                    }
                    final String code = this.getCaptchaCode(captcha_method_name, "/CaptchaSecurityImages.php?width=100&height=40&characters=5", param);
                    confirmationForm.put("security_code", Encoding.urlEncode(code));
                } else if (confirmationForm.hasInputFieldByName("Pass1")) {
                    passwordFail = true;
                    if (counter == 0) {
                        passCode = this.getPluginConfig().getStringProperty("LAST_WORKING_PASSWORD");
                    } else {
                        passCode = null;
                    }
                    if (StringUtils.isEmpty(passCode)) {
                        passCode = getUserInput("Password?", param);
                    }
                    confirmationForm.put("Pass1", Encoding.urlEncode(passCode));
                } else {
                    passwordFail = false;
                    captchaFail = false;
                    break;
                }
                submitForm(confirmationForm);
            }
        } while (confirmationForm != null && counter <= 4);
        /*
         * 2020-02-12: Some hosts will only require a captcha on the first try and allow all further URLs to be processed without captchas
         * --> Based on their PHPSESSID cookie --> Store those
         */
        final Cookies cookies = br.getCookies(br.getHost());
        // save the session!
        synchronized (antiDDoSCookies) {
            antiDDoSCookies.put(br.getHost(), cookies);
        }
        if (captchaFail) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (passwordFail) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (!StringUtils.isEmpty(passCode)) {
            /* Store valid password for next attempt */
            this.getPluginConfig().setProperty("LAST_WORKING_PASSWORD", passCode);
        }
        String fpName = new Regex(param.getCryptedUrl(), "/([^/]+)$").getMatch(0);
        final String[] links = br.getRegex("<p><a href=\"(http[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            if (singleLink.contains(this.getHost())) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.DaddyScripts_DaddysLinkProtector;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
