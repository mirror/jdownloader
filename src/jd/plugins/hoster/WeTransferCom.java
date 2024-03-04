//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.WeTransferComFolder;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.controlling.FileStateManager;
import org.jdownloader.controlling.FileStateManager.FILESTATE;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "https?://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}" })
public class WeTransferCom extends PluginForHost {
    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://wetransfer.com/de-DE/explore/legal/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.addAllowedResponseCodes(new int[] { 410, 503 });
        br.setCookie("wetransfer.com", "wt_tandc", "20240117%3A1");
        br.setCookie("wetransfer.com", "wt_lang", "en");
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "okhttp/3.12.0");
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.setAllowedResponseCodes(new int[] { 401 });
        return br;
    }

    /* 2019-09-30: https://play.google.com/store/apps/details?id=com.wetransfer.app.live */
    public static final String   API_BASE_AUTH                   = "https://api.wetransfermobile.com/v1";
    public static final String   API_BASE_NORMAL                 = "https://api.wetransfermobile.com/v2";
    private static final Pattern TYPE_DOWNLOAD                   = Pattern.compile("https?://wetransferdecrypted/([a-f0-9]{46})/([a-f0-9]{4,12})/([a-f0-9]{46})");
    public static final String   PROPERTY_DIRECT_LINK            = "direct_link";
    public static final String   PROPERTY_DIRECT_LINK_EXPIRES_AT = "direct_link_expires_at";
    public static final String   PROPERTY_SINGLE_ZIP             = "single_zip";
    public static final String   PROPERTY_COLLECTION_ID          = "collection_id";
    public static final String   PROPERTY_COLLECTION_FILE_ID     = "collection_file_id";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        prepBRWebsite(br);
        final String directurl = link.getStringProperty(PROPERTY_DIRECT_LINK);
        final long directurlExpiresTimestamp = getStoredDirecturlValidityTimestamp(link);
        if (directurl != null && directurlExpiresTimestamp > System.currentTimeMillis()) {
            /* Trust direct-URL to still be usable so item is online. */
            return AvailableStatus.TRUE;
        }
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_DOWNLOAD);
        if (urlinfo.patternFind()) {
            final String folder_id = urlinfo.getMatch(0);
            final String security_hash = urlinfo.getMatch(1);
            final String file_id = urlinfo.getMatch(2);
            if (security_hash == null || folder_id == null || file_id == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "One or multiple plugin properties are missing");
            }
            final String refererurl = link.getReferrerUrl();
            if (refererurl == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Referer property is missing");
            }
            br.getPage(refererurl);
            final String[] recipient_id = refererurl.replaceFirst("https?://[^/]+/+", "").split("/");
            if (recipient_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String domain_user_id = br.getRegex("user\\s*:\\s*\\{\\s*\"key\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            final String csrfToken = br.getRegex("name\\s*=\\s*\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("security_hash", security_hash);
            if (this.isSingleZip(link)) {
                postdata.put("intent", "entire_transfer");
            } else {
                postdata.put("intent", "single_file");
                postdata.put("file_ids", Arrays.asList(new String[] { file_id }));
            }
            if (recipient_id.length == 4) {
                postdata.put("recipient_id", recipient_id[2]);
            }
            if (domain_user_id != null) {
                postdata.put("domain_user_id", domain_user_id);
            }
            final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + folder_id + "/download")));
            post.getHeaders().put("Accept", "application/json");
            post.getHeaders().put("Content-Type", "application/json");
            post.getHeaders().put("Origin", "https://" + br.getHost());
            post.getHeaders().put("X-Requested-With", " XMLHttpRequest");
            if (csrfToken != null) {
                post.getHeaders().put("X-CSRF-Token", csrfToken);
            }
            post.setPostDataString(JSonStorage.serializeToJson(postdata));
            br.getPage(post);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final String error = (String) entries.get("error");
            if (error != null) {
                if (error.equalsIgnoreCase("invalid_transfer")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, error);
                }
            }
            final String direct_link = (String) entries.get("direct_link");
            if (!StringUtils.isEmpty(direct_link)) {
                link.setProperty(PROPERTY_DIRECT_LINK, direct_link);
            }
        } else {
            final String collectionID = link.getStringProperty(PROPERTY_COLLECTION_ID);
            final String fileID = link.getStringProperty(PROPERTY_COLLECTION_FILE_ID);
            if (collectionID == null || fileID == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Required plugin property is missing");
            }
            final Browser brc = br.cloneBrowser();
            /* TODO: Maybe try to re-use cached token */
            final String token = WeTransferComFolder.getAPIToken(brc);
            brc.getHeaders().put("Authorization", "Bearer " + token);
            brc.postPageRaw(API_BASE_NORMAL + "/web/downloads/" + collectionID + "/public", "{\"file_ids\":[\"" + fileID + "\"]}");
            if (brc.getHttpConnection().getResponseCode() == 404) {
                /* E.g. {"success":false,"message":"Collection does not contain requested file(s)"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // brc.postPageRaw(API_BASE_NORMAL + "/mobile/downloads/" + collectionID + "/private", "{\"file_ids\":[\"" + fileID + "\"]}");
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String error = (String) entries.get("error");
            if (error != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
            final String direct_link = (String) entries.get("download_url");
            if (!StringUtils.isEmpty(direct_link)) {
                link.setProperty(PROPERTY_DIRECT_LINK, direct_link);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        String direct_link = link.getStringProperty(PROPERTY_DIRECT_LINK);
        final boolean stored_direct_link = direct_link != null;
        if (direct_link != null) {
            logger.info("Trying to re-use stored directurl: " + direct_link);
        } else {
            requestFileInformation(link);
            direct_link = link.getStringProperty(PROPERTY_DIRECT_LINK);
        }
        final boolean isSingleZip = this.isSingleZip(link);
        final boolean resume;
        final int maxChunks;
        if (isSingleZip) {
            resume = false;
            maxChunks = 1;
        } else {
            maxChunks = 1;
            resume = false;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, direct_link, resume, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (stored_direct_link) {
                final long directurlExpiresTimestamp = getStoredDirecturlValidityTimestamp(link);
                final long timeDirecturlStillValid = directurlExpiresTimestamp - System.currentTimeMillis();
                if (timeDirecturlStillValid > 5 * 60 * 1000) {
                    /* Try again later with the same URL. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stored directurl did not lead to downloadable file", e);
                } else {
                    link.removeProperty(PROPERTY_DIRECT_LINK);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
                }
            } else {
                throw e;
            }
        }
        dl.startDownload();
        /**
         * 2024-02-27: This website delivers single files as .zip files without .zip file-extension while all of them contain exactly one
         * file. </br> The special handling down below corrects this by extracting such files.
         */
        if (!isSingleZip && link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && link.getDownloadCurrent() > 0) {
            extract(link);
        }
    }

    private long getStoredDirecturlValidityTimestamp(final DownloadLink link) {
        return link.getLongProperty(PROPERTY_DIRECT_LINK_EXPIRES_AT, 0) * 1000;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        final List<File> ret = super.listProcessFiles(link);
        if (!this.isSingleZip(link)) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_DOWNLOAD);
            final String file_id = urlinfo.getMatch(2);
            if (file_id != null) {
                final File extractedFile = new File(new File(link.getFileOutput()).getParent(), file_id + ".extracted");
                ret.add(extractedFile);
            }
        }
        return ret;
    }

    @Override
    public Downloadable newDownloadable(DownloadLink downloadLink, Browser br) {
        return new DownloadLinkDownloadable(downloadLink) {
            @Override
            public void setFinalFileName(final String newfinalFileName) {
                // filename header contains full path including parent directories -> only take filename part without parent directories
                final String fileNameOnly = newfinalFileName == null ? null : newfinalFileName.replaceFirst("^(.+/)", "");
                super.setFinalFileName(fileNameOnly);
            }

            @Override
            public boolean isHashCheckEnabled() {
                return false;
            }
        };
    }

    private void extract(final DownloadLink link) throws Exception {
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_DOWNLOAD);
        final String file_id = urlinfo.getMatch(2);
        if (file_id == null) {
            return;
        } else if (isSingleZip(link)) {
            return;
        }
        final File srcDst = new File(link.getFileOutput());
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcDst);
        } catch (IOException e) {
            logger.log(e);
            return;
        }
        try {
            ZipEntry zipEntry = null;
            final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            boolean multipleEntriesFound = false;
            while (zipEntries.hasMoreElements()) {
                if (zipEntry != null) {
                    multipleEntriesFound = true;
                    break;
                } else {
                    zipEntry = zipEntries.nextElement();
                }
            }
            final String fileName = getFileNameFromDispositionHeader(dl.getConnection());
            if (multipleEntriesFound) {
                logger.info("Skip extract as we found multiple ZipEntries!");
                return;
            } else {
                if (zipEntry == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no entry in zip file!");
                } else {
                    ZipEntry checkZipEntry = zipFile.getEntry(fileName);
                    if (checkZipEntry != null) {
                        logger.info("Extract!Full matching ZipEntry found:" + checkZipEntry.getName());
                        zipEntry = checkZipEntry;
                    } else if ((checkZipEntry = zipFile.getEntry(fileName.replaceFirst("^(.+/)", ""))) != null) {
                        logger.info("Extract!Filename only matching ZipEntry found:" + checkZipEntry.getName());
                        zipEntry = checkZipEntry;
                    } else {
                        logger.info("Skip!No matching ZipEntry found:" + zipEntry.getName());
                        return;
                    }
                }
            }
            final ShutdownVetoListener vetoListener = new ShutdownVetoListener() {
                @Override
                public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
                    throw new ShutdownVetoException(getHost() + " extraction in progress:" + link.getName(), this);
                }

                @Override
                public void onShutdownVeto(ShutdownRequest request) {
                }

                @Override
                public void onShutdown(ShutdownRequest request) {
                }

                @Override
                public long getShutdownVetoPriority() {
                    return 0;
                }
            };
            File extractedFile = new File(srcDst.getParent(), file_id + ".extracted");
            if (!extractedFile.delete() && extractedFile.exists()) {
                throw new IOException("Could not delete:" + extractedFile);
            }
            FileStateManager.getInstance().requestFileState(extractedFile, FILESTATE.WRITE_EXCLUSIVE, this);
            ShutdownController.getInstance().addShutdownVetoListener(vetoListener);
            try {
                synchronized (getExtractionLock(link)) {
                    DigestInputStream dis = null;
                    final InputStream zis = zipFile.getInputStream(zipEntry);
                    try {
                        final FileOutputStream fos = new FileOutputStream(extractedFile);
                        try {
                            dis = new DigestInputStream(zis, MessageDigest.getInstance("SHA-256"));
                            final byte[] buffer = new byte[2048 * 1024];
                            while (true) {
                                final int read = dis.read(buffer);
                                if (read == -1) {
                                    break;
                                } else if (read > 0) {
                                    fos.write(buffer, 0, read);
                                }
                            }
                        } finally {
                            fos.close();
                        }
                    } finally {
                        zis.close();
                    }
                    zipFile.close();
                    zipFile = null;
                    if (!srcDst.delete() && srcDst.exists()) {
                        throw new IOException("Could not delete:" + srcDst);
                    } else {
                        if (extractedFile.renameTo(srcDst)) {
                            extractedFile = null;
                            link.setVerifiedFileSize(srcDst.length());
                            link.setHashInfo(HashInfo.parse(HexFormatter.byteArrayToHex(dis.getMessageDigest().digest()), true, true));
                        } else {
                            throw new IOException("Could not rename:" + extractedFile + " -> " + srcDst);
                        }
                    }
                }
            } finally {
                ShutdownController.getInstance().removeShutdownVetoListener(vetoListener);
                if (extractedFile != null) {
                    extractedFile.delete();
                }
                FileStateManager.getInstance().releaseFileState(extractedFile, this);
            }
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
    }

    private static Object GLOBAL_EXTRACTION_LOCK = new Object();

    private Object getExtractionLock(final DownloadLink link) {
        return GLOBAL_EXTRACTION_LOCK;
    }

    /**
     * Returns true if this file is a single .zip file containing all items of a wetransfer.com item. In most cases that .zip file will
     * contain subfolders and files or at least 2 files.
     */
    private boolean isSingleZip(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_SINGLE_ZIP, false);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return WetransferConfig.class;
    }

    public static interface WetransferConfig extends PluginConfigInterface {
        public static final TRANSLATION TRANSLATION  = new TRANSLATION();
        public static final CrawlMode   DEFAULT_MODE = CrawlMode.FILES_FOLDERS;

        public static class TRANSLATION {
            public String getCrawlMode2_label() {
                return "Crawl mode";
            }
        }

        public static enum CrawlMode implements LabelInterface {
            ZIP {
                @Override
                public String getLabel() {
                    return "Add .zip only";
                }
            },
            FILES_FOLDERS {
                @Override
                public String getLabel() {
                    return "Add loose files & folders";
                }
            },
            ALL {
                @Override
                public String getLabel() {
                    return "Add loose files & folders AND .zip with all items";
                }
            },
            DEFAULT {
                @Override
                public String getLabel() {
                    return "Default: " + DEFAULT_MODE.getLabel();
                }
            };
        }

        @AboutConfig
        @DefaultEnumValue("DEFAULT")
        @Order(10)
        @DescriptionForConfigEntry("Single .zip download is recommended. Loose file download may lave you with corrupted files or wrongly named .zip files.")
        @DefaultOnNull
        CrawlMode getCrawlMode2();

        void setCrawlMode2(final CrawlMode mode);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_DIRECT_LINK);
    }
}