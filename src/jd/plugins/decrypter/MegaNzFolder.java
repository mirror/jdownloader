package jd.plugins.decrypter;

import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.MaxTimeSoftReference;
import org.appwork.storage.config.MaxTimeSoftReferenceCleanupCallback;
import org.appwork.storage.simplejson.JSonParser;
import org.appwork.storage.simplejson.JSonValue;
import org.appwork.storage.simplejson.MinimalMemoryMap;
import org.appwork.utils.ByteArrayWrapper;
import org.appwork.utils.IO;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MegaNzConfig.InvalidOrMissingDecryptionKeyAction;
import org.jdownloader.plugins.components.config.MegaNzFolderConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.MegaNz;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { MegaNz.class })
public class MegaNzFolder extends PluginForDecrypt {
    private static AtomicLong CS = new AtomicLong(System.currentTimeMillis());

    public MegaNzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        return MegaNz.getPluginDomains();
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

    public static final Pattern PATTERN_FILE_FOLDER_ID        = Pattern.compile("[a-zA-Z0-9]{8}");
    public static final Pattern PATTERN_FOLDER_ENCRYPTION_KEY = Pattern.compile("[a-zA-Z0-9_\\-\\+]{22}={0,2}");
    public static final Pattern PATTERN_FOLDER_OLD            = Pattern.compile("#F!([a-zA-Z0-9]+)(!([a-zA-Z0-9_-]+))?(!([a-zA-Z0-9]+))?");
    public static final Pattern PATTERN_FOLDER_NEW            = Pattern.compile("folder/([a-zA-Z0-9]+)(#([a-zA-Z0-9_-]+))?(/(file|folder)/([a-zA-Z0-9]+))?", Pattern.CASE_INSENSITIVE);

    /**
     * Returns ID of preferred subfolder or file. </br>
     * Returns non-validated result!
     */
    private static String getPreferredNodeID(final String url) {
        String id = new Regex(url, PATTERN_FOLDER_NEW).getMatch(5);
        if (id == null) {
            id = new Regex(url, PATTERN_FOLDER_OLD).getMatch(4);
        }
        return id;
    }

    /** Returns non-validated result! */
    private static String getFolderID(final String url) {
        String folderID = new Regex(url, PATTERN_FOLDER_NEW).getMatch(0);
        if (folderID == null) {
            /* Older URLs */
            folderID = new Regex(url, PATTERN_FOLDER_OLD).getMatch(0);
        }
        return folderID;
    }

    /** Returns non-validated result! */
    public static String getFolderMasterKey(final String url) {
        String key = new Regex(url, PATTERN_FOLDER_NEW).getMatch(2);
        if (key == null) {
            key = new Regex(url, PATTERN_FOLDER_OLD).getMatch(2);
        }
        return key;
    }

    public static boolean isValidFileFolderNodeID(final String str) {
        return new Regex(str, PATTERN_FILE_FOLDER_ID).patternFind();
    }

    public static boolean isValidFolderDecryptionKey(final String str) {
        return new Regex(str, PATTERN_FOLDER_ENCRYPTION_KEY).patternFind();
    }

    public static String getUrlPatternBase(final String[] domains) {
        return "(https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/|chrome://mega/content/secure\\.html|mega:/*)";
    }

    private Cipher cipher = null;

    @Override
    public void clean() {
        try {
            cipher = null;
        } finally {
            super.clean();
        }
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String regex = getUrlPatternBase(domains);
            /* Add domain-unspecific patterns */
            regex += "(" + PATTERN_FOLDER_OLD.pattern() + "|" + PATTERN_FOLDER_NEW.pattern() + ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        // br.setCurrentURL("https://mega.nz");
        br.setLoadLimit(256 * 1024 * 1024);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        // br.getHeaders().put("Origin", "https://mega.nz");
        br.getHeaders().put("APPID", "JDownloader");
        br.addAllowedResponseCodes(500);
        return br;
    }

