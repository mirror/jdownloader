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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pan.baidu.com" }, urls = { "https?://(?:www\\.)?(?:pan|yun)\\.baidu\\.com/(?:share|wap)/.+|https?://(?:www\\.)?pan\\.baidu\\.com/s/[A-Za-z0-9-_]+(\\?linkpassword=[^#&]+)?(?:#(dir|list)/path=%2F.+)?" })
public class PanBaiduCom extends PluginForDecrypt {
    public PanBaiduCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String                TYPE_FOLDER_SUBFOLDER                 = "https?://(?:www\\.)?pan\\.baidu\\.com/(share/.+\\&dir=.+|s/[A-Za-z0-9-_]+#(dir|list)/path=%.+)";
    private static final String                TYPE_FOLDER_GENERAL                   = "https?://(www\\.)?pan\\.baidu\\.com/share/[a-z\\?\\&]+((shareid|uk)=\\d+\\&(shareid|uk)=\\d+(.*?&dir=.+|#(list|dir)/path=%2F.+))";
    private static final String                TYPE_FOLDER_NORMAL                    = "https?://(www\\.)?pan\\.baidu\\.com/share/[a-z\\?\\&]+(shareid|uk)=\\d+\\&(uk|shareid)=\\d+(%20%E5%AF%86%E7%A0%81:.+)?";
    private static final String                TYPE_FOLDER_NORMAL_PASSWORD_PROTECTED = "https?://(www\\.)?pan\\.baidu\\.com/share/init\\?(shareid|uk)=\\d+\\&(uk|shareid)=\\d+";
    private static final String                TYPE_FOLDER_SHORT                     = "https?://(www\\.)?pan\\.baidu\\.com/s/[A-Za-z0-9-_]+";
    private static final String                TYPE_FOLDER_USER_HOME                 = ".+/share/home.+";
    private static final String                APPID                                 = "250528";
    private String                             link_password                         = null;
    private String                             link_password_cookie                  = null;
    private String                             uk                                    = null;
    private CryptedLink                        param                                 = null;
    private String                             parameter                             = null;
    private final ArrayList<DownloadLink>      decryptedLinks                        = new ArrayList<DownloadLink>();
    private int                                file_object_index                     = 0;
    private final HashMap<String, FilePackage> filePackages                          = new HashMap<String, FilePackage>();

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        file_object_index = 0;
        this.param = param;
        parameter = param.toString().replaceAll("(pan|yun)\\.baidu\\.com/", "pan.baidu.com/").replace("/wap/", "/share/");
        /* Extract password from url in case the url came from this decrypter before. */
        link_password = new Regex(parameter, "linkpassword=([^<>\"\\&=]+)").getMatch(0);
        if (link_password != null) {
            /* Remove invalid parameter from url. */
            parameter = parameter.replaceAll("(\\&|\\?|#)linkpassword=" + link_password, "");
            /* Revert urlencode */
            link_password = Encoding.htmlDecode(link_password);
        }
        if (!parameter.matches(TYPE_FOLDER_NORMAL_PASSWORD_PROTECTED) && !parameter.matches(TYPE_FOLDER_SHORT) && !parameter.matches(TYPE_FOLDER_USER_HOME)) {
            /* Correct invalid "view" linktypes - we need one general linkformat! */
            final String replace_part = new Regex(parameter, "(baidu\\.com/share/[a-z]+)").getMatch(0);
            if (replace_part != null) {
                parameter = parameter.replaceAll(replace_part, "baidu.com/share/link");
            }
        }
        br.setFollowRedirects(true);
        /* If we access urls without cookies we might get 404 responses for no reason so let's access the main page first. */
        br.getPage("http://pan.baidu.com");
        br.getPage(parameter);
        if (br.getURL().contains("/error") || br.containsHTML("id=\"share_nofound_des\"")) {
            final DownloadLink dl = this.createOfflinelink(parameter);
            dl.setFinalFileName(new Regex(parameter, "pan\\.baidu\\.com/(.+)").getMatch(0));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (this.parameter.matches(TYPE_FOLDER_USER_HOME)) {
            crawlHome();
        } else {
            crawlFoldersAndFiles();
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void crawlHome() throws Exception {
        this.uk = new Regex(this.parameter, "uk=(\\d+)").getMatch(0);
        int offset = 0;
        int currentlinksnum = 0;
        final int max_numberof_items_per_page = 60;
        do {
            if (this.isAbort()) {
                return;
            }
            /* Reset that */
            currentlinksnum = 0;
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage(String.format("http://pan.baidu.com/pcloud/feed/getsharelist?t=%d&category=0&auth_type=1&request_location=share_home&start=%d&limit=60&query_uk=%s&channel=chunlei&clienttype=0&web=1&logid=&bdstoken=null", System.currentTimeMillis(), offset, this.uk));
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> records = (ArrayList<Object>) entries.get("records");
            ArrayList<Object> filelist = (ArrayList<Object>) entries.get("records");
            if (records == null) {
                break;
            }
            for (final Object recordo : records) {
                entries = (LinkedHashMap<String, Object>) recordo;
                final String shorturl_id = (String) entries.get("shorturl");
                final String shareid = (String) entries.get("shareid");
                if (shareid == null || shareid.equals("")) {
                    continue;
                }
                filelist = (ArrayList<Object>) entries.get("filelist");
                if (filelist == null) {
                    continue;
                }
                for (final Object fileo : filelist) {
                    crawlFolderObject(fileo, shorturl_id, shareid);
                    currentlinksnum++;
                    offset++;
                }
            }
        } while (currentlinksnum >= max_numberof_items_per_page);
    }

    private void crawlFoldersAndFiles() throws Exception {
        final String shorturl_id = new Regex(parameter, "/s/([A-Za-z0-9-_]+)").getMatch(0);
        uk = br.getRegex("\"uk\":(\\d+),").getMatch(0);
        String shareid = br.getRegex("\"shareid\":(\\d+),").getMatch(0);
        JDUtilities.getPluginForHost(this.getHost());
        if (br.getURL().contains("/share/init")) {
            if (parameter.matches(TYPE_FOLDER_GENERAL) || parameter.matches(TYPE_FOLDER_NORMAL)) {
                uk = new Regex(parameter, "uk=(\\d+)").getMatch(0);
                shareid = new Regex(parameter, "shareid=(\\d+)").getMatch(0);
                if (link_password == null && parameter.matches(TYPE_FOLDER_NORMAL)) {
                    link_password = new Regex(parameter, "%20%E5%AF%86%E7%A0%81:(.+)").getMatch(0);
                }
            } else {
                uk = new Regex(br.getURL(), "uk=(\\d+)").getMatch(0);
                shareid = new Regex(br.getURL(), "shareid=(\\d+)").getMatch(0);
            }
            final String linkpart = new Regex(parameter, "(pan\\.baidu\\.com/.+)").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            for (int i = 1; i <= 3; i++) {
                if (link_password == null) {
                    link_password = getUserInput("Password for " + linkpart + "?", param);
                }
                if (uk != null) {
                    br.postPage("//pan.baidu.com/share/verify?" + "channel=chunlei&clienttype=0&web=1&shareid=" + shareid + "&uk=" + uk + "&t=" + System.currentTimeMillis(), "vcode=&vcode_str=&pwd=" + Encoding.urlEncode(link_password));
                } else {
                    String location = new Regex(br.getURL(), "\\?(.+)").getMatch(0);
                    br.postPage("//pan.baidu.com/share/verify?" + location + "&t=" + System.currentTimeMillis(), "vcode=&vcode_str=&pwd=" + Encoding.urlEncode(link_password));
                }
                if (!br.containsHTML("\"errno\":0")) {
                    link_password = null;
                    continue;
                }
                break;
            }
            if (!br.containsHTML("\"errno\":0")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            link_password_cookie = br.getCookie("http://pan.baidu.com/", "BDCLND");
            if (uk != null) {
                parameter = br.getURL("//pan.baidu.com/share/link?shareid=" + shareid + "&uk=" + uk).toString();
            }
            br.getHeaders().remove("X-Requested-With");
            br.getPage(parameter);
            if (br.getURL().contains("/error") || br.containsHTML("id=\"share_nofound_des\"") || this.br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink dl = this.createOfflinelink(parameter);
                dl.setFinalFileName(new Regex(parameter, "pan\\.baidu\\.com/(.+)").getMatch(0));
                decryptedLinks.add(dl);
                return;
            }
            if (uk == null) {
                uk = br.getRegex("yunData.SHARE_UK = \"(\\d+)\";").getMatch(0);
                if (uk == null) {
                    uk = br.getRegex("uk\":(\\d+)").getMatch(0);
                }
                shareid = br.getRegex("yunData.SHARE_ID = \"(\\d+)\";").getMatch(0);
                if (shareid == null) {
                    shareid = br.getRegex("shareid\":(\\d+)").getMatch(0);
                }
            }
        }
        if (uk == null || shareid == null) {
            /* Probably user added invalid url */
            return;
        }
        String singleFolder = new Regex(parameter, "#dir/path=(.*?)$").getMatch(0);
        if (singleFolder == null) {
            singleFolder = new Regex(parameter, "&dir=([^&\\?]+)").getMatch(0);
        }
        String dir = null;
        String dirName = null;
        boolean is_subfolder = false;
        // Jump into folder or get content of the main link
        if (param.toString().matches(TYPE_FOLDER_SUBFOLDER) || param.toString().matches(TYPE_FOLDER_GENERAL)) {
            dirName = new Regex(param.toString(), "(?:(?:dir|list)/path=|&dir=)%2F([^&\\?]+)").getMatch(0);
            dir = "%2F" + dirName;
            is_subfolder = true;
        }
        br.getHeaders().put("Accept", "Accept");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int currentpage = 1;
        long errno = -1;
        final int maxpages = 10;
        final int maxlinksperpage = 100;
        int currentlinksnum = 0;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> ressourcelist = null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return;
            }
            currentlinksnum = 0;
            if (currentpage > 1 || is_subfolder) {
                br.getPage(getFolder(parameter, dir, currentpage, shareid));
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                errno = JavaScriptEngineFactory.toLong(entries.get("errno"), -1);
                if (errno == 2) {
                    /* Empty folder */
                    final DownloadLink dl = this.createOfflinelink(parameter);
                    dl.setFinalFileName(Encoding.htmlDecode(dirName));
                    decryptedLinks.add(dl);
                    return;
                }
                ressourcelist = (ArrayList) entries.get("list");
            } else {
                final String json = this.br.getRegex("setData\\((\\{.+?\\})\\);").getMatch(0);
                if (json == null) {
                    logger.warning("Problemo! Please report to JDownloader Development Team, link: " + parameter);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "file_list/list");
            }
            if (ressourcelist.size() == 0 && errno == 0) {
                /* Empty folder */
                final DownloadLink dl = this.createOfflinelink(parameter);
                dl.setFinalFileName(Encoding.htmlDecode(dirName));
                decryptedLinks.add(dl);
                return;
            }
            for (final Object fileo : ressourcelist) {
                crawlFolderObject(fileo, shorturl_id, shareid);
                currentlinksnum++;
            }
            currentpage++;
        } while (currentlinksnum >= maxlinksperpage && currentpage <= maxpages);
    }

    private void crawlFolderObject(final Object fileo, final String shorturl_id, final String shareid) {
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) fileo;
        final String server_filename = (String) entries.get("server_filename");
        if (StringUtils.isEmpty(server_filename)) {
            /* Nothing to grab */
            return;
        }
        DownloadLink dl;
        final String fsid = Long.toString(JavaScriptEngineFactory.toLong(entries.get("fs_id"), 0));
        final long size = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        final long isdelete = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        final long isdir = JavaScriptEngineFactory.toLong(entries.get("isdir"), 0);
        if (isdir == 1) {
            final String path = (String) entries.get("path");
            String subdir_link = null;
            if (path == null) {
                /* Nothing to grab */
                return;
            }
            /* Subfolder --> Goes back into decrypter */
            if (shorturl_id == null && !shareid.equals("")) {
                String general_folder = parameter;
                String folder_path = new Regex(general_folder, "(#dir/path=.*?)$").getMatch(0);
                if (folder_path != null) {
                    general_folder = general_folder.replace(folder_path, "");
                }
                subdir_link = general_folder + "#dir/path=" + Encoding.urlEncode(path);
            } else {
                subdir_link = "http://pan.baidu.com/s/" + shorturl_id + "#dir/path=" + Encoding.urlEncode(path);
            }
            if (link_password != null) {
                /*
                 * Add passsword so in case user adds password protected mainfolder once he does not have to enter the password again for
                 * each subfolder :)
                 */
                subdir_link += "&linkpassword=" + Encoding.urlEncode(link_password);
            }
            dl = createDownloadlink(subdir_link);
        } else {
            if (fsid.equals("0") || StringUtils.isEmpty(server_filename)) {
                /* Nothing to grab */
                return;
            }
            /* 2017-07-21: 'shorturl_id' is not always given (anymore?)! */
            final String contenturl;
            if (shorturl_id != null) {
                contenturl = String.format("http://%s/s/%s", this.getHost(), shorturl_id);
            } else {
                contenturl = getPlainLink(parameter);
            }
            String path = (String) entries.get("path");
            String md5 = (String) entries.get("md5");
            if (StringUtils.isEmpty(md5)) {
                /* 2019-07-16: Workaround: md5 value is important to have but it is not always given ... */
                try {
                    entries = (LinkedHashMap<String, Object>) entries.get("thumbs");
                    final String docpreviewURL = (String) entries.get("docpreview");
                    String checkURL = (String) entries.get("url1");
                    if (StringUtils.isEmpty(checkURL)) {
                        checkURL = (String) entries.get("url2");
                    }
                    if (!StringUtils.isEmpty(checkURL)) {
                        md5 = new Regex(checkURL, "/thumbnail/([a-f0-9]{32})").getMatch(0);
                    }
                    if (StringUtils.isEmpty(md5) && !StringUtils.isEmpty(docpreviewURL)) {
                        md5 = new Regex(checkURL, "/doc/([a-f0-9]{32})").getMatch(0);
                    }
                } catch (final Throwable e) {
                }
            }
            dl = createDownloadlink("http://pan.baidudecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            if (server_filename.matches("^\\.[A-Za-z0-9]+")) {
                /*
                 * 2019-07-16: They're hiding their filenames inside an image now. In this case only the fileextension is given. Image
                 * information can be found inside 'title_img'. This is just a workaround to give the user a chance to find the URLs he
                 * added although a real filename is not given.
                 */
                String workaround_filename = "";
                if (shorturl_id != null) {
                    workaround_filename += shorturl_id + "_";
                }
                workaround_filename += shareid;
                workaround_filename += server_filename;
                dl.setName(workaround_filename);
            } else {
                dl.setProperty("server_filename", server_filename);
                dl.setFinalFileName(server_filename);
            }
            if (size > 0) {
                dl.setDownloadSize(size);
            }
            dl.setProperty("mainLink", contenturl);
            // dl.setProperty("dirName", dir);
            dl.setProperty("important_link_password", link_password);
            dl.setDownloadPassword(link_password);
            dl.setProperty("important_link_password_cookie", link_password_cookie);
            dl.setProperty("important_fsid", fsid);
            dl.setContentUrl(contenturl);
            dl.setProperty("origurl_uk", uk);
            dl.setProperty("origurl_shareid", shareid);
            /* Required for multihoster support */
            dl.setProperty("position", file_object_index);
            if (isdelete == 1) {
                dl.setAvailable(false);
            } else {
                dl.setAvailable(true);
            }
            if (!StringUtils.isEmpty(path)) {
                dl.setProperty("path_full", path);
                path = new Regex(path, "(/.+/)").getMatch(0);
                FilePackage fp = filePackages.get(path);
                if (fp == null) {
                    final String name;
                    if (getPluginConfig().getBooleanProperty("COMBINE_IN_ONE_FOLDER", true)) {
                        name = new Regex(path, ".+/(.+)/").getMatch(0);
                    } else {
                        name = new Regex(path, "/(.+)/").getMatch(0);
                    }
                    if (name != null) {
                        fp = FilePackage.getInstance();
                        fp.setName(name);
                        filePackages.put(path, fp);
                    }
                }
                if (fp != null) {
                    fp.add(dl);
                }
            }
            if (!StringUtils.isEmpty(md5)) {
                /* This is NOT the md5 hash of the actual file!! */
                // dl.setMD5Hash(md5);
                /* 2019-07-03: We store the md5 hash as property as we might need them for the download process / multihoster support. */
                dl.setProperty("internal_md5hash", md5);
            }
            if (shorturl_id != null) {
                dl.setProperty("shorturl_id", shorturl_id);
            }
            String linkid = this.getHost() + "://";
            if (shorturl_id != null) {
                linkid += shorturl_id + "/";
            }
            linkid += shareid + "/";
            linkid += "/" + fsid;
            dl.setLinkID(linkid);
            file_object_index++;
        }
        decryptedLinks.add(dl);
    }

