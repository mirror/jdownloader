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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DummyScriptEnginePlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "catshare.net" }, urls = { "https?://(?:www\\.)?catshare\\.net/folder/[A-Za-z0-9]+" }, flags = { 0 })
public class CatShareNetFolder extends PluginForDecrypt {

    public CatShareNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);

        jd.plugins.hoster.CatShareNet.prepBRAPI(this.br);
        br.postPage(jd.plugins.hoster.CatShareNet.getAPIProtocol() + this.getHost() + "/folder/json_folder", " folder_hash=" + folderid);

        if (br.getHttpConnection().getResponseCode() == 404 || jd.plugins.hoster.CatShareNet.getAPIStatuscode(this.br) != jd.plugins.hoster.CatShareNet.api_status_all_ok) {
            /* If a folder doesn't exist we will get error code 2 with error "Brak folderu" */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        HashMap<String, Object> entries = null;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(this.br.toString());
        for (final Object fileo : ressourcelist) {
            entries = (HashMap<String, Object>) fileo;
            final String linkid = (String) entries.get("linkid");
            final String filename = (String) entries.get("filename");
            final long filesize = DummyScriptEnginePlugin.toLong(entries.get("filesize"), 0);
            if (linkid == null || filename == null) {
                /* This should never happen */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("http://catshare.net/" + linkid);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName("catshare.net folder " + folderid);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
