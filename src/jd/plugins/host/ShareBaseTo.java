//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.nutils.JDHash;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class ShareBaseTo extends PluginForHost {

    public ShareBaseTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharebase.to/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://sharebase.to/terms/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        /* damit neue links mit .de als .to in die liste kommen */
        setBrowserExclusive();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("sharebase\\.de", "sharebase\\.to"));
        br.getPage(downloadLink.getDownloadURL());
        String[] infos = br.getRegex("<span class=\"font1\">(.*?) </span>\\((.*?)\\)</td>").getRow(0);
        if (infos == null || infos.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(infos[0].trim());
        downloadLink.setDownloadSize(Regex.getSize(infos[1].trim()));
        return true;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        setBrowserExclusive();
        br.setCookie("http://" + getHost(), "memm", Encoding.urlEncode(account.getUser()));
        br.setCookie("http://" + getHost(), "memp", JDHash.getMD5(account.getPass()));
        br.getPage("http://sharebase.to/members/");
        String points = br.getMatch("<td>Premiumpunkte:</td>.*?<td><input.*cleanform.*value=\"(\\d+?) Punkte\"></td>");
        String expire = br.getMatch("<td>Premium bis:</td>.*?<td><input.*?cleanform.*? value=\"(.*?)\"></td>");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd.MM.yy / hh:mm", null));
        ai.setPremiumPoints(Integer.parseInt(points));
        return ai;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        br.setCookie("http://" + getHost(), "memm", account.getUser());
        br.setCookie("http://" + getHost(), "memp", JDHash.getMD5(account.getPass()));

        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("werden derzeit Wartungsarbeiten vorgenommen")) {
            logger.severe("ShareBaseTo Error: Maintenance");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wartungsarbeiten", 30 * 60 * 1000l);
        }
        if (br.containsHTML("Der Download existiert nicht")) {
            logger.severe("ShareBaseTo Error: File not found");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        if (!br.containsHTML("favorite")) {
            logger.severe("ShareBaseTo Error: Premium account expired");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }

        dl = br.openDownload(downloadLink, br.getForm(1));
        if (dl.getConnection() == null) {
            logger.severe("ServerError");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.sharebaseto.servererror", "Service not available"), 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        /* für links welche noch mit .de in der liste stehen */
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("Der Download existiert nicht")) {
            logger.severe("ShareBaseTo Error: File not found");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        Form form = br.getFormbyValue("Please Activate Javascript");
        form.setVariable(1, Encoding.urlEncode("Download Now !"));
        br.submitForm(form);

        if (br.containsHTML("Von deinem Computer ist noch ein Download aktiv.")) {
            logger.severe("ShareBaseTo Error: Too many downloads");
            // throw new
            // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
            // "Nur ein Download gleichzeitig!", 5000);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l);
        } else if (br.containsHTML("werden derzeit Wartungsarbeiten vorgenommen")) {
            logger.severe("ShareBaseTo Error: Maintenance");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wartungsarbeiten", 30 * 60 * 1000l);
        }
        String[] wait = br.getRegex("Du musst noch <strong>(\\d*?)min (\\d*?)sec</strong> warten").getRow(0);
        if (wait != null) {
            long waitTime = (Integer.parseInt(wait[0]) * 60 + Integer.parseInt(wait[1])) * 1000l;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
        }
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation());
        if (dl.getConnection() == null) {
            logger.severe("ServerError");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.sharebaseto.servererror", "Service not available"), 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
