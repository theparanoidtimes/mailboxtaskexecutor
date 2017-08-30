package org.theparanoidtimes.tabellarium.handlers;

import org.theparanoidtimes.tabellarium.api.EmailHandler;
import org.theparanoidtimes.tabellarium.api.UnlinkedEmailHandler;

import javax.mail.Message;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * An <pre>{@link EmailHandler}</pre> implementation for writing e-mails into
 * a file.
 *
 * @author djosifovic
 */
public class PrintInFileEmailHandler extends UnlinkedEmailHandler {

    /**
     * Name of the file in which to print the output.
     */
    private String fileName;

    /**
     * <pre>{@link PrintingEmailHandler}</pre> instance that will print in
     * file.
     */
    private PrintingEmailHandler printingEmailHandler = null;

    /**
     * <pre>{@link PrintStream}</pre> instance for writing in file.
     */
    private PrintStream stream = null;

    /**
     * A flag indicating if handler should print only mail headers.
     */
    private boolean printHeadersOnly = false;

    /**
     * Constructs a new instance.
     *
     * @param fileName file in which to print the output.
     */
    public PrintInFileEmailHandler(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Prints the passed <pre>{@link Message}</pre> to the target file.
     * finish() should be called after the last call to this method.
     *
     * @param message javax.mail.Message to handle.
     * @throws Exception if there was an error while opening stream to file or
     *                   while printing in to the file.
     * @see PrintInFileEmailHandler#finish()
     */
    @Override
    public void doHandleEmail(Message message) throws Exception {
        initStream();
        printingEmailHandler.handleEmail(message);
    }

    /**
     * Initialized the print stream to file and instantiates the
     * <pre>{@link PrintingEmailHandler}</pre>.
     *
     * @throws Exception if error happens when instantiating the print stream.
     */
    private void initStream() throws Exception {
        if (printingEmailHandler == null && stream == null) {
            this.stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
            printingEmailHandler = new PrintingEmailHandler(stream);
            printingEmailHandler.setPrintHeadersOnly(printHeadersOnly);
        }
    }

    /**
     * Closes the files stream and sets stream and
     * <pre>{@link PrintInFileEmailHandler}</pre> to null.
     *
     * @see PrintInFileEmailHandler#handleEmail(Message)
     */
    public void finish() {
        if (stream != null) {
            stream.flush();
            stream.close();
            stream = null;
        }
        printingEmailHandler = null;
    }

    /**
     * Returns the current file name of the file in which this handler is
     * writing into.
     *
     * @return the current file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name of the file to write into.
     *
     * @param fileName file name to set.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns printHeadersOnly flag.
     *
     * @return printHeadersOnly flag.
     */
    public boolean isPrintHeadersOnly() {
        return printHeadersOnly;
    }

    /**
     * Sets printHeadersOnly flag.
     *
     * @param printHeadersOnly a boolean value.
     */
    public void setPrintHeadersOnly(boolean printHeadersOnly) {
        this.printHeadersOnly = printHeadersOnly;
    }
}
