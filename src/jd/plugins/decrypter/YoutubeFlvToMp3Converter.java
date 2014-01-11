package jd.plugins.decrypter;

import jd.plugins.DownloadLink;

public class YoutubeFlvToMp3Converter implements YoutubeConverter {
    private static final YoutubeFlvToMp3Converter INSTANCE = new YoutubeFlvToMp3Converter();

    /**
     * get the only existing instance of YoutubeFlvToMp3Converter. This is a singleton
     * 
     * @return
     */
    public static YoutubeFlvToMp3Converter getInstance() {
        return YoutubeFlvToMp3Converter.INSTANCE;
    }

    /**
     * Create a new instance of YoutubeFlvToMp3Converter. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private YoutubeFlvToMp3Converter() {

    }

    @Override
    public void run(DownloadLink downloadLink) {

        YoutubeHelper.convertToMp3(downloadLink);

    }

}