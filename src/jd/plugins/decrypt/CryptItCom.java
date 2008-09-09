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
import java.util.regex.Pattern;

import jd.crypt.AESdecrypt;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class CryptItCom extends PluginForDecrypt {

    static private final String HOST = "crypt-it.com";

    private static final String PATTERN_PASSWORD_FOLDER = "<input type=\"password\"";

    private static final String PATTERN_PW = "Passworteingabe";

    static private final Pattern patternSupported = Pattern.compile("(http|ccf)://[\\w\\.]*?crypt-it\\.com/(s|e|d|c)/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public static RequestInfo postRequest(URL url, String cookie, String referrer, HashMap<String, String> requestProperties, byte[] parameter, boolean redirect) throws IOException {
        HTTPConnection httpConnection = new HTTPConnection(url.openConnection());
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null) {
            httpConnection.setRequestProperty("Referer", referrer);
        } else {
            httpConnection.setRequestProperty("Referer", "http://" + url.getHost());
        }
        if (cookie != null) {
            httpConnection.setRequestProperty("Cookie", cookie);
        }
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
        RequestInfo requestInfo = HTTP.readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        return requestInfo;
    }

    public CryptItCom(String cfgName){
        super(cfgName);
    }

    private ArrayList<DownloadLink> containerStep(CryptedLink param) {
        String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        parameter = parameter.replace("/s/", "/d/");
        parameter = parameter.replace("/e/", "/d/");
        parameter = parameter.replace("ccf://", "http://");

        try {

            RequestInfo requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(parameter), null, null, null, true);

            if (requestInfo.getConnection().getContentType().indexOf("text/html") >= 0) {
                requestInfo = HTTP.readFromURL(requestInfo.getConnection());
                String cookie = requestInfo.getCookie();
                if (requestInfo.containsHTML(PATTERN_PW)) {

                    String pass = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), param.getDecrypterPassword());
                    String postData = "a=pw&pw=" + Encoding.urlEncode(pass);
                    requestInfo = HTTP.postRequest(new URL(parameter), requestInfo.getCookie(), parameter, null, postData, false);
                    if (requestInfo.containsHTML(PATTERN_PW)) {
                        logger.warning("Password wrong");
                        JDUtilities.getController().getUiInterface().showMessageDialog(JDLocale.L("plugins.decrypt.general.passwordWrong", "Passwort falsch"));
                        return decryptedLinks;
                    }
                }
                parameter = parameter.replace("/c/", "/d/");
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(parameter), cookie, null, null, true);
            }

            String name = Plugin.getFileNameFormHeader(requestInfo.getConnection());

            if (name.equals("redir.ccf") || !name.contains(".ccf")) {
                logger.severe("Container not found");
                return null;
            }

            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            Browser.download(container, requestInfo.getConnection());
                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                container.delete();
           

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    private String decrypt(String ciphertext) {
        // alt: byte[] key = new byte[] { (byte) 55, (byte) 55, (byte) 107,
        // (byte) 47, (byte) 108, (byte) 65, (byte) 87, (byte) 72, (byte) 83,
        // (byte) 110, (byte) 116, (byte) 82, (byte) 89, (byte) 100, (byte) 111,
        // (byte) 110, (byte) 116, (byte) 115, (byte) 116, (byte) 101, (byte)
        // 97, (byte) 108, (byte) 112, (byte) (byte) 114 };
        byte[] key = Encoding.Base64Decode("c281c3hOc1BLZk5TRERaSGF5cjMyNTIw").getBytes();
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
            if (rest <= blockSize) {
                break;
            }
            blocks++;
        }
        return new String(cipher).trim();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String url = param.toString();
            if (!url.endsWith("/")) url += "/";

            String[] temp = new Regex(url, Pattern.compile("http://crypt-it.com/(.*?)/(.*?)/", Pattern.CASE_INSENSITIVE)).getRow(0);
            String mode = temp[0];
            String folder = temp[1];
            RequestInfo ri = HTTP.getRequest(new URL("http://crypt-it.com/" + mode + "/" + folder));
            String pass = "";
            if (ri.containsHTML(PATTERN_PASSWORD_FOLDER)) {
                pass = param.getDecrypterPassword();
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    if (pass == null) {
                        pass = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.decrypt.cryptitcom.password", "Ordner ist Passwortgeschützt. Passwort angeben:"), param.getDecrypterPassword());
                        if (pass == null) {
                            /* auf abbruch geklickt */
                            return null;
                        }
                    }
                    String post = "a=pw&pw=" + Encoding.urlEncode(pass);
                    ri = HTTP.postRequest(new URL("http://crypt-it.com/" + mode + "/" + folder), null, null, null, post, true);
                    if (!ri.containsHTML(PATTERN_PASSWORD_FOLDER)) {
                        break;
                    }
                    pass = null;
                }
            }
            if (pass == null) pass = "";/* falls kein passwort korrekt war */
            String cookie = ri.getCookie();
            String packagename = new Regex(ri.getHtmlCode(), Pattern.compile("class=\"folder\">(.*?)</", Pattern.CASE_INSENSITIVE)).getMatch(0);
            String password = new Regex(ri.getHtmlCode(), Pattern.compile("<b>Password:</b>(.*?)<", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password != null) password = password.trim();

            HashMap<String, String> header = new HashMap<String, String>();
            header.put("Content-Type", "application/x-amf");

            byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x11, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74, 0x32, 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x11, 0x0a, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x06 };
            byte[] b2 = new byte[] { 0x02, 0x00 };
            ri = HTTP.postRequest(new URL("http://crypt-it.com/engine/"), cookie, null, header, new String(b) + folder + new String(b2) + new String(new byte[] { (byte) pass.length() }) + pass, false);
            String[] ciphers = new Regex(ri.getHtmlCode(), Pattern.compile("url(.*?)size", Pattern.CASE_INSENSITIVE)).getColumn(0);

            FilePackage fp = new FilePackage();
            fp.setName(packagename);
            fp.setPassword(password);

            progress.setRange(ciphers.length);
            for (String string : ciphers) {
                String cipher = Encoding.filterString(string, "1234567890abcdefABCDEF");
                String linktext = decrypt(cipher);

                String[] links = HTMLParser.getHttpLinks(linktext, null);
                if (links.length > 0 && links[0].startsWith("http")) {
                    DownloadLink link = createDownloadlink(links[0]);
                    link.addSourcePluginPassword(password);
                    link.setSourcePluginComment(packagename);
                    fp.add(link);
                    decryptedLinks.add(link);
                }
                progress.increase(1);
            }
            if (decryptedLinks.size() == 0) { return containerStep(param); }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return HOST;
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
