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

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptionsModifier;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.ArtstationCom;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https?://(?:www\\.)?artstation\\.com/((?:artist|artwork)/[^/]+|(?!about|marketplace|jobs|contests|blogs|users)[^/]+(/likes)?)" })
public class ArtstationComCrawler extends antiDDoSForDecrypt {
    public ArtstationComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ARTIST = "(?i)https?://(?:www\\.)?artstation\\.com/artist/[^/]+";
    private static final String TYPE_ALBUM  = "(?i)https?://(?:www\\.)?artstation\\.com/artwork/([a-zA-Z0-9]+)";
    Object                      modifier    = null;

    @Override
    public Browser createNewBrowserInstance() {
        final Browser ret = super.createNewBrowserInstance();
        ret.setSSLSocketStreamOptions(new SSLSocketStreamOptionsModifier() {
            @Override
            public SSLSocketStreamOptions modify(SSLSocketStreamOptions sslSocketStreamOptions, HTTPConnection httpConnection) {
                // may avoid cloudflare
                sslSocketStreamOptions.getDisabledCipherSuites().clear();
                sslSocketStreamOptions.getCustomFactorySettings().add("JSSE_TLS1.3_ENABLED");
                sslSocketStreamOptions.getCustomFactorySettings().add("BC_TLS1.3_ENABLED");
                return sslSocketStreamOptions;
            }
        });
        return ret;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("http:", "https:");
        final Account aa = AccountController.getInstance().getValidAccount(this.getHost());
        if (aa != null) {
            /* Login whenever possible - this may unlock some otherwise hidden user content. */
            try {
                jd.plugins.hoster.ArtstationCom.login(this.br, aa, false);
            } catch (PluginException e) {
                handleAccountException(aa, e);
            }
        }
        if (br.getURL() == null) {
            // getPage("https://www.artstation.com/");
        }
        try {
            getPage(parameter);
        } catch (PluginException e) {
            // we can still access the json api
            logger.log(e);
            br.setCurrentURL(parameter);
        }
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowInheritance(true);
        if (parameter.matches(TYPE_ALBUM)) {
            final String project_id = new Regex(parameter, TYPE_ALBUM).getMatch(0);
            if (inValidate(project_id)) {
                return ret;
            }
            jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
            getPage("https://www.artstation.com/projects/" + project_id + ".json");
            final Map<String, Object> json = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final List<Object> resource_data_list = (List<Object>) json.get("assets");
            final String full_name = (String) JavaScriptEngineFactory.walkJson(json, "user/full_name");
            final String username = (String) JavaScriptEngineFactory.walkJson(json, "user/username");
            final String projectTitle = Encoding.htmlOnlyDecode((String) json.get("title"));
            for (final Object jsono : resource_data_list) {
                if (isAbort()) {
                    break;
                }
                final Map<String, Object> imageJson = (Map<String, Object>) jsono;
                String url = (String) imageJson.get("image_url");
                final long width = JavaScriptEngineFactory.toLong(imageJson.get("width"), -1l);
                final String fid = Long.toString(JavaScriptEngineFactory.toLong(imageJson.get("id"), -1));
                final String asset_type = (String) imageJson.get("asset_type");
                final String imageTitle = (String) imageJson.get("title");
                final Boolean hasImage = (Boolean) imageJson.get("has_image");
                final String playerEmbedded = (String) imageJson.get("player_embedded");
                boolean hasVideo = false;
                PluginForHost pluginArtStationCom = null;
                if (StringUtils.isNotEmpty(playerEmbedded)) {
                    final String[] results = HTMLParser.getHttpLinks(playerEmbedded, null);
                    if (results != null) {
                        for (final String result : results) {
                            String assetURL = result;
                            final Browser br2 = br.cloneBrowser();
                            try {
                                if (result.endsWith(fid) && StringUtils.containsIgnoreCase(playerEmbedded, "<iframe")) {
                                    getPage(br2, result);
                                    String pageDetail = br2.toString();
                                    if (StringUtils.containsIgnoreCase(url, "/marmosets/") && br2.containsHTML("\"asset_type\":\"marmoset\"") && br2.containsHTML("\"attachment_content_type\":\"application/octet-stream\"")) {
                                        // Handle Marmoset 3D content
                                        String assetURLRoot = br2.getRegex("\"(https?://[^\"]+original/)").getMatch(0).toString().replace("/images/", "/attachments/");
                                        String assetFileName = br2.getRegex("\"attachment_file_name\":\"([^\"]+)\"").getMatch(0).toString();
                                        String assetFileTimeStamp = br2.getRegex("\"attachment_updated_at\":([0-9]+)").getMatch(0).toString();
                                        assetURL = assetURLRoot + assetFileName + "?" + assetFileTimeStamp;
                                    } else if (br2.containsHTML("\"asset_type\":\"pano\"")) {
                                        // Handle 3D panorama images
                                        assetURL = br2.getRegex("\"[a-z]+_image_url\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
                                    } else {
                                        assetURL = br2.getRegex("src\\s*=\\s*[\"']*([^\"']*)[\"']*").getMatch(0);
                                    }
                                } else if (result.contains("embed.html")) {
                                    /* 2020-08-25: Embedded video (selfhosted by artstation but requires this extra step to download it) */
                                    getPage(br2, result);
                                    assetURL = br2.getRegex("<source[^>]*src=\"(https?://[^<>\"]+)\"[^>]*type=\"video/mp4\"").getMatch(0);
                                    hasVideo |= StringUtils.isNotEmpty(assetURL);
                                }
                                if (StringUtils.isNotEmpty(assetURL)) {
                                    final DownloadLink dl2 = this.createDownloadlink(assetURL);
                                    if (pluginArtStationCom == null) {
                                        try {
                                            final LazyHostPlugin plugin = HostPluginController.getInstance().get(getHost());
                                            if (plugin != null) {
                                                pluginArtStationCom = getNewPluginInstance(plugin, null);
                                            }
                                        } catch (Exception e) {
                                            logger.log(e);
                                        }
                                    }
                                    if (pluginArtStationCom != null) {
                                        dl2.setDefaultPlugin(pluginArtStationCom);
                                    } else {
                                        dl2.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, getHost());
                                    }
                                    fp.add(dl2);
                                    ret.add(dl2);
                                }
                            } catch (Exception e) {
                                logger.log(e);
                            }
                        }
                    }
                }
                if (fid.equals("-1") || url == null || Boolean.FALSE.equals(hasImage)) {
                    continue;
                } else if (hasVideo || "video_clip".equals(asset_type)) {
                    // skip thumbnai
                    continue;
                } else if (width > 1920 && StringUtils.containsIgnoreCase(url, "/large/") && !StringUtils.containsIgnoreCase(url, "/assets/images/")) {
                    logger.info("Auto 4k for '" + url + "' because width>1920=" + width);
                    url = ArtstationCom.largeTo4k(url);
                }
                String filename = null;
                if (StringUtils.isNotEmpty(imageTitle)) {
                    filename = imageTitle + "_" + fid;
                } else if (StringUtils.isNotEmpty(projectTitle)) {
                    filename = projectTitle + "_" + fid;
                } else {
                    filename = fid;
                }
                if (StringUtils.isNotEmpty(full_name)) {
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
                if (!StringUtils.isEmpty(username)) {
                    /* 2020-07-29: Packagizer property */
                    dl.setProperty("username", username);
                }
                fp.add(dl);
                ret.add(dl);
            }
            String packageName = "";
            if (StringUtils.isNotEmpty(full_name)) {
                packageName = full_name;
            } else {
                packageName = project_id;
            }
            if (ret.size() > 1) {
                if (StringUtils.isNotEmpty(projectTitle)) {
                    packageName = packageName + "-" + projectTitle;
                }
            }
            fp.setName(packageName);
        } else if (parameter.matches(TYPE_ARTIST) || true) {
            final String username = new Regex(parameter, "https?://[^/]+/([^/]+)").getMatch(0);
            jd.plugins.hoster.ArtstationCom.setHeaders(this.br);
            getPage("https://www.artstation.com/users/" + username + ".json");
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                return ret;
            }
            final Map<String, Object> json = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final String full_name_of_username_in_url = (String) json.get("full_name");
            final String projectTitle = (String) json.get("title");
            final short entries_per_page = 50;
            int entries_total = 0;
            int offset = 0;
            int page = 1;
            /* Either all of user or only "likes" */
            String type = "projects";
            if (parameter.endsWith("/likes")) {
                type = "likes";
            }
            do {
                logger.info("Crawling page " + page + " | Offset " + offset);
                getPage("/users/" + username + "/" + type + ".json?randomize=false&page=" + page);
                final Map<String, Object> pageJson = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                if (ret.size() == 0) {
                    /* We're crawling the first page */
                    entries_total = (int) JavaScriptEngineFactory.toLong(pageJson.get("total_count"), 0);
                }
                final List<Object> ressourcelist = (List) pageJson.get("data");
                for (final Object resource : ressourcelist) {
                    final Map<String, Object> imageInfo = (Map<String, Object>) resource;
                    final Map<String, Object> uploaderInfo = (Map<String, Object>) imageInfo.get("user");
                    final String full_name_of_uploader;
                    if (uploaderInfo != null) {
                        /* E.g. when crawling all likes of a user */
                        full_name_of_uploader = (String) uploaderInfo.get("full_name");
                    } else {
                        /* E.g. when crawling all items of a user */
                        full_name_of_uploader = full_name_of_username_in_url;
                    }
                    final String title = (String) imageInfo.get("title");
                    final String id = (String) imageInfo.get("hash_id");
                    final String description = (String) imageInfo.get("description");
                    if (inValidate(id) || inValidate(full_name_of_uploader)) {
                        return null;
                    }
                    final String url_content = "https://artstation.com/artwork/" + id;
                    final DownloadLink dl = createDownloadlink(url_content);
                    String filename;
                    if (StringUtils.isNotEmpty(title)) {
                        filename = full_name_of_uploader + "_" + id + "_" + title + ".jpg";
                    } else {
                        filename = full_name_of_uploader + "_" + id + ".jpg";
                    }
                    filename = encodeUnicode(filename);
                    dl.setContentUrl(url_content);
                    if (description != null) {
                        dl.setComment(description);
                    }
                    dl.setLinkID(id);
                    dl.setName(filename);
                    dl.setProperty("full_name", full_name_of_uploader);
                    // dl.setAvailable(true);
                    fp.add(dl);
                    ret.add(dl);
                    distribute(dl);
                    offset++;
                }
                if (ressourcelist.size() < entries_per_page) {
                    /* Fail safe */
                    logger.info("Stopping because current page " + page + "  contains less items than: " + entries_per_page);
                    break;
                }
                page++;
            } while (!this.isAbort() && ret.size() < entries_total);
            String packageName = "";
            if (StringUtils.isNotEmpty(full_name_of_username_in_url)) {
                packageName = full_name_of_username_in_url;
            }
            if (ret.size() > 1) {
                if (StringUtils.isNotEmpty(projectTitle)) {
                    packageName = packageName + " " + projectTitle;
                }
            }
            fp.setName(packageName);
        }
        return ret;
    }
}
