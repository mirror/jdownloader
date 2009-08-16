package jd.gui.userio;

import java.awt.Point;
import java.io.File;

import javax.swing.ImageIcon;
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
    protected String showCaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain) {
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
    protected File[] showFileChooser(String id, String title, Integer fileSelectionMode, FileFilter fileFilter, Boolean multiSelection) {
        System.out.println("NoUserIO set!");
        return null;
    }

    @Override
    public int requestComboDialog(int flag, String title, String question, Object[] options, int defaultSelection, ImageIcon icon, String okText, String cancelText, Object renderer) {
        System.out.println("NoUserIO set!");
        return 0;
    }

}
