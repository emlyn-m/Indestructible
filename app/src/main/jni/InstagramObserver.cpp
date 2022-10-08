//
// Created by emlyn on 10/6/22.
//

#include <stdio.h>
#include <sys/inotify.h>
#include <cstdio>
#include <unistd.h>

#define EVENT_SIZE  ( sizeof (struct inotify_event) )
#define EVENT_BUF_LEN     ( 1024 * ( EVENT_SIZE + 16 ) )


int main() {

    int length, i = 0;

    int fd;

    int killProc;
    int instagram;

    char buffer[EVENT_BUF_LEN];

    fd = inotify_init();
    if (fd < 0) { perror("inotify_init"); }

    killProc = inotify_add_watch(fd, "/data/data/xyz.emlyn.Indestructible/", IN_CREATE);
    instagram = inotify_add_watch(fd, "/data/data/com.instagram.android/databases/", IN_MODIFY);

    while (1) {

        i = 0;

        length = read(fd, buffer, EVENT_BUF_LEN); //blocking call
        if (length < 0) { perror("read"); }

        while (i < length) {

            struct inotify_event *event = ( struct inotify_event * ) &buffer[ i ];
            if ( event->len ) {

                if (event->mask & IN_CREATE) {

                    if (event->name == "kill_sig") {  // kill sig
                        remove("/data/data/xyz.emlyn.Indestructible/kk");

                        inotify_rm_watch(fd, killProc);
                        inotify_rm_watch(fd, instagram);

                        close(fd);

                    }

                    if (event->name == "restore_db") {
                        // todo: copy db from .Indestructible to com.instagram.android/databases
                    }
                }

                if (event->mask & IN_MODIFY) {
                    // file modified
                    // for now just create a file in .Indestructible/ for logging purposes
                    // todo: probably copy instagram db to a file in .Indestructible/databases and handle it there?
                    // then backup / restore, create warning

                    // ooh maybe do some temporary copying to account for race conditions? like direct.bak.001.db, direct.bak.002.db, using largest number already exists in dir + 1

                    FILE* pFile = fopen("/data/data/xyz.emlyn.Indestructible/instagram_modified", "w");
                    fclose(pFile); // open/close to create file
                }

            }

            i += EVENT_SIZE + event->len;

        }

    }

    inotify_rm_watch(fd, killProc);
    inotify_rm_watch(fd, instagram);

    close(fd);

    return 0;

}
