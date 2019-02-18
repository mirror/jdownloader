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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https?://(?:www\\.)?artstation\\.com/((?:artist|artwork)/[^/]+|(?!about|marketplace|jobs|contests|blogs|users)[^/]+)" })
public class ArtstationCom extends antiDDoSForDecrypt {
    public ArtstationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ARTIST = "(?i)https?://(?:www\\.)?artstation\\.com/artist/[^/]+";
    private static final String TYPE_ALBUM  = "(?i)https?://(?:www\\.)?artstation\\.com/artwork/[a-zA-Z0-9]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            /* Login whenever possible - this may unlock some otherwise hidden user content. */
            jd.plugins.hoster.ArtstationCom.login(this.br, aa, false);
        }
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
        if (parameter.matches(TYPE_ALBUM)) {
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
                            // Handle Marmoset 3D content
                            if (result.endsWith(fid) && StringUtils.containsIgnoreCase(url, "/marmosets/")) {
                                final Browser br2 = br.cloneBrowser();
                                String pageMarmo = br2.getPage(result);
                                if (br2.containsHTML("\"asset_type\":\"marmoset\"") && br2.containsHTML("\"attachment_content_type\":\"application/octet-stream\"")) {
                                    String assetURLRoot = br2.getRegex("\"(https?://[^\"]+original/)").getMatch(0).toString().replace("/images/", "/attachments/");
                                    String assetFileName = br2.getRegex("\"attachment_file_name\":\"([^\"]+)\"").getMatch(0).toString();
                                    String assetFileTimeStamp = br2.getRegex("\"attachment_updated_at\":([0-9]+)").getMatch(0).toString();
                                    String assetURL = assetURLRoot + assetFileName + "?" + assetFileTimeStamp;
                                    final DownloadLink dl2 = this.createDownloadlink(assetURL);
                                    fp.add(dl2);
                                    decryptedLinks.add(dl2);
                                }
                            } else {
                                final DownloadLink dl = this.createDownloadlink(result);
                                fp.add(dl);
                                decryptedLinks.add(dl);
                            }
                        }
                    }
                }
                if (fid.equals("-1") || url == null || Boolean.FALSE.equals(hasImage)) {
                    continue;
                }
                if (isAbort()) {
                    break;
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
        } else if (parameter.matches(TYPE_ARTIST) || true) {
            final String username = parameter.substring(parameter.lastIndexOf("/") + 1);
            jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
            getPage("https://www.artstation.com/users/" + username + ".json");
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                return decryptedLinks;
            }
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
                    if (isAbort()) {
                        break;
                    }
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
        }
        return decryptedLinks;
    }
}
