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
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PsaWf extends PluginForDecrypt {
    public PsaWf(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "psa.wf" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[a-z0-9]{32}/.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Without this Cloudflare will kick in. */
        br.getHeaders().put("Referer", "https://psa.wf/");
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form redirectform = br.getFormbyProperty("name", "redirect");
        br.submitForm(redirectform);
        final String additionalParam = br.getRegex("var url8j = \"(https?://[^\"]+)\";").getMatch(0);
        /* https://www.google.es/url... */
        String googleLink = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+;url=(https?://[^\"]+)").getMatch(0);
        if (additionalParam != null && !googleLink.contains("url8j=")) {
            googleLink += "&url8j=" + Encoding.urlEncode(additionalParam);
        }
        br.getPage(googleLink);
        /* Goes to tiktokcounter.net */
        final String jsredirect = br.getRegex("var redirectUrl='(https?://[^\"\\']+)").getMatch(0);
        br.getPage(jsredirect);
        /* TODO: This next step shouldn't be needed */
        final String path = br._getURL().getPath();
        br.getPage(path + "?url8j=" + Encoding.urlEncode(additionalParam));
        /* Redirect to the next fake blog page */
        final String nextRedirect = br.getRegex("window\\.location\\.href = \"(https?://[^\"]+)\"").getMatch(0);
        if (nextRedirect == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(nextRedirect);
        final String validatorName = br.getRegex("el\\.name = \"(validator\\d+)\";").getMatch(0);
        if (validatorName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Form continueform = new Form();
        continueform.setMethod(MethodType.POST);
        continueform.setAction(br.getURL());
        continueform.put("no-recaptcha-noresponse", "true");
        continueform.put(Encoding.urlEncode(validatorName), "true");
        /* This: https://insurancexblog.blogspot.com/?url=https://psa.btcut.io/... */
        String finalresult = null;
        for (int i = 0; i <= 4; i++) {
            logger.info("Loop: " + i);
            br.submitForm(continueform);
            finalresult = br.getRegex("window\\.location\\.href = \"(https?://[^\"]+)\"").getMatch(0);
            if (finalresult != null) {
                break;
            } else if (this.isAbort()) {
                break;
            }
        }
        if (finalresult == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * This should get us back to psa.btcut.io but now with a "token" parameter inside the URL which allows us to get to the final URL.
         */
        br.getPage(finalresult);
        /* TODO: Final step back to btcut.io URL with token is missing. */
        /* This btcut.io link can be offline */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form goform = br.getFormbyProperty("id", "go-link");
        if (goform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean skipWait = false;
        final String waitSecondsStr = br.getRegex("class=\"timer\"[^>]*>\\s*(\\d+)\\s*</span>").getMatch(0);
        if (waitSecondsStr != null && !skipWait) {
            this.sleep(Integer.parseInt(waitSecondsStr) * 1000l, param);
        }
        br.submitForm(goform);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String finallink = entries.get("url").toString();
        ret.add(this.createDownloadlink(finallink));
        return ret;
    }
}
