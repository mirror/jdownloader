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

package jd.gui;

import java.awt.Point;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileFilter;

import jd.config.SubConfiguration;
import jd.gui.skins.jdgui.GUIUtils;
import jd.gui.skins.jdgui.JDGuiConstants;
import jd.gui.userio.NoUserIO;
import jd.utils.locale.JDL;

public abstract class UserIO {

    /**
     * Flag to set that no Automatic captcha detection should be used in the
     * userinput
     */
    public static final int NO_JAC = 1 << 0;
    /**
     * TO not query user. Try to fill automaticly, or return null
     */
    public static final int NO_USER_INTERACTION = 1 << 1;
    public static final int NO_COUNTDOWN = 1 << 2;
    public static final int NO_OK_OPTION = 1 << 3;
    public static final int NO_CANCEL_OPTION = 1 << 4;
    public static final int DONT_SHOW_AGAIN = 1 << 5;
    public static final int STYLE_LARGE = 1 << 6;
    public static final int STYLE_HTML = 1 << 7;
    /**
     * Return IDS
     */
    public static final int RETURN_OK = 1 << 1;
    public static final int RETURN_CANCEL = 1 << 2;
    public static final int RETURN_DONT_SHOW_AGAIN = 1 << 3;
    public static final int RETURN_SKIPPED_BY_DONT_SHOW = 1 << 4;
    public static final int RETURN_COUNTDOWN_TIMEOUT = 1 << 5;
    public static final int ICON_INFO = 0;
    public static final int ICON_WARNING = 1;
    public static final int ICON_ERROR = 2;
    public static final int ICON_QUESTION = 3;
    public static final String ICON_NONE = "NULLICONDUMMY";

    protected static UserIO INSTANCE = null;
    private static Integer COUNTDOWN_TIME = null;

    public static UserIO getInstance() {
        if (INSTANCE == null) INSTANCE = new NoUserIO();
        return INSTANCE;
    }

    public static void setInstance(UserIO instance) {
        INSTANCE = instance;
    }

    public String requestCaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain) {
        synchronized (INSTANCE) {
            return showCaptchaDialog(flag, methodname, captchafile, suggestion, explain);
        }
    }

    abstract protected String showCaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain);

    public Point requestClickPositionDialog(File imagefile, String title, String explain) {
        synchronized (INSTANCE) {
            return showClickPositionDialog(imagefile, title, explain);
        }
    }

    abstract protected Point showClickPositionDialog(File imagefile, String title, String explain);

    public int requestHelpDialog(int flag, String title, String message, String helpMessage, String url) {
        synchronized (INSTANCE) {
            return showHelpDialog(flag, title, message, helpMessage, url);
        }
    }

    abstract protected int showHelpDialog(int flag, String title, String message, String helpMessage, String url);

    public int requestHtmlDialog(int flag, String title, String message) {
        synchronized (INSTANCE) {
            return showHtmlDialog(flag, title, message);
        }
    }

    abstract protected int showHtmlDialog(int flag, String title, String message);

    public int requestConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        synchronized (INSTANCE) {
            if (icon == null) {
                icon = getDefaultIcon(title + message);

            }
            return showConfirmDialog(flag, title, message, icon, okOption, cancelOption);
        }

    }

    abstract protected int showConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption);

    public String requestInputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption) {

        synchronized (INSTANCE) {
            return showInputDialog(flag, title, message, defaultMessage, icon, okOption, cancelOption);
        }

    }

    abstract protected String showInputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption);

    public String requestTextAreaDialog(String title, String message, String def) {

        synchronized (INSTANCE) {
            return showTextAreaDialog(title, message, def);
        }

    }

    abstract protected String showTextAreaDialog(String title, String message, String def);

    public String[] requestTwoTextFieldDialog(String title, String messageOne, String defOne, String messageTwo, String defTwo) {

        synchronized (INSTANCE) {
            return showTwoTextFieldDialog(title, messageOne, defOne, messageTwo, defTwo);
        }

    }

    abstract protected String[] showTwoTextFieldDialog(String title, String messageOne, String defOne, String messageTwo, String defTwo);

    /**
     * Requests a FileChooserDialog.
     * 
     * @param id
     *            ID of the dialog (used to save and restore the old directory)
     * @param title
     *            dialog-title or null for default
     * @param fileSelectionMode
     *            mode for selecting files (like JDFileChooser.FILES_ONLY) or
     *            null for default
     * @param fileFilter
     *            filters the choosable files or null for default
     * @param multiSelection
     *            multible files choosable? or null for default
     * @return an array of files or null if the user cancel the dialog
     */
    public File[] requestFileChooser(String id, String title, Integer fileSelectionMode, FileFilter fileFilter, Boolean multiSelection) {

        synchronized (INSTANCE) {
            return showFileChooser(id, title, fileSelectionMode, fileFilter, multiSelection);
        }

    }

    abstract protected File[] showFileChooser(String id, String title, Integer fileSelectionMode, FileFilter fileFilter, Boolean multiSelection);

    public void requestMessageDialog(String message) {
        requestMessageDialog(JDL.L("gui.dialogs.message.title", "Message"), message);
    }

    public void requestMessageDialog(String title, String message) {
        synchronized (INSTANCE) {
            showConfirmDialog(UserIO.NO_CANCEL_OPTION, title, message, getIcon(UserIO.ICON_INFO), null, null);
        }
    }

    private ImageIcon getDefaultIcon(String text) {
        if (text == UserIO.ICON_NONE) return null;
        if (text.contains("?")) {
            return this.getIcon(ICON_QUESTION);
        } else if (text.matches(JDL.L("userio.errorregex", ".*(error|failed).*"))) {
            return this.getIcon(ICON_ERROR);
        } else if (text.contains("!")) {
            return this.getIcon(ICON_WARNING);
        } else {
            return this.getIcon(ICON_INFO);
        }
    }

    public abstract ImageIcon getIcon(int iconInfo);

    public static int getCountdownTime() {
        SubConfiguration cfg = GUIUtils.getConfig();
        if (COUNTDOWN_TIME != null) return COUNTDOWN_TIME.intValue();
        return Math.max(2, cfg.getIntegerProperty(JDGuiConstants.PARAM_INPUTTIMEOUT, 20));
    }

    /**
     * Sets the countdowntime for this session. does not save!
     * 
     * @param time
     */
    public static void setCountdownTime(Integer time) {
        if (time == null) {
            COUNTDOWN_TIME = null;
        } else {
            COUNTDOWN_TIME = new Integer(time);
        }
    }

    public String requestInputDialog(String message) {

        return requestInputDialog(0, message, null);
    }

    public int requestConfirmDialog(int flag, String question) {
        return UserIO.getInstance().requestConfirmDialog(flag, JDL.L("jd.gui.userio.defaulttitle.confirm", "Please confirm!"), question, this.getDefaultIcon(question), null, null);
    }

    /**
     * 
     * @param flag
     *            flag
     * @param question
     *            question
     * @param defaultvalue
     *            defaultvalue
     * @return
     */
    public String requestInputDialog(int flag, String question, String defaultvalue) {
        return this.requestInputDialog(0, JDL.L("jd.gui.userio.defaulttitle.input", "Please enter!"), question, defaultvalue, this.getDefaultIcon(question), null, null);
    }

}
