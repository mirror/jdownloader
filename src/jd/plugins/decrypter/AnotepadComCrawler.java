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
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AnotepadComCrawler extends AbstractPastebinCrawler {
    public AnotepadComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    String getFID(final String url) {
        return new Regex(url, this.getSupportedLinks()).getMatch(0);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "anotepad.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/notes/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public PastebinMetadata crawlMetadata(final CryptedLink param, final Browser br) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)This note either is private or has been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = this.getFID(param.getCryptedUrl());
        final String title = br.getRegex("span class=\"note_title\"[^>]*>([^<]+)</span>").getMatch(0);
        String plaintxt = br.getRegex("href\\s*=\\s*\"(https?[^<>\"]+)\">\\s*Download Link\\s*:\\s*Click Here").getMatch(0);
        if (plaintxt == null) {
            /* Plaintext containing multiple links (?) */
            plaintxt = br.getRegex("<div class\\s*=\\s*\"plaintext[^\"]*\">([^<>]+)</div>").getMatch(0);
            if (plaintxt == null) {
                final String plaintxtTmp = br.getRegex("<div class\\s*=\\s*\"note_content\">(.*?)class\\s*=\\s*\"sub-header\"").getMatch(0);
                if (plaintxtTmp != null) {
                    plaintxt = new Regex(plaintxtTmp, "<div class\\s*=\\s*\"richtext\">(.*?)</div>").getMatch(0);
                }
            }
        }
        if (plaintxt == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final PastebinMetadata metadata = new PastebinMetadata(param, fid);
        metadata.setPastebinText(plaintxt);
        if (title != null) {
            metadata.setTitle(Encoding.htmlDecode(title).trim());
        }
        final String downloadurl = "/note/download/" + fid + "?format=Text";
        if (br.containsHTML(Pattern.quote(downloadurl))) {
            metadata.setOfficialDirectDownloadlink(br.getURL(downloadurl).toString());
        }
        return metadata;
    }
}