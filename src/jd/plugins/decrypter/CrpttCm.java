//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.crypt.AESdecrypt;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypt-it.com" }, urls = { "(http|ccf)://[\\w\\.]*?crypt-it\\.com/(s|e|d|c)/[\\w]+" }, flags = { 0 })
public class CrpttCm extends PluginForDecrypt {

    private static final String PATTERN_PASSWORD_FOLDER = "<input type=\"password\"";

    private static final String PATTERN_PW = "Passworteingabe";

    public CrpttCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private ArrayList<DownloadLink> containerStep(CryptedLink param) throws Exception {
        String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        parameter = parameter.replace("/s/", "/d/");
        parameter = parameter.replace("/e/", "/d/");
        parameter = parameter.replace("ccf://", "http://");

        URLConnectionAdapter con = br.openGetConnection(parameter);

        if (con.getContentType().indexOf("text/html") >= 0) {
            // +"" due to refaktor compatibilities. old <ref10000 returns
            // String. else Request INstance
            logger.info(br.loadConnection(con) + "");
            if (br.containsHTML(PATTERN_PW)) {
                String pass = getUserInput(null, param);
                String postData = "a=pw&pw=" + Encoding.urlEncode(pass);
                br.postPage(parameter, postData);
                if (br.containsHTML(PATTERN_PW)) {
                    logger.warning("Password wrong");
                    return decryptedLinks;
                }
            }
            parameter = parameter.replace("/c/", "/d/");
            con = br.openGetConnection(parameter);
        }

        String name = Plugin.getFileNameFromHeader(con);

        if (name.equals("redir.ccf") || !name.contains(".ccf")) {
            logger.severe("Container not found");
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }

        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
        br.downloadConnection(container, con);

        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        container.delete();

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
        byte[] input;
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
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String url = param.toString();
            if (!url.endsWith("/")) url += "/";

            String[] temp = new Regex(url, Pattern.compile("http://crypt-it.com/(.*?)/(.*?)/", Pattern.CASE_INSENSITIVE)).getRow(0);
            String mode = temp[0];
            String folder = temp[1];
            br.getPage("http://crypt-it.com/" + mode + "/" + folder);
            String pass = "";
            if (br.containsHTML(PATTERN_PASSWORD_FOLDER)) {
                pass = param.getDecrypterPassword();
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    if (pass == null) {
                        pass = getUserInput(null, param);
                    }
                    String post = "a=pw&pw=" + Encoding.urlEncode(pass);
                    br.setFollowRedirects(true);
                    br.postPage("http://crypt-it.com/" + mode + "/" + folder, post);
                    if (!br.containsHTML(PATTERN_PASSWORD_FOLDER)) {
                        break;
                    }
                    pass = null;
                }
            }
            if (pass == null) pass = "";/* falls kein passwort korrekt war */
            String packagename = br.getRegex(Pattern.compile("class=\"folder\">(.*?)</", Pattern.CASE_INSENSITIVE)).getMatch(0);
            String password = br.getRegex(Pattern.compile("<b>Password:</b>(.*?)<", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password != null) password = password.trim();

            byte[] b = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x11, 0x63, 0x72, 0x79, 0x70, 0x74, 0x69, 0x74, 0x32, 0x2e, 0x67, 0x65, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x73, 0x00, 0x02, 0x2f, 0x31, 0x00, 0x00, 0x00, 0x11, 0x0a, 0x00, 0x00, 0x00, 0x02, 0x02, 0x00, 0x06 };
            byte[] b2 = new byte[] { 0x02, 0x00 };
            br.getHeaders().put("Content-Type", "application/x-amf");
            br.setFollowRedirects(false);
            String postdata = new String(b) + folder + new String(b2) + new String(new byte[] { (byte) pass.length() }) + pass;
            br.postPageRaw("http://crypt-it.com/engine/", postdata);
            String[] ciphers = br.getRegex(Pattern.compile("url(.*?)size", Pattern.CASE_INSENSITIVE)).getColumn(0);
            br.getHeaders().remove("Content-Type");
            FilePackage fp = FilePackage.getInstance();
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
            if (decryptedLinks.size() == 0) return containerStep(param);
        } catch (DecrypterException j) {
            throw j;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred", e);
            return null;
        }
        return decryptedLinks;
    }

}
