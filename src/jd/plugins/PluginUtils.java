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

package jd.plugins;

import java.nio.charset.CharacterCodingException;

import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.logging.Log;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Little Helper class for often used Plugin issues
 * 
 * @author Coalado
 */
public class PluginUtils {

    private static final String JDL_PREFIX = "jd.plugins.PluginUtils.";

    /**
     * Asks the user to entere a password for plugin
     */
    public static String askPassword(final Plugin plg) {
        return askPassword(plg.getHost(), "");
    }

    public static String askPassword(String message, final DownloadLink link) {
        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.requestGuiUpdate();

            if (message == null) message = JDL.LF(JDL_PREFIX + "askPassword", "Please enter the password for %s", link.getName());
            final String password = askPassword(message, link.getDecrypterPassword());

            link.requestGuiUpdate();
            return password;
        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
        }
    }

    public static String askPassword(String message, final CryptedLink link) {
        link.getProgressController().setStatusText(JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));

        if (message == null) message = JDL.LF(JDL_PREFIX + "askPassword", "Please enter the password for %s", link.getCryptedUrl());
        final String password = askPassword(message, link.getDecrypterPassword());

        link.getProgressController().setStatusText(null);
        return password;
    }

    public static String askPassword(final String message, final String defaultmessage) {
        synchronized (JDUtilities.USERIO_LOCK) {
            return UserIO.getInstance().requestInputDialog(0, message, defaultmessage);
        }
    }

    /**
     * Informs the user that the password has been wrong
     * 
     * @param plg
     * @param password
     */
    public static void informPasswordWrong(final Plugin plg, final String password) {
        Balloon.show(JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.title", "Password wrong: %s", password), UserIO.getInstance().getIcon(UserIO.ICON_ERROR), JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.message", "The password you entered for %s has been wrong.", plg.getHost()));
    }

    public static void evalJSPacker(final Browser br) {
        final String regex = "eval\\((.*?\\,\\{\\}\\))\\)";
        final String[] containers = br.getRegex(regex).getColumn(0);

        String htmlcode;
        try {
            htmlcode = br.getRequest().getHtmlCode();

            for (String c : containers) {
                final Context cx = ContextFactory.getGlobal().enter();
                final Scriptable scope = cx.initStandardObjects();
                c = c.replaceAll("return p\\}\\(", " return p}  f(").replaceAll("function\\s*\\(p\\,a\\,c\\,k\\,e\\,d\\)", "function f(p,a,c,k,e,d)");
                final Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);
                final String code = Context.toString(result);
                htmlcode = htmlcode.replaceFirst(regex, code);
            }
            br.getRequest().setHtmlCode(htmlcode);
        } catch (CharacterCodingException e) {
            Log.exception(e);
        }
    }

}
