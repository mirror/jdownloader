package jd;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import jd.gui.MainWindow;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
/**
 * Start der Applikation
 * 
 * TODO Grundgerüst erweitern
 * TODO GUI
 * TODO Reconnect Paket
 * TODO Konfiguration speichern
 * 
 * @author astaldo
 */

public class Main {
    public static void main(String args[]){
        Main main = new Main();
        main.go();
    }
    private void go(){
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        }
        catch (UnsupportedLookAndFeelException e) {}

        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}
