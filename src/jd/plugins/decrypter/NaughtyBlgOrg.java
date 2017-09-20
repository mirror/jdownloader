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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "naughtyblog.org" }, urls = { "https?://(www\\.)?naughtyblog\\.org/(?!webmasters|contact)[a-z0-9\\-]+/?" })
public class NaughtyBlgOrg extends PluginForDecrypt {

    private enum Category {
        UNDEF,
        SITERIP,
        CLIP,
        MOVIE
    }

    public NaughtyBlgOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Category category;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        category = Category.UNDEF;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent(BrowserName.Chrome));
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Page not found \\(404\\)<|>403 Forbidden<") || br.containsHTML("No htmlCode read") || br.containsHTML(">Deleted due DMCA report<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String contentReleaseName = br.getRegex("<h2 class=\"post-title\">(.*?)</h2>").getMatch(0);
        if (contentReleaseName == null) {
            contentReleaseName = br.getRegex("<h1 class=\"post-title\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (contentReleaseName == null) {
            // easier to return offline than throw error.
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }

        // replace en-dash with a real dash
        contentReleaseName = contentReleaseName.replace("&#8211;", "-");
        contentReleaseName = Encoding.htmlDecode(contentReleaseName).trim();

        String contentReleaseNamePrecise = br.getRegex("<p>\\s*<strong>(.*?)</strong>\\s*<br[/\\s]+>\\s*<em>Released:").getMatch(0);
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
        // check if DL is from the 'clips' section
        if (br.getRegex("<div id=\"post-\\d+\" class=\".*category-clips.*\">").matches()) {
            category = Category.CLIP;
        } else if (br.getRegex("<div id=\"post-\\d+\" class=\".*category-movies.*\">").matches()) {
            // check if DL is from the 'movies' section
            category = Category.MOVIE;
        } else if (br.getRegex("<div id=\"post-\\d+\" class=\".*category-siterips.*\">").matches()) {
            // check if DL is from the 'siterips' section
            category = Category.SITERIP;
        }
        String contentReleaseLinks = null;
        if (category != Category.SITERIP) {
            contentReleaseLinks = br.getRegex(">Download:?</(.*?)</div>").getMatch(0);
            // Nothing found? Get all links from title till comment field
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h[12] class=\"post-title\">(.*?)function validatecomment\\(form\\)\\{").getMatch(0);
            }
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h[12] class=\"post-title\">(.*?)class=\"comments\">Comments are closed").getMatch(0);
            }
        } else {
            // Get all links from title till comment field
            contentReleaseLinks = br.getRegex("<h2 class=\"post-title\">(.*?)function validatecomment\\(form\\)\\{").getMatch(0);
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h\\d+ class=\"post-title\">(.*?)class=\"comments\">").getMatch(0);
            }
        }
        if (contentReleaseLinks == null) {
            logger.warning("contentReleaseLinks == null");
            return null;
        }
        final String[] links = new Regex(contentReleaseLinks, "<a[^>]*\\s+href=(\"|')(https?://(www\\.)?[^\"]*?)\\1").getColumn(1);
        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        for (final String link : links) {
            if (!link.matches("https?://(www\\.)?naughtyblog\\.org/.+")) {
                final DownloadLink dl = createDownloadlink(link);
                decryptedLinks.add(dl);
            }
        }

        final String[] imgs = br.getRegex("(https?://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
        if (links != null && links.length != 0) {
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
        switch (category) {
        case CLIP:
            final int firstOccurrenceOfSeparator = filePackageName.indexOf(" - ");
            if (firstOccurrenceOfSeparator > -1) {
                StringBuffer sb = new StringBuffer(filePackageName);
                sb.insert(firstOccurrenceOfSeparator, " - Clips");
                filePackageName = sb.toString();
            }
            break;
        case MOVIE:
            filePackageName += " - Movie";
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}