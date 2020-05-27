//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "box.com" }, urls = { "https?://(?:\\w+\\.)*box\\.(?:net|com)/s(?:hared)?/([a-z0-9]{32}|[a-z0-9]{20})(?:/(?:folder|file)/(\\d+))?" })
public class BoxCom extends antiDDoSForDecrypt {
    private static final String            TYPE_APP      = "https?://(?:\\w+\\.)*box\\.(?:net|com)/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})(?:/folder/\\d+)?";
    private static final String            TYPE_APP_FILE = "https?://[^/]+/s(?:hared)?/([a-z0-9]{32}|[a-z0-9]{20})/file/(\\d+)";
    private String                         cryptedlink   = null;
    private static AtomicReference<String> lastValidPW   = new AtomicReference<String>(null);

    public BoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected String handlePassword(Browser br, final List<String> passCodes, CryptedLink parameter) throws Exception {
        synchronized (BoxCom.lastValidPW) {
            final List<String> tryPassCodes = new ArrayList<String>();
            if (passCodes != null) {
                tryPassCodes.addAll(passCodes);
            }
            final String lastValidPW = BoxCom.lastValidPW.get();
            if (lastValidPW != null && !tryPassCodes.contains(lastValidPW)) {
                tryPassCodes.add(lastValidPW);
            }
            int retry = 5 + tryPassCodes.size();
            String password = null;
            while (retry-- > 0 && isPasswordProtected(br)) {
                if (isAbort()) {
                    throw new InterruptedException();
                }
                if (tryPassCodes.size() > 0) {
                    password = tryPassCodes.remove(0);
                }
                if (StringUtils.isEmpty(password)) {
                    password = Plugin.getUserInput(null, parameter);
                }
                if (!StringUtils.isEmpty(password)) {
                    sleep(1000, parameter);
                    final PostRequest request = br.createPostRequest(br.getURL(), "password=" + Encoding.urlEncode(password));
                    br.getPage(request);
                    if (isPasswordProtected(br)) {
                        password = null;
                    }
                } else {
                    break;
                }
            }
            if (isPasswordProtected(br)) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else {
                if (password != null) {
                    BoxCom.lastValidPW.set(password);
                }
                return password;
            }
        }
    }

