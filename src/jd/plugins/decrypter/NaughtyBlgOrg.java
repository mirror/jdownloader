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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyblog.org" }, urls = { "http://(www\\.)?naughtyblog\\.org/[a-z0-9\\-]+" }, flags = { 0 })
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

    private Category            CATEGORY;
    private static final String INVALIDLINKS = "http://(www\\.)?naughtyblog\\.org/(category|linkex|feed|\\d{4}|tag|free\\-desktop\\-strippers|list\\-of\\-.+|contact\\-us|how\\-to\\-download\\-files|siterips)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CATEGORY = Category.UNDEF;

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Invalid link: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Page not found \\(404\\)<|>403 Forbidden<") || br.containsHTML("No htmlCode read")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML(">Deleted due DMCA report<")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        String contentReleaseName = br.getRegex("<h2 class=\"post\\-title\">(.*?)</h2>").getMatch(0);
        if (contentReleaseName == null) {
            contentReleaseName = br.getRegex("<h1 class=\"post\\-title\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (contentReleaseName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
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
            contentReleaseLinks = br.getRegex("<h2 class=\"post\\-title\">(.*?)function validatecomment\\(form\\)\\{").getMatch(0);
            if (contentReleaseLinks == null) {
                contentReleaseLinks = br.getRegex("<h\\d+ class=\"post\\-title\">(.*?)class=\"comments\">").getMatch(0);
            }
        }
        if (contentReleaseLinks == null) {
            logger.warning("contentReleaseLinks == null");
            return null;
        }
        final String[] links = new Regex(contentReleaseLinks, "<a href=\"(https?://(www\\.)?[^\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        for (final String link : links) {
            if (!link.matches("http://(www\\.)?naughtyblog\\.org/.+")) {
                final DownloadLink dl = createDownloadlink(link);
                decryptedLinks.add(dl);
            }
        }

        final String[] imgs = br.getRegex("(http://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
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