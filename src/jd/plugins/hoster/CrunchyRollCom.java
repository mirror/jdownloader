//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.utils.locale.JDL;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crunchyroll.com" }, urls = { "http://www\\.crunchyroll\\.com/(xml/\\?req=RpcApiVideoPlayer_GetStandardConfig\\&media_id=[0-9]+.*|xml/\\?req=RpcApiSubtitle_GetXml\\&subtitle_script_id=[0-9]+.*|android_rpc/\\?req=RpcApiAndroid_GetVideoWithAcl\\&media_id=[0-9]+.*)" }, flags = { 2 })
public class CrunchyRollCom extends PluginForHost {

    static private Object                                    lock                 = new Object();
    static private HashMap<Account, HashMap<String, String>> loginCookies         = new HashMap<Account, HashMap<String, String>>();
    static private final String                              RCP_API_VIDEO_PLAYER = "RpcApiVideoPlayer_GetStandardConfig";
    static private final String                              RCP_API_SUBTITLE     = "RpcApiSubtitle_GetXml";
    static private final String                              RCP_API_ANDROID      = "RpcApiAndroid_GetVideoWithAcl";

    @SuppressWarnings("deprecation")
    public CrunchyRollCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.crunchyroll.com/login");
    }

    /**
     * Decrypt and convert the downloaded file from CrunchyRoll's own encrypted xml format into its .ass equivalent.
     * 
     * @param downloadLink
     *            The DownloadLink to convert to .ass
     */
    private void convertSubs(final DownloadLink downloadLink) throws PluginException {
        downloadLink.getLinkStatus().setStatusText("Decrypting subtitles...");
        try {
            // Create the XML Parser
            final DocumentBuilderFactory xmlDocBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder xmlDocBuilder = xmlDocBuilderFactory.newDocumentBuilder();
            final Document xml = xmlDocBuilder.parse(new File(downloadLink.getFileOutput()));
            xml.getDocumentElement().normalize();

            // Get the subtitle information
            final Element xmlSub = (Element) xml.getElementsByTagName("subtitle").item(0);

            final Node xmlId = xmlSub.getAttributeNode("id");
            final Node xmlIv = xmlSub.getElementsByTagName("iv").item(0);
            final Node xmlData = xmlSub.getElementsByTagName("data").item(0);

            final int subId = Integer.parseInt(xmlId.getNodeValue());
            final String subIv = xmlIv.getTextContent();
            final String subData = xmlData.getTextContent();

            // Generate the AES parameters
            final byte[] key = this.subsGenerateKey(subId, 32);
            final byte[] ivData = DatatypeConverter.parseBase64Binary(subIv);
            final byte[] encData = DatatypeConverter.parseBase64Binary(subData);
            byte[] decrypted = null;
            try {
                final KeyParameter keyParam = new KeyParameter(key);
                final CipherParameters cipherParams = new ParametersWithIV(keyParam, ivData);

                // Prepare the cipher (AES, CBC, no padding)
                final BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
                cipher.reset();
                cipher.init(false, cipherParams);

                // Decrypt the subtitles
                decrypted = new byte[cipher.getOutputSize(encData.length)];
                final int decLength = cipher.processBytes(encData, 0, encData.length, decrypted, 0);
                cipher.doFinal(decrypted, decLength);
            } catch (final Throwable e) {
                logger.severe(e.getMessage());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error decrypting subtitles!");
            }
            // Create the XML Parser (and zlib decompress using InflaterInputStream)
            final DocumentBuilderFactory subsDocBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder subsDocBuilder = subsDocBuilderFactory.newDocumentBuilder();
            final Document subs = subsDocBuilder.parse(new InflaterInputStream(new ByteArrayInputStream(decrypted)));
            subs.getDocumentElement().normalize();

            // Get the header
            final Element subHeaderElem = (Element) subs.getElementsByTagName("subtitle_script").item(0);
            final String subHeaderTitle = subHeaderElem.getAttributeNode("title").getNodeValue();
            final String subHeaderWrap = subHeaderElem.getAttributeNode("wrap_style").getNodeValue();
            final String subHeaderResX = subHeaderElem.getAttributeNode("play_res_x").getNodeValue();
            final String subHeaderResY = subHeaderElem.getAttributeNode("play_res_y").getNodeValue();

            final String subHeader = "[Script Info]\nTitle: " + subHeaderTitle + "\nScriptType: v4.00+\nWrapStyle: " + subHeaderWrap + "\nPlayResX: " + subHeaderResX + "\nPlayResY: " + subHeaderResY + "\n";

            // Get the styles
            String subStyles = "[V4 Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n";
            final NodeList subStylesNodes = subs.getElementsByTagName("style");

            for (int i = 0; i < subStylesNodes.getLength(); i++) {
                final Element subStylesElem = (Element) subStylesNodes.item(i);
                final String subStylesName = subStylesElem.getAttributeNode("name").getNodeValue();
                final String subStylesFontName = subStylesElem.getAttributeNode("font_name").getNodeValue();
                final String subStylesFontSize = subStylesElem.getAttributeNode("font_size").getNodeValue();
                final String subStylesPriColor = subStylesElem.getAttributeNode("primary_colour").getNodeValue();
                final String subStylesSecColor = subStylesElem.getAttributeNode("secondary_colour").getNodeValue();
                final String subStylesOutColor = subStylesElem.getAttributeNode("outline_colour").getNodeValue();
                final String subStylesBacColor = subStylesElem.getAttributeNode("back_colour").getNodeValue();
                final String subStylesUnderline = subStylesElem.getAttributeNode("underline").getNodeValue();
                final String subStylesStrikeout = subStylesElem.getAttributeNode("strikeout").getNodeValue();
                final String subStylesAlignment = subStylesElem.getAttributeNode("alignment").getNodeValue();
                final String subStylesSpacing = subStylesElem.getAttributeNode("spacing").getNodeValue();
                final String subStylesItalic = subStylesElem.getAttributeNode("italic").getNodeValue();
                String subStylesScaleX = subStylesElem.getAttributeNode("scale_x").getNodeValue();
                String subStylesScaleY = subStylesElem.getAttributeNode("scale_y").getNodeValue();
                final String subStylesBorder = subStylesElem.getAttributeNode("border_style").getNodeValue();
                final String subStylesShadow = subStylesElem.getAttributeNode("shadow").getNodeValue();
                final String subStylesBold = subStylesElem.getAttributeNode("bold").getNodeValue();
                final String subStylesAngle = subStylesElem.getAttributeNode("angle").getNodeValue();
                final String subStylesOutline = subStylesElem.getAttributeNode("outline").getNodeValue();
                final String subStylesMarginL = subStylesElem.getAttributeNode("margin_l").getNodeValue();
                final String subStylesMarginR = subStylesElem.getAttributeNode("margin_r").getNodeValue();
                final String subStylesMarginV = subStylesElem.getAttributeNode("margin_v").getNodeValue();
                final String subStylesEncoding = subStylesElem.getAttributeNode("encoding").getNodeValue();

                // Fix the odd case where the subtitles are scaled to nothing
                if (subStylesScaleX.equals("0")) {
                    subStylesScaleX = "100";
                }
                if (subStylesScaleY.equals("0")) {
                    subStylesScaleY = "100";
                }

                subStyles += "Style: " + subStylesName + ", " + subStylesFontName + ", " + subStylesFontSize + ", " + subStylesPriColor + ", " + subStylesSecColor + ", " + subStylesOutColor + ", " + subStylesBacColor + ", " + subStylesBold + ", " + subStylesItalic + ", " + subStylesUnderline + ", " + subStylesStrikeout + ", " + subStylesScaleX + ", " + subStylesScaleY + ", " + subStylesSpacing + ", " + subStylesAngle + ", " + subStylesBorder + ", " + subStylesOutline + ", " + subStylesShadow + ", " + subStylesAlignment + ", " + subStylesMarginL + ", " + subStylesMarginR + ", " + subStylesMarginV + ", " + subStylesEncoding + "\n";
            }

            // Get the elements
            String subEvents = "[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n";
            final NodeList subEventsNodes = subs.getElementsByTagName("event");

            for (int i = 0; i < subEventsNodes.getLength(); i++) {
                final Element subEventsElem = (Element) subEventsNodes.item(i);
                final String subEventsStart = subEventsElem.getAttributeNode("start").getNodeValue();
                final String subEventsEnd = subEventsElem.getAttributeNode("end").getNodeValue();
                final String subEventsStyle = subEventsElem.getAttributeNode("style").getNodeValue();
                final String subEventsName = subEventsElem.getAttributeNode("name").getNodeValue();
                final String subEventsMarginL = subEventsElem.getAttributeNode("margin_l").getNodeValue();
                final String subEventsMarginR = subEventsElem.getAttributeNode("margin_r").getNodeValue();
                final String subEventsMarginV = subEventsElem.getAttributeNode("margin_v").getNodeValue();
                final String subEventsEffect = subEventsElem.getAttributeNode("effect").getNodeValue();
                final String subEventsText = subEventsElem.getAttributeNode("text").getNodeValue();

                subEvents += "Dialogue: 0," + subEventsStart + "," + subEventsEnd + "," + subEventsStyle + "," + subEventsName + "," + subEventsMarginL + "," + subEventsMarginR + "," + subEventsMarginV + "," + subEventsEffect + "," + subEventsText + "\n";
            }

            // Output to the original file
            final FileWriter subOutFile = new FileWriter(downloadLink.getFileOutput());
            final BufferedWriter subOut = new BufferedWriter(subOutFile);

            try {
                subOut.write(subHeader + "\n");
                subOut.write(subStyles + "\n");
                subOut.write(subEvents);
            } catch (final Throwable e) {
                subOut.close();
                subOutFile.close();
                throw new PluginException(LinkStatus.ERROR_LOCAL_IO, "Error writing decrypted subtitles!");
            }

            subOut.close();
            subOutFile.close();
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.crunchyrollcom.decryptedsubtitles", "Subtitles decrypted"));
        } catch (final SAXException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error decrypting subtitles: Invalid XML file!");
        } catch (final DOMException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error decrypting subtitles: XML file changed!");
        } catch (final PluginException e) {
            throw e;
        } catch (final Throwable e) {
            e.printStackTrace();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Error decrypting subtitles!");
        }
    }

    /**
     * Download the given file using the HTTP Android method. The file will be mp4 and have subtitles hardcoded.
     * 
     * @param downloadLink
     *            The DownloadLink to try and download using RTMP
     */
    private void downloadAndroid(final DownloadLink downloadLink) throws Exception {
        // Check if the link appears to be valid
        final String videoUrl = downloadLink.getStringProperty("videourl");
        if ((Boolean) downloadLink.getProperty("valid", false) && videoUrl != null) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, videoUrl, true, 0);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().startsWith("video")) {
                downloadLink.setProperty("valid", false);
                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            this.dl.startDownload();
        }
    }

    /**
     * Attempt to download the given file using RTMP (rtmpdump). Needs to use the properties "valid", "rtmphost", "rtmpfile", "rtmpswf",
     * "swfdir". These are set by jd.plugins.decrypter.CrchyRollCom.setRMP() through requestFileInformation()
     * 
     * @param downloadLink
     *            The DownloadLink to try and download using RTMP
     */
    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
        // Check if the link appears to be valid
        if ((Boolean) downloadLink.getProperty("valid", false) && downloadLink.getStringProperty("rtmphost").startsWith("rtmp")) {
            final String url = downloadLink.getStringProperty("rtmphost") + "/" + downloadLink.getStringProperty("rtmpfile");

            // Create the download
            this.dl = new RTMPDownload(this, downloadLink, url);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) this.dl).getRtmpConnection();

            // Set all of the needed rtmpdump parameters
            rtmp.setUrl(url);
            rtmp.setTcUrl(downloadLink.getStringProperty("rtmphost"));
            rtmp.setPlayPath(downloadLink.getStringProperty("rtmpfile"));
            rtmp.setSwfVfy(downloadLink.getStringProperty("swfdir") + downloadLink.getStringProperty("rtmpswf"));
            rtmp.setResume(true);

            ((RTMPDownload) this.dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, "Invalid download");
        }
    }

    /**
     * Download subtitles and convert them to .ass
     * 
     * @param downloadLink
     *            The DownloadLink to try and download convert to .ass
     */
    private void downloadSubs(final DownloadLink downloadLink) throws Exception {
        if ((Boolean) downloadLink.getProperty("valid", false)) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadLink.getDownloadURL(), true, 1);
            if (!this.dl.getConnection().isContentDisposition() && !this.dl.getConnection().getContentType().endsWith("xml")) {
                downloadLink.setProperty("valid", false);
                this.dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (this.dl.startDownload()) {
                this.convertSubs(downloadLink);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br, true, true);
            // TODO Find the expiration date of the premium status
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.crunchyrollcom.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.crunchyroll.com/tos";
    }

    @Override
    public String getDescription() {
        return "JDownloader's CrunchyRoll Plugin helps download videos and subtitles from crunchyroll.com. Crunchyroll provides a range of qualities, and this plugin will show all those available to you.";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        downloadLink.setProperty("valid", false);
        this.requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_VIDEO_PLAYER)) {
            this.downloadRTMP(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_SUBTITLE)) {
            this.downloadSubs(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_ANDROID)) {
            this.downloadAndroid(downloadLink);
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        downloadLink.setProperty("valid", false);
        this.login(account, this.br, false, true);
        this.requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_VIDEO_PLAYER)) {
            this.downloadRTMP(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_SUBTITLE)) {
            this.downloadSubs(downloadLink);
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_ANDROID)) {
            this.downloadAndroid(downloadLink);
        }
    }

    /**
     * Attempt to log into crunchyroll.com using the given account. Cookies are cached to 'loginCookies'.
     * 
     * @param account
     *            The account to use to log in.
     * @param br
     *            The browser to use to log in. This is the browser where the cookies will be saved.
     * @param refresh
     *            Should new cookies be retrieved (fresh login) even if cookies have previously been cached.
     * @param showDialog
     *            Display warning dialog if login fails.
     */
    public void login(final Account account, Browser br, final boolean refresh, final boolean showDialog) throws Exception {
        synchronized (CrunchyRollCom.lock) {
            if (br == null) {
                br = this.br;
            }
            try {
                this.setBrowserExclusive();
                // Load cookies from the cache if allowed, and they exist
                if (refresh == false && CrunchyRollCom.loginCookies.containsKey(account)) {
                    final HashMap<String, String> cookies = CrunchyRollCom.loginCookies.get(account);
                    if (cookies != null) {
                        if (cookies.containsKey("c_userid")) {
                            // Save cookies to the browser
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                br.setCookie("crunchyroll.com", key, value);
                            }
                            return;
                        }
                    }
                }

                // Set the POST parameters to log in
                final LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
                post.put("formname", "RpcApiUser_Login");
                post.put("next_url", Encoding.urlEncode("http://www.crunchyroll.com/acct/?action=status"));
                post.put("fail_url", Encoding.urlEncode("http://www.crunchyroll.com/login"));
                post.put("name", Encoding.urlEncode(account.getUser()));
                post.put("password", Encoding.urlEncode(account.getPass()));
                post.put("submit", "submit");

                // Load the login page (actually log in)
                br.setFollowRedirects(true);
                br.postPage("https://www.crunchyroll.com/?a=formhandler", post);

                // Check if we successfully logged in
                if (!br.containsHTML("Welcome back, .+?!") && !br.containsHTML("<title>Redirecting\\.\\.\\.</title>")) {
                    // Not successful, display dialog if we were asked to
                    if (showDialog) {
                        UserIO.getInstance().requestMessageDialog(0, "Crunchyroll Login Error", "Account check needed for crunchyroll.com!");
                    }
                    // Set account to invalid and quit
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

                final HashMap<String, String> cookies = new HashMap<String, String>();

                // Get the cookies saved into the browser
                final Cookies cYT = br.getCookies("crunchyroll.com");
                for (final Cookie c : cYT.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                // Save the cookies to the cache
                CrunchyRollCom.loginCookies.put(account, cookies);
            } catch (final PluginException e) {
                CrunchyRollCom.loginCookies.remove(account);
                throw e;
            }
        }
    }

    /**
     * Pad and format version numbers so that the String.compare() method can be used simply. ("9.10.2", ".", 4) would result in
     * "000900100002".
     * 
     * @param version
     *            The version number string to format (e.g. '9.10.2')
     * @param sep
     *            The character(s) to split the numbers by (e.g. '.')
     * @param maxWidth
     *            The number of digits to pad the numbers to (e.g. 5 would make '12' become '00012'). Note that numbers which exceed this
     *            are not truncated.
     * @return The formatted version number ready to be compared
     */
    private String normaliseRtmpVersion(final String version, final String sep, final int maxWidth) {
        final String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        final StringBuilder sb = new StringBuilder();
        for (final String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s).replace(' ', '0'));
        }
        return sb.toString();
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        downloadLink.setProperty("valid", false);

        // Try and find which download type it is
        if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_VIDEO_PLAYER)) {
            // Attempt to login
            if (this.br.getCookies("crunchyroll.com").isEmpty()) {
                final Account account = AccountController.getInstance().getValidAccount(this);
                if (account != null) {
                    try {
                        this.login(account, this.br, false, true);
                    } catch (final Exception e) {
                    }
                }
            }

            // Find matching decrypter
            final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("crunchyroll.com");
            if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Cannot decrypt video link"); }

            // Set the RTMP details (exception on error)
            ((jd.plugins.decrypter.CrhyRllCom) plugin).setRTMP(downloadLink, this.br);
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_SUBTITLE)) {
            // Validate the URL and set filename
            final String subId = new Regex(downloadLink.getDownloadURL(), "subtitle_script_id=([0-9]+)").getMatch(0);
            if (subId == null) { return AvailableStatus.FALSE; }

            if (downloadLink.getStringProperty("filename") == null) {
                final String filename = "CrunchyRoll." + subId;
                downloadLink.setProperty("filename", filename);
                downloadLink.setFinalFileName(filename + ".ass");
            } else if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("filename") + ".ass");
            }

            // Get the HTTP response headers of the XML file to check for
            // validity
            URLConnectionAdapter conn = null;
            try {
                conn = this.br.openGetConnection(downloadLink.getDownloadURL());
                final long respCode = conn.getResponseCode();
                final long length = conn.getLongContentLength();
                final String contType = conn.getContentType();
                if (respCode == 200 && contType.endsWith("xml")) {
                    // Check if the file is too small to be subtitles
                    // 20130719 length isn't given anymore so will equal -1
                    if (length != -1 && length < 200) { return AvailableStatus.FALSE; }

                    // File valid, set details
                    downloadLink.setDownloadSize(length);
                    downloadLink.setProperty("valid", true);
                }
            } finally {
                try {
                    conn.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else if (downloadLink.getDownloadURL().contains(CrunchyRollCom.RCP_API_ANDROID)) {
            // Find matching decrypter
            final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("crunchyroll.com");
            if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Cannot decrypt video link"); }

            // Set the Android video details (exception on error)
            ((jd.plugins.decrypter.CrhyRllCom) plugin).setAndroid(downloadLink, this.br);
        }
        if ((Boolean) downloadLink.getProperty("valid", false)) { return AvailableStatus.TRUE; }
        return AvailableStatus.FALSE;
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

    /**
     * Generate the AES decryption key based on the subtitle's id using some obfuscation and SHA-1 hashing.
     * 
     * @param id
     *            The id of the subtitles to generate the key for
     * @param size
     *            The number of bytes to make the key (e.g. 32 bytes for 256-bit key)
     * @return The byte formatted key to be used in AES decryption
     */
    private byte[] subsGenerateKey(final int id, final int size) throws NoSuchAlgorithmException {
        // Generate fibonacci salt
        String magicStr = "";
        int fibA = 1;
        int fibB = 2;
        for (int i = 2; i < 22; i++) {
            final int newChr = fibA + fibB;
            fibA = fibB;
            fibB = newChr;
            magicStr += Character.toString((char) (newChr % 97 + 33));
        }

        // Calculate magic number
        final int magic1 = (int) Math.floor(Math.sqrt(6.9) * Math.pow(2, 25));
        final long magic2 = id ^ magic1 ^ (id ^ magic1) >>> 3 ^ (magic1 ^ id) * 32l;

        magicStr += magic2;

        // Calculate the hash using SHA-1
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        final byte[] magicBytes = magicStr.getBytes();
        md.update(magicBytes, 0, magicBytes.length);
        final byte[] hashBytes = md.digest();

        // Create the key using the given length
        final byte[] key = new byte[size];
        Arrays.fill(key, (byte) 0);

        for (int i = 0; i < key.length && i < hashBytes.length; i++) {
            key[i] = hashBytes[i];
        }
        return key;
    }
}
