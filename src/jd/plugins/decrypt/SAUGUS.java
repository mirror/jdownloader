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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.utils.HTMLEntities;

public class SAUGUS extends PluginForDecrypt {
	final static String host = "saug.us";

	private String version = "1.0.0.0";

	// http://s2.saug.us/folder-m304wzg6qreym6d8xvtpt3gmyfqzjh0.html
	private Pattern patternSupported = Pattern.compile(
			"http://.*?saug.us/folder-[a-zA-Z0-9\\-]{30,50}\\.html",
			Pattern.CASE_INSENSITIVE);

	public SAUGUS() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
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

				
				request.getRequest(parameter);
				if(request.toString().contains("<span style=\"font-size:9pt;\">Dateien offline!"))
				{
					return null;
				}
				String hst = "http://" + request.getHost()+"/";
				String[] crypt = new Regexp(request.getHtmlCode(),
						"document.write\\(deca.*?\\(\'(.*?)\'\\)\\)\\;").getMatches(1);
				progress.setRange(crypt.length);
						for (int i = 0; i < crypt.length; i++) {
							String string = crypt[i];
							string = deca1(string);
							string = new Regexp(string,
									"document.write\\(dec.*?\\(\'(.*?)\'\\)\\)\\;")
									.getFirstMatch();
							string = deca1(string);
							string = hst+HTMLEntities.unhtmlentities(new Regexp(string,
							"javascript\\:page\\(\'(.*?)\'\\)\\;").getFirstMatch());
							string = HTMLEntities.unhtmlentities(new Regexp(request.getRequest(string).toString().replaceAll("<!--.*?-->", ""), "<iframe src=\"(.*?)\"").getFirstMatch()).trim().replaceAll("^[\\s]*", "");
							if(!string.toLowerCase().matches("http\\:\\/\\/.*"))
								decryptedLinks.add(createDownloadlink(request.getRequest(hst+string).getForm().action));
							else
								decryptedLinks.add(createDownloadlink(string));

							

							progress.increase(1);
						}
				step.setParameter(decryptedLinks);
			} catch (Exception e) {
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