    public void getPage(final CryptedLink parameter, String url) throws Exception {
        getPage(url);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            sleep(1000, parameter);
            getPage(url);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, final ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        cryptedlink = parameter.toString().replace("box.net/", "box.com/");
        logger.finer("Decrypting: " + cryptedlink);
        br.setFollowRedirects(true);
        final List<String> passCodes = new ArrayList<String>();
        CrawledLink current = getCurrentLink();
        while (current != null) {
            if (current.getDownloadLink() != null && getSupportedLinks().matcher(current.getURL()).matches()) {
                final String pass = current.getDownloadLink().getStringProperty("passCode", null);
                if (pass != null) {
                    passCodes.add(pass);
                    break;
                }
            }
            current = current.getSourceLink();
        }
        String passCode = null;
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        if (cryptedlink.matches(".+/folder/\\d+")) {
            final String rootFolder = new Regex(cryptedlink, "(.+)/folder/\\d+").getMatch(0);
            getPage(parameter, rootFolder);
            passCode = handlePassword(br, passCodes, parameter);
            if (passCode != null && !passCodes.contains(passCode)) {
                passCodes.add(0, passCode);
            }
        }
        if (cryptedlink.toString().matches(TYPE_APP_FILE)) {
            /* Single file - Pass this to host plugin */
            final String sharedname = new Regex(cryptedlink, TYPE_APP_FILE).getMatch(0);
            final String itemID = new Regex(cryptedlink, TYPE_APP_FILE).getMatch(1);
            final DownloadLink dl = this.createDownloadlink(String.format("https://app.box.com/s/%s/file/%s", sharedname, itemID));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        getPage(parameter, cryptedlink);
        final String passCodeBefore = passCode;
        passCode = handlePassword(br, passCodes, parameter);
        if (passCode == null) {
            passCode = passCodeBefore;
        }
        if (br._getURL().getPath().equals("/freeshare")) {
            decryptedLinks.add(createOfflinelink(cryptedlink));
            return decryptedLinks;
        } else if (jd.plugins.hoster.BoxCom.isOffline(br)) {
            decryptedLinks.add(createOfflinelink(cryptedlink));
            return decryptedLinks;
        }
        decryptedLinks.addAll(decryptApp(parameter, passCode));
        return decryptedLinks;
    }

    private boolean isPasswordProtected(final Browser br) {
        return (br.containsHTML("passwordRequired") || br.containsHTML("incorrectPassword")) && br.containsHTML("\"status\"\\s*:\\s*403");
    }

    private ArrayList<DownloadLink> decryptApp(CryptedLink cryptedLink, final String passCode) throws Exception {
        final String sharedname = new Regex(cryptedlink, this.getSupportedLinks()).getMatch(0);
        final String itemID = new Regex(cryptedlink, this.getSupportedLinks()).getMatch(1);
        String offlinename = sharedname;
        if (itemID != null) {
            offlinename += "_" + itemID;
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final HashSet<String> dupe = new HashSet<String>();
        final String fpName = PluginJSonUtils.getJson(br, "currentFolderName");
        final FilePackage fp = !StringUtils.isEmpty(fpName) ? FilePackage.getInstance() : null;
        if (fp != null) {
            fp.setName(Encoding.unicodeDecode(fpName));
        }
        String subFolder = getAdoptedCloudFolderStructure();
        if (subFolder == null) {
            subFolder = fpName;
        }
        if (subFolder == null) {
            subFolder = "";
        }
        do {
            final String json = br.getRegex("<script>\\s*Box\\.postStreamData\\s*=\\s*(\\{.*?\\});\\s*</script>").getMatch(0);
            Map<String, Object> rootMap = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            rootMap = (LinkedHashMap<String, Object>) rootMap.get("/app-api/enduserapp/shared-folder");
            final long pageNumber = JavaScriptEngineFactory.toLong(rootMap.get("pageNumber"), 1);
            final long pageCount = JavaScriptEngineFactory.toLong(rootMap.get("pageCount"), 1);
            logger.info("Crawling page " + pageNumber + " of " + pageCount);
            Map<String, Object> entries;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) rootMap.get("items");
            if (ressourcelist.size() == 0) {
                logger.info("Empty folder");
                decryptedLinks.add(this.createOfflinelink(cryptedlink.toString(), "Folder_empty_" + offlinename, "Empty folder"));
                return decryptedLinks;
            }
            for (final Object itemO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) itemO;
                final String type = (String) entries.get("type");
                final String item_name = (String) entries.get("name");
                final long fuid = JavaScriptEngineFactory.toLong(entries.get("id"), 0);
                if (StringUtils.isEmpty(type) || StringUtils.isEmpty(item_name) || fuid == 0) {
                    /* Skip invalid items --> This should never happen */
                    continue;
                }
                if ("file".equals(type)) {
                    final long filesize = JavaScriptEngineFactory.toLong(entries.get("itemSize"), 0);
                    final String link = new Regex(cryptedlink, "(https?://[^/]*?box\\.com/s/[a-z0-9]+)").getMatch(0) + "/file/" + fuid;
                    // logger.info("cryptedlink: " + cryptedlink);
                    // logger.info("link: " + link);
                    if (!dupe.add(link)) {
                        continue;
                    }
                    final DownloadLink dl = createDownloadlink(link);
                    dl.setLinkID("box.com://file/" + fuid);
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    dl.setName(Encoding.unicodeDecode(item_name));
                    if (filesize > 0) {
                        dl.setVerifiedFileSize(filesize);
                    }
                    dl.setAvailable(true);
                    if (StringUtils.isNotEmpty(subFolder)) {
                        dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subFolder);
                    }
                    decryptedLinks.add(dl);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    distribute(dl);
                } else {
                    // directory
                    final String rootFolder = new Regex(cryptedlink, "(.+)/folder/\\d+").getMatch(0);
                    final String link;
                    if (rootFolder != null) {
                        link = rootFolder + "/folder/" + fuid;
                    } else {
                        link = cryptedlink + "/folder/" + fuid;
                    }
                    if (!dupe.add(link)) {
                        continue;
                    }
                    final DownloadLink dl = createDownloadlink(link);
                    dl.setLinkID("box.com://folder/" + fuid);
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    final String thisSubfolder = subFolder + "/" + item_name;
                    dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, thisSubfolder);
                    decryptedLinks.add(dl);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    distribute(dl);
                }
            }
            if (pageCount > pageNumber) {
                logger.info("Accessing next page");
                final long nextPage = pageNumber + 1;
                getPage(cryptedLink, cryptedlink + "?page=" + nextPage);
                continue;
            } else {
                logger.info("Seems like we've crawled everything");
                break;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}