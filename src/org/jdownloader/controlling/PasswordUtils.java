package org.jdownloader.controlling;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class PasswordUtils {
    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        try {
            final String txt = Dialog.getInstance().showInputDialog("txt");
            System.out.println(getPasswords(txt));
        } catch (DialogClosedException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        } catch (DialogCanceledException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
    }

    public static HashSet<String> getPasswords(final String data) {
        final LinkedHashSet<String> ret = new LinkedHashSet<String>();
        if (data != null) {
            final String passwordPattern = org.jdownloader.translate._JDT.T.pattern_password();
            Pattern pattern = Pattern.compile("(" + passwordPattern + ")\\s+[\"']([[^\\:\"'\\s]][^\"'\\s])[\"']?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(data);
            while (matcher.find()) {
                final String pass = matcher.group(2);
                ret.add(pass);
            }
            pattern = Pattern.compile("(?:>\\s*|\"\\s*|'\\s*|\\s+)(" + passwordPattern + ")\\s+([[^\\:\"'\\s]][^\"'\\s]*)[\\s]?", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(data);
            while (matcher.find()) {
                final String pass = matcher.group(2);
                ret.add(pass);
            }
            pattern = Pattern.compile("(" + passwordPattern + ")\\s*(?:\\:|=)\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(data);
            while (matcher.find()) {
                final String pass = matcher.group(2);
                ret.add(pass);
            }
            pattern = Pattern.compile("(" + passwordPattern + ")\\s*(?:\\:|\\-|=)\\s*([^\"'\\s]+)", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(data);
            while (matcher.find()) {
                final String pass = matcher.group(2);
                ret.add(pass);
            }
            final Iterator<String> it = ret.iterator();
            while (it.hasNext()) {
                final String next = it.next();
                if (StringUtils.isEmpty(next)) {
                    it.remove();
                } else if (next.length() < 2) {
                    /* Remove 1-char strings. */
                    it.remove();
                } else if (next.matches("^\\s*</?span.*")) {
                    /* Remove html snippets #1 */
                    it.remove();
                } else if (next.matches("^\\s*</?td.*")) {
                    /* Remove html snippets #2 */
                    it.remove();
                } else if (next.matches("(?i).*(rar|zip|jpg|gif|png|html|php|avi|mpg)$")) {
                    /* Remove passwords which look to be filenames. */
                    it.remove();
                }
            }
        }
        return ret;
    }
}
