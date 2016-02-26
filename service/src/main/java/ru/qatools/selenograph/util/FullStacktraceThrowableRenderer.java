package ru.qatools.selenograph.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.EnhancedThrowableRenderer;
import org.apache.log4j.spi.ThrowableRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ilya Sadykov
 */
public class FullStacktraceThrowableRenderer implements ThrowableRenderer {
    private static final EnhancedThrowableRenderer RENDERER = new EnhancedThrowableRenderer();

    private static String[] formatStackTrace(Throwable exc) {
        Throwable cause = exc;
        final List<String> result = new ArrayList<>();
        result.add("=============== Full stacktrace follows: ===============");
        while (cause != null) {
            result.add("Caused by " + cause.toString() + ": " + cause.getMessage());
            result.addAll(formatStackTrace(cause.getStackTrace()));
            cause = cause.getCause();
        }
        return result.stream().toArray(String[]::new);
    }

    private static List<String> formatStackTrace(StackTraceElement[] stackTraceElements) {
        final List<String> result = new ArrayList<>();
        for (StackTraceElement element : stackTraceElements) {
            final StringBuilder builder = new StringBuilder();
            builder.append("\tat ").append(element.getClassName()).
                    append(".").append(element.getMethodName());
            builder.append("(").append(element.getFileName());
            if (element.getLineNumber() > 0) {
                builder.append(":").append(element.getLineNumber());
            }
            builder.append(")");
            result.add(builder.toString());
        }
        return result;
    }

    @Override
    public String[] doRender(Throwable t) {
        return ArrayUtils.addAll(RENDERER.doRender(t), formatStackTrace(t));
    }
}
