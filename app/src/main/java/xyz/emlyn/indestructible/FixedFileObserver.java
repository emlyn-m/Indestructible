package xyz.emlyn.indestructible;


import android.os.FileObserver;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// Modified version of FileObserver capable of handling multiple observers on the same file similtaneously
// Credit to @Fimagena on StackOverflow
// https://stackoverflow.com/a/32791762

// Slightly modified to construct with File instead of String for compatability reasons with existing code

public abstract class FixedFileObserver {

    private final static HashMap<File, Set<FixedFileObserver>> sObserverLists = new HashMap<>();

    private FileObserver mObserver;
    private final File mRootPath;
    private final int mMask;

    public FixedFileObserver(File f) {this(f, FileObserver.ALL_EVENTS);}
    public FixedFileObserver(File f, int mask) {
        mRootPath = new File(f.getAbsolutePath());
        mMask = mask;
    }

    public abstract void onEvent(int event, String path);

    public void startWatching() {
        synchronized (sObserverLists) {
            if (!sObserverLists.containsKey(mRootPath)) sObserverLists.put(mRootPath, new HashSet<FixedFileObserver>());

            final Set<FixedFileObserver> fixedObservers = sObserverLists.get(mRootPath);

            mObserver = fixedObservers.size() > 0 ? fixedObservers.iterator().next().mObserver : new FileObserver(mRootPath.getPath()) {
                @Override public void onEvent(int event, String path) {
                    for (FixedFileObserver fixedObserver : fixedObservers)
                        if ((event & fixedObserver.mMask) != 0) fixedObserver.onEvent(event, path);
                }};
            mObserver.startWatching();
            fixedObservers.add(this);
        }
    }

    public void stopWatching() {
        synchronized (sObserverLists) {
            Set<FixedFileObserver> fixedObservers = sObserverLists.get(mRootPath);
            if ((fixedObservers == null) || (mObserver == null)) return;

            fixedObservers.remove(this);
            if (fixedObservers.size() == 0) mObserver.stopWatching();

            mObserver = null;
        }
    }

    protected void finalize() {stopWatching();}
}
