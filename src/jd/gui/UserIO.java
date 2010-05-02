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
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.userio.NoUserIO;
import jd.nutils.JDFlags;
import jd.utils.locale.JDL;

public abstract class UserIO {

    /**
     * TO not query user. Try to fill automaticly, or return null
     */
    public static final int NO_USER_INTERACTION = 1 << 1;
    /**
     * do not display a countdown
     */
    public static final int NO_COUNTDOWN = 1 << 2;
    /**
     * do not display ok option
     */
    public static final int NO_OK_OPTION = 1 << 3;
    /**
     * do not display cancel option
     */
    public static final int NO_CANCEL_OPTION = 1 << 4;
    /**
     * displays a do not show this question again checkbox
     */
    public static final int DONT_SHOW_AGAIN = 1 << 5;
    /**
     * IF available a large evrsion of the dialog is used
     */
    public static final int STYLE_LARGE = 1 << 6;
    /**
     * Render html
     */
    public static final int STYLE_HTML = 1 << 7;
    /**
     * Does not display an icon
     */
    public static final int NO_ICON = 1 << 8;
    /**
     * Cancel option ignores Don't show again checkbox
     */
    public static final int DONT_SHOW_AGAIN_IGNORES_CANCEL = 1 << 9;
    /**
     * If user selects OK Option, the don't show again option is ignored
     */
    public static final int DONT_SHOW_AGAIN_IGNORES_OK = 1 << 10;
    /**
     * the textfield will be renderer as a passwordfield
     */
    public static final int STYLE_PASSWORD = 1 << 11;

    /**
     * pressed ok
     */
    public static final int RETURN_OK = 1 << 1;
    /**
     * pressed cancel
     */
    public static final int RETURN_CANCEL = 1 << 2;
    /**
     * don'tz sho again flag ahs been set. the dialog may has been visible. if
     * RETURN_SKIPPED_BY_DONT_SHOW is not set. the user set this flag latly
     */
    public static final int RETURN_DONT_SHOW_AGAIN = 1 << 3;
    /**
     * don't show again flag has been set the dialog has not been visible
     */
    public static final int RETURN_SKIPPED_BY_DONT_SHOW = 1 << 4;
    /**
     * Timeout has run out. Returns current settings or default values
     */
    public static final int RETURN_COUNTDOWN_TIMEOUT = 1 << 5;
    public static final int ICON_INFO = 0;
    public static final int ICON_WARNING = 1;
    public static final int ICON_ERROR = 2;
    public static final int ICON_QUESTION = 3;

    protected static UserIO INSTANCE = null;
    private static int COUNTDOWN_TIME = -1;

