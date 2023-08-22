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
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.UpToBoxCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { UpToBoxCom.class })
public class UpToStreamCom extends PluginForDecrypt {
    public UpToStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        return UpToBoxCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(.*?hash=[a-f0-9]{16,}\\&folder=\\d+|(?:iframe/)?([a-z0-9]{12})(/([^/]+))?)");
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.getCryptedUrl();
        /* Load sister-host plugin */
        final ArrayList<String> dupecheck = new ArrayList<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final UpToBoxCom hosterPlugin = (UpToBoxCom) this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(hosterPlugin.getHost());
        if (parameter.contains("user_public")) {
            /* Using API: https://docs.uptobox.com/#retrieve-files-in-public-folder */
            final UrlQuery query = UrlQuery.parse(parameter);
            final String hash = query.get("hash");
            final String folderID = query.get("folder");
            if (hash == null || folderID == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            UpToBoxCom.prepBrowserStatic(br);
            int pageMax = 1;
            int pageCurrent = 0;
            int offset = 0;
            String currentFolderName = null;
            String filePath = this.getAdoptedCloudFolderStructure("");
            FilePackage fp = null;
            final int maxItemsPerPage = 100;
            final boolean foldersCanHaveSubfolders = false;
            paginationLoop: do {
                pageCurrent++;
                /* 2018-10-18: default = "limit=10" */
                br.getPage(hosterPlugin.getAPIBase(param.getCryptedUrl()) + "/user/public?folder=" + folderID + "&hash=" + hash + "&orderBy=file_name&dir=asc&limit=" + maxItemsPerPage + "&offset=" + offset);
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
                final String message = (String) entries.get("message");
                if (!StringUtils.equalsIgnoreCase(message, "Success")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                if (pageCurrent == 1) {
                    /* Init some variables when we're on first page. */
                    pageMax = ((Number) data.get("pageCount")).intValue();
                    currentFolderName = data.get("folderName").toString();
                    if (filePath.isEmpty()) {
                        filePath = currentFolderName;
                    } else {
                        filePath += "/" + currentFolderName;
                    }
                    fp = FilePackage.getInstance();
                    fp.setName(filePath);
                }
                /*
                 * 2020-04-16: Folders can only contain files. They can contain subfolders and files in the users' account but not in public
                 * URLs.
                 */
                final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) data.get("list");
                for (final Map<String, Object> file : ressourcelist) {
                    final String linkid = file.get("file_code").toString();
                    final String filename = file.get("file_name").toString();
                    final Number file_size = (Number) file.get("file_size_user");
                    /* Extra failsafe - avoid infinite loop! */
                    if (dupecheck.contains(linkid)) {
                        /* This should never happen. */
                        logger.info("Stopping because: Found dupe");
                        break paginationLoop;
                    }
                    final DownloadLink dl = this.createDownloadlink("https://" + br.getHost() + "/" + linkid);
                    dl.setName(filename);
                    dl.setDownloadSize(file_size.longValue());
                    dl.setAvailable(true);
                    if (foldersCanHaveSubfolders) {
                        dl.setRelativeDownloadFolderPath(filePath);
                    }
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                    dupecheck.add(linkid);
                    offset++;
                }
                logger.info("Crawled page " + pageCurrent + " / " + pageMax + " | Found items so far: " + ret.size());
                if (pageCurrent >= pageMax) {
                    logger.info("Stopping because: Reached last page");
                    break;
                } else if (ressourcelist.size() < maxItemsPerPage) {
                    /* Additional fail-safe */
                    logger.info("Stopping because: Current page contains less items than: " + maxItemsPerPage);
                    break;
                }
            } while (!this.isAbort());
            if (ret.isEmpty()) {
                throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, "EMPTY_FOLDER_" + filePath);
            }
        } else {
            final String host_uptobox = "uptobox.com";
            final String fuid = new Regex(param.toString(), this.getSupportedLinks()).getMatch(0);
            final UpToBoxComConfig cfg = PluginJsonConfig.get(UpToBoxComConfig.class);
            final DownloadLink videoStreamURL = this.createDownloadlink(parameter.replaceFirst("uptostream.com/", host_uptobox + "/"));
            videoStreamURL.setName(fuid + ".mp4");
            UpToBoxCom.prepBrowserStatic(br);
            final boolean grabSubtitle = cfg.isGrabSubtitle();
            /* 2020-04-13: API access (= premium account) required to get subtitles */
            if (grabSubtitle && account != null && account.getType() == AccountType.PREMIUM) {
                /* Extra handling to grab subtitles */
                logger.info("Trying to crawl subtitles");
                /* We need to linkcheck the main URL to get the filename */
                boolean isOnline = false;
                /*
                 * 2020-04-13: This will most likely be true as we only accept uptostream URLs in this plugin but we double check anyways.
                 */
                boolean isAvailableUnUptostream = false;
                try {
                    hosterPlugin.setBrowser(this.br);
                    hosterPlugin.requestFileInformation(videoStreamURL);
                    isOnline = videoStreamURL.isAvailabilityStatusChecked() && videoStreamURL.isAvailable();
                    isAvailableUnUptostream = videoStreamURL.getBooleanProperty(UpToBoxCom.PROPERTY_available_on_uptostream, false);
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.info("Availablecheck in crawler failed");
                }
                if (!isOnline || !isAvailableUnUptostream) {
                    logger.info("Not looking for subtitles as main URL is offline or content is not available on uptostream");
                } else {
                    logger.info("Main URL is online --> Looking for subtitles");
                    String fpName = videoStreamURL.getFinalFileName();
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
                        br.getPage(hosterPlugin.getAPIBase(param.getCryptedUrl()) + "/streaming?token=" + Encoding.urlEncode(token) + "&file_code=" + fuid);
                        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                        final List<Object> subtitles = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/subs");
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
                                ret.add(dlSubtitle);
                            }
                            /* FilePackage is only required if we have multiple objects */
                            final FilePackage fp = FilePackage.getInstance();
                            fp.setName(fpName);
                            fp.addLinks(ret);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.info("Subtitle handling failed");
                    }
                }
            }
            ret.add(videoStreamURL);
        }
        return ret;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @Override
    public boolean hasCaptcha(final CryptedLink link, final Account acc) {
        return false;
    }
}