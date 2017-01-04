package com.paranoidtimes.mailboxtaskexecutor.api;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

/**
 * A <pre>{@link EmailHandler}</pre> implementation that copies retrieved
 * messages before handling them.
 *
 * @author djosifovic
 */
public abstract class UnlinkedEmailHandler implements EmailHandler {

    /**
     * {@inheritDoc}
     * Creates a copy of each mail retrieved so it can be handled without a
     * session.
     *
     * @param message javax.mail.Message to handle.
     * @throws Exception if message handling failed.
     */
    @Override
    public void handleEmail(Message message) throws Exception {
        doHandleEmail(new MimeMessage((MimeMessage) message));
    }

    /**
     * Handles a single mail.
     *
     * @param message a message to handle.
     * @throws Exception if message handling failed.
     */
    protected abstract void doHandleEmail(Message message) throws Exception;
}
