package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * {@link DecrypterPlugin} zum parsen von Veranstaltungsseiten on
 * http://lecture2go.uni-hamburg.de
 * 
 * @author stonedsquirrel
 * @version 14.04.2011
 */
@DecrypterPlugin(flags = { 0 }, interfaceVersion = 2, names = { "lecture2go.uni-hamburg.de" }, revision = "$Revision$", urls = { "http://lecture2go\\.uni-hamburg\\.de/veranstaltungen/-/v/[0-9]*" })
public class Lctr2GoUHH extends PluginForDecrypt {

    public Lctr2GoUHH(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.getPage(parameter.getCryptedUrl());
        String url = br.getRegex("(http://lecture2go\\.uni-hamburg\\.de/videorep/.*?\\.jpg)").getMatch(0);
        if (url != null) {
            url = url.replaceAll("\\.jpg", ".mp4");
        } else {
            return null;
        }
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        links.add(createDownloadlink("directhttp://" + url));
        progress.setFinished();
        return links;
    }
}
