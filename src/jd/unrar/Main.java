package jd.unrar;

import java.io.File;
import java.util.Map;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Unrar unrar = new Unrar(System.getProperty("user.dir"));

        for (int i = 0; i < args.length; i++) {
            if ((args[i].matches("-a") && args.length > i) || args[i].matches("--add=.*")) {
                if (args[i].matches("-a"))
                    i++;
                else
                    args[i] = args[i].replaceFirst("--add=", "");
                File file = new File(args[i]);
                if (file.isFile())
                    unrar.addToPasswordlist(file);
                else
                    unrar.addToPasswordlist(args[i]);
            } else if (args[i].matches("-h") || args[i].matches("--help")) {
                String[] helpText = {"-a, --add= \t (file/password) add passwords to the passwordlist", "-c, --setc= \t set the path to the unrarcommand", "-m, --maxsize= \t set the maximal filesize for passwordsearch", "--showmaxsize \t shows the maximal filesize for passwordsearch", "-s, --showlist \t shows the passwordlist", "-h, --help \t shows this helptext"};
                for (int j = 0; j < helpText.length; j++) {
                    System.out.println(helpText[j]);
                }
            } else if (args[i].matches("-s") || args[i].matches("--showlist")) {
                for (Map.Entry<String, Integer> entry : unrar.passwordlist.entrySet()) {
                    System.out.println(entry.getKey() + "\t" + entry.getValue());
                }
            } else if ((args[i].matches("-c") && args.length > i) || args[i].matches("--setc=.*")) {
                if (args[i].matches("-c"))
                    i++;
                else
                    args[i] = args[i].replaceFirst("--setc=", "");
                unrar.setUnrarCommand(args[i]);
            } else if ((args[i].matches("-m") && args.length > i) || args[i].matches("--maxsize=.*")) {

                if (args[i].matches("-m"))
                    i++;
                else
                    args[i] = args[i].replaceFirst("--maxsize=", "");
                unrar.config.maxFilesize = Integer.parseInt(args[i]);
                System.out.println(unrar.config.maxFilesize);
                unrar.saveConfig();
            } else if (args[i].matches("--showmaxsize")) {
                System.out.println(unrar.config.maxFilesize);

            } else if ((args[i].matches("-p") && args.length > i) || args[i].matches("--password=.*")) {

                if (args[i].matches("-p"))
                    i++;
                else
                    args[i] = args[i].replaceFirst("--password=", "");
                unrar.standardPassword=args[i];
                unrar.unrar();
            }
        }

        if (args.length == 0) {
            unrar.unrar();
        }

    }
}
