package net.kunmc.lab.newvote;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NewVote extends JavaPlugin {

    private static final String YANA_PLAYER_NAME = "Yanaaaaa";

    private static boolean vs = false;
    private static boolean vget = false;
    private static boolean vlist = false;

    private static List<String> senderListCurrent = new ArrayList<>();
    private static List<String> receiverListCurrent = new ArrayList<>();
    private static List<String> senderListResult = new ArrayList<>();
    private static List<String> receiverListResult = new ArrayList<>();
    private static List<String> voteTargetList = new ArrayList<>();

    private boolean yanaRevealReserved = false;
    private static boolean yanaRevealActive = false;
    private static boolean yanaFeatureEnabled = true;
    private int yanaCountdownVotes;
    private int rank = 1;
    private String yanaRankLabel;

    private boolean debugEnabled;
    private boolean debugLogStackTrace;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadDebugConfig();

        boolean allCommandsRegistered = true;
        allCommandsRegistered &= safeRegisterCommand("v", true);
        allCommandsRegistered &= safeRegisterCommand("yvote", false);
        allCommandsRegistered &= safeRegisterCommand("vs", false);
        allCommandsRegistered &= safeRegisterCommand("vget", false);

        if (!allCommandsRegistered) {
            getLogger().severe("Plugin disabled because one or more commands are missing.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("NewVotePlugin by Cyrne1_7208");
    }

    @Override
    public void onDisable() {
        clearAllRuntimeState();
    }

    private void loadDebugConfig() {
        debugEnabled = getConfig().getBoolean("debug.enabled", false);
        debugLogStackTrace = getConfig().getBoolean("debug.logStackTrace", true);
    }

    private void clearAllRuntimeState() {
        vs = false;
        vget = false;
        vlist = false;
        senderListCurrent.clear();
        receiverListCurrent.clear();
        senderListResult.clear();
        receiverListResult.clear();
        voteTargetList.clear();
        yanaRevealReserved = false;
        yanaRevealActive = false;
        yanaFeatureEnabled = true;
        yanaCountdownVotes = 0;
        yanaRankLabel = null;
    }

    private boolean safeRegisterCommand(String commandName, boolean tabComplete) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().severe("Command not found in plugin.yml: " + commandName);
            return false;
        }
        command.setExecutor(this);
        if (tabComplete) {
            command.setTabCompleter(this);
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            String commandName = cmd.getName().toLowerCase(Locale.ROOT);
            switch (commandName) {
                case "v":
                    return handleVoteCommand(sender, args);
                case "yvote":
                    return handleYvoteCommand(sender, args);
                case "vs":
                    return handleVsCommand(sender, args);
                case "vget":
                    return handleVgetCommand(sender, args);
                default:
                    return false;
            }
        } catch (Exception ex) {
            logError("Unexpected error while handling command", sender, cmd, args, ex);
            sender.sendMessage(ChatColor.RED + "内部エラーが発生しました。管理者にログを共有してください。");
            return true;
        }
    }

    private boolean handleVoteCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /v <投票先>");
            return true;
        }

        if (!vs) {
            sender.sendMessage(ChatColor.RED + "投票は開始されていません。");
            return true;
        }

        Player player = (Player) sender;
        reloadConfig();
        loadDebugConfig();
        loadVoteTargetList();

        UUID senderUuid = player.getUniqueId();
        if (containsSenderUuid(senderUuid, senderListCurrent)) {
            sender.sendMessage(ChatColor.YELLOW + "あなたはすでに投票済みです。");
            return true;
        }

        String receiver = args[0];
        if (!voteTargetList.contains(receiver)) {
            sender.sendMessage(ChatColor.RED + receiver + " は投票先リストに存在しません。");
            return true;
        }

        senderListCurrent.add(senderUuid.toString());
        receiverListCurrent.add(receiver);
        ScoreBoardLogic.setVoteStatus(1, player, this);
        sender.sendMessage(ChatColor.GREEN + receiver + " に投票しました。");
        debug("Vote accepted: voter=" + player.getName() + ", target=" + receiver);

        return true;
    }

    private void loadVoteTargetList() {
        voteTargetList = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> voteTargetList.add(player.getName()));

        List<String> configuredTargets = getConfig().getStringList("List");
        if (!configuredTargets.isEmpty()) {
            voteTargetList = new ArrayList<>(configuredTargets);
        }

        voteTargetList = voteTargetList.stream().distinct().collect(Collectors.toList());
    }

    private boolean containsSenderUuid(UUID uuid, List<String> senderList) {
        return senderList.contains(uuid.toString());
    }

    private boolean handleYvoteCommand(CommandSender sender, String[] args) {
        if (!sender.getName().equals(YANA_PLAYER_NAME)) {
            sender.sendMessage(ChatColor.RED + "このコマンドは使用できません。");
            return true;
        }

        if (args.length == 0) {
            yanaFeatureEnabled = !yanaFeatureEnabled;
            sender.sendMessage(ChatColor.GOLD + (yanaFeatureEnabled ? "やなーもーどおん" : "やなーもーどおふ"));
            debug("Yana mode toggled: " + yanaFeatureEnabled);
            return true;
        }

        if (args.length == 1) {
            try {
                int parsedRank = Integer.parseInt(args[0]);
                if (parsedRank < 1) {
                    sender.sendMessage(ChatColor.RED + "順位は1以上を指定してください。");
                    return true;
                }
                rank = parsedRank;
                sender.sendMessage(ChatColor.GOLD + "基準を " + parsedRank + " 位以上にしました。");
                debug("Yana rank threshold updated: " + rank);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "数値で指定してください。");
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "コマンド形式: /yvote [順位]");
        return true;
    }

    private boolean handleVsCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /vs");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。必要権限: OP");
            return true;
        }

        if (vlist) {
            ScoreBoardLogic.setVoteStatus(2, asPlayerOrNull(sender), this);
            vlist = false;
            sender.sendMessage(ChatColor.GREEN + "Tab リストから投票先表示を削除しました。");
            debug("Vote list view cleared.");
            return true;
        }

        if (!vs) {
            reloadConfig();
            loadDebugConfig();

            senderListCurrent.clear();
            receiverListCurrent.clear();
            senderListResult.clear();
            receiverListResult.clear();
            voteTargetList.clear();

            yanaRevealActive = false;
            yanaRevealReserved = false;

            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendMessage(ChatColor.GOLD + "投票が開始されました。");
                player.sendMessage(ChatColor.GREEN + "/v <投票先> で投票してください。");
                ScoreBoardLogic.setVoteStatus(0, player, this);
            });

            vs = true;
            vget = false;
            debug("Voting started.");
            return true;
        }

        senderListResult = new ArrayList<>(senderListCurrent);
        receiverListResult = new ArrayList<>(receiverListCurrent);
        senderListCurrent.clear();
        receiverListCurrent.clear();

        sendVotingResult(receiverListResult);
        ScoreBoardLogic.setVoteStatus(2, asPlayerOrNull(sender), this);
        voteTargetList = new ArrayList<>();
        debug("Voting closed. resultsCount=" + receiverListResult.size());

        return true;
    }

    private Player asPlayerOrNull(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    private boolean handleVgetCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "コマンド形式: /vget");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。必要権限: OP");
            return true;
        }

        if (!vget) {
            sender.sendMessage(ChatColor.RED + "投票結果がまだ確定していません。");
            return true;
        }

        VoteResultLogic.sendVotingDestination(senderListResult, receiverListResult, this);
        vs = false;
        vget = false;
        vlist = true;
        debug("Vote destinations revealed.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("v") || args.length != 1) {
            return new ArrayList<>();
        }

        reloadConfig();
        loadVoteTargetList();

        final String prefix = args[0].toLowerCase(Locale.ROOT);
        return voteTargetList.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }

    public void sendVotingResult(List<String> receivers) {
        try {
            Map<String, Integer> voteCountMap = new HashMap<>();
            for (String receiver : receivers) {
                Integer count = voteCountMap.get(receiver);
                voteCountMap.put(receiver, count == null ? 1 : count + 1);
            }

            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(voteCountMap.entrySet());
            Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                    int byVote = right.getValue().compareTo(left.getValue());
                    if (byVote != 0) {
                        return byVote;
                    }
                    return left.getKey().compareTo(right.getKey());
                }
            });

            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendMessage(ChatColor.GOLD + "投票結果を表示します。");
                player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
            });

            int ranking = 1;
            int iteration = 0;
            int previousVotes = Integer.MIN_VALUE;

            for (Map.Entry<String, Integer> entry : sortedEntries) {
                iteration++;
                int currentVotes = entry.getValue();
                if (iteration > 1 && currentVotes != previousVotes) {
                    ranking++;
                }

                String rankText = Integer.toString(ranking);
                Bukkit.getOnlinePlayers().forEach(player ->
                        player.sendMessage(ChatColor.GREEN + rankText + "位: " + ChatColor.WHITE + entry.getKey() + ChatColor.GOLD + " [" + currentVotes + "票]")
                );

                if (entry.getKey().equals(YANA_PLAYER_NAME) && ranking <= rank) {
                    yanaRevealReserved = true;
                    yanaCountdownVotes = currentVotes;
                    yanaRankLabel = rankText;
                }

                previousVotes = currentVotes;
            }

            Bukkit.getOnlinePlayers().forEach(player ->
                    player.sendMessage(ChatColor.AQUA + "==========投票結果==========")
            );

            if (yanaRevealReserved && yanaFeatureEnabled) {
                yanaRevealReserved = false;
                yanaRevealActive = true;

                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendTitle(ChatColor.AQUA + "おや？投票結果の様子が...!?", null, 5, 60, 5);
                    playSoundCompat(player, Sound.ENTITY_GENERIC_EXPLODE, Sound.BLOCK_ANVIL_PLACE, 5f, 1f);
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendTitle(ChatColor.GREEN + "やなぱわ～", null, 5, 40, 5);
                            playSoundCompat(player, Sound.ENTITY_PLAYER_LEVELUP, Sound.UI_TOAST_CHALLENGE_COMPLETE, 5f, 1f);
                        });
                        vs = false;
                        vget = true;
                        debug("Yana reveal first stage completed.");
                    }
                }.runTaskLater(this, 120L);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (yanaCountdownVotes >= 0) {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                player.sendTitle(
                                        ChatColor.GREEN + yanaRankLabel + "位: " + ChatColor.WHITE + YANA_PLAYER_NAME + ChatColor.GOLD + " [" + yanaCountdownVotes + "票]",
                                        null,
                                        0,
                                        20,
                                        0
                                );
                                playSoundCompat(player, Sound.BLOCK_ANVIL_PLACE, Sound.BLOCK_NOTE_BLOCK_PLING, 5f, 1f);
                            });
                            yanaCountdownVotes--;
                            return;
                        }

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendMessage(ChatColor.GOLD + "投票結果を再表示します。");
                            player.sendTitle(ChatColor.GOLD + "投票結果を再表示します。", null, 5, 20, 5);
                            player.sendMessage(ChatColor.AQUA + "==========投票結果==========");
                        });

                        int rerank = 1;
                        int reIteration = 0;
                        int rePreviousVotes = Integer.MIN_VALUE;

                        for (Map.Entry<String, Integer> entry : sortedEntries) {
                            if (entry.getKey().equals(YANA_PLAYER_NAME)) {
                                continue;
                            }

                            reIteration++;
                            int currentVotes = entry.getValue();
                            if (reIteration > 1 && currentVotes != rePreviousVotes) {
                                rerank++;
                            }

                            String rankText = Integer.toString(rerank);
                            Bukkit.getOnlinePlayers().forEach(player ->
                                    player.sendMessage(ChatColor.GREEN + rankText + "位: " + ChatColor.WHITE + entry.getKey() + ChatColor.GOLD + " [" + currentVotes + "票]")
                            );
                            rePreviousVotes = currentVotes;
                        }

                        Bukkit.getOnlinePlayers().forEach(player ->
                                player.sendMessage(ChatColor.AQUA + "==========投票結果==========")
                        );
                        this.cancel();
                        debug("Yana reveal second stage completed.");
                    }
                }.runTaskTimer(this, 200L, 15L);
            } else {
                vs = false;
                vget = true;
            }
        } catch (Exception ex) {
            logError("Failed to send voting result", null, null, null, ex);
            Bukkit.getOnlinePlayers().forEach(player ->
                    player.sendMessage(ChatColor.RED + "投票結果の表示中にエラーが発生しました。")
            );
            vs = false;
            vget = true;
        }
    }

    private void playSoundCompat(Player player, Sound primary, Sound fallback, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), primary, volume, pitch);
        } catch (IllegalArgumentException | NoSuchFieldError ex) {
            player.playSound(player.getLocation(), fallback, volume, pitch);
        }
    }

    private void debug(String message) {
        if (!debugEnabled) {
            return;
        }
        getLogger().info("[NewVote][DEBUG] " + message);
    }

    private void logError(String context, CommandSender sender, Command cmd, String[] args, Throwable ex) {
        getLogger().severe("[NewVote][ERROR] " + context);
        if (sender != null) {
            getLogger().severe("[NewVote][ERROR] sender=" + asDebugSender(sender));
        }
        if (cmd != null) {
            getLogger().severe("[NewVote][ERROR] command=" + asDebugCommand(cmd));
        }
        if (args != null) {
            getLogger().severe("[NewVote][ERROR] args=" + asDebugArgs(args));
        }
        getLogger().severe("[NewVote][ERROR] state: vs=" + vs + ", vget=" + vget + ", vlist=" + vlist
                + ", currentVotes=" + receiverListCurrent.size() + ", resultVotes=" + receiverListResult.size());

        if (debugLogStackTrace) {
            ex.printStackTrace();
        } else {
            getLogger().severe("[NewVote][ERROR] message=" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String asDebugSender(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return player.getName() + "(" + player.getUniqueId() + ")";
        }
        return sender.getName() + "(non-player)";
    }

    private String asDebugCommand(Command cmd) {
        return cmd.getName();
    }

    private String asDebugArgs(String[] args) {
        if (args.length == 0) {
            return "[]";
        }
        List<String> normalized = new ArrayList<>();
        Collections.addAll(normalized, args);
        return normalized.toString();
    }

    public boolean isVotingOpen() {
        return vs;
    }

    public boolean isVoteResultPhase() {
        return vget;
    }

    public boolean isVoteListShown() {
        return vlist;
    }

    public boolean isYanaRevealActive() {
        return yanaRevealActive;
    }

    public boolean isYanaFeatureEnabled() {
        return yanaFeatureEnabled;
    }

    public String getYanaPlayerName() {
        return YANA_PLAYER_NAME;
    }
}
