package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

//xlice.net/folder/13a3169edf4cfd3a438a40c2397724fe/
// xlice.net/file/e46b767e51b8dbdf3afb6d3ea3852c4e/
// xlice.net/file/ff139aafdf5c299c33b218b9750b3d17/%5BSanex%5D%20-

public class XliceNet extends PluginForDecrypt {
	final static String host = "xlice.net";

	private static final String[] USEARRAY = new String[] { "Rapidshare.com",
			"Uploaded.to", "FileFactory.com", "Fast-Load.net",
			"MegaUpload.com", "Netload.in", "Gulli.com", "Filer.net",
			"Load.to", "Sharebase.de", "zShare.net", "Share-Online.biz",
			/* no plugin yet >> */"BinLoad.to", "Simpleupload.net",
			"UltimateLoad.in", "MeinUpload.com", "Qshare.com", /* old >> */
			"Fastshare.org", "Uploadstube.de", "Files.to", "Datenklo.net",
			"Bluehost.to" };

	private String version = "2.1.1";

	private Pattern patternSupported = getSupportPattern("http://[*]xlice.net/(.*/)?(file|folder)/[a-zA-Z0-9]{32}[*]");

	// <a href="#" id="contentlink_0"
	// rev="/links/76b5bb4380524456c61c1afb1638fbe7/" rel="linklayer"><img
	// src="/img/download.jpg" alt="Download" width="24" height="24"
	// border="0"></a>
	static Pattern patternLink = Pattern.compile(
			"<a href=\"#\".*rev=\"([^\"].+)\" rel=\"linklayer\">",
			Pattern.CASE_INSENSITIVE);

	// <br /><a href="/" onclick="createWnd('/gateway/278450/5/', '', 1000,
	// 600);" id="dlok">share.gulli.com</a>
	static Pattern patternMirrorLink = Pattern
			.compile("<a href=\"[^\"]*\" onclick=\"createWnd\\('([^']*)[^>]*>([^<]+)</a>");

	static Pattern patternJSDESFile = Pattern
			.compile("<script type=\"text/javascript\" src=\"/([^\"]+)\">");

	static Pattern patternJsScript = Pattern
			.compile(
					"<script type=\"text/javascript\">(.*)var ciphertext = (des \\(substro, message, 0\\));",
					Pattern.DOTALL);

	static Pattern patternHosterIframe = Pattern.compile("src=\"([^\"]+)\"");

	public XliceNet() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		this.setConfigEelements();
	}

	@Override
	public String getCoder() {
		return "JD-Team";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getPluginID() {
		return host + "-" + VERSION;
	}

	@Override
	public String getPluginName() {
		return host;
	}

	@Override
	public Pattern getSupportedLinks() {
		return patternSupported;
	}

	@Override
	public String getVersion() {
		return version;
	}

	private boolean getUseConfig(String link) {
		if (link == null) {
			return false;
		}

		link = link.toLowerCase();
		for (String hoster : USEARRAY) {
			if (link.matches(".*" + hoster.toLowerCase() + ".*")) {
				return getProperties().getBooleanProperty(hoster, true);
			}
		}

		return false;
	}

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		if (step.getStep() == PluginStep.STEP_DECRYPT) {
			Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
			Context cx = null;
			try {

				Scriptable scope = null;

				URL url = new URL(parameter);
				RequestInfo reqinfo = getRequest(url, null, null, true);

				String[] links = new Regexp(reqinfo.getHtmlCode(), patternLink)
						.getMatches(1);
				progress.setRange(links.length);

				for (String link : links) {
					URL mirrorUrl = new URL("http://" + (getHost() + link));
					RequestInfo mirrorInfo = getRequest(mirrorUrl, null, null,
							true);

					Vector<Vector<String>> groups = getAllSimpleMatches(
							mirrorInfo.getHtmlCode(), patternMirrorLink);

					for (Vector<String> pair : groups) {
						// check if user does not want the links from this
						// hoster
						// if( !getUseConfig(mirrorHoster.get(i))){
						if (!getUseConfig(pair.get(1))) {
							continue;
						}

						URL fileURL = new URL("http://" + getHost()
								+ pair.get(0));
						RequestInfo fileInfo = getRequest(fileURL, null, null,
								true);

						if (null == cx) {
							// setup the JavaScrip interpreter context
							cx = Context.enter();
							scope = cx.initStandardObjects();

							// fetch the file that contains the JavaScript
							// Implementation of DES
							String jsDESLink = new Regexp(fileInfo
									.getHtmlCode(), patternJSDESFile)
									.getFirstMatch();
							URL jsDESURL = new URL("http://" + getHost() + "/"
									+ jsDESLink);
							RequestInfo desInfo = getRequest(jsDESURL);

							// compile the script and load it into context and
							// scope
							cx.compileString(desInfo.getHtmlCode(), "<des>", 1,
									null).exec(cx, scope);
						}

						// get the script that contains the link and the
						// decipher recipe
						Matcher matcher = patternJsScript.matcher(fileInfo
								.getHtmlCode());

						if (!matcher.find()) {
							continue;
						}

						// put the script together and run it
						String decypherScript = matcher.group(1)
								+ matcher.group(2);
						Object result = cx.evaluateString(scope,
								decypherScript, "<cmd>", 1, null);

						// fetch the result of the javascript interpreter and
						// finally find the link :)
						String iframe = Context.toString(result);
						String hosterURL = new Regexp(iframe,
								patternHosterIframe).getFirstMatch();

						// System.out.println(hosterURL);

						decryptedLinks.add(createDownloadlink(hosterURL));

					}
					progress.increase(1);
				}

				logger.info(decryptedLinks.size()
						+ " "
						+ JDLocale.L(
								"plugins.decrypt.general.downloadsDecrypted",
								"Downloads entschlüsselt"));

				step.setParameter(decryptedLinks);
			} catch (MissingResourceException e) {
				step.setStatus(PluginStep.STATUS_ERROR);
				logger.severe("MissingResourceException className: "
						+ e.getClassName() + " key: " + e.getKey());
				e.printStackTrace();
			} catch (IOException e) {
				step.setStatus(PluginStep.STATUS_ERROR);
				e.printStackTrace();
			} finally {
				// Exit from the context.
				if (null != cx) {
					Context.exit();
				}
			}
		}
		return null;
	}

	private void setConfigEelements() {
		ConfigEntry cfg;
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
				JDLocale.L("plugins.decrypt.general.hosterSelection",
						"Hoster Auswahl")));
		config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
		for (int i = 0; i < USEARRAY.length; i++) {
			config.addEntry(cfg = new ConfigEntry(
					ConfigContainer.TYPE_CHECKBOX, getProperties(),
					USEARRAY[i], USEARRAY[i]));
			cfg.setDefaultValue(true);
		}
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}
}