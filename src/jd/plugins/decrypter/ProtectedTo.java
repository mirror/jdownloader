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
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ProtectedTo extends PluginForDecrypt {
    public ProtectedTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "protected.to" });
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

    private static final String PATTERN_RELATIVE_FOLDER      = "(?i)/f-([a-f0-9]{16})";
    private static final String PATTERN_RELATIVE_SINGLE_ITEM = "(?i)/\\?code=.+";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_FOLDER + "|" + PATTERN_RELATIVE_SINGLE_ITEM + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("^(?i)http://", "https://");
        final Regex itemsingle = new Regex(contenturl, PATTERN_RELATIVE_SINGLE_ITEM);
        final Regex itemfolder;
        if (itemsingle.patternFind()) {
            /* Single result */
            br.setFollowRedirects(false);
            // br.setCookie("protected.to", "ASP.NET_SessionId", "<someHash>");
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String redirect = br.getRedirectLocation();
            if (redirect == null) {
                redirect = br.getRegex("location\\.replace\\(\"(https?://[^\"]+)\"\\)").getMatch(0);
            }
            logger.info("Redirect = " + redirect);
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.canHandle(redirect)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(this.createDownloadlink(redirect));
        } else if ((itemfolder = new Regex(contenturl, PATTERN_RELATIVE_FOLDER)).patternFind()) {
            /* Multiple results */
            final String folderID = itemfolder.getMatch(0);
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*NotFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Form continueform = getCaptchaform(br);
            if (continueform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String captchaurl = br.getRegex("(/Captcha\\?id[a-zA-Z0-9_/\\+\\=\\-%]+)").getMatch(0);
            final String imageCaptchaKey = "CaptchaInputText";
            final boolean captchaNeeded;
            if (continueform.hasInputFieldByName(imageCaptchaKey) && captchaurl != null) {
                final String code = this.getCaptchaCode(captchaurl, param);
                continueform.put(imageCaptchaKey, Encoding.urlEncode(code));
                captchaNeeded = true;
            } else if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(continueform)) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                continueform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                captchaNeeded = true;
            } else {
                logger.info("No captcha required");
                captchaNeeded = false;
            }
            br.submitForm(continueform);
            String html = br.getRegex("<div class=\"well Encrypted-box[^\"]+\">(.*?)</div>\\s+</div>").getMatch(0);
            if (html == null) {
                /* Fallback */
                html = br.getRequest().getHtmlCode();
            }
            if (captchaNeeded && getCaptchaform(br) != null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final String[] ids = br.getRegex("data-slug=\"([a-f0-9]{16})").getColumn(0);
            if (ids == null || ids.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String token = br.getRegex("var token = \"([^\"]+)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            FilePackage fp = null;
            String title = br.getRegex("<h3 class=\"Encrypted-folder\"[^>]*>([^<]+)</h3>").getMatch(0);
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
                /* Remove total filesize from end of title */
                title = title.replaceFirst("\\s*\\[\\s*\\d+\\.\\d+[^\\[]+\\s*$", "");
                fp = FilePackage.getInstance();
                fp.setName(title);
            }
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final HashSet<String> dupes = new HashSet<String>();
            int index = -1;
            for (final String id : ids) {
                index++;
                logger.info("Crawling item " + index + "/" + ids.length);
                if (!dupes.add(id)) {
                    continue;
                }
                final boolean isLastItem = index == ids.length - 1;
                logger.info("Crawling single link with ID: " + id);
                final UrlQuery query = new UrlQuery();
                query.add("token", Encoding.urlEncode(token));
                query.add("folder", folderID);
                query.add("link", id);
                brc.postPage("/admin/Main/GetInFo", query);
                final String finallink = brc.getRegex("^redirect: (https?://.+)").getMatch(0);
                if (finallink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink link = createDownloadlink(finallink);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
                if (!isLastItem) {
                    /* Important else we can only crawl the first item! */
                    this.sleep(2500, param);
                }
            }
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private Form getCaptchaform(final Browser br) {
        for (final Form form : br.getForms()) {
            if (form.containsHTML("Continue to folder")) {
                return form;
            }
        }
        return null;
    }
}
