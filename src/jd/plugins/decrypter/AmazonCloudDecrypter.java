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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "amazon.com" }, urls = { "https?://(?:www\\.)?amazon\\.(?:de|es|au|com|com\\.au|co\\.uk|fr|ca)/(gp/|cloud)drive/share(/|\\?).+|https?://(?:www\\.)?amazon\\.com/clouddrive/share.+" })
public class AmazonCloudDecrypter extends PluginForDecrypt {

    public AmazonCloudDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter       = null;
    private String plain_folder_id = null;
    private String plain_domain    = null;
    private String subfolder_id    = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        subfolder_id = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();

        plain_folder_id = new Regex(parameter, "[\\?\\&]s=([A-Za-z0-9\\-_^&]+)").getMatch(0);
        plain_domain = new Regex(parameter, "(amazon\\.(de|es|au|com|com\\.au|co\\.uk|fr|ca))").getMatch(0);
        if (plain_folder_id == null) {

            // there are dummy ?md5=..&name=... links. see below
            plain_folder_id = new Regex(parameter, "share/([A-Za-z0-9\\-_]+)").getMatch(0);
            // linkType https://www.amazon.es/clouddrive/share/THB_Rik<...ID....>
            return handleNewType(decryptedLinks, param, progress);

        } else {
            // Link Type: https://www.amazon.com/clouddrive/share?s=yr-4blQrTV8hRaCZ8zOOPg
            // LinkType: http://www.amazon.com/gp/drive/share/177-7117023-0446256?ie=UTF8&s=yr-4blQrTV8hRaCZ8zOOPg

            final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            main.setProperty("type", "old20140922");
            main.setProperty("plain_folder_id", plain_folder_id);
            main.setProperty("mainlink", parameter);
            main.setProperty("plain_domain", plain_domain);
            main.setContentUrl(" https://www." + plain_domain + "/clouddrive/share?s=" + plain_folder_id);

            decryptedLinks.add(main);
        }

        return decryptedLinks;

    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private ArrayList<DownloadLink> handleNewType(ArrayList<DownloadLink> decryptedLinks, CryptedLink parameter, ProgressController progress) throws Exception {
        LinkedHashMap<String, Object> entries = null;
        prepBR();
        ArrayList<Object> resource_data_list = null;
        String path_decrypted = "";
        final String path_b64 = new Regex(parameter, "&subfolderpath=(.+)").getMatch(0);
        if (path_b64 != null) {
            path_decrypted = Encoding.Base64Decode(path_b64);
        }
        subfolder_id = new Regex(parameter, "/folder/([^/\\&]+)").getMatch(0);
        if (subfolder_id != null) {
            /* Subfolders-/files */
            resource_data_list = jd.plugins.hoster.AmazonCloud.getListFromNode(this.br, this.plain_domain, this.plain_folder_id, this.subfolder_id);
            if (jd.plugins.hoster.AmazonCloud.isOffline(this.br)) {
                decryptedLinks.add(createOfflinelink(this.parameter));
                return decryptedLinks;
            }
        } else {
            final DownloadLink main = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            String requestedFilename = new Regex(parameter.getCryptedUrl(), "name=(.+)").getMatch(0);
            if (requestedFilename != null) {
                requestedFilename = Encoding.urlDecode(requestedFilename, false);
            }
            main.setProperty("plain_folder_id", plain_folder_id);
            main.setProperty("mainlink", parameter);
            main.setContentUrl("https://www." + this.plain_domain + "/clouddrive/share/" + plain_folder_id);

            jd.plugins.hoster.AmazonCloud.accessFolder(this.br, this.plain_domain, this.plain_folder_id);
            if (jd.plugins.hoster.AmazonCloud.isOffline(this.br)) {
                decryptedLinks.add(createOfflinelink(this.parameter));
                return decryptedLinks;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> nodeInfo = jd.plugins.hoster.AmazonCloud.jsonGetNodeInfo(entries);
            final String kind = jd.plugins.hoster.AmazonCloud.jsonGetKind(nodeInfo);
            if (kind.equals(jd.plugins.hoster.AmazonCloud.JSON_KIND_FILE)) {
                resource_data_list = new ArrayList<Object>();
                resource_data_list.add(nodeInfo);
            } else {
                subfolder_id = (String) nodeInfo.get("id");
                if (subfolder_id == null) {
                    return null;
                }
                resource_data_list = jd.plugins.hoster.AmazonCloud.getListFromNode(this.br, this.plain_domain, this.plain_folder_id, this.subfolder_id);
                if (jd.plugins.hoster.AmazonCloud.isOffline(this.br)) {
                    decryptedLinks.add(createOfflinelink(this.parameter));
                    return decryptedLinks;
                }
            }
        }
        for (final Object o : resource_data_list) {
            decryptedLinks.add(crawlSingleObject(o, path_decrypted));
        }

        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    private DownloadLink crawlSingleObject(final Object o, String path_decrypted) throws IOException {
        final String path_encrypted;
        final LinkedHashMap<String, Object> nodeInfo = (LinkedHashMap<String, Object>) o;
        final LinkedHashMap<String, Object> contentProperties = jd.plugins.hoster.AmazonCloud.jsonGetContentProperties(nodeInfo);
        final String kind = jd.plugins.hoster.AmazonCloud.jsonGetKind(nodeInfo);
        String name = jd.plugins.hoster.AmazonCloud.jsonGetName(nodeInfo);
        final String id = (String) nodeInfo.get("id");
        final DownloadLink dl;
        if (kind.equals(jd.plugins.hoster.AmazonCloud.JSON_KIND_FILE)) {
            dl = createDownloadlink("https://amazondecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String finallink = jd.plugins.hoster.AmazonCloud.jsonGetFinallink(nodeInfo);
            final long filesize = ((Number) contentProperties.get("size")).longValue();
            final String md5 = jd.plugins.hoster.AmazonCloud.jsonGetMd5(contentProperties);
            if (finallink == null || md5 == null) {
                return null;
            }
            final String fid = jd.plugins.hoster.AmazonCloud.getLinkid(this.plain_folder_id, md5, name);

            name = Encoding.htmlDecode(name).trim();

            dl.setDownloadSize(filesize);
            dl.setFinalFileName(name);
            dl.setProperty("plain_name", name);
            dl.setProperty("plain_size", filesize);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plain_directlink", finallink);
            dl.setProperty("plain_folder_id", plain_folder_id);
            if (subfolder_id != null) {
                dl.setProperty("subfolder_id", subfolder_id);
            }
            dl.setProperty("plain_domain", plain_domain);
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path_decrypted);
            dl.setAvailable(true);
            dl.setContentUrl(parameter);
            dl.setContainerUrl("https://www." + plain_domain + "/clouddrive/share/" + plain_folder_id);
            dl.setLinkID(fid);
            dl.setMD5Hash(md5);
            if (!inValidate(path_decrypted)) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(path_decrypted.replace("/", "_"));
                dl._setFilePackage(fp);
            }
        } else {
            path_decrypted += "/" + name;
            path_encrypted = Encoding.Base64Encode(path_decrypted);
            /* Add url to crawl subfolders/files - save subfolderpath inside url as their API won't give us that :) */
            dl = createDownloadlink("https://www." + this.plain_domain + "/clouddrive/share/" + plain_folder_id + "/folder/" + id + "&subfolderpath=" + path_encrypted);
        }
        return dl;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
    }

}
