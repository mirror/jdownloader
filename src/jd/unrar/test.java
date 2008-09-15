package jd.unrar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class test {
public static void main(String[] args) {
    ///home/dwd/.jd_home/downloads/Read Me.rar
    try {
        ProcessBuilder pb = new ProcessBuilder(new String[] {"unrar","-ierr", "e", "ReadMe.rar"});
        pb.directory(new File("/home/dwd/.jd_home/downloads/"));
        Process p = pb.start();
        BufferedInputStream buffer =
            new BufferedInputStream(p.getErrorStream());
            BufferedReader commandResult =
            new BufferedReader(new InputStreamReader(buffer));

            BufferedOutputStream bufferout =
            new BufferedOutputStream(p.getOutputStream());
            PrintWriter commandInput =
            new PrintWriter((new OutputStreamWriter(bufferout)), true);
            String s = null;
            try {

            while (true){
            s = commandResult.readLine();

            System.out.println("Output: " + s);
            System.out.flush();
            if(s.contains("password"))
            break;
            }
            System.out.println("test");
            Thread.sleep(100);
            commandInput.write("test\r\n");
            commandInput.flush();
            while ((s = commandResult.readLine()) != null)
            System.out.println("Output: " + s);

            commandInput.close();
            commandResult.close();

            if (p.exitValue() != 0) {
            System.out.println(" -- p.exitValue() != 0");
            }
            } catch (Exception e) {
            System.out.println("Exception ::" + e);
            }
            
    } catch (Exception e) {
        // TODO: handle exception
    }
}
}
