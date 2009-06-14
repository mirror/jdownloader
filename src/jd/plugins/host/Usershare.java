package jd.plugins.host;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class Usershare extends PluginForHost {

    public Usershare(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://usershare.net/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String linkurl = br.getRegex(".*?flashvars=.*?<a href=\"(.*?)\"><img src=\"/images/download_btn.jpg\" border=0>").getMatch(0);
        if (linkurl == null)
            {
            linkurl = br.getRegex(".*?document.oncontextmenu=new Function.*?<a href=\"(.*?)\"><img src=\"/images/download_btn.jpg\" border=0>").getMatch(0);
            if(linkurl == null)throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        br.setFollowRedirects(true);
        
        dl = br.openDownload(link, linkurl, true, -10);
        dl.startDownload();   
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if(br.containsHTML("No such user exist") || br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String filesize = br.getRegex("Size:</b></td><td>(.*?)<small>").getMatch(0);     
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
    
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }
}
