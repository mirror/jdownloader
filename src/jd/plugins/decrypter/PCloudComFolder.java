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
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcloud.com" }, urls = { "https?://(www\\.)?(my\\.pcloud\\.com/#page=publink\\&code=|my\\.pcloud\\.com/publink/show\\?code=|pc\\.cd/)[A-Za-z0-9]+" })
public class PCloudComFolder extends PluginForDecrypt {

    public PCloudComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     DOWNLOAD_ZIP   = "DOWNLOAD_ZIP_2";
    long                            totalSize      = 0;
    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String                  parameter      = null;
    private String                  foldercode     = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        parameter = param.toString();
        foldercode = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        final DownloadLink main = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        main.setProperty("plain_code", foldercode);
        main.setProperty("mainlink", parameter);

        prepBR();
        br.getPage("http://api.pcloud.com/showpublink?code=" + getFID(parameter));
        final String result = PluginJSonUtils.getJsonValue(this.br, "result");
        /* 7002 = deleted by the owner, 7003 = abused */
        if (br.containsHTML("\"error\": \"Invalid link") || "7002".equals(result) || "7003".equals(result)) {
            main.setFinalFileName(new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            main.setContentUrl(parameter);
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("metadata");
        final String folderNameMain = (String) entries.get("name");
        addFolder(entries, null);

        if (decryptedLinks.size() > 1 && SubConfiguration.getConfig(this.getHost()).getBooleanProperty(DOWNLOAD_ZIP, false)) {
            /* = all files (links) of the folder as .zip archive */
            final String main_name = folderNameMain + ".zip";
            main.setFinalFileName(folderNameMain);
            main.setProperty("plain_name", main_name);
            main.setProperty("plain_size", Long.toString(totalSize));
            main.setProperty("complete_folder", true);
            main.setProperty("plain_code", foldercode);
            decryptedLinks.add(main);
        }

        return decryptedLinks;
    }

    /** Recursive function to crawl all folders/subfolders */
    @SuppressWarnings("unchecked")
    private void addFolder(final LinkedHashMap<String, Object> entries, String lastFpname) {
        ArrayList<Object> ressourcelist_temp = null;
        final boolean isFolder = ((Boolean) entries.get("isfolder"));
        if (isFolder) {
            /* Only update lastFoldername if we actually have a folder ... */
            lastFpname = (String) entries.get("name");
            ressourcelist_temp = (ArrayList<Object>) entries.get("contents");
            for (final Object ressorceo : ressourcelist_temp) {
                final LinkedHashMap<String, Object> tempmap = (LinkedHashMap<String, Object>) ressorceo;
                addFolder(tempmap, lastFpname);
            }
        } else {
            addSingleItem(entries, lastFpname);
        }
    }

    private void addSingleItem(final LinkedHashMap<String, Object> entries, final String fpName) {
        final DownloadLink dl = createDownloadlink("http://pclouddecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        String filename = (String) entries.get("name");
        final long fileid = JavaScriptEngineFactory.toLong(entries.get("fileid"), 0);
        if (filename == null || fileid == 0) {
            return;
        }
        filename = Encoding.htmlDecode(filename.trim());
        totalSize += filesize;
        dl.setDownloadSize(filesize);
        dl.setFinalFileName(filename);
        dl.setProperty("plain_name", filename);
        dl.setProperty("plain_size", filesize);
        dl.setProperty("mainlink", parameter);
        dl.setProperty("plain_fileid", fileid);
        dl.setProperty("plain_code", foldercode);
        dl.setLinkID(foldercode + fileid);
        dl.setAvailable(true);
        dl.setContentUrl(parameter);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            dl._setFilePackage(fp);
        }
        decryptedLinks.add(dl);
    }

    private String getFID(final String link) {
        return new Regex(link, "([A-Za-z0-9]+)$").getMatch(0);
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

}
