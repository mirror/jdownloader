package org.jdownloader.controlling;

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class PasswordUtils {

    public static void main(String[] args) {
        String txt;
        try {
            txt = Dialog.getInstance().showInputDialog("txt");
            System.out.println(getPasswords(txt));
        } catch (DialogClosedException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);

        } catch (DialogCanceledException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);

        }

    }

    public static HashSet<String> getPasswords(String data) {
        if (data == null) {
            return new HashSet<String>();
        }
        final HashSet<String> ret = new HashSet<String>();
        // data = data.replaceAll("(?s)<!-- .*? -->", "").replaceAll("(?s)<script .*?>.*?</script>", "").replaceAll("(?s)<.*?>",
        // "").replaceAll("Spoiler:", "").replaceAll("(no.{0,2}|kein.{0,8}|ohne.{0,8}|nicht.{0,8})(pw|passwort|password|pass)",
        // "").replaceAll("(pw|passwort|password|pass).{0,12}(nicht|falsch|wrong)", "");

        String passwordPattern = org.jdownloader.translate._JDT.T.pattern_password();
        Pattern pattern = Pattern.compile("(" + passwordPattern + ")[\\s][\\s]*?[\"']([[^\\:\"'\\s]][^\"'\\s]*)[\"']?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            final String pass = matcher.group(2);
            if (pass != null && pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(" + passwordPattern + ")[\\s][\\s]*?([[^\\:\"'\\s]][^\"'\\s]*)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            final String pass = matcher.group(2);
            if (pass != null && pass.length() > 4 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(" + passwordPattern + ")[\\s]?(\\:|=)[\\s]*?[\"']([^\"']+)[\"']?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            final String pass = matcher.group(2);
            if (pass != null && pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) {
                ret.add(pass);
            }
        }
        pattern = Pattern.compile("(" + passwordPattern + ")[\\:|\\-=[\\s]]+([^\"'\\s]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {

            final String pass = matcher.group(2);
            if (pass != null && pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) {
                ret.add(pass);
            }
        }
        final Iterator<String> it = ret.iterator();
        while (it.hasNext()) {
            final String next = it.next();
            if (StringUtils.isEmpty(next)) {
                it.remove();
            } else if (next.startsWith("</span")) {
                it.remove();
            } else if (next.startsWith("<span")) {
                it.remove();
            }
        }
        return ret;

    }

}
