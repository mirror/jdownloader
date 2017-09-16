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
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginProgress;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megacrypter" }, urls = { "https?://(?:www\\.)?(megacrypter\\.noestasinvitado\\.com|youpaste\\.co|shurcrypter\\.se)/(!|%21)[A-Za-z0-9\\-_\\!%]+" })
public class MegaCrypterCom extends antiDDoSForHost {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "megacrypter.noestasinvitado.com", "youpaste.co", "shurcrypter.se" };
    }

    // note: hosts removed due to be down.
    // 20150206 megacrypter.megabuscame.me/ account suspended on datacenter server.
    // 20160308 encrypterme.ga, no dns
    // 20170112 megacrypter.sytes.net, no dns record
    // 20170728 megacrypter.neerdi.x10.bz / megacrypter.neerdi.com, dead/expired

    public MegaCrypterCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("%21", "!"));
    }

    @Override
    public String getHost(DownloadLink link, Account account) {
        if (link != null) {
            return Browser.getHost(link.getDownloadURL());
        }
        return super.getHost(link, account);
    }

    private void setUrl(final DownloadLink downloadLink) {
        if (false && downloadLink.getDownloadURL().matches("(?i)")) {
            supportsHTTPS = false;
            enforcesHTTPS = false;
        } else if (downloadLink.getDownloadURL().contains("megacrypter.noestasinvitado.com/") || downloadLink.getPluginPatternMatcher().contains("shurcrypter.se")) {
            // all others enable by default.
            supportsHTTPS = true;
            enforcesHTTPS = true;
        } else {
            // all others enable by default.
            supportsHTTPS = true;
            enforcesHTTPS = false;
        }
        boolean useHTTPS = enforcesHTTPS;
        if (supportsHTTPS && !enforcesHTTPS && this.getPluginConfig().getBooleanProperty(preferHTTPS, preferHTTPS_default)) {
            useHTTPS = true;
        }
        mcUrl = (useHTTPS ? "https" : "http") + "://" + new Regex(downloadLink.getDownloadURL(), "://([^/]+)").getMatch(0) + "/api";
    }

    private String mcUrl = null;

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(final DownloadLink downloadLink, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://megacrypter.com/";
    }

    private static enum MegaCrypterComApiErrorCodes {
        FILE_NOT_FOUND(3),
        MC_EMETHOD(1),
        MC_EREQ(2),
        MC_INTERNAL_ERROR(21),
        MC_LINK_ERROR(22),
        MC_BLACKLISTED_LINK(23),
        MC_EXPIRED_LINK(24),
        MEGA_EINTERNAL(-1),
        MEGA_EARGS(-2),
        MEGA_EAGAIN(-3),
        MEGA_ERATELIMIT(-4),
        MEGA_EFAILED(-5),
        MEGA_ETOOMANY(-6),
        MEGA_ERANGE(-7),
        MEGA_EEXPIRED(-8),
        MEGA_ENOENT(-9),
        MEGA_ECIRCULAR(-10),
        MEGA_EACCESS(-11),
        MEGA_EEXIST(-12),
        MEGA_EINCOMPLETE(-13),
        MEGA_EKEY(-14),
        MEGA_ESID(-15),
        MEGA_EBLOCKED(-16),
        MEGA_EOVERQUOTA(-17),
        MEGA_ETEMPUNAVAIL(-18),
        MEGA_ETOOMANYCONNECTIONS(-19),
        MEGA_EWRITE(-20),
        MEGA_EREAD(-21),
        MEGA_EAPPKEY(-22),
        MEGA_EDLURL(-101);

        private int code;

        private MegaCrypterComApiErrorCodes(int code) {
            this.code = code;
        }
    }

    private void checkError(final Browser br) throws PluginException {
        final String code = PluginJSonUtils.getJsonValue(br, "error");
        if (!inValidate(code)) {
            int codeInt = Integer.parseInt(code);
            for (final MegaCrypterComApiErrorCodes v : MegaCrypterComApiErrorCodes.values()) {
                if (v.code == codeInt) {
                    switch (v) {
                    case FILE_NOT_FOUND:
                    case MC_BLACKLISTED_LINK:
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    case MC_EXPIRED_LINK:
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    case MEGA_ETOOMANY:
                    case MEGA_ETOOMANYCONNECTIONS:
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server busy. Try again later", 5 * 60 * 1000l);
                    case MC_INTERNAL_ERROR:
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error. Try again later", 15 * 60 * 1000l);
                    case MC_EMETHOD:
                    case MC_EREQ:
                    case MC_LINK_ERROR:
                    case MEGA_EACCESS:
                    case MEGA_EAGAIN:
                    case MEGA_EAPPKEY:
                    case MEGA_EARGS:
                    case MEGA_EBLOCKED:
                    case MEGA_ECIRCULAR:
                    case MEGA_EDLURL:
                    case MEGA_EEXIST:
                    case MEGA_EEXPIRED:
                    case MEGA_EFAILED:
                    case MEGA_EINCOMPLETE:
                    case MEGA_EINTERNAL:
                    case MEGA_EKEY:
                    case MEGA_ENOENT:
                    case MEGA_EOVERQUOTA:
                    case MEGA_ERANGE:
                    case MEGA_ERATELIMIT:
                    case MEGA_EREAD:
                    case MEGA_ESID:
                    case MEGA_ETEMPUNAVAIL:
                    case MEGA_EWRITE:
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error:" + v + ". Try again later", 15 * 60 * 1000l);
                    default:
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error " + code);
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String        noExpire            = null;
    private String        key                 = null;
    private String        linkPart            = null;
    // Encrypt stuff
    private final String  USE_TMP             = "USE_TMP";
    private final String  encrypted           = ".encrypted";
    private boolean       dl_start            = false;

    private boolean       supportsHTTPS       = false;
    private final String  preferHTTPS         = "preferHTTPS";
    private final boolean preferHTTPS_default = false;
    private boolean       enforcesHTTPS       = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), preferHTTPS, JDL.L("plugins.hoster.megacryptercom.preferHTTPS", "Force secure communication requests via HTTPS over SSL/TLS when supported. Not all hosts support HTTPS!")).setDefaultValue(preferHTTPS_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_TMP, JDL.L("plugins.hoster.megacryptercom.usetmp", "Use tmp decrypting file?")).setDefaultValue(false));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        setUrl(link);
        br.setFollowRedirects(true);
        linkPart = new Regex(link.getDownloadURL(), "/(\\!.+)").getMatch(0);
        noExpire = link.getStringProperty("expire", null);
        // youpaste.co actually verifies if content-type application/json has been set
        postPageRaw(mcUrl, "{\"m\": \"info\", \"link\":\"" + PluginJSonUtils.escape(linkPart) + "\"}", true);
        try {
            checkError(br);
        } catch (final PluginException e) {
            if (noExpire != null) {
                // expire links wont return info, we assume its fine
                return AvailableStatus.TRUE;
            }
            if (!dl_start && e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                return AvailableStatus.UNCHECKABLE;
            }
            throw e;
        }
        // needed for decryption. since linkchecking fails when link expires we need to cache this
        key = PluginJSonUtils.getJsonValue(br, "key");
        String filename = PluginJSonUtils.getJsonValue(br, "name");
        final String filesize = PluginJSonUtils.getJsonValue(br, "size");
        final String expire = PluginJSonUtils.getJsonValue(br, "expire");
        // filename
        final String pass = PluginJSonUtils.getJsonValue(br, "pass");
        if (PluginJSonUtils.parseBoolean(pass)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // TODO, password support
            // {"name":"Zm1pXXQq4\/ovygn9YBtfTHCeDVYbipODFxmdU2RK1ns=","size":195075582,"key":"z\/JW7nqri8R0RjyN2\/iMhWnFOOKRMDo5cJ7J2MLej5n1\/uQ6pkNf7aCU4H12\/Qm4","extra":false,"expire":"1435932308#NTShVnmHzm4+12lxNuybUqng9TIPfjcUz35mRsgUzSI=","pass":"14#Y59Jufe6caJGS4fWft7PB3UTWpYvS59\/5FpQTvY3DEw=#kdFqasP9u\/U=#TTcjSEShR4kdMmzTH+WOeA=="}

            // decrypt filename and key
            // https://tonikelope.github.io/megacrypter/
        }
        // we need key!
        link.setProperty("key", key);

        if (inValidate(filename)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (!inValidate(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (!inValidate(expire)) {
            link.setProperty("expire", expire);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        dl_start = true;
        requestFileInformation(downloadLink);
        key = PluginJSonUtils.getJsonValue(br, "key");
        if (inValidate(key)) {
            key = downloadLink.getStringProperty("key", null);
            if (inValidate(key)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        postPageRaw(mcUrl, "{\"m\": \"dl\", \"link\":\"" + PluginJSonUtils.escape(linkPart) + "\"" + (inValidate(noExpire) || noExpire.split("#").length != 2 ? "" : ", \"noexpire\":\"" + JSonUtils.escape(noExpire.split("#")[1]) + "\"") + "}", true);
        checkError(br);
        String dllink = PluginJSonUtils.getJsonValue(br, "url");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.startDownload()) {
            if (downloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && downloadLink.getDownloadCurrent() > 0) {
                decrypt(downloadLink, key);
            }
        }
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
        PluginProgress progress = null;
        try {
            long total = src.length();
            progress = new PluginProgress(0, total, null) {

                long lastCurrent    = -1;
                long startTimeStamp = -1;

                public String getMessage(Object requestor) {
                    return "Decrypting";
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
            link.getLinkStatus().setStatusText("Decrypting");
            link.addPluginProgress(progress);
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