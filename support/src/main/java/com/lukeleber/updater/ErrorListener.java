// This file is protected under the KILLGPL.
// For more information, visit http://www.lukeleber.github.io/KILLGPL.html
//
// Copyright (c) Luke Leber <LukeLeber@gmail.com>

package com.lukeleber.updater;

/**
 * A listener that is invoked when an error is encountered in the background task.  Please
 * note that the {@link java.lang.Exception} provided to
 * {@link #onError(com.lukeleber.updater.ErrorListener.ErrorCode, Exception)}
 * may be null depending on whether or not an exception was available at the time of the error.
 *
 */
public interface ErrorListener
{
    /**
     * An enumeration of all possible error conditions that might occur in the background task
     *
     */
    public static enum ErrorCode
    {
        /// The background task was cancelled by the user
        USER_CANCELLED,

        /// The background task was unable to parse the provided remote database URL
        INPUT_MALFORMED_REMOTE_DATABASE_URL,

        /// The background task was unable to write to internal storage
        INTERNAL_STORAGE_WRITE_ERROR,

        /// The background task was unable to write to external storage
        EXTERNAL_STORAGE_WRITE_ERROR,

        /// The background task was unable to confirm that external storage exists
        EXTERNAL_STORAGE_NOT_MOUNTED,

        /// The background task was unable to save the remote database due to insufficient
        /// storage space
        DOWNLOAD_INSUFFICIENT_SPACE,

        /// The background task was unable to save the remote database due to a file with
        /// the same path already existing
        DOWNLOAD_FILE_ALREADY_EXISTS,

        /// The background task was unable to save the remote database due to a previously
        /// interrupted download that could not be resumed
        DOWNLOAD_CANNOT_RESUME,

        /// The background task was unable to save the remote database due to the inability
        /// to locate an external storage medium
        DOWNLOAD_DEVICE_NOT_FOUND,

        /// The background task was unable to save the remote database due to encountering
        /// too many HTTP redirects when accessing the remote URL
        DOWNLOAD_TOO_MANY_REDIRECTS,

        /// The background task was unable to save the remote database due to a generic error
        /// with local external storage that does not fall under any other categories
        DOWNLOAD_FILE_ERROR,

        /// The background task was unable to save the remote database due to corrupted http
        /// data
        DOWNLOAD_HTTP_DATA_ERROR,

        /// The background task was unable to save the remote database due to an unrecognized
        /// and/or unhandled HTTP response code
        DOWNLOAD_UNHANDLED_HTTP_CODE,

        /// The background task was unable to save the remote database due to an unknown
        /// download-related error
        DOWNLOAD_ERROR_UNKNOWN,

        /// The background task was unable to save the remote database due to a HTTP status
        /// code defined by RFC 2616 - note: To retrieve this status code, refer below.
        /// <pre>
        ///     void onError(ErrorListener.ErrorCode code, Exception cause)
        ///     {
        ///         int rfc2616_response =
        ///             ((ErrorListener.ErrorCode.RFC2616Exception)cause).getStatusCode();
        ///     }
        /// </pre>
        DOWNLOAD_RFC_2616_STATUS_CODE,

        /// The background task was unable to resolve the provided remote database URL
        DOWNLOAD_REMOTE_DATABASE_NOT_FOUND,

        /// The background task encountered an unknown error
        UNKNOWN_ERROR,

        /// No internet connection is available
        NO_INTERNET_CONNECTION;

        /**
         * A special type of exception for capturing a RFC2616-defined http response code
         *
         */
        public final static class RFC2616Exception extends Exception
        {
            /// The RFC 2616 specified status code of this error condition
            private final int statusCode;

            /**
             * Constructs a RFC2616Exception with the provided status code
             *
             * @param statusCode the RFC 2616 specified status code of this error condition
             *
             */
                /*package*/ RFC2616Exception(int statusCode)
            {
                this.statusCode = statusCode;
            }

            /**
             * Retrieves the RFC 2616 specified status code of this error condition
             *
             * @return the RFC 2616 specified status code of this error condition
             */
            @SuppressWarnings("unused")
            public int getStatusCode()
            {
                return statusCode;
            }
        }
    }

    /**
     * Invoked when the background task encounters an error
     *
     * @param code the {@link ErrorListener.ErrorCode}
     *             that best describes the error
     *
     * @param cause the {@link java.lang.Exception} that caused the error (if applicable)
     *
     */
    void onError(ErrorCode code, Exception cause);
}
