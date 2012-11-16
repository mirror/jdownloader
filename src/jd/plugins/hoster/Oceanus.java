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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@HostPlugin(revision = "$Revision: 18898 $", interfaceVersion = 2, names = { "oceanus.ch" }, urls = { "http://oceanus.ch/u/[0-9a-zA-Z\\-]*/[0-9a-zA-Z\\-]*" }, flags = { 2 })
public class Oceanus extends PluginForHost {

	private static final String DOWNLOAD_PREPARE_MSG = "Preparing to download...";
	private static final String CON_DL_REQ = "MaxConcurrentDownloadsReq";
	private static final String LOGIN_REQ = "loginReq";
	private static final String STATUS = "Status";
	private static final String FREE_CNT = "freeCount";
	private static final String PREM_CNT = "premiumCount";
	private static final String DOWNLOAD_INVALID = "DownloadInvalid";
	private static final String DOWN_INVALID_MSG = "InvalidDownload";
	private static final String REG_FREE_USER = "Registered (free) User";
	private static final String PREM_USER = "Premium User";
	private static final String CHAR_SET = "UTF-8";
	private static final String PREP_DOWN_ERR = "PrepareDownloadError";
	private static final String PREP_DOWN = "PrepareDownload";
	private static final String LNK_INVALID = "link_invalid";
	private static final String PRE_DOWN_REQ = "PrepareDownloadReq";
	private static final String UPLOAD_ID_LST = "uploadIDList";
	private static final String CON_REQ = "connectReq";
	private static final String VERIFY_CAPTCHA = "verifyCaptcha";
	private static final String CAPTCHA_VERIFIED = "captchaVerified";
	private static final String WAIT_TIME = "waitTime";
	private static final String DIGEST_ALGORITHM = "MD5";
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String USER_NAME = "UserName";
	private static final String PASSWORD = "Password";
	private static final String USERID = "userID";
	private static final String AVAIL_BYTES = "availableBytes";
	private static final String PREM_EXPIRE = "PremiumExpires";
	private static final String EMPTY_STR = "";
	private static final String OK_STR = "OK";
	private static final String HTTP = "http://";
	private static final String SERVER_ADDR = ":8080/OceanusManagementSystemServerWeb/jd?action=";
	private static final String OCEANUS_PREM_LNK = "http://www.oceanus.ch/premiumcustomer.html";

	private static final String ENCODING_DICTIONARY = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz023456789";
	private static final int ENCODING_SIZE_64 = (int) Math.ceil(64 / (Math
			.log(ENCODING_DICTIONARY.length()) / Math.log(2))); // 11
	private int CON_FREE_DL = 1;
	private int CON_PREM_DL = 5;
	public static final String DEFAULT_RELAY_NODE = "http://001webhoster.com/relay.xml,http://wp11077166.wp374.webpack.hosteurope.de/relay.xml";

	private static ObjectMapper mapper = new ObjectMapper();
	private static Map<String, Object> reqObj = null;

	public Oceanus(PluginWrapper wrapper) throws PluginException {
		super(wrapper);
		try {
			OCInMemory inMemory = OCInMemory.getInstance();
			Map<String, Object> concurrentDownResp;
			if (inMemory.getMsIP() == null
					|| EMPTY_STR.equals(inMemory.getMsIP())) {
				ManagementSystemEntry[] msList = getMSList();
				String serverIP = getServerIP(msList);
				inMemory.setMsIP(serverIP);
				concurrentDownResp = sendPostRequest(CON_DL_REQ, EMPTY_STR,
						EMPTY_STR);
				if (concurrentDownResp != null) {
					CON_FREE_DL = Integer.parseInt((String) concurrentDownResp
							.get(FREE_CNT));
					CON_PREM_DL = Integer.parseInt((String) concurrentDownResp
							.get(PREM_CNT));
				}
			}
			this.enablePremium(OCEANUS_PREM_LNK);

		} catch (Exception e) {
			throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
		}
	}

