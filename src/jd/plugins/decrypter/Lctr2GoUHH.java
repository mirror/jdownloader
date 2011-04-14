package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/**
 * {@link DecrypterPlugin} zum parsen von Veranstaltungsseiten on
 * http://lecture2go.uni-hamburg.de
 * 
 * @author stonedsquirrel
 * @version 14.04.2011
 */
@DecrypterPlugin(flags = { 0 }, interfaceVersion = 2, names = { "lecture2go.uni-hamburg.de" }, revision = "", urls = { "http://lecture2go\\.uni-hamburg\\.de/veranstaltungen/-/v/[0-9]*" })
public class Lctr2GoUHH extends PluginForDecrypt {

    public Lctr2GoUHH(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {

        String regex = "http://lecture2go\\.uni-hamburg\\.de/videorep/.*\\.jpg";

        String page = br.getPage(parameter.getCryptedUrl());

        Matcher matcher = Pattern.compile(regex).matcher(page);

        String url = "";
        if (matcher.find()) {
            url = matcher.group(0).replaceAll("\\.jpg", ".mp4");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        links.add(createDownloadlink(url));
        progress.setFinished();
        return links;
    }
}
