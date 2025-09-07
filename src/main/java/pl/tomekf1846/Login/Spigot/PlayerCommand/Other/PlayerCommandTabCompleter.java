package pl.tomekf1846.Login.Spigot.PlayerCommand.Other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return suggestions;
        }

        if (args.length == 1) {
            switch (alias.toLowerCase()) {
                case "login":
                case "log":
                case "l":
                case "premium":
                    suggestions.add("(password)");
                    break;
                case "cracked":
                    suggestions.add("(new-password)");
                    break;
                case "email":
                    suggestions.add("(email)");
                    break;
                case "register":
                case "reg":
                    suggestions.add("(password)");
                    break;
                case "changepass":
                case "changepassword":
                    suggestions.add("(old-password)");
                    break;
            }
        }
        else if (args.length == 2) {
            switch (alias.toLowerCase()) {
                case "register":
                case "reg":
                    suggestions.add("(repeat-password)");
                    break;
                case "changepass":
                case "changepassword":
                    suggestions.add("(new-password)");
                    break;
            }
        }

        return filterSuggestions(suggestions, args[args.length - 1]);
    }

    private List<String> filterSuggestions(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
