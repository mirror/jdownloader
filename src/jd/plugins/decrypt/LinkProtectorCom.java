//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;  import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class LinkProtectorCom extends PluginForDecrypt {
	private static final String host             = "link-protector.com";
	private static final String version          = "1.0.0.0";

	private static final Pattern patternSupported = getSupportPattern("http://[*]link-protector\\.com/[\\d]{6}[*]");

	public LinkProtectorCom() {
		super();
		steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
		currentStep = steps.firstElement();
	}

	@Override
	public String getCoder() {
		return "Luke";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public String getPluginID() {
		return host + "-" + version;
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

	@Override
	public PluginStep doStep(PluginStep step, String parameter) {
		//example link: http://link-protector.com/459915/ 
		
		if (step.getStep() == PluginStep.STEP_DECRYPT) {
			Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
			try {
				URL url = new URL(parameter);
				RequestInfo reqinfo = HTTP.getRequest(url); // Seite aufrufen
				
				String decryptedLink = SimpleMatches.getBetween(reqinfo.getHtmlCode(), "write\\(stream\\('", "'\\)");
				int charCode = Integer.parseInt(SimpleMatches.getBetween(reqinfo.getHtmlCode(), "fromCharCode\\(yy\\[i\\]-", "\\)\\;"));

				String link = SimpleMatches.getBetween(decryptCode(decryptedLink,charCode),"<iframe src=\"", "\" ");
				decryptedLinks.add(this.createDownloadlink(link));
				progress.increase(1);
				step.setParameter(decryptedLinks);
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String decryptCode(String decryptedLink,int charCode) {
		String result = "";
		try {
			for (int i=0;i*4<decryptedLink.length();i++){
				result = (char)(Integer.parseInt(decryptedLink.substring(i*4,i*4+4))-charCode) + result;
			}
		} catch (Exception e){
			result = "";
		}
		
		return result;
	}

	@Override
	public boolean doBotCheck(File file) {
		return false;
	}

}
