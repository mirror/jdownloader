package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy", "sxyprn.com" }, urls = { "https?://(?:www\\.)?yourporn\\.sexy/[^/]*?\\.html(\\?page=\\d+)?", "https?://(?:www\\.)?sxyprn\\.com/[^/]*?\\.html(\\?page=\\d+)?" })
public class SxyprnCom extends antiDDoSForDecrypt {
    public SxyprnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            try {
                ((jd.plugins.hoster.SxyprnCom) plg).login(this.br, account, false);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        getPage(parameter.getCryptedUrl());
        final String[] posts = br.getRegex("(<div class='post_el_small'>.*?</span>\\s*</div>\\s*</a>\\s*</div>)").getColumn(0);
        for (final String postHTML : posts) {
            if (!postHTML.contains("post_vid_thumb")) {
                /* 2019-09-24: Try to skip non-video (e.g. text-only) content */
                continue;
            }
            final String[][] hits = new Regex(postHTML, "href=(?:\"|')(/post/[a-fA-F0-9]{13}\\.html)[^<>]*?title='(.*?)'").getMatches();
            for (final String[] hit : hits) {
                final DownloadLink link = createDownloadlink(br.getURL(hit[0]).toString());
                link.setName(hit[1].trim() + ".mp4");
                link.setAvailable(true);
                ret.add(link);
            }
        }
        final String pages[] = br.getRegex("<a href=(?:\"|')(/[^/]*?\\.html\\?page=\\d+)").getColumn(0);
        for (final String page : pages) {
            final String url = br.getURL(page).toString();
            if (!StringUtils.equals(parameter.getCryptedUrl(), url)) {
                final DownloadLink link = createDownloadlink(url);
                ret.add(link);
            }
        }
        final String packageName = new Regex(parameter.getCryptedUrl(), "/([^/]*?)\\.html").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(packageName);
        fp.addLinks(ret);
        return ret;
    }
}
