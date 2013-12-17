package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Iterator;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "met-art.com" }, urls = { "https?://members\\.met-art\\.com/members/model/[a-zA0-9\\-\\_]+/movie/\\d+/[a-zA0-9\\-\\_]+/" }, flags = { 0 })
public class MetArt extends PluginForDecrypt {

    public MetArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("met-art.com");
        Account useAcc = null;
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    useAcc = n;
                    break;
                }
            }
        }
        if (useAcc == null) return ret;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(useAcc.getUser() + ":" + useAcc.getPass()));
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter.toString());
            if (con.getResponseCode() == 401) {
                useAcc.setValid(false);

                throw new DecrypterException("Account invalid!");
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        String links[] = br.getRegex("href=\"(https?://members.met-art.com/members/movie\\..*?)\"").getColumn(0);
        String title = br.getRegex("title>(MetArt.*?)</title").getMatch(0);
        for (String link : links) {
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(link));
            ret.add(dl);
        }
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
        }
        int wtf = 1;
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}