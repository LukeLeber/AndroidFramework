// This file is protected under the KILLGPL.
// For more information, visit http://www.lukeleber.github.io/KILLGPL.html
//
// Copyright (c) Luke Leber <LukeLeber@gmail.com>

package com.lukeleber.network;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.util.Log;

import com.lukeleber.BuildConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>Anything and everything related to network connectivity is stored in here.
 * Currently facilities exist for:
 * <ul>
 *     <li>Checking whether or not an internet connection is available</li>
 * </ul></p>
 * <p><strong>Many methods have preconditions that are explicitly specified in the enclosed
 * documentation.  Be sure to read and understand the documentation prior to using the
 * methods.</strong></p>
 * <br\>
 * <br\>
 * <p>Maintainer, please provide exhaustive documentation for the changes that are made
 * to this library.  Please do not utilize nontrivial objects (such as thread pools) at (static)
 * class scope in order to minimize the cost of using this class.  Only provide facilities that
 * have no quirky side-effects, unreasonable preconditions, or device/version dependent
 * behavior.  Please provide debugging information where appropriate and only wrapped within
 * debug conditionals so that no code is injected when compiled in a release build.  Please
 * also update the list found within the class documentation to incorporate a description of
 * any new additions.  Your compliance helps to ensure the quality and integrity of this
 * library for the sake of future developers.  Thank you and godspeed.</p>
 *
 */
@SuppressWarnings("unused")
public final class Connectivity
{
    /// @internal debugging tag
    private final static String TAG = Connectivity.class.getName();

    /// The default URL for tests that connect to the internet
    public final static String DEFAULT_INTERNET_TEST_URL = "http://www.google.com";

    /// The default time-out for tests that connect to the internet
    public final static int DEFAULT_INTERNET_TIMEOUT_MILLIS = 1000;

    /// Uninstantiable
    private Connectivity()
    {

    }

