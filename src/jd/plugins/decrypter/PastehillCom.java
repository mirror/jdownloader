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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PastehillCom extends AbstractPastebinCrawler {
    public PastehillCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pastehill.com" });
        ret.add(new String[] { "pastecanyon.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:download|embed/)?([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getContentURL(final CryptedLink link) {
        return "https://" + this.getHost() + "/" + this.getFID(link.getCryptedUrl());
    }

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(getContentURL(param));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = this.getFID(param.getCryptedUrl());
        final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        final UrlQuery query = new UrlQuery();
        query.add("slug", contentID);
        query.add("_token", Encoding.urlEncode(csrftoken));
        final Form pwpwotected = br.getFormbyProperty("id", "unlock_form");
        String passCode = param.getDecrypterPassword();
        Map<String, Object> entries = null;
        if (pwpwotected != null) {
            logger.info("This paste is password protected");
            boolean success = false;
            int attempt = 0;
            do {
                if (attempt > 0 || passCode == null) {
                    passCode = getUserInput("Password?", param);
                }
                query.addAndReplace("password", Encoding.urlEncode(passCode));
                brc.postPage("/get-paste", query);
                entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                final String status = (String) entries.get("status");
                if (!"error".equalsIgnoreCase(status)) {
                    success = true;
                    break;
                } else {
                    logger.info("User entered wrong password: " + passCode);
                    attempt++;
                    continue;
                }
            } while (!success && attempt <= 3);
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        } else {
            brc.postPage("/get-paste", query);
            entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
        }
        final String title = HTMLSearch.searchMetaTag(br, "twitter:title");
        final String contentb64 = entries.get("content").toString();
        final String pastebinTextUrlEncoded = Encoding.Base64Decode(contentb64);
        final String pastebinText = Encoding.htmlDecode(pastebinTextUrlEncoded);
        final PastebinMetadata metadata = new PastebinMetadata(param, contentID);
        metadata.setPassword(passCode);
        metadata.setPastebinText(pastebinText);
        if (title != null) {
            metadata.setTitle(Encoding.htmlDecode(title).trim());
        } else {
            logger.warning("Unable to find paste title");
        }
        final String username = br.getRegex("/u/([\\w\\-]+)").getMatch(0);
        if (username != null) {
            metadata.setUsername(username);
        } else {
            logger.warning("Unable to find paste username");
        }
        String dateStr = br.getRegex("title=\"Pasted at\"[^>]*></i>([^<]+)<").getMatch(0);
        if (dateStr != null) {
            dateStr = Encoding.htmlDecode(dateStr).trim();
            dateStr = dateStr.replaceAll("(?<=\\d)(st|nd|rd|th)", "");
            metadata.setDate(new Date(TimeFormatter.getMilliSeconds(dateStr, "dd MMM, yyyy", Locale.ENGLISH)));
        } else {
            logger.warning("Unable to find paste upload date");
        }
        final String officialDownloadlink = "/download/" + contentID;
        if (br.containsHTML(officialDownloadlink)) {
            logger.info("Paste is officially downloadable");
            metadata.setOfficialDirectDownloadlink(br.getURL(officialDownloadlink).toString());
        } else {
            logger.info("Paste is NOT officially downloadable");
        }
        return metadata;
    }

    private Form getPasswordForm(final Browser br) {
        return br.getFormbyProperty("id", "unlock_form");
    }
}