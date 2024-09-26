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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.PanBaiduCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pan.baidu.com" }, urls = { "https?://(?:pan|yun)\\.baidu\\.com/(?:share|wap)/.+|https?://(?:www\\.)?pan\\.baidu\\.com/s/.+" })
public class PanBaiduComCrawler extends PluginForDecrypt {
    public PanBaiduComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private static final String                TYPE_FOLDER_SUBFOLDER                 = "(?i)https?://(?:www\\.)?pan\\.baidu\\.com/(share/.+\\&dir=.+|s/[A-Za-z0-9-_]+#(dir|list)/path=%.+)";
    private static final String                TYPE_FOLDER_GENERAL                   = "(?i)https?://(www\\.)?pan\\.baidu\\.com/share/[a-z\\?\\&]+((shareid|uk)=\\d+\\&(shareid|uk)=\\d+(.*?&dir=.+|#(list|dir)/path=%2F.+))";
    private static final String                TYPE_FOLDER_NORMAL                    = "(?i)https?://(www\\.)?pan\\.baidu\\.com/share/[a-z\\?\\&]+(shareid|uk)=\\d+\\&(uk|shareid)=\\d+(%20%E5%AF%86%E7%A0%81:.+)?";
    private static final String                TYPE_FOLDER_NORMAL_PASSWORD_PROTECTED = "(?i)https?://[^/]+/share/init.+";
    private static final String                TYPE_FOLDER_SHORT                     = "(?i)https?://(www\\.)?pan\\.baidu\\.com/s/[A-Za-z0-9-_]+";
    private static final String                TYPE_FOLDER_USER_HOME                 = "(?i).+/share/home.+";
    private static final String                APPID                                 = "250528";
    private String                             link_password                         = null;
    private String                             link_password_cookie                  = null;
    private final ArrayList<DownloadLink>      ret                                   = new ArrayList<DownloadLink>();
    private int                                object_index                          = 0;
    private String                             position_arrayStr                     = null;
    private final HashMap<String, FilePackage> filePackages                          = new HashMap<String, FilePackage>();

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String contenturl = param.getCryptedUrl();
        contenturl = param.getCryptedUrl().replaceAll("(pan|yun)\\.baidu\\.com/", "pan.baidu.com/").replace("/wap/", "/share/");
        /* Extract password from url in case the url came from this decrypter before. */
        final DownloadLink source = param.getDownloadLink();
        if (source != null) {
            /* Set password cookie so we can skip password prompt. */
            final String pwcookie = source.getStringProperty(PanBaiduCom.PROPERTY_PASSWORD_COOKIE);
            if (pwcookie != null) {
                this.link_password_cookie = pwcookie;
                br.setCookie(getHost(), "BDCLND", pwcookie);
            }
            link_password = source.getDownloadPassword();
            position_arrayStr = source.getStringProperty("positionarray");
        }
        if (link_password == null) {
            /* Check for password given in URL */
            link_password = new Regex(contenturl, "linkpassword=([^\\?\\&=]+)").getMatch(0);
        }
        if (contenturl.matches(TYPE_FOLDER_NORMAL_PASSWORD_PROTECTED)) {
            br.getPage(contenturl);
        } else {
            /* If we access urls without cookies we might get 404 responses for no reason so let's access the main page first. */
            br.getPage("https://pan.baidu.com");
            br.getPage(contenturl);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean looksLikeOffline = br.containsHTML("class=\"error-reason\"");
        String uk = null;
        if (br.getURL().matches(TYPE_FOLDER_USER_HOME)) {
            // TODO: Check if this still works / is still needed
            uk = new Regex(contenturl, "uk=(\\d+)").getMatch(0);
            int offset = 0;
            final int max_numberof_items_per_page = 60;
            pagination: do {
                /* Reset that */
                int newItemsThisPage = 0;
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage(String.format("http://pan.baidu.com/pcloud/feed/getsharelist?t=%d&category=0&auth_type=1&request_location=share_home&start=%d&limit=60&query_uk=%s&channel=chunlei&clienttype=0&web=1&logid=&bdstoken=null", System.currentTimeMillis(), offset, uk));
                Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final List<Object> records = (List<Object>) entries.get("records");
                List<Map<String, Object>> filelist = (List<Map<String, Object>>) entries.get("records");
                if (records == null) {
                    /* E.g. {"errno":2,"request_id":123456789123456789} */
                    if (ret.isEmpty()) {
                        /* Invalid/offline folder */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    break pagination;
                }
                for (final Object recordo : records) {
                    entries = (Map<String, Object>) recordo;
                    final String shorturl_id = (String) entries.get("shorturl");
                    final String shareid = (String) entries.get("shareid");
                    if (StringUtils.isEmpty(shareid)) {
                        continue;
                    }
                    filelist = (List<Map<String, Object>>) entries.get("filelist");
                    if (filelist == null) {
                        continue;
                    }
                    for (final Map<String, Object> file : filelist) {
                        crawlFolderObject(file, shorturl_id, uk, shareid);
                        newItemsThisPage++;
                        offset++;
                    }
                }
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break pagination;
                } else if (newItemsThisPage < max_numberof_items_per_page) {
                    logger.info("Stopping because: Reached end");
                    break pagination;
                }
            } while (!this.isAbort());
        } else {
            /*
             * 2019-07-23: Important: RegEx from current URL as this value may change when accessing the URL which the user has added
             * (redirect)
             */
            String surl = UrlQuery.parse(br.getURL()).get("surl");
            if (surl == null) {
                /* Maybe query param behind anchor */
                surl = new Regex(br.getURL(), "(?:/s/|surl=)([A-Za-z0-9-_]+)").getMatch(0);
            }
            uk = br.getRegex("share_uk:\"(\\d+)\"").getMatch(0);
            if (uk == null) {
                uk = br.getRegex("\"uk\":(\\d+),").getMatch(0);
                if (uk == null) {
                    uk = br.getRegex("\"uk\":\"(\\d+)\",").getMatch(0);
                }
            }
            String shareid = br.getRegex("\"shareid\":(\\d+),").getMatch(0);
            if (shareid == null) {
                shareid = br.getRegex("shareid:\"(\\d+)\"").getMatch(0);
            }
            if (shareid == null) {
                shareid = UrlQuery.parse(br.getURL()).get("shareid");
            }
            if (br.getURL().contains("/share/init")) {
                if (surl == null) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find surl");
                }
                if (link_password == null && br.getURL().matches(TYPE_FOLDER_NORMAL)) {
                    // TODO: Remove this
                    link_password = new Regex(br.getURL(), "%20%E5%AF%86%E7%A0%81:(.+)").getMatch(0);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final UrlQuery pwquery = UrlQuery.parse(br.getURL());
                boolean success = false;
                pwloop: for (int i = 0; i < 3; i++) {
                    if (link_password == null || i > 0) {
                        link_password = getUserInput("Password for " + br.getHost() + "?", param);
                    }
                    pwquery.addAndReplace("t", "" + System.currentTimeMillis());
                    pwquery.addAndReplace("surl", surl);
                    pwquery.addAndReplace("channel", "chunlei");
                    pwquery.addAndReplace("web", "1");
                    pwquery.addAndReplace("app_id", APPID);
                    pwquery.addAndReplace("bdstoken", "");
                    // pwquery.add("logid", "");
                    pwquery.addAndReplace("clienttype", "0");
                    // pwquery.add("dp-logid", "");
                    final Form pwform = new Form();
                    pwform.setMethod(MethodType.POST);
                    pwform.setAction("/share/verify?" + pwquery.toString());
                    pwform.put("pwd", Encoding.urlEncode(link_password));
                    pwform.put("vcode", "");
                    pwform.put("vcode_str", "");
                    /*
                     * 2019-07-25: Wrong Referer = After one password attempt, all tried will get a "Wrong password" response even when a
                     * correct password is entered!
                     */
                    br.getHeaders().put("Referer", "https://pan.baidu.com/share/init?" + pwquery.toString());
                    br.submitForm(pwform);
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final Number errno = (Number) entries.get("errno");
                    if (errno == null || errno.intValue() == 0) {
                        success = true;
                        break pwloop;
                    } else {
                        logger.info("Wrong password: " + link_password);
                        link_password = null;
                        continue pwloop;
                    }
                }
                if (!success) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                link_password_cookie = br.getCookie(br.getHost(true), "BDCLND");
                if (StringUtils.isEmpty(link_password_cookie)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().remove("X-Requested-With");
                /*
                 * 2019-07-23: Important: Do NOT access the '/init' URL again - this will lead to response 404! Also note that the
                 * shorturl_id changes!!
                 */
                if (!surl.startsWith("1")) {
                    surl = "1" + surl;
                }
                br.getPage("/s/" + surl);
                if (br.getURL().contains("/error") || br.containsHTML("id=\"share_nofound_des\"") || this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            if (uk == null || shareid == null) {
                /* Probably user added invalid url */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String bothFolderIDs = shareid + "_" + uk;
            String dir = null;
            final String dirName = new Regex(contenturl, "(?:(?:dir|list)/path=|&dir=)%2F([^&\\?]+)").getMatch(0);
            boolean is_subfolder = false;
            final String filenameForEmptyFolder;
            if (dirName != null) {
                dir = "/" + Encoding.htmlDecode(dirName);
                is_subfolder = true;
                filenameForEmptyFolder = bothFolderIDs + "_" + Encoding.htmlDecode(dirName);
            } else {
                filenameForEmptyFolder = bothFolderIDs;
            }
            br.getHeaders().put("Accept", "Accept");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            int page = 1;
            long errno = -1;
            final int maxitemsperpage = 100;
            Map<String, Object> entries = null;
            List<Map<String, Object>> ressourcelist = null;
            pagination: do {
                int newLinksThisPage = 0;
                if (page > 1 || is_subfolder) {
                    final UrlQuery folderquery = new UrlQuery();
                    folderquery.add("is_from_web", "true");
                    if (link_password_cookie != null) {
                        folderquery.add("sekey", link_password_cookie);
                    }
                    folderquery.add("uk", uk);
                    folderquery.add("shareid", shareid);
                    folderquery.add("order", "other");
                    folderquery.add("desc", "1");
                    folderquery.add("showempty", "0");
                    folderquery.add("web", "1");
                    folderquery.add("page", Integer.toString(page));
                    folderquery.add("num", Integer.toString(maxitemsperpage));
                    folderquery.add("dir", URLEncode.encodeURIComponent(dir));
                    folderquery.add("t", "0." + System.currentTimeMillis());
                    folderquery.add("channel", "chunlei");
                    folderquery.add("app_id", APPID);
                    folderquery.add("bdstoken", "");
                    // folderquery.add("logid", "");
                    folderquery.add("clienttype", "0");
                    // folderquery.add("dp-logid", "");
                    final String url = "/share/list?" + folderquery.toString();
                    br.getPage(url);
                    entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    ressourcelist = (List) entries.get("list");
                } else {
                    /* Regex json from html code */
                    String json = this.br.getRegex("setData\\((\\{.*\\})\\);\\n").getMatch(0);
                    if (json == null) {
                        /* 2021-03-22 */
                        json = this.br.getRegex("locals\\.mset\\((\\{.*?\\})\\);\\n").getMatch(0);
                    }
                    if (json == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "json is null");
                    }
                    entries = restoreFromString(json, TypeRef.MAP);
                    ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "file_list/list");
                    if (ressourcelist == null) {
                        /* 2021-03-22 */
                        ressourcelist = (List<Map<String, Object>>) entries.get("file_list");
                    }
                }
                errno = JavaScriptEngineFactory.toLong(entries.get("errno"), 0);
                if (errno == 2) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, filenameForEmptyFolder);
                } else if (errno != 0) {
                    /* Some error happened -> Assume that folder is offline */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (ressourcelist.size() == 0) {
                    /* Empty folder */
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, filenameForEmptyFolder);
                }
                for (final Map<String, Object> file : ressourcelist) {
                    crawlFolderObject(file, surl, uk, shareid);
                    newLinksThisPage++;
                }
                logger.info("Crawled page: " + page + " | New items on this page: " + newLinksThisPage + " | Found items so far: " + ret.size());
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    break pagination;
                } else if (ressourcelist.size() < maxitemsperpage) {
                    logger.info("Stopping because: Reached end");
                    break pagination;
                }
                page++;
            } while (!this.isAbort());
        }
        if (ret.isEmpty()) {
            if (looksLikeOffline) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    private void crawlFolderObject(final Map<String, Object> entries, final String surl, final String uk, final String shareid) throws UnsupportedEncodingException {
        final String server_filename = (String) entries.get("server_filename");
        if (StringUtils.isEmpty(server_filename)) {
            /* Nothing to grab */
            return;
        }
        final DownloadLink dl;
        final String fsid = entries.get("fs_id").toString();
        final String position_arrayStr_current;
        if (position_arrayStr == null) {
            position_arrayStr_current = "" + object_index;
        } else {
            position_arrayStr_current = position_arrayStr + "," + object_index;
        }
        final String path = (String) entries.get("path");
        String pathWithoutFilename = null;
        if (((Number) entries.get("isdir")).intValue() == 1) {
            /* Subfolder --> Goes back into decrypter */
            String subdir_link = null;
            if (StringUtils.isEmpty(path)) {
                /* Nothing to grab */
                logger.info("path is empty");
                return;
            } else if (StringUtils.isEmpty(surl)) {
                /* This should never happen */
                logger.info("shorturl_id is empty");
                return;
            }
            subdir_link = "https://pan.baidu.com/s/" + surl + "#dir/path=" + URLEncode.encodeURIComponent(path);
            dl = createDownloadlink(subdir_link);
        } else {
            /*
             * Filename can be at the end of the path --> Remove that to get a valid path (= leads to folder which contains the file(s))
             */
            if (path.endsWith(server_filename)) {
                pathWithoutFilename = path.replaceFirst(Pattern.quote(server_filename) + "$", "");
            } else {
                pathWithoutFilename = path;
            }
            String md5 = (String) entries.get("md5");
            final Map<String, Object> thumbs = (Map<String, Object>) entries.get("thumbs");
            if (StringUtils.isEmpty(md5) && thumbs != null) {
                /* 2019-07-16: Workaround: md5 value is important to have but it is not always given ... */
                final String docpreviewURL = (String) thumbs.get("docpreview");
                String checkURL = (String) thumbs.get("url1");
                if (StringUtils.isEmpty(checkURL)) {
                    checkURL = (String) thumbs.get("url2");
                }
                if (!StringUtils.isEmpty(checkURL)) {
                    md5 = new Regex(checkURL, "(?i)/thumbnail/([a-f0-9]{32})").getMatch(0);
                }
                if (StringUtils.isEmpty(md5) && !StringUtils.isEmpty(docpreviewURL)) {
                    md5 = new Regex(docpreviewURL, "(?i)/doc/([a-f0-9]{32})").getMatch(0);
                }
            }
            dl = createDownloadlink("http://pan.baidudecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            if (server_filename.matches("^\\.[A-Za-z0-9]+")) {
                /*
                 * 2019-07-16: Sometimes they're hiding their filenames inside an image now. In this case only the file extension is given.
                 * Image information can be found inside 'title_img'. This is just a workaround to give the user a chance to find the URLs
                 * he added although a real filename is not given.
                 */
                String workaround_filename = "";
                if (surl != null) {
                    workaround_filename += surl + "_";
                }
                workaround_filename += shareid;
                workaround_filename += server_filename;
                dl.setName(workaround_filename);
            } else {
                dl.setProperty(PanBaiduCom.PROPERTY_SERVER_FILENAME, server_filename);
                dl.setFinalFileName(server_filename);
            }
            dl.setDownloadSize(((Number) entries.get("size")).longValue());
            if (link_password != null) {
                dl.setDownloadPassword(link_password);
                dl.setProperty(PanBaiduCom.PROPERTY_PASSWORD_COOKIE, link_password_cookie);
            }
            dl.setProperty(PanBaiduCom.PROPERTY_FSID, fsid);
            dl.setProperty(PanBaiduCom.PROPERTY_INTERNAL_UK, uk);
            dl.setProperty(PanBaiduCom.PROPERTY_INTERNAL_SHAREID, shareid);
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* Only for development purposes */
                dl.setComment("Position: " + position_arrayStr_current);
            }
            if (JavaScriptEngineFactory.toLong(entries.get("isdelete"), 0) == 1) {
                dl.setAvailable(false);
            } else {
                dl.setAvailable(true);
            }
            if (!StringUtils.isEmpty(md5)) {
                /* This is NOT the md5 hash of the actual file!! */
                // dl.setMD5Hash(md5);
                /*
                 * 2019-07-03: We store the md5 hash as property as we might need them for the download process / multihoster support.
                 */
                dl.setProperty(PanBaiduCom.PROPERTY_INTERNAL_MD5_HASH, md5);
            }
            if (surl != null) {
                dl.setProperty(PanBaiduCom.PROPERTY_SHORTURL_ID, surl);
            }
            /*
             * For single loose files, path may equal something like "rar" -> Only set path and FilePackage for items which are part of a
             * folder.
             */
            if (path != null && path.startsWith("/")) {
                /* Path we have contains internal userID and fileID -> Remove this for the path that is visible to the user in the end. */
                final String pathPretty = pathWithoutFilename.replaceFirst("/sharelink\\d+-\\d+/", "");
                FilePackage fp = filePackages.get(pathPretty);
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(pathPretty);
                    filePackages.put(pathPretty, fp);
                }
                dl.setProperty(PanBaiduCom.PROPERTY_INTERNAL_PATH, path);
                dl._setFilePackage(fp);
                dl.setRelativeDownloadFolderPath(pathPretty);
            }
            String linkid = this.getHost() + "://";
            if (surl != null) {
                linkid += surl + "/";
            }
            linkid += uk + "/" + fsid;
            dl.setLinkID(linkid);
        }
        /* Required for multihoster support */
        dl.setProperty("positionarray", position_arrayStr_current);
        if (link_password != null) {
            dl.setDownloadPassword(link_password);
        }
        object_index++;
        ret.add(dl);
        distribute(dl);
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}