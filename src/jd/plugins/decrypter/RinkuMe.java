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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RinkuMe extends PluginForDecrypt {
    public RinkuMe(PluginWrapper wrapper) {
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
        ret.add(new String[] { "rinku.me" });
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

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Form nextform = null;
        for (final Form form : br.getForms()) {
            if (form.containsHTML("btn-[a-f0-9]{32}")) {
                nextform = form;
                break;
            }
        }
        if (nextform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(nextform);
        String specialRedirect = br.getRequest().getHTMLRefresh();
        if (specialRedirect == null) {
            specialRedirect = br.getRegex("content=\"0;url=(https?://[^\"]+)\" http-equiv=\"refresh\"").getMatch(0);
        }
        if (specialRedirect != null) {
            br.getPage(specialRedirect);
        }
        final long sleepMillisBetweenSteps = 15 * 1001l;
        final Form nextform2 = br.getForms()[0];
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        nextform2.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(nextform2);
        final Form nextform3 = br.getForms()[0];
        this.sleep(sleepMillisBetweenSteps, param);
        br.submitForm(nextform3);
        final Form nextform4 = br.getForms()[0];
        /* Very very cheap errorhandling */
        if (!br.containsHTML("Step 2/2")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.sleep(sleepMillisBetweenSteps, param);
        br.submitForm(nextform4);
        /* That shall redirect us to advertisingexcel.com/rinku.me/outgoing/ */
        final Form nextform5 = br.getForms()[0];
        br.setFollowRedirects(false);
        br.submitForm(nextform5);
        /* Redirect back to our initial link. */
        final String preFinalLink = br.getRedirectLocation();
        if (preFinalLink == null || !preFinalLink.contains(contentID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(preFinalLink);
        /* Redirect to final link. */
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.add(createDownloadlink(finallink));
            return ret;
        }
    }
}
