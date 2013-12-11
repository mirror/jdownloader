package jd.gui.swing.jdgui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class OSRSurvey {
    private static final OSRSurvey INSTANCE = new OSRSurvey();

    /**
     * get the only existing instance of OSRSurvey. This is a singleton
     * 
     * @return
     */
    public static OSRSurvey getInstance() {
        return OSRSurvey.INSTANCE;
    }

    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Create a new instance of OSRSurvey. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private OSRSurvey() {

    }

    public void start() {
        if (running.get()) return;
        running.set(true);
        try {
            ConfirmDialog d = new ConfirmDialog(0, _GUI._.osr_dialog_title(), "", null, _GUI._.osr_start(), null) {
                private AbstractIcon header;
                {
                    header = new AbstractIcon("fau_osr_header", -1);
                }

                @Override
                protected int getPreferredWidth() {
                    return super.getPreferredWidth();
                }

                protected DefaultButtonPanel createBottomButtonPanel() {
                    // TODO Auto-generated method stub

                    return new DefaultButtonPanel("ins 0 0 0 5", "[]", "0[grow,fill]0");

                }

                @Override
                protected boolean isResizable() {
                    return false;
                }

                @Override
                public JComponent layoutDialogContent() {
                    this.getDialog().setLayout(new MigLayout("ins 0 0 5 0,wrap 1", "[]", "[][]"));
                    final MigPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");
                    p.add(new JLabel(header), "");

                    JLabel lbl;
                    p.add(lbl = new JLabel(_GUI._.osr_dialog_message()) {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(header.getIconWidth() - 10, super.getPreferredSize().height);
                        }
                    }, "gapleft 5,gapright 5");
                    lbl.addComponentListener(new ComponentListener() {

                        @Override
                        public void componentShown(ComponentEvent e) {
                        }

                        @Override
                        public void componentResized(ComponentEvent e) {
                            // setResizable(true);
                            getDialog().pack();

                            // setResizable(false);
                        }

                        @Override
                        public void componentMoved(ComponentEvent e) {
                        }

                        @Override
                        public void componentHidden(ComponentEvent e) {
                        }
                    });

                    return p;
                }

                @Override
                protected void packed() {

                    super.packed();

                }

                @Override
                public ModalityType getModalityType() {
                    return ModalityType.MODELESS;
                }
            };

            UIOManager.I().show(null, d);

        } finally {
            running.set(false);
        }
    }
}
