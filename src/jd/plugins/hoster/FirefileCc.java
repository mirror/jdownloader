//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.HashInfo;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.jdownloader.controlling.FileStateManager;
import org.jdownloader.controlling.FileStateManager.FILESTATE;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.components.firefile.FirefileCipherOutputStream;
import org.jdownloader.plugins.components.firefile.FirefileLink;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "firefile.cc" }, urls = { "https?://firefile\\.cc/drive/s/[a-zA-Z0-9]+![a-zA-Z0-9]+" })
public class FirefileCc extends PluginForHost {
    private static Object         DECRYPTLOCK                = new Object();
    /** Settings stuff */
    private final String          USED_PLUGIN                = "usedPlugin";
    private final String          USE_TMP                    = "USE_TMP_V2";
    private final String          encrypted                  = ".encrypted";
    private final int             CHUNK_SIZE                 = 75 * 1024 * 1024;
    private final int             ENCRYPTION_SIZE            = 32;
    private final int             CHUNK_SIZE_WITH_ENCRYPTION = CHUNK_SIZE + ENCRYPTION_SIZE;
    /**
     * The number of retries to be performed in order to determine if a file is available.
     */
    private int                   max_number_retries         = 3;
    private volatile DownloadLink decryptingDownloadLink     = null;

    @SuppressWarnings("deprecation")
    public FirefileCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000);
        setConfigElements();
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            if (!StringUtils.equals((String) downloadLink.getProperty(USED_PLUGIN, getHost()), getHost())) {
                return false;
            }
        }
        return super.canHandle(downloadLink, account);
    }

    private byte[] cipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int length2 = cipher.doFinal(outBuf, length1);
        int actualLength = length1 + length2;
        byte[] result = new byte[actualLength];
        System.arraycopy(outBuf, 0, result, 0, result.length);
        return result;
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String linkHash = link.getStringProperty("hash");
        if (linkHash != null) {
            return "firefile.cc://".concat(linkHash);
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        final FirefileLink linkData = FirefileLink.get(link);
        link.setProperty("hash", linkData.getHash());
    }

    private byte[] decrypt(byte[] data, byte[] key, byte[] iv) throws Exception {
        PaddedBufferedBlockCipher aes = this.getCipherInstanceAesEngine(iv, key);
        return cipherData(aes, data);
    }

    private byte[] decryptBytes(byte[] data, String plainKey) throws Exception {
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] payload = Arrays.copyOfRange(data, 16, data.length);
        byte[] decrypted = this.decrypt(payload, plainKey.getBytes(), iv);
        return decrypted;
    }

    private void decryptFile(final String path, AtomicLong encryptionDone, AtomicBoolean successFulFlag, final DownloadLink link, String plainKey) throws Exception {
        final ShutdownVetoListener vetoListener = new ShutdownVetoListener() {
            @Override
            public long getShutdownVetoPriority() {
                return 0;
            }

            @Override
            public void onShutdown(ShutdownRequest request) {
            }

            @Override
            public void onShutdownVeto(ShutdownRequest request) {
            }

            @Override
            public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
                throw new ShutdownVetoException(getHost() + " decryption in progress:" + link.getName(), this);
            }
        };
        ShutdownController.getInstance().addShutdownVetoListener(vetoListener);
        try {
            File dst = null;
            File src = null;
            File tmp = null;
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
            if (tmp != null && tmp.exists() && tmp.delete() == false) {
                throw new IOException("Could not delete temp:" + tmp);
            }
            if (dst.exists() && dst.delete() == false) {
                throw new IOException("Could not delete dest:" + dst);
            }
            FileInputStream fis = null;
            FileOutputStream fos = null;
            boolean deleteDst = true;
            final long total = src.length();
            final AtomicReference<String> message = new AtomicReference<String>();
            final PluginProgress progress = new PluginProgress(0, total, null) {
                long lastCurrent    = -1;
                long startTimeStamp = -1;

                @Override
                public PluginTaskID getID() {
                    return PluginTaskID.DECRYPTING;
                }

                public String getMessage(Object requestor) {
                    return message.get();
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
                        try {
                            FileStateManager.getInstance().requestFileState(outputFile, FILESTATE.WRITE_EXCLUSIVE, this);
                            fos = new FileOutputStream(outputFile);
                            final BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 1024);
                            final FirefileCipherOutputStream cos = new FirefileCipherOutputStream(bos);
                            try {
                                long fileSize = src.length();
                                int numFullChunks = (int) Math.floor(fileSize / Long.valueOf(this.CHUNK_SIZE_WITH_ENCRYPTION));
                                int lastChunkSize = (int) fileSize - (numFullChunks * this.CHUNK_SIZE_WITH_ENCRYPTION);
                                for (int i = 0; i < numFullChunks + 1; i++) {
                                    boolean isLastChunk = i == numFullChunks ? true : false;
                                    final byte[] buffer = new byte[isLastChunk ? lastChunkSize : this.CHUNK_SIZE_WITH_ENCRYPTION];
                                    fis.read(buffer, 0, buffer.length);
                                    progress.updateValues(progress.getCurrent() + buffer.length, total);
                                    final byte[] iv = Arrays.copyOfRange(buffer, 0, 16);
                                    final byte[] payload = Arrays.copyOfRange(buffer, 16, buffer.length);
                                    PaddedBufferedBlockCipher aes = this.getCipherInstanceRijndaelEngine(iv, plainKey.getBytes());
                                    cos.setCipher(aes);
                                    cos.write(payload, 0, payload.length);
                                    encryptionDone.addAndGet(-buffer.length);
                                }
                            } finally {
                                cos.close();
                                bos.close();
                                fos.close();
                            }
                        } finally {
                            fis.close();
                        }
                        if (tmp == null) {
                            src.delete();
                        } else {
                            if (tmp.renameTo(dst)) {
                                src.delete();
                            } else {
                                throw new IOException("Could not rename:" + tmp + "->" + dst);
                            }
                        }
                        deleteDst = false;
                        link.getLinkStatus().setStatusText("Finished");
                        link.removeProperty(USED_PLUGIN);
                        successFulFlag.set(true);
                        try {
                            link.setInternalTmpFilenameAppend(null);
                            link.setInternalTmpFilename(null);
                        } catch (final Throwable e) {
                        }
                    }
                } finally {
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
        } finally {
            ShutdownController.getInstance().removeShutdownVetoListener(vetoListener);
        }
    }

    private String decryptString(String data, String plainKey) throws Exception {
        byte[] dataArr = MegaConz.b64decode(data);
        byte[] decrypted = this.decryptBytes(dataArr, plainKey);
        final String decryptedStr = new String(decrypted, "UTF-8").trim();
        return decryptedStr;
    }

    private void download(DownloadLink link) throws Exception {
        if (link.getDownloadCurrent() > 0 && !StringUtils.equalsIgnoreCase(getHost(), link.getStringProperty(USED_PLUGIN, null))) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Cannot resume paritial loaded file!");
        }
        final FirefileLink linkData = FirefileLink.get(link);
        // check finished encrypted file. if the decryption interrupted - for whatever reason
        final String path = link.getFileOutput();
        final File src = new File(path);
        final AtomicLong encryptionDone = new AtomicLong(link.getVerifiedFileSize());
        final DiskSpaceReservation reservation = new DiskSpaceReservation() {
            @Override
            public File getDestination() {
                return src;
            }

            @Override
            public long getSize() {
                return Math.max(0, encryptionDone.get());
            }
        };
        final AtomicBoolean successfulFlag = new AtomicBoolean(false);
        try {
            checkAndReserve(link, reservation);
            if (src.exists() && src.length() == link.getVerifiedFileSize()) {
                // ready for decryption
                decryptingDownloadLink = link;
                try {
                    decryptFile(path, encryptionDone, successfulFlag, link, linkData.getKey());
                } finally {
                    decryptingDownloadLink = null;
                }
                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                return;
            }
            successfulFlag.set(false);
            try {
                final String downloadURL = getDownloadLink(link);
                if (downloadURL == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled error");
                }
                /* firefile does not like much connections! */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, -10);
                if (dl.getConnection().getResponseCode() == 503) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many connections", 5 * 60 * 1000l);
                }
                if (dl.getConnection().getResponseCode() == 509) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                }
                if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html")) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dl.startDownload()) {
                    if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && link.getDownloadCurrent() > 0) {
                        decryptFile(path, encryptionDone, successfulFlag, link, linkData.getKey());
                    }
                }
            } catch (IOException e) {
                throw e;
            } finally {
                if (!successfulFlag.get() && link.getDownloadCurrent() > 0) {
                    link.setProperty(USED_PLUGIN, getHost());
                }
            }
        } finally {
            free(link, reservation);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://firefile.cc/tos";
    }

    private PaddedBufferedBlockCipher getCipherInstanceAesEngine(byte[] iv, byte[] key) {
        PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new ZeroBytePadding());
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        aes.init(false, ivAndKey);
        return aes;
    }

    private PaddedBufferedBlockCipher getCipherInstanceRijndaelEngine(byte[] iv, byte[] key) {
        PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine(128)), new PKCS7Padding());
        CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        aes.init(false, ivAndKey);
        return aes;
    }

    @Override
    public String getDescription() {
        return "JDownloader's firefile.cc plugin helps downloading files from firefile.cc.";
    }

    @SuppressWarnings("unchecked")
    private String getDownloadLink(DownloadLink link) throws IOException, PluginException {
        final Map<String, Object> fileInfo = getFileInfo(link);
        final Map<String, Object> linkInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(fileInfo, "link");
        if (linkInfo == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> entryInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(fileInfo, "link/entry");
        if (entryInfo == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser api = br.cloneBrowser();
        api.getHeaders().put("Accept", "*/*");
        api.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        api.getPage("https://firefile.cc/secure/uploads/downloadChunk?hashes=" + entryInfo.get("hash") + "&shareable_link=" + linkInfo.get("id"));
        try {
            final Map<String, Object> apiResponse = JSonStorage.restoreFromString(api.toString(), TypeRef.HASHMAP);
            return (String) apiResponse.get("url");
        } catch (JSonMapperException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }

    private Map<String, Object> getFileInfo(DownloadLink link) throws IOException, PluginException {
        final FirefileLink linkData = FirefileLink.get(link);
        final Browser api = br.cloneBrowser();
        api.setAllowedResponseCodes(200, 500);
        api.getHeaders().put("Accept", "*/*");
        api.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        api.getPage("https://firefile.cc/secure/drive/shareable-links/" + linkData.getHash() + "?&withEntries=true");
        try {
            final Map<String, Object> apiResponse = JSonStorage.restoreFromString(api.toString(), TypeRef.HASHMAP);
            return apiResponse;
        } catch (JSonMapperException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        download(link);
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        final FirefileLink linkData = FirefileLink.get(link);
        final Map<String, Object> apiResponse = this.getFileInfo(link);
        final Map<String, Object> file_info = (Map<String, Object>) JavaScriptEngineFactory.walkJson(apiResponse, "link/entry");
        if (file_info == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String nameEnc = (String) file_info.get("name");
        final Long size = JavaScriptEngineFactory.toLong(file_info.get("file_size"), -1);
        if (StringUtils.isEmpty(nameEnc) || size.longValue() == -1) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String nameDec = this.decryptString(nameEnc, linkData.getKey());
        final String hash = (String) file_info.get("hash");
        final String key = (String) file_info.get("key");
        link.setFinalFileName(nameDec);
        link.setVerifiedFileSize(size.longValue());
        if (link.getInternalTmpFilename() == null) {
            link.setInternalTmpFilenameAppend(encrypted);
        }
        if (!StringUtils.isEmpty(hash)) {
            link.setHashInfo(HashInfo.parse(hash));
        }
        if (!StringUtils.isEmpty(key)) {
            link.setProperty("key", key);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_TMP, JDL.L("plugins.hoster.firefilecc.usetmp", "Use tmp decrypting file?")).setDefaultValue(false));
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "firefile.cc", "firefile" };
    }

    private boolean useTMP() {
        return getPluginConfig().getBooleanProperty(USE_TMP, false);
    }
}