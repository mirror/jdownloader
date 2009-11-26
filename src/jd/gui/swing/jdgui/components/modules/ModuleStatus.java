package jd.gui.swing.jdgui.components.modules;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.GuiRunnable;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ModuleStatus extends JPanel implements ControlListener, MouseListener {

    private static final long serialVersionUID = 1745881766942067472L;
    private static final int BARCOUNT = 15;
    private ArrayList<ProgressController> controllers;
    private JLabel title;
    private ProgressCircle[] circles;

    public ModuleStatus() {
        super(new MigLayout("ins 0", "", "[::20, center]"));
        controllers = new ArrayList<ProgressController>();
        circles = new ProgressCircle[BARCOUNT];

        setName("Module Statusbar");
        add(title = new JLabel(), "hmax 20");
        for (int i = 0; i < BARCOUNT; i++) {
            circles[i] = new ProgressCircle();
            circles[i].setOpaque(false);
            circles[i].addMouseListener(this);
            circles[i].setVisible(false);
            add(circles[i], "hidemode 3, hmax 20");
        }
        this.setOpaque(false);

        JDUtilities.getController().addControlListener(this);
    }

    private void addController(ProgressController source) {
        synchronized (controllers) {
            if (!controllers.contains(source)) controllers.add(0, source);
        }
    }

    private void removeController(ProgressController source) {
        synchronized (controllers) {
            controllers.remove(source);
        }
    }

    public void controlEvent(ControlEvent event) {
        synchronized (controllers) {
            if (event.getID() == ControlEvent.CONTROL_ON_PROGRESS && event.getSource() instanceof ProgressController) {
                ProgressController source = (ProgressController) event.getSource();
                addController(source);
                if (source.isFinished()) removeController(source);
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        update();
                        return null;
                    }
                }.start();
            }
        }
    }

    private void update() {
        synchronized (controllers) {
            if (controllers.isEmpty()) {
                title.setText("");
            } else {
                title.setText(JDL.LF("gui.progresspane.title", "%s module(s) running", controllers.size()));
            }
            int i;
            for (i = 0; i < Math.min(BARCOUNT, controllers.size()); ++i) {
                circles[i].setController(controllers.get(i));
                if (controllers.get(i).isInterruptable()) {
                    circles[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    circles[i].addMouseListener(this);
                } else {
                    circles[i].setCursor(Cursor.getDefaultCursor());
                    circles[i].removeMouseListener(this);
                }
                circles[i].setVisible(true);
            }
            for (int j = i; j < BARCOUNT; ++j) {
                circles[j].setVisible(false);
            }
            revalidate();
            repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() instanceof ProgressCircle) {
            ProgressController controller = ((ProgressCircle) e.getSource()).getController();
            if (controller != null && !controller.isAbort()) controller.fireCancelAction();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
