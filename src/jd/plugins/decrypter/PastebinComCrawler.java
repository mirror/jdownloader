//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PastebinComCrawler extends AbstractPastebinCrawler {
    public PastebinComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pastebin.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:(?:download|raw)\\.php\\?i=|(?:raw|dl|print|report)/)?([0-9A-Za-z]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.setFollowRedirects(true);
        final String pasteID = this.getFID(param.getCryptedUrl());
        br.getPage("https://" + this.getHost() + "/" + pasteID);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (getPwProtectedForm(br) != null) {
            int counter = 0;
            Form pwprotected = getPwProtectedForm(br);
            do {
                String passCode = null;
                if (param.getDownloadLink() != null) {
                    passCode = param.getDownloadLink().getDownloadPassword();
                }
                if (passCode == null || counter > 0) {
                    passCode = this.getUserInput("Enter password", param);
                }
                pwprotected.put(Encoding.urlEncode("PostPasswordVerificationForm[password]"), Encoding.urlEncode(passCode));
                this.br.submitForm(pwprotected);
                pwprotected = getPwProtectedForm(br);
                if (pwprotected == null) {
                    logger.info("User entered valid password: " + passCode);
                    param.setDecrypterPassword(passCode);
                    break;
                } else {
                    logger.info("User entered invalid password: " + passCode);
                    counter++;
                    continue;
                }
            } while (!this.isAbort() && counter <= 2);
            if (pwprotected != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        /* Check for invalid URLs e.g. https://pastebin.com/tools */
        if (!br.containsHTML("/report/" + pasteID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String plaintext = br.getRegex("<textarea(.*?)</textarea>").getMatch(0);
        if (plaintext == null) {
            plaintext = br.getRegex("<div class\\s*=\\s*\"source[^\"]*\"[^>]*>(.*?)</div>\\s*</div>").getMatch(0);
            if (plaintext != null) {
                plaintext = plaintext.replaceAll("<li[^>]*>", "");
                plaintext = plaintext.replaceAll("<div[^>]*>", "");
                plaintext = plaintext.replaceAll("</li>", "");
                plaintext = plaintext.replaceAll("</div>", "");
            }
        }
        if (plaintext == null && (br.getURL().contains("raw.php") || br.getURL().contains("/raw/"))) {
            /* Current browser instance contains raw plain text -> Use that */
            plaintext = br.getRequest().getHtmlCode();
        } else if (plaintext == null && br.containsHTML("/raw/" + pasteID)) {
            /*
             * Fallback if we failed to extract the paste text from html -> Access special URL which will provide us with the plain text we
             * are looking for.
             */
            br.getPage("/raw/" + pasteID);
            plaintext = br.getRequest().getHtmlCode();
        }
        if (plaintext == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PastebinMetadata metadata = new PastebinMetadata(param, this.getFID(param.getCryptedUrl()));
        metadata.setPastebinText(plaintext);
        final String title = br.getRegex("<h1>([^<]+)</h1>").getMatch(0);
        if (title != null && !title.trim().equalsIgnoreCase("untitled")) {
            metadata.setTitle(Encoding.htmlDecode(title).trim());
        } else {
            logger.warning("Unable to find paste title");
        }
        final String username = br.getRegex("<a href=\"/message/compose\\?to=([^\"]+)\"").getMatch(0);
        if (username != null) {
            metadata.setUsername(username);
        } else {
            logger.warning("Unable to find paste username");
        }
        final Regex dateregex = br.getRegex("class=\"date\"[^>]*>\\s*<span title=\"[A-Za-z]+ (\\d+)([a-z]{2})? of ([^\"]+)\"");
        if (dateregex.matches()) {
            final String dayNumber = dateregex.getMatch(0);
            final String restOfDate = dateregex.getMatch(2);
            final String newDateString = dayNumber + " " + restOfDate;
            metadata.setDate(new Date(TimeFormatter.getMilliSeconds(newDateString, "dd MMM yyyy HH:mm:ss a ZZZ", Locale.ENGLISH)));
        } else {
            logger.warning("Unable to find paste date");
        }
        return metadata;
    }

    private Form getPwProtectedForm(final Browser br) {
        for (final Form form : br.getForms()) {
            if (form.containsHTML("postpasswordverificationform-password")) {
                return form;
            }
        }
        return null;
    }
}