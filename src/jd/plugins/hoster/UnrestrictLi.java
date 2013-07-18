//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "unrestrict.li" }, urls = { "http://\\w+\\.(unrestrict|unr)\\.li/dl/\\w+/.+" }, flags = { 2 })
public class UnrestrictLi extends PluginForHost {

    private static Object LOCK = new Object();

    public UnrestrictLi(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://unrestrict.li");
    }

    public void setBrowser() {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "http://unrestrict.li/terms_and_conditions";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        // Set JDownloader User-Agent
        br.getHeaders().put("User-Agent", "JDownloader");
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                if (dl.getFinalFileName() == null) dl.setFinalFileName(getFileNameFromHeader(con));
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                dl.setAvailable(false);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        showMessage(downloadLink, "Task 1: Check URL validity!");
        requestFileInformation(downloadLink);
        showMessage(downloadLink, "Task 2: Download begins!");
        handleDL(downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 16;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, link.getDownloadURL());
    }

    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        setBrowser();
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        br.setCookie("http://unrestrict.li", "lang", "EN");
        br.setCookie("http://unrestrict.li", "ssl", "0");
        br.postPage("http://unrestrict.li/unrestrict.php", "jdownloader=1&domain=long&link=" + Encoding.urlEncode(link.getDownloadURL()) + (link.getStringProperty("pass", null) != null ? "&download_password=" + Encoding.urlEncode(link.getStringProperty("pass", null)) : ""));
        String generated = br.getRegex("\\{\"(.*?)\":\\{\"host").getMatch(0);
        // Get and set chunks to use
        String cons = br.getRegex("\"cons\":(.*?)\\}").getMatch(0);
        if (cons != null) {
            try {
                link.setProperty("cons", Integer.parseInt(cons));
            } catch (NumberFormatException e) {
            }
        }
        /* START Possible Error Messages */
        if (br.containsHTML("invalid\":\"File offline")) {
            logger.info("File offline");
            MessageDialog("Error", "File offline", false);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("invalid\":\"Host is not supported or unknown link format")) {
            logger.info("Unknown link format");
            MessageDialog("Error", "Unknown link format", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You are not allowed to download from this host")) {
            logger.info("You are not allowed to download from this host");
            MessageDialog("Error", "You are not allowed to download from this host", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"Host is down")) {
            logger.info("Host is down");
            MessageDialog("Error", "Host is down", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"This file is too large")) {
            logger.info("This file is too large");
            MessageDialog("Error", "This file is too large", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You have reached your total daily limit. (Fair Use)")) {
            logger.info("You have reached your total daily limit. (Fair Use)");
            MessageDialog("Error", "You have reached your total daily limit. (Fair Use)", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You have reached your daily limit for this host")) {
            logger.info("You have reached your daily limit for this host");
            MessageDialog("Error", "You have reached your daily limit for this host", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"This link has been reported and blocked")) {
            logger.info("This link has been reported and blocked");
            MessageDialog("Error", "This link has been reported and blocked", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"Error receiving page")) {
            logger.info("Error receiving page");
            if (link.getLinkStatus().getRetryCount() >= 3) {
                link.getLinkStatus().setRetryCount(0);
                MessageDialog("Error", "Error receiving page", false);
                removeHostFromMultiHost(link, acc);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error fetching file information:" + msg, 20 * 1000l);
        }
        if (generated == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* END Possible Error Messages */
        showMessage(link, "Task 2: Download begins!");
        generated = generated.replaceAll("\\\\/", "/");
        try {
            handleDL(link, generated);
            return;
        } catch (PluginException e1) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            /* START Possible Error Messages */
            if (br.containsHTML("Invalid API response.")) {
                logger.info("Invalid API response.");
                MessageDialog("Error", "Invalid API response", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Host not supported.")) {
                logger.info("Host not supported.");
                MessageDialog("Error", "Host not supported", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Error downloading file.")) {
                logger.info("Error downloading file.");
                MessageDialog("Error", "Error downloading file", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Invalid URL returned.")) {
                logger.info("Invalid URL returned.");
                MessageDialog("Error", "Invalid URL returned", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Wrong server.")) {
                logger.info("Wrong server.");
                MessageDialog("Error", "Wrong server", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Request denied")) {
                logger.info("Request denied. Please wait at least 5 seconds before refreshing. (Flooding)");
                MessageDialog("Error", "Request denied. Please wait at least 5 seconds before refreshing. (Flooding)", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Error receiving page.")) {
                logger.info("Error receiving page.");
                MessageDialog("Error", "Error receiving page", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("An unknown error has occured.")) {
                logger.info("An unknown error has occured.");
                MessageDialog("Error", "An unknown error has occured", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            /* END Possible Error Messages */
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(DownloadLink link, String generated) throws Exception {
        if (generated.startsWith("https")) {
            generated = generated.replace("https://", "http://");
        }
        // Get and set chunks, default = 1
        int chunks = 1;
        try {
            chunks = link.hasProperty("cons") ? ((Integer) link.getProperty("cons")) : 1;
        } catch (final Throwable e) {
        }
        // Set JDownloader User-Agent
        br.getHeaders().put("User-Agent", "JDownloader");
        // Chunks is negative to set max number of chunks
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generated, true, -chunks);
        if (dl.getConnection().isContentDisposition()) {
            if (dl.startDownload()) {
                // Generated download link will end with .mega if it's a mega.co.nz link -> Decrypting required
                if (link.getLinkStatus().isFinished() && generated.endsWith(".mega")) {
                    decrypt(link, generated);
                }
            }
            return;
        } else {
            br.followConnection();
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void removeHostFromMultiHost(DownloadLink link, Account acc) throws PluginException {
        Object supportedHosts = acc.getAccountInfo().getProperty("multiHostSupport", null);
        if (supportedHosts != null && supportedHosts instanceof List) {
            ArrayList<String> newList = new ArrayList<String>((List<String>) supportedHosts);
            newList.remove(link.getHost());
            acc.getAccountInfo().setProperty("multiHostSupport", newList);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void MessageDialog(String title, String text, boolean report) {
        UserIO.getInstance().requestMessageDialog(0, title, text);
        // To do: Reporting errors
        /*
         * if(report && UserIO.getInstance().requestConfirmDialog(0, "Error", "Do you want to report this error?") == 2){
         * 
         * }
         */
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setStatus("Wrong username/password or Captcha");
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        account.setConcurrentUsePossible(true);
        // Max. 1 downloads
        account.setMaxSimultanDownloads(16);

        br.getPage("http://unrestrict.li/api/jdownloader/user.php");
        String expires = br.getRegex("<expires>(\\d+)</expires>").getMatch(0);
        String traffic = br.getRegex("<traffic>(\\d+)</traffic>").getMatch(0);
        if (expires != null) {
            account.setValid(true);
            ai.setValidUntil(Long.parseLong(expires) * 1000);
            // Max. 75 GB/day
            ai.setTrafficMax(80530636800l);
            ai.setTrafficLeft(Long.parseLong(traffic));
            ai.setStatus("VIP");
        } else {
            // Not a VIP member
            account.setValid(false);
            // MessageDialog("Error", "Please upgrade to VIP to use this plugin", false);
            ai.setStatus("only VIP members can use this plugin");
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        try {
            String apihosts = br.cloneBrowser().getPage("http://unrestrict.li/api/jdownloader/hosts.php");
            String[] hosts = new Regex(apihosts, "<host>(.*?)</host>").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            // remove youtube support from this multihoster. Our youtube plugin works from cdn/cached final links and this does not work
            // with multihosters as it has geolocation issues. To over come this we need to pass the watch link and not decrypted finallink
            // results...
            supportedHosts.remove("youtube.com");
            supportedHosts.remove("youtu.be");
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Failed to load Unrestrict.li hosts list. Error:" + e.toString());
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                if (ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://unrestrict.li", key, value);
                        }
                        return;
                    }
                }
                setBrowser();
                br.getPage("http://unrestrict.li/sign_in");
                Form form = br.getFormbyProperty("id", "signin_form");
                if (br.containsHTML("solvemedia\\.com/papi/")) {
                    logger.info("Detected captcha method \"solvemedia\" for this host");
                    final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    DownloadLink dummyLink = new DownloadLink(this, "Account", "unrestrict.li", "http://unrestrict.li", true);
                    String code = getCaptchaCode(cf, dummyLink);
                    String chid = sm.getChallenge(code);
                    form.put("adcopy_challenge", chid);
                    form.put("adcopy_response", code.replace(" ", "+"));

                }
                form.put("return", "home");
                form.put("username", Encoding.urlEncode(account.getUser()));
                form.put("password", Encoding.urlEncode(account.getPass()));
                form.put("signin", "Log%20in");
                br.submitForm(form);
                if (br.getCookie("http://unrestrict.li", "unrestrict_user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://unrestrict.li");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void decrypt(DownloadLink link, String generated) throws Exception {
        // Split file name
        String[] fileData = generated.split("\\.");
        int fileDataLength = fileData.length;
        // If last elemement was mega, assume previous element = keyString
        String keyString = fileData[fileDataLength - 2];
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        int[] keyNOnce = new int[] { intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7], intKey[4], intKey[5] };
        byte[] key = aInt_to_aByte(keyNOnce[0], keyNOnce[1], keyNOnce[2], keyNOnce[3]);
        int[] iiv = new int[] { keyNOnce[4], keyNOnce[5], 0, 0 };
        byte[] iv = aInt_to_aByte(iiv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        File dec = new File(link.getFileOutput() + ".dec");
        File org = new File(link.getFileOutput());
        if (dec.exists() && dec.delete() == false) throw new IOException("Could not delete " + dec);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteOutput = true;
        try {
            link.getLinkStatus().setStatusText("Decrypting");
            fis = new FileInputStream(link.getFileOutput());
            fos = new FileOutputStream(dec);

            Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            final CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            int read = 0;
            final byte[] buffer = new byte[32767];
            while ((read = fis.read(buffer)) != -1) {
                if (read > 0) {
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
            deleteOutput = false;
            if (link.getFileOutput().endsWith(".mega")) {
                // Remove key and .mega suffix from file name
                String[] fileNameSplit = link.getFileOutput().split("\\.");
                String[] newFileNameSplit = new String[fileNameSplit.length - 2];
                System.arraycopy(fileNameSplit, 0, newFileNameSplit, 0, fileNameSplit.length - 2);
                String newFileName = implodeArray(newFileNameSplit, ".");
                // Update file names - remove path
                String[] nameNoPathSplit = newFileName.split("\\\\");
                String nameNoPath = nameNoPathSplit[nameNoPathSplit.length - 1];
                link.forceFileName(nameNoPath);
                link.setFinalFileName(nameNoPath);
                link.setName(nameNoPath);
                // Remove any existing files with the same name
                File finalFile = new File(newFileName);
                if (finalFile.exists()) {
                    finalFile.delete();
                }
                // Delete original, rename final file
                org.delete();
                dec.renameTo(finalFile);
            } else {
                org.delete();
                dec.renameTo(org);
            }
            link.getLinkStatus().setStatusText("Finished");
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
            try {
                fos.close();
            } catch (final Throwable e) {
            }
            if (deleteOutput) {
                dec.delete();
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

    private static String implodeArray(String[] inputArray, String glueString) {

        /** Output variable */
        String output = "";

        if (inputArray.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(inputArray[0]);

            for (int i = 1; i < inputArray.length; i++) {
                sb.append(glueString);
                sb.append(inputArray[i]);
            }

            output = sb.toString();
        }

        return output;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}