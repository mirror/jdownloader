package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinksafeInfo extends PluginForDecrypt {

    public LinksafeInfo(PluginWrapper wrapper) {
        super(wrapper);
    }
    
	@Override
	public ArrayList<DownloadLink> decryptIt(CryptedLink param,
			ProgressController progress) throws Exception {
		
		ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        
        progress.setRange(0);
        
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
        
        String matchText = "posli\\(\\\"([0-9]+)\\\",\\\"([0-9]+)\\\"\\)";
        
		String downloadId = br.getRegex(matchText).getMatch(1);
        Pattern pattern = Pattern.compile(matchText, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        
        String[] fileIds = new Regex(page,pattern).getColumn(0);
        progress.addToMax(fileIds.length);
        for (String fileId : fileIds) {
        	br.getPage("http://www.linksafe.info/posli.php?match="+fileId+"&id="+downloadId);
        	decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(br.getURL())));
        	progress.increase(1);
        }
                
        return decryptedLinks;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

}