    /**
     * <p>Attempts to check whether or not the device has an active internet connection.</p>
     *
     * <p><strong>This method may block the calling thread.  Attempting to call this method
     * from a user interface thread may result in an {@link java.lang.IllegalStateException}
     * being thrown by the android framework depending on whether or not this method has
     * to utilize blocking network operations to determine connectivity status.</strong></p>
     *
     * <p>The following provides insight into the behavior of this method:
     *     <ol>
     *         <li>Verify that the provided arguments do not violate the following preconditions:
     *             <ul>
     *                 <li><i>context</i> shall not be null - if <i>context</i> is null, then
     *                 this method shall throw an instance of
     *                 {@link java.lang.IllegalArgumentException}</li>
     *                 <li><i>testURL</i> shall not be null, and shall point to a valid address -
     *                 if <i>testURL</i> is null, then this method shall throw an instance of
     *                 {@link java.lang.IllegalArgumentException}.  At this point, this method
     *                 shall assume that <i>testURL</i> is a valid, parseable URL.</li>
     *                 <li><i>timeoutMillis</i> shall not be negative - if <i>timeoutMillis</i>
     *                 is negative, then this method shall throw an instance of
     *                 {@link java.lang.IllegalArgumentException}</li>
     *             </ul>
     *             Only if the above preconditions are met shall this method continue execution.
     *         </li>
     *         <li>Check if the provided calling context has the following permissions:
     *             <ul>
     *             <li>{@link Manifest.permission#ACCESS_NETWORK_STATE}</li>
     *             <li>{@link Manifest.permission#INTERNET}</li>
     *             </ul>
     *             If the calling context does have the above permissions, this method shall
     *             return false.
     *         </li>
     *         <li>Check if this android device is capable of providing the
     *         {@link Context#CONNECTIVITY_SERVICE} system service.  If this device does not
     *         provide the {@link Context#CONNECTIVITY_SERVICE} system service, then this method
     *         shall return false.
     *         </li>
     *         <li>Check whether or not a network connection is available.  If no network
     *         connection is available, then this method shall return false.</li>
     *         <li>Attempt to parse <i>testURL</i> - if <i>testURL</i> is not a valid URL
     *         then this method shall throw an exception of type
     *         {@link java.lang.IllegalArgumentException}</li>
     *         <li>Attempt to reach <i>testURL</i> by sending a HTTP request with a time-out
     *         of <i>timeoutMillis</i>.  If the connection is unsuccessful, or if the HTTP
     *         response is not {@link Constants.HTTP#HTTP_RESPONSE_OK}, then this method shall
     *         return false, otherwise this method shall return true.
     *         </li>
     *     </ol>
     * </p>
     *
     * @param context the calling {@link android.content.Context} to use in this method
     *
     * @param testURL the remote URL to use in this method
     *
     * @param timeoutMillis the timeout valid (in milliseconds) to use in this method
     *
     * @return True if the calling context can connect to the internet, otherwise false.
     *
     * @throws IllegalArgumentException if:
     * <ul>
     *     <li><i>context</i> is null</li>
     *     <li><i>testURL</i> is null</li>
     *     <li><i>timeoutMillis</i> is negative</li>
     *     <li><i>testURL</i> is not a valid URL</li>
     * </ul>
     *
     * @throws java.lang.IllegalStateException if this method is called on a UI thread and
     * performs blocking network operations.  This exception will not be thrown if
     * this method does not perform blocking network operations (IE. no network connections
     * exist, causing this method to return early).  <strong>Even in such cases, calling this
     * method from a UI thread is still an error.</strong>
     *
     */
    public static boolean isConnectedToInternet(final Context context, final String testURL,
                                                final int timeoutMillis)
    {
        /// Debugging check only - provides a slightly stricter policy than the android runtime,
        /// as this check catches 100% of the erroneous calls to this method whereas the built-in
        /// routine will only catch calls that actually make it to the blocking HTTP connection.
        /// As this check is omitted from release builds, we can only hope that the sources were
        /// at least compiled and ran in debug mode prior to shipping.
        if(BuildConfig.DEBUG && Looper.myLooper().equals(Looper.getMainLooper()))
        {
            Log.e(TAG, "blocking method called on UI thread");
        }
        if(context == null)
        {
            if(BuildConfig.DEBUG)
            {
                Log.w(TAG, "Null value for 'context'");
            }
            throw new IllegalArgumentException("context == null");
        }
        if(testURL == null)
        {
            if(BuildConfig.DEBUG)
            {
                Log.w(TAG, "Null value for 'testURL'");
            }
            throw new IllegalArgumentException("testURL == null");
        }
        if(timeoutMillis < 0)
        {
            if(BuildConfig.DEBUG)
            {
                Log.w(TAG, "Illegal value for 'timeoutMillis': " + timeoutMillis);
            }
            throw new IllegalArgumentException("timeoutMillis < 0");
        }
        final String[] requiredPermissions = new String[]
        {
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET
        };
        final PackageManager pm = context.getPackageManager();
        final String packageName = context.getPackageName();
        for(final String permission : requiredPermissions)
        {
            if(PackageManager.PERMISSION_GRANTED != pm.checkPermission(permission, packageName))
            {
                Log.w(TAG, "Unable to check internet connectivity: Permission " +
                        permission + " is not granted.");
                return false;
            }
        }
        final ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm == null)
        {
            return false;
        }
        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected())
        {
            HttpURLConnection connection = null;
            try
            {
                connection = (HttpURLConnection)new URL(testURL).openConnection();
                    connection.setConnectTimeout(1);
                connection.connect();
                return connection.getResponseCode() == Constants.HTTP.HTTP_RESPONSE_OK;
            }
            catch(final MalformedURLException murle)
            {
                if(BuildConfig.DEBUG)
                {
                    Log.w(TAG, "testURL is an invalid URL", murle);
                }
                throw (IllegalArgumentException)
                        new IllegalArgumentException("testURL is not a valid URL")
                                .initCause(murle);
            }
            catch (final IOException e)
            {
                /// todo: research why this might happen
                /// Maintainer,
                /// Is it better to return false (assume the internet is unreachable)
                /// or to throw an exception (we don't know if the internet is reachable)?
                if(BuildConfig.DEBUG)
                {
                    Log.w(TAG, "Unknown error", e);
                }
            }
            finally
            {
                if(connection != null)
                {
                    connection.disconnect();
                }
            }
        }
        return false;
    }

    /**
     * This convenience method is equivalent to the following invocation:
     * <pre>isConnectedToInternet(context, testURL, DEFAULT_INTERNET_TIMEOUT_MILLIS);</pre>
     * Refer to {@link Connectivity#isConnectedToInternet(android.content.Context, String, int)}
     * for exhaustive documentation.
     *
     * @see Connectivity#isConnectedToInternet(android.content.Context, String, int)
     *
     * @param context the calling {@link android.content.Context} to use in this method
     *
     * @param testURL the remote URL to use in this method
     *
     * @return True if the calling context can connect to the internet, otherwise false.
     *
     */
    public static boolean isConnectedToInternet(Context context, String testURL)
    {
        return isConnectedToInternet(context, testURL, DEFAULT_INTERNET_TIMEOUT_MILLIS);
    }

    /**
     * This convenience method is equivalent to the following invocation:
     * <pre>isConnectedToInternet(context, DEFAULT_INTERNET_TEST_URL, timeoutMillis);</pre>
     * Refer to {@link Connectivity#isConnectedToInternet(android.content.Context, String, int)}
     * for exhaustive documentation.
     *
     * @see Connectivity#isConnectedToInternet(android.content.Context, String, int)
     *
     * @param context the calling {@link android.content.Context} to use in this method
     *
     * @param timeoutMillis the timeout valid (in milliseconds) to use in this method
     *
     * @return True if the calling context can connect to the internet, otherwise false.
     *
     */
    public static boolean isConnectedToInternet(Context context, int timeoutMillis)
    {
        return isConnectedToInternet(context, DEFAULT_INTERNET_TEST_URL, timeoutMillis);
    }

    /**
     * This convenience method is equivalent to the following invocation:
     * <pre>isConnectedToInternet(context, DEFAULT_INTERNET_TEST_URL, DEFAULT_INTERNET_TIMEOUT_MILLIS);
     * </pre>
     * Refer to {@link Connectivity#isConnectedToInternet(android.content.Context, String, int)}
     * for exhaustive documentation.
     *
     * @see Connectivity#isConnectedToInternet(android.content.Context, String, int)
     *
     * @param context the calling {@link android.content.Context} to use in this method
     *
     * @return True if the calling context can connect to the internet, otherwise false.
     *
     */
    public static boolean isConnectedToInternet(Context context)
    {
        return isConnectedToInternet(context, DEFAULT_INTERNET_TEST_URL, DEFAULT_INTERNET_TIMEOUT_MILLIS);
    }
}
