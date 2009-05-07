package jd.controlling;

import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDLogger {
    private static Logger LOGGER = null;
    public static String LOGGER_NAME = "java_downloader";
    private static ConsoleHandler console;

    /**
     * Liefert die Klasse zurück, mit der Nachrichten ausgegeben werden können
     * Falls dieser Logger nicht existiert, wird ein neuer erstellt
     * 
     * @return LogKlasse
     */
    public static Logger getLogger() {

        if (LOGGER == null) {

            LOGGER = Logger.getLogger(LOGGER_NAME);
            Formatter formatter = new LogFormatter();
            LOGGER.setUseParentHandlers(false);
         
            console = new ConsoleHandler();
            console.setLevel(Level.ALL);
            console.setFormatter(formatter);
            LOGGER.addHandler(console);            

            LOGGER.setLevel(Level.ALL);
            LOGGER.addHandler(JDLogHandler.getHandler());
            JDLogHandler.getHandler().setFormatter(formatter);
           
        }
        return LOGGER;
    }

    public static void exception(Exception e) {
        exception(Level.SEVERE,e);
        
    }

    public static void removeConsoleHandler() {
      if(console!=null) getLogger().removeHandler(console);
      System.err.println("Removed Consolehandler. Start with -debug to see console output");
       
    }

    public static void addHeader(String string) {
       getLogger().info("\r\n\r\n--------------------------------------"+string+"-----------------------------------");
        
    }

    public static void exception(Level finest, Exception e) {
        getLogger().log(finest,finest.getName()+" Exception occured",e);
        
    }
    

}
