package jd.plugins;

import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Little Helper class for often used Plugin issues
 * 
 * @author Coalado
 */
public class PluginUtils {
    /**
     * Asks the user to entere a password for plugin
     */
    public static String askPassword(Plugin plg) {
        return UserIO.getInstance().requestInputDialog(0, JDL.LF("jd.plugins.PluginUtils.askPassword", "Please enter the password for %s", plg.getHost()), "");
    }

    /**
     * Informs the user that the password has been wrong
     * 
     * @param plg
     * @param password
     */
    public static void informPasswordWrong(Plugin plg, String password) {
        Balloon.show(JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.title", "Password wrong: %s", password), UserIO.getInstance().getIcon(UserIO.ICON_ERROR), JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.message", "The password you entered for %s has been wrong.", plg.getHost()));

    }

    public static void evalJSPacker(Browser br) {
        String regex = "eval\\((.*?\\,\\{\\}\\))\\)";
        String[] containers = br.getRegex(regex).getColumn(0);

        String htmlcode = br.getRequest().getHtmlCode();
        for (String c : containers) {
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();
            c = c.replaceAll("return p\\}\\(", " return p}  f(").replaceAll("function\\s*\\(p\\,a\\,c\\,k\\,e\\,d\\)", "function f(p,a,c,k,e,d)");
            Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);
            String code = Context.toString(result);
            htmlcode = htmlcode.replaceFirst(regex, code);

        }
        br.getRequest().setHtmlCode(htmlcode);
    }

}
