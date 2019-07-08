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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class CtDiskComFolder extends PluginForDecrypt {
    // DEV NOTES
    // protocol: no https.
    // user unique id
    private String        uuid  = null;
    // folder unique id
    private String        fuid  = null;
    private static String agent = null;
    private static Object LOCK  = new Object();

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ctfile.com", "ctdisk.com", "400gb.com", "pipipan.com", "t00y.com", "bego.cc" });
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
            ret.add("https?://[A-Za-z0-9]+\\." + buildHostsPatternPart(domains) + "/(?:dir/.+|u/\\d+/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public CtDiskComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser prepBrowser(Browser prepBr) {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 1000);
        prepBr.setCookiesExclusive(true);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        prepBr.setReadTimeout(3 * 60 * 1000);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        return prepBr;
    }

    protected String correctHost(String host) {
        final List<String[]> pluginDomains = getPluginDomains();
        for (final String[] domains : pluginDomains) {
            for (String domain : domains) {
                if (StringUtils.equalsIgnoreCase(host, domain)) {
                    return domains[0];
                }
            }
        }
        return host;
    }

    public String getUserID(final String url) {
        String userid = new Regex(url, "https?://u(\\d+)").getMatch(0);
        if (userid == null) {
            userid = new Regex(url, "/fs/(\\d+)").getMatch(0);
        }
        if (userid == null) {
            /* Older stype folder URLs */
            userid = new Regex(url, "/u/(\\d+)").getMatch(0);
        }
        return userid;
    }

    public String getFileID(final String url) {
        String fileid = new Regex(url, "/fs/(?:\\d+\\-|file/)(\\d+)$").getMatch(0);
        return fileid;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String host_current = Browser.getHost(param.getCryptedUrl());
        final String host_new = correctHost(host_current);
        String parameter = param.toString().replace(host_current + "/", host_new + "/");
        prepBrowser(br);
        // lock to one thread!
        synchronized (LOCK) {
            getPage(br, parameter);
            final boolean accessDenied = br.containsHTML("主页分享功能已经关闭，请直接分享文件或文件夹");
            if (br.getHttpConnection().getResponseCode() == 404 || accessDenied || br.containsHTML("(Due to the limitaion of local laws, this url has been disabled!<|该用户还未打开完全共享\\。|您目前无法访问他的资源列表\\。)")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
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
            parsePage(decryptedLinks, parameter);
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }

    private Browser prepAjax(Browser prepBr) {
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
        getPage(ajax, ajaxSource);
        // ajax.getHttpConnection().getRequest().setHtmlCode(ajax.toString().replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\""));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(ajax.toString());
        /*
         * 2019-07-08: 'iTotalRecords' only counts for images. If we only have folders, it will return 0 although 'aaData' is present and
         * contains objects!
         */
        // final long totalCount = JavaScriptEngineFactory.toLong(entries.get("iTotalRecords"), 0);
        // if (totalCount == 0) {
        // ret.add(this.createOfflinelink(parameter));
        // return;
        // }
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("aaData");
        ArrayList<Object> fileinfo = (ArrayList<Object>) entries.get("aaData");
        if (fileinfo.isEmpty()) {
            ret.add(this.createOfflinelink(parameter));
            return;
        }
        for (final Object fileO : ressourcelist) {
            fileinfo = (ArrayList<Object>) fileO;
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

    /**
     * site has really bad connective issues, this method helps retry over throwing exception at the first try
     *
     * @author raztoki
     */
    private boolean getPage(Browser ibr, final String url) throws Exception {
        if (ibr == null || url == null) {
            return false;
        }
        final Browser obr = ibr.cloneBrowser();
        boolean failed = false;
        int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            if (failed) {
                long meep = 0;
                while (meep == 0) {
                    meep = new Random().nextInt(4) * 1371;
                }
                Thread.sleep(meep);
                failed = false;
                ibr = obr.cloneBrowser();
            }
            try {
                ibr.getPage(url);
                break;
            } catch (IOException e) {
                if (i == (repeat - 1)) {
                    logger.warning("Exausted retry getPage count");
                    throw e;
                }
                failed = true;
                continue;
            }
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}