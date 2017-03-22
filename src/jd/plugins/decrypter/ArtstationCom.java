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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https?://(?:www\\.)?artstation\\.com/(?:artist|artwork)/[^/]+" })
public class ArtstationCom extends antiDDoSForDecrypt {

    public ArtstationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ARTIST = "https?://(?:www\\.)?artstation\\.com/artist/[^/]+";
    private static final String TYPE_ALBUM  = "https?://(?:www\\.)?artstation\\.com/artwork/[A-Z0-9]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
        if (parameter.matches(TYPE_ARTIST)) {
            final String username = parameter.substring(parameter.lastIndexOf("/") + 1);
            jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
            getPage("https://www.artstation.com/users/" + username + ".json");
            final LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final String full_name = (String) json.get("full_name");
            final String projectTitle = (String) json.get("title");
            final short entries_per_page = 50;
            int entries_total = (int) JavaScriptEngineFactory.toLong(json.get("projects_count"), 0);
            int offset = 0;
            int page = 1;
            do {
                getPage("/users/" + username + "/projects.json?randomize=false&page=" + page);
                final LinkedHashMap<String, Object> pageJson = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final ArrayList<Object> ressourcelist = (ArrayList) pageJson.get("data");
                for (final Object resource : ressourcelist) {
                    final LinkedHashMap<String, Object> imageJson = (LinkedHashMap<String, Object>) resource;
                    final String title = (String) imageJson.get("title");
                    final String id = (String) imageJson.get("hash_id");
                    final String description = (String) imageJson.get("description");
                    if (inValidate(id)) {
                        return null;
                    }
                    final String url_content = "https://artstation.com/artwork/" + id;
                    final DownloadLink dl = createDownloadlink(url_content);
                    String filename;
                    if (!inValidate(title)) {
                        filename = full_name + "_" + id + "_" + title + ".jpg";
                    } else {
                        filename = full_name + "_" + id + ".jpg";
                    }
                    filename = encodeUnicode(filename);
                    dl.setContentUrl(url_content);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    dl.setLinkID(id);
                    dl.setName(filename);
                    dl.setProperty("full_name", full_name);
                    fp.add(dl);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    offset++;
                }
                if (ressourcelist.size() < entries_per_page) {
                    /* Fail safe */
                    break;
                }
                page++;
            } while (decryptedLinks.size() < entries_total);

            String packageName = "";
            if (!inValidate(full_name)) {
                packageName = full_name;
            }
            if (decryptedLinks.size() > 1) {
                if (!inValidate(projectTitle)) {
                    packageName = packageName + " " + projectTitle;
                }
            }
            fp.setName(packageName);
        } else {
            final String project_id = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            if (inValidate(project_id)) {
                return decryptedLinks;
            }
            jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
            getPage("https://www.artstation.com/projects/" + project_id + ".json");
            final LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> resource_data_list = (ArrayList<Object>) json.get("assets");
            final String full_name = (String) JavaScriptEngineFactory.walkJson(json, "user/full_name");
            final String projectTitle = (String) json.get("title");
            for (final Object jsono : resource_data_list) {
                final LinkedHashMap<String, Object> imageJson = (LinkedHashMap<String, Object>) jsono;
                final String url = (String) imageJson.get("image_url");
                final String fid = Long.toString(JavaScriptEngineFactory.toLong(imageJson.get("id"), -1));
                final String imageTitle = (String) imageJson.get("title");
                final Boolean hasImage = (Boolean) imageJson.get("has_image");
                final String playerEmbedded = (String) imageJson.get("player_embedded");
                if (!inValidate(playerEmbedded)) {
                    final String[] results = HTMLParser.getHttpLinks(playerEmbedded, null);
                    if (results != null) {
                        for (final String result : results) {
                            final DownloadLink dl = this.createDownloadlink(result);
                            fp.add(dl);
                            decryptedLinks.add(dl);
                        }
                    }
                }
                if (fid.equals("-1") || url == null || Boolean.FALSE.equals(hasImage)) {
                    continue;
                }
                String filename = null;
                if (!inValidate(imageTitle)) {
                    filename = imageTitle + "_" + fid;
                } else if (!inValidate(projectTitle)) {
                    filename = projectTitle + "_" + fid;
                } else {
                    filename = fid;
                }
                if (!inValidate(full_name)) {
                    filename = full_name + "_" + filename;
                }
                filename = Encoding.htmlDecode(filename);
                filename = filename.trim();
                filename = encodeUnicode(filename);
                String ext = getFileNameExtensionFromString(url, jd.plugins.hoster.ArtstationCom.default_Extension);
                /* Make sure that we get a correct extension */
                if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
                    ext = jd.plugins.hoster.ArtstationCom.default_Extension;
                }
                if (!filename.endsWith(ext)) {
                    filename += ext;
                }
                final DownloadLink dl = this.createDownloadlink(url);
                dl.setLinkID(getHost() + "://" + project_id + "/" + fid);
                dl.setAvailable(true);
                dl.setFinalFileName(filename);
                dl.setProperty("decrypterfilename", filename);
                fp.add(dl);
                decryptedLinks.add(dl);
            }
            String packageName = "";
            if (!inValidate(full_name)) {
                packageName = full_name;
            } else {
                packageName = project_id;
            }
            if (decryptedLinks.size() > 1) {
                if (!inValidate(projectTitle)) {
                    packageName = packageName + "-" + projectTitle;
                }
            }
            fp.setName(packageName);
        }
        return decryptedLinks;
    }

}
