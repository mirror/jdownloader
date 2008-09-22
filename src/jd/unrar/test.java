package jd.unrar;

import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class test {
    public static void main(String[] args) {
        // /home/dwd/.jd_home/downloads/Read Me.rar
        try {

            ProcessBuilder pb = new ProcessBuilder(new String[] {"C:\\Users\\coalado\\.jd_home\\tools\\windows\\unrarw32\\unrar.exe", "e", "test.rar"});
            pb.directory(new File("c:\\"));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            InputStreamReader inp = new InputStreamReader(p.getInputStream());
            int c;
            StringBuffer buff = new StringBuffer();
            while ((c = inp.read()) != -1) {
                // type type = (type) en.nextElement();
                buff.append((char) c);
                System.out.print((char) c);
                if (buff.indexOf("password") != -1 && c == ':') {
                    c = inp.read();
                    System.out.print((char) c);
                    Thread.sleep(200);
                    PrintStream s = new PrintStream(p.getOutputStream());
                    s.print("test\r\n");
                    s.flush();
                    s.close();
                    System.out.println("test");
//                    c = inp.read();
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
