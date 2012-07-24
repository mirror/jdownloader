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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "naughtyblog.org" }, urls = { "http://(www\\.)?naughtyblog\\.org/(?!category)[^/]+" }, flags = { 0 })
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

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        Category category = Category.UNDEF;

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);

        String contentReleaseName = br.getRegex("<h2 class=\"post\\-title\">(.*?)</h2>").getMatch(0);
        if (contentReleaseName == null) return null;

        // check if DL is from the 'clips' section
        Regex categoryCheck = null;
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-clips.*\">");
        if (categoryCheck.matches()) {
            category = Category.CLIP;
        }
        // check if DL is from the 'movies' section
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-movies.*\">");
        if (categoryCheck.matches()) {
            category = Category.MOVIE;
        }
        // check if DL is from the 'siterips' section
        categoryCheck = br.getRegex("<div id=\"post-\\d+\" class=\".*category\\-siterips.*\">");
        if (categoryCheck.matches()) {
            category = Category.SITERIP;
        }
        String contentReleaseLinks = null;
        if (category != Category.SITERIP) {
            contentReleaseLinks = br.getRegex(">Download:?</(.*?)</div>").getMatch(0);
            if (contentReleaseLinks == null) {
                logger.warning("contentReleaseLinks == null");
                return null;
            }
        } else {
            // <em>Download all screenhots:</em>
            // <font size="3px">
            contentReleaseLinks = br.getRegex("<strong>Download all screenhots:</strong>(.*?)<p><strong>Previews:</strong><br").getMatch(0);
            if (contentReleaseLinks == null) {
                logger.warning("contentReleaseLinks == null");
                return null;
            }
        }
        String[] links = new Regex(contentReleaseLinks, "<a href=\"(http[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        for (final String link : links) {
            if (!this.canHandle(link)) decryptedLinks.add(createDownloadlink(link));
        }

        String[] imgs = br.getRegex("(http://([\\w\\.]+)?pixhost\\.org/show/[^\"]+)").getColumn(0);
        if (links != null && links.length != 0) {
            for (String img : imgs) {
                decryptedLinks.add(createDownloadlink(img));
            }
        }
        FilePackage fp = FilePackage.getInstance();
        String filePackageName = Encoding.htmlDecode(contentReleaseName).trim();
        switch (category) {
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
        fp.setName(filePackageName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}