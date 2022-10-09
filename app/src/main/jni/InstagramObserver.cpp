//
// Created by emlyn on 10/6/22.
//

#include <stdio.h>
#include <sys/inotify.h>
#include <cstdio>
#include <unistd.h>
#include <string>

#define EVENT_SIZE  ( sizeof (struct inotify_event) )
#define EVENT_BUF_LEN     ( 1024 * ( EVENT_SIZE + 16 ) )

void log(char* msg) {
    //TODO: Do this logfile in a way that it uses the strings.xml file (somehow)
    //TODO: Include timestamp
    FILE* lf = fopen("/data/data/xyz.emlyn.indestructible/log", "a");
    fwrite(msg, sizeof(char), strlen(msg), lf);
    fclose(lf);
}

int main() {

    int length, i = 0;

    int fd;

    int killProc;
    int instagram;

    char buffer[EVENT_BUF_LEN];

    fd = inotify_init();
    if (fd < 0) { perror("inotify_init"); }

    killProc = inotify_add_watch(fd, "/data/data/xyz.emlyn.indestructible/", IN_CREATE);
    instagram = inotify_add_watch(fd, "/data/data/com.instagram.android/databases/", IN_MODIFY);

    //TODO: Do this logfile in a way that it uses the strings.xml file (somehow)
    //TODO: Include timestamp
    log("C++ Observer started\n");

    while (1) {

        i = 0;

        length = read(fd, buffer, EVENT_BUF_LEN); //blocking call
        if (length < 0) { perror("read"); }

        try {
            while (i < length) {

                struct inotify_event *event = ( struct inotify_event * ) &buffer[ i ];
                if ( event->len ) {

                    if (event->mask & IN_CREATE) {


                        if (!std::strcmp(event->name, "kill_sig")) {  // kill sig
                            remove("/data/data/xyz.emlyn.indestructible/kill_sig");

                            //TODO: Do this logfile in a way that it uses the strings.xml file (somehow)
                            //TODO: Include timestamp
                            log("C++ Observer killed\n");


                            inotify_rm_watch(fd, killProc);
                            inotify_rm_watch(fd, instagram);

                            close(fd);

                            return 0;

                        }

                    }

                    if (event->mask & IN_MODIFY) {
                        // file modified
                        // for now just create a file in .Indestructible/ for logging purposes
                        // todo: probably copy instagram db to a file in .Indestructible/databases and handle it there?
                        // then backup / restore, create warning

                        // ooh maybe do some temporary copying to account for race conditions? like direct.bak.001.db, direct.bak.002.db, using largest number already exists in dir + 1

                        log("IG DB Modified\n");
                    }

                }

                i += EVENT_SIZE + event->len;

            }

        }
    } catch() {}  // if error, run standard cleanup routine

    inotify_rm_watch(fd, killProc);
    inotify_rm_watch(fd, instagram);

    close(fd);
    //TODO: Potentially create some crash file
    log("C++ Observer closed unexpectedly!");



    return 0;

}
