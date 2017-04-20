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

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "https?://(www\\.)?instagram\\.com/(?!explore/)(p/[A-Za-z0-9_-]+|[^/]+)" })
public class InstaGramComDecrypter extends PluginForDecrypt {

    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String           TYPE_GALLERY           = ".+/p/[A-Za-z0-9_-]+/?$";

    private String                        username_url           = null;
    private final ArrayList<DownloadLink> decryptedLinks         = new ArrayList<DownloadLink>();
    private boolean                       prefer_server_filename = jd.plugins.hoster.InstaGramCom.defaultPREFER_SERVER_FILENAMES;
    private Boolean                       isPrivate              = false;
    private FilePackage                   fp                     = null;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        prefer_server_filename = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.PREFER_SERVER_FILENAMES, jd.plugins.hoster.InstaGramCom.defaultPREFER_SERVER_FILENAMES);
        fp = FilePackage.getInstance();
        fp.setProperty("ALLOW_MERGE", true);
        // https and www. is required!
        String parameter = param.toString().replaceFirst("^http://", "https://").replaceFirst("://in", "://www.in");
        if (parameter.contains("?private_url=true")) {
            isPrivate = Boolean.TRUE;
            /* Remove this from url as it is only required for decrypter */
            parameter = parameter.replace("?private_url=true", "");
        }
        if (!parameter.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the crawl process a tiny bit. */
            parameter += "/";
        }
        final PluginForHost hostplugin = JDUtilities.getPluginForHost(this.getHost());
        boolean logged_in = false;
        final Account aa = AccountController.getInstance().getValidAccount(hostplugin);
        if (aa != null) {
            /* Login whenever possible */
            try {
                jd.plugins.hoster.InstaGramCom.login(this.br, aa, false);
                logged_in = true;
            } catch (final Throwable e) {
            }
        }

        if (isPrivate && !logged_in) {
            logger.info("Account required to crawl this url");
            return decryptedLinks;
        }

        jd.plugins.hoster.InstaGramCom.prepBR(this.br);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String json = br.getRegex(">window\\._sharedData\\s*?=\\s*?(\\{.*?);</script>").getMatch(0);
        if (json == null) {
            return null;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        ArrayList<Object> resource_data_list;
        ArrayList<Object> resource_data_list2 = null;
        if (parameter.matches(TYPE_GALLERY)) {
            /* Crawl single images & galleries */
            resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
            for (final Object galleryo : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) galleryo;
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "graphql/shortcode_media");
                username_url = (String) JavaScriptEngineFactory.walkJson(entries, "owner/username");
                this.isPrivate = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "owner/is_private")).booleanValue();
                if (username_url != null) {
                    fp.setName(username_url);
                }
                decryptAlbum(entries);
            }
        } else {
            if (!this.br.containsHTML("user\\?username=.+")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            /* Crawl all items of a user */
            final String id_owner = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
            username_url = new Regex(parameter, "instagram\\.com/([^/]+)").getMatch(0);
            final boolean isPrivate = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/is_private")).booleanValue();

            if (username_url != null) {
                fp.setName(username_url);
            }

            final boolean abort_on_rate_limit_reached = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.QUIT_ON_RATE_LIMIT_REACHED, jd.plugins.hoster.InstaGramCom.defaultQUIT_ON_RATE_LIMIT_REACHED);
            final boolean only_grab_x_items = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS);
            final long maX_items = SubConfiguration.getConfig(this.getHost()).getLongProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS_NUMBER, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS_NUMBER);
            String nextid = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/page_info/end_cursor");
            final String maxid = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/__get_params/max_id");
            resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/nodes");
            final long count = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/count"), -1);
            if (isPrivate && !logged_in && count != -1 && resource_data_list == null) {
                logger.info("Cannot parse url as profile is private");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }

            if (id_owner == null) {
                // this isn't a error persay! check https://www.instagram.com/israbox/
                return decryptedLinks;
            }

            int page = 0;
            do {
                if (this.isAbort()) {
                    logger.info("User aborted decryption");
                    return decryptedLinks;
                } else if (only_grab_x_items && decryptedLinks.size() >= maX_items) {
                    logger.info("Number of items selected by user has been crawled --> Done");
                    break;
                }
                if (page > 0) {
                    Browser br = null;
                    // prepBRAjax(br, username_url, maxid);
                    int retrycounter = 1;
                    int errorcounter_403_wtf = 0;
                    int errorcounter_429_ratelimit_reached = 0;
                    boolean failed = true;
                    int responsecode;

                    /* Access next page - 403 error may happen once for logged in users - reason unknown - will work fine on 2nd request! */
                    do {
                        if (this.isAbort()) {
                            logger.info("User aborted decryption");
                            return decryptedLinks;
                        }

                        br = this.br.cloneBrowser();
                        if (retrycounter > 1) {
                            if (abort_on_rate_limit_reached) {
                                logger.info("abort_on_rate_limit_reached setting active --> Rate limit has been reached --> Aborting");
                                return decryptedLinks;
                            }
                            /*
                             * Try to bypass rate-limit - usually kicks in after about 4000 items and it is bound to IP, not User-Agent or
                             * cookies! Also we need to continue with the cookies we got at the beginning otherwise we'll get a 403! Aftzer
                             * about 60 seconds wait we should be able to continue but it might happen than we only get one batch of items
                             * and are blocked again then.
                             */
                            this.sleep(30000, param);
                        }
                        prepBRAjax(br, username_url, maxid);
                        final String p = "q=ig_user(" + id_owner + ")+%7B+media.after(" + nextid + "%2C+12)+%7B%0A++count%2C%0A++nodes+%7B%0A++++caption%2C%0A++++code%2C%0A++++comments+%7B%0A++++++count%0A++++%7D%2C%0A++++date%2C%0A++++dimensions+%7B%0A++++++height%2C%0A++++++width%0A++++%7D%2C%0A++++display_src%2C%0A++++id%2C%0A++++is_video%2C%0A++++likes+%7B%0A++++++count%0A++++%7D%2C%0A++++owner+%7B%0A++++++id%0A++++%7D%2C%0A++++thumbnail_src%0A++%7D%2C%0A++page_info%0A%7D%0A+%7D&ref=users%3A%3Ashow";
                        br.postPage("https://www." + this.getHost() + "/query/", p);
                        responsecode = br.getHttpConnection().getResponseCode();
                        if (responsecode == 403 || responsecode == 429) {
                            failed = true;
                            if (responsecode == 403) {
                                errorcounter_403_wtf++;
                            } else {
                                errorcounter_429_ratelimit_reached++;
                            }
                            logger.info("403 errors so far: " + errorcounter_403_wtf);
                            logger.info("429 errors so far: " + errorcounter_429_ratelimit_reached);
                        } else {
                            failed = false;
                        }
                        retrycounter++;
                        /* Stop on too many 403s as 403 is not a rate limit issue! */
                    } while (failed && retrycounter <= 300 && errorcounter_403_wtf < 20);

                    if (failed) {
                        logger.warning("Failed to bypass rate-limit!");
                        return decryptedLinks;
                    } else if (responsecode == 439) {
                        logger.info("Seems like user is using an unverified account - cannot grab more items");
                        break;
                    }
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "media/nodes");
                    nextid = (String) JavaScriptEngineFactory.walkJson(entries, "media/page_info/end_cursor");
                }
                if (resource_data_list.size() == 0) {
                    logger.info("Found no new links on page " + page + " --> Stopping decryption");
                    break;
                }
                for (final Object o : resource_data_list) {
                    decryptAlbum((LinkedHashMap<String, Object>) o);
                }
                page++;
            } while (nextid != null);
        }

        return decryptedLinks;
    }

    private void decryptAlbum(LinkedHashMap<String, Object> entries) {
        final long date = JavaScriptEngineFactory.toLong(entries.get("date"), 0);
        // is this id? // final String linkid_main = (String) entries.get("id");
        final String linkid_main = (String) entries.get("code");
        final String description = (String) entries.get("caption");

        final ArrayList<Object> resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "edge_sidecar_to_children/edges");
        if (resource_data_list != null && resource_data_list.size() > 0) {
            /* Album */
            for (final Object pictureo : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) pictureo;
                entries = (LinkedHashMap<String, Object>) entries.get("node");
                decryptSingleImage(entries, linkid_main, date, description);
            }
        } else {
            /* Single image */
            decryptSingleImage(entries, linkid_main, date, description);
        }
    }

    private void decryptSingleImage(LinkedHashMap<String, Object> entries, String linkid_main, final long date, final String description) {
        String server_filename = null;
        final String shortcode = (String) entries.get("shortcode");
        if (linkid_main == null && shortcode != null) {
            // link uid, with /p/ its shortcode
            linkid_main = shortcode;
        }
        final boolean isVideo = ((Boolean) entries.get("is_video")).booleanValue();
        String dllink;
        if (isVideo) {
            dllink = (String) entries.get("video_url");
        } else {
            dllink = (String) entries.get("display_src");
            if (dllink == null || !dllink.startsWith("http")) {
                dllink = (String) entries.get("display_url");
            }
            if (dllink == null || !dllink.startsWith("http")) {
                dllink = (String) entries.get("thumbnail_src");
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            try {
                server_filename = getFileNameFromURL(new URL(dllink));
            } catch (final Throwable e) {
            }
        }
        String filename;
        final String ext;
        if (isVideo) {
            ext = ".mp4";
        } else {
            ext = ".jpg";
        }
        if (prefer_server_filename && server_filename != null) {
            server_filename = jd.plugins.hoster.InstaGramCom.fixServerFilename(server_filename, ext);
            filename = server_filename;
        } else {
            if (StringUtils.isNotEmpty(username_url)) {
                filename = username_url + " - " + linkid_main;
            } else {
                filename = linkid_main;
            }
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(linkid_main)) {
                filename += "_" + shortcode;
            }
            filename += ext;
        }
        String hostplugin_url = "instagrammdecrypted://" + linkid_main;
        if (!StringUtils.isEmpty(shortcode)) {
            hostplugin_url += "/" + shortcode;
        }

        final DownloadLink dl = this.createDownloadlink(hostplugin_url);
        final String linkid = linkid_main + shortcode != null ? shortcode : "";
        String content_url = "https://www.instagram.com/p/" + linkid_main;
        if (isPrivate) {
            /*
             * Without account, private urls look exactly the same as offline urls --> Save private status for better host plugin
             * errorhandling.
             */
            content_url += "?private_url=true";
            dl.setProperty("private_url", true);
        }
        dl.setContentUrl(content_url);
        dl.setLinkID(linkid);
        if (fp != null && !"Various".equals(fp.getName())) {
            fp.add(dl);
        }
        dl.setAvailable(true);
        dl.setName(filename);
        if (date > 0) {
            jd.plugins.hoster.InstaGramCom.setReleaseDate(dl, date);
        }
        if (!StringUtils.isEmpty(shortcode)) {
            dl.setProperty("shortcode", shortcode);
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty("directurl", dllink);
        }
        if (!StringUtils.isEmpty(description)) {
            dl.setComment(description);
        }
        decryptedLinks.add(dl);
        distribute(dl);
    }

    private void prepBRAjax(final Browser br, final String username_url, final String maxid) {
        final String csrftoken = br.getCookie("instagram.com", "csrftoken");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Instagram-AJAX", "1");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (csrftoken != null) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (maxid != null) {
            br.getHeaders().put("Referer", "https://www.instagram.com/" + username_url + "/?max_id=" + maxid);
        }
        br.setCookie(this.getHost(), "ig_vw", "1680");
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 4;
    }
}
