package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class TakemyfileCom extends PluginForDecrypt {

    public TakemyfileCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        String url = br.getRegex("<a href=\"(http://tmf.myegy.com/2-ar.php\\?id=.*?)\">").getMatch(0);
        if (url != null) br.getPage(url);
        url = br.getRegex("onclick=\"NewWindow\\('(.*?)',").getMatch(0);
        if (url == null) return null;
        decryptedLinks.add(this.createDownloadlink(url));
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
