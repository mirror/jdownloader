package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinksafeInfo extends PluginForDecrypt {

    public LinksafeInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);

        String matchText = "posli\\(\\\"([0-9]+)\\\",\\\"([0-9]+)\\\"\\)";

        String downloadId = br.getRegex(matchText).getMatch(1);

        String[] fileIds = br.getRegex(matchText).getColumn(0);
        progress.setRange(fileIds.length);
        for (String fileId : fileIds) {
            br.getPage("http://www.linksafe.info/posli.php?match=" + fileId + "&id=" + downloadId);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(br.getURL())));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
