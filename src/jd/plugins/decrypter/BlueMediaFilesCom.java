package jd.plugins.decrypter;

import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bluemediafiles.com" }, urls = { "https?://(?:www\\.)?bluemediafiles\\.com/url-generator\\.php\\?url=.+" })
public class BlueMediaFilesCom extends PluginForDecrypt {
    public BlueMediaFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        String link = br.getRegex("Goroi_n_Create_Button\\s*\\(\"(.*?)\"").getMatch(0);
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            final ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = mgr.getEngineByName("JavaScript");
            engine.eval("var _bluemediafiles_decodeKey=function(encoded) {var key='';for (var i=encoded.length/2 - 5;i>= 0;i=i-2){key+=encoded[i];}for(i=encoded.length/2 + 4;i<encoded.length;i=i+2){key+=encoded[i];}return key;};");
            engine.eval("var result=_bluemediafiles_decodeKey(\"" + link + "\");");
            link = StringUtils.valueOfOrNull(engine.get("result"));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        br.getPage("/get-url.php?url=" + URLEncode.encodeURIComponent(link));
        final String redirect = br.getRedirectLocation();
        if (redirect != null && !canHandle(redirect)) {
            ret.add(createDownloadlink(redirect));
            return ret;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }
}
