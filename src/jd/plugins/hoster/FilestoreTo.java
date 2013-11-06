//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestore.to" }, urls = { "http://(www\\.)?filestore\\.to/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class FilestoreTo extends PluginForHost {

    private String aBrowser = "";

    public FilestoreTo(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l);

    }

    @Override
    public String getAGBLink() {
        return "http://www.filestore.to/?p=terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    // Hm da stehts drinne: http://filestore.to/script/241013.js
    final String KK  = "LDC";
    final String KK2 = "LC";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        // Many other browsers seem to be blocked
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        br.setCustomCharset("utf-8");
        final String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 3; i++) {
            try {
                br.getPage(url);
            } catch (final Exception e) {
                continue;
            }
            if (br.containsHTML(">Download\\-Datei wurde nicht gefunden<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.containsHTML(">Download\\-Datei wurde gesperrt<")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (br.containsHTML("Entweder wurde die Datei von unseren Servern entfernt oder der Download-Link war")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            haveFun();
            downloadName = new Regex(aBrowser, zStatistic("f980f9f7fa02cce21b91b694df0640fc299570bb085ed8c71a4f5f3a1538868158d8fe3c8800e26f211652e9ef741972e4722ccac7c86638ac72d2e36f59363d5aa3cd7f5826fe9c21ac2eb95260c8b445fe8bdb6af198a19a8d2ec629657911c7763f016e8e81633b4605fa1b3f167f02b28e2031f8709081c0662e4e6f5c958e6249e7c93cd996324a0cdb")).getMatch(1);
            if (downloadName == null) {
                downloadName = new Regex(
                        aBrowser,
                        zStatistic("fc8dfba6fa07c9e61a97b799de5443fa289071ba0c5bd8ca1a1b5f6e15398284598ffc3f885ce63c231750eaee721973e4242dcfc5cf6635af72d6b36c5f326959a4c92f5f7cff9827a828b8523ccfb041ae8ad96fa99ffd9cd82a9529657847c37b3d006fdf84313f1701fd1f60152c07e18e2037fd74c0849b612b4e615dc68f344de3c864d8ce334f09817708c2e3d962031957c0b0a81c8238a56d6d739518715d0f345c7a141173c24fdf36db3119b47f2440f34dc00028a9f9d3f8535964def58c9e6e86309fe43d08a12274e0bad237e6f8e610ee720b9174cb7f2cf236dee2062bdd3acd2fd12b5533ed13ddd9794c5be927dbace1cf4752663c38f1045c06aebec50039dfedddb14c30edc2e6c45ea538121fd325bf553f22fd230fc82622fe3675c1a59357a3c2cb15936aef8a59801b477a273b2b8cd65c052970208e5b687057a0c5fc7736288b9247778e4ed949341278b3943018a5f035548c65be518296537eab2eaf79bbb1fa72280485b01413de8ff9af8a92addf272e047311543548c847a27b5fd6d573458a9e10a395a17efc93e093f4142d61078c453d2f36c54db384a5a20a1fe5ac5396b0aa78d2cbf70b5d86ad4f31ec31c06d55769486dc62f0364d79a0e25ddfd412da7ff4e33ad693616706cecce47714bdd768adc15a11501f0059da18889b2c130e434f1ede1d584b73cbfbfb2466e817e4f987f1715f9d8e0e1a5c02b77887adc53c2b35f178dba812f766225cbf2f37af92f9416adf529ea7528622158bb852cd12f04a75a0fa5459ead88d0bfb5cccf95536437a006e5d81ea8fa6eb6fab0cc27d32435f65deb4a7f9e18f82b51af31420e499698e7fb8cc9c59ccbc986b358ff7a767025cacf6cc83db988851b3d919576b20c0640681892f25e2591c4f4ba9e1b7af9264dd681c64afb24fa58f2c4a9dd5110554cb30cea93f3dfba214e66186810bdfd809718fc5db581bf63ebe8bd7126b5c17b865ab272d6a5b307381316198f08248fc754cecaac7909cefe82e092911fffe3caabc1ee8db0c99a889f4b99628582960f9a6771d4a3f6fbaaaa09e9c07f90fb9b28815a7a8fc8b13528abfd4c19b7d625394fee0d8729eb319d2d88ed43dccac2fdbb66a15d9f4b56dbef8e03a184989a592b80d87feeffa643a0ce0a08093a3fc5ca2253ca0aa8cdd36ff9e27ca564ac6ed5c98120c5c6697dcf01f4f4fc6960128368b22a58b74d32fd71cee0d93da6dd89ad370635a34773afb58eff4676bc24f681a5308adf20c8f2387ddddd1be092b72b5510b4c1d86ce541b85d1fd3d54f76ca894b4aa8b5f51ac9d9a2f9d033ce51d06a8782142543379d953919237c68ec9e25f82eac231af699ebc9e3d97ad89702ada218d191af14249666494e861fab20c17bcc2d0f3772a13d315f60286a9e3cf05120022d7e5834b01ea0da45e4bc83587542eda212887f2848d13c7b43f73ffbaffe852ba95c8e5a9586d0dd2ec68f15b158216d710de768287966f9ea8f86551fdefa4ad4baa31715bc0820df3d28cf3ba8cdcb66f7a742f45a2d3236c44c44225b69915284f50c4840e530fccfb02b2a5fb1be71700b74c83913bbd8cdb7714d84ad447032fb1bdb6f2f7e6bae4a78664f3d011e53d4890f0e808dd47393845a2cba1c3fc301461358790e4c4645544ac1876db9bfaa20b98b406897a4ea60b3038f87cc7386b41db1b0bd9cfb3b7d4eb5017874f8a4343627850d92d0f00af92c9360dbf6a95b7944c000bd4618d1dd60bc4d21316748c60db5cc2af07c1c6e18948d48338f4f660e0d6d9ff"))
                        .getMatch(0);
            }
            downloadSize = new Regex(aBrowser, zStatistic("f980f9f7fa02cce21b91b694de5243fa2cc274e9085ed99d181c5f32153f86815888fc338958e239204052bbee771c70e1212d9cc1c76360a977d6e66b04356f5af3c9285827ff9e27a92abd523ac8e844f98a8d6af09eab99d92d962e317d14c5273c506dd98365384001f61f3f")).getMatch(1);
            if (downloadSize == null) {
                downloadSize = new Regex(aBrowser, zStatistic("f980f8a0fa07c9b41f9cb2cedd0642fc2dc675b60d0ddcce1e125d39143f84d65b89fd698a5de03e204052bbed771d2fe07e")).getMatch(0);
            }
            if (downloadName != null) downloadLink.setName(Encoding.htmlDecode(downloadName.trim()));
            if (downloadSize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.").trim()));
            return AvailableStatus.TRUE;

        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String pwnage = br.getRegex("id=\"" + KK2 + "\" style=\"display:none\" wert=\"([^<>\"]*?)\"").getMatch(0);
        if (pwnage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String waittime = br.getRegex("Bitte warte (\\d+) Sekunden und starte dann").getMatch(0);
        final String dlink = "http://filestore.to/ajax/download.php?" + KK + "=";
        int wait = 10;
        if (waittime != null) {
            if (Integer.parseInt(waittime) < 61) {
                wait = Integer.parseInt(waittime);
            }
        }
        sleep(wait * 1001l, downloadLink);
        // If plugin breaks most times this link is changed
        br.getPage(dlink + pwnage);
        if (br.containsHTML("(Da hat etwas nicht geklappt|Wartezeit nicht eingehalten|Versuche es erneut)")) {
            logger.warning("FATAL waittime error!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        final String dllink = br.toString().replaceAll("%0D%0A", "").trim();
        if (!dllink.startsWith("http://")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void haveFun() throws Exception {
        final ArrayList<String> someStuff = new ArrayList<String>();
        final ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add(zStatistic("Rjk4MEZFQTBGQjA3Q0RFNzFCOTZCNkNFREU1MDQ2QUQyREM1NzRFOTA4NThEODlEMUExQjVFMzgxNDZGODNEMDVEODlGQzMyODgwMEUyMzgyMTE2NTRCRUVCMjAxRDc3RTEyMTI4Q0FDMTk4NjczNUFDMjBEMkU0NkY1RjM2Mzk1RkEyQzk3Mw=="));
        regexStuff.add(zStatistic("Rjk4MEZFQTBGRTU2QzlCNzFFQzJCM0M4REE1Qw=="));
        regexStuff.add(zStatistic("Rjk4MEZEQTdGRTBB"));
        regexStuff.add(zStatistic("Rjk4MEZEQTJGQzUyQzlFRg=="));
        for (final String aRegex : regexStuff) {
            aBrowser = br.toString();
            final String replaces[] = br.getRegex(aRegex).getColumn(0);
            if (replaces != null && replaces.length != 0) {
                for (final String dingdang : replaces) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (final String gaMing : someStuff) {
            aBrowser = aBrowser.replace(gaMing, "");
        }
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }

    private String zStatistic(String s) {
        s = Encoding.Base64Decode(s);
        final PluginForDecrypt adsplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
        return JDHexUtils.toString(jd.plugins.decrypter.LnkCrptWs.IMAGEREGEX(s));
    }

}