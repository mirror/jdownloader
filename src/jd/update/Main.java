package jd.update;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;


public class Main {
  
    /**
     * @param args
     */









    public static void main(String args[]) {
         final StringBuffer  log    = new StringBuffer();
         try {
             UIManager.setLookAndFeel(new MetalLookAndFeel());
         }
         catch (UnsupportedLookAndFeelException e) {
         }
        JFrame frame= new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JD Update");
         final JTextArea logWindow = new JTextArea(10, 60);
         JScrollPane scrollPane = new JScrollPane(logWindow);
         logWindow.setEditable(true);
         
      
             frame.add(new JLabel("Webupdate is running..."),BorderLayout.NORTH);
     
       log.append("Starting...");
             logWindow.setText(log.toString());
         
         frame.add(scrollPane,BorderLayout.CENTER);
   
         frame.pack();
         frame.setVisible(true);
         
      
         boolean all=false;
         boolean restart=false;
         int runtype=1;
       for( int i=0; i<args.length;i++){
           
           if(args[i].trim().equalsIgnoreCase("/all"))all=true;
           if(args[i].trim().equalsIgnoreCase("/restart"))restart=true;
           if(args[i].trim().equalsIgnoreCase("/rt0"))runtype=0;
           if(args[i].trim().equalsIgnoreCase("/rt1"))runtype=1;
           if(args[i].trim().equalsIgnoreCase("/rt2"))runtype=2;
           log.append("Parameter "+i+" "+args[i]+" "+System.getProperty("line.separator"));
           logWindow.setText(log.toString());
       }
       new Thread(){
           public void run(){
               while(true){
                   logWindow.setText(log.toString());
                   try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {}
               }
           }
       }.start();
        WebUpdater updater = new WebUpdater(null);
        updater.setLogger(log);
        trace("Start Webupdate");
        Vector<Vector<String>> files = updater.getAvailableFiles();
        boolean success=false;
        if (files != null) {
           // int totalFiles = files.size();
            updater.filterAvailableUpdates(files);
           updater.updateFiles(files);
        }

        trace(updater.getLogger().toString());
        trace("End Webupdate");
        logWindow.setText(log.toString());
        trace(new File("updateLog.txt").getAbsoluteFile());
      
        if(restart){
            if(new File("webcheck.tmp").exists()){
                new File("webcheck.tmp").delete(); 
            }
            if(runtype==2){
                log.append("Start java -jar JDownloader.jar in "+new File("").getAbsolutePath());
                runCommand("java", new String[] { "-jar","jDownloader.jar" },new File("").getAbsolutePath(), 0);
            }else if(runtype==1 &&new File("jd/Main.class").exists()){
                log.append("java Main.class in "+new File("jd/").getAbsolutePath());
                runCommand("java", new String[] { "Main.class" },new File("jd/").getAbsolutePath(), 0);
            }else{
                log.append("Start jDownloader.jnlp in "+new File("").getAbsolutePath());
                runCommand("javaws", new String[] { "jDownloader.jnlp" },new File("").getAbsolutePath(), 0);
                
            }
           
        }
        logWindow.setText(log.toString());
        writeLocalFile(new File("updateLog.txt"), log.toString());
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {}
        System.exit(0);
    }

   public static void trace(Object arg){
      try{
          System.out.println(arg.toString());
      }catch(Exception e){
          System.out.println(arg);
      }
   }
   /**
    * Schreibt content in eine Lokale textdatei
    * 
    * @param file
    * @param content
    * @return true/False je nach Erfolg des Schreibvorgangs
    */
   public static boolean writeLocalFile(File file, String content) {
       try {
           if (file.isFile()) {
               if (!file.delete()) {
                   
                   return false;
               }
           }
           if (file.getParent() != null && !file.getParentFile().exists()) {
               file.getParentFile().mkdirs();
           }
           file.createNewFile();
           BufferedWriter f = new BufferedWriter(new FileWriter(file));
           f.write(content);
           f.close();
           return true;
       }
       catch (Exception e) {
           //  e.printStackTrace();
           return false;
       }
   }
   /**
    * Führt einen Externen befehl aus.
    * 
    * @param command
    * @param parameter
    * @param runIn
    * @param waitForReturn
    * @return null oder die rückgabe des befehls falls waitforreturn == true
    *         ist
    */
   public static String runCommand(String command, String[] parameter, String runIn, int waitForReturn) {

       if (parameter == null) parameter = new String[] {};
       String[] params = new String[parameter.length + 1];
       params[0] = command;
       System.arraycopy(parameter, 0, params, 1, parameter.length);
       Vector<String> tmp = new Vector<String>();
       String par = "";
       for (int i = 0; i < params.length; i++) {
           if (params[i] != null && params[i].trim().length() > 0) {
               par += params[i] + " ";
               tmp.add(params[i].trim());
           }
       }
       

       params = tmp.toArray(new String[] {});
       ProcessBuilder pb = new ProcessBuilder(params);
       
       
       if (runIn != null && runIn.length() > 0) {
           if (new File(runIn).exists()) {
               pb.directory(new File(runIn));
           }
           else {
              trace("Working drectory " + runIn + " does not exist!");
           }
       }
       Process process;

       try {
          trace("Start " + par + " in " + runIn + " wait " + waitForReturn);
           process = pb.start();
           if (waitForReturn > 0) {
               long t = System.currentTimeMillis();
               while (true) {
                   try {
                       process.exitValue();
                       break;
                   }
                   catch (Exception e) {
                       if (System.currentTimeMillis() - t > waitForReturn * 1000) {
                           trace(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                           process.destroy();
                       }
                   }
               }
               Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");
               String ret = "";
               while (s.hasNext())
                   ret += s.next();
               return ret;
           }
           return null;
       }
       catch (Exception e) {
            e.printStackTrace();
           trace("Error executing " + command + ": " + e.getLocalizedMessage());
           return null;
       }
   }
}
