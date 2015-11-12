package jd.gui.swing.jdgui;

public interface Flashable {

    void onFlashRegister();

    void onFlashUnRegister();

    boolean onFlash(long l);

}
