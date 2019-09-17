package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornroleplay.org" }, urls = { "https?://(\\w+\\.)?pornroleplay.org/\\d+-[a-zA-Z0-9\\-_]+\\.html" })
public class PornRoleplayOrg extends antiDDoSForDecrypt {
    // finallink will usually be an hotlink.cc URL
    public PornRoleplayOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl().replace("http://", "https://"));
        final String dle_skin = br.getRegex("var\\s*dle_skin\\s*=\\s*'(.*?)'").getMatch(0);
        if (dle_skin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!br.containsHTML("<meta property\\s*=\\s*\"og:title\"")) {
            return ret;
        }
        final String id = new Regex(parameter, "pornroleplay.org/(\\d+)").getMatch(0);
        Browser brc = br.cloneBrowser();
        getPage(brc, "/engine/ajax/getcap.php");
        final Form form = new Form();
        final String ajax_action = "https://pornroleplay.org/engine/ajax/getlink.php";
        form.setAction(ajax_action);
        form.setMethod(MethodType.POST);
        final String code = getCaptchaCode(br.cloneBrowser(), getHost(), "https://pornroleplay.org/engine/modules/antibot/antibot.php?rndval=" + System.currentTimeMillis(), parameter);
        form.put("sec_code", code);
        form.put("skin", Encoding.urlEncode(dle_skin));
        form.put("id", id);
        /* Important!! */
        brc = br.cloneBrowser();
        submitForm(brc, form);
        final String redirect = brc.getRegex("(https?://(\\w+\\.)?pornroleplay\\.org/(video|file)/[a-zA-Z0-9]+)").getMatch(0);
        if (redirect == null) {
            if (brc.containsHTML("To access the exclusive category you need to purchase")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        getPage(brc, redirect);
        final String finallink = brc.getRedirectLocation();
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.add(createDownloadlink(finallink));
            return ret;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.AntiBotCMS;
    }
}
