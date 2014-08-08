package org.jdownloader.controlling.ffmpeg;

import java.awt.Dialog.ModalityType;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class FFMpegInstallTypeChooserDialog extends AbstractDialog<Object> implements ConfirmDialogInterface {

    private MigPanel p;

    private String   task;

    public FFMpegInstallTypeChooserDialog(String task) {
        super(0, _GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_title(), null, _GUI._.ffmpeg_install_now(), null);
        this.task = task;
    }

    @Override
    public boolean isRemoteAPIEnabled() {
        return true;
    }

    public static class FoundException extends Exception {

        private File file;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public FoundException(File f) {
            file = f;
        }

    }

    private JComponent header(String text) {
        JLabel ret = SwingUtils.toBold(new JLabel(text));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public JComponent layoutDialogContent() {
        p = new MigPanel("ins 10 10 10 0, wrap 2", "[][grow,fill]", "[]");

        JLabel lbl;
        p.add(header(_GUI._.FFMpegInstallTypeChooserDialog_layoutDialogContent_problem()), "spanx");
        p.add(new JLabel(new AbstractIcon("ffmpeg", 32)), "gapleft 10,gapright 10");
        // p.add(textField, "spanx");
        p.add(new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_parameterd_2(task)), "spanx");

        p.add(header(_GUI._.FFMpegInstallTypeChooserDialog_layoutDialogContent_path_chooser()), "spanx");

        // p.add(textField, "gapleft 10,spanx");
        p.add(new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_solve2()), "gapleft 10,spanx");

        return p;
    }

    @Override
    protected int getPreferredWidth() {
        return Math.max(500, super.getPreferredWidth());
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public String getMessage() {
        return _GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_solve2();
    }

}
