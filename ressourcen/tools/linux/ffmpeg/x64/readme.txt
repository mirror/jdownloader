
            ______ ______                                  
           / ____// ____/____ ___   ____   ___   ____ _
          / /_   / /_   / __ `__ \ / __ \ / _ \ / __ `/
         / __/  / __/  / / / / / // /_/ //  __// /_/ /
        /_/    /_/    /_/ /_/ /_// .___/ \___/ \__, /
                                /_/           /____/


                build: ffmpeg-git-20200211-amd64-static.tar.xz
              version: f15007afa90a3eb3639848d9702c1cc3ac3e896b

                  gcc: 8.3.0
                 yasm: 1.3.0.36.ge2569
                 nasm: 2.14.02

               libaom: 1.0.0-errata1-avif-274-g9b224eab7
               libass: 0.14.0
               libgme: 0.6.2
               libsrt: 1.4.1
               libvpx: 1.8.2-78-g6ca77eb7c
              libvmaf: 1.3.14
              libx264: 0.159.2991 
              libx265: 3.2+39-b11042b384af
              libxvid: 1.3.5-1
              libwebp: 0.6.1 
              libzimg: 2.8.0
              libzvbi: 0.2.36
             libdav1d: 0.6.0
            libgnutls: 3.6.10
            libtheora: 1.2.0alpha1+git
            libfrei0r: 1.6.1-2
           libvidstab: 1.10
          libfreetype: 2.9.1-3+deb10u1
          libharfbuzz: 2.6.4
          libopenjpeg: 2.3.1 

              libalsa: 1.2.1.2
              libsoxr: 0.1.3
              libopus: 1.3.1
             libspeex: 1.2
            libvorbis: 1.3.6
           libmp3lame: 3.100 
        librubberband: 1.8.2
       libvo-amrwbenc: 0.1.3-1+b1
    libopencore-amrnb: 0.1.3-2.1+b2
    libopencore-amrwb: 0.1.3-2.1+b2


     Notes:  A limitation of statically linking glibc is the loss of DNS resolution. Installing
             nscd through your package manager will fix this.

             The vmaf filter needs external files to work- see model/000-README.TXT


      This static build is licensed under the GNU General Public License version 3.

      
      Patreon: https://www.patreon.com/johnvansickle
      Paypal:  https://www.paypal.me/johnvansickle 
      Bitcoin: 13pZjChR1gR6wqzGMuwLAzqeVR5o9XGoCP 

      email: john.vansickle@gmail.com
      irc:   relaxed @ irc://chat.freenode.net #ffmpeg
      url:   https://johnvansickle.com/ffmpeg/