    public static UserIO getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoUserIO();
        }
        return INSTANCE;
    }

    public static void setInstance(final UserIO instance) {
        INSTANCE = instance;
    }

    public String requestCaptchaDialog(final int flag, final String host, final ImageIcon icon, final File captchafile, final String suggestion, final String explain) {
        synchronized (INSTANCE) {
            return showCaptchaDialog(flag, host, icon, captchafile, suggestion, explain);
        }
    }

    abstract protected String showCaptchaDialog(int flag, String host, ImageIcon icon, File captchafile, String suggestion, String explain);

    public Point requestClickPositionDialog(final File imagefile, final String title, final String explain) {
        synchronized (INSTANCE) {
            return showClickPositionDialog(imagefile, title, explain);
        }
    }

    abstract protected Point showClickPositionDialog(File imagefile, String title, String explain);

    public int requestHelpDialog(final int flag, final String title, final String message, final String helpMessage, final String url) {
        synchronized (INSTANCE) {
            return showHelpDialog(flag, title, message, helpMessage, url);
        }
    }

    abstract protected int showHelpDialog(int flag, String title, String message, String helpMessage, String url);

    public int requestConfirmDialog(final int flag, final String title, final String message, final ImageIcon icon, final String okOption, final String cancelOption) {
        synchronized (INSTANCE) {
            return showConfirmDialog(flag, title, message, icon == null ? getDefaultIcon(title + message) : icon, okOption, cancelOption);
        }
    }

    abstract protected int showConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption);

    public String requestInputDialog(final int flag, final String title, final String message, final String defaultMessage, final ImageIcon icon, final String okOption, final String cancelOption) {
        synchronized (INSTANCE) {
            return showInputDialog(flag, title, message, defaultMessage, icon, okOption, cancelOption);
        }
    }

    abstract protected String showInputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption);

    public String requestTextAreaDialog(final String title, final String message, final String def) {
        synchronized (INSTANCE) {
            return showTextAreaDialog(title, message, def);
        }
    }

    abstract protected String showTextAreaDialog(String title, String message, String def);

    public String[] requestTwoTextFieldDialog(final String title, final String messageOne, final String defOne, final String messageTwo, final String defTwo) {
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
    public File[] requestFileChooser(final String id, final String title, final Integer fileSelectionMode, final FileFilter fileFilter, final Boolean multiSelection) {
        synchronized (INSTANCE) {
            return showFileChooser(id, title, fileSelectionMode, fileFilter, multiSelection);
        }
    }

    abstract protected File[] showFileChooser(String id, String title, Integer fileSelectionMode, FileFilter fileFilter, Boolean multiSelection);

    public void requestMessageDialog(final String message) {
        requestMessageDialog(0, JDL.L("gui.dialogs.message.title", "Message"), message);
    }

    public void requestMessageDialog(final int flag, final String message) {
        requestMessageDialog(flag, JDL.L("gui.dialogs.message.title", "Message"), message);
    }

    public void requestMessageDialog(final String title, final String message) {
        requestMessageDialog(0, title, message);
    }

    public void requestMessageDialog(final int flag, final String title, final String message) {
        synchronized (INSTANCE) {
            showConfirmDialog(UserIO.NO_CANCEL_OPTION | flag, title, message, getIcon(UserIO.ICON_INFO), null, null);
        }
    }

    private ImageIcon getDefaultIcon(final String text) {
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
        return (COUNTDOWN_TIME > 0) ? COUNTDOWN_TIME : Math.max(2, GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_INPUTTIMEOUT, 20));
    }

    /**
     * Sets the countdowntime for this session. does not save!
     * 
     * @param time
     */
    public static void setCountdownTime(final int time) {
        COUNTDOWN_TIME = (time > 0) ? time : -1;
    }

    public String requestInputDialog(final String message) {
        return requestInputDialog(0, message, null);
    }

    public int requestConfirmDialog(final int flag, final String question) {
        return requestConfirmDialog(flag, JDL.L("jd.gui.userio.defaulttitle.confirm", "Please confirm!"), question, this.getDefaultIcon(question), null, null);
    }

    public int requestConfirmDialog(final int flag, final String title, final String question) {
        return requestConfirmDialog(flag, title, question, this.getDefaultIcon(title + question), null, null);
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
    public String requestInputDialog(final int flag, final String question, final String defaultvalue) {
        return requestInputDialog(flag, JDL.L("jd.gui.userio.defaulttitle.input", "Please enter!"), question, defaultvalue, this.getDefaultIcon(question), null, null);
    }

    /**
     * Shows a combobox dialog. returns the options id if the user confirmed, or
     * -1 if the user canceled
     * 
     * @param flag
     * @param title
     * @param question
     * @param options
     * @param defaultSelection
     * @param icon
     * @param okText
     * @param cancelText
     * @param renderer
     *            TODO
     * @return
     */
    public abstract int requestComboDialog(int flag, String title, String question, Object[] options, int defaultSelection, ImageIcon icon, String okText, String cancelText, ListCellRenderer renderer);

    /**
     * Checks wether this answerfalg contains the ok option
     * 
     * @param answer
     * @return
     */
    public static boolean isOK(final int answer) {
        return JDFlags.hasSomeFlags(answer, UserIO.RETURN_OK);
    }

}
