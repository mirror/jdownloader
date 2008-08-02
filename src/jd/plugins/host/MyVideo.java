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

package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDMediaConvert;
import jd.utils.JDUtilities;

public class MyVideo extends PluginForHost {
    // static private final String new Regex("$Revision: 2107 $","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision: 2107 $","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";
    static private final String HOST = "myvideo.de";

    static private final Pattern URL =         Pattern.compile("< myvideodl url=\"(.*?)\" decrypted=\".*?\" convert=\".*?\" name=\".*?\" >", Pattern.CASE_INSENSITIVE);
    static private final Pattern DOWNLOADFILE= Pattern.compile("< myvideodl url=\".*?\" decrypted=\"(.*?)\" convert=\".*?\" name=\".*?\" >", Pattern.CASE_INSENSITIVE);
    static private final Pattern CONVERT =     Pattern.compile("< myvideodl url=\".*?\" decrypted=\".*?\" convert=\"(.*?)\" name=\".*?\" >", Pattern.CASE_INSENSITIVE);
    static private final Pattern FILENAME=     Pattern.compile("< myvideodl url=\".*?\" decrypted=\".*?\" convert=\".*?\" name=\"(.*?)\" >", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_SUPPORTED=Pattern.compile("< myvideodl url=\".*\" decrypted=\".*\" convert=\".*\" name=\".*?\" >", Pattern.CASE_INSENSITIVE);
    
    
    public MyVideo() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.myvideo.de/news.php?rubrik=jjghf&p=hm8";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

    	String name = new Regex(downloadLink.getDownloadURL(), FILENAME).getFirstMatch();           
    	downloadLink.setName(name + ".tmp");
    	downloadLink.setStaticFileName(name + ".tmp");
    	String browserurl = new Regex(downloadLink.getDownloadURL(), URL).getFirstMatch();  
    	downloadLink.setBrowserUrl(browserurl);
        //TODO: Anderen Dateinamen anzeigen, sprich .tmp durch entsprechendes ersetzen
        if (name == null) { return false; }
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        //LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 50;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2107 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        
        final ConversionMode convertto = ConversionMode.valueOf(new Regex(downloadLink.getDownloadURL(), CONVERT).getFirstMatch());
         
    	String name = new Regex(downloadLink.getDownloadURL(), FILENAME).getFirstMatch();           
    	downloadLink.setName(name + ".tmp");
    	downloadLink.setStaticFileName(name + ".tmp");    
        String downloadfile = new Regex(downloadLink.getDownloadURL(), DOWNLOADFILE).getFirstMatch().trim();

        if (name == null || downloadfile == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            
            linkStatus.setErrorMessage(JDLocale.L("plugins.host.myvideo.unavailable", "MyVideo Serverfehler"));
            return;

        }
        
        RequestInfo requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadfile), null, downloadfile, true);
        HTTPConnection urlConnection = requestInfo.getConnection();

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setRequestTimeout(100000);
        dl.setReadTimeout(1000000);
        dl.setResume(false);
        if (dl.startDownload()) {
        	
        	ConversionMode InType = ConversionMode.VIDEOFLV;
        	
        	if(JDMediaConvert.ConvertFile(downloadLink, InType, convertto));
        	{
        	    linkStatus.addStatus(LinkStatus.FINISHED);
        	}
            
        }

    }

    @Override
    public void reset() {
    	
    }

    @Override
    public void resetPluginGlobals() {

    }
}
