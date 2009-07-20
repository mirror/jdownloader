package jd.gui.skins.jdgui.actions;

import java.awt.event.ActionEvent;

public abstract class ThreadedAction extends ToolBarAction {

    private boolean actionrunning = false;
    private boolean multiplethreadsallowed = false;

    public ThreadedAction(String menukey, String iconkey) {
        super(menukey, iconkey);
    }

    public ThreadedAction(String menukey, String iconkey, boolean allowmultiplethreads) {
        super(menukey, iconkey);
        multiplethreadsallowed = allowmultiplethreads;
    }

    private static final long serialVersionUID = -1483816271981451352L;

    /*
     * this action is performed in its own thread, explicit edt queueing is
     * needed
     */
    public void actionPerformed(final ActionEvent e) {
        if (actionrunning && !multiplethreadsallowed) return;
        actionrunning = true;
        new Thread() {
            public void run() {
                this.setName(getID());
                try {
                    threadedActionPerformed(e);
                } catch (Exception e) {
                }
                actionrunning = false;
            }
        }.start();
    }

    public abstract void threadedActionPerformed(ActionEvent e);

}
