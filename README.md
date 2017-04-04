# Tabellarium #

Version: 0.2-SNAPSHOT.

Tabellarium executes task against an e-mail mailbox.

Current API supports three methods which are implemented for IMAP
protocol in ```ImapMailboxFolderTaskExecutor```. This implementation is IMAP
folder based, so each task is done only in the specified folder.

Java SDK 1.8 is required.

# Usage #

To create ```ImapMailboxFolderTaskExecutor```:

```java
MailboxTaskExecutor executor = new ImapMailboxFolderTaskExecutor("hostAddress",
                                                                 "username",
                                                                 "password",
                                                                 "folderName");
executor.setDeleteAfterRetrieval(false);
executor.setRetrieveSeenEmails(true);
```
Upon creating the instance ```retrieveSeenEmails``` and ```deleteAfterRetrieval```
can be set. They indicate if seen e-mails should be taken into account for all
tasks and should processed e-mails be marked for deletion when processing is
finished, respectively. Both flags are ```false``` by default.

First method in the API is ```retrieveEmails``` that returns all e-mails from
the specified folder. Returned e-mails are ```javax.mail.Message``` instances
and are "copied" from the mailbox. This enables the executor to close the
connection to the mailbox upon retrieval. Also messages can be referenced
without maintaining the connection open. If ```retrieveSeenEmails``` is ```true```
seen e-mails will also be retrieved. If ```deleteAfterRetrieval``` is ```true```
than all processed e-mails will be marked for deletion.

```java
List<Message> message = executor.retrieveEmails();

// do something with Message list...
```

---

Second method returns true if there are more e-mails in the folder. If 
```retrieveSeenEmails``` is ```true```, those e-mails will be counted also.
```deleteAfterRetrieval``` flag is ignored.

```java
boolean result = executor.areThereRemainingEmails();
```

---

Third method is ```executeForEachEmail```. This method provides a extensible
handling mechanism for each e-mail in the folder. It executes the specified
handler task on each e-mail. If ```retrieveSeenEmails``` is ```true```, seen
e-mails will be also processed. If ```deleteAfterRetrieval``` is ```true``` than
all processed e-mails will be marked for deletion.

```java
mailboxTaskExecutor.executeForEachEmail(message -> {
    // do something with the e-mail message...
});
```

This is an example ```EmailHandler``` implementation that prints out the passed
e-mail to the given ```PrintStream```.

```java
executor.executeForEachEmail(new PrintingEmailHandler(System.out));
```

Example ```EmailHandler``` is actually an instance of ```UnlinkedEmailHandler```
which copies the retrieved message before executing the handling code. This
means that messages will be retrieved fully (its contents, headers, flags etc)
before handling. There is no 'connection' with IMAP server when using
```UnlinkedEmailHandler``` because handling is done on message copies so tasks
like modifying flags or deleting messages will not be possible.

# License #

Distributed under GNU LESSER GENERAL PUBLIC LICENSE Version 3.

Copyright © Dejan Josifovic, theparanoidtimes 2015.
