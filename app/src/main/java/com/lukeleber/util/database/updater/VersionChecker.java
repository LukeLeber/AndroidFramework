package com.lukeleber.util.database.updater;

import java.io.File;
import java.net.HttpURLConnection;

/**
 * An interface for classes that act to distinguish whether or not an update is available
 * given a local file and a remote file.  There are a few generic implementations provided
 * as nested static classes that may be used as defaults.
 *
 * Specifically, the
 * {@link com.lukeleber.util.database.updater.VersionChecker.TimestampVersionChecker}
 * class is used as a default if no
 * {@link com.lukeleber.util.database.updater.VersionChecker} is provided to the
 * {@link com.lukeleber.util.database.updater.DatabaseUpdater} constructor.
 */
public interface VersionChecker
{
    /**
     * Determines whether or not a newer version of the provided local file is to be found
     * at the provided remote {@link java.net.HttpURLConnection} endpoint.
     *
     * @param localVersion the local version of the file
     *
     * @param remoteVersion the remote version of the file
     *
     * @return true if the remote version is newer than the local version
     *
     */
    boolean isUpdateAvailable(File localVersion, HttpURLConnection remoteVersion);

    /**
     * An implementation of the {@link com.lukeleber.util.database.updater.VersionChecker}
     * interface that relies upon the last modified times to determine whether or not
     * an update is available.
     *
     */
    static class TimestampVersionChecker
            implements VersionChecker
    {
        /**
         * {@inheritDoc}
         *
         * <p>Specifically, this method returns true if the local installation was last
         * modified prior to the last modified http response field of the remote file.</p>
         *
         */
        @Override
        public boolean isUpdateAvailable(File localVersion, HttpURLConnection remoteVersion)
        {
            return remoteVersion.getLastModified() > localVersion.lastModified();
        }
    }
}
