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
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "onedrive.live.com" }, urls = { "https?://(www\\.)?(onedrive\\.live\\.com/.+|skydrive\\.live\\.com/.+|(sdrv|1drv)\\.ms/[A-Za-z0-9&!=#\\.,-_]+)" })
public class OneDriveLiveCom extends PluginForDecrypt {
    public OneDriveLiveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_DRIVE_ALL               = "https?://(www\\.)?(onedrive\\.live\\.com/(redir)?\\?[A-Za-z0-9\\&\\!=#\\.,]+|skydrive\\.live\\.com/(\\?cid=[a-z0-9]+[A-Za-z0-9&!=#\\.,-_]+|redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9&!=#\\.,-_]+|redir\\?resid=[A-Za-z0-9&!=#\\.,-_]+))";
    private static final String TYPE_ONEDRIVE_REDIRECT_RESID = "https?://(www\\.)?onedrive\\.live\\.com/redir\\?resid=[a-z0-9]+[A-Za-z0-9&!=#\\.,-_]+";
    private static final String TYPE_SKYDRIVE_REDIRECT_RESID = "https?://(www\\.)?skydrive\\.live\\.com/redir\\?resid=[a-z0-9]+[A-Za-z0-9&!=#\\.,-_]+";
    private static final String TYPE_ONEDRIVE_VIEW_RESID     = "https?://(www\\.)?onedrive\\.live\\.com/view\\.aspx\\?resid=.+";
    private static final String TYPE_SKYDRIVE_REDIRECT       = "https?://(www\\.)?skydrive\\.live\\.com/redir\\.aspx\\?cid=[a-z0-9]+[A-Za-z0-9&!=#\\.,-_]+";
    private static final String TYPE_SKYDRIVE_SHORT          = "https?://(www\\.)?(1|s)drv\\.ms/[A-Za-z0-9&!=#\\.,-_]+";
    private static final String TYPE_ONEDRIVE_ROOT           = "https?://onedrive\\.live\\.com/\\?cid=[a-z0-9]+";
    /* Constants */
    public static final int     MAX_ENTRIES_PER_REQUEST      = 200;
    private static final long   ITEM_TYPE_FILE               = 1;
    private static final long   ITEM_TYPE_PICTURE            = 3;
    private static final long   ITEM_TYPE_VIDEO              = 5;
    private static final int    ITEM_TYPE_FOLDER             = 32;
    /* Plugin settings */
    private static final String DOWNLOAD_ZIP                 = "DOWNLOAD_ZIP_2";
    private String              parameter                    = null;
    private String              original_link                = null;
    private String              cid                          = null;
    private String              id                           = null;
    private String              authkey                      = null;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = Encoding.urlDecode(param.toString(), false);
        original_link = parameter;
        CrawledLink current = getCurrentLink();
        String subFolderBase = "";
        while (current != null) {
            if (current.getDownloadLink() != null && getSupportedLinks().matcher(current.getURL()).matches()) {
                final String path = current.getDownloadLink().getStringProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, null);
                if (path != null) {
                    subFolderBase = path;
                }
                break;
            }
            current = current.getSourceLink();
        }
        br.setLoadLimit(Integer.MAX_VALUE);
        final DownloadLink main = createDownloadlink("http://onedrivedecrypted.live.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        try {
            FilePackage fp = null;
            if (parameter.matches(TYPE_SKYDRIVE_REDIRECT)) {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = new Regex(parameter, "(?:&|\\?)resid=([A-Za-z0-9]+\\!\\d+)").getMatch(0);
            } else if (parameter.matches(TYPE_ONEDRIVE_REDIRECT_RESID) || parameter.matches(TYPE_SKYDRIVE_REDIRECT_RESID) || parameter.matches(TYPE_ONEDRIVE_VIEW_RESID)) {
                final Regex fInfo = new Regex(parameter, "\\?resid=([A-Za-z0-9]+)(\\!\\d+)");
                cid = fInfo.getMatch(0);
                id = cid + fInfo.getMatch(1);
            } else if (parameter.matches(TYPE_ONEDRIVE_ROOT)) {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
            } else if (parameter.matches(TYPE_DRIVE_ALL)) {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = getLastID(parameter);
            } else if (parameter.matches(TYPE_SKYDRIVE_SHORT)) {
                br.getPage(parameter);
                String redirect = br.getRedirectLocation();
                if (redirect == null) {
                    return null;
                }
                redirect = Encoding.htmlDecode(redirect);
                if (!redirect.contains("live")) {
                    br.getPage(redirect);
                    redirect = br.getRedirectLocation();
                    if (redirect == null) {
                        return null;
                    }
                }
                redirect = Encoding.htmlDecode(redirect);
                /* Don't set redirect url as original source - we need the correct current url as source! */
                original_link = redirect;
                cid = new Regex(redirect, "cid=([A-Za-z0-9]*)").getMatch(0);
                if (cid == null) {
                    cid = new Regex(redirect, "resid=([A-Z0-9]+)").getMatch(0);
                }
                id = new Regex(redirect, "resid=([A-Za-z0-9]+\\!\\d+)").getMatch(0);
                if (id == null) {
                    id = getLastID(parameter);
                }
                authkey = new Regex(redirect, "(?:&|\\?)authkey=(\\![A-Za-z0-9\\-_]+)").getMatch(0);
            } else {
                cid = new Regex(parameter, "cid=([A-Za-z0-9]*)").getMatch(0);
                id = getLastID(parameter);
            }
            if (authkey == null) {
                authkey = new Regex(parameter, "(?:&|\\?)authkey=(\\![A-Za-z0-9\\-_]+)").getMatch(0);
            }
            if (!parameter.matches(TYPE_ONEDRIVE_ROOT) && (cid == null || id == null)) {
                if (cid != null) {
                    main.setFinalFileName(cid);
                } else {
                    main.setFinalFileName(new Regex(parameter, "\\.com/(.+)").getMatch(0));
                }
                main.setAvailable(false);
                main.setProperty("offline", true);
                decryptedLinks.add(main);
                return decryptedLinks;
            }
            cid = cid.toUpperCase();
            parameter = "https://onedrive.live.com/?cid=" + cid;
            if (id != null) {
                parameter += "&id=" + id;
            }
            param.setCryptedUrl(parameter);
            prepBrAPI(this.br);
            final String additional_data;
            if (authkey != null) {
                additional_data = "&authkey=" + Encoding.urlEncode(authkey);
            } else {
                additional_data = "";
            }
            int startIndex = 0;
            int nextStartIndex = 0;
            int lastSize = -1;
            while (!isAbort()) {
                if (decryptedLinks.size() == lastSize) {
                    break;
                }
                startIndex = nextStartIndex;
                accessItems_API(this.br, original_link, cid, id, additional_data, startIndex, MAX_ENTRIES_PER_REQUEST);
                nextStartIndex = startIndex + MAX_ENTRIES_PER_REQUEST;
                lastSize = decryptedLinks.size();
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                final Object error = entries.get("error");
                if (error != null) {
                    /*
                     * E.g. "{"error":{"code":3000,"debugMessage":"Item not found or access denied.","isExpected":true,"message":"This item
                     * might have been deleted, expired, or you might not have permission to access it. Contact the owner of this item for
                     * more information."....
                     */
                    final DownloadLink offline = this.createOfflinelink(parameter);
                    offline.setFinalFileName(new Regex(parameter, "onedrive\\.live\\.com/(.+)").getMatch(0));
                    if (fp != null) {
                        fp.add(offline);
                    }
                    decryptedLinks.add(offline);
                    return decryptedLinks;
                }
                ArrayList<Object> ressourcelist = (ArrayList) entries.get("items");
                entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
                if (fp == null) {
                    final String fpName = (String) entries.get("name");
                    if (fpName != null) {
                        fp = FilePackage.getInstance();
                        fp.setName(fpName);
                    }
                }
                final long totalItemType = JavaScriptEngineFactory.toLong(entries.get("itemType"), -1);
                if (totalItemType == ITEM_TYPE_FILE || totalItemType == ITEM_TYPE_PICTURE || totalItemType == ITEM_TYPE_VIDEO) {
                    /* Single file */
                    final DownloadLink link = parseFile(entries, startIndex, MAX_ENTRIES_PER_REQUEST);
                    if (fp != null) {
                        fp.add(link);
                    }
                    if (StringUtils.isNotEmpty(subFolderBase)) {
                        link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolderBase);
                    }
                    decryptedLinks.add(link);
                    distribute(link);
                } else {
                    entries = (LinkedHashMap<String, Object>) entries.get("folder");
                    ressourcelist = (ArrayList) entries.get("children");
                    if (fp == null) {
                        /* This should NEVER happen */
                        fp = FilePackage.getInstance();
                        fp.setName("onedrive.live.com content of user " + cid + " - folder - " + id);
                    }
                    /* Folder, maybe with subfolders */
                    final long totalCount = JavaScriptEngineFactory.toLong(entries.get("totalCount"), 0);
                    final long childCount = JavaScriptEngineFactory.toLong(entries.get("childCount"), 0);
                    if (br.containsHTML("\"code\":154")) {
                        if (fp != null) {
                            main.setFinalFileName(fp.getName());
                        }
                        main.setAvailable(false);
                        main.setProperty("offline", true);
                        if (fp != null) {
                            fp.add(main);
                        }
                        decryptedLinks.add(main);
                        return decryptedLinks;
                    } else if (totalCount == 0 && childCount == 0) {
                        if (fp != null) {
                            main.setFinalFileName(fp.getName());
                        }
                        main.setAvailable(false);
                        main.setProperty("offline", true);
                        if (fp != null) {
                            fp.add(main);
                        }
                        decryptedLinks.add(main);
                        return decryptedLinks;
                    }
                    main.setProperty("mainlink", parameter);
                    main.setProperty("original_link", original_link);
                    main.setProperty("plain_cid", cid);
                    main.setProperty("plain_id", id);
                    main.setProperty("plain_authkey", authkey);
                    for (final Object ressource : ressourcelist) {
                        final LinkedHashMap<String, Object> entry = (LinkedHashMap<String, Object>) ressource;
                        final long type = ((Number) entry.get("itemType")).longValue();
                        final String item_id = (String) entry.get("id");
                        final String creatorCid = (String) entry.get("creatorCid");
                        if (type == ITEM_TYPE_FOLDER) {
                            /* Folder --> Goes back into decrypter */
                            if (item_id == null || creatorCid == null) {
                                logger.warning("Decrypter broken for link: " + parameter);
                                return null;
                            }
                            String folderlink = "https://onedrive.live.com/?cid=" + creatorCid + "&id=" + item_id;
                            if (authkey != null) {
                                /* Don't forget to add authKey if needed */
                                folderlink += "&authkey=" + authkey;
                            }
                            final DownloadLink dl = createDownloadlink(folderlink);
                            final String name = (String) entry.get("name");
                            if (StringUtils.isNotEmpty(name)) {
                                final String path;
                                if (StringUtils.isNotEmpty(subFolderBase)) {
                                    path = subFolderBase + "/" + name;
                                } else {
                                    path = name;
                                }
                                dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
                            }
                            decryptedLinks.add(dl);
                        } else {
                            /* File --> Grab information & return to decrypter. All found links are usually ONLINE and downloadable! */
                            final DownloadLink link = parseFile(entry, startIndex, MAX_ENTRIES_PER_REQUEST);
                            if (fp != null) {
                                fp.add(link);
                            }
                            if (StringUtils.isNotEmpty(subFolderBase)) {
                                link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolderBase);
                            }
                            decryptedLinks.add(link);
                            distribute(link);
                        }
                    }
                    // if (decryptedLinks.size() > 1 && SubConfiguration.getConfig("onedrive.live.com").getBooleanProperty(DOWNLOAD_ZIP,
                    // false))
                    // {
                    // /* = all files (links) of the folder as .zip archive */
                    // if (fp != null) {
                    // final String main_name = fp.getName() + ".zip";
                    // main.setFinalFileName(main_name);
                    // main.setProperty("plain_name", main_name);
                    // }
                    // main.setProperty("plain_size", Long.toString(totalSize));
                    // main.setProperty("complete_folder", true);
                    // /* TODO: */
                    // // decryptedLinks.add(main);
                    // }
                }
            }
        } catch (final BrowserException e) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "onedrive\\.live\\.com/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private DownloadLink parseFile(final LinkedHashMap<String, Object> entry, final int startIndex, final int maxItems) throws DecrypterException {
        /* File --> Grab information & return to decrypter. All found links are usually ONLINE and downloadable! */
        final LinkedHashMap<String, Object> urls = (LinkedHashMap<String, Object>) entry.get("urls");
        final String name = (String) entry.get("name");
        final String view_url = (String) urls.get("viewInBrowser");
        final String iconType = (String) entry.get("iconType");
        final String extension = (String) entry.get("extension");
        /* For single pictures, get the highest quality pic */
        if ("Photo".equals(iconType) && extension == null) {
            /* Download and view of the original picture only possible via account */
            // br.getPage("https://onedrive.live.com/download.aspx?cid=" + cid + "&resid=" + Encoding.urlEncode(id) +
            // "&canary=");
            // download_url = br.getRedirectLocation();
            // final String photoLinks[] = new Regex(singleinfo, "\"streamVersion\":\\d+,\"url\":\"([^<>\"]*?)\"").getColumn(0);
            // if (photoLinks != null && photoLinks.length != 0) {
            // download_url = "https://dm" + photoLinks[photoLinks.length - 1];
            // }
            /* TODO: */
            throw new DecrypterException("Decrypter broken");
        }
        if (name == null) {
            throw new DecrypterException("Decrypter broken");
        }
        final DownloadLink dl = createDownloadlink("http://onedrivedecrypted.live.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
        /* Files without extension == possible */
        final String filename;
        if (extension != null) {
            filename = name + extension;
        } else {
            filename = name;
        }
        final long size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
        if (size >= 0) {
            dl.setVerifiedFileSize(size);
        }
        dl.setFinalFileName(filename);
        dl.setProperty("mainlink", parameter);
        dl.setProperty("original_link", original_link);
        dl.setProperty("plain_name", filename);
        dl.setProperty("plain_filesize", size);
        dl.setContentUrl(view_url);
        final String download_url = (String) urls.get("download");
        if (download_url != null) {
            dl.setProperty("plain_download_url", download_url);
        } else {
            dl.setProperty("account_only", true);
        }
        final String itemId = (String) entry.get("id");
        dl.setProperty("plain_item_id", itemId);
        dl.setProperty("plain_cid", cid);
        dl.setProperty("plain_id", id);
        dl.setProperty("plain_authkey", authkey);
        dl.setProperty("plain_item_si", startIndex);
        dl.setLinkID(getHost() + "://" + id + "/" + itemId);
        dl.setAvailable(true);
        return dl;
    }

    public static String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        }
        return result;
    }

    private String getLastID(final String parameter) {
        /* Get last ID */
        int pos = parameter.lastIndexOf("&id=") + 4;
        final String parameter_part = parameter.substring(pos, parameter.length());
        return new Regex(parameter_part, "([A-Z0-9]+\\!\\d+)").getMatch(0);
    }

    public static String getLinktext(final Browser br) {
        String linktext = br.getRegex("\"children\":\\[(\\{.*?\\})\\],\"covers\":").getMatch(0);
        if (linktext == null) {
            linktext = br.getRegex("\"children\":\\[(\\{.*?\\})\\],\"defaultSort\":\\d+").getMatch(0);
        }
        // Check for single pictures: https://onedrive.live.com/?cid=E0615573A3471F93&id=E0615573A3471F93!1567
        if (linktext == null) {
            linktext = br.getRegex("\"items\":\\[(\\{.*?\\})\\]").getMatch(0);
        }
        if (linktext == null) {
            linktext = br.getRegex("\"children\":\\[(.*?)\\],\"defaultSort\":").getMatch(0);
        }
        return linktext;
    }

    public static void prepBrAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("X-ForceCache", "1");
        br.getHeaders().put("X-SkyApiOriginId", "0.9554840477898046");
        br.getHeaders().put("Referer", "https://skyapi.onedrive.live.com/api/proxy?v=3");
        br.getHeaders().put("AppId", "1141147648");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        br.setAllowedResponseCodes(500);
    }

    /* TODO: Once it breaks down next time: Simply make a hashMap that contains the needed post data... */
    public static void accessItems_API(final Browser br, final String original_link, final String cid, final String id, final String additional, final int startIndex, final int maxItems) throws IOException {
        final String v = "0.10707631620552516";
        String data = null;
        final String fromTo = "&si=" + startIndex + "&ps=" + maxItems;
        if (original_link.contains("ithint=") && id != null) {
            data = "&cid=" + Encoding.urlEncode(cid) + additional;
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?id=" + id + "&group=0&qt=&ft=&sb=1&sd=1&gb=0%2C1%2C2&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
        } else if (id == null && original_link.matches(TYPE_ONEDRIVE_ROOT)) {
            /* Access root-dir */
            data = "&cid=" + Encoding.urlEncode(cid);
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?id=root&group=0&qt=&ft=&sb=0&sd=0&gb=0%2C1%2C2&rif=0&d=1&iabch=1&caller=unauth&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
        } else {
            data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(id) + additional;
            boolean failed = false;
            br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=unauth&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
            /* Maybe the folder is empty but we can move one up and get its contents... */
            if (br.getRequest().getHttpConnection().getResponseCode() == 500 || getLinktext(br) == null) {
                br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0%2C1%2C2&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
                final String parentID = getJson("parentId", br.toString());
                if (parentID != null) {
                    /* Error 500 will happen on invalid API requests */
                    data = "&cid=" + Encoding.urlEncode(cid) + "&id=" + Encoding.urlEncode(parentID) + "&sid=" + Encoding.urlEncode(id) + additional;
                    br.getPage("https://skyapi.onedrive.live.com/API/2/GetItems?group=0&qt=&ft=&sb=0&sd=0&gb=0&d=1&iabch=1&caller=&path=1&pi=5&m=de-DE&rset=skyweb&lct=1&v=" + v + data + fromTo);
                }
            }
        }
    }
}
