# Mailbox Task Executor #

Version: 0.1.

Mailbox Task Executor executes task against an e-mail mailbox.

Currently API supports only three methods which are implemented only for IMAP
protocol in ```ImapMailboxFolderTaskExecutor```. This implementation is IMAP
folder based, so each task is done only in the specified folder.

Java SDK 1.8 is required.

# Usage #

To create ```ImapMailboxFolderTaskExecutor```:

```java
MailboxTaskExecutor executor = new ImapMailboxFolderTaskExecutor("hostAddress", "username", "password", "folderName");
executor.setDeleteAfterRetrieval(false);
executor.setRetrieveSeenEmails(true);
```
Upon creating the instance ```retrieveSeenEmails``` and ```deleteAfterRetrieval```
should be set. They indicate if seen e-mails should be taken into account for all
tasks and should processed e-mails be marked for deletion when processing is finished,
respectively. Both flags are ```false``` by default.

First method in the API is ```retrieveEmails``` that returns all e-mails from
the specified folder. Returned e-mails are ```javax.mail.Message``` instances
and are "copied" from the mailbox. This enables the executor to close the
connection to the mailbox upon retrieval. Also messages can be referenced without
maintaining the connection open. If ```retrieveSeenEmails``` is ```true``` seen e-mails
will also be retrieved.

```java
List<Message> message = executor.retrieveEmails();
```

Second method returns true if there are more e-mails in the folder. If 
```retrieveSeenEmails``` is ```true```, those e-mails will be counted also.

```java
boolean result = executor.isThereRemainingEmails();
```

This methods provides a extendible handling mechanism for each e-mail in the
folder. It executes the specified handler task on each e-mail. If 
```retrieveSeenEmails``` is ```true```, seen e-mails will be also processed. If
```deleteAfterRetrieval``` is ```true``` than all processed e-mails will be marked
for deletion.

All exceptions that might occur while processing e-mails will be collected to a 
```List``` and when the task is finished will be apart of thrown
```MailBoxTaskExecutorException```. Also, all modified flags on the e-mail which
processing caused an error will be reverted to the previous state.

```java
executor.executeForEachEmail(new EmailHandler() {
    @Override public void handleEmail(Message message) throws Exception {
        // do something with e-mail...
    }
});

// To reference processing exceptions:
...
(catch MailBoxTaskExecutorException e) {
    List<Throwable> errors = e.getProcessingExceptions();
}
```

This is an example ```EmailHandler``` implementation that prints out the passed
e-mail to the given ```PrintStream```.

```java
executor.executeForEachEmail(new PrintingEmailHandler(System.out));
```

# License #

Distributed under GNU LESSER GENERAL PUBLIC LICENSE Version 3.

Copyright © Dejan Josifovic, theparanoidtimes 2015.
