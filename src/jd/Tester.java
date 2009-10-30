package jd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Tester {

    public static void main(String ss[]) throws Exception {
        String command = "G:\\Users\\coalado\\Desktop\\test.bat";
        // contents of test.bat: ping google.de -n 9999999
        final InputStream stderr;
        final InputStream stdout;

        Process process = Runtime.getRuntime().exec(command);
        stderr = process.getErrorStream();
        stdout = process.getInputStream();

        new Thread() {
            public void run() {
                BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(stdout));
                String line;
                try {
                    while ((line = brCleanUp.readLine()) != null) {
                        System.out.println("[Stdout] " + line);
                    }
                    brCleanUp.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            public void run() {
                BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(stderr));
                String line;
                try {
                    while ((line = brCleanUp.readLine()) != null) {
                        System.out.println("[Stderr] " + line);
                    }
                    brCleanUp.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }.start();

        Thread.sleep(5000);

        process.destroy();

    }
}