    private String getFolder(final String parameter, String dir, final int page, String shareid) {
        // String unicode_stuff = new Regex(dir, "_(.+)$").getMatch(0);
        // if (unicode_stuff != null) {
        // dir = dir.replace(unicode_stuff, "");
        // unicode_stuff = unescape(unicode_stuff);
        // dir += Encoding.urlEncode(unicode_stuff);
        // }
        if (shareid == null) {
            shareid = new Regex(parameter, "shareid=(\\d+)").getMatch(0);
        }
        if (shareid == null) {
            shareid = br.getRegex("\"shareid\":(\\d+)").getMatch(0);
        }
        if (shareid == null) {
            shareid = new Regex(parameter, "/s/(.*)").getMatch(0);
        }
        if (uk == null) {
            uk = new Regex(parameter, "uk=(\\d+)").getMatch(0);
        }
        if (uk == null) {
            uk = br.getRegex("uk=(\\d+)").getMatch(0);
        }
        return "//pan.baidu.com/share/list?uk=" + (uk != null ? uk : "") + "&shareid=" + (shareid != null ? shareid : "") + "&page=" + page + "&num=100&dir=" + dir + "&order=time&desc=1&_=" + System.currentTimeMillis() + "&bdstoken=&channel=chunlei&clienttype=0&web=1&app_id=" + APPID;
    }

    private String getPlainLink(final String input) {
        return input.replace("/share/init?", "/share/link?");
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "COMBINE_IN_ONE_FOLDER", "Add only 1 folder to the linkgrabber.").setDefaultValue(true));
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}