package com.paranoidtimes.mailboxtaskexecutor.test;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.paranoidtimes.mailboxtaskexecutor.api.EmailHandler;
import com.paranoidtimes.mailboxtaskexecutor.api.MailboxTaskExecutor;
import com.paranoidtimes.mailboxtaskexecutor.handlers.ChangeMessageFlagEmailHandler;
import com.paranoidtimes.mailboxtaskexecutor.imap.ImapMailboxFolderTaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;

public class ImapMailboxFolderTaskExecutorTest {

    private static GreenMail greenMail;
    private static MailFolder inbox;

    @BeforeClass
    public static void setUp() throws Exception {
        ServerSetup serverSetup = new ServerSetup(30993, "localhost", "imap");
        greenMail = new GreenMail(serverSetup);
        GreenMailUser user = greenMail.setUser("user@localhost", "user@localhost", "password");
        inbox = greenMail.getManagers().getImapHostManager().getInbox(user);
        greenMail.start();
    }

    @After
    public void cleanUpMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        greenMail.stop();
    }

    @Test
    public void executorWillExecuteHandlerForEachEmail() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        getMailboxTaskExecutor().executeForEachEmail(message -> {});

        assertThat(inbox.getNonDeletedMessages().size(), equalTo(2));
    }

    @Test
    public void executorWillExecuteHandlerAndMarkAsDeletedIfDeleteAfterRetrievalIsTrueForEachEmail() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        MailboxTaskExecutor mailboxTaskExecutor = getMailboxTaskExecutor();
        mailboxTaskExecutor.setDeleteAfterRetrieval(true);
        mailboxTaskExecutor.executeForEachEmail(message -> {});

        assertThat(inbox.getMessageCount(), equalTo(0));
    }

    @Test
    public void executorWillRecoverSeenFlagsInCaseOfFailedHandling() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        getMailboxTaskExecutor().executeForEachEmail(new FaultyEmailHandler());

        assertThat(inbox.getUnseenCount(), equalTo(2));
    }

    @Test
    public void executorWillRecoverDeletedFlagsInCaseOfFailedHandling() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        MailboxTaskExecutor mailboxTaskExecutor = getMailboxTaskExecutor();
        mailboxTaskExecutor.setDeleteAfterRetrieval(true);
        mailboxTaskExecutor.executeForEachEmail(new FaultyEmailHandler());

        assertThat(inbox.getUnseenCount(), equalTo(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executorWillRetrieveAllUnseenEmails() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        List<Message> messages = getMailboxTaskExecutor().retrieveEmails();

        assertThat(messages.size(), equalTo(2));
        assertThat(messages, contains(
                allOf(
                        hasProperty("from", hasItemInArray(new InternetAddress("f1@localhost"))),
                        hasProperty("subject", equalTo("s1")),
                        hasProperty("content", equalTo("c1"))),
                allOf(
                        hasProperty("from", hasItemInArray(new InternetAddress("f2@localhost"))),
                        hasProperty("subject", equalTo("s2")),
                        hasProperty("content", equalTo("c2")))));
    }

    @Test
    public void executorWillRetrieveJustUnseenEmails() throws Exception {
        MimeMessage mimeMessage1 = mimeMessageWithFromSubjectAndContent("f1@localhost", "s1", "c1");
        MimeMessage mimeMessage2 = mimeMessageWithFromSubjectAndContent("f2@localhost", "s2", "c2");
        inbox.appendMessage(mimeMessage1, new Flags(), new Date());
        inbox.appendMessage(mimeMessage2, new Flags(Flags.Flag.SEEN), new Date());

        List<Message> messages = getMailboxTaskExecutor().retrieveEmails();

        assertThat(messages.size(), equalTo(1));
        assertThat(messages, contains(allOf(
                hasProperty("from", hasItemInArray(new InternetAddress("f1@localhost"))),
                hasProperty("subject", equalTo("s1")),
                hasProperty("content", equalTo("c1")))));
    }

    @Test
    public void executorWillReturnTrueForIsThereRemainingEmailsWhenThereAreUnseenEmailsInMailboxFolder() throws Exception {
        MimeMessage mimeMessage = mimeMessageWithFromSubjectAndContent("f@localhost", "s", "c");
        inbox.appendMessage(mimeMessage, new Flags(), new Date());

        boolean result = getMailboxTaskExecutor().areThereRemainingEmails();

        assertThat(result, equalTo(true));
    }

    @Test
    public void executorWillReturnFalseForIsThereRemainingEmailsWhenThereAreNoUnseenEmailsInMailboxFolder() throws Exception {
        boolean result = getMailboxTaskExecutor().areThereRemainingEmails();

        assertThat(result, equalTo(false));
    }

    @Test
    public void executorWillReturnFalseForIsThereRemainingEmailsWhenThereAreNoUnseenEmailsAndSomeSeenEmailsInMailboxFolder() throws Exception {
        MimeMessage mimeMessage = mimeMessageWithFromSubjectAndContent("f@localhost", "s", "c");
        Flags flags = new Flags(Flags.Flag.SEEN);
        inbox.appendMessage(mimeMessage, flags, new Date());

        boolean result = getMailboxTaskExecutor().areThereRemainingEmails();

        assertThat(result, equalTo(false));
    }

    // Handler tests

    @Test
    public void changeMessageFlagMailHandlerWillChangeAllEmailFlags() throws Exception {
        appendTwoUnseenMessagesToUserInbox();

        getMailboxTaskExecutor().executeForEachEmail(new ChangeMessageFlagEmailHandler("recent", true));

        assertThat(inbox.getRecentCount(false), equalTo(2));
    }

    // Utilities

    private void appendTwoUnseenMessagesToUserInbox() throws Exception {
        MimeMessage mimeMessage1 = mimeMessageWithFromSubjectAndContent("f1@localhost", "s1", "c1");
        MimeMessage mimeMessage2 = mimeMessageWithFromSubjectAndContent("f2@localhost", "s2", "c2");
        inbox.appendMessage(mimeMessage1, new Flags(), new Date());
        inbox.appendMessage(mimeMessage2, new Flags(), new Date());
    }

    private MailboxTaskExecutor getMailboxTaskExecutor() {
        return new ImapMailboxFolderTaskExecutor("localhost", 30993, "user@localhost", "password", "INBOX", false);
    }

    private MimeMessage mimeMessageWithFromSubjectAndContent(String from, String subject, String content) throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(greenMail.getImap().getServerSetup().configureJavaMailSessionProperties(null, false)));
        mimeMessage.setFrom(new InternetAddress(from));
        mimeMessage.setSender(new InternetAddress("user@localhost"));
        mimeMessage.setSubject(subject);
        mimeMessage.setText(content);
        return mimeMessage;
    }

    private static class FaultyEmailHandler implements EmailHandler {

        @Override
        public void handleEmail(Message message) throws Exception {
            throw new Exception(String.format("Message %s handling failed!", message.toString()));
        }
    }
}
