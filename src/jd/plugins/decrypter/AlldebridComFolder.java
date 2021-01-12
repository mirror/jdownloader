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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.hoster.AllDebridCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AlldebridComFolder extends PluginForDecrypt {
    public AlldebridComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "alldebrid.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/getMagnets/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /*
         * Important: Every account has its own files. A magnetID generated inside account A will not work in account B! Alldebrid support
         * knows about this issue and is thinking about adding a modifier into their magnetURL so that we know which account to use in case
         * the user owns multiple alldebrid.com accounts.
         */
        final String magnetID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final Account account = AccountController.getInstance().getValidAccount("alldebrid.com");
        if (account == null) {
            throw new AccountRequiredException();
        }
        /* API docs: https://docs.alldebrid.com/#status */
        final UrlQuery query = new UrlQuery();
        query.appendEncoded("agent", AllDebridCom.agent_raw);
        query.appendEncoded("apikey", AllDebridCom.getStoredApiKey(account));
        query.add("id", magnetID);
        br.getPage(AllDebridCom.api_base + "/magnet/status?" + query.toString());
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /* Don't care which error happens - always treat it as an offline file! */
        if (entries.containsKey("error")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Object magnetsO = JavaScriptEngineFactory.walkJson(entries, "data/magnets");
        if (magnetsO instanceof List) {
            final List<Object> tmpList = (List<Object>) magnetsO;
            entries = (Map<String, Object>) tmpList.get(0);
        } else {
            entries = (Map<String, Object>) magnetsO;
        }
        String folderRootName = (String) entries.get("filename");
        final List<Object> linksO = (List<Object>) entries.get("links");
        if (linksO.isEmpty()) {
            /* Probably unfinished torrent download */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        for (final Object linkO : linksO) {
            entries = (Map<String, Object>) linkO;
            /*
             * Most times these will be uptobox.com URLs. Sometimes .rar files which contain parts/"the rest" of the initial torrent folder
             * structure.
             */
            final String url = (String) entries.get("link");
            final String filename = (String) entries.get("filename");
            final long filesize = ((Number) entries.get("size")).longValue();
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, folderRootName);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(folderRootName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
