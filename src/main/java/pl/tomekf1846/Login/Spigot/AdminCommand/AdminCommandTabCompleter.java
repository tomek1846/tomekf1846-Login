package pl.tomekf1846.Login.Spigot.AdminCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("about");
            suggestions.add("gui");
            suggestions.add("reload");
            suggestions.add("cracked");
            suggestions.add("premium");
            suggestions.add("changepass");
            suggestions.add("changepassword");
            suggestions.add("unregister");
            suggestions.add("register");
            suggestions.add("email");
            suggestions.add("forcelogin");
            suggestions.add("ip");
        } else if (args.length == 2) {
            if (matchesAny(args[0], "cracked", "premium", "forcelogin", "ip", "unregister", "changepass", "changepassword", "email", "register")) {
                return getMatchingPlayers(args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("cracked")) {
                suggestions.add("(new-password)");
            } else if (matchesAny(args[0], "changepass", "changepassword")) {
                suggestions.add("(password)");
            } else if (args[0].equalsIgnoreCase("email")) {
                suggestions.add("(Email)");
                suggestions.add("(None)");
            } else if (args[0].equalsIgnoreCase("register")) {
                suggestions.add("(Password)");
            }
        }

        return filterSuggestions(suggestions, args[args.length - 1]);
    }

    private List<String> getMatchingPlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filterSuggestions(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean matchesAny(String input, String... options) {
        for (String option : options) {
            if (input.equalsIgnoreCase(option)) {
                return true;
            }
        }
        return false;
    }
}
