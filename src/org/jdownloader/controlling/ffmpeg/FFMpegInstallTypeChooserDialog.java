package org.jdownloader.controlling.ffmpeg;

import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextPane;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.utils.Files;
import org.appwork.utils.Files.Handler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class FFMpegInstallTypeChooserDialog extends AbstractDialog<Object> implements FFMPegInstallTypeChooserDialogInterface {

    private MigPanel    p;
    private PathChooser pc;
    private JLabel      iconLbl;

    public FFMpegInstallTypeChooserDialog() {
        super(0, _GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_title(), null, _GUI._.lit_continue(), null);
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

    public static File searchFileIn(final File file, final Window owner, final boolean modeless) {
        try {
            final File[] ret = new File[1];
            Dialog.getInstance().showDialog(new ProgressDialog(new ProgressGetter() {
                private File current;

                @Override
                public void run() throws Exception {
                    File binary = null;
                    try {
                        if (file.isFile() && file.canExecute()) {
                            if (new FFmpeg(file.getAbsolutePath()).validateBinary()) {
                                ret[0] = file;
                                return;
                            }
                        }
                        Files.walkThroughStructure(new Handler<FoundException>() {
                            private HashSet<File> dupe;

                            {
                                dupe = new HashSet<File>();
                            }

                            @Override
                            public void intro(File f) throws FoundException {
                            }

                            @Override
                            public void onFile(File f) throws FoundException {
                                if (!dupe.add(f)) return;
                                if (Thread.interrupted()) throw new FoundException(null);
                                if (f.isDirectory()) current = f;
                                switch (CrossSystem.getOSFamily()) {
                                case WINDOWS:
                                    if (f.getName().equalsIgnoreCase("ffmpeg.exe") && f.isFile()) { throw new FoundException(f); }
                                    break;
                                case OTHERS:
                                    if (f.getName().equalsIgnoreCase("ffmpeg") && f.isFile() && f.canExecute()) { throw new FoundException(f); }
                                    break;
                                }
                            }

                            @Override
                            public void outro(File f) throws FoundException {
                            }
                        }, file);
                        binary = null;
                    } catch (FoundException e) {
                        binary = e.getFile();
                    }
                    ret[0] = binary;

                }

                @Override
                public String getString() {
                    return current == null ? null : current.getAbsolutePath();

                }

                @Override
                public int getProgress() {
                    return -1;
                }

                @Override
                public String getLabelString() {
                    return null;
                }
            }, 0, _GUI._.FFMpegInstallTypeChooserDialog_run_searching_(), _GUI._.lit_please_wait_dotdotdot(), new AbstractIcon(IconKey.ICON_FIND, 32), null, null) {
                public java.awt.Window getOwner() {
                    return owner;
                };

                @Override
                public ModalityType getModalityType() {
                    return modeless ? ModalityType.MODELESS : super.getModalityType();
                }
            });

            return ret[0];
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return null;
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
        JTextPane textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {

                return true;
            }
        };

        textField.setContentType("text/plain");

        textField.setText(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setFocusable(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        JLabel lbl;
        p.add(header(_GUI._.FFMpegInstallTypeChooserDialog_layoutDialogContent_problem()), "spanx");
        p.add(new JLabel(new AbstractIcon("ffmpeg", 32)), "gapleft 10");
        p.add(textField, "spanx");

        p.add(header(_GUI._.FFMpegInstallTypeChooserDialog_layoutDialogContent_path_chooser()), "spanx");
        textField = new JTextPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {

                return true;
            }
        };

        textField.setContentType("text/plain");

        textField.setText(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_solve());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setFocusable(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);
        p.add(textField, "gapleft 10,spanx");
        p.add(step(1), "gapleft 10");
        switch (CrossSystem.getOSFamily()) {
        case LINUX:
            lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_download_linux());

            break;
        case MAC:
            lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_download_mac());

            break;
        case WINDOWS:
            lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_download_windows());

            break;
        default:
            lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_download_others(CrossSystem.getOSString()));

        }
        lbl.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                CrossSystem.openURL("http://ffmpeg.org/");
            }
        });
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        p.add(lbl, "");
        p.add(step(2), "gapleft 10");
        lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_install());
        p.add(lbl, "");
        p.add(step(3), "gapleft 10");
        lbl = new JLabel(_GUI._.FFMpegInstallTypeChooserDialog_FFMpegInstallTypeChooserDialog_message_path());
        p.add(lbl, "");
        iconLbl = new JLabel();
        p.add(iconLbl, "alignx right");
        iconLbl.setIcon(new AbstractIcon("error", 20));
        pc = new PathChooser("ffmpeg") {
            {
                txt.setEditable(false);
            }

            @Override
            public FileChooserSelectionMode getSelectionMode() {
                return FileChooserSelectionMode.FILES_AND_DIRECTORIES;
            }

            protected String getHelpText() {
                return _GUI._.FFMpegInstallTypeChooserDialog_layoutDialogContent_help_();

            };

            public void setSuperFile(final File file) {
                if (file != null) {
                    super.setFile(file);
                }
            }

            @Override
            public void setFile(final File file) {
                getTxt().setText(_GUI._.FFMpegInstallTypeChooserDialog_run_searching_());

                new Thread("Search") {
                    public void run() {
                        final File finalBinary = searchFileIn(file, FFMpegInstallTypeChooserDialog.this.getDialog(), false);

                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {

                                if (finalBinary == null || !finalBinary.exists()) {
                                    iconLbl.setIcon(new AbstractIcon("error", 18));
                                    getTxt().setText(getHelpText());

                                    okButton.setEnabled(false);
                                } else {
                                    FFmpeg ff = new FFmpeg();
                                    ff.setPath(finalBinary.getAbsolutePath());
                                    if (ff.validateBinary()) {
                                        iconLbl.setIcon(new AbstractIcon("ok", 18));
                                        okButton.setEnabled(true);
                                        setSuperFile(finalBinary);
                                    } else {
                                        iconLbl.setIcon(new AbstractIcon("error", 18));
                                        getTxt().setText(getHelpText());

                                        okButton.setEnabled(false);
                                    }
                                }
                            }
                        };

                    }

                }.start();

            }

            @Override
            protected void onChanged(ExtTextField txt2) {
                super.onChanged(txt2);

            }
        };
        p.add(pc, "");
        return p;
    }

    private JLabel step(int i) {
        JLabel ret = new JLabel(_GUI._.lit_step_x(i));
        SwingUtils.toBold(ret);
        ret.setEnabled(false);
        return ret;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public String getFFmpegBinaryPath() {
        return pc.getPath();
    }

}
