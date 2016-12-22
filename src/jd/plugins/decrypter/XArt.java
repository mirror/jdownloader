//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.XArtCom;
import jd.plugins.hoster.XArtCom.XArtConfigInterface;
import jd.utils.JDUtilities;

import org.appwork.utils.Files;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "x-art.com" }, urls = { "^https?://(www\\.)?x-art\\.com/(members/)?.+" })
public class XArt extends PluginForDecrypt {

    public XArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static DownloadLink createDownloadlink(final Plugin plugin, final String contentURL, String url) throws MalformedURLException {
        final DownloadLink ret = new DownloadLink(null, null, plugin.getHost(), url.replaceFirst("x-art\\.com", "x-artdecrypted.com"), true);
        ret.setContentUrl(contentURL);
        ret.setProperty("pageURL", contentURL);
        ret.setFinalFileName(Plugin.getFileNameFromURL(new URL(url)));
        return ret;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public boolean add(final DownloadLink e) {
                try {
                    distribute(e);
                } catch (Throwable ee) {
                }
                return super.add(e);
            };

            @Override
            public boolean addAll(Collection<? extends DownloadLink> c) {
                for (final DownloadLink d : c) {
                    add(d);
                }
                return true;
            }
        };
        String url = parameter.getCryptedUrl();
        if (!url.contains("/members/") && !url.contains("/download/")) {
            url = url.replaceAll("x-art\\.com\\/", "x-art.com/members/");
        }
        if (url.matches(".*/members(/?|/messages/?|/community/?|/contact/?|/streaming/?|/index/?|/updates/?|/videos/?|/models/?|/favorite/?)$")) {
            return ret;
        }
        if (url.matches("(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)")) {
            ret.add(createDownloadlink(this, url, url));
            return ret;
        }
        boolean prem = false;
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    prem = this.login(n);
                    break;
                }
            }
        }
        if (!prem) {
            logger.warning("You need to use an Account with this provider");
            return ret;
        }
        if (url.matches("^https?://(www\\.)?x-art\\.com/(members/)?(videos|galleries)/[a-zA0-9\\-\\_]+/?$")) {
            parseUrl(br, this, ret, url, false);
        } else {
            br.getPage(url);
            final String[] subs = br.getRegex("(https?://(www\\.)?x-art\\.com/(members/)?(videos|galleries)/[a-zA0-9\\-\\_]+/?)").getColumn(0);
            final HashSet<String> dupe = new HashSet<String>();
            for (final String sub : subs) {
                try {
                    if (!isAbort()) {
                        if (dupe.add(sub)) {
                            parseUrl(br, this, ret, sub, false);
                        }
                    }
                } catch (Throwable ee) {
                }
            }
        }
        return ret;
    }

    public static void parseUrl(final Browser br, final Plugin plugin, final ArrayList<DownloadLink> ret, final String url, boolean returnAll) throws IOException {
        br.setFollowRedirects(true);
        br.getPage(url);
        final XArtConfigInterface cfg = PluginJsonConfig.get(XArtCom.XArtConfigInterface.class);
        String title = br.getRegex("<div class=\"small-12 medium-12 large-12 columns\">\\s*<h1>([a-zA-Z0-9\\_\\-\\ ]*)<\\/h1>").getMatch(0);
        if (url.matches("^https?://(www\\.)?x-art\\.com/(members/)?galleries/[a-zA0-9\\-\\_]+/?$")) {
            final String fid = new Regex(url, "/galleries/([^/]+)").getMatch(0);
            final FilePackage fp;
            if (title != null) {
                fp = FilePackage.getInstance();
                fp.setName(title);
            } else {
                fp = null;
            }
            final String links[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*\\.(mp4|wmv|mov|zip))\"").getColumn(0);
            final String lnks[] = br.getRegex("href=(\"|')(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)").getColumn(1);
            if (links != null) {
                for (String link : links) {
                    final DownloadLink dl = createDownloadlink(plugin, url, br.getURL(link).toString());
                    dl.setAvailable(true);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    ret.add(dl);
                }
            }
            if (lnks != null) {
                final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
                final HashMap<String, DownloadLink> qualities = new HashMap<String, DownloadLink>();
                for (final String link : lnks) {
                    final String quality_url = new Regex(link, "([A-Za-z]+)\\.zip").getMatch(0);
                    final DownloadLink dl = createDownloadlink(plugin, url, link);
                    dl.setAvailable(true);
                    dl.setProperty("imageID", fid);
                    dl.setProperty("quality", quality_url);
                    dl.setLinkID(plugin.getHost() + "://zip/" + fid + "/" + quality_url);
                    qualities.put(quality_url, dl);
                }
                final boolean allQualities = returnAll || (!cfg.isGrab1200pImagesVersionEnabled() && !cfg.isGrab2000pImagesVersionEnabled() && !cfg.isGrab4000pImagesVersionEnabled());
                final boolean bestOnly = !returnAll && cfg.isGrabBestImagesVersionEnabled();

                if ((allQualities || cfg.isGrab4000pImagesVersionEnabled()) && qualities.containsKey("lrg")) {
                    results.add(qualities.get("lrg"));
                }
                if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab2000pImagesVersionEnabled()) && qualities.containsKey("med")) {
                    results.add(qualities.get("med"));
                }
                if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab1200pImagesVersionEnabled()) && qualities.containsKey("sml")) {
                    results.add(qualities.get("sml"));
                }

                if (fp != null) {
                    fp.addLinks(results);
                }
                ret.addAll(results);
            }
        } else {
            /* Decrypt video */
            final String fid = new Regex(url, "/videos/([^/]+)").getMatch(0);
            if (fid == null) {
                return;
            }
            if (title == null) {
                /* Fallback */
                title = fid;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            final String htmldownload = br.getRegex("<ul[^>]*?id=\"drop\\-download\"[^>]*?>(.*?)</ul>").getMatch(0);
            final String[] dlinfo = htmldownload.split("</li>");
            final HashMap<String, List<DownloadLink>> qualities = new HashMap<String, List<DownloadLink>>();
            final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
            for (final String video : dlinfo) {
                final String dlurl = new Regex(video, "(/download/[^<>\"]+)\"").getMatch(0);
                final String filesize = new Regex(video, "\\((\\d+(?:\\.\\d+)? [A-Za-z]{2,5})\\)").getMatch(0);
                final String quality_url = dlurl != null ? new Regex(dlurl, "([A-Za-z0-9]+)\\.(mp4|mov|wmv)").getMatch(0) : null;
                if (dlurl == null || quality_url == null) {
                    continue;
                }
                final URL finalURL = br.getURL(dlurl);
                final DownloadLink dl = createDownloadlink(plugin, url, finalURL.toString());
                dl.setAvailable(true);
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                String ext = Files.getExtension(finalURL.getPath());
                if (ext == null) {
                    ext = "mp4";
                }
                dl.setFinalFileName(title + "_" + quality_url + "." + ext);
                dl.setProperty("videoID", fid);
                dl.setProperty("ext", ext);
                dl.setProperty("quality", quality_url);
                dl.setLinkID(plugin.getHost() + "://" + ext + "/" + fid + "/" + quality_url);
                List<DownloadLink> list = qualities.get(quality_url.toLowerCase(Locale.ENGLISH));
                if (list == null) {
                    list = new ArrayList<DownloadLink>();
                    qualities.put(quality_url.toLowerCase(Locale.ENGLISH), list);
                }
                list.add(dl);
            }
            final boolean allQualities = returnAll || (!cfg.isGrab4KVideoEnabled() && !cfg.isGrab1080pVideoEnabled() && !cfg.isGrab720pVideoEnabled() && !cfg.isGrab540pVideoEnabled() && !cfg.isGrab360pVideoEnabled());
            final boolean bestOnly = !returnAll && cfg.isGrabBestVideoVersionEnabled();

            if ((allQualities || cfg.isGrab4KVideoEnabled()) && qualities.containsKey("4k")) {
                results.addAll(qualities.get("4k"));
            }
            if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab1080pVideoEnabled()) && qualities.containsKey("1080")) {
                results.addAll(qualities.get("1080"));
            }
            if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab720pVideoEnabled()) && qualities.containsKey("720")) {
                results.addAll(qualities.get("720"));
            }
            if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab540pVideoEnabled()) && qualities.containsKey("540")) {
                results.addAll(qualities.get("540"));
            }
            if ((!bestOnly || results.isEmpty()) && (allQualities || cfg.isGrab360pVideoEnabled()) && qualities.containsKey("360")) {
                results.addAll(qualities.get("360"));
            }
            if (fp != null) {
                fp.addLinks(results);
            }
            ret.addAll(results);
        }

    }

    private boolean login(final Account account) throws Exception {
        this.setBrowserExclusive();
        final PluginForHost plugin = JDUtilities.getPluginForHost(this.getHost());
        try {
            if (plugin != null) {
                ((jd.plugins.hoster.XArtCom) plugin).login(account, br, false);
            } else {
                return false;
            }
        } catch (final PluginException e) {
            logger.log(e);
            account.setValid(false);
            return false;
        }
        return true;
    }

}