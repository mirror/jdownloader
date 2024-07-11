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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.MaxTimeSoftReference;
import org.appwork.storage.config.MaxTimeSoftReferenceCleanupCallback;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.storage.simplejson.JSonParser;
import org.appwork.storage.simplejson.JSonValue;
import org.appwork.storage.simplejson.JsonObjectLinkedHashMap;
import org.appwork.storage.simplejson.MinimalMemoryMap;
import org.appwork.storage.simplejson.mapper.JSonMapper;
import org.appwork.utils.ByteArrayWrapper;
import org.appwork.utils.IO;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MegaConzConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(?:https?://(www\\.)?mega\\.(co\\.)?nz/[^/:]*#F|chrome://mega/content/secure\\.html#F|mega:/*#F)(!|%21)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-%]{16,}((!|%21)[a-zA-Z0-9]+)?(\\?[a-zA-Z0-9]+)?" })
public class MegaConz extends PluginForDecrypt {
    private static AtomicLong CS = new AtomicLong(System.currentTimeMillis());

    public MegaConz(PluginWrapper wrapper) {
        super(wrapper);
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
                content = new HashMap<String, Object>();
            }
            content.put(key, value);
        }

        private MegaFolder(String nodeID) {
            id = nodeID;
        }
    }

    private static String getParentNodeID(final String url) {
        final String ret = new Regex(url, "(?:!|%21)([a-zA-Z0-9]+)$").getMatch(0);
        final String folderID = getFolderID(url);
        final String masterKey = getFolderMasterKey(url);
        if (ret != null && !StringUtils.startsWithCaseInsensitive(ret, folderID) && !StringUtils.startsWithCaseInsensitive(ret, masterKey)) {
            return ret;
        }
        return null;
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
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        String contenturl = param.getCryptedUrl().replaceAll("%21", "!");
        final String folderID = getFolderID(contenturl);
        final String folderNodeID = getFolderNodeID(contenturl);
        final String folderMasterKey = getFolderMasterKey(contenturl);
        final String preferredNodeID = getParentNodeID(contenturl);
        final String containerURL;
        if (StringUtils.startsWithCaseInsensitive(contenturl, "chrome:") || StringUtils.startsWithCaseInsensitive(contenturl, "mega:")) {
            /* Change "Application url" to "http url" */
            if (folderID != null && folderMasterKey != null) {
                containerURL = "https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/folder/" + folderID + "#" + folderMasterKey;
            } else {
                containerURL = contenturl;
            }
        } else {
            containerURL = contenturl;
        }
        int retryCounter = 0;
        final Map<String, FilePackage> fpMap = new HashMap<String, FilePackage>();
        final MegaConzConfig config = PluginJsonConfig.get(MegaConzConfig.class);
        boolean newCore = false;
        try {
            Class.forName("org.appwork.storage.config.MaxTimeSoftReference");
            newCore = true;
        } catch (Throwable ignore) {
        }
        final boolean isCrawlerSetFullPathAsPackagename = config.isCrawlerSetFullPathAsPackagename();
        List<Map<String, Object>> folderNodes = null;
        if (!newCore) {
            folderNodes = new ArrayList<Map<String, Object>>();
        } else {
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
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String sn = null;
        final boolean wscSupport = false;
        String w = null;// websocket
        synchronized (folderNodes) {
            if (folderNodes.size() > 0) {
                logger.info("Found Cache:Nodes=" + folderNodes.size() + "|FolderID=" + folderID);
            } else {
                List<Map<String, Object>> parsedNodes = new ArrayList<Map<String, Object>>();
                while (!isAbort()) {
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
                        if (true) {
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
                        } else {
                            SimpleMapper mapper = new SimpleMapper() {
                                @Override
                                protected JSonFactory newJsonFactory(String jsonString) {
                                    if (false) {
                                        return super.newJsonFactory(jsonString);
                                    } else {
                                        return new JSonFactory(jsonString) {
                                            @Override
                                            protected JSonObject createJSonObject() {
                                                return new JsonObjectLinkedHashMap();
                                            }
                                        };
                                    }
                                };

                                @Override
                                protected JSonMapper buildMapper() {
                                    if (false) {
                                        return super.buildMapper();
                                    } else {
                                        return new JSonMapper() {
                                            {
                                                autoMapJsonObjectClass = HashMap.class;
                                                autoMapJsonArrayclass = ArrayList.class;
                                            }
                                        };
                                    }
                                }
                            };
                            response = mapper.inputStreamToObject(con.getInputStream(), TypeRef.OBJECT);
                            mapper = null;
                        }
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
                                break;
                            }
                        } else {
                            logger.info("no more nodes found->abort pagination");
                            break;
                        }
                        final String nextSN = toString(JavaScriptEngineFactory.walkJson(response, "{0}/sn"));
                        if (StringUtils.isEmpty(nextSN)) {
                            logger.info("no next page");
                            break;
                        } else if (!StringUtils.equals(sn, nextSN)) {
                            logger.info("next page:" + sn);
                            sn = nextSN;
                        } else {
                            logger.info("same next page?:" + sn);
                            break;
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
                                break;
                            }
                        } else {
                            logger.info("no more nodes found->abort pagination");
                            break;
                        }
                        final String nextSN = toString(JavaScriptEngineFactory.walkJson(response, "sn"));
                        if (StringUtils.isEmpty(nextSN)) {
                            logger.info("no next page");
                            break;
                        } else if (!StringUtils.equals(sn, nextSN)) {
                            logger.info("next page:" + sn);
                            sn = nextSN;
                        } else {
                            logger.info("same next page?:" + sn);
                            break;
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
                            // https://help.servmask.com/knowledgebase/mega-error-codes/
                            return ret;
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
                    final String nodeKey;
                    try {
                        nodeKey = decryptNodeKey(encryptedNodeKey, folderMasterKey);
                    } catch (final InvalidKeyException e) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
                    }
                    final String nodeAttr = decrypt(toString(parsedNode.remove("a")), nodeKey);
                    if (nodeAttr == null) {
                        it.remove();
                        logger.info("NodeAttr missing:" + JSonStorage.toString(parsedNode));
                        continue;
                    }
                    // TODO: Use json parser here
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
        Boolean preferredNodeIDFound = StringUtils.isNotEmpty(preferredNodeID) ? Boolean.FALSE : null;
        final LinkedHashMap<String, MegaFolder> folders = new LinkedHashMap<String, MegaFolder>();
        for (final Map<String, Object> folderNode : folderNodes) {
            if (isAbort()) {
                throw new InterruptedException();
            }
            final String nodeID = toString(folderNode.get("h"));
            final String nodeParentID = toString(folderNode.get("p"));
            if (folderNode.containsKey("nodeDirectory")) {
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
                final Long nodeSize = (Long) toObject(folderNode.get("nodeSize"));
                final MegaFolder folder = folders.get(nodeParentID);
                if (StringUtils.isNotEmpty(preferredNodeID)) {
                    // see RewriteMegaConz
                    if (StringUtils.equals(preferredNodeID, nodeID)) {
                        // preferred nodeID
                        logger.info("Preferred File NodeID found:" + preferredNodeID);
                        preferredNodeIDFound = true;
                    } else if (folder != null) {
                        // check parentNodeID recursive of file
                        MegaFolder checkParent = folder;
                        while (checkParent != null) {
                            if (preferredNodeID.equals(checkParent.id)) {
                                logger.info("Preferred Folder NodeID found:" + preferredNodeID);
                                preferredNodeIDFound = true;
                                break;
                            } else {
                                checkParent = folders.get(checkParent.parent);
                            }
                        }
                        if (checkParent == null) {
                            continue;
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
                        if (isCrawlerSetFullPathAsPackagename) {
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
                if (folderNodeID != null && !StringUtils.equalsIgnoreCase(nodeID, folderNodeID)) {
                    continue;
                }
                final String nodeName = toString(folderNode.get("nodeName"));
                final String nodeKey = toString(folderNode.get("nodeKey"));
                final String safeNodeKey = nodeKey.replace("+", "-").replace("/", "_");
                final DownloadLink link;
                if (folderID == null) {
                    link = createDownloadlink("https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#N!" + nodeID + "!" + safeNodeKey);
                } else {
                    if (safeNodeKey.endsWith("=")) {
                        link = createDownloadlink("https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#N!" + nodeID + "!" + safeNodeKey + "###n=" + folderID);
                    } else {
                        link = createDownloadlink("https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#N!" + nodeID + "!" + safeNodeKey + "=###n=" + folderID);
                    }
                }
                final MegaFolder parentFolder = folders.get(nodeParentID);
                if (parentFolder != null) {
                    parentFolder.put(nodeID, link);
                }
                if (folderID != null) {
                    // folder nodes can only be downloaded with knowledge of the folderNodeID
                    link.setProperty("public", false);
                    link.setProperty("pn", folderID);
                    link.setProperty("mk", folderMasterKey);
                    link.setLinkID(getHost() + "N" + "/" + folderID + "/" + nodeID);
                } else {
                    link.setLinkID(getHost() + "N" + "/" + nodeID);
                }
                // alternative: https://mega.nz/folder/folderID#masterKey/file/nodeID
                link.setProperty("fa", toObject(folderNode.get("fa")));// file attributes
                link.setContainerUrl(containerURL);
                link.setFinalFileName(nodeName);
                link.setRelativeDownloadFolderPath(path);
                if (path != null) {
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
                link.setContentUrl(jd.plugins.hoster.MegaConz.buildFileLink(link));
                if (fp != null) {
                    fp.add(link);
                }
                ret.add(link);
                distribute(link);
                if (folderNodeID != null && StringUtils.equalsIgnoreCase(nodeID, folderNodeID)) {
                    break;
                }
            }
        }
        if (this.isAbort()) {
            throw new InterruptedException();
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
                    if (preferredNodeIDFound != null) {
                        emptyFolder = folders.get(preferredNodeID);
                    }
                    if (emptyFolder == null) {
                        emptyFolder = folders.entrySet().iterator().next().getValue();
                    }
                }
                if (emptyFolder != null) {
                    final String path = getRelPath(emptyFolder, folders);
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, emptyFolder.id + "_" + path);
                } else {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                }
            }
            if (Boolean.FALSE.equals(preferredNodeIDFound)) {
                logger.info("Preferred NodeID NOT found:" + preferredNodeID);
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
        byte[] masterKeyBytes = jd.plugins.hoster.MegaConz.b64decode(masterKey);
        byte[] encryptedNodeKeyBytes = jd.plugins.hoster.MegaConz.b64decode(encryptedNodeKey);
        byte[] ret = new byte[encryptedNodeKeyBytes.length];
        byte[] iv = jd.plugins.hoster.MegaConz.aInt_to_aByte(0, 0, 0, 0);
        for (int index = 0; index < ret.length; index = index + 16) {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(masterKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            System.arraycopy(cipher.doFinal(Arrays.copyOfRange(encryptedNodeKeyBytes, index, index + 16)), 0, ret, index, 16);
        }
        return Base64.encodeToString(ret, false);
    }

    private String decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = jd.plugins.hoster.MegaConz.b64decode(keyString);
        int[] intKey = jd.plugins.hoster.MegaConz.aByte_to_aInt(b64Dec);
        byte[] key = null;
        if (intKey.length == 4) {
            /* folder key */
            key = b64Dec;
        } else {
            /* file key */
            key = jd.plugins.hoster.MegaConz.aInt_to_aByte(intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7]);
        }
        byte[] iv = jd.plugins.hoster.MegaConz.aInt_to_aByte(0, 0, 0, 0);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        byte[] unPadded = jd.plugins.hoster.MegaConz.b64decode(input);
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

    private static String getFolderID(final String url) {
        return new Regex(url, "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private static String getFolderNodeID(final String url) {
        return new Regex(url, "\\?([a-zA-Z0-9]+)$").getMatch(0);
    }

    public static String getFolderMasterKey(final String url) {
        String ret = new Regex(url, "#F\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-%]+)").getMatch(0);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }
}
