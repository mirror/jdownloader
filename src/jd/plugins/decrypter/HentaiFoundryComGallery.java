//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentai-foundry.com" }, urls = { "https?://(?:www\\.)?hentai-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+(?:/scraps)?(?:/\\d+)?|https?://(?:www\\.)?hentai-foundry\\.com/user/[A-Za-z0-9\\-_]+/faves/pictures" })
public class HentaiFoundryComGallery extends PluginForDecrypt {

    public HentaiFoundryComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        getUserLogin(false);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (new Regex(parameter, Pattern.compile(".+/pictures/user/[A-Za-z0-9\\-_]+/\\d+", Pattern.CASE_INSENSITIVE)).matches()) {
            decryptedLinks.add(createDownloadlink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter + "?enterAgree=1&size=0");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("class=\"empty\"")) {
            /* User has not uploaded any content */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = new Regex(parameter, "/user/(.+)").getMatch(0);
        int page = 1;
        String next = null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return decryptedLinks;
            }
            logger.info("Decrypting page " + page);
            if (page > 1) {
                br.getPage(next);
            }
            String[] links = br.getRegex("<[^<>]+class='thumb_square'.*?</").getColumn(-1);
            if (links == null || links.length == 0) {
                return null;
            }
            for (String link : links) {
                String title = new Regex(link, "thumbTitle\"><[^<>]*?>([^<>]+)<").getMatch(0);
                final String url = new Regex(link, "\"(/pictures/user/[A-Za-z0-9\\-_]+/\\d+[^<>\"]*?)\"").getMatch(0);
                if (title == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    logger.info("link: " + link);
                    logger.info("title: " + title + "url: " + url);
                    return null;
                }
                final String pic_id = jd.plugins.hoster.HentaiFoundryCom.getFID(url);
                title = pic_id + "_" + Encoding.htmlDecode(title).trim();
                title = encodeUnicode(title);
                final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
                dl.setName(title);
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.BMP);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            next = br.getRegex("class=\"next\"><a href=\"(/pictures/user/[A-Za-z0-9\\-_]+/page/\\d+)\"").getMatch(0);
            page++;
        } while (next != null);

        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, continuing without logging in (if possible)");
            return false;
        }
        try {
            jd.plugins.hoster.HentaiFoundryCom.login(br, aa, force);
        } catch (final PluginException e) {
            logger.warning("Login failed - continuing without login");
            aa.setValid(false);
            return false;
        }
        logger.info("Logged in successfully");
        return true;
    }

}
