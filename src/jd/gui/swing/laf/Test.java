package jd.gui.swing.laf;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        long t = System.currentTimeMillis();
        de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel");
        System.out.println(System.currentTimeMillis() - t);

    }
}
