package jd.plugins.hoster;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.FileStateManager;
import org.jdownloader.controlling.FileStateManager.FILESTATE;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(https?://(www\\.)?mega\\.(co\\.)?nz/(#N?|\\$)|chrome://mega/content/secure\\.html#)(!|%21)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-]{16,}((=###n=|!)[a-zA-Z0-9]+)?|mega:///#(?:!|%21)[a-zA-Z0-9]+(?:!|%21)[a-zA-Z0-9]{16,}" }, flags = { 0 })
public class MegaConz extends PluginForHost {
    private static AtomicLong CS        = new AtomicLong(System.currentTimeMillis());
    private final String      USE_SSL   = "USE_SSL_V2";
    private final String      USE_TMP   = "USE_TMP_V2";
    private final String      encrypted = ".encrypted";

    public MegaConz(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://mega.co.nz/#terms";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        String url = link.getPluginPatternMatcher();
        final String linkID = link.getSetLinkID();
        if (url.contains(".nz/$!")) {
            url = url.replaceFirst("nz/\\$!", "nz/#!");
            link.setUrlDownload(url);
        }
        final String fileID = getPublicFileID(link);
        if (linkID == null) {
            if (fileID != null) {
                link.setLinkID(getHost() + "F" + fileID);
            } else {
                final String nodeID = getNodeFileID(link);
                if (nodeID != null) {
                    link.setProperty("public", false);
                    link.setLinkID(getHost() + "N" + nodeID);
                }
            }
        }
        if (url.startsWith("chrome://") || url.startsWith("mega:///")) {
            final String keyString = getPublicFileKey(link);
            if (fileID != null && keyString != null) {
                link.setUrlDownload("https://mega.co.nz/#!" + fileID + "!" + keyString);
            }
        } else {
            link.setUrlDownload(url.replaceAll("%21", "!").replaceAll("%20", ""));
        }
        String parentNode = link.getStringProperty("pn", null);
        if (parentNode == null) {
            parentNode = getParentNodeID(link);
            if (parentNode != null) {
                link.setProperty("public", false);
                link.setProperty("pn", parentNode);
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink != null) {
            if (!StringUtils.equals((String) downloadLink.getProperty("usedPlugin", getHost()), getHost())) {
                return false;
            }
        }
        return super.canHandle(downloadLink, account);
    }

    private String getFolderID(final String url) {
        return new Regex(url, "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private final boolean isPublic(final DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("public", true);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        setBrowserExclusive();
        String fileID = getPublicFileID(link);
        String keyString = getPublicFileKey(link);
        if (fileID == null) {
            fileID = getNodeFileID(link);
            keyString = getNodeFileKey(link);
            if (isPublic(link)) {
                // update existing links
                link.setProperty("public", false);
            }
        }
        if (fileID == null || keyString == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("APPID", "JDownloader");
        try {
            final PostRequest request;
            final String parentNode = getParentNodeID(link);
            if (parentNode != null) {
                request = new PostRequest("https://eu.api.mega.co.nz/cs?id=" + CS.incrementAndGet() + "&n=" + parentNode);
            } else {
                request = new PostRequest("https://eu.api.mega.co.nz/cs?id=" + CS.incrementAndGet());
            }
            request.setContentType("text/plain; charset=UTF-8");
            if (isPublic(link)) {
                request.setPostDataString("[{\"a\":\"g\",\"ssl\":" + useSSL() + ",\"p\":\"" + fileID + "\"}]");
            } else {
                request.setPostDataString("[{\"a\":\"g\",\"ssl\":" + useSSL() + ",\"n\":\"" + fileID + "\"}]");
            }
            br.getPage(request);
        } catch (IOException e) {
            // java.io.IOException: 500 Server Too Busy
            if (br.getRequest() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection().getResponseCode() == 500) {
                return AvailableStatus.UNCHECKABLE;
            }
            throw e;
        }
        String fileSize = br.getRegex("\"s\"\\s*?:\\s*?(\\d+)").getMatch(0);
        if (fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            link.setDownloadSize(Long.parseLong(fileSize));
            try {
                link.setVerifiedFileSize(Long.parseLong(fileSize));
            } catch (final Throwable e) {
            }
        }
        String at = br.getRegex("\"at\"\\s*?:\\s*?\"(.*?)\"").getMatch(0);
        if (at == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fileInfo = null;
        try {
            fileInfo = decrypt(at, keyString);
        } catch (final StringIndexOutOfBoundsException e) {
            /* key is incomplete */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileName = new Regex(fileInfo, "\"n\"\\s*?:\\s*?\"(.*?)(?<!\\\\)\"").getMatch(0);
        if (fileName == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(fileName.replaceAll("\\\\", ""));
        try {
            if (link.getInternalTmpFilename() == null) {
                link.setInternalTmpFilenameAppend(encrypted);
            }
        } catch (final Throwable e) {
        }
        link.setProperty("ALLOW_HASHCHECK", false);
        return AvailableStatus.TRUE;

    }

    @Override
    public long calculateAdditionalRequiredDiskSpace(DownloadLink link) {
        final long finalSize = link.getVerifiedFileSize();
        return finalSize >= 0 ? finalSize : 0;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        AvailableStatus available = requestFileInformation(link);
        if (AvailableStatus.FALSE == available) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (AvailableStatus.TRUE != available) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 1 * 60 * 1000l);
        }
        String fileID = getPublicFileID(link);
        String keyString = getPublicFileKey(link);
        if (fileID == null) {
            fileID = getNodeFileID(link);
            keyString = getNodeFileKey(link);
        }
        // check finished encrypted file. if the decryption interrupted - for whatever reason
        String path = link.getFileOutput();
        final File src;
        if (path.endsWith(encrypted)) {
            src = new File(path);
        } else {
            src = new File(path);
        }
        final AtomicLong encryptionDone = new AtomicLong(link.getVerifiedFileSize());
        final DiskSpaceReservation reservation = new DiskSpaceReservation() {

            @Override
            public long getSize() {
                return Math.max(0, encryptionDone.get());
            }

            @Override
            public File getDestination() {
                return src;
            }
        };
        try {
            checkAndReserve(link, reservation);
            if (src.exists() && src.length() == link.getVerifiedFileSize()) {
                // ready for decryption
                decrypt(encryptionDone, link, keyString);
                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                return;
            }
            try {
                if (fileID == null || keyString == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final PostRequest request;
                final String parentNode = getParentNodeID(link);
                if (parentNode != null) {
                    request = new PostRequest("https://eu.api.mega.co.nz/cs?id=" + CS.incrementAndGet() + "&n=" + parentNode);
                } else {
                    request = new PostRequest("https://eu.api.mega.co.nz/cs?id=" + CS.incrementAndGet());
                }
                request.setContentType("text/plain; charset=UTF-8");
                request.setContentType("text/plain; charset=UTF-8");
                if (isPublic(link)) {
                    request.setPostDataString("[{\"a\":\"g\",\"g\":\"1\",\"ssl\":" + useSSL() + ",\"p\":\"" + fileID + "\"}]");
                } else {
                    request.setPostDataString("[{\"a\":\"g\",\"g\":\"1\",\"ssl\":" + useSSL() + ",\"n\":\"" + fileID + "\"}]");
                }
                br.getPage(request);
                String downloadURL = br.getRegex("\"g\"\\s*?:\\s*?\"(https?.*?)\"").getMatch(0);
                if (downloadURL == null) {
                    String error = getError(br);
                    /*
                     * https://mega.co.nz/#doc
                     */
                    if ("-3".equals(error) || br.getRequest().getHtmlCode().trim().equals("-3")) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry again later", 2 * 60 * 1000l);
                    }
                    if ("-11".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Access violation", 5 * 60 * 1000l);
                    }
                    if ("-17".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Request over quota", 60 * 60 * 1000l);
                    }
                    if ("-18".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Resource temporarily not available, please try again later", 5 * 60 * 1000l);
                    }

                    checkServerBusy();
                    logger.info("Unhandled error code: " + error);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* mega does not like much connections! */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, -10);
                if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty("usedPlugin", getHost());
                if (dl.startDownload()) {
                    if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && link.getDownloadCurrent() > 0) {
                        decrypt(encryptionDone, link, keyString);
                    }
                }
            } catch (IOException e) {
                checkServerBusy();
                if (dl != null && dl.getConnection() != null && dl.getConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 1 * 60 * 1000l);
                }
                throw e;
            }
        } finally {
            free(link, reservation);
        }
    }

    @Override
    public void preHandle(DownloadLink downloadLink, Account account, PluginForHost pluginForHost) throws Exception {
        if (downloadLink != null && pluginForHost != null && !StringUtils.equalsIgnoreCase(getHost(), pluginForHost.getHost())) {
            downloadLink.setInternalTmpFilenameAppend(null);
            downloadLink.setInternalTmpFilename(null);
            downloadLink.setProperty("ALLOW_HASHCHECK", Property.NULL);
        }
        super.preHandle(downloadLink, account, pluginForHost);
    }

    /**
     * @throws PluginException
     */
    public void checkServerBusy() throws PluginException {
        if (br.getRequest() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 1 * 60 * 1000l);
        }
    }

    private String decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        if (intKey.length < 8) {
            /* key is not given in link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        byte[] key = aInt_to_aByte(intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7]);
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

    public String getError(Browser br) {
        if (br == null) {
            return null;
        }
        return br.getRegex("\"e\"\\s*?:\\s*?(-?\\d+)").getMatch(0);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_SSL, JDL.L("plugins.hoster.megaconz.usessl", "Use SSL?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_TMP, JDL.L("plugins.hoster.megaconz.usetmp", "Use tmp decrypting file?")).setDefaultValue(false));
    }

    private static Object DECRYPTLOCK = new Object();

    private void decrypt(AtomicLong encryptionDone, DownloadLink link, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        int[] keyNOnce = new int[] { intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7], intKey[4], intKey[5] };
        byte[] key = aInt_to_aByte(keyNOnce[0], keyNOnce[1], keyNOnce[2], keyNOnce[3]);
        int[] iiv = new int[] { keyNOnce[4], keyNOnce[5], 0, 0 };
        byte[] iv = aInt_to_aByte(iiv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        File dst = null;
        File src = null;
        File tmp = null;
        String path = link.getFileOutput();
        if (path.endsWith(encrypted)) {
            src = new File(path);
            String path2 = path.substring(0, path.length() - encrypted.length());
            if (useTMP()) {
                tmp = new File(path2 + ".decrypted");
            }
            dst = new File(path2);
        } else {
            src = new File(path);
            tmp = new File(path + ".decrypted");
            dst = new File(path);
        }
        if (tmp != null) {
            if (tmp.exists() && tmp.delete() == false) {
                throw new IOException("Could not delete " + tmp);
            }
        } else {
            if (dst.exists() && dst.delete() == false) {
                throw new IOException("Could not delete " + dst);
            }
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteDst = true;
        final long total = src.length();
        final AtomicReference<String> message = new AtomicReference<String>();
        final PluginProgress progress = new PluginProgress(0, total, null) {
            long lastCurrent    = -1;
            long startTimeStamp = -1;

            public String getMessage(Object requestor) {
                return message.get();
            }

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.DECRYPTING;
            }

            @Override
            public void updateValues(long current, long total) {
                super.updateValues(current, total);
                if (lastCurrent == -1 || lastCurrent > current) {
                    lastCurrent = current;
                    startTimeStamp = System.currentTimeMillis();
                    this.setETA(-1);
                    return;
                }
                long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                if (currentTimeDifference <= 0) {
                    return;
                }
                long speed = (current * 10000) / currentTimeDifference;
                if (speed == 0) {
                    return;
                }
                long eta = ((total - current) * 10000) / speed;
                this.setETA(eta);
            }

        };
        progress.setProgressSource(this);
        progress.setIcon(new AbstractIcon(IconKey.ICON_LOCK, 16));
        final File outputFile;
        if (tmp != null) {
            outputFile = tmp;
        } else {
            outputFile = dst;
        }
        try {
            try {
                message.set("Queued for decryption");
                link.addPluginProgress(progress);
                synchronized (DECRYPTLOCK) {
                    message.set("Decrypting");
                    fis = new FileInputStream(src);
                    FileStateManager.getInstance().requestFileState(outputFile, FILESTATE.WRITE_EXCLUSIVE, this);
                    fos = new FileOutputStream(outputFile);
                    Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
                    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                    final CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                    int read = 0;
                    final byte[] buffer = new byte[32767];
                    while ((read = fis.read(buffer)) != -1) {
                        if (read > 0) {
                            progress.updateValues(progress.getCurrent() + read, total);
                            cos.write(buffer, 0, read);
                            encryptionDone.addAndGet(-read);
                        }
                    }
                    cos.close();
                    try {
                        fos.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                    deleteDst = false;
                    link.getLinkStatus().setStatusText("Finished");
                    try {
                        link.setInternalTmpFilenameAppend(null);
                        link.setInternalTmpFilename(null);
                    } catch (final Throwable e) {
                    }
                    if (tmp == null) {
                        src.delete();
                    } else {
                        src.delete();
                        tmp.renameTo(dst);
                    }
                }
            } finally {
                try {
                    fis.close();
                } catch (final Throwable e) {
                }
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
                link.removePluginProgress(progress);
                if (deleteDst) {
                    if (tmp != null) {
                        tmp.delete();
                    } else {
                        dst.delete();
                    }
                }
            }
        } finally {
            FileStateManager.getInstance().releaseFileState(outputFile, this);
        }

    }

    private String getPublicFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#(!|%21)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private String getPublicFileKey(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#(!|%21)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-]+)").getMatch(2);
    }

    private String getNodeFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#N(!|%21)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private String getParentNodeID(DownloadLink link) {
        final String ret = new Regex(link.getDownloadURL(), "(=###n=|!|%21)([a-zA-Z0-9]+)$").getMatch(1);
        final String publicFileKey = getPublicFileKey(link);
        final String nodeFileKey = getNodeFileKey(link);
        if (ret != null && !ret.equals(publicFileKey) && !ret.equals(nodeFileKey)) {
            return ret;
        }
        return link.getStringProperty("pn", null);
    }

    private String getNodeFileKey(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#N(!|%21)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-]+)").getMatch(2);
    }

    private byte[] b64decode(String data) {
        data = data.replace(",", "");
        data += "==".substring((2 - data.length() * 3) & 3);
        data = data.replace("-", "+").replace("_", "/");
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

    private String useSSL() {
        if (getPluginConfig().getBooleanProperty(USE_SSL, false)) {
            return "1";
        } else {
            return "0";
        }
    }

    private boolean useTMP() {
        if (getPluginConfig().getBooleanProperty(USE_TMP, false)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (isPublic(downloadLink)) {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        } else {
            if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
                return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
            } else if (StringUtils.equals("real-debrid.com", buildForThisPlugin.getHost())) {
                String fileID = getPublicFileID(downloadLink);
                String keyString = getPublicFileKey(downloadLink);
                if (fileID == null) {
                    fileID = getNodeFileID(downloadLink);
                    keyString = getNodeFileKey(downloadLink);
                }
                return "https://mega.co.nz/#!" + fileID + "!" + keyString + "!" + getParentNodeID(downloadLink);
            } else {
                return null;
            }
        }

    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (downloadLink != null) {
            if (plugin != null) {
                if (!StringUtils.equals((String) downloadLink.getProperty("usedPlugin", plugin.getHost()), plugin.getHost())) {
                    return false;
                }
            }
            return buildExternalDownloadURL(downloadLink, plugin) != null;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("usedPlugin", Property.NULL);
    }

}
