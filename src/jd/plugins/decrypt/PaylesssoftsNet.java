package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class PaylesssoftsNet extends PluginForDecrypt {
	public PaylesssoftsNet(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
		ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        br.setFollowRedirects(true);
        br.getPage(param);
        param = br.getURL();
        br.setFollowRedirects(false);
        
        String[] worker = param.split("=");
        String code = worker[1].substring(0,3);
        String id = worker[1].substring(3);
        HashMap<String, String> post = new HashMap<String, String>();
        post.put("code", code);
        post.put("id", id);
        String rsOrMega = new Regex(param, "\\.net\\/(.*?)\\/").getMatch(0);
        br.postPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngenerate.php", post);
        
        //If Uploader posted not existent URL, Decrypter reports a warning which we catch here to get the link
        String failedUrl = br.getRegex("<b>Warning<\\/b>\\:  file\\((.*?)\\)\\:").getMatch(0);
        if (failedUrl != null) {
        	decryptedLinks.add(createDownloadlink(failedUrl));
    		return decryptedLinks;
        }
        
        br.getPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngetfile.php");
        String finalurl = br.getRegex("<INPUT type=hidden value=(.*?) name=link>").getMatch(0);
        decryptedLinks.add(createDownloadlink(finalurl));
		return decryptedLinks;
	}

	@Override
	public String getVersion() {
		return getVersion("$Revision: 4839 $");
	}

}
