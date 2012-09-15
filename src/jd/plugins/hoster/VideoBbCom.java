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
package jd.plugins.hoster;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobb.com" }, urls = { "http://(www\\.)?videobb\\.com/(video/|watch_video\\.php\\?v=|e/)\\w+" }, flags = { 2 })
public class VideoBbCom extends PluginForHost {

    protected static class getFinallinkValue {

        private BigInteger BI;
        String             DLLINK;

        getFinallinkValue(final int[] internalKey, final String token, final Browser br) {
            /*
             * TODO: parsing content after 0.9xx through org.codehaus.jackson.JsonNode class
             */
            String dllink = Encoding.Base64Decode(br.getRegex(token + "\":\"(.*?)\",").getMatch(0));
            dllink = dllink.substring(dllink.length() - 1).equals("&") ? dllink : dllink + "&";
            String keyString, decryptedString = "";
            final HashMap<String, Integer> outputKeys = new HashMap<String, Integer>();

            final String outputInfoRaw = br.getRegex("spen\":\"(.*?)\",").getMatch(0);
            final int outputInfoKey = Integer.parseInt(br.getRegex("salt\":(\\d+),").getMatch(0));
            final String[] algoCtrl = JDHexUtils.toString(decryptBit(outputInfoRaw, outputInfoKey, 950569)).split(";");

            if (algoCtrl != null && algoCtrl.length > 1) {

                for (final String eachValue : algoCtrl[1].split("&")) {
                    final String[] parameterTyp = eachValue.split("=");
                    outputKeys.put(parameterTyp[0], Integer.parseInt(parameterTyp[1]));
                }

                final int keyTwo = internalKey[outputKeys.get("ik") - 1];
                final int keyOne = Integer.parseInt(br.getRegex("rkts\":(\\d+),").getMatch(0));

                for (final String eachValue : algoCtrl[0].split("&")) {

                    final String[] parameterTyp = eachValue.split("=");

                    switch (Integer.parseInt(parameterTyp[1])) {
                    case 1:
                        keyString = br.getRegex("sece2\":\"([0-9a-f]+)\",").getMatch(0);
                        decryptedString = decryptByte(keyString, keyOne, keyTwo);
                        break;
                    case 2:
                        keyString = br.getRegex("\\{\"url\":\"([0-9a-f]+)\",").getMatch(0);
                        decryptedString = decryptBit(keyString, keyOne, keyTwo);
                        break;
                    case 3:
                        keyString = br.getRegex("type\":\"([0-9a-f]+)\",").getMatch(0);
                        decryptedString = decryptBit9300(keyString, keyOne, keyTwo);
                        break;
                    case 4:
                        keyString = br.getRegex("time\":\"(.*?)\"").getMatch(0);
                        decryptedString = decryptBitLion(keyString, keyOne, keyTwo);
                        break;
                    case 5:
                        keyString = br.getRegex("euno\":\"(.*?)\"").getMatch(0);
                        decryptedString = decryptBitHeal(keyString, keyOne, keyTwo);
                        break;
                    case 6:
                        keyString = br.getRegex("sugar\":\"(.*?)\"").getMatch(0);
                        decryptedString = decryptBitBrokeUp(keyString, keyOne, keyTwo);
                        break;
                    default:
                        dllink = "";
                        decryptedString = "";
                        break;
                    }
                    dllink = dllink + parameterTyp[0] + "=" + decryptedString + "&";
                }

                DLLINK = dllink + "start=0";
            } else if (algoCtrl != null) {
                DLLINK = "ALGO_CONTROL_ERROR";
            } else {
                DLLINK = null;
            }
        };

        public String convertBin2Str(final String s) {
            /* 11111111 -> FF */
            BI = new BigInteger(s, 2);
            return BI.toString(16);
        }

        public String convertStr2Bin(final String s) {
            /* FF -> 11111111 */
            BI = new BigInteger(1, JDHexUtils.getByteArray(s));
            String result = BI.toString(2);
            while (result.length() % 2 > 0) {
                result = "0" + result;
            }
            return result;
        }

        private String decryptBit(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(false, arg1, arg2, arg3, 11, 77213, 81371, 17, 92717, 192811);
        }

        private String decryptBit9300(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(false, arg1, arg2, arg3, 26, 25431, 56989, 93, 32589, 784152);
        }

        private String decryptBitBrokeUp(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(false, arg1, arg2, arg3, 22, 66595, 17447, 52, 66852, 400595);
        }

        private String decryptBitHeal(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(false, arg1, arg2, arg3, 10, 12254, 95369, 39, 21544, 545555);
        }

        private String decryptBitLion(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(false, arg1, arg2, arg3, 82, 84669, 48779, 32, 65598, 115498);
        }

        private String decryptByte(final String arg1, final int arg2, final int arg3) {
            return zDecrypt(true, arg1, arg2, arg3, 11, 77213, 81371, 17, 92717, 192811);
        }

