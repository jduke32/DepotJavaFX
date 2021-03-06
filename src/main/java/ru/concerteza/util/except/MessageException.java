package ru.concerteza.util.except;

import static ru.concerteza.util.string.CtzFormatUtils.format;

/**
 * Runtime exception, that carries important business-logic error message,
 * to extract message from exception stack
 *
 * @author alexey,
 * Date: 10/18/11
 */
public abstract class MessageException extends RuntimeException {
    private static final long serialVersionUID = 4668320002581645785L;

    /**
     * Message only constructor,  message will be formatted using {@link ru.concerteza.util.string.CtzFormatUtils#format}
     * @param formatString formatString format string with '{}' placeholders
     * @param args format arguments
     */
    protected MessageException(String formatString, Object... args) {
        super(format(formatString, args));
    }

    /**
     * Exceptions and message constructor, message will be formatted using {@link ru.concerteza.util.string.CtzFormatUtils#format}
     * @param cause Cause exception
     * @param formatString format string with '{}' placeholders
     * @param args format arguments
     */
    protected MessageException(Exception cause, String formatString, Object... args) {
        super(format(formatString, args), cause);
    }
}