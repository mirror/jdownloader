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
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;

import jd.gui.UserIO;

public class NoUserIO extends UserIO {

    public NoUserIO() {
        super();
    }

    @Override
    public ImageIcon getIcon(int iconInfo) {
        System.out.println("NoUserIO set!");
        return null;
    }

    @Override
    protected String showCaptchaDialog(int flag, String host, ImageIcon icon, File captchafile, String suggestion, String explain) {
        System.out.println("NoUserIO set!");
        return null;
    }

    @Override
    protected Point showClickPositionDialog(File imagefile, String title, String explain) {
        System.out.println("NoUserIO set!");
        return null;
    }

    @Override
    protected int showConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        System.out.println("NoUserIO set!");
        return 0;
    }

    @Override
    protected int showHelpDialog(int flag, String title, String message, String helpMessage, String url) {
        System.out.println("NoUserIO set!");
        return 0;
    }

    @Override
    protected String showInputDialog(int flag, String title, String message, String defaultMessage, ImageIcon icon, String okOption, String cancelOption) {
        System.out.println("NoUserIO set!");
        return "";
    }

    @Override
    protected String showTextAreaDialog(String title, String message, String def) {
        System.out.println("NoUserIO set!");
        return "";
    }

    @Override
    protected String[] showTwoTextFieldDialog(String title, String messageOne, String defOne, String messageTwo, String defTwo) {
        System.out.println("NoUserIO set!");
        return new String[0];
    }

    @Override
    protected File[] showFileChooser(String id, String title, Integer fileSelectionMode, FileFilter fileFilter, Boolean multiSelection, File startDirectory, Integer dialogType) {
        System.out.println("NoUserIO set!");
        return null;
    }

    @Override
    public int requestComboDialog(int flag, String title, String question, Object[] options, int defaultSelection, ImageIcon icon, String okText, String cancelText, ListCellRenderer renderer) {
        System.out.println("NoUserIO set!");
        return 0;
    }

}
