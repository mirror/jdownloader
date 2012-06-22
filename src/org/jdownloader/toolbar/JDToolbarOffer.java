package org.jdownloader.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.Executer;

import org.appwork.app.gui.copycutpaste.CopyCutPasteHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class JDToolbarOffer extends AbstractDialog<Object> implements Runnable {

    private JCheckBox cbSearch;
    private JCheckBox cbhp;

    public JDToolbarOffer() {
        super(0, _GUI._.JDToolbarOffer_JDToolbarOffer_title_(), null, _GUI._.JDToolbarOffer_JDToolbarOffer_install_(), _GUI._.JDToolbarOffer_JDToolbarOffer_nothanks_());
    }

    public static void main(String[] args) {
        LookAndFeelController.getInstance().setUIManager();
        try {
            Dialog.getInstance().showDialog(new JDToolbarOffer());
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(final ActionEvent e) {

        if (e.getSource() == this.okButton) {

            install(cbSearch.isSelected(), cbhp.isSelected());
            Log.L.fine("Answer: Button<OK:" + this.okButton.getText() + ">");
            this.setReturnmask(true);
        } else if (e.getSource() == this.cancelButton) {
            Log.L.fine("Answer: Button<CANCEL:" + this.cancelButton.getText() + ">");
            this.setReturnmask(false);
        }
        this.dispose();
    }

    private void install(boolean search, boolean hp) {
        final ArrayList<String> ret = new ArrayList<String>();
        final StringBuilder sb = new StringBuilder();
        sb.append("toolbar.exe ");
        sb.append("-ie ");
        sb.append("-ff ");
        sb.append("-ch ");
        sb.append("-ie ");
        sb.append("-ctid=CT3175297 ");
        sb.append("-openwelcomedialog=FALSE ");
        sb.append("-showpersonalcompdialog=FALSE ");

        if (search) {
            sb.append("-defaultsearch=TRUE ");

        } else {
            sb.append("-defaultsearch=FALSE ");
        }

        if (hp) {
            sb.append("-startpage=TRUE");
        } else {
            sb.append("-startpage=FALSE");
        }

        ProgressGetter getter = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {

                Executer exec = new Executer("cmd.exe");
                LogSource l = LogController.getInstance().getLogger("JDToolBarOffer");
                l.setInstantFlush(true);
                exec.setLogger(l);
                exec.addParameter("/C");
                exec.addParameter("\"" + sb.toString() + "\"");
                exec.setRunin(Application.getHome());
                exec.start();
                exec.waitTimeout();
                final File file = Application.getResource("cfg/JDToolbarOffer.dat");
                if (exec.getException() != null) {
                    file.delete();
                    Dialog.getInstance().showExceptionDialog("Failed", "Installation Failed:", exec.getException());
                    throw exec.getException();
                }

                try {
                    file.delete();
                    IO.writeStringToFile(file, ret.toString());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public String getString() {
                return "Installing - Please wait";
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        try {
            Dialog.getInstance().showDialog(new ProgressDialog(getter, 0, "Installing JDownloader Controlbar", null, NewTheme.I().getIcon("wait", 32)));

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (true) return;
        final File file = Application.getResource("cfg/JDToolbarOffer.dat");

        // only show once
        if (file.exists()) return;
        new Thread("WaitForMouse") {
            public void run() {
                while (true) {
                    if (System.currentTimeMillis() - CopyCutPasteHandler.getInstance().getLastMouseEvent() < 5000) {
                        try {
                            file.delete();
                            IO.writeStringToFile(file, "true");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        try {
                            Dialog.getInstance().showDialog(new JDToolbarOffer());
                        } catch (DialogClosedException e) {
                            e.printStackTrace();
                        } catch (DialogCanceledException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }.start();

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel panel = new MigPanel("ins 0,wrap 1", "[]", "[][]10[]10[]20[]2[]10[]0[]");
        // panel.setPreferredSize(new Dimension(525, 301));

        JLabel header = new JLabel(_GUI._.JDToolbarOffer_layoutDialogContent_toolbar_());
        final Font f = header.getFont();
        header.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD, 13));

        JLabel screenshot = new JLabel();

        screenshot.setIcon(NewTheme.I().getIcon("toolbarss", -1));

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFocusable(false);
        SwingUtils.setOpaque(desc, false);
        JTextArea desc2 = new JTextArea();
        desc2.setEditable(false);
        desc2.setLineWrap(true);
        desc2.setWrapStyleWord(true);
        desc2.setFocusable(false);
        SwingUtils.setOpaque(desc2, false);
        // desc.setEnabled(false);
        desc.setText(_GUI._.JDToolbarOffer_layoutDialogContent_desc_());
        desc2.setText(_GUI._.JDToolbarOffer_layoutDialogContent_desc2_());
        panel.add(header, "pushx,spanx");
        panel.add(desc, "gaptop 0,spanx,growx,pushx,wmin 10");
        panel.add(screenshot, "pushx,spanx");
        panel.add(desc2, "gaptop 0,spanx,growx,pushx,wmin 10");

        // cbPanel=new MigPanel("ins 0 32 0 0", [], rows)

        final JLabel lblSearch = (new JLabel("Make the JDownloader web search my default search engine"));
        final JLabel lblHp = (new JLabel("Make the JDownloader web search my home page"));

        cbSearch = new JCheckBox();
        cbhp = new JCheckBox();

        cbSearch.setSelected(true);
        cbhp.setSelected(true);

        panel.add(cbSearch, "split 2,gapleft 10");
        panel.add(lblSearch);
        panel.add(cbhp, "split 2,gapleft 10");
        panel.add(lblHp, "");
        panel.add(Box.createHorizontalGlue(), "pushx,growx,split 6");
        panel.add(toGray(new JLabel("By installing, you agree to the Controlbar's ")));
        panel.add(toUrl("http://jdownloader.ourtoolbar.com/eula/", new JLabel("<html><u>End User License Agreement</u></html>")), "gapleft 0");
        panel.add(toGray(new JLabel(" and ")), "gapleft 0");
        panel.add(toUrl("http://jdownloader.ourtoolbar.com/privacy/", new JLabel("<html><u>Privacy Policy</u></html>")), "gapleft 0");
        panel.add(toGray(new JLabel(". You may access")), "gapleft 0");
        panel.add(Box.createHorizontalGlue(), "pushx,growx,split 6");
        panel.add(toGray(new JLabel("content or features that require use of personal information. See our ")), "gapleft 0");
        panel.add(toUrl("http://jdownloader.ourtoolbar.com/contentpolicy/", new JLabel("<html><u>Content Policy</u></html>")), "gapleft 0");
        panel.add(toGray(new JLabel(" for more information.")), "gapleft 0");
        return panel;
    }

    private Component toUrl(final String url, JLabel gray) {
        gray.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
                CrossSystem.openURL(url);
            }
        });

        gray.setFont(gray.getFont().deriveFont(10f));
        gray.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gray.setForeground(Color.BLUE.darker());
        return gray;
    }

    private JLabel toGray(JLabel jLabel) {
        jLabel.setForeground(Color.GRAY.darker());
        jLabel.setFont(jLabel.getFont().deriveFont(10f));
        return jLabel;
    }
}
