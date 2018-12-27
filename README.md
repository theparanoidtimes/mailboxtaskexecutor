# Tabellarium #

[![Build Status](https://travis-ci.org/theparanoidtimes/tabellarium.svg?branch=master)](https://travis-ci.org/theparanoidtimes/tabellarium)

Version: 0.2.

*tabellarium* executes tasks against e-mail messages in one mailbox folder.

Current API supports three methods which are implemented for IMAP
protocol in `ImapMailboxFolderTaskExecutor`. Each task is done only in
the specified folder.

# Usage #

Java 8 or above is required.

To create ```ImapMailboxFolderTaskExecutor``` do:

```java
MailboxTaskExecutor executor = new ImapMailboxFolderTaskExecutor("hostAddress",
                                                                 "username",
                                                                 "password",
                                                                 "Inbox");
executor.setDeleteAfterRetrieval(false);
executor.setRetrieveSeenEmails(true);
```
Upon creating the instance `retrieveSeenEmails` and `deleteAfterRetrieval`
can be set. They indicate if seen e-mails should be taken into account for all
tasks and should processed e-mails be marked for deletion when processing is
finished, respectively. Both flags are `false` by default.

First method in the API is `retrieveEmails` that returns all e-mails from
the specified folder. Returned e-mails are `javax.mail.Message` instances
and are "copied" from the mailbox. This enables the executor to close the
connection to the mailbox upon retrieval. Also messages can be referenced
without maintaining the connection open. If `retrieveSeenEmails` is `true`
seen e-mails will also be retrieved. If `deleteAfterRetrieval` is `true`
than all processed e-mails will be marked for deletion.

```java
List<Message> message = executor.retrieveEmails();

// do something with messages...
```

---

Second method returns true if there are more e-mails in the folder. If 
`retrieveSeenEmails` is `true`, those e-mails will be counted also.
`deleteAfterRetrieval` flag is ignored.

```java
boolean result = executor.areThereRemainingEmails();
```

---

Third method is `executeForEachEmail`. This method provides a extensible
handling mechanism for each e-mail in the folder. It executes the specified
handler task on each e-mail. If `retrieveSeenEmails` is `true`, seen
e-mails will be also processed. If `deleteAfterRetrieval` is `true` then
all processed e-mails will be marked for deletion.

```java
mailboxTaskExecutor.executeForEachEmail(message -> {
    // do something with the e-mail message...
});
```

This is an example `EmailHandler` implementation that prints out the passed
e-mail to the given `PrintStream`.

```java
executor.executeForEachEmail(new PrintingEmailHandler(System.out));
```

Example `EmailHandler` is actually an instance of `UnlinkedEmailHandler`
which copies the retrieved message before executing the handling code. This
means that messages will be retrieved fully (its contents, headers, flags etc)
before handling. There is no 'connection' with IMAP server when using
`UnlinkedEmailHandler` because handling is done on message copies so tasks
like modifying flags or deleting messages will not be possible.

Another example of `UnlinkedEmailHandler` usage is writting the e-mails to a
file. *tabellarium* also provides a handler for that:
```java
executor.executeForEachEmail(new PrintInFileEmailHandler("file name"));
```

On the other hand there are cases when it is desirable to leave the
connection opet, if some state needs to be modified on the server, marking
all e-mails as seen, for an example. This is done by suppling an `EmailHandler`
instance to the executor. There is a built-in handler for status flags
modifications in *tabellarium*:
```java
executor.executeForEachEmail(new ChangeMessageFlagEmailHandler("seen", true));
```

# License #

MIT License

Copyright (c) 2015 Dejan Josifović, the paranoid times

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
