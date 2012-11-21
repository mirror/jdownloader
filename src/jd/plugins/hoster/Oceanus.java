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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@HostPlugin(revision = "$Revision: 18910 $", interfaceVersion = 2, names = { "oceanus.ch" }, urls = { "oceanus://\\d+,\\d+,[0-9a-zA-Z\\-]*" }, flags = { 2 })
public class Oceanus extends PluginForHost {

    private final String           DOWNLOAD_PREPARE_MSG  = "Preparing to download...";
    private final String           CON_DL_REQ            = "MaxConcurrentDownloadsReq";
    private final String           LOGIN_REQ             = "loginReq";
    private final String           STATUS                = "Status";
    private final String           FREE_CNT              = "freeCount";
    private final String           PREM_CNT              = "premiumCount";
    public final String            DOWNLOAD_INVALID      = "DownloadInvalid";
    private final String           DOWN_INVALID_MSG      = "InvalidDownload";
    private final String           REG_FREE_USER         = "Registered (free) User";
    private final String           PREM_USER             = "Premium User";
    private final String           PREP_DOWN_ERR         = "PrepareDownloadError";
    private final String           PREP_DOWN             = "PrepareDownload";
    private final String           LNK_INVALID           = "link_invalid";
    private final String           PRE_DOWN_REQ          = "PrepareDownloadReq";
    private final String           UPLOAD_ID_LST         = "uploadIDList";
    private final String           CON_REQ               = "connectReq";
    private final String           VERIFY_CAPTCHA        = "verifyCaptcha";
    private final String           CAPTCHA_VERIFIED      = "captchaVerified";
    private final String           WAIT_TIME             = "waitTime";
    private final String           USER_NAME             = "UserName";
    private final String           PASSWORD              = "Password";
    private final String           USERID                = "userID";
    private final String           AVAIL_BYTES           = "availableBytes";
    private final String           PREM_EXPIRE           = "PremiumExpires";
    private final String           OK_STR                = "OK";
    private final String           HTTP                  = "http://";
    private final String           SERVER_ADDR           = ":8080/OceanusManagementSystemServerWeb/jd?action=";
    private final String           OCEANUS_PREM_LNK      = "http://www.oceanus.ch/premiumcustomer.html";

    private AtomicInteger          CON_FREE_DL           = new AtomicInteger(1);
    private AtomicInteger          CON_PREM_DL           = new AtomicInteger(5);
    private AtomicBoolean          CON_DL_UPDATED        = new AtomicBoolean(false);
    public final String            DEFAULT_RELAY_NODES[] = new String[] { "http://001webhoster.com/relay.xml", "http://wp11077166.wp374.webpack.hosteurope.de/relay.xml" };

    private static StringContainer SERVERIP              = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    public Oceanus(PluginWrapper wrapper) throws PluginException {
        super(wrapper);
        this.enablePremium(OCEANUS_PREM_LNK);
    }

    private String getSERVERIP() throws Exception {
        if (!StringUtils.isEmpty(SERVERIP.string)) return SERVERIP.string;
        synchronized (SERVERIP) {
            if (!StringUtils.isEmpty(SERVERIP.string)) return SERVERIP.string;
            for (ManagementSystemEntry ms : new OceanusResolver(DEFAULT_RELAY_NODES).getManagementSystems()) {
                try {
                    if (sendConnectReq(ms.getIpAddress())) {
                        SERVERIP.string = ms.getIpAddress();
                        return SERVERIP.string;
                    }
                } catch (final Throwable e) {
                    LogSource.exception(logger, e);
                }
            }
            throw new WTFException("Could not find valid ServerIP");
        }
    }

