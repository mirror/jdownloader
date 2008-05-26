//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.controlling.JDController;
import jd.crypt.AESdecrypt;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR
// http://crypt-it.com/s/B44Z4A

public class CryptItCom extends PluginForDecrypt {

    static private final String HOST = "crypt-it.com";

    private String VERSION = "0.2.0";

    private String CODER = "jD-Team";

    static private final Pattern patternSupported = getSupportPattern("(http|ccf)://[*]crypt-it.com/(s|e|d|c)/[a-zA-Z0-9]+");

    private static final String PATTERN_PW = "Passworteingabe";

    private static final String PATTERN_PACKAGENAME = "<div class=\"folder\">°</div>";

    private static final String PATTERN_PASSWORD = "<b>Password:</b>°<";

    private static final String PATTERN_PASSWORD_FOLDER = "<input type=\"password\"";

    private String PASSWORD_PROTECTED = "Passworteingabe erforderlich";

    public CryptItCom() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();

    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {

        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                String url = parameter;
                if (!url.endsWith("/")) url += "/";
                String mode = getSimpleMatch(url, "http://crypt-it.com/°/°/", 0);
                String folder = getSimpleMatch(url, "http://crypt-it.com/°/°/", 1);
                RequestInfo ri = getRequest(new URL("http://crypt-it.com/" + mode + "/" + folder));
                String pass = "";
                while (ri.containsHTML(PATTERN_PASSWORD_FOLDER)) {
                    pass = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.decrypt.cryptitcom.password", "Ordner ist Passwortgeschützt. Passwort angeben:"));
                    if (pass == null) { return null; }
                    String post = "a=pw&pw=" + JDUtilities.urlEncode(pass);
                    ri = postRequest(new URL("http://crypt-it.com/" + mode + "/" + folder), null, null, null, post, true);
 }
                String cookie = ri.getCookie();
                String packagename = getSimpleMatch(ri, PATTERN_PACKAGENAME, 0);
                String password = getSimpleMatch(ri, PATTERN_PASSWORD, 0);
                if (password != null) password = password.trim();
                HashMap<String, String> header = new HashMap<String, String>();
                header.put("Content-Type", "application/x-amf");
                // alt: byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00,
                // 0x01, 0x00, 0x10, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74,
                // 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00,
                // 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x0e, 0x0a, 0x00, 0x00,
                // 0x00, 0x01, 0x02, 0x00, 0x06 };
                byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x11, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74, 0x32, 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x11, 0x0a, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x06 };
                byte[] b2 = new byte[] { 0x02, 0x00 };
                ri = postRequest(new URL("http://crypt-it.com/engine/"), cookie, null, header, new String(b) + folder + new String(b2) + String.valueOf(pass.length()) + pass, false);
                ArrayList<String> ciphers = getAllSimpleMatches(ri, "url°size", 1);
                progress.setRange(ciphers.size());           
                FilePackage fp = new FilePackage();
                fp.setName(packagename);
                fp.setPassword(password);
                Vector<String> p = new Vector<String>();
                p.add(password);
                for (Iterator<String> it = ciphers.iterator(); it.hasNext();) {
                    String cipher = JDUtilities.filterString(it.next(), "1234567890abcdefABCDEF");
                    String linktext = decrypt(cipher);
                    progress.increase(1);
                    String[] links;

                    links = Plugin.getHttpLinks(linktext, null);
                    if (links.length > 0 && links[0].startsWith("http")) {
                        DownloadLink link = this.createDownloadlink(links[0]);
                        link.setSourcePluginPasswords(p);
                        link.setSourcePluginComment(packagename);

                        fp.add(link);
                        decryptedLinks.add(link);
                    }
                }
                if (decryptedLinks.size() == 0) {
                    return containerStep(step, parameter);
                } else {
                    step.setParameter(decryptedLinks);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    private PluginStep containerStep(PluginStep step, String parameter) {

        if (step.getStep() == PluginStep.STEP_DECRYPT) {

            // surpress jd warning
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            step.setParameter(decryptedLinks);

            parameter = parameter.replace("/s/", "/d/");
            parameter = parameter.replace("/e/", "/d/");
            parameter = parameter.replace("ccf://", "http://");

            try {

                requestInfo = getRequestWithoutHtmlCode(new URL(parameter), null, null, null, true);

                if (requestInfo.getConnection().getContentType().indexOf("text/html") >= 0) {
                    requestInfo = readFromURL(requestInfo.getConnection());
                    String cookie = requestInfo.getCookie();
                    if (requestInfo.containsHTML(PATTERN_PW)) {

                        String pass = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                        String postData = "a=pw&pw=" + JDUtilities.urlEncode(pass);
                        requestInfo = postRequest(new URL(parameter), requestInfo.getCookie(), parameter, null, postData, false);
                        if (requestInfo.containsHTML(PATTERN_PW)) {

                            logger.warning("Password wrong");
                            JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("plugins.decrypt.general.passwordWrong", "Passwort falsch"));
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;

                        }
                    }

                    parameter = parameter.replace("/c/", "/d/");
                    requestInfo = getRequestWithoutHtmlCode(new URL(parameter), cookie, null, null, true);
                }

                String folder = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
                String name = this.getFileNameFormHeader(requestInfo.getConnection());

                if (name.equals("redir.ccf") || !name.contains(".ccf")) {

                    logger.severe("Container not found");
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return null;

                }

                // download
                File file = new File(folder, name);
                int i = 0;

                while (file.exists()) {

                    String newName = name.substring(0, name.length() - 4) + "-" + String.valueOf(i) + ".ccf";
                    file = new File(folder, newName);
                    i++;

                }

                logger.info("Download container: " + file.getAbsolutePath());
                JDUtilities.download(file, requestInfo.getConnection());

                // read container
                JDController controller = JDUtilities.getController();
                controller.loadContainerFile(file);

                // delete container
                file.deleteOnExit();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    private String decrypt(String ciphertext) {
        // alt: byte[] key = new byte[] { (byte) 55, (byte) 55, (byte) 107,
        // (byte) 47, (byte) 108, (byte) 65, (byte) 87, (byte) 72, (byte) 83,
        // (byte) 110, (byte) 116, (byte) 82, (byte) 89, (byte) 100, (byte) 111,
        // (byte) 110, (byte) 116, (byte) 115, (byte) 116, (byte) 101, (byte)
        // 97, (byte) 108, (byte) 112, (byte) (byte) 114 };
        byte[] key = JDUtilities.Base64Decode("c281c3hOc1BLZk5TRERaSGF5cjMyNTIw").getBytes();
        byte[] cipher = new byte[ciphertext.length() / 2 + ciphertext.length() % 2];

        for (int i = 0; i < ciphertext.length(); i += 2) {
            String sub = ciphertext.substring(i, Math.min(ciphertext.length(), i + 2));
            cipher[i / 2] = (byte) Integer.parseInt(sub, 16);

        }

        AESdecrypt aes = new AESdecrypt(key, 6);
        int blockSize = 16;
        byte[] input = new byte[blockSize];
        byte[] output = new byte[blockSize];
        int blocks = 0;
        int rest = 0;
        while (true) {
            rest = cipher.length - blocks * blockSize;
            int cb = Math.min(rest, blockSize);
            input = new byte[blockSize];
            System.arraycopy(cipher, blocks * blockSize, input, 0, cb);
            aes.InvCipher(input, output);
            System.arraycopy(output, 0, cipher, blocks * blockSize, cb);
            if (rest <= blockSize) break;
            blocks++;

        }
        return new String(cipher).trim();

    }

    public static RequestInfo postRequest(URL url, String cookie, String referrer, HashMap<String, String> requestProperties, byte[] parameter, boolean redirect) throws IOException {
        // logger.finer("post: "+link+"(cookie:"+cookie+" parameter:
        // "+parameter+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(url.openConnection());

        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + url.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        if (requestProperties != null) {
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }
        if (parameter != null) {

            httpConnection.setRequestProperty("Content-Length", parameter.length + "");
        }
        httpConnection.setDoOutput(true);
        httpConnection.connect();

        httpConnection.post(parameter);

        RequestInfo requestInfo = readFromURL(httpConnection);

        requestInfo.setConnection(httpConnection);
        // logger.finer("postRequest " + url + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
