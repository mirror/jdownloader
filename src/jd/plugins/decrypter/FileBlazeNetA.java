//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.UUID;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileblaze.net" }, urls = { "https?://((www|betastage)\\.)?fileblaze\\.net/external(\\-html)?\\.html\\?key=[0-9a-f]+|http://fblaz\\.in/\\w+" }, flags = { 0 })
public class FileBlazeNetA extends PluginForDecrypt {

    private String KEY = null;

    private String ID  = null;

    public FileBlazeNetA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String beautifierString(final Browser amf) {
        final StringBuffer sb = new StringBuffer();
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        for (final byte element : amf.toString().getBytes()) {
            if (element < 127) {
                if (element > 31) {
                    sb.append((char) element);
                } else {
                    sb.append("#");
                }
            }
        }
        if (sb == null || sb.length() == 0) { return null; }
        return sb.toString().replaceAll("#+", "#");
    }

    private boolean checkForErrors(final String s, final String p) {
        if (s.contains("The files have expired")) {
            logger.warning("The files have expired! Url: " + p);
            return false;
        }
        return true;
    }

    private String correctCryptedLink(String s) {
        return s.replaceAll("external\\.html", "external-html.html");
    }

    private byte[] createAMFRequest(final boolean b) {
        String data = "00030000000100046E756C6C00022F31000000E00A00000001110A81134D666C65782E6D6573736167696E672E6D657373616765732E436F6D6D616E644D657373616765136F7065726174696F6E1B636F7272656C6174696F6E496409626F6479136D657373616765496411636C69656E7449641574696D65546F4C6976651764657374696E6174696F6E0F686561646572731374696D657374616D70040506010A0B01010649";
        data += JDHexUtils.getHexString(UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH));
        data += "01040006010A052544534D6573736167696E6756657273696F6E0401094453496406076E696C010400";
        if (b) {
            data = "00030000000100046E756C6C00022F32000001340A00000001110A81134F666C65782E6D6573736167696E672E6D657373616765732E52656D6F74696E674D657373616765136F7065726174696F6E0D736F7572636509626F6479136D657373616765496411636C69656E7449641574696D65546F4C6976651764657374696E6174696F6E0F686561646572731374696D657374616D70061973686F7746696C6573536574010903010641";
            data += JDHexUtils.getHexString(KEY);
            data += "0649";
            data += JDHexUtils.getHexString(UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH));
            data += "010400062B66696C65626C617A655368617265536572766963650A0B0109445349640649";
            data += JDHexUtils.getHexString(ID);
            data += "154453456E64706F696E7401010400";
        }
        return JDHexUtils.getByteArray(data);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.startsWith("http://fblaz.in")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            parameter = br.getURL();
        }
        parameter = parameter.replace("https://", "http://");
        final String domainValue = parameter.startsWith("http://betastage.") ? "betastageflash" : "stream";
        KEY = new Regex(parameter, "key=(\\w+)").getMatch(0);
        if (KEY == null) { return null; }
        boolean htmlFive = true;

        br.getPage(correctCryptedLink(parameter));
        if (br.getHttpConnection().getResponseCode() == 200) { // HTML5 Version
            br.getPage("/soundblaze/external/template?key=" + KEY);
            String sId = br.getRegex("\"sessionId\":\"([0-9A-F]+)\"").getMatch(0);
            String fpName = br.getRegex("\"title\":\"(.*?)\",").getMatch(0);
            if (sId == null || fpName == null) htmlFive = false;
            if (htmlFive) {
                br.getPage("/soundblaze/api/folder?key=" + KEY);
                String[][] songs = br.getRegex("\\{.*?\\}").getMatches();
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.replaceAll("\\\\\"", ""));
                for (String[] song : songs) {
                    HashMap<String, String> ret = new HashMap<String, String>();
                    for (String[] ss : new Regex(song[0], "\"(.*?)\":(\".*?\"|\\d+|true|false)").getMatches()) {
                        ret.put(ss[0], ss[1].replaceAll("\"", ""));
                    }
                    DownloadLink dlLink = createDownloadlink("http://www.fileblaze.net/soundblaze/external/music;jsessionid=" + sId + "?key=" + ret.get("downloadKey"));
                    for (Entry<String, String> next : ret.entrySet()) {
                        dlLink.setProperty(next.getKey(), next.getValue());
                    }
                    String filename = dlLink.getStringProperty("title", "UnknownSong");
                    if (filename.endsWith(".zip")) continue;
                    dlLink.setName(Encoding.htmlDecode(filename.trim()));
                    String filesize = ret.get("size");
                    if (filesize != null) dlLink.setDownloadSize(SizeFormatter.getSize(filesize));
                    dlLink.setAvailable(true);
                    fp.add(dlLink);
                    decryptedLinks.add(dlLink);
                }
            }
        }
        if (!htmlFive) { // AMF Version
            final Browser amf = new Browser();
            // Initialrequest
            getAMFRequest(amf, createAMFRequest(false));
            String result = beautifierString(amf);
            ID = new Regex(result, "([a-f0-9]{8}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{12})").getMatch(0);
            if (ID == null) { return null; }
            // Finalrequest
            getAMFRequest(amf, createAMFRequest(true));
            result = beautifierString(amf);
            if (result == null || !checkForErrors(result, parameter)) { return null; }
            // parsing downloadkeys
            final String[] res = new Regex(result, "([a-f0-9]{8}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{12})#Bs").getColumn(0);
            String dllink = null;
            for (final String s : res) {
                dllink = "http://" + domainValue + ".fileblaze.net/soundblaze/download/file?key=" + s.toLowerCase();
                if (!isAvailable(dllink)) continue;
                DownloadLink dl = createDownloadlink(dllink);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    private void getAMFRequest(final Browser amf, final byte[] b) {
        amf.getHeaders().put("Content-Type", "application/x-amf");
        try {
            PostRequest request = (PostRequest) amf.createPostRequest("http://www.fileblaze.net/soundblaze/messagebroker/amf;jsessionid=null", (String) null);
            request.setPostBytes(b);
            amf.openRequestConnection(request);
            amf.loadConnection(null);
        } catch (Throwable e) {
            /* does not exist in 09581 */
            logger.warning("Not working in JDownloader 0.9581. Please update to JDownloader2!");
        }
    }

    private boolean isAvailable(String s) throws Exception {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(s);
            if (!con.getContentType().contains("html")) return true;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}