    private void getMaxConcurrentDownloads() throws Exception {
        if (CON_DL_UPDATED.get() == true) return;
        synchronized (CON_DL_UPDATED) {
            if (CON_DL_UPDATED.get() == true) return;
            try {
                Map<String, Object> concurrentDownResp = null;
                concurrentDownResp = sendPostRequest(CON_DL_REQ, null, null);
                if (concurrentDownResp != null) {
                    CON_FREE_DL.set(Integer.parseInt((String) concurrentDownResp.get(FREE_CNT)));
                    CON_PREM_DL.set(Integer.parseInt((String) concurrentDownResp.get(PREM_CNT)));
                }
            } catch (final Throwable e) {
                /* this should not break plugin! */
                LogSource.exception(logger, e);
            } finally {
                CON_DL_UPDATED.set(true);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        Map<String, Object> respObj, reqObj;
        AccountInfo accInfo = new AccountInfo();
        String premiumDateStr = null;
        reqObj = new HashMap<String, Object>();
        reqObj.put(USER_NAME, account.getUser());
        reqObj.put(PASSWORD, Hash.getMD5(account.getPass()));
        respObj = sendPostRequest("accFetchReq", JSonStorage.toString(reqObj), null);
        if (Boolean.TRUE.equals(respObj.get(STATUS))) {
            account.setValid(true);
            accInfo.setTrafficLeft((Long) respObj.get(AVAIL_BYTES));
            premiumDateStr = (String) respObj.get(PREM_EXPIRE);
            if (StringUtils.isEmpty(premiumDateStr)) {
                accInfo.setStatus(REG_FREE_USER);
            } else {
                accInfo.setStatus(PREM_USER);
                accInfo.setValidUntil(TimeFormatter.getMilliSeconds(premiumDateStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        return accInfo;
    }

    /**
     * login with an account
     * 
     * @param account
     * @return accInfo
     * @throws PluginException
     */
    private Long doLogin(Account account) throws Exception {
        Map<String, Object> respObj, reqObj;
        reqObj = new HashMap<String, Object>();
        reqObj.put(USER_NAME, account.getUser());
        reqObj.put(PASSWORD, Hash.getMD5(account.getPass()));
        respObj = sendPostRequest(LOGIN_REQ, JSonStorage.toString(reqObj), null);
        if (Boolean.TRUE.equals(respObj.get(STATUS))) {
            return (Long) respObj.get("UserID");
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return CON_FREE_DL.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return CON_PREM_DL.get();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        try {
            dl = prepareDownload(downloadLink);
            dl.startDownload();
        } finally {
            try {
                sendPostRequest("removeConnectReq", null, null);
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
        }
    }

    @Override
    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {

        getMaxConcurrentDownloads();
        super.handle(downloadLink, account);

    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        try {
            Long userID = doLogin(account);
            Downloader downloader = prepareDownload(downloadLink);
            downloader.setUserID(userID);
            dl = downloader;
            dl.startDownload();
        } finally {
            try {
                sendPostRequest("removeConnectReq", null, null);
            } catch (final Throwable e) {
                LogSource.exception(logger, e);
            }
        }
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /**
     * send connect request to server.
     * 
     * @param msIP
     * @return boolean
     * @throws Exception
     */
    private boolean sendConnectReq(String msIP) throws Exception {
        Map<String, Object> resp = sendPostRequest(CON_REQ, null, msIP);
        return OK_STR.equals(resp.get(STATUS));
    }

    public long getUploadID(DownloadLink link) {
        return Long.parseLong(new Regex(link.getDownloadURL(), "oceanus://(\\d+)").getMatch(0));
    }

    public long getFileID(DownloadLink link) {
        return Long.parseLong(new Regex(link.getDownloadURL(), "oceanus://\\d+,(\\d+)").getMatch(0));
    }

    public String getDecryptionKey(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "oceanus://\\d+,\\d+,([0-9a-zA-Z\\-]+)").getMatch(0);
    }

    /**
     * method to get downloadXml,parse it and to update downloader and download link(filename,filesize etc).
     * 
     * 
     * @param downloadLink
     * @return Downloader
     * @throws PluginException
     */
    private Downloader prepareDownload(DownloadLink downloadLink) throws Exception {
        String downloadXML = sendDownloadRequest(getUploadID(downloadLink));
        if (DOWNLOAD_INVALID.equals(downloadXML)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (!StringUtils.isEmpty(downloadXML)) {
            Downloader downloader = parseXML(downloadXML);
            dl = downloader;
            long fileID = getFileID(downloadLink);
            for (OCFile file : downloader.getFileList()) {
                if (file.getFileId() == fileID) {
                    if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(file.getName());
                    downloadLink.setDownloadSize(file.getSize());
                    downloadLink.setAvailable(true);
                    downloader.setDownloadLink(downloadLink);
                    downloader.setOceanus(this);
                    return downloader;
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, LNK_INVALID);
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, DOWN_INVALID_MSG);
    }

    /**
     * parse the xml string to form downloader.
     * 
     * @param downloadXMLStr
     * @return Downloader
     * @throws Exception
     */
    public Downloader parseXML(String downloadXMLStr) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Downloader.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        // unMarshall the input stream to downloader class
        return (Downloader) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(downloadXMLStr.getBytes("UTF-8")));
    }

    /**
     * sends download request.
     * 
     * @param oceanusLink
     * @return downloadXml
     * @throws Exception
     */
    public String sendDownloadRequest(Long uploadID) throws Exception {
        Map<String, Object> response = sendPrepareDownloadReq(uploadID);
        if (!StringUtils.isEmpty((String) response.get(PREP_DOWN_ERR))) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, (String) response.get(PREP_DOWN_ERR)); }
        return ((ArrayList<?>) response.get(PREP_DOWN)).get(0).toString();
    }

    /**
     * method to get the download xml;
     * 
     * @param uploadIDList
     * @return JSONObject
     * @throws Exception
     */
    private Map<String, Object> sendPrepareDownloadReq(Long uploadID) throws Exception {
        Map<String, Object> jsObj = new HashMap<String, Object>();
        ArrayList<Long> uploadIDList = new ArrayList<Long>();
        uploadIDList.add(uploadID);
        jsObj.put(UPLOAD_ID_LST, uploadIDList);
        return sendPostRequest(PRE_DOWN_REQ, JSonStorage.toString(jsObj), null);
    }

    /**
     * handles captcha for free users.
     * 
     * @param downloadLink
     * @param captchaKey
     * @param uploadID
     * @param userID
     * @throws Exception
     */
    public void handleCaptcha(DownloadLink downloadLink, String captchaKey, long uploadID, long userID) throws Exception {
        int waitTime;
        Recaptcha recaptcha = new Recaptcha(br);
        Map<String, Object> resp;
        final String id = captchaKey;
        recaptcha.setId(id);
        recaptcha.load();
        final File captchaFile = recaptcha.downloadCaptcha(this.getLocalCaptchaFile());
        final String captchaCode = this.getCaptchaCode(captchaFile, downloadLink);
        resp = recaptcha.verifyCaptcha(captchaCode, uploadID, userID);
        if (!(Boolean) resp.get(CAPTCHA_VERIFIED)) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            waitTime = Integer.parseInt((String) resp.get(WAIT_TIME));
            sleep(waitTime * 1000, downloadLink);
        }

    }

    /**
     * sends post requests to server with parameters.
     * 
     * @param mode
     * @param post
     * @param msIP
     * @return JSONObject
     * @throws Exception
     */
    public Map<String, Object> sendPostRequest(String action, String post, String msIP) throws Exception {
        URLConnectionAdapter connection = null;
        try {
            String msUrl = null;
            if (!StringUtils.isEmpty(msIP)) {
                msUrl = HTTP + msIP + SERVER_ADDR + action;
            } else {
                msUrl = HTTP + getSERVERIP() + SERVER_ADDR + action;
            }
            // Create Post-Connection
            PostRequest postRequest = new PostRequest(msUrl);
            postRequest.getHeaders().put("Content-Type", "application/json");
            postRequest.setPostDataString(post);
            connection = br.openRequestConnection(postRequest);
            return JSonStorage.restoreFromString(br.loadConnection(connection).getHtmlCode(), new TypeRef<HashMap<String, Object>>() {
            }, new HashMap<String, Object>());
        } finally {
            try {
                connection.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * Class Recaptcha
     * 
     * @author Arunkumar load captcha and verify.
     * 
     */
    public class Recaptcha {
        private final Browser br;
        private String        challenge;
        private String        server;
        private String        captchaAddress;
        private String        id;
        private Browser       rcBr;
        private int           tries = 0;

        public Recaptcha(final Browser br) {
            this.br = br;
        }

        /**
         * download captcha
         * 
         * @param captchaFile
         *            - captchaFile
         * @return captchaFile
         * @throws IOException
         * @throws PluginException
         */
        private File downloadCaptcha(final File captchaFile) throws IOException, PluginException {
            /* follow redirect needed as google redirects to another domain */
            if (this.getTries() > 0) {
                this.reload();
            }
            this.rcBr.setFollowRedirects(true);
            try {
                Browser.download(captchaFile, this.rcBr.openGetConnection(this.captchaAddress));
            } catch (IOException e) {
                captchaFile.delete();
                throw e;
            }
            return captchaFile;
        }

        /**
         * load Captcha
         * 
         * @throws IOException
         * @throws PluginException
         */
        private void load() throws IOException, PluginException {
            this.rcBr = this.br.cloneBrowser();
            /* follow redirect needed as google redirects to another domain */
            this.rcBr.setFollowRedirects(true);
            this.rcBr.getPage("http://api.recaptcha.net/challenge?k=" + this.id);
            this.challenge = this.rcBr.getRegex("challenge.*?:.*?'(.*?)',").getMatch(0);
            this.server = this.rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(0);
            if (this.challenge == null || this.server == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.captchaAddress = this.server + "image?c=" + this.challenge;
        }

        /**
         * reload Captcha
         * 
         * @throws IOException
         * @throws PluginException
         */
        private void reload() throws IOException, PluginException {
            this.rcBr.getPage("http://www.google.com/recaptcha/api/reload?c=" + this.challenge + "&k=" + this.id + "&reason=r&type=image&lang=en");
            this.challenge = this.rcBr.getRegex("Recaptcha\\.finish\\_reload\\(\\'(.*?)\\'\\, \\'image\\'\\)\\;").getMatch(0);

            if (this.challenge == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.captchaAddress = this.server + "image?c=" + this.challenge;
        }

        /**
         * verify Captcha
         * 
         * @param code
         * @return
         * @throws Exception
         */
        private Map<String, Object> verifyCaptcha(final String code, final long uploadID, final long userID) throws PluginException {
            Map<String, Object> respObj, jsObj;
            try {
                if (this.challenge == null || code == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                jsObj = new HashMap<String, Object>();
                jsObj.put("userID", userID);
                jsObj.put("uploadID", uploadID);
                jsObj.put("challenge", challenge);
                jsObj.put("response", code);
                respObj = sendPostRequest(VERIFY_CAPTCHA, JSonStorage.toString(jsObj), null);
                this.tries++;
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return respObj;
        }

        private void setId(final String id) {
            this.id = id;
        }

        private int getTries() {
            return this.tries;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        String downloadXML = sendDownloadRequest(getUploadID(downloadLink));
        if (DOWNLOAD_INVALID.equals(downloadXML)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (!StringUtils.isEmpty(downloadXML)) {
            Downloader downloader = parseXML(downloadXML);
            long fileID = getFileID(downloadLink);
            for (OCFile file : downloader.getFileList()) {
                if (file.getFileId() == fileID) {
                    if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(file.getName());
                    downloadLink.setDownloadSize(file.getSize());
                    downloadLink.setAvailable(true);
                    return AvailableStatus.TRUE;
                }
            }
            return AvailableStatus.FALSE;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @XmlRootElement(name = "PrepareDownload")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Downloader extends DownloadInterface {

        private static final String               ENCODING_DICTIONARY = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz023456789";

        @XmlAttribute(name = "scrambledPW")
        private String                            encryptionPwd;
        @XmlAttribute(name = "uploadid")
        private long                              uploadId;
        @XmlAttribute(name = "oceanuslink")
        public String                             oceanusLink;
        @XmlElement(name = "File")
        private List<OCFile>                      fileList;
        @XmlAnyElement(lax = true)
        private DownloadLink                      downloadLink;
        @XmlAnyElement(lax = true)
        private Oceanus                           oceanus;
        private long                              userID              = -1;
        private AtomicLong                        liveBytesLoaded     = new AtomicLong(0);
        private AtomicBoolean                     externalAbort       = new AtomicBoolean(false);
        @XmlAnyElement(lax = true)
        private ManagedThrottledConnectionHandler manager             = null;
        private long                              startTimeStamp      = -1;
        @XmlAnyElement(lax = true)
        private MeteredThrottledInputStream       meteredStream       = null;

        public Downloader() {
            fileList = new ArrayList<OCFile>();
        }

        public List<OCFile> getFileList() {
            return fileList;
        }

        public void setFileList(List<OCFile> fileList) {
            this.fileList = fileList;
        }

        public long getUploadID() {
            return uploadId;
        }

        public void setUploadID(long uploadId) {
            this.uploadId = uploadId;
        }

        public String getEncryptionPwd() {
            return encryptionPwd;
        }

        public void setEncryptionPwd(String encryptionPwd) {
            this.encryptionPwd = encryptionPwd;
        }

        public String getOceanusLink() {
            return oceanusLink;
        }

        public void setOceanusLink(String oceanusLink) {
            this.oceanusLink = oceanusLink;
        }

        public void setDownloadLink(DownloadLink downloadLink) {
            this.downloadLink = downloadLink;
            this.manager = new ManagedThrottledConnectionHandler(this.downloadLink);
            downloadLink.setDownloadInstance(this);
        }

        public void setOceanus(Oceanus oceanus) {
            this.oceanus = oceanus;
        }

        /**
         * loops through the files and starts download.
         * 
         * @throws Exception
         */
        public boolean startDownload() throws Exception {
            startTimeStamp = System.currentTimeMillis();
            RandomAccessFile raf = null;
            File outputCompleteFile = null;
            File partCompleteFile = null;
            try {
                downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
                try {
                    downloadLink.getDownloadLinkController().getConnectionHandler().addConnectionHandler(this.getManagedConnetionHandler());
                } catch (final Throwable e) {
                }
                long fileID = oceanus.getFileID(downloadLink);
                long position = 0;
                for (OCFile file : getFileList()) {
                    if (file.getFileId() == fileID) {
                        outputCompleteFile = new File(downloadLink.getFileOutput());
                        partCompleteFile = new File(outputCompleteFile.getAbsolutePath() + ".part");
                        if (!outputCompleteFile.getParentFile().exists()) {
                            if (outputCompleteFile.getParentFile().mkdirs() == false) { throw new IOException("Could not create " + outputCompleteFile); }
                        }
                        raf = new RandomAccessFile(partCompleteFile, "rw");
                        ArrayList<OceanusChunk> chunks = new ArrayList<OceanusChunk>(file.getChunks());
                        /* sort offsets from start till end */
                        Collections.sort(chunks, new Comparator<OceanusChunk>() {

                            @Override
                            public int compare(OceanusChunk o1, OceanusChunk o2) {
                                if (o1.getOffset() < o2.getOffset()) return -1;
                                if (o1.getOffset() > o2.getOffset()) return 1;
                                return 0;
                            }
                        });
                        for (OceanusChunk chunk : chunks) {
                            if (externalDownloadStop()) return false;
                            for (int index = 0; index < chunk.getCdnNodes().size(); index++) {
                                CDNNode cdnNode = chunk.getCdnNodes().get(index);
                                boolean lastCDNNode = (index == chunk.getCdnNodes().size() - 1);
                                position = raf.getFilePointer();
                                if (raf.length() >= chunk.getOffset() + chunk.getSize()) {
                                    chunk.setChunkFinished(true);
                                    raf.seek(chunk.getOffset() + chunk.getSize());
                                    break;
                                }
                                try {
                                    liveBytesLoaded.set(chunk.getOffset());
                                    if (beginDownload(file, chunk, cdnNode, raf, lastCDNNode) && "SUCCESS".equals(chunk.getStatus())) {
                                        downloadLink.setDownloadCurrent(position);
                                        chunk.setChunkFinished(true);
                                        break;
                                    }
                                } catch (final Exception e) {
                                    if (e instanceof PluginException) {
                                        PluginException pe = (PluginException) e;
                                        switch (pe.getLinkStatus()) {
                                        case LinkStatus.ERROR_PREMIUM:
                                        case LinkStatus.ERROR_IP_BLOCKED:
                                        case LinkStatus.ERROR_FATAL:
                                        case LinkStatus.ERROR_CAPTCHA:
                                            throw pe;
                                        }
                                    }
                                    if (e instanceof InterruptedException) {
                                        if (externalDownloadStop()) return false;
                                        throw (InterruptedException) e;
                                    }
                                    if (lastCDNNode) throw e;
                                    raf.seek(position);
                                }
                            }
                        }
                        if (externalDownloadStop()) {
                            return false;
                        } else {
                            for (OceanusChunk chunk : file.getChunks()) {
                                if (chunk.isChunkFinished() == false) return false;
                            }
                            try {
                                raf.close();
                            } catch (final Throwable e) {
                            }
                            if (partCompleteFile.renameTo(outputCompleteFile) == false) { throw new IOException("Could not rename " + partCompleteFile + " to " + outputCompleteFile); }
                            downloadLink.getLinkStatus().setStatusText(null);
                            downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
                            try {
                                Map<String, Object> jsonReq = new HashMap<String, Object>();
                                jsonReq.put("userID", userID);
                                jsonReq.put("uploadID", uploadId);
                                oceanus.sendPostRequest("wholeDownloadComplete", JSonStorage.toString(jsonReq), null);
                            } catch (final Throwable e) {
                                LogSource.exception(oceanus.getLogger(), e);
                            }
                            return true;
                        }
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } finally {
                try {
                    manager.removeThrottledConnection(meteredStream);
                } catch (final Throwable e) {
                }
                try {
                    raf.close();
                } catch (final Throwable e) {
                }
                try {
                    downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(this.getManagedConnetionHandler());
                } catch (final Throwable e) {
                }
                downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            }
        }

        /**
         * method to download individual chunks within a file.
         * 
         * @param file
         * @param chunk
         * @param cdnNodeID
         * @throws Exception
         */
        private boolean beginDownload(OCFile file, OceanusChunk chunk, CDNNode cdnNode, RandomAccessFile raf, boolean lastCDNNode) throws Exception {
            String captchaKey = sendDownloadRequest(chunk, cdnNode.getCdnNodeID());
            if (!StringUtils.isEmpty(captchaKey)) {
                oceanus.handleCaptcha(downloadLink, captchaKey, uploadId, userID);
                sendDownloadRequest(chunk, cdnNode.getCdnNodeID());
            }
            if ("OK".equals(chunk.getStatus())) {
                return download(file, chunk, raf);
            } else {
                if (lastCDNNode) calculateNextTryTime(chunk, chunk.getStatus());
                return false;
            }
        }

        /**
         * sends start download request to server and returns captcha key if any exists.
         * 
         * @param chunk
         * @param cdnNodeId
         * @return
         * @throws Exception
         */
        private String sendDownloadRequest(OceanusChunk chunk, long cdnNodeId) throws Exception {
            boolean dwnLimitExceeded;
            String chunkStatus;
            Map<String, Object> respObj, jsObj;
            String captchaKey = null;
            jsObj = new HashMap<String, Object>();
            jsObj.put("uploadID", uploadId);
            jsObj.put("chunkID", chunk.getChunkID());
            jsObj.put("cdnNodeID", cdnNodeId);
            jsObj.put("userID", userID);
            respObj = oceanus.sendPostRequest("startDownloadReq", JSonStorage.toString(jsObj), null);
            // process using reply message from MS
            if (respObj != null) {
                chunk.setDownloadID((Long) respObj.get("downloadID"));
                dwnLimitExceeded = (Boolean) respObj.get("DownloadLimitExceeded");
                chunkStatus = (String) respObj.get("Status");
                if (dwnLimitExceeded) {
                    chunk.setStatus("download limit exceeded");
                    if (userID != -1) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 600000);
                    }
                }
                chunk.setDownloadURL((String) respObj.get("downloadURL"));
                chunk.setStatus(chunkStatus);
                if ("Captcha Not Solved".equals(chunkStatus)) {
                    captchaKey = (String) respObj.get("captchaKey");
                }
            }
            return captchaKey;
        }

        /**
         * downloads chunk.
         * 
         * @param file
         * @param chunk
         * @throws Exception
         */
        private boolean download(OCFile file, OceanusChunk chunk, RandomAccessFile raf) throws Exception {
            URLConnectionAdapter urlConnection = null;
            try {
                urlConnection = oceanus.br.cloneBrowser().openGetConnection(chunk.getDownloadURL());
                if (meteredStream != null) {
                    meteredStream.setInputStream(urlConnection.getInputStream());
                } else {
                    meteredStream = new MeteredThrottledInputStream(urlConnection.getInputStream(), new AverageSpeedMeter(10));
                    manager.addThrottledConnection(meteredStream);
                }
                readChunk(chunk, meteredStream, raf);
                return handleCDNResponse(file, chunk, urlConnection.getResponseCode(), meteredStream);
            } finally {
                try {
                    urlConnection.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        /**
         * reads stream from the connection and decrypt it.
         * 
         * @param chunk
         * @param inputStream
         * @param fileOutputStream
         * @throws Exception
         */
        private void readChunk(OceanusChunk chunk, InputStream inputStream, RandomAccessFile raf) throws Exception {
            int bytesRead;
            byte[] buf = new byte[32767];
            String decryptionKeyStr = oceanus.getDecryptionKey(downloadLink);
            InputStream cipherInputStream = getDecryptionInputStream(inputStream, decryptionKeyStr);
            while ((bytesRead = cipherInputStream.read(buf)) != -1) {
                if (externalDownloadStop()) throw new InterruptedException("ExternalStop");
                if (bytesRead > 0) {
                    raf.write(buf, 0, bytesRead);
                    liveBytesLoaded.addAndGet(bytesRead);
                }
            }
        }

        /**
         * handles response from Content Delivery Network.
         * 
         * @param file
         * @param chunk
         * @param responseCode
         * @throws Exception
         */
        private boolean handleCDNResponse(OCFile file, OceanusChunk chunk, int responseCode, MeteredThrottledInputStream inputStream) throws Exception {
            if (responseCode == 200) {
                sendDownloadCompleted(chunk, inputStream);
                chunk.setStatus("SUCCESS");
                return true;
            } else if (responseCode == 500) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Download is not approved");
            } else {
                calculateNextTryTime(chunk, "Other Error");
                return false;
            }
        }

        /**
         * sends chunk completed message to server.
         * 
         * @param chunk
         * @throws Exception
         */
        private void sendDownloadCompleted(OceanusChunk chunk, MeteredThrottledInputStream inputStream) throws Exception {
            Map<String, Object> reqObj = new HashMap<String, Object>();
            reqObj.put("userID", userID);
            reqObj.put("downloadID", chunk.getDownloadID());
            reqObj.put("bytesTransferred", inputStream.transfered());
            oceanus.sendPostRequest("DownloadCompleted", JSonStorage.toString(reqObj), null);
        }

        /**
         * calculate next try time for a chunk.
         * 
         * @param chunk
         * @param status
         */
        private void calculateNextTryTime(OceanusChunk chunk, String status) throws PluginException, InterruptedException {
            if ("Temporarily not available".equals(status)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 180000l);
            } else if ("OK".equals(status)) {
                return;
            } else if ("download limit exceeded".equals(status)) {
                if (userID != -1) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 600000);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 90000l);
            }
        }

        /**
         * adds decryption cipher to the inputstream.
         * 
         * @param originalInputStream
         * @param encryptionKey
         * @return
         * @throws Exception
         */
        private InputStream getDecryptionInputStream(InputStream originalInputStream, String encryptionKey) throws Exception {
            SecretKeySpec key = new SecretKeySpec(decodeArbitrary(encryptionKey, 128), "AES");
            Cipher cp = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cp.init(Cipher.DECRYPT_MODE, key);
            return new CipherInputStream(originalInputStream, cp);
        }

        /**
         * decode the string to get the decryption key.
         * 
         * @param toDecode
         * @param numBits
         * @return
         * @throws Exception
         */
        private byte[] decodeArbitrary(String toDecode, int numBits) throws Exception {
            int encodingSize = (int) Math.ceil(numBits / (Math.log(ENCODING_DICTIONARY.length()) / Math.log(2)));
            if (toDecode.length() > encodingSize) throw new Exception("Passed string '" + toDecode + "' has " + "an invalid length.");

            int skip = encodingSize - toDecode.length();
            BigInteger bi = BigInteger.valueOf(0);
            BigInteger encodingDictionaryLength = BigInteger.valueOf(ENCODING_DICTIONARY.length());
            for (int i = 0; i < encodingSize; i++) {
                bi = bi.multiply(encodingDictionaryLength);

                if (i < skip) continue;
                int idx = ENCODING_DICTIONARY.indexOf(toDecode.charAt(i - skip));
                if (idx == -1) throw new Exception("Passed string '" + toDecode + "' contains illegal " + "characters");
                bi = bi.add(BigInteger.valueOf(idx));
            }

            byte[] out = new byte[(int) (Math.ceil(numBits / 8))];
            byte[] decoded = bi.toByteArray();
            if (decoded.length == out.length + 1 && decoded[0] == 0) {
                // For some reason, biginteger sometimes returns a bytearray
                // with a single 0-valued byte
                System.arraycopy(decoded, 1, out, 0, out.length);
            } else {
                if (decoded.length > out.length) throw new Exception("Passed value equals more than " + numBits + " bits");
                System.arraycopy(decoded, 0, out, out.length - decoded.length, decoded.length);
            }
            return out;
        }

        public void setUserID(long userID) {
            this.userID = userID;
        }

        @Override
        public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
            return manager;
        }

        @Override
        public URLConnectionAdapter connect(Browser br) throws Exception {
            throw new WTFException("Not implemented");
        }

        @Override
        public long getTotalLinkBytesLoadedLive() {
            return liveBytesLoaded.get();
        }

        @Override
        public boolean isResumable() {
            return false;
        }

        @Override
        public URLConnectionAdapter getConnection() {
            throw new WTFException("Not implemented");
        }

        @Override
        public void stopDownload() {
            externalAbort.set(true);
        }

        @Override
        public boolean externalDownloadStop() {
            return externalAbort.get();
        }

        @Override
        public long getStartTimeStamp() {
            return startTimeStamp;
        }

        @Override
        public void close() {
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CDNNode {

        @XmlAttribute(name = "cdnnodeid")
        private long cdnNodeID;

        public long getCdnNodeID() {
            return cdnNodeID;
        }

        public void setCdnNodeID(long cdnNodeID) {
            this.cdnNodeID = cdnNodeID;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OceanusChunk {

        private long          uploadID, downloadID;
        private String        downloadURL;
        @XmlAttribute(name = "chunkid")
        private Long          chunkId;
        @XmlElement(name = "offset")
        private long          offset;
        @XmlElement(name = "Size")
        private long          size;
        @XmlElement(name = "Status")
        private String        status;
        @XmlElementWrapper(name = "CDNNodes")
        @XmlElement(name = "CDNNode")
        private List<CDNNode> cdnNodes;
        private boolean       chunkFinished = false;

        /**
         * no-arg constructor is necessary for JAXB unmarshall
         */
        public OceanusChunk() {
            cdnNodes = new ArrayList<CDNNode>();
        }

        public OceanusChunk(long uploadID, long chunkID, long chunkSize, long offset) {
            this.uploadID = uploadID;
            this.chunkId = chunkID;
            this.size = chunkSize;
            this.offset = offset;
        }

        public long getUploadID() {
            return uploadID;
        }

        public Long getChunkID() {
            return chunkId;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<CDNNode> getCdnNodes() {
            return cdnNodes;
        }

        public void setCdnNodes(List<CDNNode> cdnNodes) {
            this.cdnNodes = cdnNodes;
        }

        public long getDownloadID() {
            return downloadID;
        }

        public void setDownloadID(long downloadID) {
            this.downloadID = downloadID;
        }

        public String getDownloadURL() {
            return downloadURL;
        }

        public void setDownloadURL(String downloadURL) {
            this.downloadURL = downloadURL;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        /**
         * addCDNToList-adds cdn to cdn list
         * 
         * @param cdn
         *            -chunk object to add
         */
        public void addCDNToList(CDNNode cdn) {
            cdnNodes.add(cdn);
        }

        /**
         * @return the chunkFinished
         */
        public boolean isChunkFinished() {
            return chunkFinished;
        }

        /**
         * @param chunkFinished
         *            the chunkFinished to set
         */
        public void setChunkFinished(boolean chunkFinished) {
            this.chunkFinished = chunkFinished;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OCFile {
        @XmlAttribute(name = "fileID")
        private long                    fileId;
        @XmlElement(name = "FileName")
        private String                  name;
        @XmlElement(name = "FilePath")
        private String                  path;
        @XmlElementWrapper(name = "Chunks")
        @XmlElement(name = "Chunk")
        private ArrayList<OceanusChunk> chunks = new ArrayList<OceanusChunk>();

        public OCFile() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setFilePath(String filePath) {
            this.path = filePath;
        }

        public ArrayList<OceanusChunk> getChunks() {
            return chunks;
        }

        public void setChunks(ArrayList<OceanusChunk> chunks) {
            this.chunks = chunks;
        }

        public long getFileId() {
            return fileId;
        }

        public void setFileId(long fileId) {
            this.fileId = fileId;
        }

        public long getSize() {
            long size = 0;
            for (OceanusChunk chunk : chunks) {
                size += chunk.getSize();
            }
            return size;
        }
    }

    public class ManagementSystemEntry {

        private String ipAddress;
        private int    sslPort;

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public int getSslPort() {
            return sslPort;
        }

        public void setSslPort(int sslPort) {
            this.sslPort = sslPort;
        }
    }

    public class OceanusResolver {

        private SingleRelayProcessor winner;

        public OceanusResolver(String[] relayNodes) throws PluginException {
            List<String> randomNodes = Arrays.asList(relayNodes);
            Collections.shuffle(randomNodes);

            ArrayList<SingleRelayProcessor> list = new ArrayList<SingleRelayProcessor>(randomNodes.size());
            for (String node : randomNodes) {
                try {
                    SingleRelayProcessor sp = new SingleRelayProcessor(node);
                    list.add(sp);
                    Collections.sort(list);
                    if (list.size() >= 2 && list.get(0).getTimestamp() == list.get(1).getTimestamp()) {
                        winner = sp;
                        break;
                    }
                } catch (Throwable e) {
                    LogSource.exception(logger, e);
                    continue;
                }
            }
            if (list.size() == 0) { throw new WTFException("No valid relay document found in any of the nodes"); }
            winner = list.get(0);
        }

        public ManagementSystemEntry[] getManagementSystems() {
            List<ManagementSystemEntry> entries = Arrays.asList(winner.getManagementSystems());
            Collections.shuffle(entries);
            return entries.toArray(new ManagementSystemEntry[entries.size()]);
        }
    }

    public class SingleRelayProcessor implements Comparable<SingleRelayProcessor> {

        private static final String xpathTimeStamp = "/oceanusxmlrelay/timestamp";
        private static final String xpathRelays    = "/oceanusxmlrelay/relays/relay";
        private static final String xpathMs        = "/oceanusxmlrelay/managementsystems/ms";

        String                      url;
        Document                    doc;

        int                         timestamp;
        String[]                    relayHosts;
        ManagementSystemEntry[]     managementSystems;

        public SingleRelayProcessor(String url) throws IOException, ParserConfigurationException, SAXException {
            this.url = url;
            fetchURL();
            getData();
        }

        private void getData() {
            try {
                NodeList nl = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpathTimeStamp, doc, XPathConstants.NODESET);
                if (nl.getLength() != 1) throw new WTFException("Couldn't find Timestamp element at " + url);
                Element e = (Element) nl.item(0);
                try {
                    timestamp = Integer.parseInt(e.getTextContent());
                } catch (NumberFormatException ex) {
                    throw new WTFException("Invalid timestamp at " + url);
                }

                nl = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpathRelays, doc, XPathConstants.NODESET);
                relayHosts = new String[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    relayHosts[i] = nl.item(i).getTextContent();
                }

                nl = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpathMs, doc, XPathConstants.NODESET);
                managementSystems = new ManagementSystemEntry[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    ManagementSystemEntry ms = new ManagementSystemEntry();
                    Element msElement = (Element) nl.item(i);
                    NodeList nlIp = msElement.getElementsByTagName("ip");
                    NodeList nlSsl = msElement.getElementsByTagName("sslport");
                    if (nlIp.getLength() != 1 || nlSsl.getLength() != 1) { throw new WTFException("Invalid ManagementSystem entry at " + url); }
                    ms.setIpAddress(nlIp.item(0).getTextContent());
                    try {
                        ms.setSslPort(Integer.parseInt(nlSsl.item(0).getTextContent()));
                    } catch (NumberFormatException ex) {
                        throw new WTFException("Invalid sslport entry at " + url);
                    }
                    managementSystems[i] = ms;
                }
            } catch (XPathExpressionException e) {
                throw new WTFException(e.getMessage(), e);
            }
        }

        private void fetchURL() throws IOException, ParserConfigurationException, SAXException {
            URL url = new URL(this.url);
            InputStream is = url.openStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);
        }

        public int getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public String[] getRelayHosts() {
            return relayHosts;
        }

        public void setRelayHosts(String[] relayHosts) {
            this.relayHosts = relayHosts;
        }

        public ManagementSystemEntry[] getManagementSystems() {
            return managementSystems;
        }

        public void setManagementSystems(ManagementSystemEntry[] managementSystems) {
            this.managementSystems = managementSystems;
        }

        @Override
        public int compareTo(SingleRelayProcessor o) {
            SingleRelayProcessor other = (SingleRelayProcessor) o;
            if (other.getTimestamp() < getTimestamp()) return -1;
            if (other.getTimestamp() > getTimestamp()) return 1;
            return 0;
        }
    }

}
