//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-art.com" }, urls = { "^https?://(www\\.)?x-art\\.com/(members/)?.+" }, flags = { 0 })
public class XArt extends PluginForDecrypt {

    public XArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        link = link.replaceFirst("x\\-art\\.com", "x-artdecrypted.com");
        DownloadLink ret = super.createDownloadlink(link);
        try {
            ret.setContentUrl(link);
        } catch (Throwable e) {

        }
        return ret;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            public boolean add(DownloadLink e) {
                try {
                    distribute(e);
                } catch (Throwable ee) {

                }
                return super.add(e);
            };

            @Override
            public boolean addAll(Collection<? extends DownloadLink> c) {
                for (DownloadLink d : c) {
                    add(d);
                }
                return true;
            }
        };
        String url = parameter.getCryptedUrl();

        if (!url.contains("/members/")) {
            url = url.replaceAll("x\\-art\\.com\\/", "x-art.com/members/");
        }
        if (url.matches("(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)")) {
            ret.add(createDownloadlink(url));
            return ret;
        }
        boolean prem = false;
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("x-art.com");
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    prem = this.login(n);
                    break;
                }
            }
        }
        if (!prem) {
            logger.warning("You need to use an Account with this provider");
            return ret;
        }
        if (url.matches("^https?://(www\\.)?x-art\\.com/(members/)?(videos|galleries)/[a-zA0-9\\-\\_]+/?$")) {
            parseUrl(ret, url);
        } else {
            br.getPage(url);
            String[] sub = br.getRegex("(https?://(www\\.)?x-art\\.com/(members/)?(videos|galleries)/[a-zA0-9\\-\\_]+/?)").getColumn(0);
            HashSet<String> dupe = new HashSet<String>();
            for (String s : sub) {
                try {
                    if (isAbort()) {
                        return ret;
                    }
                } catch (Throwable ee) {

                }

                if (dupe.add(s)) {
                    parseUrl(ret, s);
                }
            }

        }
        return ret;
    }

    public void parseUrl(ArrayList<DownloadLink> ret, String url) throws IOException {
        br.setFollowRedirects(true);

        br.getPage(url);
        ArrayList<DownloadLink> dlinks = new ArrayList<DownloadLink>();
        String links[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*\\.(mp4|wmv|mov|zip))\"").getColumn(0);
        String lnks[] = br.getRegex("href=(\"|')(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)").getColumn(1);
        if (links != null) {
            for (String link : links) {
                String fulllink = br.getURL() + link;
                DownloadLink dl = createDownloadlink(fulllink);
                dlinks.add(dl);
            }
        }
        if (lnks != null) {
            for (String link : lnks) {
                DownloadLink dl = createDownloadlink((link));
                dlinks.add(dl);
            }
        }
        ret.addAll(dlinks);
        String title = br.getRegex("<h1>([a-zA-Z0-9\\_\\-\\ ]*)<\\/h1>").getMatch(0);
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(dlinks);
        }
    }

    private boolean login(final Account account) throws Exception {
        this.setBrowserExclusive();
        final PluginForHost plugin = JDUtilities.getPluginForHost("x-art.com");
        try {
            if (plugin != null) {
                ((jd.plugins.hoster.XArtCom) plugin).login(account, br, false);
            } else {
                return false;
            }
        } catch (final PluginException e) {

            account.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}