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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "x-art.com" }, urls = { "^https?://(www\\.)?x-art\\.com/(members/)?.+" })
public class XArt extends PluginForDecrypt {

    public XArt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private SubConfiguration cfg = null;

    @Override
    protected DownloadLink createDownloadlink(String link) {
        final String original_url = link;
        link = link.replaceFirst("x-art\\.com", "x-artdecrypted.com");
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setContentUrl(original_url);
        return ret;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        cfg = SubConfiguration.getConfig(this.getHost());
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
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
        if (url.matches("(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)")) {
            ret.add(createDownloadlink(url));
            return ret;
        }
        boolean prem = false;
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("x-art.com");
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
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
            parseUrl(ret, url);
        } else {
            br.getPage(url);
            String[] sub = br.getRegex("(https?://(www\\.)?x-art\\.com/(members/)?(videos|galleries)/[a-zA0-9\\-\\_]+/?)").getColumn(0);
            HashSet<String> dupe = new HashSet<String>();
            for (String s : sub) {
                try {
                    if (isAbort()) {
                        return ret;
                    }
                } catch (Throwable ee) {

                }

                if (dupe.add(s)) {
                    parseUrl(ret, s);
                }
            }

        }
        return ret;
    }

    public void parseUrl(final ArrayList<DownloadLink> ret, final String url) throws IOException {
        br.setFollowRedirects(true);

        br.getPage(url);
        String title = br.getRegex("<div class=\"small-12 medium-12 large-12 columns\">\\s*<h1>([a-zA-Z0-9\\_\\-\\ ]*)<\\/h1>").getMatch(0);
        if (url.matches("^https?://(www\\.)?x-art\\.com/(members/)?galleries/[a-zA0-9\\-\\_]+/?$")) {
            ArrayList<DownloadLink> dlinks = new ArrayList<DownloadLink>();
            String links[] = br.getRegex("href=\"([a-zA-Z0-9\\_\\-]*\\.(mp4|wmv|mov|zip))\"").getColumn(0);
            String lnks[] = br.getRegex("href=(\"|')(https?://([^\r\n\t\"']+\\.)?x-art\\.com/[^\r\n\t\"']+\\.(mp4|wmv|mov|zip)[^\r\n\t\"']*)").getColumn(1);
            if (links != null) {
                for (String link : links) {
                    DownloadLink dl = createDownloadlink(br.getURL(link).toString());
                    dlinks.add(dl);
                }
            }
            if (lnks != null) {
                for (final String link : lnks) {
                    final String quality_url = new Regex(link, "([A-Za-z]+)\\.zip").getMatch(0);
                    if (quality_url != null && !cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                        /* Skip unwanted content */
                        continue;
                    }
                    final DownloadLink dl = createDownloadlink((link));
                    dlinks.add(dl);
                }
            }
            ret.addAll(dlinks);
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
            final String htmldownload = this.br.getRegex("<ul[^>]*?id=\"drop\\-download\"[^>]*?>(.*?)</ul>").getMatch(0);
            final String[] dlinfo = htmldownload.split("</li>");
            for (final String video : dlinfo) {
                final String dlurl = new Regex(video, "(/download/[^<>\"]+)\"").getMatch(0);
                final String filesize = new Regex(video, "\\((\\d+(?:\\.\\d+)? [A-Za-z]{2,5})\\)").getMatch(0);
                final String quality_url = dlurl != null ? new Regex(dlurl, "([A-Za-z0-9]+)\\.mp4").getMatch(0) : null;
                if (dlurl == null || quality_url == null) {
                    continue;
                }
                if (!cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                    /* Skip unwanted content */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(br.getURL(dlurl).toString());
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    dl.setAvailable(true);
                }
                dl.setName(title + "_" + quality_url + ".mp4");
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality_url);
                ret.add(dl);
            }
        }
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
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
            account.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}