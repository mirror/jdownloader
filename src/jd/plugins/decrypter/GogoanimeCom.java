//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class GogoanimeCom extends antiDDoSForDecrypt {

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "gogoanime.com", "gogoanime.to", "goodanime.co", "goodanime.net", "gooddrama.net", "playbb.me", "videowing.me", "easyvideo.me", "videozoo.me", "video66.org", "animewow.tv", "dramago.com", "playpanda.net", "byzoo.org", "vidzur.com", "animetoon.tv", "dramagalaxy.com", "toonget.com", "goodmanga.net" };
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] a = new String[getAnnotationNames().length];
        int i = 0;
        for (final String domain : getAnnotationNames()) {
            a[i] = "http://(?:\\w+\\.)?" + Pattern.quote(domain) + "/(?:embed(\\.php)?\\?.*?vid(?:eo)?=.+|gogo/\\?.*?file=.+|(?!flowplayer)(?:[a-z\\-]+\\-(drama|movie|episode)/)?[a-z0-9\\-_]+(?:/\\d+)?)";
            i++;
        }
        return a;
    }

    // NOTE:
    // play44.net = gogoanime.com url (doesn't seem to have mirror in its own domain happening)
    // videobug.net = gogoanime.com url (doesn't seem to have mirror in its own domain happening)

    public GogoanimeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String invalidLinks = ".+" + Pattern.quote(this.getHost()) + "/(category|thumbs|sitemap|img|xmlrpc|fav|images|ads|gga\\-contact).*?";
    private final String embed        = ".+/(embed(\\.php)?\\?.*?vid(eo)?=.+|gogo/\\?.*?file=.+)";

    @Override
    protected boolean useRUA() {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(invalidLinks)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        // Offline
        if (br.containsHTML("Oops\\! Page Not Found<|>404 Not Found<|Content has been removed due to copyright or from users\\.<") || br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 403) {
            logger.info("This link is offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Invalid link
        if (br.containsHTML("No htmlCode read")) {
            logger.info("This link is invalid: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        if (parameter.matches(embed)) {
            // majority, if not all are located on play44.net (or ip address). There for there is no need for many hoster plugins, best to
            // use single hoster plugin so connection settings are aok.
            final String json = br.getRegex("var video_links = (\\{.*?\\});").getMatch(0);
            if (json != null) {
                final String[] links = new Regex(json, "\"link\"\\s*:\\s*\"(.*?)\"").getColumn(0);
                final String filename = PluginJSonUtils.getJson(json, "filename");
                for (final String link : links) {
                    final DownloadLink dl = createDownloadlink(PluginJSonUtils.unescape(link));
                    dl.setName(filename);
                    decryptedLinks.add(dl);
                }
            }
            if (decryptedLinks.isEmpty()) {
                // fail over
                final String url = br.getRegex(".+\\s*(?:url|file): (\"|')(.+\\.(mp4|flv|avi|mpeg|mkv).*?)\\1").getMatch(1);
                if (url != null) {
                    final DownloadLink link = createDownloadlink(Encoding.htmlOnlyDecode(url));
                    if (link != null) {
                        link.setProperty("forcenochunkload", Boolean.TRUE);
                        link.setProperty("forcenochunk", Boolean.TRUE);
                        decryptedLinks.add(link);
                    }
                }
            }
        } else {
            String fpName = br.getRegex("<h1( class=\"generic\">|>[^\r\n]+)(.*?)</h1>").getMatch(1);
            if (fpName == null || fpName.length() == 0) {
                fpName = br.getRegex("<title>(?:Watch\\s*)?([^<>\"]*?)( \\w+ Sub.*?|\\s*\\|\\s* Watch anime online, English anime online)?</title>").getMatch(0);
            }

            final String[] links = br.getRegex("<iframe.*?src=(\"|\\')(http[^<>\"]+)\\1").getColumn(1);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links) {
                // lets prevent returning of links which contain itself.
                if (singleLink.matches(embed)) {
                    singleLink = Encoding.htmlOnlyDecode(singleLink);
                    final DownloadLink dl = createDownloadlink(singleLink);
                    if (dl != null) {
                        dl.setProperty("forcenochunkload", Boolean.TRUE);
                        dl.setProperty("forcenochunk", Boolean.TRUE);
                        decryptedLinks.add(dl);
                    }
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
                fp.setProperty("ALLOW_MERGE", true);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public Boolean siteTesterDisabled() {
        // these are streaming domains, they do not have website and timeout. making tests take longer to finish for same outcome.
        if ("playbb.me".equals(getHost()) || "video66.org".equals(getHost()) || "vidzur.com".equals(getHost()) || "playpanda.net".equals(getHost()) || "videobug.net".equals(getHost())) {
            return Boolean.TRUE;
        }
        return super.siteTesterDisabled();
    }

}