	@Override
	public AccountInfo fetchAccountInfo(Account account) throws Exception {
		Map<String, Object> respObj, reqObj;
		AccountInfo accInfo = account.getAccountInfo();
		String premiumDateStr = null;
		if (accInfo == null) {
			accInfo = new AccountInfo();
			account.setAccountInfo(accInfo);
		}
		reqObj = getRequestObject();
		reqObj.put(USER_NAME, account.getUser());
		respObj = sendPostRequest("accFetchReq",
				mapper.writeValueAsString(reqObj), EMPTY_STR);
		if ((Boolean) respObj.get(STATUS)) {
			account.setValid(true);
			accInfo.setTrafficLeft((Long) respObj.get(AVAIL_BYTES));
			premiumDateStr = (String) respObj.get(PREM_EXPIRE);
			if (premiumDateStr.equals(EMPTY_STR)) {
				accInfo.setStatus(REG_FREE_USER);
			} else {
				accInfo.setStatus(PREM_USER);
				accInfo.setValidUntil(stringToDate(premiumDateStr,
						DATE_TIME_FORMAT).getTime());
			}
		} else {
			account.setValid(false);
			throw new PluginException(LinkStatus.ERROR_PREMIUM,
					PluginException.VALUE_ID_PREMIUM_DISABLE);
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
	private void doLogin(Account account) throws Exception {
		Map<String, Object> respObj, reqObj;
		reqObj = getRequestObject();
		reqObj.put(USER_NAME, account.getUser());
		reqObj.put(PASSWORD, encode(account.getPass()));
		respObj = sendPostRequest(LOGIN_REQ, mapper.writeValueAsString(reqObj),
				EMPTY_STR);
		if ((Boolean) respObj.get(STATUS)) {
			account.setProperty(USERID, (Long) respObj.get("UserID"));
			account.setValid(true);
		} else {
			account.setValid(false);
			throw new PluginException(LinkStatus.ERROR_PREMIUM,
					PluginException.VALUE_ID_PREMIUM_DISABLE);
		}
	}

	@Override
	public int getMaxSimultanFreeDownloadNum() {
		return CON_FREE_DL;
	}

	@Override
	public int getMaxSimultanPremiumDownloadNum() {
		return CON_PREM_DL;
	}

	@Override
	public void handleFree(DownloadLink downloadLink) throws Exception {
		try {
			Downloader downloader = prepareDownload(downloadLink);
			downloader.startDownload();
		} catch (Exception e) {
			throw e;
		} finally {
			sendPostRequest("removeConnectReq", EMPTY_STR, EMPTY_STR);
		}
	}

	@Override
	public void handlePremium(final DownloadLink downloadLink,
			final Account account) throws Exception {
		try {
			doLogin(account);
			Downloader downloader = prepareDownload(downloadLink);
			downloader.setUserID(account.getLongProperty(USERID, -1));
			downloader.startDownload();
		} catch (Exception e) {
			throw e;
		} finally {
			sendPostRequest("removeConnectReq", EMPTY_STR, EMPTY_STR);
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
	 * encode the plaintext into MD5 hexstring.
	 * 
	 * @param plaintext
	 * @return MD5encodedtext
	 * @throws Exception
	 */
	private String encode(String plaintext) throws Exception {
		MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
		md.update(plaintext.getBytes());
		byte byteData[] = md.digest();
		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (byte b : byteData) {
			sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	/**
	 * converts string to date.
	 * 
	 * @param dateStr
	 * @param format
	 * @return Date
	 * @throws Exception
	 */
	private Date stringToDate(String dateStr, String format) throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.parse(dateStr);
	}

	/**
	 * gets ManagementSystemList(Server list) from the relay nodes.
	 * 
	 * @return ManagementSystemEntryList
	 * @throws Exception
	 */
	private ManagementSystemEntry[] getMSList() throws Exception {
		ManagementSystemEntry[] msList;
		String relayNodeStr = DEFAULT_RELAY_NODE;
		String[] relayNodes = null;
		relayNodes = relayNodeStr.split(",");
		OceanusResolver resolver = new OceanusResolver(relayNodes);
		msList = resolver.getManagementSystems();
		return msList;
	}

	/**
	 * gets one of the running server IPs.
	 * 
	 * @param msList
	 * @return ServerIP
	 * @throws Exception
	 */
	private String getServerIP(ManagementSystemEntry[] msList) throws Exception {
		String serverIP = null;
		String msIP;
		for (ManagementSystemEntry ms : msList) {
			msIP = ms.getIpAddress();
			if (sendConnectReq(msIP)) {
				// inmemory.setMsIP(msIP);
				serverIP = msIP;
				break;
			}
		}
		return serverIP;
	}

	private static Map<String, Object> getRequestObject() {
		if (reqObj == null) {
			reqObj = new HashMap<String, Object>();
		} else {
			reqObj.clear();
		}
		return reqObj;
	}

	/**
	 * send connect request to server.
	 * 
	 * @param msIP
	 * @return boolean
	 * @throws Exception
	 */
	private boolean sendConnectReq(String msIP) throws Exception {
		boolean connected = false;
		Map<String, Object> resp = sendPostRequest(CON_REQ, EMPTY_STR, msIP);
		String status = (String) resp.get(STATUS);
		if (status != null && status.equals(OK_STR)) {
			connected = true;
		}
		return connected;
	}

	/**
	 * method to get downloadXml,parse it and to update downloader and download
	 * link(filename,filesize etc).
	 * 
	 * 
	 * @param downloadLink
	 * @return Downloader
	 * @throws PluginException
	 */
	private Downloader prepareDownload(DownloadLink downloadLink)
			throws PluginException {
		Downloader downloader = null;
		File destDir = null;
		String downloadXML = null;
		try {
			downloadLink.getLinkStatus().setStatusText(DOWNLOAD_PREPARE_MSG);
			String link = downloadLink.getDownloadURL();
			downloadXML = sendDownloadRequest(link);
			if (downloadXML != null && downloadXML != EMPTY_STR
					&& !downloadXML.equals(DOWNLOAD_INVALID)) {
				downloader = parseXML(downloadXML);
				downloadLink.setFinalFileName(downloader.getDownloadFileName());
				downloadLink.setDownloadSize(downloader.getSize());
				downloadLink.setAvailable(true);
				OceanusProgress ocProgress = new OceanusProgress(
						downloader.getSize(), downloadLink);
				downloader.setDownloadLink(downloadLink);
				downloader.setOceanus(this);
				downloader.setocProgress(ocProgress);
				String fout = downloadLink.getFileOutput().replace(
						"\\" + downloader.getDownloadFileName(), "");
				destDir = new File(fout);
				downloader.setDestinationPath(destDir);
			} else {
				throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT,
						DOWN_INVALID_MSG);
			}
			return downloader;

		} catch (Exception e) {
			throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT,
					e.getMessage());
		}
	}

	/**
	 * parse the xml string to form downloader.
	 * 
	 * @param downloadXMLStr
	 * @return Downloader
	 * @throws Exception
	 */
	private Downloader parseXML(String downloadXMLStr) throws Exception {
		InputStream inputStream = null;
		JAXBContext jaxbContext;
		Unmarshaller jaxbUnmarshaller;
		Downloader download = null;
		try {
			inputStream = new ByteArrayInputStream(
					downloadXMLStr.getBytes(CHAR_SET));
			jaxbContext = JAXBContext.newInstance(Downloader.class);
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			// unMarshall the input stream to downloader class
			download = (Downloader) jaxbUnmarshaller.unmarshal(inputStream);
			if (download.getEncryptionPwd() == null)
				download.setEncryptionPwd(OCInMemory.getInstance()
						.getActiveDownloadEncryptionKey(download.getUploadID()));

		} catch (JAXBException e) {
			throw new Exception(e);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
		return download;
	}

	/**
	 * sends download request.
	 * 
	 * @param oceanusLink
	 * @return downloadXml
	 * @throws Exception
	 */
	private String sendDownloadRequest(String oceanusLink) throws Exception {
		Map<String, Object> response;
		String downloadXml = "", errorMsg;
		ArrayList<Long> uploadIDList = new ArrayList<Long>();
		uploadIDList.add(getUploadId(oceanusLink));
		if (uploadIDList.size() > 0) {
			response = sendPrepareDownloadReq(uploadIDList);
			errorMsg = (String) response.get(PREP_DOWN_ERR);
			if (errorMsg != null && !errorMsg.isEmpty()) {
				return errorMsg;
			}
			downloadXml = ((ArrayList) response.get(PREP_DOWN)).get(0)
					.toString();
		}
		return downloadXml;
	}

	/**
	 * method to get the download xml;
	 * 
	 * @param uploadIDList
	 * @return JSONObject
	 * @throws Exception
	 */
	private Map<String, Object> sendPrepareDownloadReq(
			ArrayList<Long> uploadIDList) throws Exception {
		Map<String, Object> respObj, jsObj;
		try {
			jsObj = getRequestObject();
			jsObj.put(UPLOAD_ID_LST, uploadIDList);
			respObj = sendPostRequest(PRE_DOWN_REQ,
					mapper.writeValueAsString(jsObj), EMPTY_STR);
			return respObj;
		} catch (Exception e) {
			throw new Exception(e);
		}
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
	public void handleCaptcha(DownloadLink downloadLink, String captchaKey,
			long uploadID, long userID) throws Exception {
		int waitTime;
		Recaptcha recaptcha = new Recaptcha(br);
		Map<String, Object> resp;
		final String id = captchaKey;
		recaptcha.setId(id);
		recaptcha.load();
		final File captchaFile = recaptcha.downloadCaptcha(this
				.getLocalCaptchaFile());
		final String captchaCode = this.getCaptchaCode(captchaFile,
				downloadLink);
		resp = recaptcha.verifyCaptcha(captchaCode, uploadID, userID);
		if (!(Boolean) resp.get(CAPTCHA_VERIFIED)) {
			throw new PluginException(LinkStatus.ERROR_CAPTCHA);
		} else {
			waitTime = Integer.parseInt((String) resp.get(WAIT_TIME));
			wait(waitTime, downloadLink);
		}

	}

	/**
	 * get uploadID from the link
	 * 
	 * @param oceanusLink
	 *            - the oceanus download link
	 * @return uploadID
	 * @throws Exception
	 */
	private long getUploadId(String oceanusLink) throws Exception {
		OCInMemory inMemory = OCInMemory.getInstance();
		String idString;
		String encryptionKey = null, encodedUploadID = null;
		long uploadID;
		idString = oceanusLink.split("/u/")[1];
		if (idString != null && !idString.isEmpty()) {
			encryptionKey = idString.split("/")[1];
			if (encryptionKey != null && !encryptionKey.isEmpty()) {
				encodedUploadID = idString.split("/")[0];
			}
		}
		// If encoded upload id is in link proceed with download
		if (encodedUploadID != null) {
			// Decode the uploadID
			uploadID = decode64(encodedUploadID);
			// Send download request only if it is not already running
			if (!inMemory.isActiveDownload(uploadID)) {
				// set in-memory for download(to get scrambled password for
				// the download)
				inMemory.setActiveDownload(uploadID, encryptionKey);
			}

		} else {
			// else throw link invalid exception
			throw new Exception(LNK_INVALID);
		}
		return uploadID;
	}

	/**
	 * decodes the encoded uploadID into long.
	 * 
	 * @param toDecode
	 * @return long
	 * @throws Exception
	 */
	private long decode64(String toDecode) throws Exception {
		if (toDecode.length() > ENCODING_SIZE_64)
			throw new Exception("Passed string '" + toDecode + "' has "
					+ "an invalid length.");
		long ret = 0l;
		int skip = ENCODING_SIZE_64 - toDecode.length();

		for (int i = 0; i < ENCODING_SIZE_64; i++) {
			ret *= ENCODING_DICTIONARY.length();
			if (i < skip)
				continue;
			int idx = ENCODING_DICTIONARY.indexOf(toDecode.charAt(i - skip));
			if (idx == -1)
				throw new Exception("Passed string '" + toDecode
						+ "' contains illegal " + "characters");
			ret += idx;
		}
		return ret;
	}

	/**
	 * wait to start the download
	 * 
	 * @param waitTime
	 * @param downloadLink
	 * @throws PluginException
	 */
	private void wait(long waitTime, DownloadLink downloadLink)
			throws PluginException {
		logger.info("Waittime detected, waiting " + waitTime
				+ " seconds from now on...");
		sleep(waitTime * 1000l, downloadLink);
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
	public Map<String, Object> sendPostRequest(String action, String post,
			String msIP) throws Exception {
		URL url;
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		Map<String, Object> resp = null;
		String msUrl = null;
		OCInMemory inmemory = OCInMemory.getInstance();
		try {
			if (msIP != null && !(msIP.equals(EMPTY_STR))) {
				msUrl = HTTP + msIP + SERVER_ADDR + action;
			} else {
				msUrl = HTTP + inmemory.getMsIP() + SERVER_ADDR + action;
			}
			// Create connection
			url = new URL(msUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");

			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(post.getBytes().length));
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(post);
			writer.flush();
			resp = handlePostResponse(connection.getInputStream());
		} catch (Exception e) {
			throw e;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			if (writer != null) {
				writer.close();
			}
		}
		return resp;
	}

	/**
	 * handles response from server.
	 * 
	 * @param is
	 * @return JSONObject
	 * @throws Exception
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> handlePostResponse(InputStream is)
			throws Exception {
		// Get Response
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuffer response = new StringBuffer();
		Map<String, Object> respData = null;
		try {
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\n');
			}
		} catch (IOException e) {
			throw e;
		} finally {
			rd.close();
		}
		if (response.length() > 0) {
			respData = mapper.readValue(response.toString(), Map.class);
		}
		return respData;
	}

	/**
	 * @author Arunkumar Description: OceanusProgress Updates progress Add and
	 *         Remove speedMeter.
	 * 
	 */
	public class OceanusProgress {
		private static final String WAIT_MSG = "Please wait...";

		private long totalRead = 0;
		private long totalSize = 0;
		private DownloadLink downloadLink;
		private ManagedThrottledConnectionHandler manager;

		public OceanusProgress(long total, DownloadLink downloadLink) {
			this.totalSize = total;
			this.downloadLink = downloadLink;
			this.manager = new ManagedThrottledConnectionHandler(
					this.downloadLink);
			downloadLink.getDownloadLinkController().getConnectionHandler()
					.addConnectionHandler(manager);
			// downloadLink.getLinkStatus().addStatus(
			// LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
		}

		public InputStream addSpeedMeter(InputStream inputStream) {
			MeteredThrottledInputStream meteredStream = new MeteredThrottledInputStream(
					inputStream, new AverageSpeedMeter(10));
			manager.addThrottledConnection(meteredStream);
			return meteredStream;
		}

		public void removeSpeedMeter(InputStream inputStream) {
			MeteredThrottledInputStream meteredStream = (MeteredThrottledInputStream) inputStream;
			manager.removeThrottledConnection(meteredStream);
		}

		public void updateProgress(long bytesRead) {
			this.totalRead += bytesRead;
			downloadLink.setDownloadCurrent(totalRead);
			if (totalRead >= totalSize) {
				downloadLink.getDownloadLinkController().getConnectionHandler()
						.removeConnectionHandler(manager);
				downloadLink.getLinkStatus().setStatusText(WAIT_MSG);
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
		private String challenge;
		private String server;
		private String captchaAddress;
		private String id;
		private Browser rcBr;
		private int tries = 0;

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
		private File downloadCaptcha(final File captchaFile)
				throws IOException, PluginException {
			/* follow redirect needed as google redirects to another domain */
			if (this.getTries() > 0) {
				this.reload();
			}
			this.rcBr.setFollowRedirects(true);
			try {
				Browser.download(captchaFile,
						this.rcBr.openGetConnection(this.captchaAddress));
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
			this.rcBr
					.getPage("http://api.recaptcha.net/challenge?k=" + this.id);
			this.challenge = this.rcBr.getRegex("challenge.*?:.*?'(.*?)',")
					.getMatch(0);
			this.server = this.rcBr.getRegex("server.*?:.*?'(.*?)',").getMatch(
					0);
			if (this.challenge == null || this.server == null) {
				throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
			}
			this.captchaAddress = this.server + "image?c=" + this.challenge;
		}

		/**
		 * reload Captcha
		 * 
		 * @throws IOException
		 * @throws PluginException
		 */
		private void reload() throws IOException, PluginException {
			this.rcBr.getPage("http://www.google.com/recaptcha/api/reload?c="
					+ this.challenge + "&k=" + this.id
					+ "&reason=r&type=image&lang=en");
			this.challenge = this.rcBr
					.getRegex(
							"Recaptcha\\.finish\\_reload\\(\\'(.*?)\\'\\, \\'image\\'\\)\\;")
					.getMatch(0);

			if (this.challenge == null) {
				throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
			}
			this.captchaAddress = this.server + "image?c=" + this.challenge;
		}

		/**
		 * verify Captcha
		 * 
		 * @param code
		 * @return
		 * @throws Exception
		 */
		private Map<String, Object> verifyCaptcha(final String code,
				final long uploadID, final long userID) throws PluginException {
			Map<String, Object> respObj, jsObj;
			try {
				if (this.challenge == null || code == null) {
					throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
				}
				jsObj = getRequestObject();
				jsObj.put("userID", userID);
				jsObj.put("uploadID", uploadID);
				jsObj.put("challenge", challenge);
				jsObj.put("response", code);
				respObj = sendPostRequest(VERIFY_CAPTCHA,
						mapper.writeValueAsString(jsObj), EMPTY_STR);
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
	public AvailableStatus requestFileInformation(DownloadLink parameter)
			throws Exception {
		return null;
	}

	@XmlRootElement(name = "PrepareDownload")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Downloader {

		private static final String ENCODING_DICTIONARY = "ABCDEFGHJKLMNOPQRSTUVWXYZabcdefghjkmnopqrstuvwxyz023456789";
		private static final String DOWNLOADING_MSG = "Downloading...";
		private static final String DOWNLOAD_COMPLETE_MSG = "Download Completed";
		private static final String EMPTY_STR = "";
		private Long totalChunks = 0l, completedChunks = 0l;
		@XmlAnyElement(lax = true)
		protected OceanusProgress ocProgress;
		@XmlAttribute(name = "scrambledPW")
		private String encryptionPwd;
		@XmlAttribute(name = "uploadid")
		private long uploadId;
		@XmlAttribute(name = "oceanuslink")
		public String oceanusLink;
		@XmlElement(name = "File")
		private List<OCFile> fileList;
		Queue<Chunk> chunkQueue;
		private File destinationPath;
		@XmlAnyElement(lax = true)
		private DownloadLink downloadLink;
		@XmlAnyElement(lax = true)
		private Oceanus oceanus;
		private long userID = -1;

		public Downloader() {
			// to sort queue
			Comparator<Chunk> comparator = new Chunk();
			chunkQueue = new PriorityQueue<Chunk>(10, comparator);
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

		/**
		 * Check if the download is directory
		 * 
		 * @return - directory download/file download
		 */
		public boolean isDirDownload() {
			return !(fileList.size() == 1 && (fileList.get(0).getPath() == null || fileList
					.get(0).getPath().equals("null")));
		}

		public File getDestinationPath() {
			return destinationPath;
		}

		public void setDestinationPath(File destinationPath) {
			this.destinationPath = destinationPath;
		}

		public String getOceanusLink() {
			return oceanusLink;
		}

		public void setOceanusLink(String oceanusLink) {
			this.oceanusLink = oceanusLink;
		}

		public String getDownloadFileName() {
			String filePath;
			if (isDirDownload()) {
				filePath = fileList.get(0).getPath().split("/")[1];
			} else {
				filePath = fileList.get(0).getName();
			}
			return filePath;
		}

		/**
		 * Get total size of file
		 * 
		 * @return - total file size
		 */
		public long getSize() {
			long size = 0;
			for (OCFile ocFile : fileList) {
				size += ocFile.getSize();
			}
			return size;
		}

		public OceanusProgress getocProgress() {
			return ocProgress;
		}

		public void setocProgress(OceanusProgress ocProgress) {
			this.ocProgress = ocProgress;
		}

		public void setDownloadLink(DownloadLink downloadLink) {
			this.downloadLink = downloadLink;
		}

		public DownloadLink getDownloadLink() {
			return downloadLink;
		}

		public void setOceanus(Oceanus oceanus) {
			this.oceanus = oceanus;
		}

		public Oceanus getOceanus() {
			return oceanus;
		}

		public void enqueue(Chunk chunk) {
			synchronized (chunkQueue) {
				chunkQueue.offer(chunk);
			}
		}

		/**
		 * dequeue-removes the top most chunk from chunk queue
		 */
		public Chunk removeChunk() {
			Chunk chunk;
			synchronized (chunkQueue) {
				chunk = chunkQueue.remove();
			}
			return chunk;
		}

		/**
		 * loops through the files and starts download.
		 * 
		 * @throws Exception
		 */
		public void startDownload() throws Exception {
			long cdnNodeID;
			ArrayList<Chunk> chunksList;
			for (OCFile file : fileList) {
				chunksList = file.getChunks();
				totalChunks += chunksList.size();
				for (Chunk chunk : chunksList) {
					for (CDNNode cdnNode : chunk.getCdnNodes()) {
						cdnNodeID = cdnNode.getCdnNodeID();
						try {
							beginDownload(file, chunk, cdnNodeID);
							if ("SUCCESS".equals(chunk.getStatus())) {
								break;
							}
						} catch (Exception e) {
							retry(file, chunk);
						}

					}
				}
			}
			downloadCompleted();

		}

		/**
		 * method to download individual chunks within a file.
		 * 
		 * @param file
		 * @param chunk
		 * @param cdnNodeID
		 * @throws Exception
		 */
		private void beginDownload(OCFile file, Chunk chunk, long cdnNodeID)
				throws Exception {
			String captchaKey = sendDownloadRequest(chunk, cdnNodeID);
			if (captchaKey != null && !captchaKey.isEmpty()) {
				oceanus.handleCaptcha(downloadLink, captchaKey, uploadId,
						userID);
			}
			if ("OK".equals(chunk.getStatus())) {
				download(file, chunk);
			} else {
				calculateNextTryTime(chunk, chunk.getStatus());
				retry(file, chunk);
			}
		}

		/**
		 * sends start download request to server and returns captcha key if any
		 * exists.
		 * 
		 * @param chunk
		 * @param cdnNodeId
		 * @return
		 * @throws Exception
		 */
		private String sendDownloadRequest(Chunk chunk, long cdnNodeId)
				throws Exception {
			boolean dwnLimitExceeded;
			String chunkStatus;
			Map<String, Object> respObj, jsObj;
			String captchaKey = null;
			jsObj = getRequestObject();
			jsObj.put("uploadID", uploadId);
			jsObj.put("chunkID", chunk.getChunkID());
			jsObj.put("cdnNodeID", cdnNodeId);
			jsObj.put("userID", userID);
			respObj = oceanus.sendPostRequest("startDownloadReq",
					mapper.writeValueAsString(jsObj), EMPTY_STR);
			// process using reply message from MS
			if (respObj != null) {
				chunk.setDownloadID((Long) respObj.get("downloadID"));
				dwnLimitExceeded = (Boolean) respObj
						.get("DownloadLimitExceeded");
				chunkStatus = (String) respObj.get("Status");
				if (dwnLimitExceeded) {
					chunk.setStatus("download limit exceeded");
					chunk.setNextTryInMs(600000);
				}
				chunk.setDownloadURL((String) respObj.get("downloadURL"));
				chunk.setStatus(chunkStatus);
				if (chunkStatus.equals("Captcha Not Solved")) {
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
		private void download(OCFile file, Chunk chunk) throws Exception {
			downloadLink.getLinkStatus().setStatusText(DOWNLOADING_MSG);
			String chunkPath, chunkStatus;
			FileOutputStream fileOutputStream = null;
			HttpURLConnection urlConnection;
			InputStream inputStream = null;
			File tempFile;
			try {
				chunkStatus = chunk.getStatus();
				if (!("download limit exceeded".equals(chunkStatus))) {
					tempFile = File.createTempFile("tmp", null);
					// create temporary file in user temp location to store
					// decrypted downloaded chunk as temp file
					fileOutputStream = new FileOutputStream(tempFile);
					chunkPath = tempFile.getAbsolutePath();
					chunk.setPath(chunkPath);
					// Connect with CDN using the download URL
					urlConnection = connectToCDN(chunk.getDownloadURL());
					// add speedmeter to the inputstream.
					inputStream = ocProgress.addSpeedMeter(urlConnection
							.getInputStream());
					// Read chunk from CDN
					readChunk(chunk, inputStream, fileOutputStream);
					handleCDNResponse(file, chunk,
							urlConnection.getResponseCode());
					urlConnection.disconnect();
				} else {
					throw new Exception("download_limit_exceeded");
				}
			} catch (IOException ie) {
				throw ie;
			} finally {
				if (inputStream != null) {
					try {
						ocProgress.removeSpeedMeter(inputStream);
						inputStream.close();
					} catch (IOException ie) {
						throw ie;
					}
				}
				if (fileOutputStream != null) {
					try {
						fileOutputStream.flush();
						fileOutputStream.close();
					} catch (IOException ie) {
						throw ie;
					}
				}
			}
		}

		/**
		 * opens http get connection with given url.
		 * 
		 * @param downloadURL
		 * @return
		 * @throws Exception
		 */
		private HttpURLConnection connectToCDN(String downloadURL)
				throws Exception {
			URL url;
			HttpURLConnection urlConnection;
			url = new URL(downloadURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(false);
			urlConnection.setDoInput(true);
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();
			return urlConnection;
		}

		/**
		 * reads stream from the connection and decrypt it.
		 * 
		 * @param chunk
		 * @param inputStream
		 * @param fileOutputStream
		 * @throws Exception
		 */
		private void readChunk(Chunk chunk, InputStream inputStream,
				OutputStream fileOutputStream) throws Exception {
			int bytesRead;
			long tBytesRead = 0;
			byte[] buf = new byte[4096];
			OCInMemory inMemory = OCInMemory.getInstance();
			// Get the decryption key from in-memory using upload id
			String decryptionKeyStr = inMemory.getEncryptionKey(uploadId);
			InputStream cipherInputStream = null;
			// downloader.clearErrorMessage(file.getRowNumber());
			try {
				// get the decrypted stream of the input stream
				cipherInputStream = getDecryptionInputStream(inputStream,
						decryptionKeyStr);
				while ((bytesRead = cipherInputStream.read(buf)) != -1) {
					tBytesRead += bytesRead;
					fileOutputStream.write(buf, 0, bytesRead);
					// Set bytes transferred for chunk
					chunk.setBytesTransferred(tBytesRead);
					// updateProgress(bytesRead);
					ocProgress.updateProgress(bytesRead);
				}
			} catch (Exception ie) {
				throw ie;
			} finally {
				try {
					if (cipherInputStream != null) {
						cipherInputStream.close();
					}
				} catch (IOException e) {
					throw e;
				}
				try {
					if (fileOutputStream != null) {
						fileOutputStream.flush();
						fileOutputStream.close();
					}
				} catch (IOException e) {
					throw e;
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
		private void handleCDNResponse(OCFile file, Chunk chunk,
				int responseCode) throws Exception {
			if (responseCode == 200) {
				sendDownloadCompleted(chunk);
				chunk.setStatus("SUCCESS");
				combineChunks(file);
			} else if (responseCode == 500) {
				throw new Exception("Download is not approved");
			} else {
				calculateNextTryTime(chunk, "Other Error");
				retry(file, chunk);

			}
		}

		/**
		 * retry download incomplete chunks.
		 * 
		 * @param file
		 * @param chunk
		 * @throws Exception
		 */
		private void retry(OCFile file, Chunk chunk) throws Exception {
			enqueue(chunk);
			retryChunk(file);
		}

		/**
		 * retries chunk download
		 * 
		 * @param file
		 * @throws Exception
		 */
		private void retryChunk(OCFile file) throws Exception {
			long cdnNodeID;
			Chunk chunk;
			while (chunkQueue.size() > 0) {
				chunk = removeChunk();
				for (CDNNode cdnNode : chunk.getCdnNodes()) {
					cdnNodeID = cdnNode.getCdnNodeID();
					beginDownload(file, chunk, cdnNodeID);
					if ("SUCCESS".equals(chunk.getStatus())) {
						break;
					}
				}
			}

		}

		/**
		 * sends chunk completed message to server.
		 * 
		 * @param chunk
		 * @throws Exception
		 */
		private void sendDownloadCompleted(Chunk chunk) throws Exception {
			Map<String, Object> reqObj;
			reqObj = getRequestObject();
			reqObj.put("userID", userID);
			reqObj.put("downloadID", chunk.getDownloadID());
			reqObj.put("bytesTransferred", chunk.getBytesTransferred());
			oceanus.sendPostRequest("DownloadCompleted",
					mapper.writeValueAsString(reqObj), EMPTY_STR);

		}

		/**
		 * calculate next try time for a chunk.
		 * 
		 * @param chunk
		 * @param status
		 */
		private void calculateNextTryTime(Chunk chunk, String status) {
			if ("Temporarily not available".equals(status)) {
				chunk.setNextTryInMs(180000);
			} else if ("OK".equals(status)) {
				chunk.setNextTryInMs(0);
			} else if ("download limit exceeded".equals(status)) {
				chunk.setNextTryInMs(600000);
			} else {
				chunk.setNextTryInMs(90000);
			}
		}

		/**
		 * combine chunks into a file.
		 * 
		 * @param file
		 * @throws Exception
		 */
		protected void combineChunks(OCFile file) throws Exception {
			File dir, downloadFile;
			completedChunks++;
			String filePath = getDestinationPath().getAbsolutePath();
			if (file.isAllChunkSucceeded()) {
				if (isDirDownload()) {
					filePath = filePath + "/" + file.getPath();
				}
				dir = new File(filePath);
				if (!dir.exists()) {
					dir.mkdirs();
				} else {
					dir.delete();
				}
				downloadFile = new File(dir.getAbsolutePath() + "/"
						+ file.getName());

				// Combine the file chunks into one file when all chunks of
				// a
				// file are completed
				if (file.isAllChunkExist()) {
					file.combineChunks(downloadFile);
				}
			}

		}

		/**
		 * sends whole download completed message to server.
		 * 
		 * @throws Exception
		 */
		private void downloadCompleted() throws Exception {
			if (completedChunks.equals(totalChunks)) {
				Map<String, Object> jsonReq = getRequestObject();
				jsonReq.put("userID", userID);
				jsonReq.put("uploadID", uploadId);
				oceanus.sendPostRequest("wholeDownloadComplete",
						mapper.writeValueAsString(jsonReq), EMPTY_STR);
				OCInMemory.getInstance().removeActiveDownload(uploadId);
				// downloadLink.getLinkStatus().removeStatus(
				// LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
				downloadLink.getLinkStatus().setStatus(LinkStatus.FINISHED);
				downloadLink.getLinkStatus().setStatusText(
						DOWNLOAD_COMPLETE_MSG);

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
		private InputStream getDecryptionInputStream(
				InputStream originalInputStream, String encryptionKey)
				throws Exception {
			SecretKeySpec key = new SecretKeySpec(decodeArbitrary(
					encryptionKey, 128), "AES");
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
		private byte[] decodeArbitrary(String toDecode, int numBits)
				throws Exception {
			int encodingSize = (int) Math.ceil(numBits
					/ (Math.log(ENCODING_DICTIONARY.length()) / Math.log(2)));
			if (toDecode.length() > encodingSize)
				throw new Exception("Passed string '" + toDecode + "' has "
						+ "an invalid length.");

			int skip = encodingSize - toDecode.length();
			BigInteger bi = BigInteger.valueOf(0);
			BigInteger encodingDictionaryLength = BigInteger
					.valueOf(ENCODING_DICTIONARY.length());
			for (int i = 0; i < encodingSize; i++) {
				bi = bi.multiply(encodingDictionaryLength);

				if (i < skip)
					continue;
				int idx = ENCODING_DICTIONARY
						.indexOf(toDecode.charAt(i - skip));
				if (idx == -1)
					throw new Exception("Passed string '" + toDecode
							+ "' contains illegal " + "characters");
				bi = bi.add(BigInteger.valueOf(idx));
			}

			byte[] out = new byte[(int) (Math.ceil(numBits / 8))];
			byte[] decoded = bi.toByteArray();
			if (decoded.length == out.length + 1 && decoded[0] == 0) {
				// For some reason, biginteger sometimes returns a bytearray
				// with a single 0-valued byte
				System.arraycopy(decoded, 1, out, 0, out.length);
			} else {
				if (decoded.length > out.length)
					throw new Exception("Passed value equals more than "
							+ numBits + " bits");
				System.arraycopy(decoded, 0, out, out.length - decoded.length,
						decoded.length);
			}
			return out;
		}

		public void setUserID(long userID) {
			this.userID = userID;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class CDNNode {

		@XmlAttribute(name = "cdnnodeid")
		private long cdnNodeID;
		private String name;

		public long getCdnNodeID() {
			return cdnNodeID;
		}

		public void setCdnNodeID(long cdnNodeID) {
			this.cdnNodeID = cdnNodeID;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Chunk implements Comparator<Chunk> {

		private long uploadID, downloadID;
		private String path, downloadURL;
		private long nextTryInMS, bytesTransferred = -1;
		@XmlAttribute(name = "chunkid")
		private Long chunkId;
		@XmlElement(name = "offset")
		private long offset;
		@XmlElement(name = "Size")
		private long size;
		@XmlElement(name = "Status")
		private String status;
		@XmlElementWrapper(name = "CDNNodes")
		@XmlElement(name = "CDNNode")
		private List<CDNNode> cdnNodes;

		/**
		 * no-arg constructor is necessary for JAXB unmarshall
		 */
		public Chunk() {
			cdnNodes = new ArrayList<CDNNode>();

		}

		public Chunk(long uploadID, long chunkID, long chunkSize, long offset) {
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

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public long getDownloadID() {
			return downloadID;
		}

		public void setDownloadID(long downloadID) {
			this.downloadID = downloadID;
		}

		public long getNextTryInMS() {
			return nextTryInMS;
		}

		public void setNextTryInMs(long nextTryInSec) {
			this.nextTryInMS = nextTryInSec;
		}

		public String getDownloadURL() {
			return downloadURL;
		}

		public void setDownloadURL(String downloadURL) {
			this.downloadURL = downloadURL;
		}

		/**
		 * compare chunks by using next try of the chunk
		 * 
		 * @param chunk1
		 *            - chunk to compare
		 * @param chunk2
		 *            - chunk to compare
		 * @return nextTryInMS -the greater nextTryInMS
		 */
		@Override
		public int compare(Chunk chunk1, Chunk chunk2) {
			int nextTryInMS;
			if (chunk1.getNextTryInMS() > chunk2.getNextTryInMS()) {
				nextTryInMS = 1;
			} else if (chunk1.getNextTryInMS() < chunk2.getNextTryInMS()) {
				nextTryInMS = -1;
			} else {
				nextTryInMS = 0;
			}
			// chunk1.getNextTryInSec() - chunk2.getNextTryInSec();
			return nextTryInMS;
		}

		public long getBytesTransferred() {
			return bytesTransferred;
		}

		public void setBytesTransferred(long bytesTransferred) {
			this.bytesTransferred = bytesTransferred;
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
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class OCFile {
		@XmlAttribute(name = "fileID")
		private long fileId;
		long uploadId;
		@XmlElement(name = "FileName")
		private String name;
		@XmlElement(name = "FilePath")
		private String path;
		@XmlElementWrapper(name = "Chunks")
		@XmlElement(name = "Chunk")
		private ArrayList<Chunk> chunks;
		private long size;
		private int rowNumber; // TreeView rowNumber in UI for progressBar
								// Update.

		public OCFile() {
			chunks = new ArrayList<Chunk>();
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

		public ArrayList<Chunk> getChunks() {
			return chunks;
		}

		public void setChunks(ArrayList<Chunk> chunks) {
			this.chunks = chunks;
		}

		public long getFileId() {
			return fileId;
		}

		public void setFileId(long fileId) {
			this.fileId = fileId;
		}

		public int getRowNumber() {
			return rowNumber;
		}

		public void setRowNumber(int rowNumber) {
			this.rowNumber = rowNumber;
		}

		public long getSize() {
			long size = 0;
			for (Chunk chunk : chunks) {
				size += chunk.getSize();
			}
			return size;
		}

		/**
		 * Combine chunks into a single file by reading its associated temp
		 * files(Created while reading chunks from CDN) using chunk path
		 * 
		 * @param downloadFile
		 *            - the file (download file)
		 */
		public void combineChunks(File downloadFile) throws Exception {

			byte[] buf = new byte[4096];
			FileInputStream fileInputStream = null;
			FileOutputStream fileOutputStream = null;
			String path;
			int bytesRead;
			File file;
			try {
				fileOutputStream = new FileOutputStream(downloadFile);
				// Read for each chunk(using its path ) of the file
				for (Chunk chunk : chunks) {
					path = chunk.getPath();
					if (path != null && !path.isEmpty()) {
						file = new File(path);
						try {
							fileInputStream = new FileInputStream(file);
							while ((bytesRead = fileInputStream.read(buf, 0,
									4096)) != -1) {
								fileOutputStream.write(buf, 0, bytesRead);
							}
						} catch (IOException ie) {
							throw ie;
						} finally {
							if (fileInputStream != null) {
								fileInputStream.close();
							}
						}
					}
				}
				// delete chunks from temp file after combining
				deleteChunks();
			} catch (IOException ie) {
				throw ie;
			} finally {
				try {
					if (fileOutputStream != null) {
						fileOutputStream.flush();
						fileOutputStream.close();
					}
				} catch (IOException ie) {
					throw ie;
				}
			}
		}

		/**
		 * Check if all chunks of file are completed
		 * 
		 * @return - completed/not completed
		 */
		public boolean isAllChunkSucceeded() {
			boolean isSucceeded = false;
			for (Chunk chunk : chunks) {
				if ("SUCCESS".equals(chunk.getStatus())) {
					isSucceeded = true;
				} else {
					isSucceeded = false;
					break;
				}
			}
			return isSucceeded;
		}

		/**
		 * addChunkToList-adds chunk to chunk list
		 * 
		 * @param chunk
		 *            -chunk object to add
		 */
		public void addChunkToList(Chunk chunk) {
			chunks.add(chunk);
		}

		/**
		 * isAllChunkExist-checks if all chunks
		 * 
		 * @return isExist-true/false
		 */
		public boolean isAllChunkExist() {
			boolean isExist = false;
			File file;
			for (Chunk chunk : chunks) {
				file = new File(chunk.getPath());
				if (file.exists()) {
					isExist = true;
				} else {
					isExist = false;
					break;
				}
			}
			return isExist;
		}

		/**
		 * Delete temporary chunk files after reading all chunks of file
		 */
		private void deleteChunks() {
			File file;
			for (Chunk chunk : chunks) {
				file = new File(chunk.getPath());
				file.delete();
			}
		}
	}

	public static class OCInMemory {
		private static OCInMemory instance;
		private Hashtable<Long, String> activeDownloads;
		private String msIP;

		/**
		 * singleton class, can't be instantiated
		 */
		private OCInMemory() {
			activeDownloads = new Hashtable<Long, String>();
		}

		/**
		 * getInstance-Get instance of class
		 * 
		 * @return OCInMemory
		 */
		public static OCInMemory getInstance() {
			if (instance == null) {
				instance = new OCInMemory();
			}
			return instance;
		}

		/**
		 * Set active download
		 * 
		 * @param uploadID
		 *            - the uploadID
		 * @param encryptionKey
		 *            - scrambled password
		 */
		public void setActiveDownload(long uploadID, String encryptionKey) {
			activeDownloads.put(uploadID, encryptionKey);
		}

		public String getActiveDownloadEncryptionKey(long uploadId) {
			return activeDownloads.get(uploadId);
		}

		/**
		 * Check if the current download is already running
		 * 
		 * @param uploadID
		 *            - upload id
		 * @return - active download/not
		 */
		public boolean isActiveDownload(long uploadID) {
			return activeDownloads.containsKey(uploadID);
		}

		/**
		 * Remove active download entry using uploadid
		 * 
		 * @param uploadID
		 *            - upload id of download
		 */
		public void removeActiveDownload(long uploadID) {
			activeDownloads.remove(uploadID);
		}

		public String getEncryptionKey(long uploadID) {
			return activeDownloads.get(uploadID);
		}

		public void setMsIP(String msIP) {
			this.msIP = msIP;
		}

		public String getMsIP() {
			return msIP;
		}
	}

	public class ManagementSystemEntry {

		private String ipAddress;
		private int sslPort;

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

		public OceanusResolver(String[] relayNodes) throws ResolverException {
			List<String> randomNodes = Arrays.asList(relayNodes);
			Collections.shuffle(randomNodes);

			ArrayList<SingleRelayProcessor> list = new ArrayList<SingleRelayProcessor>(
					randomNodes.size());
			for (String node : randomNodes) {
				try {
					SingleRelayProcessor sp = new SingleRelayProcessor(node);
					/*
					 * if (!sp.verifySignature()) { //
					 * logger.info("Signature verify error at " + node);
					 * continue; } else {
					 */
					list.add(sp);
					// }
					Collections.sort(list);
					if (list.size() >= 2
							&& list.get(0).getTimestamp() == list.get(1)
									.getTimestamp()) {
						winner = sp;
						break;
					}
				} catch (ResolverException e) {
					// logger.info(e.getMessage(), e);
					continue;
				}
			}
			if (list.size() == 0) {
				throw new ResolverException(
						"No valid relay document found in any of the nodes");
			}
			winner = list.get(0);
		}

		public String[] getRelayHosts() {
			return winner.getRelayHosts();
		}

		public ManagementSystemEntry[] getManagementSystems() {
			List<ManagementSystemEntry> entries = Arrays.asList(winner
					.getManagementSystems());
			Collections.shuffle(entries);
			return entries.toArray(new ManagementSystemEntry[entries.size()]);
		}
	}

	public class ResolverException extends Exception {
		public ResolverException() {
			super();
		}

		public ResolverException(String message) {
			super(message);
		}

		public ResolverException(String message, Throwable cause) {
			super(message, cause);
		}

		public ResolverException(Throwable cause) {
			super(cause);
		}
	}

	public class SingleRelayProcessor implements Comparable {

		private static final String xpathTimeStamp = "/oceanusxmlrelay/timestamp";
		private static final String xpathRelays = "/oceanusxmlrelay/relays/relay";
		private static final String xpathMs = "/oceanusxmlrelay/managementsystems/ms";

		public int[] OCEANUSCASIGNATURE = new int[] { 0xa1, 0x8d, 0xdf, 0xd2,
				0x7d, 0x1b, 0x01, 0x75, 0x09, 0x2e, 0xcb, 0x87, 0xb1, 0x45,
				0x9f, 0xff, 0xdb, 0x8f, 0x99, 0xbb, 0x8b, 0xac, 0x06, 0xbd,
				0x2f, 0x4f, 0xa6, 0x41, 0xb2, 0x47, 0x77, 0xb0, 0x47, 0xe7,
				0x9a, 0xdd, 0x26, 0x7b, 0x65, 0x0f, 0x36, 0xf6, 0x07, 0x21,
				0xd3, 0x41, 0xf7, 0xc1, 0x27, 0xab, 0xe2, 0xa2, 0x30, 0x49,
				0xa4, 0x71, 0x10, 0x5d, 0x3e, 0x28, 0xfb, 0x3a, 0x68, 0xae,
				0x3b, 0xb1, 0x98, 0x81, 0x35, 0xdf, 0xbb, 0xcb, 0x86, 0x2e,
				0xf3, 0x73, 0x91, 0xe7, 0x63, 0xd2, 0x97, 0xef, 0xd1, 0x66,
				0x67, 0x19, 0x12, 0x65, 0x69, 0x9c, 0x01, 0xad, 0x56, 0x7d,
				0x70, 0x73, 0x39, 0x8f, 0xef, 0xbc, 0xbd, 0xf4, 0x29, 0x04,
				0x30, 0xa6, 0x9c, 0x42, 0x00, 0x3b, 0x9a, 0x07, 0x3f, 0x03,
				0x88, 0x93, 0x3b, 0x89, 0xe9, 0x5b, 0x9f, 0xb4, 0x7f, 0x0c,
				0x57, 0x0a, 0x45, 0x57 };

		String url;
		Document doc;

		int timestamp;
		String[] relayHosts;
		ManagementSystemEntry[] managementSystems;

		public SingleRelayProcessor(String url) throws ResolverException {
			this.url = url;
			try {
				fetchURL();
				getData();
			} catch (IOException e) {
				throw new ResolverException(e.getMessage(), e);
			} catch (ParserConfigurationException e) {
				throw new ResolverException(e.getMessage(), e);
			} catch (SAXException e) {
				throw new ResolverException(e.getMessage(), e);
			}
		}

		private void getData() throws ResolverException {
			try {
				NodeList nl = (NodeList) XPathFactory.newInstance().newXPath()
						.evaluate(xpathTimeStamp, doc, XPathConstants.NODESET);
				if (nl.getLength() != 1)
					throw new ResolverException(
							"Couldn't find Timestamp element at " + url);
				Element e = (Element) nl.item(0);
				try {
					timestamp = Integer.parseInt(e.getTextContent());
				} catch (NumberFormatException ex) {
					throw new ResolverException("Invalid timestamp at " + url);
				}

				nl = (NodeList) XPathFactory.newInstance().newXPath()
						.evaluate(xpathRelays, doc, XPathConstants.NODESET);
				relayHosts = new String[nl.getLength()];
				for (int i = 0; i < nl.getLength(); i++) {
					relayHosts[i] = nl.item(i).getTextContent();
				}

				nl = (NodeList) XPathFactory.newInstance().newXPath()
						.evaluate(xpathMs, doc, XPathConstants.NODESET);
				managementSystems = new ManagementSystemEntry[nl.getLength()];
				for (int i = 0; i < nl.getLength(); i++) {
					ManagementSystemEntry ms = new ManagementSystemEntry();
					Element msElement = (Element) nl.item(i);
					NodeList nlIp = msElement.getElementsByTagName("ip");
					NodeList nlSsl = msElement.getElementsByTagName("sslport");
					if (nlIp.getLength() != 1 || nlSsl.getLength() != 1) {
						throw new ResolverException(
								"Invalid ManagementSystem entry at " + url);
					}
					ms.setIpAddress(nlIp.item(0).getTextContent());
					try {
						ms.setSslPort(Integer.parseInt(nlSsl.item(0)
								.getTextContent()));
					} catch (NumberFormatException ex) {
						throw new ResolverException("Invalid sslport entry at "
								+ url);
					}
					managementSystems[i] = ms;
				}
			} catch (XPathExpressionException e) {
				throw new ResolverException(e.getMessage(), e);
			}
		}

		private void fetchURL() throws IOException,
				ParserConfigurationException, SAXException {
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

		public void setManagementSystems(
				ManagementSystemEntry[] managementSystems) {
			this.managementSystems = managementSystems;
		}

		@Override
		public int compareTo(Object o) {
			SingleRelayProcessor other = (SingleRelayProcessor) o;
			if (other.getTimestamp() < getTimestamp())
				return -1;
			if (other.getTimestamp() > getTimestamp())
				return 1;
			return 0;
		}
	}

}
