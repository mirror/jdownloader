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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OneInkCc extends PluginForDecrypt {
    public OneInkCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "1ink.cc", "1link.live" });
        ret.add(new String[] { "cuturl.cc" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        final String contentID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(false);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String redirect = br.getRegex("window\\.location\\.href\\s*=\\s*\"(https?://[^\"]+)").getMatch(0);
        if (redirect != null && !redirect.contains(contentID)) {
            /* 2021-03-19: Direct redirect */
            ret.add(createDownloadlink(redirect));
            return ret;
        } else if (redirect != null) {
            br.setFollowRedirects(true);
            br.getPage(redirect);
        }
        final Form captchaForm = br.getForm(0);
        if (captchaForm != null && captchaForm.containsHTML("/captcha\\.php")) {
            /* 2020-08-06: Added captcha support */
            logger.info("Captcha required");
            final String code = this.getCaptchaCode("/captcha.php", param);
            captchaForm.put("captcha", Encoding.urlEncode(code));
            br.submitForm(captchaForm);
        } else {
            logger.info("Captcha NOT required");
        }
        redirect = br.getRegex("function SkipAd\\(\\) \\{\\s*?window\\.location\\.href = \"(https?://1ink\\.info/[^\"]+)\"").getMatch(0);
        if (redirect == null) {
            redirect = br.getRegex("window\\.location\\.href\\s*=\\s*\"(https?://[^\"]+)").getMatch(0);
        }
        if (redirect != null) {
            br.getPage(redirect);
        }
        final String[] keys = new String[] { "token", "uri", "key", "pub", "r", "pubkey", "codec", "api" };
        final String data = br.getRegex("data\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        final Form passForm = new Form();
        passForm.setMethod(MethodType.POST);
        passForm.setAction("/api/pass.php");
        int foundValueNum = 0;
        for (final String key : keys) {
            final String valueOfKey = br.getRegex(key + "=([a-z0-9]+)").getMatch(0);
            if (valueOfKey != null) {
                passForm.put(key, valueOfKey);
                foundValueNum++;
            }
        }
        if (foundValueNum < 3 && data == null) {
            if (br.containsHTML("/api/captchafront")) {
                /*
                 * 2018-11-13: Broken/offline URL with infinite captcha loop (for valid URLs, we will find values for some of the keys[] and
                 * can skip this captcha!)
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (data != null) {
            /* 2021-03-19 */
            br.postPage(passForm.getAction(), data);
        } else {
            br.submitForm(passForm);
        }
        final String finallink = br.getRequest().getHtmlCode();
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!finallink.startsWith("http")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ret.add(createDownloadlink(finallink));
        return ret;
    }
}
