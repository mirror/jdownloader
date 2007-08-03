package jd;

import jd.gui.MainWindow;
/**
 * Start der Applikation
 * 
 * @author astaldo
 */
public class Main {
    public static void main(String args[]){
        Main main = new Main();
        main.go();
    }
    private void go(){
        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}
