package org.jdownloader.phantomjs;

import java.awt.Dialog.ModalityType;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;

public class DebugWindow extends AbstractDialog<Object> {
    private PhantomJS pjs;
    private Thread    thread;
    private MigPanel  cp;

    @Override
    public void dispose() {
        super.dispose();
        thread.interrupt();
    }

    public static DebugWindow show(final PhantomJS js) {
        final DebugWindow window = new DebugWindow(js);
        new Thread() {
            public void run() {
                UIOManager.I().show(null, window);
            };
        }.start();
        return window;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    public DebugWindow(PhantomJS phantomJS) {
        super(UIOManager.BUTTONS_HIDE_OK, "PhantomJS View Debugger", null, null, "close");
        setLocator(new RememberAbsoluteDialogLocator("PhantomJS Debugger"));
        this.pjs = phantomJS;

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        cp = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");

        cp.add(new JLabel("Phantom Debugger"));
        thread = new Thread("Debugger") {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        if (isDisposed()) {
                            return;
                        }
                        final Image sh = pjs.getScreenShot();
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {

                                cp.removeAll();
                                JLabel lbl;
                                cp.add(lbl = new JLabel(new ImageIcon(sh)));
                                lbl.addMouseListener(new MouseAdapter() {
                                    public void mouseClicked(java.awt.event.MouseEvent e) {
                                        try {
                                            // pjs.eval("page.sendEvent('mousedown', " + e.getX() + ", " + e.getY() + ",'left');");
                                            // pjs.eval("page.sendEvent('mouseup', " + e.getX() + ", " + e.getY() + ",'left');");
                                            pjs.eval("page.sendEvent('click', " + e.getX() + ", " + e.getY() + ",'left');");
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        }

                                    };

                                });
                                cp.repaint();
                                cp.revalidate();
                                pack();
                            }
                        };
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        return cp;
    }

}
