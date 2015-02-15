// This file is protected under the KILLGPL.
// For more information, visit http://www.lukeleber.github.io/KILLGPL.html
//
// Copyright (c) Luke Leber <LukeLeber@gmail.com>

package com.lukeleber.updater;

/**
 * A listener that is invoked when the background task successfully completes the update
 * procedure (or if an update was not required).
 *
 */
public interface CompletionListener
{
    /**
     * An enumeration of all possible conditions under which a completion may occur
     *
     */
    public static enum CompletionStatus
    {
        /// No update was required, as the local database was up to date
        ALREADY_UP_TO_DATE,

        /// The local database was successfully updated
        UPDATE_COMPLETED
    }

    /**
     * Invoked when the background task successfully completes the update procedure
     *
     * @param status the {@link CompletionListener.CompletionStatus}
     *               that best describes the condition under which the update completed
     *
     */
    void onUpdateCompleted(CompletionStatus status);
}
