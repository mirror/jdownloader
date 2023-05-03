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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.TeraboxCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TeraboxComFolder extends PluginForDecrypt {
    public TeraboxComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "terabox.com", "dubox.com", "4funbox.com", "mirrobox.com", "1024tera.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(web/share/(?:link|init)\\?surl=[A-Za-z0-9\\-_]+(\\&path=[^/]+)?|web/share/videoPlay\\?surl=[A-Za-z0-9\\-_]+\\&dir=[^\\&]+|s/[A-Za-z0-9\\-_]+|(?:[a-z0-9]+/)?sharing/link\\?surl=[A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2021-04-14: Try to avoid captchas */
        return 1;
    }

    public static final String getAppID() {
        return "250528";
    }

    public static final String getClientType() {
        return "0";
    }

    public static final String getChannel() {
        return "dubox";
    }

    public static final void setPasswordCookie(final Browser br, final String host, final String passwordCookie) {
        br.setCookie(host, "BOXCLND", passwordCookie);
    }

    private static final String TYPE_SHORT        = "https?://[^/]+/s/(.+)";
    private static final String TYPE_SHORT_NEW    = "https?://[^/]+/(?:[a-z0-9]+/)?sharing/link\\?surl=([A-Za-z0-9\\-_]+)";
    /* For such URLs leading to single files we'll crawl all items of the folder that file is in -> Makes it easier */
    private static final String TYPE_SINGLE_VIDEO = "https?://[^/]+/web/share/videoPlay\\?surl=([A-Za-z0-9\\-_]+)\\&dir=([^\\&]+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return crawlFolder(param, account, null);
    }

    public ArrayList<DownloadLink> crawlFolder(final CryptedLink param, final Account account, final String targetFileID) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final UrlQuery paramsOfAddedURL = UrlQuery.parse(param.getCryptedUrl());
        String surl;
        String preGivenPath = null;
        /*
         * ContainerURL is really important for the single file items we crawl! It should go to the folder the file is in -> We can't just
         * use the URL which the user has added!
         */
        final String containerURL;
        if (param.getCryptedUrl().matches(TYPE_SHORT)) {
            surl = new Regex(param.getCryptedUrl(), TYPE_SHORT).getMatch(0);
            containerURL = param.getCryptedUrl();
        } else if (param.getCryptedUrl().matches(TYPE_SHORT_NEW)) {
            surl = new Regex(param.getCryptedUrl(), TYPE_SHORT_NEW).getMatch(0);
            containerURL = param.getCryptedUrl();
        } else if (param.getCryptedUrl().matches(TYPE_SINGLE_VIDEO)) {
            surl = paramsOfAddedURL.get("surl");
            preGivenPath = paramsOfAddedURL.get("dir");
            containerURL = "https://www." + this.getHost() + "/web/share/link?surl=" + surl + "&path=" + preGivenPath;
        } else {
            surl = paramsOfAddedURL.get("surl");
            preGivenPath = paramsOfAddedURL.get("path");
            containerURL = param.getCryptedUrl();
        }
        if (!Encoding.isUrlCoded(preGivenPath)) {
            preGivenPath = Encoding.urlEncode(preGivenPath);
        }
        /*
         * Fix surl value - website would usually do this via redirect. Just really strange that whenever that value starts with "1" they
         * just remove this in order to be able to use that id as a param for ajax requests.
         */
        if (surl.startsWith("1")) {
            surl = surl.substring(1, surl.length());
        }
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        /*
         * Login whenever possible. This way we will get direct downloadable URLs right away which we can store --> Saves a LOT of time- and
         * http requests later on!
         */
        if (account != null) {
            ((jd.plugins.hoster.TeraboxCom) plg).login(account, false);
        }
        String passCode = param.getDecrypterPassword();
        boolean trustPassword = passCode != null;
        /**
         * TODO: That is not enough -> We might have to re-use all cookies and/or maybe always store current/new session on account. </br>
         * It is only possible to use one "passwordCookie" at the same time!
         */
        String passwordCookie = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE) : null;
        if (passwordCookie != null) {
            setPasswordCookie(this.br, this.br.getHost(), passwordCookie);
        }
        int page = 1;
        final int maxItemsPerPage = 20;
        final UrlQuery queryFolder = new UrlQuery();
        queryFolder.add("order", "time");
        queryFolder.add("desc", "1");
        queryFolder.add("shorturl", surl);
        if (!StringUtils.isEmpty(preGivenPath)) {
            queryFolder.add("dir", preGivenPath);
        } else {
            queryFolder.add("root", "1");
        }
        /* 2021-04-14 */
        queryFolder.add("app_id", getAppID());
        queryFolder.add("web", "1");
        queryFolder.add("channel", getChannel());
        queryFolder.add("clienttype", getClientType());
        Map<String, Object> entries = null;
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.setFollowRedirects(true);
        if (targetFileID != null) {
            logger.info("Trying to find item with the following fs_id ONLY: " + targetFileID);
        }
        do {
            logger.info("Crawling page: " + page);
            queryFolder.addAndReplace("page", Integer.toString(page));
            queryFolder.addAndReplace("num", Integer.toString(maxItemsPerPage));
            br.getPage("https://www." + this.getHost() + "/share/list?" + queryFolder.toString());
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            int errno = ((Number) entries.get("errno")).intValue();
            if (errno == -9) {
                /* Password protected folder */
                final UrlQuery querypw = new UrlQuery();
                querypw.add("surl", surl);
                querypw.add("app_id", getAppID());
                querypw.add("web", "1");
                querypw.add("channel", getChannel());
                querypw.add("clienttype", getClientType());
                boolean captchaRequired = false;
                int count = 0;
                final int maxTries = 10;
                do {
                    count += 1;
                    logger.info("Captcha/password attempt " + count + " / " + maxTries);
                    /*
                     * Let's trust the password for the first 5 tries if a password existed before as their captchas are hard to solve and
                     * folder passwords usually don't get changed over time.
                     */
                    if (passCode == null || !trustPassword || count > 5) {
                        passCode = getUserInput("Password?", param);
                    }
                    errno = ((Number) entries.get("errno")).intValue();
                    final UrlQuery querypwPOST = new UrlQuery();
                    querypwPOST.appendEncoded("pwd", passCode);
                    /* 2021-04-14: Captcha only happens when adding a lot of items within a short amount of time. */
                    if (errno == -62) {
                        captchaRequired = true;
                        br.getPage("/api/getcaptcha?prod=shareverify&app_id=" + getAppID() + "&web=1&channel=" + getChannel() + "&clienttype=" + getClientType());
                        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                        final String captchaurl = (String) entries.get("vcode_img");
                        final String code = this.getCaptchaCode(captchaurl, param);
                        querypwPOST.appendEncoded("vcode", code);
                        querypwPOST.add("vcode_str", (String) entries.get("vcode_str"));
                    } else {
                        querypwPOST.add("vcode", "");
                        querypwPOST.add("vcode_str", "");
                    }
                    br.postPage("/share/verify?" + querypw.toString(), querypwPOST);
                    entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    errno = ((Number) entries.get("errno")).intValue();
                    passwordCookie = (String) entries.get("randsk");
                    if (!StringUtils.isEmpty(passwordCookie)) {
                        break;
                    } else {
                        if (count >= maxTries) {
                            logger.info("Giving up");
                            break;
                        } else {
                            logger.info("Wrong password or captcha");
                            continue;
                        }
                    }
                } while (!this.isAbort());
                if (passwordCookie == null) {
                    logger.info("Wrong password and/or captcha");
                    /* Assume wrong captcha if one was required */
                    if (captchaRequired) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                }
                setPasswordCookie(this.br, this.br.getHost(), passwordCookie);
                /*
                 * Let's assume that dubox can ask again for password/captchas withing a long pagination -> Don't ask for password again as
                 * we know it!
                 */
                trustPassword = true;
                /* Repeat the first request -> We should be able to access the folder now. */
                br.getPage("https://www." + this.getHost() + "/share/list?" + queryFolder.toString());
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            }
            errno = ((Number) entries.get("errno")).intValue();
            if (errno != 0 || !entries.containsKey("list")) {
                logger.info("Assume that this folder is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final List<Object> ressourcelist = (List<Object>) entries.get("list");
            if (ressourcelist.isEmpty()) {
                logger.info("Stopping because: Current page doesn't contain any items");
                break;
            }
            for (final Object ressourceO : ressourcelist) {
                entries = (Map<String, Object>) ressourceO;
                final String path = (String) entries.get("path");
                /* 2021-04-14: 'category' is represented as a String. */
                final long category = JavaScriptEngineFactory.toLong(entries.get("category"), -1);
                if (JavaScriptEngineFactory.toLong(entries.get("isdir"), -1) == 1) {
                    final String url = "https://www." + this.getHost() + "/web/share/link?surl=" + surl + "&path=" + Encoding.urlEncode(path);
                    final DownloadLink folder = this.createDownloadlink(url);
                    if (passCode != null) {
                        folder.setDownloadPassword(passCode);
                    }
                    /* Saving- and re-using this can save us some time later. */
                    if (passwordCookie != null) {
                        folder.setProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookie);
                    }
                    distribute(folder);
                    decryptedLinks.add(folder);
                } else {
                    final String serverfilename = (String) entries.get("server_filename");
                    // final long fsid = JavaScriptEngineFactory.toLong(entries.get("fs_id"), -1);
                    final String fsidStr = Long.toString(JavaScriptEngineFactory.toLong(entries.get("fs_id"), -1));
                    final String realpath;
                    if (path.endsWith("/" + serverfilename)) {
                        realpath = path.replaceFirst("/" + org.appwork.utils.Regex.escape(serverfilename) + "$", "");
                    } else {
                        realpath = path;
                    }
                    final UrlQuery thisparams = new UrlQuery();
                    thisparams.add("surl", surl);
                    thisparams.appendEncoded("dir", realpath);// only the path!
                    thisparams.add("fsid", fsidStr);
                    thisparams.appendEncoded("fileName", serverfilename);
                    final String url = "https://www." + this.getHost() + "/web/share/?" + thisparams.toString();
                    final String contentURL;
                    if (category == 1) {
                        thisparams.add("page", Integer.toString(page));
                        contentURL = "https://www." + this.getHost() + "/web/share/videoPlay?" + thisparams.toString();
                    } else {
                        /* No URL available that points directly to that file! */
                        contentURL = param.toString();
                    }
                    final DownloadLink dl = new DownloadLink(plg, "dubox", this.getHost(), url, true);
                    dl.setContentUrl(contentURL);
                    dl.setContainerUrl(containerURL);
                    jd.plugins.hoster.TeraboxCom.parseFileInformation(dl, entries);
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    /* Saving- and re-using this can save us some time later. */
                    if (passwordCookie != null) {
                        dl.setProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookie);
                    }
                    /* This can be useful to refresh directurls a lot quicker. */
                    dl.setProperty(TeraboxCom.PROPERTY_PAGINATION_PAGE, page);
                    if (realpath.length() > 1) {
                        dl.setRelativeDownloadFolderPath(realpath);
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(realpath);
                        dl._setFilePackage(fp);
                    }
                    if (targetFileID == null) {
                        /* We want to crawl all items. */
                        distribute(dl);
                        decryptedLinks.add(dl);
                    } else if (!StringUtils.equalsIgnoreCase(fsidStr, targetFileID)) {
                        /* we' re looking for a single item but this is not it. */
                        decryptedLinks.add(dl);
                    } else {
                        /* We're looking for a single item and found it! */
                        decryptedLinks.clear();
                        decryptedLinks.add(dl);
                        logger.info("Stopping because: Found item matching target fileID: " + targetFileID);
                        return decryptedLinks;
                    }
                }
            }
            if (ressourcelist.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than: " + maxItemsPerPage + " (only " + ressourcelist.size() + ")");
                break;
            } else {
                logger.info("Number of items found on current page: " + ressourcelist.size());
                page++;
                continue;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
