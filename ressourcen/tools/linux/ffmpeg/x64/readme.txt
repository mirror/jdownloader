
    ______ ______                                  __            _  __     __
   / ____// ____/____ ___   ____   ___   ____ _   / /_   __  __ (_)/ /____/ /
  / /_   / /_   / __ `__ \ / __ \ / _ \ / __ `/  / __ \ / / / // // // __  / 
 / __/  / __/  / / / / / // /_/ //  __// /_/ /  / /_/ // /_/ // // // /_/ /  
/_/    /_/    /_/ /_/ /_// .___/ \___/ \__, /  /_.___/ \__,_//_//_/ \__,_/   
                        /_/           /____/                                 


                build: ffmpeg-git-20181002-64bit-static.tar.xz
              version: gdcbd89e000
 
                  gcc: 6.3.0
                 yasm: 1.3.0.36.ge2569
                 nasm: 2.13.03

               libaom: 1.0.0-703-g51b282d13
               libass: 0.14.0
               libvpx: 1.7.0-1115-g2beb5c9f9
              libvmaf: 1.3.9
              libx264: 0.157.2935 545de2f
              libx265: 2.8+74-fd517ae68f93
              libxvid: 1.3.4-1+b2
              libwebp: 0.5.2 
              libzimg: 2.7.5
            libgnutls: 3.5.19
            libtheora: 1.2.0alpha1+git
            libfrei0r: 1.5.0-1
           libvidstab: 1.10
          libfreetype: 2.6.3-3.2
          libopenjpeg: 2.3.0 

              libalsa: 1.1.6
              libsoxr: 0.1.3
              libopus: 1.3-rc2
             libspeex: 1.2
            libvorbis: 1.3.6
           libmp3lame: 3.100 
        librubberband: 1.8.1 
       libvo-amrwbenc: 0.1.3-1
    libopencore-amrnb: 0.1.3-2.1+b2
    libopencore-amrwb: 0.1.3-2.1+b2

      For HEVC/H.265 encoding:  ffmpeg -h encoder=libx265
                                http://x265.readthedocs.org/en/default/cli.html#standalone-executable-options

      For AVC/H.264 encoding:   ffmpeg -h encoder=libx264
                                http://mewiki.project357.com/wiki/X264_Settings

                 FFmpeg Wiki:   https://trac.ffmpeg.org/wiki


      Notes: ffmpeg-10bit has support for AVC/H.264, HEVC/H.265, and VP9 high bit depth encoding.
             However, the 32bit builds lack HEVC/H.265 high bit depth because it's not supported by
             libx265.

             A limitation of statically linking glibc is the loss of DNS resolution. Installing
             nscd through your package manager will fix this or you can use
             "ffmpeg -i http://<ip address here>/" instead of "ffmpeg -i http://example.com/"

             The vmaf filter needs external files to work- see model/000-README.TXT

      This static build is licensed under the GNU General Public License version 3.

      If you appreciate this up-to-date build of FFmpeg and my time that goes into to maintaining it,
      please consider making a donation. Thank you.

      
      Patreon: https://www.patreon.com/johnvansickle
      Paypal tinyurl: http://goo.gl/1Ol8N
      Bitcoin: 1JfPDkULqcqzAnmHQfboa3rXTLVftJJ5qt 

      email: john.vansickle@gmail.com
      irc: relaxed @ irc://irc.freenode.net #ffmpeg
      url: https://johnvansickle.com/ffmpeg/
