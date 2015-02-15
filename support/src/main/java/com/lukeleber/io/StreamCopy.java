// This file is protected under the KILLGPL.
// For more information, visit http://www.lukeleber.github.io/KILLGPL.html
//
// Copyright (c) Luke Leber <LukeLeber@gmail.com>

package com.lukeleber.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple utility methods to take an input stream and copy it to *somewhere* using either
 * default or customized transfer buffers are in this class.  Currently facilities exist to:
 * <ul>
 *     <li>Copy an {@link java.io.InputStream} to an {@link java.io.OutputStream}</li>
 * </ul>
 *
 */
public class StreamCopy
{

    /// The default size of transfer buffers (1kB)
    private final static int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * <p>Copies the provided contents of the provided {@link java.io.InputStream} to the provided
     * {@link java.io.OutputStream} using the provided buffer between <i>offset</i> and
     * <i>offset + length</i></p>
     *
     * <p><strong>Consider using this method if multiple streams need to be copied within the
     * same scope.  If only a single stream is to be copied, consider using
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream, int)} or
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream)} instead.
     * </strong></p>
     *
     * @param is the {@link java.io.InputStream} to read from
     *
     * @param os the {@link java.io.OutputStream} to write to
     *
     * @param transferBuffer the buffer to use during the transfer
     *
     * @param offset the distance from the beginning of the buffer to copy to
     *
     * @param length the maximum number of bytes to copy at a time
     *
     * @throws IOException if any I/O error occurs with either stream
     *
     * @throws NullPointerException if <i>is</i>, <i>os</i>, or <i>transferBuffer</i> are null
     *
     * @throws IndexOutOfBoundsException if <i>offset</i> < 0, <i>length</i> < 0, or if
     * <i>offset</i> + <i>length</i> > <i>transferBuffer.length</i>
     *
     */
    public static void copyInputStream(final InputStream is, final OutputStream os,
                                       final byte[] transferBuffer, int offset, int length)
            throws IOException
    {
        int length0;
        while((length0 = is.read(transferBuffer, offset, length)) > 0)
        {
            os.write(transferBuffer, 0, length0);
        }
    }

    /**
     * <p>Copies the provided contents of the provided {@link java.io.InputStream} to the provided
     * {@link java.io.OutputStream} using the provided buffer.</p>
     *
     * <p><strong>Consider using this method if multiple streams need to be copied within the
     * same scope.  If only a single stream is to be copied, consider using
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream, int)} or
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream)} instead.
     * </strong></p>
     *
     * @param is the {@link java.io.InputStream} to read from
     *
     * @param os the {@link java.io.OutputStream} to write to
     *
     * @param transferBuffer the buffer to use during the transfer
     *
     * @throws IOException if any I/O error occurs with either stream
     *
     * @throws NullPointerException if <i>is</i>, <i>os</i>, or <i>transferBuffer</i> are null
     *
     */
    public static void copyInputStream(final InputStream is, final OutputStream os, final byte[] transferBuffer)
            throws IOException
    {
        int length;
        while((length = is.read(transferBuffer)) > 0)
        {
            os.write(transferBuffer, 0, length);
        }
    }

    /**
     * <p>Copies the provided contents of the provided {@link java.io.InputStream} to the provided
     * {@link java.io.OutputStream} using the provided buffer using a locally created buffer of
     * the provided length.</p>
     *
     * <p><strong>Consider using this method if only a single input stream needs
     * to be copied.  Otherwise, consider reuse a buffer that lives in a broader scope using
     * {@link StreamCopy#copyInputStream(java.io.InputStream,
     * java.io.OutputStream, byte[], int, int)} or
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream, byte[])}.  It
     * should also be noted that a power-of-two value for <i>bufferSize</i> is generally
     * recommended.
     * </strong></p>
     *
     * @param is the {@link java.io.InputStream} to read from
     *
     * @param os the {@link java.io.OutputStream} to write to
     *
     * @param bufferSize the size of the transfer buffer
     *
     * @throws IOException if any I/O error occurs with either stream
     *
     * @throws NullPointerException if <i>is</i> or <i>os</i> are null
     *
     */
    public static void copyInputStream(final InputStream is, final OutputStream os, int bufferSize)
            throws IOException
    {
        byte[] transferBuffer = new byte[bufferSize];
        copyInputStream(is, os, transferBuffer);
    }

    /**
     * <p>Copies the provided contents of the provided {@link java.io.InputStream} to the provided
     * {@link java.io.OutputStream} using the provided buffer using a locally created buffer of
     * length {@link StreamCopy#DEFAULT_BUFFER_SIZE}.</p>
     *
     * <p><strong>Consider using this method if only a single input stream needs
     * to be copied.  Otherwise, consider reuse a buffer that lives in a broader scope using
     * {@link StreamCopy#copyInputStream(java.io.InputStream,
     * java.io.OutputStream, byte[], int, int)} or
     * {@link StreamCopy#copyInputStream(java.io.InputStream, java.io.OutputStream, byte[])}.
     * </strong></p>
     *
     * @param is the {@link java.io.InputStream} to read from
     *
     * @param os the {@link java.io.OutputStream} to write to
     *
     * @throws IOException if any I/O error occurs with either stream
     *
     * @throws NullPointerException if <i>is</i> or <i>os</i> are null
     *
     */

    public static void copyInputStream(final InputStream is, final OutputStream os) throws IOException
    {
        byte[] transferBuffer = new byte[DEFAULT_BUFFER_SIZE];
        copyInputStream(is, os, transferBuffer);
    }
}
