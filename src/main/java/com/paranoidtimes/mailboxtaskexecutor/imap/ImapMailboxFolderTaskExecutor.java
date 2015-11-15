package com.paranoidtimes.mailboxtaskexecutor.imap;

import com.paranoidtimes.mailboxtaskexecutor.api.EmailHandler;
import com.paranoidtimes.mailboxtaskexecutor.api.MailBoxTaskExecutorException;
import com.paranoidtimes.mailboxtaskexecutor.api.MailboxTaskExecutor;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

/**
 * A Mailbox Task Executor implementation for IMAP protocol mailboxes. This
 * executor concentrates on a specific folder in the given mailbox which means
 * that all tasks are executed in the specified folder and have no knowledge of
 * other folders in the mailbox.
 *
 * @author djosifovic
 */
public class ImapMailboxFolderTaskExecutor implements MailboxTaskExecutor {

    /**
     * Mailbox host address.
     */
    private final String imapHostAddress;

    /**
     * Mailbox username.
     */
    private final String username;

    /**
     * Mailbox password.
     */
    private final String password;

    /**
     * Mailbox folder name.
     */
    private final String folderName;

    /**
     * Batch size for retrieving e-mails.
     */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * Flag marks if e-mails should be marked for deletion after retrieval.
     */
    private boolean deleteAfterRetrieval = false;

    /**
     * Flag marks if e-mails that are marked as seen should also be retrieved.
     */
    private boolean retrieveSeenEmails = false;

    /**
     * This is the javax.mail.Folder expunge field. If is set to true all
     * e-mails with added "DELETE" flag will be deleted upon folder closing and
     * thus from the mailbox.
     */
    private boolean expunge = true;

    /**
     * The connection timeout for connecting to the mailbox.
     */
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /**
     * A default connection timeout - infinite timeout.
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = -1;

    /**
     * A default batch size - all e-mails.
     */
    private static final int DEFAULT_BATCH_SIZE = 0;

    /**
     * A default
     * <pre>ImapMailboxFolderTaskExecutor</pre> constructor.
     *
     * @param imapHostAddress the host address of the mailbox.
     * @param username the username for the mailbox.
     * @param password the password for the mailbox.
     * @param folderName the folder name in which all tasks will be executed.
     */
    public ImapMailboxFolderTaskExecutor(String imapHostAddress, String username, String password, String folderName) {
        this.imapHostAddress = imapHostAddress;
        this.username = username;
        this.password = password;
        this.folderName = folderName;
    }

