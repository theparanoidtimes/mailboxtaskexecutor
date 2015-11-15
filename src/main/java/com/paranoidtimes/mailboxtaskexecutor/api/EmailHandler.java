package com.paranoidtimes.mailboxtaskexecutor.api;

import javax.mail.Message;

/**
 * The handler for a single e-mail.
 *
 * @author djosifovic
 */
public interface EmailHandler {

    /**
     * Handles a single e-mail message.
     *
     * @param message javax.mail.Message to handle.
     * @throws Exception if an error occurs.
     */
    void handleEmail(Message message) throws Exception;
}
