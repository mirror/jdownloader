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

@DecrypterPlugin(revision = "$Revision: 1 $", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://x-art\\.com/members/videos/[a-zA0-9\\-\\_]+/$" }, flags = { 0 })
public class XArt extends PluginForDecrypt {

    public XArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("x-art.com");
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
                useAcc.setEnabled(false);
                throw new DecrypterException("Account invalid!");
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        String links[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*)\\.(mp4|wmv|mov)\"").getColumn(0);
        String ext[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*)\\.(mp4|wmv|mov)\"").getColumn(1);
        for (int n = 0; n < links.length; n++) {
            String fulllink = parameter.toString() + links[n] + "." + ext[n];
            DownloadLink dl = createDownloadlink(Encoding.htmlDecode(fulllink));
            ret.add(dl);
        }
        String title = br.getRegex("<h1>([a-zA-Z0-9\\_\\-\\ ]*)<\\/h1>").getMatch(0);
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName("XArt Movie: " + title);
            fp.addLinks(ret);
        }
        return ret;
    }
}
