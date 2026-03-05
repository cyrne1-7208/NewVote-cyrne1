package net.kunmc.lab.newvote;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ScoreBoardLogic {

    static void setVoteStatus(int pattern, Player sender, NewVote plugin) {
        switch (pattern) {
            case 0:
                if (sender != null) {
                    safeSetPlayerListName(plugin, sender, ChatColor.GRAY + " × " + ChatColor.WHITE + sender.getName());
                }
                break;
            case 1:
                if (sender != null) {
                    safeSetPlayerListName(plugin, sender, ChatColor.GOLD + " ✓ " + ChatColor.WHITE + sender.getName());
                }
                break;
            default:
                Bukkit.getOnlinePlayers().forEach(player -> safeSetPlayerListName(plugin, player, player.getName()));
                break;
        }
    }

    static void setVoteResult(String sender, String receiver, NewVote plugin) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getName().equals(sender)) {
                if (plugin.isYanaRevealActive() && receiver.equals(plugin.getYanaPlayerName()) && plugin.isYanaFeatureEnabled()) {
                    safeSetPlayerListName(plugin, player, player.getName() + " : " + ChatColor.MAGIC + "????????");
                } else {
                    safeSetPlayerListName(plugin, player, player.getName() + " : " + ChatColor.AQUA + receiver);
                }
            }
        });
    }

    private static void safeSetPlayerListName(NewVote plugin, Player player, String text) {
        try {
            player.setPlayerListName(text);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to update player list name for " + player.getName() + ": " + ex.getMessage());
        }
    }
}
