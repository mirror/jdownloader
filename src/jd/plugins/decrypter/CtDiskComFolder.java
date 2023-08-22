//  jDownloader - Downloadmanager
//  Copyright (C) 2012  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class CtDiskComFolder extends PluginForDecrypt {
    @Deprecated
    private String             uuid                = null;
    // folder unique id
    @Deprecated
    private String             fuid                = null;
    private static Object      LOCK                = new Object();
    public static final String PROPERTY_PARENT_DIR = "parent_dir";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ctfile.com", "ctdisk.com", "400gb.com", "pipipan.com", "t00y.com", "bego.cc", "72k.us", "tc5.us", "545c.com", "sn9.us", "089u.com", "u062.com", "474b.com", "590m.com", "n802.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String[]> pluginDomains = getPluginDomains();
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://([A-Za-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/(?:dir/[a-f0-9\\-]+(?:\\?\\d+)?|u/\\d+/\\d+|d/[a-f0-9\\-]+(?:\\?\\d+)?)");
        }
        return ret.toArray(new String[0]);
    }

    public CtDiskComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser prepBrowser(final Browser prepBr) {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 1000);
        prepBr.setCookiesExclusive(true);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        prepBr.setReadTimeout(3 * 60 * 1000);
        return prepBr;
    }

    @Deprecated
    protected String correctHost(final String oldhostOld) {
        final List<String[]> pluginDomains = getPluginDomains();
        for (final String[] domains : pluginDomains) {
            for (String domain : domains) {
                if (StringUtils.equalsIgnoreCase(oldhostOld, domain)) {
                    return domains[0];
                }
            }
        }
        return oldhostOld;
    }

    public static String getUserID(final String url) {
        String userid = new Regex(url, "https?://u(\\d+)").getMatch(0);
        if (userid == null) {
            userid = new Regex(url, "/fs/(\\d+)").getMatch(0);
        }
        if (userid == null) {
            userid = new Regex(url, "file/(\\d+)-\\d+$").getMatch(0);
        }
        return userid;
    }

    public static String getFileID(final String url) {
        String fileid = new Regex(url, "/fs/(?:\\d+\\-|file/)(\\d+)$").getMatch(0);
        if (fileid == null) {
            fileid = new Regex(url, "/file/\\d+-(\\d+)$").getMatch(0);
        }
        return fileid;
    }

    private static final String TYPE_NEW_1    = "https?://[^/]+/(d)/([a-f0-9\\-]+)(?:\\?(\\d+))?";
    private static final String TYPE_NEW_2    = "https?://[^/]+/(dir)/([a-f0-9\\-]+)(?:\\?(\\d+))?";
    private static final String TYPE_NEW_BOTH = "https?://[^/]+/(d|dir)/([a-f0-9\\-]+)(?:\\?(\\d+))?";
    private static final String WEBAPI_BASE   = "https://webapi.ctfile.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_NEW_1) || param.getCryptedUrl().matches(TYPE_NEW_2)) {
            return crawlFolderNew(param);
        } else {
            return crawlFolderOld(param);
        }
    }

    /** 2021-08-10: New */
    public ArrayList<DownloadLink> crawlFolderNew(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_NEW_BOTH);
        /* Root-ID of a folder */
        final String folderBaseID = urlinfo.getMatch(1);
        /* ID that goes to specific subfolder */
        final String folderID = urlinfo.getMatch(2);
        prepAjax(this.br);
        final UrlQuery query = new UrlQuery();
        query.add("path", urlinfo.getMatch(0));
        query.add("d", folderBaseID);
        query.add("folder_id", folderID != null ? folderID : "");
        query.add("token", "false");
        query.add("ref", Encoding.urlEncode(param.getCryptedUrl()));
        br.getHeaders().put("Origin", "https://" + Browser.getHost(param.getCryptedUrl()));
        br.getHeaders().put("Referer", param.getCryptedUrl());
        String passCode = param.getDecrypterPassword();
        Map<String, Object> folderinfo = null;
        int passwordCounter = 0;
        do {
            passwordCounter += 1;
            query.addAndReplace("passcode", passCode != null ? Encoding.urlEncode(passCode) : "");
            query.addAndReplace("r", "0." + System.currentTimeMillis());
            br.getPage(WEBAPI_BASE + "/getdir.php?" + query.toString());
            folderinfo = restoreFromString(br.toString(), TypeRef.MAP);
            if (((Number) folderinfo.get("code")).intValue() == 401) {
                if (passwordCounter > 3) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                } else {
                    logger.info("Wrong password or password required");
                    passCode = getUserInput("Password?", param);
                    continue;
                }
            } else {
                break;
            }
        } while (true);
        br.getPage(folderinfo.get("url").toString());
        String subfolderpath = this.getAdoptedCloudFolderStructure();
        if (subfolderpath == null) {
            subfolderpath = (String) folderinfo.get("folder_name");
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(subfolderpath);
        final Map<String, Object> folderoverview = restoreFromString(br.toString(), TypeRef.MAP);
        if (((Number) folderoverview.get("iTotalRecords")).intValue() == 0) {
            ret.add(this.createOfflinelink(param.getCryptedUrl(), "EMPTY_FOLDER " + subfolderpath, "EMPTY_FOLDER " + subfolderpath));
            return ret;
        }
        /* This is where the crappy part starts: json containing string-arrays with HTML code... */
        final List<List<Object>> items = (List<List<Object>>) folderoverview.get("aaData");
        final String folderBaseURL;
        if (param.getCryptedUrl().contains("?")) {
            /* User added subfolder --> We need to build the root folder URL on our own. */
            folderBaseURL = param.getCryptedUrl().substring(0, param.getCryptedUrl().lastIndexOf("?"));
        } else {
            /* User added root folder */
            folderBaseURL = param.getCryptedUrl();
        }
        for (final List<Object> item : items) {
            // final String info0 = item.get(0).toString();
            final String info1 = item.get(1).toString();
            final Regex folderRegex = new Regex(info1, "onclick=\"load_subdir\\((\\d+)\\)\">([^<]+)</a>");
            if (folderRegex.matches()) {
                final String subfolderID = folderRegex.getMatch(0);
                final String subfolderName = folderRegex.getMatch(1);
                /* Subfolder */
                final DownloadLink folder = this.createDownloadlink(folderBaseURL + "?" + subfolderID);
                if (passCode != null) {
                    folder.setDownloadPassword(passCode);
                }
                folder.setRelativeDownloadFolderPath(subfolderpath + "/" + subfolderName);
                ret.add(folder);
            } else {
                final Regex fileinfo = new Regex(info1, "href=\"(/f/tempdir-[A-Za-z0-9_\\-]+)\">([^<>\"]+)<");
                final String url = fileinfo.getMatch(0);
                final String filename = fileinfo.getMatch(1);
                final String filesize = item.get(2).toString();
                if (url == null || filename == null || StringUtils.isEmpty(filesize)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink file = this.createDownloadlink(URLHelper.createURL(URLHelper.parseLocation(new URL(param.getCryptedUrl()), url)).toString());
                file.setName(filename);
                file.setDownloadSize(SizeFormatter.getSize(filesize));
                file.setAvailable(true);
                if (passCode != null) {
                    file.setDownloadPassword(passCode);
                }
                file.setRelativeDownloadFolderPath(subfolderpath);
                file.setProperty(PROPERTY_PARENT_DIR, param.getCryptedUrl());
                file._setFilePackage(fp);
                ret.add(file);
            }
        }
        return ret;
    }

    @Deprecated
    public ArrayList<DownloadLink> crawlFolderOld(final CryptedLink param) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String host_current = Browser.getHost(param.getCryptedUrl());
        final String host_new = correctHost(host_current);
        String parameter = param.toString().replace(host_current + "/", host_new + "/");
        prepBrowser(br);
        // lock to one thread!
        synchronized (LOCK) {
            br.getPage(parameter);
            final boolean accessDenied = br.containsHTML("主页分享功能已经关闭，请直接分享文件或文件夹");
            if (br.getHttpConnection().getResponseCode() == 404 || accessDenied || br.containsHTML("(Due to the limitaion of local laws, this url has been disabled!<|该用户还未打开完全共享\\。|您目前无法访问他的资源列表\\。)")) {
                ret.add(this.createOfflinelink(parameter));
                return ret;
            }
            uuid = getUserID(parameter);
            if (uuid == null) {
                logger.warning("Failed to find userid");
                return null;
            }
            if (fuid == null) {
                fuid = "0";
            }
            String fpName = uuid;
            if (!"0".equals(fuid)) {
                // covers sub directories. /u/uuid/fuid/
                fpName = br.getRegex("href=\"/u/" + uuid + "/" + fuid + "\">(.*?)</a>").getMatch(0);
                if (fpName == null && uuid != null) {
                    /* Fallback */
                    fpName = "User " + uuid + " - Sub Directory " + fuid;
                }
            } else {
                fpName = uuid;
            }
            // covers base /u/\d+ directories,
            // no fpName for these as results of base directory returns subdirectories.
            parsePage(ret, parameter);
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    private Browser prepAjax(final Browser prepBr) {
        prepBr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String parameter) throws Exception {
        // "/iajax_guest.php?item=file_act&action=file_list&folder_id=0&uid=1942919&task=file_list&t=1420817115&k=40d90e63574e9dce0af62dfb94aafdf7"
        String ajaxSource = PluginJSonUtils.getJson(br, "sAjaxSource");
        if (StringUtils.isEmpty(ajaxSource)) {
            logger.warning("Can not find 'ajax source' : " + parameter);
            return;
        }
        Browser ajax = br.cloneBrowser();
        prepAjax(ajax);
        ajax.getPage(ajaxSource);
        // ajax.getHttpConnection().getRequest().setHtmlCode(ajax.toString().replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\""));
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(ajax.toString());
        /*
         * 2019-07-08: 'iTotalRecords' only counts for images. If we only have folders, it will return 0 although 'aaData' is present and
         * contains objects!
         */
        // final long totalCount = JavaScriptEngineFactory.toLong(entries.get("iTotalRecords"), 0);
        // if (totalCount == 0) {
        // ret.add(this.createOfflinelink(parameter));
        // return;
        // }
        final List<Object> ressourcelist = (List<Object>) entries.get("aaData");
        List<Object> fileinfo = (List<Object>) entries.get("aaData");
        if (fileinfo.isEmpty()) {
            ret.add(this.createOfflinelink(parameter));
            return;
        }
        for (final Object fileO : ressourcelist) {
            fileinfo = (List<Object>) fileO;
            final String objectIDhtml = (String) fileinfo.get(0);
            final String filehtml = (String) fileinfo.get(1);
            final String filesize = (String) fileinfo.get(2);
            final boolean isFolder = objectIDhtml.contains("folder_ids[]");
            final String objectID = new Regex(objectIDhtml, "value=\"(\\d+)\"").getMatch(0);
            // String url = new Regex(filehtml, "href=\"(/[^<>\"]+)").getMatch(0);
            if (StringUtils.isEmpty(objectID)) {
                /* Skip invalid items */
                continue;
            }
            /* Build url */
            String url;
            if (isFolder) {
                url = "https://" + br.getHost(true) + "/u/" + this.uuid + "/" + objectID;
            } else {
                url = "https://" + br.getHost(true) + "/fs/" + this.uuid + "-" + objectID;
            }
            final String filename = new Regex(filehtml, ">([^<>\"]+)</a>").getMatch(0);
            final DownloadLink dl = this.createDownloadlink(url);
            if (!isFolder) {
                /* Set info only for fileURLs which then go into the hosterplugin! */
                if (filename != null) {
                    dl.setName(filename);
                }
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}