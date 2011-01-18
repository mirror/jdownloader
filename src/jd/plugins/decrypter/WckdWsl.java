package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wickedweasel.com" }, urls = { "http://[\\w\\.]*?wickedweasel\\.com/.*?_galleries/\\d+" }, flags = { 0 })
public class WckdWsl extends PluginForDecrypt {

    public WckdWsl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String url = new Regex(parameter.getCryptedUrl(), ".*?(contributor_galleries/\\d+)").getMatch(0);
        if (url == null) url = new Regex(parameter.getCryptedUrl(), ".*?(ww_model_galleries/\\d+)").getMatch(0);
        if (url == null) return null;
        String number = new Regex(url, ".*?/(\\d+)").getMatch(0);
        br.getPage("http://wickedweasel.com/en-de/" + url);
        String name = br.getRegex("bikini contest.*?" + url + ".*?>(.*?)<").getMatch(0);
        if (name == null) return null;
        name = name + "-" + number;
        String links[] = br.getRegex("(http://[^\"]*?wickedweasel.com/assets/images/pics/\\d+/\\d+/\\d+\\.jpg)").getColumn(0);
        if (links == null || links.length == 0) return null;
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        for (String link : links) {
            DownloadLink add = this.createDownloadlink("directhttp://" + link);
            ret.add(add);
        }
        fp.addLinks(ret);
        return ret;
    }

}
