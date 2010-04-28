//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.Youtube;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youtube.com" }, urls = { "http://[\\w\\.]*?youtube\\.com/(watch.*?v=[a-z-_A-Z0-9]+|view_play_list\\?p=[a-z-_A-Z0-9]+(.*?page=\\d+)?)" }, flags = { 0 })
public class TbCm extends PluginForDecrypt {

    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swf_Args.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);
    HashMap<ConversionMode, ArrayList<Info>> possibleconverts = null;

    public TbCm(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 100);
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    private boolean login(Account account) throws Exception {
        this.setBrowserExclusive();
        PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        try {
            if (plugin != null) {
                ((Youtube) plugin).login(account, br);
            } else
                return false;
        } catch (PluginException e) {
            account.setEnabled(false);
            account.setValid(false);
            return false;
        }
        return true;
    }

    private void addVideosCurrentPage(ArrayList<DownloadLink> links) {
        String[] videos = br.getRegex("id=\"video-long-.*?href=\"(/watch\\?v=.*?)&").getColumn(0);
        for (String video : videos) {
            links.add(this.createDownloadlink("http://www.youtube.com" + video));
        }
    }

    static class Info {
        public String link;
        public long size;
        public int fmt;
        public String desc;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        possibleconverts = new HashMap<ConversionMode, ArrayList<Info>>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("watch#!v", "watch?v");
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");
        if (parameter.contains("watch#")) {
            parameter = parameter.replace("watch#", "watch?");
        }
        if (parameter.contains("view_play_list")) {
            br.getPage(parameter);
            addVideosCurrentPage(decryptedLinks);
            if (!parameter.contains("page=")) {
                String[] pages = br.getRegex("<a href=(\"|')(http://www.youtube.com/view_play_list\\?p=.*?page=\\d+)(\"|')").getColumn(1);
                for (int i = 0; i < pages.length - 1; i++) {
                    br.getPage(pages[i]);
                    addVideosCurrentPage(decryptedLinks);
                }
            }
        } else {
            boolean prem = false;
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("youtube.com");
            if (accounts != null && accounts.size() != 0) prem = login(accounts.get(0));

            try {
                if (StreamingShareLink.matcher(parameter).matches()) {
                    // StreamingShareLink

                    String[] info = new Regex(parameter, StreamingShareLink).getMatches()[0];

                    for (String debug : info) {
                        logger.info(debug);
                    }
                    DownloadLink thislink = createDownloadlink(info[1]);
                    thislink.setBrowserUrl(info[2]);
                    thislink.setFinalFileName(info[0]);
                    thislink.setSourcePluginComment("Convert to " + (ConversionMode.valueOf(info[3])).getText());
                    thislink.setProperty("convertto", info[3]);

                    decryptedLinks.add(thislink);
                    return decryptedLinks;
                }
                boolean oldLayout = false;
                HashMap<Integer, String> linksFound = getLinks(parameter, prem, this.br);
                if (linksFound != null) oldLayout = true;
                if (linksFound == null) linksFound = getLinksNew(parameter, prem, this.br);
                if ((linksFound == null || linksFound.size() == 0) && br.containsHTML("verify_age")) throw new DecrypterException(DecrypterException.ACCOUNT);
                if (linksFound == null || linksFound.size() == 0) throw new DecrypterException("Video no longer available");
                String name = Encoding.htmlDecode(br.getRegex(YT_FILENAME).getMatch(0).trim());
                /* check for wished formats first */
                HashMap<Integer, String> links = new HashMap<Integer, String>();
                if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEO3GP) && linksFound.keySet().contains(13)) {
                    links.put(13, linksFound.get(13));
                } else if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4) && (linksFound.keySet().contains(18) || linksFound.keySet().contains(22) || linksFound.keySet().contains(34) || linksFound.keySet().contains(35) || linksFound.keySet().contains(37))) {
                    Integer mp4[] = new Integer[] { 18, 22, 34, 35, 37 };
                    for (Integer f : mp4) {
                        if (linksFound.containsKey(f)) {
                            links.put(f, linksFound.get(f));
                        }
                    }
                } else {
                    links = linksFound;
                }
                /* Konvertierungsmöglichkeiten adden */
                for (Integer format : links.keySet()) {
                    String link = links.get(format);
                    String dlLink;
                    if (format == 0) {
                        dlLink = link;
                    } else {
                        dlLink = link + (oldLayout == true ? "&fmt=" + format : "");
                    }
                    switch (format) {
                    case 18:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(18)", format);
                            br.getHttpConnection().disconnect();
                        }
                        break;
                    case 22:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(22,720p)", format);
                            br.getHttpConnection().disconnect();
                        }
                        break;
                    case 34:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "(HQ)", format);
                            br.getHttpConnection().disconnect();
                        }
                        break;
                    case 35:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(35,720p/HQ)", format);
                            br.getHttpConnection().disconnect();
                        }
                        break;
                    case 37:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOMP4, dlLink, br.getHttpConnection().getLongContentLength(), "(37,1080p)", format);
                            br.getHttpConnection().disconnect();
                        }
                        if (ConvertDialog.getKeeped().contains(ConversionMode.VIDEOMP4)) break;
                        break;
                    case 13:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEO3GP, dlLink, br.getHttpConnection().getLongContentLength(), "(13)", format);
                            br.getHttpConnection().disconnect();
                        }
                        break;
                    default:
                        if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                            addtopos(ConversionMode.VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                            addtopos(ConversionMode.AUDIOMP3, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                            addtopos(ConversionMode.AUDIOMP3_AND_VIDEOFLV, dlLink, br.getHttpConnection().getLongContentLength(), "", format);
                            br.getHttpConnection().disconnect();
                        }
                    }
                }

                ConversionMode convertTo = Plugin.showDisplayDialog(new ArrayList<ConversionMode>(possibleconverts.keySet()), name, param);

                for (Info info : possibleconverts.get(convertTo)) {
                    DownloadLink thislink = createDownloadlink("INSIDEJD" + info.link);
                    thislink.setBrowserUrl(parameter);
                    thislink.setFinalFileName(name + info.desc + ".tmp");
                    thislink.setSourcePluginComment("Convert to " + convertTo.getText());
                    thislink.setProperty("size", Long.valueOf(info.size));
                    thislink.setProperty("name", name + info.desc + ".tmp");
                    thislink.setProperty("convertto", convertTo.name());
                    thislink.setProperty("videolink", parameter);
                    thislink.setProperty("valid", true);
                    thislink.setProperty("fmtNew", info.fmt);
                    decryptedLinks.add(thislink);
                }
            } catch (IOException e) {
                br.getHttpConnection().disconnect();
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                return null;
            }
        }
        return decryptedLinks;
    }

    public HashMap<Integer, String> getLinksNew(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setFollowRedirects(true);
        br.getPage(video);
        /* age verify with activated premium? */
        if (br.containsHTML("verify_age") && prem) {
            String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
            String url = br.getURL();
            LinkedHashMap<String, String> p = Request.parseQuery(url);
            String next = p.get("next_url");
            Form form = new Form();
            form.setAction(url);
            form.setMethod(MethodType.POST);
            form.put("next_url", "%2F" + next.substring(1));
            form.put("action_confirm", "Confirm+Birth+Date");
            form.put("session_token", Encoding.urlEncode(session_token));
            br.submitForm(form);
            if (br.getCookie("http://www.youtube.com", "is_adult") == null) return null;
        } else if (br.containsHTML("verify_age")) {
            /* no account used, cannot decrypt link */
            return null;
        }
        HashMap<Integer, String> links = new HashMap<Integer, String>();

        /* new site layout */
        String data = br.getRegex("swfHTML.*?\" : \"(.*?)\";").getMatch(0);
        // data = Encoding.urlDecode(data, true);
        String linksData[][] = new Regex(data, "(\\d+)%7C(http.*?)(&|%2C)").getMatches();
        for (String linkData[] : linksData) {
            if (!links.containsKey(linkData[0])) links.put(Integer.parseInt(linkData[0]), Encoding.htmlDecode(Encoding.urlDecode(linkData[1], true)));
        }
        return links;
    }

    public HashMap<Integer, String> getLinks(String video, boolean prem, Browser br) throws Exception {
        if (br == null) br = this.br;
        br.setFollowRedirects(true);
        br.getPage(video);
        /* age verify with activated premium? */
        if (br.containsHTML("verify_age") && prem) {
            String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
            String url = br.getURL();
            LinkedHashMap<String, String> p = Request.parseQuery(url);
            String next = p.get("next_url");
            Form form = new Form();
            form.setAction(url);
            form.setMethod(MethodType.POST);
            form.put("next_url", "%2F" + next.substring(1));
            form.put("action_confirm", "Confirm+Birth+Date");
            form.put("session_token", Encoding.urlEncode(session_token));
            br.submitForm(form);
            if (br.getCookie("http://www.youtube.com", "is_adult") == null) return null;
        } else if (br.containsHTML("verify_age")) {
            /* no account used, cannot decrypt link */
            return null;
        }
        String video_id = "";
        String t = "";
        String match = br.getRegex(patternswfArgs).getMatch(0);
        if (match == null) return null;

        /* DownloadUrl holen */
        String[] lineSub = match.split(",|:");
        for (int i = 0; i < lineSub.length; i++) {
            String s = lineSub[i];
            if (s.indexOf(VIDEO_ID) > -1) {
                video_id = clean(lineSub[i + 1]);
            }
            if (s.indexOf(T) > -1) {
                t = clean(lineSub[i + 1]);
            }
        }
        HashMap<Integer, String> links = new HashMap<Integer, String>();
        String url = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;
        links.put(0, url);
        links.put(22, url);
        links.put(34, url);
        links.put(35, url);
        links.put(37, url);
        links.put(13, url);
        return links;
    }

    private void addtopos(ConversionMode mode, String link, long size, String desc, int fmt) {
        ArrayList<Info> info = possibleconverts.get(mode);
        if (info == null) {
            info = new ArrayList<Info>();
            possibleconverts.put(mode, info);
        }
        Info tmp = new Info();
        tmp.link = link;
        tmp.size = size;
        tmp.desc = desc;
        tmp.fmt = fmt;
        info.add(tmp);
    }

}
