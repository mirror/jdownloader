//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OneCloudfileCom extends YetiShareCore {
    public OneCloudfileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null reCaptchaV2, hcaptcha<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "1cloudfile.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    protected String getContinueLink(final Browser br) throws Exception {
        /* 2023-12-06: Similar for: 1cloudfile.com, bowfile.com */
        final String jsFunc = br.getRegex("(function getNextDownloadPageLink\\(crl, btnText\\).*?\\s+\\})\\s+").getMatch(0);
        final String jsCall = br.getRegex("(getNextDownloadPageLink\\(\".*?\\))\n").getMatch(0);
        if (jsFunc != null && jsCall != null) {
            /* 2023-12-06: This doesn't work yet */
            String result = null;
            final StringBuilder sb = new StringBuilder();
            sb.append(jsFunc);
            sb.append("\nvar result = " + jsCall + ";");
            // System.out.print(sb.toString());
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval(sb.toString());
                result = engine.get("result").toString();
                return result;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return super.getContinueLink(br);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }
}