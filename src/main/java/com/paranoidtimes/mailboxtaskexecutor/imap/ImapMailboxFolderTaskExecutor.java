package com.paranoidtimes.mailboxtaskexecutor.imap;

import com.paranoidtimes.mailboxtaskexecutor.api.EmailHandler;
import com.paranoidtimes.mailboxtaskexecutor.api.MailBoxTaskExecutorException;
import com.paranoidtimes.mailboxtaskexecutor.api.MailboxTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * A Mailbox Task Executor implementation for IMAP(S) protocol mailboxes. This
 * executor concentrates on a specific folder in the given mailbox which means
 * that all tasks are executed in the specified folder and have no knowledge of
 * other folders in the mailbox.
 *
 * @author djosifovic
 */
public class ImapMailboxFolderTaskExecutor implements MailboxTaskExecutor {

    /**
     * Log instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImapMailboxFolderTaskExecutor.class);

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
     * Batch size for retrieving e-mails. More specifically the maximum number
     * of messages that will be retrieved in one run. Does not indicate the
     * number of messages that are pulled from mailbox in one time.
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
     * The port number on which to connect to. When null defaults to IMAP(S)
     * port number.
     */
    private Integer port = null;

    /**
     * A flag indicating if IMAP or IMAPS protocol should be used.
     * Default is true which means IMAPS.
     */
    private boolean secure = true;

    /**
     * A default connection timeout - infinite timeout.
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = -1;

    /**
     * A default batch size - all e-mails.
     */
    private static final int DEFAULT_BATCH_SIZE = 0;

    /**
     * Constructs a new <pre>{@link ImapMailboxFolderTaskExecutor}</pre> instance
     * with specified host address, username, password and folder
     * name.
     *
     * @param imapHostAddress the host address of the mailbox.
     * @param username        the username for the mailbox.
     * @param password        the password for the mailbox.
     * @param folderName      the folder name in which all tasks will be executed.
     */
    public ImapMailboxFolderTaskExecutor(String imapHostAddress, String username, String password, String folderName) {
        this.imapHostAddress = imapHostAddress;
        this.username = username;
        this.password = password;
        this.folderName = folderName;
    }

    /**
     * Constructs a new <pre>{@link ImapMailboxFolderTaskExecutor}</pre> instance
     * with specified host address, port number, username, password and folder
     * name.
     *
     * @param imapHostAddress the host address of the mailbox.
     * @param port            the port number to connect to.
     * @param username        the username for the mailbox.
     * @param password        the password for the mailbox.
     * @param folderName      the folder name in which all tasks will be executed.
     */
    public ImapMailboxFolderTaskExecutor(String imapHostAddress, Integer port, String username, String password, String folderName) {
        this(imapHostAddress, username, password, folderName);
        this.port = port;
    }

    /**
     * Constructs a new <pre>{@link ImapMailboxFolderTaskExecutor}</pre> instance
     * with specified host address, username, password, folder name and secure
     * flag.
     *
     * @param imapHostAddress the host address of the mailbox.
     * @param username        the username for the mailbox.
     * @param password        the password for the mailbox.
     * @param folderName      the folder name in which all tasks will be executed.
     * @param secure          the flag indicating if IMAP or IMAPS should be used.
     */
    public ImapMailboxFolderTaskExecutor(String imapHostAddress, String username, String password, String folderName, boolean secure) {
        this(imapHostAddress, username, password, folderName);
        this.secure = secure;
    }

    /**
     * Constructs a new <pre>{@link ImapMailboxFolderTaskExecutor}</pre> instance
     * with specified host address, port number, username, password, folder name
     * and secure flag.
     *
     * @param imapHostAddress the host address of the mailbox.
     * @param port            the port number to connect to.
     * @param username        the username for the mailbox.
     * @param password        the password for the mailbox.
     * @param folderName      the folder name in which all tasks will be executed.
     * @param secure          the flag indicating if IMAP or IMAPS should be used.
     */
    public ImapMailboxFolderTaskExecutor(String imapHostAddress, Integer port, String username, String password, String folderName, boolean secure) {
        this(imapHostAddress, username, password, folderName);
        this.port = port;
        this.secure = secure;
    }

