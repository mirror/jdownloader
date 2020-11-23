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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "furaffinity.net" }, urls = { "https?://(?:www\\.)?furaffinity\\.net/(?:gallery|scraps)/([^/]+)" })
public class FuraffinityNet extends PluginForDecrypt {
    public FuraffinityNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-08-19: Avoid 503 rate limit */
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String username = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        int page = 1;
        boolean hasNextPage = false;
        /* Login if account is available */
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null) {
            final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.FuraffinityNet) plg).login(acc, false);
        }
        do {
            logger.info("Crawling page " + page);
            br.getPage(parameter + "/" + page);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*System Message")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String json = br.getRegex("var descriptions = (\\{.*?\\});").getMatch(0);
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
            int itemsCounter = 0;
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final String itemID = entry.getKey();
                final Map<String, Object> itemProperties = (Map<String, Object>) entry.getValue();
                String title = (String) itemProperties.get("title");
                final String description = (String) itemProperties.get("description");
                if (StringUtils.isEmpty(title)) {
                    /* Fallback */
                    title = itemID;
                }
                final DownloadLink dl = this.createDownloadlink("https://www.furaffinity.net/view/" + itemID);
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
                dl.setName(title);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(description);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                distribute(dl);
                itemsCounter += 1;
            }
            logger.info("Number of items on current page: " + itemsCounter);
            page++;
            hasNextPage = br.containsHTML("/" + username + "/" + page);
        } while (!this.isAbort() && hasNextPage);
        return decryptedLinks;
    }
}
