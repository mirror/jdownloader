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
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class HdencodeOrg extends PluginForDecrypt {
    public HdencodeOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "hdencode.org", "hdencode.com", "hdencode.ro" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!rss)[a-z0-9]+-[a-z0-9\\-]+/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Form captchaform = br.getFormByRegex(".*content-protector-access-form.*");
        if (captchaform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Add special values to Form - without them, captcha will not be accepted (in browser, js needs to be active for this to work). */
        final String[][] specialFormKeyValuePairs = br.getRegex("data\\.data\\.append\\(\"([^\"]+)\", \"([^\"]+)\"\\);").getMatches();
        if (specialFormKeyValuePairs == null || specialFormKeyValuePairs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String[] keyValuePair : specialFormKeyValuePairs) {
            captchaform.put(Encoding.urlEncode(keyValuePair[0]), Encoding.urlEncode(keyValuePair[1]));
        }
        final String recaptchaV2Response;
        if (br.containsHTML("\"version\"\\s*:\\s*\"invisible")) {
            /* Invisible reCaptchaV2 */
            recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
                public TYPE getType() {
                    return TYPE.INVISIBLE;
                }
            }.getToken();
        } else {
            /* reCaptchaV2 auto handling */
            recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        }
        captchaform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(captchaform);
        final String[] htmls = br.getRegex("<blockquote>(.*?)</blockquote>").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            /* This should never happen. */
            logger.warning("Somehow wrong captcha or plugin broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String html : htmls) {
            final String[] urls = HTMLParser.getHttpLinks(html, br.getURL());
            if (urls == null || urls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String url : urls) {
                ret.add(createDownloadlink(url));
            }
        }
        logger.info("Number of results: " + ret.size());
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            /* Title contains file-size -> Remove that */
            title = title.replaceFirst("\\s*– [0-9\\.]+ ?(KB|MB|GB|TB)$", "");
        }
        final String nfoArchiveContentFilename = br.getRegex("Filename[\\.]*: (.*?)\\s").getMatch(0);
        /* We want all results to go into one package */
        final FilePackage fp = FilePackage.getInstance();
        if (nfoArchiveContentFilename != null) {
            if (nfoArchiveContentFilename.contains(".")) {
                /* Package name = Filename without file-extension */
                fp.setName(nfoArchiveContentFilename.substring(0, nfoArchiveContentFilename.lastIndexOf(".")));
            } else {
                fp.setName(nfoArchiveContentFilename);
            }
        } else if (title != null) {
            fp.setName(title);
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.setCleanupPackageName(false);
        fp.addLinks(ret);
        return ret;
    }
}
