package jd.plugins.decrypt;

import java.util.ArrayList;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.nutils.OSDetector;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class BadongoCom extends PluginForDecrypt {
    static private String host = "badongo.com";

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setCookiesExclusive(true);
        br.clearCookies(host);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(parameter);
        parameter=parameter.replaceFirst("badongo.com", "badongo.viajd");
        if (!br.containsHTML("Diese Datei wurde gesplittet")) {
            DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter));
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
        } else {
            String[] links = br.getRegex("<div class=\"m\">Download Teil(.*?)</div>").getColumn(0);
            progress.setRange(links.length);
           for(String partId: links){               
              
                DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter)+"?"+partId.trim());
                dlLink.setName(dlLink.getName()+"."+partId.trim());
                dlLink.setProperty("type", "split");
                dlLink.setProperty("part", Integer.parseInt(partId.trim()));
                dlLink.setProperty("parts", links.length);
                decryptedLinks.add(dlLink);
                progress.increase(1);
            }
           
           if(OSDetector.isWindows()){
           String bat=br.getRegex("(http\\:\\/\\/www\\.badongo\\.com\\/fm\\/\\d+\\/bat)").getMatch(0);
           DownloadLink dlLink = createDownloadlink(bat.replaceAll("http://","httpviajd://")+"#dummy.zip");
   
           decryptedLinks.add(dlLink);
           }else{
               String bat=br.getRegex("(http\\:\\/\\/www\\.badongo\\.com\\/fm\\/\\d+\\/sh)").getMatch(0);
               DownloadLink dlLink = createDownloadlink(bat.replaceAll("http://","httpviajd://")+"#dummy.zip");
       
               decryptedLinks.add(dlLink); 
           }
          
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}