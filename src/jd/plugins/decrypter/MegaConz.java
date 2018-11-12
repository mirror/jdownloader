package jd.plugins.decrypter;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(?:https?://(www\\.)?mega\\.(co\\.)?nz/[^/:]*#F|chrome://mega/content/secure\\.html#F|mega:/*#F)(!|%21)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-%]{16,}((!|%21)[a-zA-Z0-9]+)?" })
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

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter.setCryptedUrl(parameter.toString().replaceAll("%21", "!"));
        final String folderID = getFolderID(parameter);
        final String masterKey = getMasterKey(parameter);
        final String parentNodeID = getParentNodeID(parameter);
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
        final URLConnectionAdapter con = br.openRequestConnection(br.createJSonPostRequest("https://g.api.mega.co.nz/cs?id=" + CS.incrementAndGet() + "&n=" + folderID/*
                                                                                                                                                                       * +
                                                                                                                                                                       * "&domain=meganz
                                                                                                                                                                       */, "[{\"a\":\"f\",\"c\":\"1\",\"r\":\"1\",\"ca\":1}]"));// ca=1
                                                                                                                                                                                                                                // ->
                                                                                                                                                                                                                                // !nocache,
                                                                                                                                                                                                                                // commands.cpp
        final Object response;
        try {
            response = JSonStorage.getMapper().inputStreamToObject(con.getInputStream(), TypeRef.OBJECT);
        } finally {
            con.disconnect();
        }
        if (response instanceof Number) {
            return decryptedLinks;
        } else if (!(response instanceof List)) {
            logger.info(JSonStorage.toString(response));
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final List<Map<String, Object>> nodes = (List<Map<String, Object>>) ((List<Map<String, Object>>) response).get(0).get("f");
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
        for (final Map<String, Object> node : nodes) {
            final String encryptedNodeKey = new Regex(node.get("k"), ":(.*?)$").getMatch(0);
            if (encryptedNodeKey == null) {
                continue;
            }
            if (isAbort()) {
                break;
            }
            String nodeAttr = (String) node.get("a");
            final String nodeID = (String) node.get("h");
            final String nodeParentID = (String) node.get("p");
            final String nodeKey;
            try {
                nodeKey = decryptNodeKey(encryptedNodeKey, masterKey);
            } catch (InvalidKeyException e) {
                logger.log(e);
                decryptedLinks.add(createOfflinelink(parameter.toString()));
                return decryptedLinks;
            }
            nodeAttr = decrypt(nodeAttr, nodeKey);
            if (nodeAttr == null) {
                continue;
            }
            final String nodeName = removeEscape(new Regex(nodeAttr, "\"n\"\\s*?:\\s*?\"(.*?)(?<!\\\\)\"").getMatch(0));
            final String nodeType = String.valueOf(node.get("t"));
            if ("1".equals(nodeType)) {
                /* folder */
                final MegaFolder fo = new MegaFolder(nodeID);
                fo.parent = nodeParentID;
                fo.name = nodeName;
                folders.put(nodeID, fo);
            } else if ("0".equals(nodeType)) {
                /* file */
                final Long nodeSize = JavaScriptEngineFactory.toLong(node.get("s"), -1);
                if (nodeSize == -1) {
                    continue;
                }
                final MegaFolder folder = folders.get(nodeParentID);
                if (parentNodeID != null && folder != null) {
                    // check parentNodeID recursive of file
                    MegaFolder checkParent = folder;
                    while (checkParent != null) {
                        if (parentNodeID.equals(checkParent.id)) {
                            break;
                        } else {
                            checkParent = folders.get(checkParent.parent);
                        }
                    }
                    if (checkParent == null) {
                        continue;
                    }
                }
                final FilePackage fp;
                final String path;
                if (folder != null) {
                    fp = FilePackage.getInstance();
                    path = getRelPath(folder, folders);
                    fp.setName(path.substring(1));
                    fp.setProperty("ALLOW_MERGE", true);
                } else {
                    fp = null;
                    path = null;
                }
                final String safeNodeKey = nodeKey.replace("+", "-").replace("/", "_");
                final DownloadLink link = createDownloadlink("http://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey);
                if (folderID != null) {
                    // folder nodes can only be downloaded with knowledge of the folderNodeID
                    link.setProperty("public", false);
                    link.setProperty("pn", folderID);
                    if (safeNodeKey.endsWith("=")) {
                        link.setContentUrl("http://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey + "###n=" + folderID);
                    } else {
                        link.setContentUrl("http://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey + "=###n=" + folderID);
                    }
                    link.setLinkID(getHost() + "N" + "/" + folderID + "/" + nodeID);
                } else {
                    link.setLinkID(getHost() + "N" + "/" + nodeID);
                }
                link.setContainerUrl(containerURL);
                link.setFinalFileName(nodeName);
                link.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, path);
                link.setAvailable(true);
                link.setVerifiedFileSize(nodeSize);
                if (fp != null) {
                    fp.add(link);
                }
                decryptedLinks.add(link);
                distribute(link);
            }
        }
        return decryptedLinks;
    }

    private String removeEscape(String match) {
        if (match != null) {
            return match.replaceAll("\\\\", "");
        }
        return null;
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

    private String getMasterKey(CryptedLink link) {
        String ret = new Regex(link.getCryptedUrl(), "#F\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-%]+)").getMatch(0);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }
}
