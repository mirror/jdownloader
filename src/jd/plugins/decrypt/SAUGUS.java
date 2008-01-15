package jd.plugins.decrypt;

import jd.plugins.DownloadLink;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.Form;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.utils.HTMLEntities;

public class SAUGUS extends PluginForDecrypt {
	final static String host = "saug.us";

	private String version = "1.0.0.0";

	// http://s2.saug.us/folder-m304wzg6qreym6d8xvtpt3gmyfqzjh0.html
	private Pattern patternSupported = Pattern.compile(
			"http://.*?saug.us/folder-[a-zA-Z0-9]{30,34}\\.html",
			Pattern.CASE_INSENSITIVE);

	public SAUGUS() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
	}

	@Override
	public String getCoder() {
		return "scr4ve";
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

	public String deca1(String input) {
		final String keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
		String output = "";
		char type1, type2, type3;
		char get1, get2 = 0, get3 = 0, get4 = 0;
		int i = 0;

		// remove all characters that are not A-Z, a-z, 0-9, +, /, or =
		input = input.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");

		do {
			get1 = (char) keyStr.indexOf(input.charAt(i++));

			get2 = (char) keyStr.indexOf(input.charAt(i++));

			get3 = (char) keyStr.indexOf(input.charAt(i++));

			get4 = (char) keyStr.indexOf(input.charAt(i++));

			type1 = (char) ((get1 << 2) | (get2 >> 4));
			type2 = (char) (((get2 & 15) << 4) | (get3 >> 2));
			type3 = (char) (((get3 & 3) << 6) | get4);

			output = output + (char) type1;

			if (get3 != 64) {
				output = output + (char) type2;
			}
			if (get4 != 64) {
				output = output + (char) type3;
			}
		} while (i < input.length());

		return output;
	}

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		if (step.getStep() == PluginStep.STEP_DECRYPT) {
			Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
			try {
				URL url = new URL(parameter);
				String hst = "http://" + url.getHost()+"/";
				requestInfo = getRequest(url);
				String cookie = requestInfo.getCookie();
				String[] crypt = new Regexp(requestInfo.getHtmlCode(),
						"document.write\\(deca.*?\\(\'(.*?)\'\\)\\)\\;").getMatches(1);
				progress.setRange(crypt.length);
						for (int i = 0; i < crypt.length; i++) {
							String string = crypt[i];
							string = deca1(string);
							string = new Regexp(string,
									"document.write\\(dec.*?\\(\'(.*?)\'\\)\\)\\;")
									.getFirstMatch();
							string = deca1(string);
							progress.increase(1);
							requestInfo = getRequest(new URL(hst+new Regexp(string,
							"javascript\\:page\\(\'(go.php.*?)\'\\)\\;")
							.getFirstMatch()), cookie, parameter, true);
							string = requestInfo.getHtmlCode().replaceAll("<!--.*?-->", "");
							string = new Regexp(string, "<iframe src=\"(.*?)\"").getFirstMatch();
							string = HTMLEntities.unhtmlentities(string);
							string = getRequest(new URL(hst+string), cookie, requestInfo.getConnection().getURL().toString(), true).getHtmlCode();
							string = new Regexp(string, "<iframe src=\"(.*?)\"").getFirstMatch();
							if(string.matches(".*weiterleitung.*"))
							string = Form.getForms(getRequest(new URL(string), cookie, requestInfo.getConnection().getURL().toString(), true))[0].getConnection().getURL().toString();
							decryptedLinks.add(createDownloadlink(string));
						}
				step.setParameter(decryptedLinks);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}
}