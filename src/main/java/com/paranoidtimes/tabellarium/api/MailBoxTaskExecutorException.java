package com.paranoidtimes.tabellarium.api;

/**
 * A generic Tabellarium exception.
 *
 * @author djosifovic
 */
public class MailBoxTaskExecutorException extends Exception {

    /**
     * A default constructor with message.
     *
     * @param message a message describing the exception.
     */
    public MailBoxTaskExecutorException(String message) {
        super(message);
    }

    /**
     * A constructor with message and a cause exception.
     *
     * @param message a message describing the exception.
     * @param cause   a cause exception.
     */
    public MailBoxTaskExecutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
