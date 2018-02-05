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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fshare.vn" }, urls = { "https?://(?:www\\.)?fshare\\.vn/folder/([A-Z0-9]+)" })
public class FShareVnFolder extends PluginForDecrypt {
    public FShareVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String folderid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        jd.plugins.hoster.FShareVn.prepBrowser(this.br);
        /* Important or we'll get XML ;) */
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("https://www." + this.getHost() + "/api/v3/files/folder?linkcode=" + folderid + "&sort=type,name");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        LinkedHashMap<String, Object> entries2;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("items");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        } else if (ressourcelist.isEmpty()) {
            logger.info("Empty folder");
            return decryptedLinks;
        }
        entries = (LinkedHashMap<String, Object>) entries.get("current");
        String fpName = (String) entries.get("name");
        if (StringUtils.isEmpty(fpName)) {
            fpName = folderid;
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName.trim());
        for (final Object linkO : ressourcelist) {
            entries2 = (LinkedHashMap<String, Object>) linkO;
            // final String path = (String) entries2.get("path");
            final String linkcode = (String) entries2.get("linkcode");
            final String filename = (String) entries2.get("name");
            final long filesize = JavaScriptEngineFactory.toLong(entries2.get("size"), 0);
            if (StringUtils.isEmpty(linkcode)) {
                /* This should never happen */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("https://www." + this.getHost() + "/file/" + linkcode);
            dl.setLinkID(linkcode);
            if (filesize > 0) {
                /* Should always be the case. */
                dl.setDownloadSize(filesize);
            }
            dl.setName(filename);
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
            distribute(dl);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}