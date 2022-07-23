package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "forum.lewdweb.net" }, urls = { "https?://forum\\.lewdweb\\.net/threads/[a-z\\-]+\\.[\\d]+/" })
public class LewdWebNet extends PluginForDecrypt {
    public LewdWebNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> result = new ArrayList<DownloadLink>();
        int pageNr = 1;
        boolean fetchNext = true;
        Browser myBr = this.getBrowser().cloneBrowser();
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        final jd.plugins.hoster.LewdWebNet hosterPlugin = (jd.plugins.hoster.LewdWebNet) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hosterPlugin.login(myBr, account, false);
        }
        myBr.getPage(parameter.getCryptedUrl());
        String title = myBr.getRegex("<meta property=\"og:title\" content=\"([^\"]+)").getMatch(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        while (fetchNext) {
            if (this.isAbort()) {
                return result;
            }
            try {
                String url = parameter.getCryptedUrl();
                if (pageNr > 1) {
                    url += "page-" + pageNr;
                }
                myBr.getPage(url);
                String[][] matches = myBr.getRegex("<a class=\"file-preview js-lbImage\" href=\"/attachments/([^\"]+)\\.(\\d+)/\"").getMatches();
                for (String[] match : matches) {
                    DownloadLink dl = this.createDownloadlink("directhttp://https://forum.lewdweb.net/attachments/" + match[0] + "." + match[1] + "/");
                    dl.setName(match[1] + "-" + match[0]);
                    fp.add(dl);
                    result.add(dl);
                    this.distribute(dl);
                }
                matches = myBr.getRegex("<img src=\".+/attachments/([^\"]+)/\"").getMatches();
                for (String[] match : matches) {
                    DownloadLink dl = this.createDownloadlink("directhttp://https://forum.lewdweb.net/attachments/" + match[0] + "/");
                    dl.setName(match[0]);
                    fp.add(dl);
                    result.add(dl);
                    this.distribute(dl);
                }
                matches = myBr.getRegex("<a href=\"([^\"]+)\"[\\s\\r\\n\\w=\"]+class=\"link").getMatches();
                for (String[] match : matches) {
                    DownloadLink dl = this.createDownloadlink(match[0]);
                    result.add(dl);
                    this.distribute(dl);
                }
                if (myBr.getHttpConnection().getResponseCode() != 200) {
                    fetchNext = false;
                    break;
                }
                pageNr++;
            } catch (Exception e) {
                fetchNext = false;
            }
        }
        return result;
    }
}
