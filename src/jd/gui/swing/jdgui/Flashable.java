package jd.gui.swing.jdgui;

public interface Flashable {

    void onFlashRegister(long counter);

    void onFlashUnRegister(long counter);

    boolean onFlash(long l);

}
