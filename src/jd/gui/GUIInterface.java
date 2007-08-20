package jd.gui;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.ClipboardHandler;
import jd.controlling.StartDownloads;
import jd.controlling.event.ControlListener;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginListener;

public abstract class GUIInterface implements PluginListener, ControlListener, ClipboardOwner{

    protected final String JD_TITLE  = "jDownloader 0.0.1";
    protected final Image JD_ICON = JDUtilities.getImage("mind");
    /**
     * Der Thread, der das Downloaden realisiert
     */
    protected StartDownloads download = null;
    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    protected File currentDirectory = null;    
    /**
     * Methode, um eine Veränderung der Zwischenablage zu bemerken und zu verarbeiten
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        new ClipboardHandler(this).start();
    }
    /**
     * Verarbeitet Aktionen
     * 
     * @param actionID eine Aktion
     */
    public abstract void doAction(int actionID);
    /**
     * Liefert das Hauptfenster zurück oder null
     * @return Das Hauptfenster
     */
    public abstract JFrame getFrame();
    public abstract DownloadLink getNextDownloadLink();
    protected abstract Vector<DownloadLink> getDownloadLinks();
    protected abstract void setDownloadLinks(Vector<DownloadLink> links);
    protected void startStopDownloads(){
        if(download == null){
            download = new StartDownloads(this, Configuration.getInteractions());
            download.addControlListener(this);
            download.start();
        }
        else{
            download.interrupt();
            download=null;
        }
    }
    /**
     * Speichert die Links in einer Datei
     */
    protected void saveFile(){
        JFileChooser fileChooserSave = new JFileChooser();
        if(currentDirectory != null)
            fileChooserSave.setCurrentDirectory(currentDirectory);
        if(fileChooserSave.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION){
            File fileOutput = fileChooserSave.getSelectedFile();
            currentDirectory = fileChooserSave.getCurrentDirectory();
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                Vector<DownloadLink> links = getDownloadLinks();
                oos.writeObject(links);
                oos.close();
            }
            catch (FileNotFoundException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
    }
    protected void loadFile(){
        JFileChooser fileChooserLoad = new JFileChooser();
        if(currentDirectory != null)
            fileChooserLoad.setCurrentDirectory(currentDirectory);
        if(fileChooserLoad.showOpenDialog(getFrame()) == JFileChooser.APPROVE_OPTION){
            File fileInput = fileChooserLoad.getSelectedFile();
            currentDirectory = fileChooserLoad.getCurrentDirectory();
            try {
                FileInputStream fis = new FileInputStream(fileInput);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Vector<DownloadLink> links;
                try {
                    links = (Vector<DownloadLink>)ois.readObject();
                    PluginForHost neededPlugin;
                    for(int i=0;i<links.size();i++){

                        neededPlugin = JDUtilities.getPluginForHost(links.get(i).getHost());
                        links.get(i).setPlugin(neededPlugin);
                    }
                    setDownloadLinks(links);
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                ois.close();
            }
            catch (FileNotFoundException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
    }
}
