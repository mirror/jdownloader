//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Vector;
import java.util.ArrayList;
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
	
	final static String HOST 				= "xlice.net";
	private String 		VERSION 			= "2.1.2";
	private String 		CODER 				= "JD-Team";
	private Pattern 	patternSupported 	= getSupportPattern("http://[*]xlice.net/(.*/)?(file|folder)/[a-zA-Z0-9]{32}[*]");
	
	private static final String[] USEARRAY = new String[] { "Rapidshare.com",
			"Uploaded.to", "FileFactory.com", "Fast-Load.net",
			"MegaUpload.com", "Netload.in", "Gulli.com", "Filer.net",
			"Load.to", "Sharebase.de", "zShare.net", "Share-Online.biz",
			"Bluehost.to", /* no plugin yet >> */ "BinLoad.to", "Simpleupload.net",
			"UltimateLoad.in", "MeinUpload.com", "Qshare.com", /* old >> */
			"Fastshare.org", "Uploadstube.de", "Files.to", "Datenklo.net"
			 };

	private final static Pattern patternTableRowLink = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
	private final static Pattern patternFileName = Pattern.compile("<div align=\"left\">(.*?) \\(.*\\) <br />");

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

	static Pattern patternJsScript = Pattern.compile("<script[^>].*>(.*)\\n[^\\n]*=\\s*(des.*).\\n[^\\n]*document.write\\(.*?</script>", Pattern.DOTALL);

	static Pattern patternHosterIframe = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"");

	public XliceNet() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
		this.setConfigEelements();
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

				//just to fetch the link count
				String[] links = new Regexp(reqinfo.getHtmlCode(), patternLink )
						.getMatches(1);
				progress.setRange(links.length);
				
				String[] rowCandidates = new Regexp(reqinfo.getHtmlCode(), patternTableRowLink).getMatches(1);
				
				for (String rowCandiate : rowCandidates) {
					
					//check if there is a link in rowCandidate
					String link = new Regexp(rowCandiate, patternLink).getFirstMatch(1);

					if(null == link){
						continue;
					}
					
					//check if there is a filename in row Candidate
					String fileName = new Regexp(rowCandiate, patternFileName).getFirstMatch();
					
					URL mirrorUrl = new URL("http://" + (getHost() + link));
					RequestInfo mirrorInfo = getRequest(mirrorUrl, null, null,
							true);

					ArrayList<ArrayList<String>> groups = getAllSimpleMatches(
							mirrorInfo.getHtmlCode(), patternMirrorLink);

					for (ArrayList<String> pair : groups) {
						// check if user does not want the links from this
						// hoster
						// if( !getUseConfig(mirrorHoster.get(i))){
						if (!getUseConfig(pair.get(1))) {
							logger.info(pair.get(1)+" is ignored due to user config");
							continue;
						}

						URL fileURL = new URL("http://" + getHost()
								+ pair.get(0));
						
						//System.out.println(fileURL);
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
						Matcher matcher = patternJsScript.matcher(fileInfo.getHtmlCode());

						if (!matcher.find()) {
							logger.severe("Unable to find decypher recipe - step to next link");
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
						
						if( null == hosterURL ){
							logger.severe("Unable to determin hosterURL - adapt patternHosterIframe");
							continue;
						}

						DownloadLink downloadLink = createDownloadlink(hosterURL);
						downloadLink.setName(fileName);

						decryptedLinks.add(downloadLink);
					}
					progress.increase(1);
				}

				logger.info(decryptedLinks.size() + " downloads decrypted");

				step.setParameter(decryptedLinks);
			} catch (MissingResourceException e) {
				step.setStatus(PluginStep.STATUS_ERROR);
				logger.severe("MissingResourceException class name: "
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