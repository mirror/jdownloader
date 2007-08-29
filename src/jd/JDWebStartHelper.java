package jd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;

import jd.captcha.JAntiCaptcha;
import jd.plugins.Plugin;
/**
 * Diese Klasse verhindert, daß beim Start als Applikation WebStart Klassen nicht gefunden werden.
 * Diese sind hier gekapselt 
 * 
 * @author astaldo
 */
public class JDWebStartHelper {
    private static Logger logger = Plugin.getLogger();
    
    public static String getJDHomeDirectoryFromWebStartCookie(){
        BasicService basicService; 
        PersistenceService persistentService; 
        try { 
            basicService = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService"); 
            persistentService = (PersistenceService)ServiceManager.lookup("javax.jnlp.PersistenceService"); 
        } 
        catch (UnavailableServiceException e) { 
            persistentService = null; 
            basicService = null; 
            logger.warning("PersistenceService not available.");
        } 
        try {
            if (persistentService != null && basicService != null) { 

                URL codebase = basicService.getCodeBase();
                FileContents fc=null;
                URL url =new URL(codebase.toString() + "JD_HOME"); 
                try {
                    fc = persistentService.get(url);
                }
                catch (Exception e) { }
                if(fc != null){
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fc.getInputStream()));
                    String directory = reader.readLine();
                    reader.close();
//                    homeDirectoryFile=new File(line);
//                    if(!homeDirectoryFile.exists()){
//                        homeDirectoryFile.mkdirs();
//                    }
                    logger.info("JD_HOME from WebStart:"+directory);
//                    homeDirectory = homeDirectoryFile.getAbsolutePath();
//                    return homeDirectoryFile;
                    return directory;
                }
                else{
                    logger.info("Creating new entry for JD_HOME");
                    String newHome = JOptionPane.showInputDialog("Bitte einen Pfad für jDownloader eingeben");
                    File homeDirectoryFile = new File(newHome);
                    boolean createSuccessfull=true;
                    if(!homeDirectoryFile.exists())
                        createSuccessfull = homeDirectoryFile.mkdirs();
                    if(createSuccessfull){
                        persistentService.create(url, 1024);
                        fc = persistentService.get(url);
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fc.getOutputStream(true)));
                        writer.write(homeDirectoryFile.getAbsolutePath());
                        writer.close();
//                        homeDirectory = homeDirectoryFile.getAbsolutePath();
                        // Da dies anscheinend eine Neuinstallation ist, wird direkt ein Update durchgeführt
                        JAntiCaptcha.updateMethods();
                        JDUtilities.getConfiguration().setDownloadDirectory(homeDirectoryFile.getAbsolutePath());
                        return homeDirectoryFile.getAbsolutePath();
                    }
                }
            }
        }
        catch (MalformedURLException e) { e.printStackTrace(); }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e)           { e.printStackTrace(); }
        return null;
    }
    /**
     * Liefert das HomeDirectory aus dem WebStart Cookie
     * 
     * @param newHomeDir Das neue JD-HOME
     * @return das neue HomeDirectory
     */
    public static String writeJDHomeDirectoryToWebStartCookie(String newHomeDir){
        String homeDirectory;
        BasicService basicService; 
        PersistenceService persistentService;
        homeDirectory = newHomeDir;
        try { 
            basicService = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService"); 
            persistentService = (PersistenceService)ServiceManager.lookup("javax.jnlp.PersistenceService"); 
        } 
        catch (UnavailableServiceException e) { 
            persistentService = null; 
            basicService = null; 
            logger.warning("PersistenceService not available.");
        } 
        try {
            if (persistentService != null && basicService != null) { 

                URL codebase = basicService.getCodeBase();
                FileContents fc=null;
                URL url =new URL(codebase.toString() + "JD_HOME"); 
                fc = persistentService.get(url);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fc.getOutputStream(true)));
                writer.write(homeDirectory);
                writer.close();
                return homeDirectory;
            }
        }
        catch (MalformedURLException e) { e.printStackTrace(); }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e)           { e.printStackTrace(); }
        return null;
    }
}