    private class MegaFolder {
        private String              parent;
        private String              name;
        private final String        id;
        private Map<String, Object> content = null;

        private void put(String key, Object value) {
            if (content == null) {
                content = new MinimalMemoryMap<String, Object>();
            }
            content.put(key, value);
        }

        private MegaFolder(String nodeID) {
            id = nodeID;
        }
    }

    private static HashMap<String, Reference<List<Map<String, Object>>>> GLOBAL_CACHE = new HashMap<String, Reference<List<Map<String, Object>>>>();
    private final Charset                                                UTF8         = Charset.forName("UTF-8");

    protected Object toObject(final Object object) {
        if (object == null) {
            return object;
        } else if (object instanceof JSonValue) {
            return ((JSonValue) object).getValue();
        } else {
            return object;
        }
    }

    protected String toString(final Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof JSonValue) {
            return toString(((JSonValue) object).getValue());
        } else if (object instanceof String) {
            return (String) object;
        } else if (object instanceof ByteArrayWrapper) {
            return ((ByteArrayWrapper) object).toString(UTF8);
        } else {
            throw new WTFException("unsupported type:" + object.getClass());
        }
    }

    @Override
    public Class<? extends MegaNzFolderConfig> getConfigInterface() {
        return MegaNzFolderConfig.class;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        cipher = Cipher.getInstance("AES/CBC/nopadding");
        final String contenturl = Encoding.htmlDecode(param.getCryptedUrl());
        final String folderID = getFolderID(contenturl);
        String folderMasterKey = getFolderMasterKey(contenturl);
        final String preferredNodeIDFromURL = getPreferredNodeID(contenturl);
        final MegaNzFolderConfig config = PluginJsonConfig.get(getConfigInterface());
        String preferredNodeType = null;
        final String preferredNodeID;
        if (isValidFileFolderNodeID(preferredNodeIDFromURL)) {
            preferredNodeID = preferredNodeIDFromURL;
            preferredNodeType = new Regex(contenturl, PATTERN_FOLDER_NEW).getMatch(4);
        } else {
            /* It doesn't make sense to look for a nodeID if we know that it is invalid. */
            preferredNodeID = null;
        }
        if (folderID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!isValidFileFolderNodeID(folderID)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid folderID");
        } else if (!isValidFolderDecryptionKey(folderMasterKey) && config.getInvalidOrMissingDecryptionKeyAction().getAction() != InvalidOrMissingDecryptionKeyAction.ASK) {
            /* No valid password given and user does not want to be asked. */
            throw new DecrypterRetryException(RetryReason.PASSWORD);
        }
        int retryCounter = 0;
        final Map<String, FilePackage> fpMap = new HashMap<String, FilePackage>();
        List<Map<String, Object>> folderNodes = null;
        synchronized (GLOBAL_CACHE) {
            final String key = getHost() + "://FolderNodes/" + folderID;
            LinkCrawler crawler = getCrawler();
            if (crawler != null) {
                crawler = crawler.getRoot();
            }
            if (crawler != null) {
                folderNodes = (List<Map<String, Object>>) crawler.getCrawlerCache(key);
            }
            if (folderNodes == null) {
                final Reference<List<Map<String, Object>>> cached = GLOBAL_CACHE.get(folderID);
                if (cached != null) {
                    folderNodes = cached.get();
                }
                if (folderNodes == null) {
                    folderNodes = new ArrayList<Map<String, Object>>();
                    final long minTime = config.getMaxCacheFolderDetails();
                    if (minTime > 0) {
                        GLOBAL_CACHE.put(folderID, new MaxTimeSoftReference<List<Map<String, Object>>>(folderNodes, minTime * 60 * 1000, folderID, new MaxTimeSoftReferenceCleanupCallback() {
                            @Override
                            public void onMaxTimeSoftReferenceCleanup(MaxTimeSoftReference<?> minTimeWeakReference) {
                                minTimeWeakReference.clear();
                                synchronized (GLOBAL_CACHE) {
                                    if (JVMVersion.isMinimum(JVMVersion.JAVA_1_8)) {
                                        GLOBAL_CACHE.remove(minTimeWeakReference.getID(), minTimeWeakReference);
                                    } else {
                                        final Reference<List<Map<String, Object>>> cache = GLOBAL_CACHE.get(minTimeWeakReference.getID());
                                        if (cache == minTimeWeakReference) {
                                            GLOBAL_CACHE.remove(minTimeWeakReference.getID());
                                        }
                                    }
                                }
                            }
                        }));
                    }
                }
                if (crawler != null) {
                    crawler.putCrawlerCache(key, folderNodes);
                }
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String sn = null;
        final boolean wscSupport = false;
        String w = null;// websocket
        synchronized (folderNodes) {
            if (folderNodes.size() > 0) {
                logger.info("Found Cache:Nodes=" + folderNodes.size() + "|FolderID=" + folderID);
            } else {
                final List<Map<String, Object>> parsedNodes = new ArrayList<Map<String, Object>>();
                pagination: while (!isAbort()) {
                    final URLConnectionAdapter con;
                    if (sn != null) {
                        if (w != null) {
                            // blocks for longer time, waits for events
                            con = br.openRequestConnection(br.createJSonPostRequest(w + "?id=" + CS.incrementAndGet() + "&n=" + folderID + "&sn=" + sn, ""));
                        } else {
                            if (wscSupport) {
                                con = br.openRequestConnection(br.createJSonPostRequest("https://g.api.mega.co.nz/wsc?id=" + CS.incrementAndGet() + "&n=" + folderID + "&sn=" + sn, ""));
                            } else {
                                con = br.openRequestConnection(br.createJSonPostRequest("https://g.api.mega.co.nz/sc?id=" + CS.incrementAndGet() + "&n=" + folderID + "&sn=" + sn, ""));
                            }
                        }
                    } else {
                        con = br.openRequestConnection(br.createJSonPostRequest("https://g.api.mega.co.nz/cs?id=" + CS.incrementAndGet() + "&n=" + folderID
                        /*
                         * + "&domain=meganz
                         */, "[{\"a\":\"f\",\"c\":\"1\",\"r\":\"1\",\"ca\":1}]"));// ca=1
                        // ->
                        // !nocache,
                        // commands.cpp
                    }
                    if (con.getResponseCode() == 500) {
                        br.followConnection(true);
                        if (retryCounter < 10) {
                            sleep(5000, param);
                            retryCounter++;
                            continue;
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    final Object response;
                    try {
                        JSonParser factory = new JSonParser(IO.BOM.read(IO.readStream(-1, con.getInputStream()), IO.BOM.UTF8.getCharSet())) {
                            @Override
                            protected Map<String, ? extends Object> createJSonObject() {
                                return new MinimalMemoryMap<String, Object>();
                            }

                            @Override
                            protected void putKeyValuePair(Object newPath, Map<String, Object> map, String key, Object value) {
                                if (("p".equals(key) || "u".equals(key)) && value instanceof String) {
                                    // dedupe parent node (ID)
                                    value = dedupeString((String) value);
                                } else if ("ts".equals(key)) {// remove unused timestamp
                                    return;
                                }
                                super.putKeyValuePair(newPath, map, key, value);
                            }
                        };
                        response = factory.parse();
                        // allow GC of JSonFactory
                        factory = null;
                    } finally {
                        con.disconnect();
                    }
                    if (response instanceof List && StringUtils.isEmpty(sn)) {
                        List<Map<String, Object>> pageNodes = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "{0}/f");
                        if (pageNodes == null) {
                            pageNodes = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "{0}/a/{0}/t/f");
                        }
                        if (pageNodes != null) {
                            final int nodesBefore = parsedNodes.size();
                            parsedNodes.addAll(pageNodes);
                            if (parsedNodes.size() == nodesBefore) {
                                logger.info("no more nodes found->stop pagination");
                                break pagination;
                            }
                        } else {
                            logger.info("no more nodes found->abort pagination");
                            break pagination;
                        }
                        final String nextSN = toString(JavaScriptEngineFactory.walkJson(response, "{0}/sn"));
                        if (StringUtils.isEmpty(nextSN)) {
                            logger.info("no next page");
                            break pagination;
                        } else if (!StringUtils.equals(sn, nextSN)) {
                            logger.info("next page:" + sn);
                            sn = nextSN;
                        } else {
                            logger.info("same next page?:" + sn);
                            break pagination;
                        }
                    } else if (response instanceof Map && StringUtils.isNotEmpty(sn)) {
                        final List<Map<String, Object>> pageNodes = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "a");
                        if (pageNodes != null) {
                            final int nodesBefore = parsedNodes.size();
                            for (Map<String, Object> pageNode : pageNodes) {
                                final List<Map<String, Object>> additionalNodes = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(pageNode, "t/f");
                                if (additionalNodes != null) {
                                    parsedNodes.addAll(additionalNodes);
                                }
                            }
                            if (parsedNodes.size() == nodesBefore) {
                                logger.info("no more nodes found->stop pagination");
                                break pagination;
                            }
                        } else {
                            logger.info("no more nodes found->abort pagination");
                            break pagination;
                        }
                        final String nextSN = toString(JavaScriptEngineFactory.walkJson(response, "sn"));
                        if (StringUtils.isEmpty(nextSN)) {
                            logger.info("no next page");
                            break pagination;
                        } else if (StringUtils.equals(sn, nextSN)) {
                            logger.info("same next page?:" + sn);
                            break pagination;
                        } else {
                            logger.info("next page:" + sn);
                            sn = nextSN;
                        }
                        if (wscSupport && w == null) {
                            w = toString(JavaScriptEngineFactory.walkJson(response, "w"));
                            if (StringUtils.isEmpty(w)) {
                                logger.info("wsc missing");
                                break;
                            } else {
                                logger.info("next page:" + w + "|" + sn);
                            }
                        }
                    } else if (response instanceof Number || response instanceof JSonValue) {
                        logger.info("Response:" + JSonStorage.toString(response));
                        final Number num = ((Number) toObject(response));
                        if (num.intValue() == 0 && w != null) {
                            // WebSocket(wsc) = empty
                            break;
                        } else if (num.intValue() == -3) {
                            /**
                             * Error: RequestFailedRetry API_EAGAIN (-3) (always at the request level): A temporary congestion or server
                             * malfunction is preventing your request from processing. Could not alter any data. Retry. Retries must be
                             * spaced with exponential backoff.
                             */
                            if (retryCounter < 10) {
                                sleep(5000, param);
                                retryCounter++;
                                continue;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else if (num.intValue() == -9) {
                            /**
                             * Error: ResourceDoesNotExists API_EOENT (-9): Object (typically, node or user) not found
                             */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        } else {
                            /* Unknown error code, typically -2 or -9 */
                            // https://help.servmask.com/knowledgebase/mega-error-codes/
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    } else {
                        logger.info("Response:" + JSonStorage.toString(response));
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (this.isAbort()) {
                    throw new InterruptedException();
                }
                final Iterator<Map<String, Object>> it = parsedNodes.iterator();
                while (it.hasNext()) {
                    final Map<String, Object> parsedNode = it.next();
                    if (isAbort()) {
                        throw new InterruptedException();
                    }
                    final String encryptedNodeKey = new Regex(toString(parsedNode.remove("k")), ":([^:]*?)$").getMatch(0);
                    if (encryptedNodeKey == null) {
                        it.remove();
                        logger.info("NodeKey missing:" + JSonStorage.toString(parsedNode));
                        continue;
                    }
                    boolean success = false;
                    int password_tries = 0;
                    String nodeKey = null;
                    password: while (!this.isAbort() && password_tries <= 3) {
                        if (password_tries > 0 || !isValidFolderDecryptionKey(folderMasterKey)) {
                            folderMasterKey = getUserInput("Decryption key", param);
                        }
                        try {
                            nodeKey = decryptNodeKey(encryptedNodeKey, folderMasterKey);
                            success = true;
                            break password;
                        } catch (final InvalidKeyException e) {
                            password_tries++;
                            if (!InvalidOrMissingDecryptionKeyAction.ASK.equals(config.getInvalidOrMissingDecryptionKeyAction().getAction())) {
                                break password;
                            }
                        }
                    }
                    if (!success) {
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                    final String nodeAttr = decrypt(toString(parsedNode.remove("a")), nodeKey);
                    if (nodeAttr == null) {
                        it.remove();
                        logger.info("NodeAttr missing:" + JSonStorage.toString(parsedNode));
                        continue;
                    }
                    final String nodeName = removeEscape(new Regex(nodeAttr, "\"n\"\\s*?:\\s*?\"(.*?)(?<!\\\\)\"").getMatch(0));
                    final String nodeType = String.valueOf(toObject(parsedNode.remove("t")));
                    if ("1".equals(nodeType)) {
                        parsedNode.put("nodeDirectory", Boolean.TRUE);
                    } else if ("0".equals(nodeType)) {
                        // parsedNode.put("nodeDirectory", Boolean.FALSE);
                        final Long nodeSize = JavaScriptEngineFactory.toLong(toObject(parsedNode.remove("s")), -1);
                        if (nodeSize == -1) {
                            it.remove();
                            logger.info("NodeSize missing:" + JSonStorage.toString(parsedNode));
                            continue;
                        } else {
                            parsedNode.put("nodeSize", nodeSize);
                        }
                    } else {
                        it.remove();
                        logger.info("Unknown type(" + nodeType + "):" + JSonStorage.toString(parsedNode));
                        continue;
                    }
                    parsedNode.put("nodeKey", nodeKey);
                    parsedNode.put("nodeName", nodeName);
                }
                logger.info("Fill Cache:Nodes=" + parsedNodes.size() + "|FolderID=" + folderID);
                folderNodes.addAll(parsedNodes);
            }
        }
        if (StringUtils.isEmpty(folderMasterKey)) {
            /* Something must have gone seriously wrong */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * p = parent node (ID)
         *
         * s = size
         *
         * t = type (0=file, 1=folder, 2=root, 3=inbox, 4=trash
         *
         * ts = timestamp
         *
         * h = node (ID)
         *
         * u = owner
         *
         * a = attribute (contains name)
         *
         * k = node key
         */
        final PluginForHost megaplugin = this.getNewPluginForHostInstance(getHost());
        final LinkedHashMap<String, MegaFolder> folders = new LinkedHashMap<String, MegaFolder>();
        final ArrayList<DownloadLink> desiredItems = new ArrayList<DownloadLink>();
        for (final Map<String, Object> folderNode : folderNodes) {
            if (isAbort()) {
                break;
            }
            final String nodeID = toString(folderNode.get("h"));
            final String nodeParentID = toString(folderNode.get("p"));
            if (folderNode.containsKey("nodeDirectory")) {
                // directory
                final String nodeName = toString(folderNode.get("nodeName"));
                final MegaFolder fo = new MegaFolder(nodeID);
                fo.parent = nodeParentID;
                fo.name = nodeName;
                folders.put(nodeID, fo);
                final MegaFolder parentFolder = folders.get(nodeParentID);
                if (parentFolder != null) {
                    parentFolder.put(nodeID, fo);
                }
            } else {
                // file
                final Long nodeSize = (Long) toObject(folderNode.get("nodeSize"));
                final MegaFolder folder = folders.get(nodeParentID);
                boolean partOfPreferredNodeID = false;
                if (StringUtils.isNotEmpty(preferredNodeID)) {
                    /* Look for preferred item such as a specific subfolder inside a folder or specific file inside a folder. */
                    if (StringUtils.equals(preferredNodeID, nodeID)) {
                        // preferred nodeID
                        partOfPreferredNodeID = true;
                        preferredNodeType = "file";
                    } else if (folder != null) {
                        // check parentNodeID recursive of file
                        MegaFolder checkParent = folder;
                        while (checkParent != null) {
                            if (preferredNodeID.equals(checkParent.id)) {
                                partOfPreferredNodeID = true;
                                preferredNodeType = "folder";
                                break;
                            } else {
                                checkParent = folders.get(checkParent.parent);
                            }
                        }
                    }
                }
                FilePackage fp;
                final String path;
                if (folder != null) {
                    path = getRelPath(folder, folders);
                    fp = fpMap.get(path);
                    if (fp == null) {
                        fp = FilePackage.getInstance();
                        if (config.isCrawlerSetFullPathAsPackagename()) {
                            fp.setName(path);
                        } else {
                            fp.setName(path.substring(path.lastIndexOf("/") + 1));
                        }
                        fpMap.put(path, fp);
                    }
                } else {
                    fp = null;
                    path = null;
                }
                final String nodeName = toString(folderNode.get("nodeName"));
                final String nodeKey = toString(folderNode.get("nodeKey"));
                final String folderFileUrl = jd.plugins.hoster.MegaNz.buildFileFolderLink(folderID, folderMasterKey, "file", nodeID);
                final DownloadLink link = createDownloadlink(folderFileUrl);
                link.setDefaultPlugin(megaplugin);
                final MegaFolder parentFolder = folders.get(nodeParentID);
                if (parentFolder != null) {
                    parentFolder.put(nodeID, link);
                }
                /* folder nodes (files inside folders) can only be downloaded with knowledge of the folderNodeID */
                link.setProperty("public", false);
                link.setProperty("pn", folderID);
                link.setProperty("mk", folderMasterKey);
                link.setProperty("fk", nodeKey);
                // alternative: https://mega.nz/folder/folderID#masterKey/file/nodeID
                link.setProperty("fa", toObject(folderNode.get("fa")));// file attributes
                link.setFinalFileName(nodeName);
                if (path != null) {
                    link.setRelativeDownloadFolderPath(path);
                    /*
                     * Packagizer property so user can e.g. merge all files of a folder and subfolders in a package named after the name of
                     * the root dir.
                     */
                    final String root_dir_name = new Regex(path, "^/?([^/]+)").getMatch(0);
                    if (root_dir_name != null) {
                        link.setProperty("root_dir", root_dir_name);
                    }
                }
                link.setAvailable(true);
                link.setVerifiedFileSize(nodeSize);
                if (fp != null) {
                    fp.add(link);
                }
                if (partOfPreferredNodeID) {
                    desiredItems.add(link);
                    if ("file".equals(preferredNodeType)) {
                        /* We are looking for exactly one file and we found it -> End loop */
                        break;
                    }
                } else {
                    ret.add(link);
                }
            }
        }
        if (desiredItems.size() > 0) {
            ret.clear();
            ret.addAll(desiredItems);
        }
        String containerURL = "https://" + jd.plugins.hoster.MegaNz.MAIN_DOMAIN + "/folder/" + folderID + "#" + folderMasterKey;
        if (preferredNodeID != null && preferredNodeType != null) {
            containerURL += "/" + preferredNodeType + "/" + preferredNodeID;
        }
        for (DownloadLink link : ret) {
            link.setContainerUrl(containerURL);
        }
        if (!isAbort()) {
            if (preferredNodeID != null) {
                /* Preferred file/subfolder was not found --> Reurn all items instead */
                logger.info("Preferred NodeID NOT found:" + preferredNodeID);
            }
            if (ret.size() == 0) {
                boolean hasFiles = false;
                for (final MegaFolder folder : folders.values()) {
                    if (folder.content != null) {
                        for (final Object item : folder.content.values()) {
                            if (item instanceof DownloadLink) {
                                hasFiles = true;
                                break;
                            }
                        }
                    }
                }
                if (!hasFiles) {
                    MegaFolder emptyFolder = null;
                    if (folders.size() > 0) {
                        if (preferredNodeID != null) {
                            emptyFolder = folders.get(preferredNodeID);
                        }
                        if (emptyFolder == null) {
                            emptyFolder = folders.entrySet().iterator().next().getValue();
                        }
                    }
                    if (emptyFolder != null) {
                        /* Show name/path of empty folder to user */
                        final String path = getRelPath(emptyFolder, folders);
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, emptyFolder.id + "_" + path);
                    } else {
                        throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                    }
                }
            }
        }
        return ret;
    }

    private String removeEscape(String match) {
        if (match != null) {
            return match.replaceAll("\\\\", "");
        } else {
            return null;
        }
    }

    private String getRelPath(MegaFolder folder, Map<String, MegaFolder> folders) {
        if (folder == null) {
            return "/";
        }
        final StringBuilder ret = new StringBuilder();
        while (true) {
            ret.insert(0, folder.name);
            ret.insert(0, "/");
            final MegaFolder parent = folders.get(folder.parent);
            if (parent == null || parent == folder) {
                final String path = ret.toString();
                if (path.startsWith("/") && path.length() > 1) {
                    /* Remove flash from beginning */
                    return path.substring(1, path.length());
                } else {
                    return path;
                }
            }
            folder = parent;
        }
    }

    private String decryptNodeKey(String encryptedNodeKey, String masterKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        final byte[] masterKeyBytes = jd.plugins.hoster.MegaNz.b64decode(masterKey);
        final byte[] encryptedNodeKeyBytes = jd.plugins.hoster.MegaNz.b64decode(encryptedNodeKey);
        final byte[] ret = new byte[encryptedNodeKeyBytes.length];
        final byte[] iv = jd.plugins.hoster.MegaNz.aInt_to_aByte(0, 0, 0, 0);
        for (int index = 0; index < ret.length; index = index + 16) {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(masterKeyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            System.arraycopy(cipher.doFinal(Arrays.copyOfRange(encryptedNodeKeyBytes, index, index + 16)), 0, ret, index, 16);
        }
        return Base64.encodeToString(ret, false);
    }

    private String decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = jd.plugins.hoster.MegaNz.b64decode(keyString);
        int[] intKey = jd.plugins.hoster.MegaNz.aByte_to_aInt(b64Dec);
        byte[] key = null;
        if (intKey.length == 4) {
            /* folder key */
            key = b64Dec;
        } else {
            /* file key */
            key = jd.plugins.hoster.MegaNz.aInt_to_aByte(intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7]);
        }
        byte[] iv = jd.plugins.hoster.MegaNz.aInt_to_aByte(0, 0, 0, 0);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        byte[] unPadded = jd.plugins.hoster.MegaNz.b64decode(input);
        int len = 16 - ((unPadded.length - 1) & 15) - 1;
        byte[] payLoadBytes = new byte[unPadded.length + len];
        System.arraycopy(unPadded, 0, payLoadBytes, 0, unPadded.length);
        payLoadBytes = cipher.doFinal(payLoadBytes);
        String ret = new String(payLoadBytes, "UTF-8");
        ret = new Regex(ret, "MEGA(\\{.+\\})").getMatch(0);
        if (ret == null) {
            /* verify if the keyString is correct */
            return null;
        } else {
            return ret;
        }
    }
}
