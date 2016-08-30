package com.paranoidtimes.mailboxtaskexecutor.imap;

import javax.mail.Folder;

/**
 * A task that needs to be executed against an IMAP mailbox folder.
 *
 * @param <T> the return type of the task.
 * @author djosifovic
 */
interface ImapFolderTask<T> {

    /**
     * A task to be executed.
     *
     * @param folder IMAP mailbox folder where the task will be executed.
     * @return the result of the task execution of the type T.
     * @throws java.lang.Exception if the task fails.
     */
    T doTaskInFolder(Folder folder) throws Exception;
}
