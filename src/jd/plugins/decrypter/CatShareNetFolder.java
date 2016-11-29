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
import java.util.HashMap;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "catshare.net" }, urls = { "https?://(?:www\\.)?catshare\\.net/folder/[A-Za-z0-9]+" })
public class CatShareNetFolder extends PluginForDecrypt {

    public CatShareNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);

        postPage(jd.plugins.hoster.CatShareNet.getAPIProtocol() + this.getHost() + "/folder/json_folder", " folder_hash=" + folderid);

        if (br.getHttpConnection().getResponseCode() == 404 || jd.plugins.hoster.CatShareNet.getAPIStatuscode(this.br) != jd.plugins.hoster.CatShareNet.api_status_all_ok) {
            /* If a folder doesn't exist we will get error code 2 with error "Brak folderu" */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        /* 2016-06-09: The admin uses json Arrays in a wrong way but this plugin should work ... as long as he doesn't change the json! */
        // final ArrayList<Object> ressourcelist = (ArrayList<Object>)
        // JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());

        final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        HashMap<String, Object> entries_tmp = null;
        Object entries_tmp_o = null;

        String fpName = (String) entries.get("folder_name");
        if (fpName == null) {
            fpName = "catshare.net folder " + folderid;
        }

        int counter = 0;
        do {
            entries_tmp_o = entries.get(Integer.toString(counter));
            if (entries_tmp_o == null) {
                break;
            }

            entries_tmp = (HashMap<String, Object>) entries_tmp_o;
            final String linkid = (String) entries_tmp.get("linkid");
            final String filename = (String) entries_tmp.get("filename");
            final long filesize = JavaScriptEngineFactory.toLong(entries_tmp.get("filesize"), 0);
            if (linkid == null || filename == null) {
                /* This should never happen */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("http://catshare.net/" + linkid);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        } while (true);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private PluginForHost plugin = null;

    private void postPage(final String url, final String arg) throws Exception {
        postPage(br, url, arg);
    }

    private void postPage(final Browser br, final String url, final String arg) throws Exception {
        loadPlugin();
        ((jd.plugins.hoster.CatShareNet) plugin).setBrowser(br);
        ((jd.plugins.hoster.CatShareNet) plugin).postPage(url, arg);
    }

    public void loadPlugin() {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("catshare.net");
            if (plugin == null) {
                throw new IllegalStateException(getHost() + " hoster plugin not found!");
            }
        }
    }

}
