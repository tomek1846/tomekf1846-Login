package pl.tomekf1846.Login.Spigot.Security;

import java.util.List;
import java.util.Locale;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLogFilter implements Filter {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/([\\w:-]+)");
    private final Filter delegate;
    private final boolean enabled;
    private final List<String> hiddenCommands;

    public CommandLogFilter(Filter delegate, boolean enabled, List<String> hiddenCommands) {
        this.delegate = delegate;
        this.enabled = enabled;
        this.hiddenCommands = hiddenCommands;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (delegate != null && !delegate.isLoggable(record)) {
            return false;
        }
        if (!enabled || record == null) {
            return true;
        }
        String message = record.getMessage();
        if (message == null) {
            return true;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.contains("issued server command")) {
            return true;
        }
        Matcher matcher = COMMAND_PATTERN.matcher(lower);
        if (!matcher.find()) {
            return true;
        }
        String command = matcher.group(1);
        return hiddenCommands.stream().noneMatch(command::equalsIgnoreCase);
    }
}