    /**
     * {@inheritDoc}
     * Retrieves all e-mails or
     * <pre>batchSize</pre> of e-mails from the specified mailbox folder.
     *
     * Will retrieve seen e-mail if
     * <pre>retrieveSeenEmails</pre> is set to true.
     *
     * All messages are copied to the returning list so no connection must be
     * maintained in order to use them.
     *
     * This is designed as a 'batch' task, so for retrieving all e-mails using a
     * positive batch size the task should be called multiple times.
     *
     * @return a list of e-mails from the folder.
     * @throws MailBoxTaskExecutorException if the task can't execute properly.
     */
    @Override
    public List<Message> retrieveEmails() throws MailBoxTaskExecutorException {
        try {
            return doImapTask(new ImapFolderTask<List<Message>>() {
                @Override
                public List<Message> doTaskInFolder(Folder folder) throws Exception {
                    List<Message> retrievedEmails = new LinkedList<>();
                    Message[] messages = folder.search(getSearchTerm());
                    int retrieveCount = getRetrieveCount(messages.length);

                    for (int i = 0; i < retrieveCount; i++) {
                        if (messages[i].isSet(Flag.DELETED) || (!retrieveSeenEmails && messages[i].isSet(Flag.SEEN))) {
                            LOG.trace("Skipping message {} because it is marked as DELETED or SEEN and retrieveSeenEmails is false!", i);
                            continue;
                        }
                        retrievedEmails.add(copyOf(folder.getMessage(messages[i].getMessageNumber())));
                        if (deleteAfterRetrieval) {
                            LOG.trace("Marking message {} as DELETED.", i);
                            messages[i].setFlag(Flags.Flag.DELETED, true);
                        }
                    }
                    return retrievedEmails;
                }

                @Override
                public String getTaskName() {
                    return "RetrieveEmailsImapFolderTask";
                }
            });
        } catch (Exception ex) {
            throw new MailBoxTaskExecutorException("Error while retrieving e-mail from the mailbox folder.", ex);
        }
    }

    /**
     * {@inheritDoc}
     * Returns true if there are more e-mails in the folder, otherwise false.
     * If
     * <pre>retrieveSeenEmails</pre> is set to true this task will count those
     * mails to.
     *
     * @return true if there are more e-mails in the folder, otherwise false.
     * @throws MailBoxTaskExecutorException if the task can't execute properly.
     */
    @Override
    public boolean areThereRemainingEmails() throws MailBoxTaskExecutorException {
        try {
            return doImapTask(new ImapFolderTask<Boolean>() {
                @Override
                public Boolean doTaskInFolder(Folder folder) throws Exception {
                    return folder.search(getSearchTerm()).length > 0;
                }

                @Override
                public String getTaskName() {
                    return "AreThereRemainingEmailsImapFolderTask";
                }
            });
        } catch (Exception ex) {
            throw new MailBoxTaskExecutorException("Error while getting remaining e-mails number.", ex);
        }
    }

    /**
     * {@inheritDoc}
     * Executes a passed
     * <pre>EmailHandler</pre> method on each e-mail in folder.
     *
     * Task is invoked on each e-mail in the mailbox. If some Exception happens
     * during the execution of tasks, all flags that are set during the
     * processing are reverted.
     *
     * @param emailHandler handler for each e-mail.
     * @throws MailBoxTaskExecutorException if some error happened during
     *                                      execution.
     */
    @Override
    public void executeForEachEmail(final EmailHandler emailHandler) throws MailBoxTaskExecutorException {
        try {
            doImapTask(new ImapFolderTask<Void>() {
                @Override
                public Void doTaskInFolder(Folder folder) throws Exception {
                    Message[] messages = folder.search(getSearchTerm());
                    int retrieveCount = getRetrieveCount(messages.length);

                    for (int i = 0; i < retrieveCount; i++) {
                        Message message = messages[i];
                        if (message.isSet(Flag.DELETED) || (!retrieveSeenEmails && message.isSet(Flag.SEEN))) {
                            LOG.trace("Skipping message {} because it is marked as DELETED or SEEN and retrieveSeenEmails is false!", i);
                            continue;
                        }
                        try {
                            emailHandler.handleEmail(copyOf(folder.getMessage(message.getMessageNumber())));
                            if (deleteAfterRetrieval) {
                                LOG.trace("Marking message {} as DELETED.", i);
                                message.setFlag(Flags.Flag.DELETED, true);
                            }
                        } catch (Throwable e) {
                            LOG.error("Error happened while handling e-mail message {}!", i, e);
                            if (!retrieveSeenEmails && message.getFlags().contains(Flag.SEEN)) {
                                LOG.trace("Reverting SEEN flag for message {}...", i);
                                message.setFlag(Flags.Flag.SEEN, false);
                            }
                            if (message.getFlags().contains(Flag.DELETED)) {
                                LOG.trace("Reverting DELETED flag for message {}...", i);
                                message.setFlag(Flags.Flag.DELETED, false);
                            }
                        }
                    }
                    return null;
                }

                @Override
                public String getTaskName() {
                    return "ExecuteForEachEmailImapFolderTask";
                }
            });
        } catch (Exception ex) {
            expunge = false;
            throw new MailBoxTaskExecutorException("Error while executing task in folder.", ex);
        }
    }

