package org.theparanoidtimes.tabellarium.handlers;

import org.theparanoidtimes.tabellarium.api.EmailHandler;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A <pre>{@link EmailHandler}</pre> implementation that overrides message
 * flags.
 *
 * @author djosifovic
 */
public class ChangeMessageFlagEmailHandler implements EmailHandler {

    /**
     * A collection with flags to filter messages with.
     */
    private final Collection<String> oldFlagSet = new HashSet<>();

    /**
     * The name of the flag that will override all other flags.
     */
    private final String newFlag;

    /**
     * The value of the new flag.
     */
    private final boolean newFlagValue;

    /**
     * Map containing name to <pre>{@link javax.mail.Flags.Flag}</pre> mappings
     * for already defined system flags.
     */
    private Map<String, Flags.Flag> systemFlagsMap = new HashMap<>();

    /**
     * Constructs a new instance that will override all message flags with given
     * new flag name and its value.
     * <p>
     * Also initializes systemFlagsMap.
     *
     * @param newFlag      the new flag to set.
     * @param newFlagValue the value of the new flag.
     */
    public ChangeMessageFlagEmailHandler(String newFlag, boolean newFlagValue) {
        this.newFlag = newFlag;
        this.newFlagValue = newFlagValue;

        this.systemFlagsMap.put("answered", Flags.Flag.ANSWERED);
        this.systemFlagsMap.put("deleted", Flags.Flag.DELETED);
        this.systemFlagsMap.put("draft", Flags.Flag.DRAFT);
        this.systemFlagsMap.put("flagged", Flags.Flag.FLAGGED);
        this.systemFlagsMap.put("recent", Flags.Flag.RECENT);
        this.systemFlagsMap.put("seen", Flags.Flag.SEEN);
        this.systemFlagsMap.put("user", Flags.Flag.USER);
    }

    /**
     * Constructs a new instance that will override filtered messages only with
     * given flag name and its value. It will filter messages based on
     * oldFlagSet collection - if a message has some of the flags contained in
     * this collection it will be affected, otherwise it will be unchanged.
     *
     * @param oldFlagSet   a collection with flag names to filter messages with.
     * @param newFlag      the new flag to set.
     * @param newFlagValue the value of the new flag.
     */
    public ChangeMessageFlagEmailHandler(Collection<String> oldFlagSet, String newFlag, boolean newFlagValue) {
        this(newFlag, newFlagValue);
        this.oldFlagSet.addAll(oldFlagSet);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the message flags with given configuration.
     *
     * @param message javax.mail.Message to handle.
     * @throws Exception if message flags cannot be changed.
     */
    @Override
    public void handleEmail(Message message) throws Exception {
        if (!oldFlagSet.isEmpty()) {
            if (message.getFlags().contains(toFlags(oldFlagSet)))
                setMessageFlags(message);
        } else
            setMessageFlags(message);

    }

    /**
     * Sets the <pre>{@link Flags}</pre> object for the message.
     *
     * @param message message to set flags to.
     * @throws MessagingException if flags cannot be set.
     */
    private void setMessageFlags(Message message) throws MessagingException {
        message.setFlags(getFlags(newFlag), newFlagValue);
    }

    /**
     * Transforms the collection of flag names to <pre>{@link Flags}</pre>
     * object.
     *
     * @param flagNames a collection of flag names to transform.
     * @return <pre>{@link Flags}</pre> object that contains the flags defined
     * by flagNames
     */
    private Flags toFlags(Collection<String> flagNames) {
        Flags flags = new Flags();
        flagNames.forEach(name -> flags.add(getFlags(name)));
        return flags;
    }

    /**
     * Returns <pre>{@link Flags}</pre> object for the given flagName.
     * If the given flagName is defined in systemFlagsMap it will return a
     * <pre>{@link Flags}</pre> with predefined
     * <pre>{@link javax.mail.Flags.Flag}</pre> otherwise it will contain a
     * user defined custom flag.
     *
     * @param flagName the name of the flag.
     * @return <pre>{@link Flags}</pre> object that contains the flag with the
     * flagName.
     */
    private Flags getFlags(String flagName) {
        Flags.Flag flag = systemFlagsMap.get(flagName);
        return flag == null ? new Flags(flagName) : new Flags(flag);
    }
}
