package jd.plugins.decrypter;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.controlling.ProgressController;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 20929 $", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "https?://(www\\.)?mega\\.co\\.nz/#F![a-zA-Z0-9]+![a-zA-Z0-9_,\\-]+" }, flags = { 0 })
public class MegaConz extends PluginForDecrypt {
    private static AtomicLong CS = new AtomicLong(System.currentTimeMillis());

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String folderID = getFolderID(parameter);
        String masterKey = getMasterKey(parameter);
        br.getHeaders().put("APPID", "JDownloader");
        br.postPageRaw("https://eu.api.mega.co.nz/cs?id=" + CS.incrementAndGet() + "&n=" + folderID, "[{\"a\":\"f\",\"c\":\"1\",\"r\":\"1\"}]");
        String nodes[] = br.getRegex("\\{\\s*?(\"h\".*?)\\}").getColumn(0);
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
        HashMap<String, FilePackage> filePackages = new HashMap<String, FilePackage>();
        for (String node : nodes) {
            String encryptedNodeKey = new Regex(getField("k", node), ":(.*?)$").getMatch(0);
            if (encryptedNodeKey == null) {
                continue;
            }
            String nodeAttr = getField("a", node);
            String nodeID = getField("h", node);
            String nodeKey = decryptNodeKey(encryptedNodeKey, masterKey);
            nodeAttr = decrypt(nodeAttr, nodeKey);
            String nodeName = new Regex(nodeAttr, "\"n\"\\s*?:\\s*?\"(.*?)\"").getMatch(0);
            String nodeType = getField("t", node);
            if ("1".equals(nodeType)) {
                /* folder */
                FilePackage fp = FilePackage.getInstance();
                fp.setName(nodeName);
                filePackages.put(nodeID, fp);
            } else if ("0".equals(nodeType)) {
                /* file */
                String nodeParentID = getField("p", node);
                FilePackage fp = filePackages.get(nodeParentID);
                String nodeSize = getField("s", node);
                if (nodeSize == null) continue;
                String safeNodeKey = nodeKey.replace("+", "-").replace("/", "_");
                DownloadLink link = createDownloadlink("http://mega.co.nz/#N!" + nodeID + "!" + safeNodeKey);
                link.setFinalFileName(nodeName);
                link.setAvailable(true);
                try {
                    link.setVerifiedFileSize(Long.parseLong(nodeSize));
                } catch (final Throwable e) {
                    link.setDownloadSize(Long.parseLong(nodeSize));
                }
                if (fp != null) fp.add(link);
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }

    private String decryptNodeKey(String encryptedNodeKey, String masterKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] masterKeyBytes = b64decode(masterKey);
        byte[] encryptedNodeKeyBytes = b64decode(encryptedNodeKey);
        byte[] ret = new byte[encryptedNodeKeyBytes.length];
        byte[] iv = aInt_to_aByte(0, 0, 0, 0);
        for (int index = 0; index < ret.length; index = index + 16) {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(masterKeyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            System.arraycopy(cipher.doFinal(Arrays.copyOfRange(encryptedNodeKeyBytes, index, index + 16)), 0, ret, index, 16);
        }
        return Base64.encodeToString(ret, false);

    }

    private String getField(String field, String input) {
        return new Regex(input, "\"" + field + "\":\\s*?\"?(.*?)(\"|,)").getMatch(0);
    }

    private byte[] b64decode(String data) {
        data += "==".substring((2 - data.length() * 3) & 3);
        data = data.replace("-", "+").replace("_", "/").replace(",", "");
        return Base64.decode(data);
    }

    private byte[] aInt_to_aByte(int... intKey) {
        byte[] buffer = new byte[intKey.length * 4];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        for (int i = 0; i < intKey.length; i++) {
            bb.putInt(intKey[i]);
        }
        return bb.array();
    }

    private int[] aByte_to_aInt(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        int[] res = new int[bytes.length / 4];
        for (int i = 0; i < res.length; i++) {
            res[i] = bb.getInt(i * 4);
        }
        return res;
    }

    private String decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        byte[] key = null;
        if (intKey.length == 4) {
            /* folder key */
            key = b64Dec;
        } else {
            /* file key */
            key = aInt_to_aByte(intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7]);
        }
        byte[] iv = aInt_to_aByte(0, 0, 0, 0);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        byte[] unPadded = b64decode(input);
        int len = 16 - ((unPadded.length - 1) & 15) - 1;
        byte[] payLoadBytes = new byte[unPadded.length + len];
        System.arraycopy(unPadded, 0, payLoadBytes, 0, unPadded.length);
        payLoadBytes = cipher.doFinal(payLoadBytes);
        String ret = new String(payLoadBytes, "UTF-8");
        if (ret != null && !ret.startsWith("MEGA{")) {
            /* verify if the keyString is correct */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return ret;
    }

    private String getFolderID(CryptedLink link) {
        return new Regex(link.getCryptedUrl(), "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private String getMasterKey(CryptedLink link) {
        return new Regex(link.getCryptedUrl(), "#F\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-]+)").getMatch(0);
    }

}
