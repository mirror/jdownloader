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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class JustPasteIt extends AbstractPastebinCrawler {
    public JustPasteIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        br.setLoadLimit(3 * br.getLoadLimit());
        br.setAllowedResponseCodes(451);
        return br;
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "justpaste.it", "jpst.it" });
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

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 451) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"isCaptchaRequired\":true")) {
            logger.warning("Captcha is not supported");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String pastebinText = br.getRegex("<div[^>]+id=\"articleContent\"[^>]*>(.*?)</div>").getMatch(0);
        if (pastebinText == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PastebinMetadata metadata = new PastebinMetadata(param, this.getFID(param.getCryptedUrl()));
        metadata.setPastebinText(pastebinText);
        // metadata.setUsername("@anonymous");
        final String json = br.getRegex("window\\.barOptions\\s*=\\s*(\\{.*?\\});").getMatch(0);
        final String title = br.getRegex("class=\"articleFirstTitle\"[^>]*>([^<>\"]+)</h1>").getMatch(0);
        if (title != null) {
            metadata.setTitle(title);
        } else {
            logger.warning("Failed to find pastebin title");
        }
        try {
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final String createdText = entries.get("createdText").toString();
            metadata.setDate(new Date(TimeFormatter.getMilliSeconds(createdText.substring(0, createdText.lastIndexOf(":")), "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)));
        } catch (final Throwable e) {
            logger.log(e);
            logger.info("Failed to find pastebin create date");
        }
        return metadata;
    }
}