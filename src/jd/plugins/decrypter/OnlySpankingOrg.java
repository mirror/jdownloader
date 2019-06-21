package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40753 $", interfaceVersion = 3, names = { "onlyspanking.org" }, urls = { "https?://onlyspanking.org/\\d+-[a-zA-Z0-9\\-]+\\.html" })
public class OnlySpankingOrg extends antiDDoSForDecrypt {
    public OnlySpankingOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final String dle_skin = br.getRegex("var\\s*dle_skin\\s*=\\s*'(.*?)'").getMatch(0);
        if (dle_skin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!br.containsHTML("<meta property\\s*=\\s*\"og:title\"")) {
            return ret;
        }
        Browser brc = br.cloneBrowser();
        getPage(brc, "/engine/ajax/getcap.php");
        final Form form = brc.getForm(0);
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            form.setAction("https://onlyspanking.org/engine/ajax/getlink.php");
        }
        final String code = getCaptchaCode(br.cloneBrowser(), getHost(), "https://onlyspanking.org/engine/modules/antibot/antibot.php?rndval=" + System.currentTimeMillis(), parameter);
        form.put("sec_code", code);
        form.put("skin", Encoding.urlEncode(dle_skin));
        brc = br.cloneBrowser();
        submitForm(brc, form);
        final String video = brc.getRegex("(https?://onlyspanking.org/video/[a-zA-Z0-9]+)").getMatch(0);
        if (video == null) {
            if (brc.containsHTML("To access the exclusive category you need to purchase")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        getPage(brc, video);
        final String url = brc.getRedirectLocation();
        if (url != null) {
            ret.add(createDownloadlink(url));
            return ret;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }
}
