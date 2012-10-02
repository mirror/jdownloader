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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyblog.org" }, urls = { "http://(www\\.)?naughtyblog\\.org/(?!category|\\d{4}/)[^/]+" }, flags = { 0 })
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

    private Category CATEGORY;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CATEGORY = Category.UNDEF;

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Page not found \\(404\\)<|>403 Forbidden<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        String contentReleaseName = br.getRegex("<h2 class=\"post\\-title\">(.*?)</h2>").getMatch(0);
        if (contentReleaseName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        contentReleaseName = Encoding.htmlDecode(contentReleaseName).trim();

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
            if (contentReleaseLinks == null) contentReleaseLinks = br.getRegex("<h2 class=\"post\\-title\">(.*?)function validatecomment\\(form\\)\\{").getMatch(0);
            if (contentReleaseLinks == null) {
                logger.warning("contentReleaseLinks == null");
                return null;
            }
        } else {
            // <em>Download all screenhots:</em>
            // <font size="3px">
            contentReleaseLinks = br.getRegex(">Download all screenhots:(.*?)Previews:<").getMatch(0);
            if (contentReleaseLinks == null) {
                logger.warning("contentReleaseLinks == null");
                return null;
            }
        }
        final String[] links = new Regex(contentReleaseLinks, "<a href=\"(https?://(www\\.)?[^\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        for (final String link : links) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(getFpName(contentReleaseName));
            if (!link.matches("http://(www\\.)?naughtyblog\\.org/(?!category|\\d{4}/)[^/]+")) {
                final DownloadLink dl = createDownloadlink(link);
                fp.add(dl);
                decryptedLinks.add(createDownloadlink(link));
            }
        }

        final String[] imgs = br.getRegex("(http://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
        if (links != null && links.length != 0) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(getFpName(contentReleaseName) + " (Pictures)");
            for (final String img : imgs) {
                final DownloadLink dl = createDownloadlink(img);
                fp.add(dl);
                decryptedLinks.add(createDownloadlink(img));
            }
        }

        return decryptedLinks;
    }

    private String getFpName(String filePackageName) {
        switch (CATEGORY) {
        case CLIP:
            int firstOccurrenceOfSeparator = filePackageName.indexOf(" – ");
            StringBuffer sb = new StringBuffer(filePackageName);
            sb.insert(firstOccurrenceOfSeparator, " – Clips");
            filePackageName = sb.toString();
            break;
        case MOVIE:
            filePackageName += " – Movie";
            break;
        case SITERIP:
            if (!filePackageName.toLowerCase().contains("siterip")) filePackageName += " – SiteRip";
            break;
        default:
            break;
        }
        return filePackageName;
    }

}