//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 5, names = {}, urls = {})
public class NaughtyBlgOrg extends antiDDoSForDecrypt {
    private enum Category {
        UNDEF,
        SITERIP,
        CLIP,
        MOVIE
    }

    public NaughtyBlgOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        /* Always add current domain to first position! */
        ret.add(new String[] { "naughtyblog.org", "naughtyblog.co", "naughtyblog.me", "nablog.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!webmasters|contact)[a-z0-9\\-]+/?");
        }
        return ret.toArray(new String[0]);
    }

    private Category CATEGORY;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CATEGORY = Category.UNDEF;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches("https://[^/]+/(category|linkex|feed|\\d{4}|tag|free\\-desktop\\-strippers|list\\-of\\-.+|contact\\-us|how\\-to\\-download\\-files|siterips)")) {
            logger.info("Invalid link: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Page not found \\(404\\)<|>403 Forbidden<") || br.containsHTML("No htmlCode read")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML(">Deleted due DMCA report<")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        getPage(parameter);
        final String nonce = br.getRegex("\"nonce\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String post_id = br.getRegex("\"post_id\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String recaptcha_key = br.getRegex("\"recaptcha_key\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String data_protection = br.getRegex("data-protection\\s*=\\s*\"(.*?)\"").getMatch(0);
        final String data_area = br.getRegex("data-area\\s*=\\s*\"(.*?)\"").getMatch(0);
        final String data_psid = br.getRegex("data-psid\\s*=\\s*\"(.*?)\"").getMatch(0);
        String downloadhidden = null;
        if (StringUtils.isAllNotEmpty(nonce, post_id, recaptcha_key, data_area, data_protection, data_psid)) {
            final Form form = new Form();
            form.setAction("/wp-admin/admin-ajax.php");
            form.setMethod(MethodType.POST);
            form.put("action", "validate_input");
            form.put("nonce", nonce);
            form.put("post_id", post_id);
            form.put("protection", URLEncode.encodeRFC2396(data_protection));
            form.put("area", URLEncode.encodeRFC2396(data_area));
            form.put("captcha_id", data_psid);
            form.put("type", "recaptcha");
            try {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, recaptcha_key).getToken();
                form.put("token", recaptchaV2Response);
                final Browser brc = br.cloneBrowser();
                brc.submitForm(form);
                final Map<String, Object> response = restoreFromString(brc.toString(), TypeRef.MAP);
                if (Boolean.TRUE.equals(response.get("success"))) {
                    downloadhidden = (String) response.get("content");
                }
            } catch (PluginException e) {
                logger.log(e);
            }
        }
        // String content = this.br.getRegex(Pattern.compile("<div id=\"main\\-content\" class=\"main\\-content\\-single\">(.*?)<h3
        // class=\"comments\"", 34)).getMatch(0);
        String contentReleaseName = br.getRegex("<h1 class=\"post\\-title entry\\-title\">(.*?)</h1>").getMatch(0);
        if (contentReleaseName == null) {
            // contentReleaseName = br.getRegex("<h1 class=\"post\\-title\">([^<>\"]*?)</h1>").getMatch(0);
            contentReleaseName = br.getRegex("<h1 class=\"post\\-title(.*?)</h1>").getMatch(0);
        }
        if (contentReleaseName == null) {
            logger.warning("Crawler broken or content offline");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // replace en-dash with a real dash
        contentReleaseName = contentReleaseName.replace("&#8211;", "-");
        contentReleaseName = Encoding.htmlDecode(contentReleaseName).trim();
        String contentReleaseNamePrecise = br.getRegex("<p>[\\r\\n\\s]*<strong>(.*?)</strong>[\\r\\n\\s]*<br[/\\s]+>[\\r\\n\\s]*<em>Released:").getMatch(0);
        if (contentReleaseNamePrecise != null) {
            // remove possible link to tag-cloud
            contentReleaseNamePrecise = contentReleaseNamePrecise.replaceAll("<.*?>", "");
            // replace en-dash with a real dash
            contentReleaseNamePrecise = contentReleaseNamePrecise.replace("&#8211;", "-");
            contentReleaseNamePrecise = Encoding.htmlDecode(contentReleaseNamePrecise).trim();
            int pos = contentReleaseName.lastIndexOf("-");
            if (pos != -1) {
                contentReleaseName = contentReleaseName.substring(0, pos).trim();
                contentReleaseName = contentReleaseName + " - " + contentReleaseNamePrecise;
            }
        }
        String contentReleaseNamePreciseSceneRelease = br.getRegex("<p>[\\r\\n\\s]*<strong>(.*?)</strong>[\\r\\n\\s]*<br[/\\s]+>[\\r\\n\\s]*<em>").getMatch(0);
        if (contentReleaseNamePreciseSceneRelease != null && contentReleaseNamePrecise == null) {
            // remove possible link to tag-cloud
            contentReleaseNamePreciseSceneRelease = contentReleaseNamePreciseSceneRelease.replaceAll("<.*?>", "");
            // contentReleaseNamePreciseSceneRelease = Encoding.htmlDecode(contentReleaseNamePreciseSceneRelease).trim();
            contentReleaseName = contentReleaseNamePreciseSceneRelease;
        }
        // check if DL is from the 'clips' section
        Regex categoryCheck = null;
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-clips.*\">");
        if (categoryCheck.matches()) {
            CATEGORY = Category.CLIP;
        }
        // check if DL is from the 'movies' section
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-movies.*\">");
        if (categoryCheck.matches()) {
            CATEGORY = Category.MOVIE;
        }
        // check if DL is from the 'siterips' section
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-siterips.*\">");
        if (categoryCheck.matches()) {
            CATEGORY = Category.SITERIP;
        }
        String contentReleaseLinks = null;
        if (CATEGORY != Category.SITERIP) {
            contentReleaseLinks = br.getRegex(">Download:?</(.*?)</div>").getMatch(0);
            // Nothing found? Get all links from title till comment field
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h(1|2) class=\"post\\-title\">(.*?)function validatecomment\\(form\\)\\{").getMatch(1);
            }
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h(1|2) class=\"post\\-title\">(.*?)class=\"comments\">Comments are closed").getMatch(1);
            }
        } else {
            // Get all links from title till comment field
            contentReleaseLinks = br.getRegex("<h(?:1|2) class=\"post\\-title[^\"]*?\">(.*?)function validatecomment\\(form\\)\\{").getMatch(0);
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h\\d+ class=\"post\\-title\">(.*?)class=\"comments\">").getMatch(0);
            }
        }
        if (contentReleaseLinks == null) {
            contentReleaseLinks = br.getRegex("<div\\s+id\\s*=[^>]*downloadhidden[^>]*>([^$]+)<div[^>]*id\\s*=[^>]*postinfo[^>]*class\\s*=[^>]*categories[^>]*>").getMatch(0);
        }
        if (contentReleaseLinks == null) {
            logger.warning("contentReleaseLinks == null");
            /* Final fallback --> Scan complete html */
            contentReleaseLinks = br.toString();
        }
        final Set<String> links = new HashSet<String>();
        final String[] foundLinks = HTMLParser.getHttpLinks(contentReleaseLinks, null);
        if (foundLinks != null) {
            links.addAll(Arrays.asList(foundLinks));
        }
        final String[] foundHiddenLinks = HTMLParser.getHttpLinks(downloadhidden, null);
        if (foundHiddenLinks != null) {
            links.addAll(Arrays.asList(foundHiddenLinks));
        }
        if (links.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches()) {
                String cleanedRegExString = link.replace("<a href=", "");
                cleanedRegExString = link.replace(" title", "");
                final DownloadLink dl = createDownloadlink(cleanedRegExString);
                decryptedLinks.add(dl);
            }
        }
        // final String[] imgs = br.getRegex("(https://([\\w\\.]+)?pixhost\\.to/show/[^\"]+)").getColumn(0);
        final String[] imgs = br.getRegex("(https?://(?:[\\w\\.]+)?pixhost\\.to/show/[^\"\\'<>]+)").getColumn(0);
        if (links != null && links.size() != 0) {
            for (final String img : imgs) {
                final DownloadLink dl = createDownloadlink(img);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage linksFP = FilePackage.getInstance();
        linksFP.setName(getFpName(contentReleaseName));
        linksFP.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getFpName(String filePackageName) {
        switch (CATEGORY) {
        case CLIP:
            final int firstOccurrenceOfSeparator = filePackageName.indexOf(" - ");
            if (firstOccurrenceOfSeparator > -1) {
                StringBuffer sb = new StringBuffer(filePackageName);
                sb.insert(firstOccurrenceOfSeparator, " - Clips");
                filePackageName = sb.toString();
            }
            break;
        case MOVIE:
            // filePackageName += " - Movie";
            break;
        case SITERIP:
            if (!filePackageName.toLowerCase().contains("siterip")) {
                filePackageName += " - SiteRip";
            }
            break;
        default:
            break;
        }
        return filePackageName;
    }
}