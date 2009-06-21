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

package jd.gui.userio;

import java.awt.Point;
import java.io.File;

import javax.swing.ImageIcon;

import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.userio.dialog.CaptchaDialog;
import jd.gui.userio.dialog.ClickPositionDialog;
import jd.gui.userio.dialog.ConfirmDialog;
import jd.gui.userio.dialog.HelpDialog;
import jd.gui.userio.dialog.HtmlDialog;
import jd.gui.userio.dialog.InputDialog;
import jd.utils.JDTheme;

public class SimpleUserIO extends UserIO {
    private SimpleUserIO() {
        super();
    }

    public static UserIO getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleUserIO();
        }
        return INSTANCE;
    }

    @Override
    protected String showCaptchaDialog(final int flag, final String methodname, final File captchafile, final String suggestion, final String explain) {
        if ((flag & UserIO.NO_USER_INTERACTION) > 0) return suggestion;
        return new GuiRunnable<String>() {

            @Override
            public String runSave() {
                return new CaptchaDialog(flag, methodname, captchafile, suggestion, explain).getCaptchaText();
            }

        }.getReturnValue();
    }

    @Override
    protected Point showClickPositionDialog(final File imagefile, final String title, final String explain) {
        return new GuiRunnable<Point>() {

            @Override
            public Point runSave() {
                return new ClickPositionDialog(imagefile, title, explain).getPoint();
            }

        }.getReturnValue();
    }

    @Override
    protected int showHelpDialog(final int flag, final String title, final String message, final String helpMessage, final String url) {
        return new GuiRunnable<Integer>() {

            @Override
            public Integer runSave() {
                return new HelpDialog(flag, title, message, helpMessage, url).getReturnValue();
            }

        }.getReturnValue();
    }

    @Override
    protected int showHtmlDialog(final int flag, final String title, final String message) {
        return new GuiRunnable<Integer>() {

            @Override
            public Integer runSave() {
                return new HtmlDialog(flag, title, message).getReturnValue();
            }

        }.getReturnValue();
    }

    public static void main(String args[]) {
        UserIO.setInstance(SimpleUserIO.getInstance());
    }

    @Override
    protected int showConfirmDialog(final int flag, final String title, final String message, final ImageIcon icon, final String okOption, final String cancelOption) {
        if ((flag & UserIO.NO_USER_INTERACTION) > 0) return 0;
        return new GuiRunnable<Integer>() {

            @Override
            public Integer runSave() {
                return new ConfirmDialog(flag, title, message, icon, okOption, cancelOption).getReturnID();
            }

        }.getReturnValue();
    }

    @Override
    protected String showInputDialog(final int flag, final String title, final String message, final String defaultMessage, final ImageIcon icon, final String okOption, final String cancelOption) {
        if ((flag & UserIO.NO_USER_INTERACTION) > 0) return defaultMessage;
        return new GuiRunnable<String>() {

            @Override
            public String runSave() {
                return new InputDialog(flag, title, message, defaultMessage, icon, okOption, cancelOption).getReturnID();
            }

        }.getReturnValue();
    }

    @Override
    public ImageIcon getIcon(int iconInfo) {
        switch (iconInfo) {
        case UserIO.ICON_ERROR:
            return JDTheme.II("gui.images.stop", 32, 32);
        case UserIO.ICON_WARNING:
            return JDTheme.II("gui.images.warning", 32, 32);
        case UserIO.ICON_QUESTION:
            return JDTheme.II("gui.images.help", 32, 32);
        default:
            return JDTheme.II("gui.images.config.tip", 32, 32);
        }
    }
}
