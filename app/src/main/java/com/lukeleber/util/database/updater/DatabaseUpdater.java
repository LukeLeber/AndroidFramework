// This file is protected under the KILLGPL.
// For more information, visit http://www.lukeleber.github.io/KILLGPL.html
//
// Copyright (c) Luke Leber <LukeLeber@gmail.com>

package com.lukeleber.util.database.updater;

import android.app.DownloadManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.lukeleber.util.BuildConfig;
import com.lukeleber.util.io.StreamCopy;
import com.lukeleber.util.network.Connectivity;
import com.lukeleber.util.network.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A utility class for providing a means by which to keep an up-to-date local copy of a
 * database hosted on an external server.
 * </p>
 * <p><strong>It is highly recommended that the user is asked to confirm whether or not they
 * wish to update, as this utility will semi-transparently connect to a remote server.  Typically
 * this is achieved through a simple yes/no {@link android.app.AlertDialog}</strong></p>
 * <p>
 *     <pre>
 *         public class SomeActivity
 *              extends Activity
 *         {
 *             private final static String REMOTE_DB = ...;
 *             private final static String LOCAL_DB = ...;
 *
 *             protected void onCreate(Bundle sis)
 *             {
 *                  new AlertDialog.Builder(this)
 *                   .setTitle("Check For Updates")
 *                   .setMessage("This app must connect to an external server in order to
 *                   check for updates.  Do you wish to allow this? (The app will still
 *                   function if you opt out of updating.)")
 *                   .setPositiveButton("Update",
 *                   new DialogInterface.OnClickListener()
 *                   {
 *                      public void onClick(DialogInterface dialog, int which)
 *                      {
 *                          new DatabaseUpdater(SomeActivity.this, REMOTE_DB, LOCAL_DB).execute();
 *                      }
 *                  }).create().show();
 *             }
 *         }
 *     </pre>
 * </p>
 *
 * <p>This utility has three (3) main steps:
 * <ol>
 *     <li>Determine if the local database needs to be updated</li>
 *     <li>Download the remote database (if an update is required)</li>
 *     <li>Copy the new database to internal storage (if an update has been downloaded)</li>
 * </ol>
 * </p>
 * <p>Example Usage:
 * <pre>
 *     class DemoDatabaseUpdater
 *          extends DatabaseUpdater
 *          implements ErrorListener,
 *                     CompletionListener
 *     {
 *         private final static String LOCAL_DB = "demo.sqlite";
 *         private final static String REMOTE_DB = "http://www.lukeleber.com/demo.sqlite";
 *
 *         public void onError(ErrorListener.ErrorCode code, Exception cause)
 *         {
 *             ...
 *         }
 *
 *         public void onUpdateCompleted(CompletionListener.CompletionStatus status)
 *         {
 *             ...
 *         }
 *
 *         public DemoDatabaseUpdater(Context context)
 *         {
 *             super(context, REMOTE_DB, LOCAL_DB);
 *             super.addErrorListener(this);
 *             super.addCompletionListener(this);
 *         }
 *     }
 * </pre>
 * </p>
 *
 */
public class DatabaseUpdater
        extends AsyncTask<Object, Float, Object>
{

    /// @internal Debugging tag
    private final static String TAG = DatabaseUpdater.class.getName();

    /// The buffer to use in transferring data from external to internal storage
    private final static int DEFAULT_TRANSFER_BUFFER_SIZE = 1024;

    /// The number of milliseconds to wait before issuing a connection time-out
    private final static int DEFAULT_TIMEOUT_MILLIS = 1000;

    /// The version checker to use for determining whether or not an update is available
    private final VersionChecker versionChecker;

    /// The list of registered error listeners
    private final List<ErrorListener> errorListeners = new ArrayList<>();

    /// The list of registered completion listeners
    private final List<CompletionListener> completionListeners = new ArrayList<>();

    /// The name of the local database to update
    private final String localDatabaseName;

    /// The name of the temporary rollback file
    private File rollback;

    /// The URL of the remote database
    private final String remoteDatabaseURL;

    /// The context that this utility is used under
    private final Context context;

    /**
     * An internal helper method for firing off all associated error listeners in the order
     * that they were added.
     *
     * @param code the {@link com.lukeleber.util.database.updater.ErrorListener.ErrorCode}
     *             that best describes the error
     *
     * @param cause the {@link java.lang.Exception} that caused the error (if applicable)
     *
     */
    private void onError(ErrorListener.ErrorCode code, Exception cause)
    {
        for(ErrorListener listener : errorListeners)
        {
            listener.onError(code, cause);
        }
        rollback();
    }

    /**
     * An internal helper method for firing off all associated completion listeners in the
     * order that they were added.
     *
     * @param status the {@link com.lukeleber.util.database.updater.CompletionListener.CompletionStatus}
     *               that best describes the condition under which the update completed
     *
     */
    private void onComplete(CompletionListener.CompletionStatus status)
    {
        for(CompletionListener listener : completionListeners)
        {
            listener.onUpdateCompleted(status);
        }
    }

    /**
     * An internal helper method that attempts to initialize the local database in internal
     * storage.  Basically this method simply creates, opens, and closes the local database
     * to ensure that the file exists prior to attempting to copy the remote database to it.
     *
     * @throws android.database.sqlite.SQLiteException if a SQL error occurs
     */
    private void initDatabase()
    {
        File localDatabase = context.getDatabasePath(localDatabaseName);
        if(!localDatabase.exists())
        {
            SQLiteDatabase db = null;
            try
            {
                db = new SQLiteOpenHelper(context, localDatabaseName, null, 1)
                {
                    @Override
                    public void onCreate(SQLiteDatabase db) {}

                    @Override
                    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
                }.getReadableDatabase();
            }
            catch (SQLiteException e)
            {
                onError(ErrorListener.ErrorCode.INTERNAL_STORAGE_WRITE_ERROR, e);
                throw e;
            }
            finally
            {
                if (db != null) {
                    db.close();
                }
            }
        }
        else
        {
            try
            {
                rollback = File.createTempFile("", null);
                try(FileInputStream is = new FileInputStream(localDatabase);
                    FileOutputStream os = new FileOutputStream(rollback))
                {
                    byte[] buffer = new byte[DEFAULT_TRANSFER_BUFFER_SIZE];
                    int length;
                    while((length = is.read(buffer)) > 0)
                    {
                        os.write(buffer, 0, length);
                    }
                }
            }
            catch(IOException ioe)
            {
                if(BuildConfig.DEBUG)
                {
                    Log.e(TAG, "Unable to create temporary rollback file", ioe);
                }
            }
        }
    }

    private void rollback()
    {
        File localDatabase = context.getDatabasePath(localDatabaseName);
        if(rollback != null && localDatabase.exists())
        {
            try(FileInputStream is = new FileInputStream(rollback);
                FileOutputStream os = new FileOutputStream(localDatabase))
            {
                StreamCopy.copyInputStream(is, os, DEFAULT_TRANSFER_BUFFER_SIZE);
            }
            catch(IOException ioe)
            {
                if(BuildConfig.DEBUG)
                {
                    Log.e(TAG, "Unable to delete local database", ioe);
                }
            }
        }
        else if(localDatabase.exists())
        {
            if(!localDatabase.delete() && BuildConfig.DEBUG)
            {
                Log.e(TAG, "Unable to delete local database");
            }
        }
    }

    /**
     * Retrieves the {@link android.content.Context} that this background task is running under
     *
     * @return the {@link android.content.Context} that this background task is running under
     *
     */
    @SuppressWarnings("unused")
    protected final Context getContext()
    {
        return context;
    }

    /**
     * Adds an {@link com.lukeleber.util.database.updater.ErrorListener} to this
     * {@link com.lukeleber.util.database.updater.DatabaseUpdater}.  Listeners shall be invoked in the exact
     * order by which they were added.
     *
     * @param listener the {@link com.lukeleber.util.database.updater.ErrorListener} to add
     *
     * @return this
     *
     */
    @SuppressWarnings("unused")
    public final DatabaseUpdater addErrorListener(ErrorListener listener)
    {
        this.errorListeners.add(listener);
        return this;
    }

    /**
     * Adds an {@link com.lukeleber.util.database.updater.CompletionListener} to this
     * {@link com.lukeleber.util.database.updater.DatabaseUpdater}.  Listeners shall be invoked in the exact
     * order by which they were added.
     *
     * @param listener the {@link com.lukeleber.util.database.updater.CompletionListener} to add
     *
     * @return this
     *
     */
    @SuppressWarnings("unused")
    public final DatabaseUpdater addCompletionListener(CompletionListener listener)
    {
        this.completionListeners.add(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * Invokes {@link DatabaseUpdater#onError(ErrorListener.ErrorCode, Exception)}
     * with {@link ErrorListener.ErrorCode#USER_CANCELLED} and a null exception
     *
     */
    @Override
    protected final void onCancelled()
    {
        onError(ErrorListener.ErrorCode.USER_CANCELLED, null);
    }

    /// Short & Sweet - Take the input stream from the remote file and transfer all bytes
    /// to the output stream into internal storage or die trying.
    private void download(HttpURLConnection remoteFile)
        throws IOException
    {
        try(InputStream is = remoteFile.getInputStream();
            OutputStream os = new FileOutputStream(context.getDatabasePath(localDatabaseName)))
        {
            StreamCopy.copyInputStream(is, os, DEFAULT_TRANSFER_BUFFER_SIZE);
        }
    }


    /**
     * {@inheritDoc}
     *
     * <p>Essentially this background task does three things (or dies trying):
     * <ol>
     *  <li>Check whether an update is available or not</li>
     *  <li>Download an update to external storage if available</li>
     *  <li>Write the new database to internal storage</li>
     * </ol>
     * </p>
     *
     * @return always returns null
     */
    @Override
    protected final Object doInBackground(Object[] params)
    {
        if(!Connectivity.isConnectedToInternet(context))
        {
            onError(ErrorListener.ErrorCode.NO_INTERNET_CONNECTION, null);
            return null;
        }

        HttpURLConnection remoteDatabaseConnection = null;
        try
        {
            remoteDatabaseConnection =
                    (HttpURLConnection) new URL(remoteDatabaseURL).openConnection();
            remoteDatabaseConnection.setConnectTimeout(DEFAULT_TIMEOUT_MILLIS);
            remoteDatabaseConnection.setReadTimeout(DEFAULT_TIMEOUT_MILLIS);
            remoteDatabaseConnection.connect();
            if(remoteDatabaseConnection.getResponseCode() != Constants.HTTP.HTTP_RESPONSE_OK)
            {
                throw new IOException("Unable to connect to remote database");
            }
            File localDatabase = context.getDatabasePath(localDatabaseName);
            if(!localDatabase.exists() || versionChecker.isUpdateAvailable(localDatabase, remoteDatabaseConnection))
            {
                initDatabase();
                try
                {
                    download(remoteDatabaseConnection);
                }
                catch(IOException ioe)
                {
                    rollback();
                }
            }
            else
            {
                onComplete(CompletionListener.CompletionStatus.ALREADY_UP_TO_DATE);
            }
        }
        catch(MalformedURLException murle)
        {
            onError(ErrorListener.ErrorCode.INPUT_MALFORMED_REMOTE_DATABASE_URL, murle);
            return null;
        }
        catch(IOException ioe)
        {
            onError(ErrorListener.ErrorCode.DOWNLOAD_REMOTE_DATABASE_NOT_FOUND, ioe);
            return null;
        }
        finally
        {
            if(remoteDatabaseConnection != null)
            {
                remoteDatabaseConnection.disconnect();
            }
        }
        return null;
    }

    /**
     * Constructs a {@link com.lukeleber.util.database.updater.DatabaseUpdater} with the provided host
     * {@link android.content.Context},
     * {@link com.lukeleber.util.database.updater.VersionChecker}, remote database url, and
     * local database name
     *
     * @param context The context that this utility is used under
     *
     * @param versionChecker The version checker to use for determining whether or not an update
     *                       is available
     *
     * @param remoteDatabaseURL The URL of the remote database
     *
     * @param localDatabaseName The name of the local database to update
     *
     * @throws IllegalStateException if either permission
     * {@link android.Manifest.permission#INTERNET} or
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} is not granted
     *
     */
    public DatabaseUpdater(Context context, VersionChecker versionChecker,
                           String remoteDatabaseURL, String localDatabaseName)
    {
        this.context = context;
        this.versionChecker = versionChecker;
        this.localDatabaseName = localDatabaseName;
        this.remoteDatabaseURL = remoteDatabaseURL;
    }

    /**
     * Constructs a {@link com.lukeleber.util.database.updater.DatabaseUpdater} with the provided host
     * {@link android.content.Context}, remote database url, and local database name utilizing
     * the default {@link com.lukeleber.util.database.updater.VersionChecker} implementation:
     * {@link com.lukeleber.util.database.updater.VersionChecker.TimestampVersionChecker}.
     *
     * @param context The context that this utility is used under
     *
     * @param remoteDatabaseURL The URL of the remote database
     *
     * @param localDatabaseName The name of the local database to update
     *
     */
    @SuppressWarnings("unused")
    public DatabaseUpdater(Context context, String remoteDatabaseURL, String localDatabaseName)
    {
        this(context, new VersionChecker.TimestampVersionChecker(),
                remoteDatabaseURL, localDatabaseName);
    }
}
