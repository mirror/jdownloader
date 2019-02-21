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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "swisstransfer.com" }, urls = { "https?://(?:www\\.)?swisstransfer\\.com/d/([a-z0-9\\-]+)" })
public class SwisstransferComFolder extends antiDDoSForDecrypt {
    public SwisstransferComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkUUID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getHeaders().put("accept", "application/json, text/plain, */*");
        postPage("https://www." + this.getHost() + "/api/isPasswordValid", "linkUUID=" + linkUUID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. response "e034b988-de97-4333-956b-28ba66ed88888 Not found" (with "") */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("container");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("files");
        String fpName = (String) entries.get("message");
        FilePackage fp = null;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        int offset = 0;
        int page = 0;
        final int maxItemsPerRequest = 1;
        boolean hasNext = false;
        do {
            if (this.isAbort()) {
                break;
            }
            // getPage("");
            // if (br.getHttpConnection().getResponseCode() == 404) {
            // decryptedLinks.add(this.createOfflinelink(parameter));
            // return decryptedLinks;
            // }
            for (final Object fileO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) fileO;
                final String filename = (String) entries.get("fileName");
                final String fileid = (String) entries.get("UUID");
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("fileSizeInBytes"), 0);
                if (StringUtils.isEmpty(filename) || StringUtils.isEmpty(fileid)) {
                    continue;
                }
                final DownloadLink dl = createDownloadlink(String.format("directhttp://https://www.swisstransfer.com/api/download/%s/%s", linkUUID, fileid));
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                }
                dl.setFinalFileName(filename);
                dl.setAvailable(true);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
                distribute(dl);
                offset++;
            }
            page++;
        } while (hasNext);
        return decryptedLinks;
    }
}
