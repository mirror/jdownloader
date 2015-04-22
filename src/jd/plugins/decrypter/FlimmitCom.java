//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 29087 $", interfaceVersion = 2, names = { "flimmit.com" }, urls = { "https?://(?:www\\.)?flimmit\\.com/(?:catalog/product/view/id/|video/stream/play/(?:product_id/|order_item/))(\\d+)" }, flags = { 2 })
public class FlimmitCom extends PluginForDecrypt {

    public FlimmitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (true) {
            // segmented dash or segmented hls, we don't support either.
            return decryptedLinks;
        }
        String parameter = param.toString();
        final String vid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        login();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // support via mpd / mobilempd / hls
        // we get hls since thats all we support at this stage.
        final String m3u = getJson("hls");
        if (m3u == null) {
            return null;
        }
        String filename = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (filename == null) {
            return null;
        }
        br.getPage(m3u);
        final String[] medias = br.getRegex("#EXT-X.*?\\.m3u8").getColumn(-1);
        if (medias == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String media : medias) {
            // segmented audio and segmented video in differnet objects. dl both and then mux?

            // audio is first
            if (media.contains("TYPE=AUDIO")) {
                continue;
            }
            final String res = new Regex(media, "RESOLUTION=\\d+x(\\d+)").getMatch(0);
            final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
            final String m3u8 = new Regex(media, "(?:\n|\")([^\n\"]+\\.m3u8)").getMatch(0);

            final DownloadLink dlink = createDownloadlink("http://flimmit.com/" + System.currentTimeMillis() + new Random().nextInt(100000000));
            dlink.setProperty("filename", filename);
            dlink.setProperty("ext", media);
            dlink.setProperty("m3uVideo", br.getBaseURL() + m3u8);
            dlink.setProperty("vid", vid);
            dlink.setProperty("res", res);
            final String linkID = "flimmit:" + vid + ":HLS:" + bw;
            try {
                dlink.setLinkID(linkID);
            } catch (final Throwable t) {
                dlink.setProperty("LINKDUPEID", linkID);
            }
            // let linkchecking routine do all this!
            // final String formattedFilename = jd.plugins.hoster.JustinTv.getFormattedFilename(dlink);
            // dlink.setName(formattedFilename);
            dlink.setName("linkcheck-failed-recheck-online-status-manually.mp4");
            try {
                dlink.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            decryptedLinks.add(dlink);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public void login() throws Exception {
        // we need an account
        Account account = null;
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
        if (accounts != null && accounts.size() != 0) {
            // lets sort, premium > non premium
            Collections.sort(accounts, new Comparator<Account>() {
                @Override
                public int compare(Account o1, Account o2) {
                    final int io1 = o1.getBooleanProperty("free", false) ? 0 : 1;
                    final int io2 = o2.getBooleanProperty("free", false) ? 0 : 1;
                    return io1 <= io2 ? io1 : io2;
                }
            });
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }
        if (account == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Account Required");
        }
        final PluginForHost plugin = JDUtilities.getPluginForHost("flimmit.com");
        if (plugin == null) {
            throw new IllegalStateException("flimmit hoster plugin not found!");
        }
        // set cross browser support
        ((jd.plugins.hoster.FlimmitCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.FlimmitCom) plugin).login(account, false);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

}