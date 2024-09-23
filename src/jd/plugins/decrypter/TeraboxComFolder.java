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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
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
import jd.plugins.Plugin;
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
        ret.add(new String[] { "terabox.com", "teraboxapp.com", "dubox.com", "4funbox.com", "mirrobox.com", "1024tera.com", "1024terabox.com", "terabox.app", "gibibox.com" });
        return ret;
    }

    public static List<String> getDeadDomains() {
        final List<String> ret = new ArrayList<String>();
        /* 2024-02-12: They still own this domain but older URLs will redirect to an error page while the content may still be online. */
        ret.add("dubox.com");
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(web/share/(?:init|link|filelist)\\?surl=[A-Za-z0-9\\-_]+.*|web/share/videoPlay\\?surl=[A-Za-z0-9\\-_]+\\&dir=[^\\&]+|s/[A-Za-z0-9\\-_]+|(?:[a-z0-9]+/)?sharing/link\\?surl=[A-Za-z0-9\\-_]+.*)");
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

    private static final Pattern           TYPE_SHORT                = Pattern.compile("https?://[^/]+/s/(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern           TYPE_SHORT_NEW            = Pattern.compile("https?://[^/]+/(?:[a-z0-9]+/)?sharing/link\\?surl=([A-Za-z0-9\\-_]+).*", Pattern.CASE_INSENSITIVE);
    /* For such URLs leading to single files we'll crawl all items of the folder that file is in -> Makes it easier */
    private static final Pattern           TYPE_SINGLE_VIDEO         = Pattern.compile("https?://[^/]+/web/share/videoPlay\\?surl=([A-Za-z0-9\\-_]+)\\&dir=([^\\&]+)", Pattern.CASE_INSENSITIVE);
    private static final AtomicLong        anonymousJstokenTimestamp = new AtomicLong(-1);
    private static AtomicReference<String> anonymousJstoken          = new AtomicReference<String>(null);

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return crawlFolder(this, param, account, null);
    }

    public ArrayList<DownloadLink> crawlFolder(final Plugin callingPlugin, final CryptedLink param, final Account account, final String targetFileID) throws Exception {
        String contenturl = param.getCryptedUrl();
        final List<String> deadDomains = getDeadDomains();
        final String domainOfAddedURL = Browser.getHost(contenturl);
        if (deadDomains.contains(domainOfAddedURL)) {
            /* Fix domain inside URL */
            contenturl = contenturl.replaceFirst(Pattern.quote(domainOfAddedURL) + "/", getHost() + "/");
        }
        final UrlQuery paramsOfAddedURL = UrlQuery.parse(contenturl);
        String surl = null;
        String preGivenPath = null;
        if (new Regex(contenturl, TYPE_SHORT).patternFind()) {
            surl = new Regex(contenturl, TYPE_SHORT).getMatch(0);
        } else {
            surl = paramsOfAddedURL.get("surl");
        }
        preGivenPath = paramsOfAddedURL.get("dir");
        if (preGivenPath == null) {
            preGivenPath = paramsOfAddedURL.get("path");
        }
        if (surl == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!Encoding.isUrlCoded(preGivenPath)) {
            preGivenPath = Encoding.urlEncode(preGivenPath);
        }
        final TeraboxCom plg = (TeraboxCom) this.getNewPluginForHostInstance(this.getHost());
        /*
         * Login whenever possible. This way we will get direct downloadable URLs right away which we can store --> Saves a LOT of time- and
         * http requests later on!
         */
        if (account != null) {
            plg.login(account, false);
        }
        String passCode = param.getDecrypterPassword();
        boolean trustPassword = passCode != null;
        /**
         * TODO: That is not enough -> We might have to re-use all cookies and/or maybe always store current/new session on account. </br>
         * It is only possible to use one "passwordCookie" at the same time!
         */
        final DownloadLink parent = param.getDownloadLink();
        String passwordCookie = parent != null ? param.getDownloadLink().getStringProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE) : null;
        if (passwordCookie != null) {
            setPasswordCookie(this.br, this.br.getHost(), passwordCookie);
        }
        String jstoken = null;
        /*
         * Fix surl value - website would usually do this via redirect. Just really strange that whenever that value starts with "1" they
         * just remove this in order to be able to use that id as a param for ajax requests.
         */
        /* 2023-06-26: Looks like this is not needed anymore. */
        boolean useStrangeSUrlWorkaround = false;
        if (surl.startsWith("1") && (parent == null || callingPlugin instanceof PluginForHost)) {
            useStrangeSUrlWorkaround = true;
            // surl = surl.substring(1, surl.length());
        }
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.setFollowRedirects(true);
        Browser surlbrowser = null;
        if (account != null) {
            jstoken = account.getStringProperty(TeraboxCom.PROPERTY_ACCOUNT_JS_TOKEN);
            if (jstoken == null) {
                logger.warning("Failed to find jstoken while account is given -> Download of crawled item(s) may fail!");
            }
            if (useStrangeSUrlWorkaround) {
                surlbrowser = br.cloneBrowser();
                surlbrowser.getPage(contenturl);
            }
        } else {
            synchronized (anonymousJstokenTimestamp) {
                if (anonymousJstoken.get() == null || Time.systemIndependentCurrentJVMTimeMillis() - anonymousJstokenTimestamp.get() < 5 * 60 * 1000 || useStrangeSUrlWorkaround) {
                    logger.info("Obtaining fresh anonymous jstoken");
                    final String newJstoken;
                    if (useStrangeSUrlWorkaround) {
                        surlbrowser = br.cloneBrowser();
                        surlbrowser.getPage(contenturl);
                        newJstoken = TeraboxCom.regexJsToken(surlbrowser);
                    } else {
                        newJstoken = TeraboxCom.getJsToken(br, this.getHost());
                    }
                    if (newJstoken != null) {
                        anonymousJstoken.set(newJstoken);
                        anonymousJstokenTimestamp.set(Time.systemIndependentCurrentJVMTimeMillis());
                        jstoken = newJstoken;
                    }
                } else {
                    jstoken = anonymousJstoken.get();
                }
            }
        }
        if (surlbrowser != null) {
            final String newSurlValue = UrlQuery.parse(surlbrowser.getURL()).get("surl");
            if (newSurlValue != null && !newSurlValue.equals(surl)) {
                logger.info("Found new surl value (mostly only number 1 at beginning is missing): Old" + surl + " | New: " + newSurlValue);
                surl = newSurlValue;
            }
        }
        int page = 1;
        final int maxItemsPerPage = 20;
        final UrlQuery queryFolder = new UrlQuery();
        queryFolder.add("app_id", getAppID());
        queryFolder.add("web", "1");
        queryFolder.add("channel", getChannel());
        queryFolder.add("clienttype", getClientType());
        /* 2023-06-21: jstoken is mandatory when account is given. */
        queryFolder.add("jsToken", jstoken != null ? jstoken : "");
        queryFolder.add("dp-logid", "");
        queryFolder.add("site_referer", "");
        queryFolder.add("scene", "purchased_list");
        queryFolder.add("by", "name");
        queryFolder.add("order", "time");
        queryFolder.add("desc", "1");
        queryFolder.add("shorturl", surl);
        if (!StringUtils.isEmpty(preGivenPath)) {
            queryFolder.add("dir", preGivenPath);
        } else {
            queryFolder.add("root", "1");
        }
        Map<String, Object> entries = null;
        if (targetFileID != null) {
            logger.info("Trying to find item with the following fs_id ONLY: " + targetFileID);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            logger.info("Crawling page: " + page);
            queryFolder.addAndReplace("page", Integer.toString(page));
            queryFolder.addAndReplace("num", Integer.toString(maxItemsPerPage));
            final String requesturl = "https://www." + this.getHost() + "/share/list?" + queryFolder.toString();
            br.getPage(requesturl);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
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
                        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                        final String captchaurl = (String) entries.get("vcode_img");
                        final String code = this.getCaptchaCode(captchaurl, param);
                        querypwPOST.appendEncoded("vcode", code);
                        querypwPOST.add("vcode_str", (String) entries.get("vcode_str"));
                    } else {
                        querypwPOST.add("vcode", "");
                        querypwPOST.add("vcode_str", "");
                    }
                    br.postPage("/share/verify?" + querypw.toString(), querypwPOST);
                    entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
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
                setPasswordCookie(br, br.getHost(), passwordCookie);
                /*
                 * Let's assume that dubox can ask again for password/captchas withing a long pagination -> Don't ask for password again as
                 * we know it!
                 */
                trustPassword = true;
                /* Repeat the first request -> We should be able to access the folder now. */
                br.getPage(requesturl);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            }
            errno = ((Number) entries.get("errno")).intValue();
            if (errno != 0 || !entries.containsKey("list")) {
                logger.info("Assume that this folder is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) entries.get("list");
            if (ressourcelist.isEmpty()) {
                logger.info("Stopping because: Current page doesn't contain any items");
                break;
            }
            for (final Map<String, Object> ressource : ressourcelist) {
                final String path = (String) ressource.get("path");
                /* 2021-04-14: 'category' is represented as a String. */
                final long category = JavaScriptEngineFactory.toLong(ressource.get("category"), -1);
                if (JavaScriptEngineFactory.toLong(ressource.get("isdir"), -1) == 1) {
                    /* Folder */
                    final String url = "https://www." + this.getHost() + "/web/share/link?surl=" + surl + "&path=" + Encoding.urlEncode(path);
                    final DownloadLink folder = this.createDownloadlink(url);
                    if (passCode != null) {
                        folder.setDownloadPassword(passCode);
                    }
                    /* Saving- and re-using this can save us some time later. */
                    folder.setProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookie);
                    folder.setProperty(TeraboxCom.PROPERTY_ACCOUNT_JS_TOKEN, jstoken);
                    distribute(folder);
                    ret.add(folder);
                } else {
                    /* File */
                    final String serverfilename = (String) ressource.get("server_filename");
                    // final long fsid = JavaScriptEngineFactory.toLong(entries.get("fs_id"), -1);
                    final String fsidStr = Long.toString(JavaScriptEngineFactory.toLong(ressource.get("fs_id"), -1));
                    final String realpath;
                    if (path.endsWith("/" + serverfilename)) {
                        realpath = path.replaceFirst("/" + Pattern.quote(serverfilename) + "$", "");
                    } else {
                        realpath = path;
                    }
                    final UrlQuery thisparams = new UrlQuery();
                    thisparams.add("surl", surl);
                    thisparams.appendEncoded("dir", realpath);// only the path!
                    thisparams.add("fsid", fsidStr);
                    thisparams.appendEncoded("fileName", serverfilename);
                    final String url = "https://www." + this.getHost() + "/sharing/link?" + thisparams.toString();
                    final String contentURL;
                    if (category == 1) {
                        thisparams.add("page", Integer.toString(page));
                        contentURL = "https://www." + this.getHost() + "/sharing/videoPlay?" + thisparams.toString();
                    } else {
                        /* No URL available that points directly to that file! */
                        contentURL = param.toString();
                    }
                    final DownloadLink dl = new DownloadLink(plg, "dubox", this.getHost(), url, true);
                    dl.setContentUrl(contentURL);
                    TeraboxCom.parseFileInformation(dl, ressource);
                    if (passCode != null) {
                        dl.setDownloadPassword(passCode);
                    }
                    /* Saving- and re-using this can save us some time later. */
                    dl.setProperty(TeraboxCom.PROPERTY_PASSWORD_COOKIE, passwordCookie);
                    dl.setProperty(TeraboxCom.PROPERTY_ACCOUNT_JS_TOKEN, jstoken);
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
                        ret.add(dl);
                    } else if (!StringUtils.equalsIgnoreCase(fsidStr, targetFileID)) {
                        /* we' re looking for a single item but this is not it. */
                        ret.add(dl);
                    } else {
                        /* We're looking for a single item and found it! */
                        ret.clear();
                        ret.add(dl);
                        logger.info("Stopping because: Found item matching target fileID: " + targetFileID);
                        return ret;
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
        return ret;
    }
}
