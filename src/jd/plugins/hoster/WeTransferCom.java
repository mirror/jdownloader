//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.LinkedHashMap;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDHexUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "http://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}" })
public class WeTransferCom extends PluginForHost {

    private String security_hash = null;
    private String id_main       = null;
    private String id_single     = null;
    private String dllink        = null;
    private String param         = null;

    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wetransfer.info/terms/";
    }

    private String getAMFRequest() {
        final String data = "0A000000020200" + getHexLength(id_main) + JDHexUtils.getHexString(id_main) + "0200" + getHexLength(security_hash) + JDHexUtils.getHexString(security_hash);
        return JDHexUtils.toString("000000000001002177657472616E736665722E446F776E6C6F61642E636865636B446F776E6C6F616400022F31000000" + getHexLength(JDHexUtils.toString(data)) + data);
    }

    private String getHexLength(final String s) {
        final String result = Integer.toHexString(s.length());
        return result.length() % 2 > 0 ? "0" + result : result;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBR(final Browser br) {
        br.addAllowedResponseCodes(new int[] { 410, 503 });
        br.setCookie("wetransfer.com", "wt_tandc", "20170208");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = prepBR(new Browser());
        setBrowserExclusive();
        final String[] dlinfo = link.getDownloadURL().replace("http://wetransferdecrypted/", "").split("/");
        id_main = dlinfo[0];
        security_hash = dlinfo[1];
        id_single = dlinfo[2];
        if (security_hash == null || id_main == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean withRecepient = false;
        if (withRecepient) {
            br.getPage("https://" + this.getHost() + "/api/ui/transfers/" + id_main + "/" + security_hash + "/download?recipient_id=" + security_hash);
        } else {
            /*
             * https://wetransfer.com/api/ui/transfers/0d9f92839e6772d79ce5ee4256d936a620170524074451/4fd9d9/files/
             * cbd496b1ef50e4ff5d98ffb9337e394920170524074451/download
             */
            final String referer = link.getStringProperty("referer", null);
            if (referer != null) {
                br.getPage(referer);
            }
            // now without id_single
            br.getHeaders().put("Accept", "application/json");
            br.getHeaders().put("Content-Type", "application/json");
            br.postPageRaw("https://" + this.getHost() + "/api/ui/transfers/" + this.id_main + "/" + this.security_hash + "/download", "{\"file_ids\":[\"" + this.id_single + "\"]}");
        }
        if ("invalid_transfer".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = PluginJSonUtils.getJsonValue(br, "direct_link");
        String filename1 = null;
        long filesize1 = 0;
        if (dllink == null) {
            // 20160415-raztoki
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            final LinkedHashMap<String, Object> field = (LinkedHashMap<String, Object>) entries.get("fields");
            final String action = (String) JavaScriptEngineFactory.walkJson(entries, "formdata/action");
            final String method = (String) JavaScriptEngineFactory.walkJson(entries, "formdata/method");
            if (action != null && field != null) {
                if ("GET".equalsIgnoreCase(method)) {
                    dllink = action + "?" + processJson(field.toString().substring(1, field.toString().length() - 1));
                } else {
                    dllink = action;
                    param = processJson(field.toString().substring(1, field.toString().length() - 1));
                }
            }
        }
        if (dllink != null) {
            String filename = new Regex(Encoding.htmlDecode(dllink), "filename=([^&]+)").getMatch(0);
            if (filename == null) {
                filename = PluginJSonUtils.getJsonValue(br, "filename");
            }
            if (filename == null) {
                filename = filename1;
            }
            if (filename != null) {
                /* 2017-05-30: Filename will usually be set already within decrypter. */
                link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            }
            if (filesize1 > 0) {
                link.setDownloadSize(filesize1);
            }
        } else {
            /** Old way */
            // AMF-Request
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Accept-Charset", null);
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Content-Type", "application/x-amf");
            br.getHeaders().put("Referer", "https://www.wetransfer.com/index.swf?nocache=" + String.valueOf(System.currentTimeMillis() / 1000));
            br.postPageRaw("https://v1.wetransfer.com/amfphp/gateway.php", getAMFRequest());
            // successfully request?
            // final int rC = br.getHttpConnection().getResponseCode();
            // if (rC != 200) {
            // logger.warning("File not found! Link: " + dlink);
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            final StringBuffer sb = new StringBuffer();
            /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
            for (final byte element : br.toString().getBytes()) {
                if (element < 127) {
                    if (element > 31) {
                        sb.append((char) element);
                    } else {
                        sb.append("#");
                    }
                }
            }
            final String result = sb.toString();
            if (new Regex(result, "(download_error_no_download|download_error_file_expired)").matches()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = new Regex(result, "#filename[#]+\\$?([^<>#]+)").getMatch(0);
            if (filename == null) {
                filename = filename1;
            }
            if (filesize1 == 0) {
                final String filesize = new Regex(result, "#size[#]+(\\d+)[#]+").getMatch(0);
                if (filesize != null) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            } else {
                link.setDownloadSize(filesize1);
            }
            dllink = new Regex(result, "#awslink[#]+\\??([^<>#]+)").getMatch(0);
            if (filename != null) {
                /* 2017-05-30: Filename will usually be set already within decrypter. */
                link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // More chunks are possible for some links but not for all
        if (param != null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, param, true, -2);
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, -2);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error while downloading your file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private String processJson(final String s) {
        String fields = s;
        final String callback = new Regex(fields, "callback=(\\{.*?\\}\\}$)").getMatch(0);
        fields = fields.replace(callback, "-JDTEMPREMOVED-");
        String postString = fields.replace(", ", "&");
        postString = postString.replace("-JDTEMPREMOVED-", Encoding.urlEncode(callback));
        return postString;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}