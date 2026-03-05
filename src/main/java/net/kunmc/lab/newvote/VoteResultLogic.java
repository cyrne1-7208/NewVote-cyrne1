package net.kunmc.lab.newvote;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class VoteResultLogic {

    static void sendVotingDestination(List<String> senders, List<String> receivers, NewVote plugin) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(ChatColor.GOLD + "投票先を開示します。");
            player.sendMessage(ChatColor.AQUA + "Tabを押してスコアボードを確認してください。");
        });

        int loopCount = Math.min(senders.size(), receivers.size());
        for (int i = 0; i < loopCount; i++) {
            String senderName = resolveSenderName(senders.get(i));
            String receiverName = receivers.get(i);
            ScoreBoardLogic.setVoteResult(senderName, receiverName, plugin);
        }
    }

    private static String resolveSenderName(String rawSender) {
        if (rawSender == null || rawSender.isEmpty()) {
            return "";
        }

        try {
            UUID uuid = UUID.fromString(rawSender);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return player.getName();
            }
        } catch (IllegalArgumentException ignored) {
            return rawSender;
        }

        return rawSender;
    }
}
