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

package jd.controlling;

import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;
import jd.controlling.authentication.AuthenticationInfo.Type;
import jd.http.Browser;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;

/**
 * @Deprecated Only here for stable plugin compatibility
 */
public class HTACCESSController {

    private final static HTACCESSController INSTANCE = new HTACCESSController();

    private HTACCESSController() {
    }

    public String get(final String url) {
        String[] ret = AuthenticationController.getInstance().getLogins(url);
        if (ret == null) return null;
        return ret[0] + ":" + ret[1];
    }

    public void addValidatedAuthentication(String url, String username, String password) {
        if (StringUtils.isEmpty(url)) return;
        AuthenticationInfo.Type type = null;
        if (url.startsWith("ftp")) {
            type = Type.FTP;
        } else if (url.startsWith("http")) {
            type = Type.HTTP;
        } else {
            Log.L.info("Unknown Protocoll: " + url);
            return;
        }
        AuthenticationInfo info = new AuthenticationInfo();
        info.setType(type);
        info.setEnabled(true);
        info.setUsername(username);
        info.setPassword(password);
        info.setHostmask(Browser.getHost(url, true));
        AuthenticationController.getInstance().add(info);
    }

    @Deprecated
    public static HTACCESSController getInstance() {
        return INSTANCE;
    }

}