    /**
     * {@inheritDoc}
     * 
     * Retrieves all e-mails or
     * <pre>batchSize</pre> of e-mails from the specified mailbox folder.
     *
     * Will retrieve seen e-mail if
     * <pre>retrieveSeenEmails</pre> is set to true.
     *
     * All messages are copied to the returning list so no connection must be
     * maintained in order to use them.
     *
     * This is designed as a batch task, so for retrieving all e-mails using a
     * positive batch size the task should be called multiple times.
     *
     * @return a list of e-mails from the folder.
     * @throws MailBoxTaskExecutorException if the task can't execute properly.
     */
    @Override
    public List<Message> retrieveEmails() throws MailBoxTaskExecutorException {
        try {
            return doImapTask((Folder folder) -> {
                List<Message> retreivedEmails = new LinkedList<>();
                Flags seenFlag = new Flags(Flag.SEEN);
                FlagTerm flagTerm = new FlagTerm(seenFlag, retrieveSeenEmails);
                Message[] messages = folder.search(flagTerm);
                int retrieveCount = getRetrieveCount(messages.length);

                for (int i = 0; i < retrieveCount; i++) {
                    if (messages[i].isSet(Flag.DELETED) || (messages[i].isSet(Flag.SEEN) && !retrieveSeenEmails)) {
                        continue;
                    }
                    retreivedEmails.add(new MimeMessage((MimeMessage) folder.getMessage(messages[i].getMessageNumber())));
                    if (deleteAfterRetrieval) {
                        messages[i].setFlag(Flags.Flag.DELETED, true);
                    }
                }
                return retreivedEmails;
            });
        } catch (Exception ex) {
            throw new MailBoxTaskExecutorException("Error while retrieving e-mail from the mailbox folder.", ex);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Returns true if there are more e-mails in the folder, otherwise false.
     *
     * If
     * <pre>retrieveSeenEmails</pre> is set to true this task will count those
     * mails to.
     *
     * @return true if there are more e-mails in the folder, otherwise false.
     * @throws MailBoxTaskExecutorException if the task can't execute properly.
     */
    @Override
    public boolean isThereRemainingEmails() throws MailBoxTaskExecutorException {
        try {
            return doImapTask((final Folder folder) -> {
                Flags seenFlag = new Flags(Flag.SEEN);
                FlagTerm flagTerm = new FlagTerm(seenFlag, retrieveSeenEmails);
                Message[] messages;
                messages = folder.search(flagTerm);
                return messages.length > 0;
            });
        } catch (Exception ex) {
            throw new MailBoxTaskExecutorException("Error while getting remaining e-mails number.", ex);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Executes a passed
     * <pre>EmailHandler</pre> method on each e-mail in folder.
     *
     * Task is invoked on each e-mail in the mailbox. If some Exception happens
     * during the execution of tasks, all flags that are set during the
     * processing are reverted. All exceptions that might've happened while
     * processing e-mails ( exceptions for each e-mail) are collected and will
     * be thrown after the processing. To get the collection use
     * <pre>MailBoxTaskExecutorException
     * .getProcessingExceptions</pre>.
     *
     * @param emailHandler handler for each e-mail.
     * @throws MailBoxTaskExecutorException if some error happened during
     * execution.
     */
    @Override
    public void executeForEachEmail(final EmailHandler emailHandler) throws MailBoxTaskExecutorException {
        final LinkedList<Throwable> processingExceptions = new LinkedList<>();
        try {
            doImapTask((Folder folder) -> {
                Flags seenFlag = new Flags(Flag.SEEN);
                FlagTerm flagTerm = new FlagTerm(seenFlag, retrieveSeenEmails);
                Message[] messages = folder.search(flagTerm);
                int retrieveCount = getRetrieveCount(messages.length);

                for (int i = 0; i < retrieveCount; i++) {
                    if (messages[i].isSet(Flag.DELETED) || (messages[i].isSet(Flag.SEEN) && !retrieveSeenEmails)) {
                        continue;
                    }
                    try {
                        emailHandler.handleEmail(folder.getMessage(messages[i].getMessageNumber()));
                        if (deleteAfterRetrieval) {
                            messages[i].setFlag(Flags.Flag.DELETED, true);
                        }
                    } catch (Throwable e) {
                        if (!retrieveSeenEmails && messages[i].getFlags().contains(Flag.SEEN)) {
                            messages[i].setFlag(Flags.Flag.SEEN, false);
                        }
                        if (messages[i].getFlags().contains(Flag.DELETED)) {
                            messages[i].setFlag(Flags.Flag.DELETED, false);
                        }
                        processingExceptions.add(e);
                    }
                }
                return null;
            });
        } catch (Exception ex) {
            expunge = false;
            throw new MailBoxTaskExecutorException("Error while executing task in folder.", ex, processingExceptions.isEmpty() ? null : processingExceptions);
        }
        if (!processingExceptions.isEmpty()) {
            throw new MailBoxTaskExecutorException("Error(s) happened while handling e-mails.", processingExceptions);
        }
    }

    /**
     * Returns the number of messages to retrieve depending on the set
     * <pre>batchSize</pre> and the current number of messages in the folder.
     *
     * If batch size is set to 0, the number of messages in the folder will be
     * returned. If
     * <pre>batchSize</pre> is greater than 0 and number of messages is greater
     * than
     * <pre>batchSize</pre> number will be returned. If
     * <pre>batchSize</pre> is greater then zero and number of messages is
     * smaller than
     * <pre>batchSize</pre>, the number of messages in folder will be returned.
     *
     * @param numberOfMessages number of messages in the folder.
     * @return the number of messages that should be retrieved.
     */
    private int getRetrieveCount(int numberOfMessages) {
        int retrieveCount = batchSize == 0 ? numberOfMessages
                : numberOfMessages < batchSize ? numberOfMessages : batchSize;
        return retrieveCount;
    }

    /**
     * The common skeleton for all tasks. Handles the connection to the IMAP
     * mailbox.
     *
     * @param <T> return type of the <pre>ImapFolderTask</pre>.
     *
     * @param imapTask task to execute.
     * @return the result of task execution.
     * @throws Exception if the connection can't be established successfully or
     * the task fails.
     */
    private <T> T doImapTask(final ImapFolderTask<T> imapTask) throws Exception {
        Folder folder = null;
        Store store = null;

        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", "imaps");
        properties.setProperty("mail.imap.connectiontimeout", String.valueOf(connectionTimeout));
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.imap.socketFactory.fallback", "false");

        try {
            Session session = Session.getDefaultInstance(properties, null);
            store = session.getStore();
            store.connect(imapHostAddress, username, password);

            folder = store.getFolder(folderName);
            if (!folder.exists()) {
                throw new IllegalArgumentException("The specified folder doesn't exist.");
            }
            folder.open(Folder.READ_WRITE);

            return imapTask.doTaskInFolder(folder);
        } catch (Exception e) {
            throw new MailBoxTaskExecutorException("Error while retreiving e-mails.", e);
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(expunge);
            }
            if (store != null) {
                store.close();
            }
        }
    }

    /**
     * Returns the current connection timeout.
     *
     * @return connection timeout.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout for IMAP connection.
     *
     * @param connectionTimeout timeout to set.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Returns the current batch size.
     *
     * @return the current batch size.
     */
    public final int getBatchSize() {
        return batchSize;
    }

    /**
     * Returns the IMAP host address.
     *
     * @return IMAP host address.
     */
    public final String getImapHostAddress() {
        return imapHostAddress;
    }

    /**
     * Returns the mailbox username.
     *
     * @return the username.
     */
    public final String getUsername() {
        return username;
    }

    /**
     * Returns the mailbox password.
     *
     * @return
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Returns the folder name in which tasks are executed.
     *
     * @return the folder name.
     */
    public final String getFolderName() {
        return folderName;
    }

    /**
     * Returns true if e-mails are deleted after retrieval, otherwise false.
     *
     * @return true if e-mails are deleted after retrieval, otherwise false.
     */
    public final boolean isDeleteAfterRetrieval() {
        return deleteAfterRetrieval;
    }

    /**
     * Returns true if seen e-mails are retrieved, otherwise false.
     *
     * @return true if seen e-mails are retrieved, otherwise false.
     */
    public final boolean isRetrieveSeenEmails() {
        return retrieveSeenEmails;
    }

    /**
     * Sets the current batch size. Batch size must be 0 or greater.
     *
     * @param batchSize batch size to set.
     * @throws IllegalArgumentException if batch size is less then 0.
     */
    public final void setBatchSize(int batchSize) {
        if (batchSize < 0) {
            throw new IllegalArgumentException("Batch size must be eather zero or possitive!");
        }
        this.batchSize = batchSize;
    }

    /**
     * {@inheritDoc}
     * 
     * Sets the
     * <pre>deleteAfterRetrieval</pre> flag.
     *
     * @param deleteAfterRetrieval boolean to set.
     */
    @Override
    public final void setDeleteAfterRetrieval(boolean deleteAfterRetrieval) {
        this.deleteAfterRetrieval = deleteAfterRetrieval;
    }

    /**
     * {@inheritDoc}
     * 
     * Sets the
     * <pre>retrieveSeenEmails</pre> flag.
     *
     * @param retrieveSeenEmails boolean to set.
     */
    @Override
    public final void setRetrieveSeenEmails(boolean retrieveSeenEmails) {
        this.retrieveSeenEmails = retrieveSeenEmails;
    }
}