    /**
     * Returns a <pre>{@link SearchTerm}</pre> to be used when retrieving messages.
     * Default term is only for unseen emails, but when retrieveSeenEmails is
     * set to true it returns a union of terms for both unseen and seen messages.
     *
     * @return a search term to be used for message retrieval.
     */
    private SearchTerm getSearchTerm() {
        Flags seenFlag = new Flags(Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seenFlag, false);
        if (retrieveSeenEmails) {
            FlagTerm seenFlagTerm = new FlagTerm(seenFlag, true);
            return new OrTerm(unseenFlagTerm, seenFlagTerm);
        }
        return unseenFlagTerm;
    }

    /**
     * Returns a copy of passed message as a instance of
     * <pre>{@link MimeMessage}</pre>.
     *
     * @param message a message to copy.
     * @return a copy of the message.
     * @throws MessagingException if passed message failed to copy.
     */
    private Message copyOf(Message message) throws MessagingException {
        return new MimeMessage((MimeMessage) message);
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
        return batchSize == 0 ? numberOfMessages
                : numberOfMessages < batchSize ? numberOfMessages : batchSize;
    }

    /**
     * The common skeleton for all tasks. Handles the connection to the IMAP
     * mailbox.
     *
     * @param <T>      return type of the <pre>ImapFolderTask</pre>.
     * @param imapTask task to execute.
     * @return the result of task execution.
     * @throws Exception if the connection can't be established successfully or
     *                   the task fails.
     */
    private <T> T doImapTask(final ImapFolderTask<T> imapTask) throws Exception {
        Folder folder = null;
        Store store = null;

        Properties properties = new Properties();
        properties.setProperty("mail.imap.connectiontimeout", String.valueOf(connectionTimeout));
        if (secure) {
            properties.setProperty("mail.store.protocol", "imaps");
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.imap.socketFactory.fallback", "false");
        } else
            properties.setProperty("mail.store.protocol", "imap");

        try {
            Session session = Session.getInstance(properties);
            store = session.getStore();
            if (port == null)
                store.connect(imapHostAddress, username, password);
            else
                store.connect(imapHostAddress, port, username, password);

            folder = store.getFolder(folderName);
            if (!folder.exists()) {
                throw new IllegalArgumentException("The specified folder doesn't exist!");
            }
            folder.open(Folder.READ_WRITE);

            LOG.trace("Starting {} with retrieveSeenEmails set to {} and deleteAfterRetrieval set to {}.", imapTask.getTaskName(), retrieveSeenEmails, deleteAfterRetrieval);
            long startTime = System.currentTimeMillis();
            T result = imapTask.doTaskInFolder(folder);
            long endTime = System.currentTimeMillis();

            LOG.info("Finished task {}, with result {} in {} seconds", imapTask.getTaskName(), result, (endTime - startTime) / 1000);
            return result;
        } catch (Exception e) {
            LOG.error("Error happened while executing task {}!", imapTask.getTaskName(), e);
            throw new MailBoxTaskExecutorException("Error while retrieving e-mails.", e);
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
     * Sets the connection timeout for IMAP connection. Timeout bust be positive
     * or it can be -1 which means infinite timeout (no timeout).
     *
     * @param connectionTimeout timeout to set.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout < -1)
            throw new IllegalArgumentException("Connection timeout must be greater then or equal to -1!");
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
     * @return the mailbox password.
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
     * Returns the port number.
     *
     * @return port number or <pre>null</pre> when port is not defined.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Returns the value of the secure flag.
     *
     * @return true if IMAPS is used, false when IMAP is used.
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * {@inheritDoc}
     * Zero size batch means all e-mails in mailbox.
     *
     * @throws IllegalArgumentException if batch size is less then 0.
     */
    @Override
    public final void setBatchSize(int batchSize) {
        if (batchSize < 0) {
            throw new IllegalArgumentException("Batch size must be either zero or positive!");
        }
        this.batchSize = batchSize;
    }

    /**
     * {@inheritDoc}
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
