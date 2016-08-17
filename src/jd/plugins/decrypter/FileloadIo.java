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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fileload.io" }, urls = { "https?://(?:www\\.)?fileload\\.io/[A-Za-z0-9]+(/[^/]+)?" }) 
public class FileloadIo extends PluginForDecrypt {

    public FileloadIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* TODO: Remove website dependancy to fully use API! */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        jd.plugins.hoster.FileloadIo.prepBRAPI(this.br);
        final String folder_url_part = new Regex(parameter, "https?://[^/]+/(.+)").getMatch(0);
        final String folderid = new Regex(parameter, "https?://[^/]+/([A-Za-z0-9]+)").getMatch(0);
        jd.plugins.hoster.FileloadIo.prepBRWebsite(this.br);

        String[] free_download_fileids = null;
        if (!jd.plugins.hoster.FileloadIo.USE_API_FOR_FREE_UNREGISTERED_DOWNLOADS) {
            /* Important: Access main-folder to find all free_download_fileids for non-API mode!! */
            br.getPage("https://" + this.getHost() + "/" + folderid);
            if (jd.plugins.hoster.FileloadIo.mainlinkIsOffline(this.br)) {
                /* Folder offline */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (this.br.containsHTML("Please wait a moment while the files are being prepared for download|The page will reload automatically once the files are ready")) {
                /* Happens directly after uploading new files to this host. */
                logger.info("There is nothing to download YET ...");
                return decryptedLinks;
            }
            /* Find ids needed for free download via website. */
            free_download_fileids = br.getRegex("data\\-fileid=\"(\\d+)\"").getColumn(0);
        }

        int counter = 0;
        br.getPage("https://api." + this.getHost() + "/onlinestatus/" + folder_url_part);
        final String error = PluginJSonUtils.getJson(this.br, "error");
        if (error != null) {
            logger.info("Folder seems to be offline for reason: " + error);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = null;
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        if (ressourcelist.size() == 0) {
            /* Folder offline */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        for (final Object linko : ressourcelist) {
            if (counter > ressourcelist.size() - 1) {
                /* Last element of API-array is .zip for complete folder --> Ignore that! */
                break;
            }
            entries = (LinkedHashMap<String, Object>) linko;
            final String free_download_fileid;
            if (free_download_fileids != null && counter <= free_download_fileids.length - 1) {
                free_download_fileid = free_download_fileids[counter];
            } else {
                /* Free download impossible or our website handling/RegEx above failed ... */
                free_download_fileid = null;
            }
            final String filename = (String) entries.get("filename");
            final String status = (String) entries.get("status");
            final String content_url = (String) entries.get("link_single");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("filesize_bytes"), 0);
            final String sha1 = (String) entries.get("sha1");

            if (filename == null || content_url == null) {
                continue;
            }

            final DownloadLink dl = createDownloadlink(content_url.replace("fileload.io/", "fileloaddecrypted.io/"));
            dl.setContentUrl(content_url);
            dl.setFinalFileName(filename);
            dl.setProperty("directfilename", filename);
            dl.setDownloadSize(filesize);
            dl.setLinkID(folderid + "/" + filename);
            if ("online".equalsIgnoreCase(status)) {
                dl.setAvailable(true);
            } else {
                dl.setAvailable(false);
            }
            if (content_url != null) {
                dl.setContentUrl(content_url);
            }
            if (sha1 != null) {
                dl.setSha1Hash(sha1);
            }
            if (free_download_fileid != null) {
                dl.setProperty("free_download_fileid", free_download_fileid);
            }
            decryptedLinks.add(dl);
            counter++;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName("fileload.io folder " + folderid);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
