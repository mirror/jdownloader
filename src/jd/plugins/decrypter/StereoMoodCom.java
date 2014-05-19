package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stereomood.com" }, urls = { "http://(www\\.)?stereomood\\.com/(song/\\d+|artist/.+|activity/.+|mood/.+)" }, flags = { 0 })
public class StereoMoodCom extends PluginForDecrypt {

    public StereoMoodCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter);
            if (con.getResponseCode() == 400) {
                logger.info("Link refers to a server error: " + parameter);
                return decryptedLinks;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.getURL().contains("/search/") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        LinkedList<String> locations = new LinkedList<String>();
        final String link = br.getRegex("playlist_url.*?(http://.*?json)").getMatch(0);
        if (link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int index = 1;
        while (true) {
            String add = "";
            if (index > 1) {
                add = "?index=" + index;
            }
            br.getPage(link + add);
            String locations2[] = br.getRegex("location\":\"(http:.*?)\"").getColumn(0);
            if (locations2 == null || locations2.length == 0) {
                break;
            }
            for (String loc : locations2) {
                locations.add(loc);
            }
            index++;
        }
        for (String location : locations) {
            location = location.replaceAll("\\\\/", "/");
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(false);
            br2.getPage(location);
            final String url = br2.getRedirectLocation();
            if (url == null) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url);
            // Don't create too many requests
            dl.setAvailable(true);
            try {
                distribute(dl);
            } catch (final Throwable e) {
                // Not available in 0.851 Stable
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}