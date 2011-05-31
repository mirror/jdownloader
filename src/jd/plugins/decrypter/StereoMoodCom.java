package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stereomood.com" }, urls = { "http://[\\w\\.]*?stereomood\\.com/(song/\\d+|artist/.+|activity/.+|mood/.+)" }, flags = { 0 })
public class StereoMoodCom extends PluginForDecrypt {

    public StereoMoodCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        LinkedList<String> locations = new LinkedList<String>();
        String link = br.getRegex("playlist_url.*?(http://.*?json)").getMatch(0);
        if (link == null) return null;
        int index = 1;
        while (true) {
            String add = "";
            if (index > 1) {
                add = "?index=" + index;
            }
            br.getPage(link + add);
            String locations2[] = br.getRegex("location\":\"(http:.*?)\"").getColumn(0);
            if (locations2 == null || locations2.length == 0) break;
            for (String loc : locations2) {
                locations.add(loc);
            }
            try {
                if (progress.isAbort()) return decryptedLinks;
            } catch (Throwable e) {
                /* stable does not have appwork utils yet */
            }
            index++;
        }
        progress.setRange(locations.size());
        for (String location : locations) {
            location = location.replaceAll("\\\\/", "/");
            Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(false);
            br2.getPage(location);
            String url = br2.getRedirectLocation();
            if (url == null) continue;
            decryptedLinks.add(this.createDownloadlink("directhttp://" + url));
            try {
                if (progress.isAbort()) return decryptedLinks;
            } catch (Throwable e) {
                /* stable does not have appwork utils yet */
            }
            progress.increase(1);
        }
        return decryptedLinks;
    }

}
