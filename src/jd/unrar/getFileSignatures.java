package jd.unrar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class getFileSignatures {

    private static void firl(File file)
    {
        int ad;
        FileInputStream fin;
        try {
            fin = new FileInputStream(file);
            int c = 0;
            BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
            System.out.print("{");
            while ((ad = myInput.read()) != -1) {
                System.out.print(ad+((c==5)?"":","));
                
                if(c++==5)
                    break;
            }
            System.out.print("}");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        File[] file = new File("/home/dwd/.jd_home").listFiles();
        for (int i = 0; i < file.length; i++) {
            if(file[i].isFile())
            {
                System.out.println("\n"+file[i].getName()+" ");
                firl(file[i]);
            }
        }

    }

}