        private String zDecrypt(final boolean algo, final String cipher, int keyOne, int keyTwo, final int arg0, final int arg1, final int arg2, final int arg3, final int arg4, final int arg5) {
            int x = 0, y = 0, z = 0;
            final char[] C = convertStr2Bin(cipher).toCharArray();
            int len = C.length * 2;
            if (algo) {
                len = 256;
            }
            final int[] B = new int[(int) (len * 1.5)];
            final int[] A = new int[C.length];
            int i = 0;
            for (final char c : C) {
                A[i++] = Character.digit(c, 10);
            }
            i = 0;
            while (i < len * 1.5) {
                keyOne = (keyOne * arg0 + arg1) % arg2;
                keyTwo = (keyTwo * arg3 + arg4) % arg5;
                B[i] = (keyOne + keyTwo) % (len / 2);
                i++;
            }
            i = len;
            while (i >= 0) {
                x = B[i];
                y = i % (len / 2);
                z = A[x];
                A[x] = A[y];
                A[y] = z;
                i--;
            }
            i = 0;
            while (i < len / 2) {
                A[i] = A[i] ^ B[i + len] & 1;
                i++;
            }
            i = 0;
            final StringBuilder sb = new StringBuilder();
            while (i < A.length) {
                sb.append(A[i]);
                i++;
            }
            return convertBin2Str(sb.toString());
        }
    }

    private static String       UA               = RandomUserAgent.generate();
    private static Object       LOCK             = new Object();
    private static final String MAINPAGE         = "http://www.videobb.com/";
    public static final String  ALGOCONTROLERROR = "Server: answer is not correct";
    private final int[]         internalKeys     = { 226593, 441252, 301517, 596338, 852084 };

    public VideoBbCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.videobb.com/premium.php");
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(video|e)/", "/watch_video.php?v="));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (isPremium()) {
            final String expire = br.getRegex(">Premium<.*?until (.*?)</span").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM yyyy", null) + 1000l * 60 * 60 * 24);
            } else {
                logger.warning("Couldn't get the expire date, stopping premium!");
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            }
            ai.setUnlimitedTraffic();
            account.setValid(true);
        } else {
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobb.com/terms.php";
    }

    private String getFinalLink(final DownloadLink downloadLink, final String token) throws Exception {
        final String link = downloadLink.getDownloadURL();
        String referer = br.getRegex("<param value=\"(.*?)\" name=\"movie\">").getMatch(0);
        String setting = Encoding.Base64Decode(br.getRegex("<param value=\"setting=(.*?)\"").getMatch(0));
        if (setting == null || !setting.startsWith("http://")) { return null; }
        setting += "&fv=v1.2.72"; // variable fv aus player.swf

        referer = referer == null ? "http://videobb.com/player/p.swf?v=1_2_8_5" : referer;
        referer = referer.startsWith("http") ? referer : "http://videobb.com" + referer;
        br.getHeaders().put("Referer", referer);
        br.getHeaders().put("x-flash-version", "10,3,183,7");
        br.getPage(setting);
        if (br.containsHTML("\"text\":\"This video can be viewed by owner only\\.\"")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.videobbcom", "Only the owner of this file can download it"));
        try {
            String dllink = new getFinallinkValue(internalKeys, token, br).DLLINK;
            if (isPremium()) {
                dllink = dllink + "&d=" + link.replaceFirst(":", Encoding.urlEncode(":"));
            }
            return dllink;
        } catch (final Throwable e) {
            return null;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setDebug(true);
        final String dllink = getFinalLink(downloadLink, "token1");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (dllink.equals("ALGO_CONTROL_ERROR")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ALGOCONTROLERROR, 15 * 1000l); }
        sleep(3 * 1000l, downloadLink); // Flasplayer to slow
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError: ", 30 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        final String dllink = getFinalLink(downloadLink, "token3");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean isPremium() throws IOException {
        br.getPage(VideoBbCom.MAINPAGE + "my_profile.php");
        final String type = br.getRegex("Account Type:<.*?(>Premium<)").getMatch(0);
        if (type != null) { return true; }
        return false;
    }

    public void login(final Account account) throws Exception {
        synchronized (VideoBbCom.LOCK) {
            setBrowserExclusive();
            br.forceDebug(true);
            prepareBrowser(br);
            br.setFollowRedirects(true);
            final String user = Encoding.urlEncode(account.getUser());
            final String pass = Encoding.urlEncode(account.getPass());
            br.getPage("http://www.videobb.com/index.php");
            br.postPage(VideoBbCom.MAINPAGE + "login.php", "login_username=" + user + "&login_password=" + pass);
            final String cookie = br.getCookie("http://www.videobb.com", "P_sk");
            if (cookie == null || "deleted".equalsIgnoreCase(cookie)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            if (br.containsHTML("The username or password you entered is incorrect")) {
                final String error = br.getRegex("msgicon_error\">(.*?)</div>").getMatch(0);
                logger.warning("Error: " + error);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            this.br.getHeaders().put("Accept-Encoding", "");
            br.getHeaders().put("User-Agent", UA);
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (final Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        try {
            setBrowserExclusive();
            br.getPage(downloadLink.getDownloadURL());
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("(>The page or video you are looking for cannot be found|>Video is not available<|<title>videobb \\- Free Video Hosting \\- Your #1 Video Site</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("Content\\-Disposition: attachment; filename\\*= UTF\\-8\\'\\'(.*?)(<|\")").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("content=\"videobb \\- (.*?)\"  name=\"title\"").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }
}