package jd.unrar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class test {
public static void main(String[] args) {
    ///home/dwd/.jd_home/downloads/Read Me.rar
    try {
        ProcessBuilder pb = new ProcessBuilder(new String[] {"unrar", "-ierr", "e", "ad.rar"});
        pb.directory(new File("/home/dwd/.jd_home/downloads/"));
        final Process p = pb.start();
        
         InputStreamReader inp = new InputStreamReader(p.getErrorStream());


        int c;
        StringBuffer buff = new StringBuffer();
        while ((c=inp.read())!=-1) {
//            type type = (type) en.nextElement();
            buff.append((char)c);
            System.out.print((char)c);
            if(buff.indexOf("password") != -1 && c==':')
            {
                            BufferedWriter opw = new BufferedWriter(new OutputStreamWriter(p
                                    .getOutputStream()));
                            opw.write("test");
                            opw.newLine();
                            opw.flush();
                            opw.close();
                            System.out.println("test");
                            
            }
        }
            
    } catch (Exception e) {
        // TODO: handle exception
    }
}
}
