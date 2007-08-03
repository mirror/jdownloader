package jd;

import java.io.File;

import jd.gui.MainWindow;
import jd.router.Parser;
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
        Parser parser = new Parser();
        parser.parseFile(new File("c:/programme/RouterControl/Routers.dat"));
        if(true)
            return;
        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}
