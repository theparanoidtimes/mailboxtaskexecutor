package com.paranoidtimes.mailboxtaskexecutor.api;

import java.util.List;

/**
 * A generic Mailbox Task Executor exception.
 *
 * @author djosifovic
 */
public class MailBoxTaskExecutorException extends Exception {

    /**
     * A list of e-mail processing exceptions.
     */
    private List<Throwable> processingExceptions = null;

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

    /**
     * A constructor with a message and a list of e-mail processing exceptions.
     *
     * @param message              a message describing the exception.
     * @param processingExceptions a list of e-mail processing exceptions.
     */
    public MailBoxTaskExecutorException(String message, List<Throwable> processingExceptions) {
        super(message);
        this.processingExceptions = processingExceptions;
    }

    /**
     * A constructor with a message, cause exception and a list of e-mail
     * processing exceptions.
     *
     * @param message              a message describing the exception.
     * @param cause                a cause exception.
     * @param processingExceptions a list of e-mail processing exceptions.
     */
    public MailBoxTaskExecutorException(String message, Throwable cause, List<Throwable> processingExceptions) {
        this(message, cause);
        this.processingExceptions = processingExceptions;
    }

    /**
     * Returns the list of e-mail processing exceptions.
     *
     * @return the list of e-mail processing exceptions.
     */
    public List<Throwable> getProcessingExceptions() {
        return processingExceptions;
    }
}
