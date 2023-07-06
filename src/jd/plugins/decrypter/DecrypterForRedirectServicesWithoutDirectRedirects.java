//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "animeforce.org", "sipkur.net", "otrkeyfinder.com", "is.gd", "djurl.com", "q32.ru", "adfoc.us", "lnk.co", "myurl.in", "href.hu", "songspk.info", "academicearth.org", "tm-exchange.com", "mafia.to" }, urls = { "http://(?:www\\.)?animeforce\\.org/ds(?:\\d+)?\\.php\\?file=.+", "http://sipkur\\.net/[a-z0-9\\-_]+\\.html", "https?://otrkeyfinder\\.com/de/go\\-to\\-mirror\\?otrkey=.+", "https?://(?:www\\.)?is\\.gd/[a-zA-Z0-9]+", "http://djurl\\.com/[A-Za-z0-9]+", "http://q32\\.ru/\\d+/c/[A-Za-z0-9\\-_]+", "http://(www\\.)?adfoc\\.us/(serve/\\?id=[a-z0-9]+|(?!serve|privacy|terms)[a-z0-9]+)", "http://(www\\.)?lnk\\.co/[A-Za-z0-9]+", "http://(www\\.)?protect\\.myurl\\.in/[A-Za-z0-9]+", "http://href\\.hu/x/[a-zA-Z0-9\\.]+",
        "http://[\\w\\.]*?(link\\.songs\\.pk/(popsong|song1|bhangra)\\.php\\?songid=|songspk\\.info/ghazals/download/ghazals\\.php\\?id=|link\\.songspk\\.help/\\S+/download\\.php\\?id=)[0-9]+", "http://[\\w\\.]*?academicearth\\.org/lectures/.{2,}", "http://[\\w\\.]*?tm-exchange\\.com/(get\\.aspx\\?action=trackgbx|\\?action=trackshow)\\&id=\\d+", "http://[\\w\\.]*?mafia\\.to/download-[a-z0-9]+\\.cfm" })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        if ("songspk.info".equals(this.getHost())) {
            return new String[] { "link.songspk.help", "songspk.info" };
        }
        return super.siteSupportedNames();
    }

    public DecrypterForRedirectServicesWithoutDirectRedirects(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl();
        br.setFollowRedirects(false);
        br.setReadTimeout(60 * 1000);
        boolean dh = false;
        boolean offline = false;
        String finallink = null;
        String finalfilename = null;
        /* Some links don't have to be accessed (here) */
        try {
            if (!new Regex(parameter, "tm-exchange\\.com/|is\\.gd/|animeforce\\.org/").matches()) {
                getPage(parameter);
            }
            if (parameter.contains("link.songs.pk/") || parameter.contains("songspk.info/ghazals/download/ghazals.php?id=") || parameter.contains("link.songspk.help/")) {
                finallink = br.getRedirectLocation();
                dh = true;
            } else if (parameter.contains("academicearth.org/")) {
                if (!(br.getRedirectLocation() != null && br.getRedirectLocation().contains("users/login"))) {
                    if (br.containsHTML(">Looks like the Internet may require a little disciplinary action")) {
                        offline = true;
                    }
                    finallink = br.getRegex("flashVars\\.flvURL = \"(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        finallink = br.getRegex("<div><embed src=\"(.*?)\"").getMatch(0);
                    }
                    if (finallink != null) {
                        if (!finallink.contains("blip.tv") && !finallink.contains("youtube")) {
                            throw new DecrypterException("Found unsupported link in link: " + parameter);
                        }
                        if (finallink.contains("blip.tv/")) {
                            br.getPage(finallink);
                            finallink = br.getRedirectLocation();
                            dh = true;
                        }
                    }
                } else {
                    throw new DecrypterException("Login required to download link: " + parameter);
                }
            } else if (parameter.contains("tm-exchange.com/")) {
                finallink = "directhttp://" + parameter.replace("?action=trackshow", "get.aspx?action=trackgbx");
            } else if (parameter.contains("mafia.to/download")) {
                br.getPage(parameter.replace("download-", "dl-"));
                finallink = br.getRedirectLocation();
            } else if (parameter.contains("view.stern.de/de/")) {
                br.setFollowRedirects(true);
                br.getPage(parameter.replace("/picture/", "/original/"));
                if (br.containsHTML("/erotikfilter/")) {
                    br.postPage(br.getURL(), "savefilter=1&referer=" + Encoding.urlEncode(parameter.replace("/original/", "/picture/")) + "%3Fr%3D1%26g%3Dall");
                    br.getPage(parameter.replace("/picture/", "/original/"));
                }
                finallink = br.getRegex("<div class=\"ImgBig\" style=\"width:\\d+px\">[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\"(http://view\\.stern\\.de/de/original/([a-z0-9]+/)?\\d+/.*?\\..{3,4})\"").getMatch(0);
                }
                dh = true;
            } else if (parameter.contains("protect.myurl.in/")) {
                finallink = br.getRegex("<iframe scrolling=\"(yes|no)\" src=\"(.*?)\"").getMatch(1);
            } else if (parameter.contains("lnk.co/")) {
                finallink = br.getRedirectLocation();
                if (finallink == null) {
                    finallink = br.getRegex("window\\.top\\.location = \\'srh\\.php\\?u=(http://[^<>\"]*?)\\'").getMatch(0);
                }
                if (finallink == null) {
                    finallink = br.getRegex("style=\\'pointer\\-events: none;\\' id=\\'dest\\' src=\"(http://[^<>\"]*?)\"").getMatch(0);
                }
                if (finallink == null) {
                    finallink = br.getRegex("linkurl.*?counter.*?linkurl' href=\"(http://[^<>\"]*?)\"").getMatch(0);
                }
            } else if (parameter.contains("adfoc.us/")) {
                String id = new Regex(parameter, ".us/(.+)").getMatch(0);
                if ("forum".equalsIgnoreCase(id) || "support".equalsIgnoreCase(id) || "self".equalsIgnoreCase(id) || "user".equalsIgnoreCase(id) || "payout".equalsIgnoreCase(id) || "api".equalsIgnoreCase(id) || "js".equalsIgnoreCase(id) || "ajax".equalsIgnoreCase(id) || "faq".equalsIgnoreCase(id) || "1How".equalsIgnoreCase(id) || "tickets".equalsIgnoreCase(id) || "advertise".equalsIgnoreCase(id)) {
                    logger.info("Invalid link: " + parameter);
                    return decryptedLinks;
                }
                br.setFollowRedirects(true);
                getPage(parameter);
                br.setFollowRedirects(false);
                if (br.containsHTML(">403 Forbidden<")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                } else if (br.containsHTML("No htmlCode read")) {
                    logger.info("Link offline (server error): " + parameter);
                    return decryptedLinks;
                } else if (br.getURL().equals("http://adfoc.us/")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                String click = br.getRegex("var click_url = \"(https?://[^\"]+)").getMatch(0);
                if (click != null && click.equals(parameter)) {
                    click = null;
                }
                if (click == null) {
                    click = br.getRegex("(http://adfoc\\.us/serve/click/\\?id=[a-z0-9]+\\&servehash=[a-z0-9]+\\&timestamp=\\d+)").getMatch(0);
                }
                if (click != null && click.contains("adfoc.us/") && !click.contains(id)) {
                    /* adfoc link leads to another adfoc link */
                    decryptedLinks.add(createDownloadlink(click));
                    return decryptedLinks;
                } else if (click != null && click.contains("adfoc.us/")) {
                    br.getHeaders().put("Referer", parameter);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    getPage(HTMLEntities.unhtmlentities(click));
                    if (br.getRedirectLocation() != null && !br.getRedirectLocation().matches("http://adfoc.us/")) {
                        finallink = br.getRedirectLocation();
                    }
                } else {
                    finallink = click;
                }
            } else if (parameter.contains("gabber.od.ua/")) {
                finallink = br.getRegex("Download link:<br><br><br><a href=\\'([^<>\"\\']+)\\'").getMatch(0);
            } else if (parameter.contains("q32.ru/")) {
                final Form dlForm = br.getForm(0);
                if (dlForm != null) {
                    br.submitForm(dlForm);
                    finallink = br.getRegex("http\\-equiv=\"Refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0);
                }
            } else if (parameter.contains("madlink.sk/") || parameter.contains("m-l.sk/")) {
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://madlink.sk/ajax/check_redirect.php", "link=" + new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
                finallink = br.toString();
                if (finallink == null || finallink.length() > 500 || !finallink.startsWith("http")) {
                    finallink = null;
                }
            } else if (parameter.contains("djurl.com/")) {
                finallink = br.getRedirectLocation();
                if (finallink == null) {
                    finallink = br.getRegex("var finalStr = \"(.*?)\";").getMatch(0);
                }
                if (finallink != null) {
                    finallink = Encoding.Base64Decode(finallink);
                } else {
                    finallink = br.getRegex("var finalLink = \"(.*?)\";").getMatch(0);
                    if (finallink == null) {
                        finallink = br.getRegex("<a href\\s*=\\s*('|\")([^\r\n]*/\\?r=.*?)\\1[^>]*>Close</a>").getMatch(1);
                    }
                }
                if (finallink == null && (br.containsHTML("<title>DJURL\\.COM \\- The DJ Link Shortener</title>") || br.getHttpConnection().getResponseCode() == 404)) {
                    offline = true;
                }
            } else if (parameter.contains("is.gd/")) {
                final String fid = new Regex(parameter, "[^/]+//[^/]+/(.+)").getMatch(0);
                parameter = "https://is.gd/" + fid;
                getPage(parameter);
                finallink = br.getRedirectLocation();
                if (finallink == null || finallink.contains("is.gd/")) {
                    finallink = br.getRegex("the destination shown: \\-<br /><a href=\"(http[^<>\"]*?)\"").getMatch(0);
                }
                if (br.containsHTML(">Sorry, we couldn't find the shortened URL you requested") || br.containsHTML(">Link Disabled<") || parameter.equals("http://is.gd") || parameter.equals("http://www.is.gd")) {
                    offline = true;
                }
            } else if (parameter.contains("otrkeyfinder.com/")) {
                final String redirect = this.br.getRedirectLocation();
                if (redirect != null && !redirect.contains("otrkeyfinder.com/")) {
                    /* 2016-12-16 */
                    finallink = redirect;
                } else {
                    finallink = br.getRegex("id=\"mirror\\-link\" href=\"(http[^<>\"]*?)\"").getMatch(0);
                    if (finallink != null) {
                        finallink = Encoding.htmlDecode(finallink);
                    } else {
                        offline = true;
                    }
                }
            } else if (parameter.contains("sipkur.net/")) {
                finallink = this.br.getRegex("onclick=\"window\\.open\\(\\'(http[^<>\"]*?)\\'").getMatch(0);
            } else if (parameter.contains("animeforce.org/")) {
                // use cloudflare
                getPage(parameter);
                if (br.getRedirectLocation() != null) {
                    br.setFollowRedirects(true);
                    getPage(br.getRedirectLocation());
                }
                finallink = br.getRegex("href=\"(http:[^<>\"]+/)?(http[^<>\"]*?)\" target=\"_blank\"\\s?>Download</a>").getMatch(1);
                if (finallink == null) {
                    if (br.containsHTML(">Il file che stai provando a scaricare non esiste,<br>oppure deve essere ancora caricato<|>o semplicemente hai cliccato/digitato un link sbagliato\\s*<")) {
                        return decryptedLinks;
                    }
                }
            }
        } catch (final SocketTimeoutException e) {
            logger.info("Link offline (timeout): " + parameter);
            return decryptedLinks;
        } catch (final BrowserException e) {
            logger.info("BrowserException occured - timeout or server error: " + parameter);
            return decryptedLinks;
        }
        if ("http://adfoc.us/".equals(br.getRedirectLocation())) {
            offline = true;
        }
        if (offline) {
            logger.info("Link offline: " + parameter);
            if (decryptedLinks.isEmpty()) {
                decryptedLinks.add(this.createOfflinelink(parameter));
            }
            return decryptedLinks;
        }
        if (finallink == null) {
            logger.info("DecrypterForRedirectServicesWithoutDirectRedirects says \"Out of date\" for link: " + parameter);
            return null;
        }
        if (dh) {
            finallink = "directhttp://" + finallink;
        }
        final DownloadLink dl = createDownloadlink(finallink);
        if (finalfilename != null) {
            dl.setFinalFileName(finalfilename);
        }
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    private String getHttpMetaRefreshURL() {
        return br.getRegex("http-equiv=\"refresh\" content=\"\\d+; url=(http[^<>\"]*?)\"").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.NotApplicable;
    }
}