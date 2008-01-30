package jd.update;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;


public class utils {
   
    public static Object loadObject(File fileInput) {
 
        Object objectLoaded = null;    
        if (fileInput != null) {
      
            try {
                FileInputStream fis = new FileInputStream(fileInput);             
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    objectLoaded = ois.readObject();
                    ois.close();       
                return objectLoaded;
            }
           catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
     
    }


    public static void saveObject(Object objectToSave, File fileOutput) {
   
        if (fileOutput != null) {          
         
            if (fileOutput.exists()) fileOutput.delete();
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
             
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(objectToSave);
                    oos.close();             
            }
            catch (IOException e) {
                e.printStackTrace();
            }

      
            }
        }

    
    
}