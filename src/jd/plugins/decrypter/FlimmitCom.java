//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flimmit.com" }, urls = { "https?://flimmit\\.at/([a-z0-9\\-]+)/assets/(\\d+)" })
public class FlimmitCom extends PluginForDecrypt {
    public FlimmitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String titleURL = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String contentID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        login();
        br.setFollowRedirects(true);
        br.getPage("https://flimmit.at/dynamically/video/" + contentID);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        // final String httpURL = (String) JavaScriptEngineFactory.walkJson(entries, "data/config/progressive");
        // we get hls since thats all we support at this stage.
        final String error = (String) JavaScriptEngineFactory.walkJson(entries, "data/error");
        /* Content is not a video object --> Maybe Series */
        if ("Video not found.".equalsIgnoreCase(error)) {
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] assetIDs = br.getRegex("/assets..?(\\d+)").getColumn(0);
            for (final String assetID : assetIDs) {
                if (assetIDs.equals(contentID)) {
                    /* Do not add currently processed URL again! */
                    continue;
                } else {
                    decryptedLinks.add(this.createDownloadlink("https://flimmit.at/" + titleURL + "/assets/" + assetID));
                }
            }
        } else {
            /* Process video object */
            final String m3u = (String) JavaScriptEngineFactory.walkJson(entries, "data/config/hls");
            final String title = (String) JavaScriptEngineFactory.walkJson(entries, "data/modules/titles/title");
            final String description = (String) JavaScriptEngineFactory.walkJson(entries, "data/modules/titles/description");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(titleURL);
            if (m3u == null) {
                logger.info("Failed to find any downloadable content");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            br.getPage(m3u);
            final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
            for (final HlsContainer quality : qualities) {
                final DownloadLink dl = this.createDownloadlink(quality.getDownloadurl().replaceAll("https?://", "m3u8://"));
                dl.setFinalFileName(titleURL + "_" + title + "_" + quality.getStandardFilename());
                dl.setAvailable(true);
                dl.setComment(description);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
        }
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
            throw new AccountRequiredException();
        }
        final PluginForHost plugin = JDUtilities.getPluginForHost("flimmit.com");
        if (plugin == null) {
            throw new IllegalStateException("flimmit hoster plugin not found!");
        }
        // set cross browser support
        ((jd.plugins.hoster.FlimmitCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.FlimmitCom) plugin).login(account, false);
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }
}