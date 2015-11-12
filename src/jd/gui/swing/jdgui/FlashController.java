package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.appwork.exceptions.WTFException;

public class FlashController implements ActionListener {

    private final ArrayList<Flashable> list    = new ArrayList<Flashable>();
    private Timer                      timer;
    private long                       counter = 0l;

    public FlashController() {
    }

    public void register(Flashable button) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new WTFException("This has to be done in the EDT");
        }
        if (!list.contains(button)) {
            list.add(button);
            button.onFlashRegister(counter);
            if (timer == null) {
                timer = new Timer(1000, this);
                timer.setRepeats(true);
                timer.setInitialDelay(1000);
                timer.start();
            }
        }
    }

    public void unregister(Flashable button) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new WTFException("This has to be done in the EDT");
        }
        if (list.remove(button)) {
            if (list.size() == 0 && timer != null) {
                timer.stop();
                timer = null;
            }
            button.onFlashUnRegister(counter);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        counter++;
        final Iterator<Flashable> it = list.iterator();
        while (it.hasNext()) {
            final Flashable cur = it.next();
            if (!cur.onFlash(counter)) {
                it.remove();
            }
        }
        if (list.size() == 0 && timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public boolean isRegistered(Flashable button) {
        return list.contains(button);
    }
}
