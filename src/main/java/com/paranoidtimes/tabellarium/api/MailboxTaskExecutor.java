package com.paranoidtimes.tabellarium.api;

import javax.mail.Message;
import java.util.List;

/**
 * The executor holds tasks that are executed on one (or even more) mailboxes.
 *
 * @author djosifovic
 */
public interface MailboxTaskExecutor {

    /**
     * Retrieves all e-mail from the specified location (mailbox, folder etc).
     *
     * @return a list of retrieved e-mails.
     * @throws MailBoxTaskExecutorException if executor can't retrieve e-mails.
     */
    List<Message> retrieveEmails() throws MailBoxTaskExecutorException;

    /**
     * Returns true if there is more e-mails in the specified location otherwise
     * false.
     *
     * @return true if there is more e-mail in the specified location, otherwise
     * false.
     * @throws MailBoxTaskExecutorException if executor can't execute the task
     *                                      do to an error.
     */
    boolean areThereRemainingEmails() throws MailBoxTaskExecutorException;

    /**
     * Invokes the passed handler on each e-mail in the specified location.
     *
     * @param emailHandler the handler for each e-mail.
     * @throws MailBoxTaskExecutorException if executor can't execute due to an
     *                                      error.
     */
    void executeForEachEmail(EmailHandler emailHandler) throws MailBoxTaskExecutorException;

    /**
     * This flag should indicate should seen e-mails also be retrieved.
     *
     * @param retrieveSeenEmails boolean to set.
     */
    void setRetrieveSeenEmails(boolean retrieveSeenEmails);

    /**
     * This flag should indicate if e-mail should bed deleted after processing.
     *
     * @param deleteAfterRetrieval boolean to set.
     */
    void setDeleteAfterRetrieval(boolean deleteAfterRetrieval);

    /**
     * Sets the current batch size. Batch size is the maximum number of emails
     * to process in one task. Batch size value rules can be enforced (like must
     * be greater than zero or similar).
     *
     * @param batchSize batch size to set.
     */
    void setBatchSize(int batchSize);
}
