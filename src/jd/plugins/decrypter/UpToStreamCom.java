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
import java.util.LinkedHashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.UpToBoxComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.UpToBoxCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uptostream.com", "uptobox.com" }, urls = { "https?://(?:www\\.)?uptostream\\.com/(?:iframe/)?([a-z0-9]{12})(/([^/]+))?", "https?://(?:www\\.)?uptobox\\.com/(\\?op=user_public\\&|user_public\\?)hash=[a-f0-9]{16}\\&folder=\\d+" })
public class UpToStreamCom extends antiDDoSForDecrypt {
    public UpToStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN = "uptostream.com";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter;
        /* Load sister-host plugin */
        final ArrayList<String> dupecheck = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.toString().contains("user_public")) {
            /* Using API: https://docs.uptobox.com/#retrieve-files-in-public-folder */
            parameter = param.toString();
            final UrlQuery query = new UrlQuery().parse(parameter);
            final String hash = query.get("hash");
            final String folderID = query.get("folder");
            if (hash == null || folderID == null) {
                /* This should never happen */
                return null;
            }
            UpToBoxCom.prepBrowserStatic(br);
            int pageMax = 1;
            int pageCurrent = 0;
            int offset = 0;
            do {
                pageCurrent++;
                logger.info("Crawling page " + pageCurrent + " of " + pageMax);
                /* 2018-10-18: default = "limit=10" */
                getPage(UpToBoxCom.API_BASE + "/user/public?folder=" + folderID + "&hash=" + hash + "&orderBy=file_name&dir=asc&limit=100&offset=" + offset);
                final String errormessage = PluginJSonUtils.getJson(br, "message");
                if (!StringUtils.isEmpty(errormessage) && !StringUtils.equalsIgnoreCase(errormessage, "Success")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    break;
                }
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("data");
                if (pageCurrent == 1) {
                    /* Set maxPage on first request */
                    pageMax = (int) JavaScriptEngineFactory.toLong(entries.get("pageCount"), 0);
                }
                /*
                 * 2020-04-16: Folders can only contain files. They can contain subfolders and files in the users' account but not in public
                 * URLs.
                 */
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("list");
                for (final Object fileO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) fileO;
                    final String linkid = (String) entries.get("file_code");
                    final String filename = (String) entries.get("file_name");
                    if (StringUtils.isEmpty(linkid) || StringUtils.isEmpty(filename)) {
                        continue;
                    }
                    /* Extra failsafe - avoid infinite loop! */
                    if (dupecheck.contains(linkid)) {
                        logger.info("Found dupe");
                        break;
                    }
                    final DownloadLink dl = this.createDownloadlink("https://" + br.getHost() + "/" + linkid);
                    dl.setName(filename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    dupecheck.add(linkid);
                    offset++;
                }
            } while (!this.isAbort() && pageCurrent < pageMax);
        } else {
            parameter = param.toString();
            final String host_uptobox = "uptobox.com";
            final String fuid = new Regex(param.toString(), this.getSupportedLinks()).getMatch(0);
            final UpToBoxComConfig cfg = PluginJsonConfig.get(UpToBoxComConfig.class);
            final DownloadLink uptoboxURL = this.createDownloadlink(parameter.replace("uptostream.com", host_uptobox));
            uptoboxURL.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            UpToBoxCom.prepBrowserStatic(br);
            final PluginForHost plg = JDUtilities.getPluginForHost(host_uptobox);
            final Account account = AccountController.getInstance().getValidAccount(plg.getHost());
            final boolean grabSubtitle = cfg.isGrabSubtitle();
            /* 2020-04-13: API access (= premium account) required to get subtitles */
            if (grabSubtitle && account != null && account.getType() == AccountType.PREMIUM) {
                logger.info("Trying to crawl subtitles");
                /* We need to linkcheck the main URL to get the filename */
                boolean isOnline = false;
                /*
                 * 2020-04-13: This will most likely be true as we only accept uptostream URLs in this plugin but we double check anyways.
                 */
                boolean isAvailableUnUptostream = false;
                try {
                    plg.setBrowser(this.br);
                    ((jd.plugins.hoster.UpToBoxCom) plg).requestFileInformation(uptoboxURL);
                    isOnline = uptoboxURL.isAvailabilityStatusChecked() && uptoboxURL.isAvailable();
                    isAvailableUnUptostream = uptoboxURL.getBooleanProperty(UpToBoxCom.PROPERTY_available_on_uptostream, false);
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.info("Availablecheck in crawler failed");
                }
                if (!isOnline || !isAvailableUnUptostream) {
                    logger.info("Not looking for subtitles as main URL is offline or content is not available on uptostream");
                } else {
                    logger.info("Main URL is online --> Looking for subtitles");
                    String fpName = uptoboxURL.getFinalFileName();
                    if (fpName == null) {
                        /* Fallback - should not be required */
                        fpName = fuid;
                    }
                    if (fpName.contains(".")) {
                        /* Remove extension */
                        fpName = fpName.substring(0, fpName.lastIndexOf("."));
                    }
                    try {
                        final String token = account.getPass();
                        this.getPage(UpToBoxCom.API_BASE + "/streaming?token=" + Encoding.urlEncode(token) + "&file_code=" + fuid);
                        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                        final ArrayList<Object> subtitles = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/subs");
                        if (subtitles == null || subtitles.size() == 0) {
                            logger.info("Failed to find any subtitles");
                        } else {
                            logger.info(String.format("Found %d subtitles", subtitles.size()));
                            for (final Object subtitleO : subtitles) {
                                entries = (Map<String, Object>) subtitleO;
                                final String url = (String) entries.get("src");
                                final String language = (String) entries.get("srcLang");
                                if (StringUtils.isEmpty(url) || StringUtils.isEmpty(language)) {
                                    /* Skip invalid items */
                                    continue;
                                }
                                final DownloadLink dlSubtitle = this.createDownloadlink("directhttp://" + url);
                                dlSubtitle.setAvailable(true);
                                String extension = Plugin.getFileNameExtensionFromURL(url);
                                if (extension == null) {
                                    /* Fallback */
                                    extension = ".vtt";
                                }
                                String filename = fpName;
                                if (subtitles.size() > 1) {
                                    /* Only add language info to filename if we got more than 1 subtitle available. */
                                    filename += "_" + language;
                                }
                                filename += extension;
                                dlSubtitle.setFinalFileName(filename);
                                decryptedLinks.add(dlSubtitle);
                            }
                            /* FilePackage is only required if we have multiple objects */
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(fpName);
                            fp.addLinks(decryptedLinks);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.info("Subtitle handling failed");
                    }
                }
            }
            decryptedLinks.add(uptoboxURL);
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(final CryptedLink link, final Account acc) {
        return false;
    }
}