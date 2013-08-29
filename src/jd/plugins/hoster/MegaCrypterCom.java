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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

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
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.images.NewTheme;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megacrypter.com" }, urls = { "http://(www\\.)?megacrypter\\.com/\\![A-Za-z0-9\\-_\\!]+" }, flags = { 0 })
public class MegaCrypterCom extends PluginForHost {

    public MegaCrypterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://megacrypter.com/";
    }

    private String            LINKPART  = null;
    private static AtomicLong CS        = new AtomicLong(System.currentTimeMillis());
    private final String      USE_SSL   = "USE_SSL_V2";
    private final String      USE_TMP   = "USE_TMP_V2";
    private final String      encrypted = ".encrypted";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        LINKPART = new Regex(link.getDownloadURL(), "megacrypter\\.com/(.+)").getMatch(0);
        br.postPageRaw("http://megacrypter.com/api", "{\"m\": \"info\", \"link\":\"" + LINKPART + "\"}");
        if (br.containsHTML("\\{\"error\":3\\}")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("\"name\":\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("\"size\":(\\d+)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getHeaders().put("Content-Type", "application/json");
        br.postPageRaw("http://megacrypter.com/api", "{\"m\": \"dl\", \"link\":\"" + LINKPART + "\"}");
        String dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // if (dl.startDownload()) {
        // if (downloadLink.getLinkStatus().isFinished() && downloadLink.getDownloadCurrent() > 0) {
        // decrypt(downloadLink, keyString);
        // }
        // }
    }

    private String decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_SSL, JDL.L("plugins.hoster.megaconz.usessl", "Use SSL?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_TMP, JDL.L("plugins.hoster.megaconz.usetmp", "Use tmp decrypting file?")).setDefaultValue(false));
    }

    private void decrypt(DownloadLink link, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
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
            if (tmp.exists() && tmp.delete() == false) throw new IOException("Could not delete " + tmp);
        } else {
            if (dst.exists() && dst.delete() == false) throw new IOException("Could not delete " + dst);
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteDst = true;
        try {
            long total = src.length();
            final PluginProgress progress = new PluginProgress(0, total, null) {
                long lastCurrent    = -1;
                long startTimeStamp = -1;

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
                    if (currentTimeDifference <= 0) return;
                    long speed = (current * 10000) / currentTimeDifference;
                    if (speed == 0) return;
                    long eta = ((total - current) * 10000) / speed;
                    this.setETA(eta);
                }

            };
            progress.setProgressSource(this);
            progress.setIcon(NewTheme.I().getIcon("lock", 16));
            link.getLinkStatus().setStatusText("Decrypting");
            progress.setMessage("Decrypting");
            link.setPluginProgress(progress);
            fis = new FileInputStream(src);
            if (tmp != null) {
                fos = new FileOutputStream(tmp);
            } else {
                fos = new FileOutputStream(dst);
            }

            Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            final CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            int read = 0;
            final byte[] buffer = new byte[32767];
            while ((read = fis.read(buffer)) != -1) {
                if (read > 0) {
                    progress.updateValues(progress.getCurrent() + read, total);
                    cos.write(buffer, 0, read);
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
                link.setFinalFileOutput(dst.getAbsolutePath());
                link.setCustomFileOutputFilenameAppend(null);
                link.setCustomFileOutputFilename(null);
            } catch (final Throwable e) {
            }
            if (tmp == null) {
                src.delete();
            } else {
                src.delete();
                tmp.renameTo(dst);
            }
        } finally {
            link.setPluginProgress(null);
            try {
                fis.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (deleteDst) {
                if (tmp != null) {
                    tmp.delete();
                } else {
                    dst.delete();
                }
            }
        }

    }

    private String getPublicFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private String getPublicFileKey(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-]+)").getMatch(0);
    }

    private String getNodeFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#N\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private String getNodeFileKey(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#N\\![a-zA-Z0-9]+\\!([a-zA-Z0-9_,\\-]+)").getMatch(0);
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}