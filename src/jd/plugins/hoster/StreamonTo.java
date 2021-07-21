//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamonTo extends XFileSharingProBasic {
    public StreamonTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2021-05-31: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "streamon.to" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return -4;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -4;
        } else {
            /* Free(anonymous) and unknown account type */
            return -4;
        }
    }

    @Override
    public String[] scanInfo(String[] fileInfo) {
        fileInfo = super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = br.getRegex("\"info\"\\s*>\\s*<h\\d+>\\s*(.*?)\\s*</").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    protected void resolveShortURL(DownloadLink link, Account account) throws Exception {
        /* /d/(shortURL) are normal links */
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        // final String dllink = super.getDllink(link, account, br, src);
        return StreamonTo.huntDllink(this, src);
    }

    public static final String huntDllink(final PluginForHost plg, final String src) {
        String dllink = null;
        String[][] hunters = new Regex(src, "<script[^>]*>\\s*(var _.*?\\})eval(\\(function\\(h,u,n,t,e,r\\).*?)</script>").getMatches();
        int counter = 0;
        for (final String[] hunter : hunters) {
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(plg);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final StringBuilder sb = new StringBuilder();
            /* First function always has the same functionality but is always named differently */
            sb.append(hunter[0]);
            sb.append("var res = ");
            /* 2nd function always calls the same function but with different parameters. */
            sb.append(hunter[1]);
            String result = null;
            try {
                engine.eval(sb.toString());
                result = engine.get("res").toString();
                // System.out.println(counter + ":\r\n" + result);
                final String relevantPart1 = new Regex(result, "\\);[\r\n\\s]*(var.*?)\\s*(?:window\\.videoConfig|function)").getMatch(0);
                if (relevantPart1 != null) {
                    final StringBuilder sb2 = new StringBuilder();
                    sb2.append(relevantPart1);
                    sb2.append("function atob (f){var g={},b=65,d=0,a,c=0,h,e='',k=String.fromCharCode,l=f.length;for(a='';91>b;)a+=k(b++);a+=a.toLowerCase()+'0123456789+/';for(b=0;64>b;b++)g[a.charAt(b)]=b;for(a=0;a<l;a++)for(b=g[f.charAt(a)],d=(d<<6)+b,c+=6;8<=c;)((h=d>>>(c-=8)&255)||a<l-2)&&(e+=k(h));return e};");
                    sb2.append("var baabaffcac = efdcdbbfbefd.replace(\"RWFiZGVkYmJlZg\", \"\");var bbafcafccebb= atob(baabaffcac);var towait = 5;var fabefacdffbd = \"#badaebaccbacff\";var res = cccebacdeccc.replace(\"YWJmNDQ0YWIyOWFiYTdlNzE2ZjgwMzdlMjIyZGEwOGM\", \"\");var res2 = res.replace(\"NTljOTkzNTcxMTdjZDc1YzZiMTlhYzFjODY0NjBhZGE=\", \"\");var decode = atob(res2);");
                    engine.eval(sb2.toString());
                    dllink = engine.get("decode").toString();
                    if (!StringUtils.isEmpty(dllink)) {
                        plg.getLogger().info("js success");
                        break;
                    } else {
                        plg.getLogger().warning("Hunter failed -> Hunter won?");
                    }
                }
            } catch (final Exception e) {
                plg.getLogger().log(e);
            }
            counter += 1;
        }
        return dllink;
    }
}