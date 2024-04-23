//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.security.DigestException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.BatoTo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BatoTo.class })
/** This crawler is for bato.to website version v2. */
public class BatoToCrawlerV2 extends PluginForDecrypt {
    public BatoToCrawlerV2(PluginWrapper wrapper) {
        super(wrapper);
        /* Prevent server response 503! */
        BatoTo.setRequestLimits();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public static List<String[]> getPluginDomains() {
        return BatoTo.getPluginDomains();
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

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(series|chapter)/(\\d+)(/[^/\\s]+)?");
        }
        return ret.toArray(new String[0]);
    }

    public int getMaxConcurrentProcessingInstances() {
        /* Prevent server response 503! */
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        /* Login if possible */
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account acc = AccountController.getInstance().getValidAccount(hostPlugin);
        if (acc != null) {
            ((jd.plugins.hoster.BatoTo) hostPlugin).login(acc, false);
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        if (StringUtils.containsIgnoreCase(param.getCryptedUrl(), "/series/")) {
            String[] chapters = br.getRegex("<a[^>]+class\\s*=\\s*\"[^\"]*chapt[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]*/chapter/[^\"]+)\"").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                for (String chapter : chapters) {
                    String url = br.getURL(Encoding.htmlOnlyDecode(chapter)).toExternalForm();
                    final DownloadLink link = createDownloadlink(url);
                    fp.add(link);
                    distribute(link);
                    ret.add(link);
                }
            }
        } else {
            // final String chapterID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
            final String batojs = br.getRegex("const batoPass = (.*?);\\s+").getMatch(0);
            final String cipherText = br.getRegex("const batoWord = \"([^\"]+)").getMatch(0);
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final StringBuilder sb = new StringBuilder();
            sb.append("var batojs = " + batojs + ";");
            sb.append("var server = \"" + cipherText + "\";");
            String secret = null;
            try {
                engine.eval(sb.toString());
                secret = engine.get("batojs").toString();
            } catch (final Exception e) {
                logger.log(e);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String result = aesDecrypt(secret, cipherText);
            final String[] imgParams = result.replaceAll("(\\[|\\]|\")", "").split(",");
            final String titleSeries = br.getRegex("<a href=\"/series/\\d+\">([^<]+)</a>").getMatch(0);
            final String titleChapter = br.getRegex("property=\"og:title\" content=\"([^\"]+)").getMatch(0);
            final String chapterNumber = new Regex(titleChapter, "Chapter\\s*(\\d+)").getMatch(0);
            if (titleSeries != null && chapterNumber != null && titleChapter != null) {
                if (titleChapter.matches("(?i)^" + Pattern.quote(titleSeries) + "\\s*-\\s*Chapter\\s*\\d+")) {
                    /* Title of series and chapter title are the same */
                    fp.setName(titleSeries + " - Chapter " + chapterNumber);
                } else {
                    fp.setName(titleSeries + " - Chapter " + chapterNumber + ": " + titleChapter);
                }
            } else if (titleChapter != null) {
                fp.setName(titleChapter);
            } else {
                fp = null;
            }
            String imgsText = br.getRegex("const (?:imgHttpLis|imgHttps) = \\[(.*?);").getMatch(0);
            imgsText = imgsText.replace("\"", "");
            final String[] imgs = imgsText.split(",");
            final int padLength = StringUtils.getPadLength(imgs.length);
            for (int index = 0; index < imgs.length; index++) {
                String url = imgs[index];
                final String params = imgParams[index];
                url = url + "?" + params;
                final DownloadLink link = createDownloadlink(url);
                final String fname_without_ext = fp.getName() + " - Page " + StringUtils.formatByPadLength(padLength, index);
                final String ext = Plugin.getFileNameExtensionFromURL(url);
                link.setProperty("fname_without_ext", fname_without_ext);
                link.setFinalFileName(fname_without_ext + ext);
                link.setAvailable(true);
                link.setContainerUrl(param.getCryptedUrl());
                link.setContentUrl(br.getURL());
                if (fp != null) {
                    fp.add(link);
                }
                distribute(link);
                ret.add(link);
                index++;
            }
        }
        return ret;
    }

    /**
     * Source: https://stackoverflow.com/questions/41432896/cryptojs-aes-encryption-and-java-aes-decryption </br>
     * Replacement function for this js call: JSON.parse(CryptoJS.AES.decrypt(server, batojs).toString(CryptoJS.enc.Utf8))
     */
    private String aesDecrypt(final String secret, final String cipherText) {
        try {
            byte[] cipherData = Base64.decode(cipherText);
            byte[] saltData = Arrays.copyOfRange(cipherData, 8, 16);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[][] keyAndIV = GenerateKeyAndIV(32, 16, 1, saltData, secret.getBytes("UTF-8"), md5);
            SecretKeySpec key = new SecretKeySpec(keyAndIV[0], "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
            byte[] encrypted = Arrays.copyOfRange(cipherData, 16, cipherData.length);
            Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCBC.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedData = aesCBC.doFinal(encrypted);
            String decryptedText = new String(decryptedData, "UTF-8");
            return decryptedText;
        } catch (final Throwable e) {
            logger.log(e);
            return null;
        }
    }

    /**
     * Generates a key and an initialization vector (IV) with the given salt and password.
     * <p>
     * This method is equivalent to OpenSSL's EVP_BytesToKey function (see
     * https://github.com/openssl/openssl/blob/master/crypto/evp/evp_key.c). By default, OpenSSL uses a single iteration, MD5 as the
     * algorithm and UTF-8 encoded password data.
     * </p>
     *
     * @param keyLength
     *            the length of the generated key (in bytes)
     * @param ivLength
     *            the length of the generated IV (in bytes)
     * @param iterations
     *            the number of digestion rounds
     * @param salt
     *            the salt data (8 bytes of data or <code>null</code>)
     * @param password
     *            the password data (optional)
     * @param md
     *            the message digest algorithm to use
     * @return an two-element array with the generated key and IV
     */
    public static byte[][] GenerateKeyAndIV(int keyLength, int ivLength, int iterations, byte[] salt, byte[] password, MessageDigest md) {
        int digestLength = md.getDigestLength();
        int requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength;
        byte[] generatedData = new byte[requiredLength];
        int generatedLength = 0;
        try {
            md.reset();
            // Repeat process until sufficient data has been generated
            while (generatedLength < keyLength + ivLength) {
                // Digest data (last digest if available, password data, salt if available)
                if (generatedLength > 0) {
                    md.update(generatedData, generatedLength - digestLength, digestLength);
                }
                md.update(password);
                if (salt != null) {
                    md.update(salt, 0, 8);
                }
                md.digest(generatedData, generatedLength, digestLength);
                // additional rounds
                for (int i = 1; i < iterations; i++) {
                    md.update(generatedData, generatedLength, digestLength);
                    md.digest(generatedData, generatedLength, digestLength);
                }
                generatedLength += digestLength;
            }
            // Copy key and IV into separate byte arrays
            byte[][] result = new byte[2][];
            result[0] = Arrays.copyOfRange(generatedData, 0, keyLength);
            if (ivLength > 0) {
                result[1] = Arrays.copyOfRange(generatedData, keyLength, keyLength + ivLength);
            }
            return result;
        } catch (DigestException e) {
            throw new RuntimeException(e);
        } finally {
            // Clean out temporary data
            Arrays.fill(generatedData, (byte) 0);
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}