package jd.plugins.decrypter;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(?:https?://(www\\.)?mega\\.(co\\.)?nz/[^/:]*#F|chrome://mega/content/secure\\.html#F|mega:/*#F)(!|%21)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-%]{16,}((!|%21)[a-zA-Z0-9]+)?(\\?[a-zA-Z0-9]+)?" })
public class MegaConz extends PluginForDecrypt {
    private static AtomicLong CS = new AtomicLong(System.currentTimeMillis());

    public MegaConz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private class MegaFolder {
        private String parent;
        private String name;
        private String id;

        private MegaFolder(String nodeID) {
            id = nodeID;
        }
    }

    private String getParentNodeID(CryptedLink link) {
        final String ret = new Regex(link.getCryptedUrl(), "(!|%21)([a-zA-Z0-9]+)$").getMatch(1);
        final String folderID = getFolderID(link);
        final String masterKey = getMasterKey(link);
        if (ret != null && !ret.equals(folderID) && !ret.equals(masterKey)) {
            return ret;
        }
        return null;
    }

    private static Object                                                           GLOBAL_LOCK = new Object();
    private static WeakHashMap<LinkCrawler, Map<String, List<Map<String, Object>>>> CACHE       = new WeakHashMap<LinkCrawler, Map<String, List<Map<String, Object>>>>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter.setCryptedUrl(parameter.toString().replaceAll("%21", "!"));
        final String folderID = getFolderID(parameter);
        final String folderNodeID = getFolderNodeID(parameter);
        final String masterKey = getMasterKey(parameter);
        final String preferredNodeID = getParentNodeID(parameter);
        final String containerURL;
        if (StringUtils.startsWithCaseInsensitive(parameter.getCryptedUrl(), "chrome:") || StringUtils.startsWithCaseInsensitive(parameter.getCryptedUrl(), "mega:")) {
            if (folderID != null && masterKey != null) {
                containerURL = "https://mega.co.nz/#F!" + folderID + "!" + masterKey;
            } else {
                containerURL = parameter.getCryptedUrl();
            }
        } else {
            containerURL = parameter.getCryptedUrl();
        }
        // br.setCurrentURL("https://mega.nz");
        br.setLoadLimit(256 * 1024 * 1024);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        // br.getHeaders().put("Origin", "https://mega.nz");
        br.getHeaders().put("APPID", "JDownloader");
        br.addAllowedResponseCodes(500);
        int retryCounter = 0;
        final Map<String, FilePackage> fpMap = new HashMap<String, FilePackage>();
        List<Map<String, Object>> folderNodes = new ArrayList<Map<String, Object>>();
        synchronized (CACHE) {
            LinkCrawler crawler = getCrawler();
            if (crawler != null) {
                crawler = crawler.getRoot();
            }
            if (crawler != null) {
                Map<String, List<Map<String, Object>>> map = CACHE.get(crawler);
                if (map == null) {
                    map = new HashMap<String, List<Map<String, Object>>>();
                    CACHE.put(crawler, map);
                }
                if (map.containsKey(folderID)) {
                    folderNodes = map.get(folderID);
                } else {
                    map.put(folderID, folderNodes);
                }
            }
        }
        String sn = null;
        final boolean wscSupport = false;
        String w = null;// websocket
        synchronized (folderNodes) {
            if (folderNodes.size() > 0) {
                logger.info("Found Cache:Nodes" + folderNodes.size() + "|FolderID" + folderID);
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
                            sleep(5000, parameter);
                            retryCounter++;
                            continue;
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    final Object response;
                    try {
                        response = JSonStorage.getMapper().inputStreamToObject(con.getInputStream(), TypeRef.OBJECT);
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
                        final String nextSN = (String) JavaScriptEngineFactory.walkJson(response, "{0}/sn");
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
                        final String nextSN = (String) JavaScriptEngineFactory.walkJson(response, "sn");
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
                            w = (String) JavaScriptEngineFactory.walkJson(response, "w");
                            if (StringUtils.isEmpty(w)) {
                                logger.info("wsc missing");
                                break;
                            } else {
                                logger.info("next page:" + w + "|" + sn);
                            }
                        }
                    } else if (response instanceof Number) {
                        logger.info("Response:" + JSonStorage.toString(response));
                        final Number num = ((Number) response);
                        if (num.intValue() == 0 && w != null) {
                            // WebSocket(wsc) = empty
                            break;
                        } else if (num.intValue() == -3) {
                            if (retryCounter < 10) {
                                sleep(5000, parameter);
                                retryCounter++;
                                continue;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        } else {
                            // https://help.servmask.com/knowledgebase/mega-error-codes/
                            // -3 for EAGAIN
                            return decryptedLinks;
                        }
                    } else {
                        logger.info("Response:" + JSonStorage.toString(response));
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                if (!isAbort()) {
                    final Iterator<Map<String, Object>> it = parsedNodes.iterator();
                    while (it.hasNext()) {
                        final Map<String, Object> parsedNode = it.next();
                        if (isAbort()) {
                            throw new InterruptedException();
                        } else {
                            final String encryptedNodeKey = new Regex(parsedNode.remove("k"), ":([^:]*?)$").getMatch(0);
                            if (encryptedNodeKey == null) {
                                it.remove();
                                logger.info("NodeKey missing:" + JSonStorage.toString(parsedNode));
                                continue;
                            }
                            final String nodeKey;
                            try {
                                nodeKey = decryptNodeKey(encryptedNodeKey, masterKey);
                            } catch (InvalidKeyException e) {
                                logger.log(e);
                                decryptedLinks.add(createOfflinelink(parameter.toString()));
                                return decryptedLinks;
                            }
                            final String nodeAttr = decrypt((String) parsedNode.remove("a"), nodeKey);
                            if (nodeAttr == null) {
                                it.remove();
                                logger.info("NodeAttr missing:" + JSonStorage.toString(parsedNode));
                                continue;
                            }
                            final String nodeName = removeEscape(new Regex(nodeAttr, "\"n\"\\s*?:\\s*?\"(.*?)(?<!\\\\)\"").getMatch(0));
                            final String nodeType = String.valueOf(parsedNode.remove("t"));
                            if ("1".equals(nodeType)) {
                                parsedNode.put("nodeDirectory", Boolean.TRUE);
                            } else if ("0".equals(nodeType)) {
                                // parsedNode.put("nodeDirectory", Boolean.FALSE);
                                final Long nodeSize = JavaScriptEngineFactory.toLong(parsedNode.remove("s"), -1);
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
                    }
                    logger.info("Fill Cache:Nodes:" + parsedNodes.size() + "|FolderID:" + folderID);
                    folderNodes.addAll(parsedNodes);
                }
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
        final HashMap<String, MegaFolder> folders = new HashMap<String, MegaFolder>();
        for (final Map<String, Object> folderNode : folderNodes) {
            if (isAbort()) {
                break;
            }
            final String nodeID = (String) folderNode.get("h");
            final String nodeParentID = (String) folderNode.get("p");
            if (folderNode.containsKey("nodeDirectory")) {
                final String nodeName = (String) folderNode.get("nodeName");
                final MegaFolder fo = new MegaFolder(nodeID);
                fo.parent = nodeParentID;
                fo.name = nodeName;
                folders.put(nodeID, fo);
            } else {
                final Long nodeSize = (Long) folderNode.get("nodeSize");
                final MegaFolder folder = folders.get(nodeParentID);
                if (StringUtils.isNotEmpty(preferredNodeID)) {
                    // see RewriteMegaConz
                    if (StringUtils.equals(preferredNodeID, nodeID)) {
                        // preferred nodeID
                        logger.info("Preferred File NodeID found:" + preferredNodeID);
                    } else if (folder != null) {
                        // check parentNodeID recursive of file
                        MegaFolder checkParent = folder;
                        while (checkParent != null) {
                            if (preferredNodeID.equals(checkParent.id)) {
                                logger.info("Preferred Folder NodeID found:" + preferredNodeID);
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
                        fp.setName(path.substring(path.lastIndexOf("/") + 1));
                        fpMap.put(path, fp);
                    }
                } else {
                    fp = null;
                    path = null;
                }
                if (folderNodeID != null && !StringUtils.equalsIgnoreCase(nodeID, folderNodeID)) {
                    continue;
                }
                final String nodeName = (String) folderNode.get("nodeName");
                final String nodeKey = (String) folderNode.get("nodeKey");
                final String safeNodeKey = nodeKey.replace("+", "-").replace("/", "_");
                final DownloadLink link;
                if (folderID == null) {
                    link = createDownloadlink("https://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey);
                } else {
                    if (safeNodeKey.endsWith("=")) {
                        link = createDownloadlink("https://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey + "###n=" + folderID);
                    } else {
                        link = createDownloadlink("https://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey + "=###n=" + folderID);
                    }
                }
                if (folderID != null) {
                    // folder nodes can only be downloaded with knowledge of the folderNodeID
                    link.setProperty("public", false);
                    link.setProperty("pn", folderID);
                    link.setLinkID(getHost() + "N" + "/" + folderID + "/" + nodeID);
                } else {
                    link.setLinkID(getHost() + "N" + "/" + nodeID);
                }
                link.setContentUrl("https://mega.co.nz/#F!" + folderID + "!" + masterKey + "?" + nodeID);
                link.setContainerUrl(containerURL);
                link.setFinalFileName(nodeName);
                link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
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
                if (fp != null) {
                    fp.add(link);
                }
                decryptedLinks.add(link);
                distribute(link);
                if (folderNodeID != null && StringUtils.equalsIgnoreCase(nodeID, folderNodeID)) {
                    break;
                }
            }
        }
        if (!isAbort() && decryptedLinks.size() == 0 && StringUtils.isNotEmpty(preferredNodeID)) {
            logger.info("Preferred NodeID NOT found:" + preferredNodeID);
        }
        return decryptedLinks;
    }

    private String removeEscape(String match) {
        if (match != null) {
            return match.replaceAll("\\\\", "");
        } else {
            return null;
        }
    }

    private String getRelPath(MegaFolder folder, HashMap<String, MegaFolder> folders) {
        if (folder == null) {
            return "/";
        }
        final StringBuilder ret = new StringBuilder();
        while (true) {
            ret.insert(0, folder.name);
            ret.insert(0, "/");
            final MegaFolder parent = folders.get(folder.parent);
            if (parent == null || parent == folder) {
                //
                return ret.toString();
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
        if (ret != null && !ret.startsWith("MEGA{")) {
            /* verify if the keyString is correct */
            return null;
        }
        return ret;
    }

    private String getFolderID(CryptedLink link) {
        return new Regex(link.getCryptedUrl(), "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private String getFolderNodeID(CryptedLink link) {
        return new Regex(link.getCryptedUrl(), "\\?([a-zA-Z0-9]+)$").getMatch(0);
    }

    private String getMasterKey(CryptedLink link) {
        String ret = new Regex(link.getCryptedUrl(), "#F\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-%]+)").getMatch(0);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }
}
