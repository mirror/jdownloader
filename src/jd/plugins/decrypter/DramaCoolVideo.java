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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DramaCoolVideo extends PluginForDecrypt {
    public DramaCoolVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dramacool.pa", "dramacool.cr", "dramacool.ch", "dramacool.bz", "dramacool.video", "dramacool.movie", "dramacool.so", "dramacool.link", "dramacool.vc", "dramacool.fo" });
        ret.add(new String[] { "watchasian.pe", "watchasian.vc" });
        ret.add(new String[] { "gogoanime3.net", "gogoanime3.co", "gogoanime.tel", "gogoanime.tv", "gogoanime.io", "gogoanime.vc", "gogoanime.sh", "gogoanime.gg", "gogoanime.run" });
        return ret;
    }

    private static String[] getDeadDomains() {
        return new String[] { "dramacool.link", "gogoanime.io", "gogoanime.sh" };
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
            ret.add("https?://(?:www\\d*\\.)?" + buildHostsPatternPart(domains) + "/[\\w\\-]+(\\.html)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String domainInAddedURL = Browser.getHost(param.getCryptedUrl(), true);
        String includedDeadDomain = null;
        for (final String deadDomain : getDeadDomains()) {
            if (domainInAddedURL.contains(deadDomain)) {
                includedDeadDomain = deadDomain;
                break;
            }
        }
        if (includedDeadDomain != null) {
            /* Replace dead domain with our main domain which is hopefully working. Do not touch subdomains. */
            final String newDomain = domainInAddedURL.replace(includedDeadDomain, this.getHost());
            logger.info("Added URL contains dead domain " + includedDeadDomain + " | Using this full domain instead: " + newDomain);
            br.getPage(param.getCryptedUrl().replaceFirst(Pattern.quote(domainInAddedURL), newDomain));
        } else {
            br.getPage(param.getCryptedUrl());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>(?:Watch\\s+)([^<]+)\\s+\\|[\\s\\w]+").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath().substring(1).replace("-", " ").trim();
        }
        String[] links = br.getRegex("data-video=\"([^\"]+)\"\\s*>").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<li>\\s*<a href=\"([^\"]+)\" class=\"img\">\\s*<span class=\"type[^\"]*\">").getColumn(0);
        }
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (links != null && links.length > 0) {
            final ArrayList<String> gogoplayURLs = new ArrayList<String>();
            for (String link : links) {
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                // link = Encoding.htmlDecode(link);
                if (Gogoplay4Com.looksLikeSupportedPattern(link)) {
                    /*
                     * Do not add those in first run because those will lead to captchas and return the same URLs that we might find here
                     * directly in html without captchas.
                     */
                    gogoplayURLs.add(link);
                } else {
                    ret.add(createDownloadlink(link));
                }
            }
            if (ret.isEmpty() && !gogoplayURLs.isEmpty()) {
                /* Fallback: User will need to solve captchas to crawl those URLs! */
                for (final String gogoplayURL : gogoplayURLs) {
                    ret.add(createDownloadlink(gogoplayURL));
                }
            }
        }
        if (title != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.setAllowMerge(true);
            fp.addLinks(ret);
        }
        return ret;
    }
}