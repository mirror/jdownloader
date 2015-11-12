package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.appwork.exceptions.WTFException;

public class FlashController implements ActionListener {

    private ArrayList<Flashable> list;
    private Timer                timer;
    private long                 counter;
    private HashSet<Flashable>   set;

    public FlashController() {
        list = new ArrayList<Flashable>();
        set = new HashSet<Flashable>();
        counter = 0l;
    }

    public void register(Flashable button) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new WTFException("This has to be done in the EDT");
        }
        if (!set.add(button)) {
            return;
        }
        list.add(button);
        button.onFlashRegister();

        if (timer == null) {
            timer = new Timer(1000, this);
            timer.setRepeats(true);
            timer.setInitialDelay(1000);
            timer.start();
        }

    }

    public void unregister(Flashable button) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new WTFException("This has to be done in the EDT");
        }
        if (!set.remove(button)) {
            return;
        }
        list.remove(button);
        if (list.size() == 0 && timer != null) {
            timer.stop();
            timer = null;
        }
        button.onFlashUnRegister();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Iterator<Flashable> it = list.iterator();
        counter++;
        while (it.hasNext()) {
            Flashable cur;
            if (!(cur = it.next()).onFlash(counter)) {
                it.remove();
                set.remove(cur);
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
