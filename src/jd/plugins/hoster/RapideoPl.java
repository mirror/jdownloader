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
package jd.plugins.hoster;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapideo.net" }, urls = { "" })
public class RapideoPl extends RapideoCore {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("rapideo.net");

    public RapideoPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + getHost() + "/rejestracja");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/legal ";
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }

    @Override
    protected String getAPIV1Base() {
        /* Do NOT use rapideo.pl here! */
        return "https://enc.rapideo.pl/";
    }

    @Override
    protected String getAPIV1SiteParam() {
        return "newrd";
    }

    @Override
    protected String getPasswordAPIV1(final Account account) {
        return md5HEX(account.getPass());
    }

    private static String md5HEX(final String s) {
        String result = null;
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] digest = md5.digest(s.getBytes());
            result = toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // this won't happen, we know Java has MD5!
        }
        return result;
    }

    private static String toHex(final byte[] a) {
        final StringBuilder sb = new StringBuilder(a.length * 2);
        for (int i = 0; i < a.length; i++) {
            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(a[i] & 0x0f, 16));
        }
        return sb.toString();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}