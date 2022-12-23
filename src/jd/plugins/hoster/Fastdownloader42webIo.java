package jd.plugins.hoster;

import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fastdownloader.42web.io" }, urls = { "https?://fastdownloader\\.42web\\.io/download\\.php\\?h=[a-f0-9]+/[^/\\?]+" })
public class Fastdownloader42webIo extends DirectHTTP {
    public Fastdownloader42webIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected AvailableStatus requestFileInformation(final DownloadLink downloadLink, int retry, Set<String> optionSet) throws Exception {
        if (retry == 0) {
            br.setCookiesExclusive(true);
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            brc.getPage(getDownloadURL(downloadLink));
            final byte[] a = HexFormatter.hexToByteArray(brc.getRegex("a\\s*=\\s*toNumbers\\(\"(.*?)\"").getMatch(0));
            final byte[] b = HexFormatter.hexToByteArray(brc.getRegex("b\\s*=\\s*toNumbers\\(\"(.*?)\"").getMatch(0));
            final byte[] c = HexFormatter.hexToByteArray(brc.getRegex("c\\s*=\\s*toNumbers\\(\"(.*?)\"").getMatch(0));
            if (a == null || b == null || c == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final SecretKey aesKey = new SecretKeySpec(a, "AES");
            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(b));
            byte[] d = cipher.doFinal(c);
            br.setCookie(brc.getHost(), "__test", HexFormatter.byteArrayToHex(d));
        }
        return super.requestFileInformation(downloadLink, retry, optionSet);
    }

    @Override
    protected int getMaxChunks(DownloadLink downloadLink, Set<String> optionSet, int chunks) {
        return -4;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    protected void setBrowserExclusive() {
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[0];
    }

    @Override
    public String getHost(final DownloadLink link, Account account, boolean includeSubdomain) {
        return "fastdownloader.42web.io";
    }
}
