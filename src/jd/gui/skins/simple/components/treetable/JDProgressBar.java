package jd.gui.skins.simple.components.treetable;

import javax.swing.JProgressBar;

public class JDProgressBar extends JProgressBar {
    /**
     * Diese Klasse skaliert die Werte der JProgressbar auf Integer ranges herunter
     */
    private static final long serialVersionUID = 7787146508749392032L;
    private int faktor = 1;

    public void setMaximum(long value) {
        while ((value / faktor) >= Integer.MAX_VALUE) {
            increaseFaktor();

        }
        this.setMaximum((int) (value / faktor));
    }

    public void setValue(long value) {
        while ((value / faktor) >= Integer.MAX_VALUE) {
            increaseFaktor();

        }
        this.setValue((int) (value / faktor));
    }

    private void increaseFaktor() {
        faktor += 2;
        this.setValue(getValue() / 2);
        this.setMaximum(getMaximum() / 2);
    }

}
