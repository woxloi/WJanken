package github.com.woxloi.janken;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class JankenGame {
    private final String name;
    private final Set<UUID> participants = new HashSet<>();
    private final Map<UUID, String> hands = new HashMap<>();
    private final FileConfiguration config;
    private final Economy econ;
    private final JavaPlugin plugin;
    private final int joinPrice;
    private boolean isActive = true;

    public JankenGame(String name, FileConfiguration config, Economy econ, JavaPlugin plugin) {
        this.name = name;
        this.config = config;
        this.econ = econ;
        this.plugin = plugin;
        this.joinPrice = config.getInt("games." + name + ".join_price", 1000);
    }

    public void addPlayer(Player player) {
        participants.add(player.getUniqueId());
        player.sendMessage("§a「" + name + "」に参加しました！手を選んでね！");
    }

    public boolean hasPlayer(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public void setHand(Player player, String hand) {
        if (!isActive) {
            player.sendMessage("§cゲームは終了しています。");
            return;
        }
        if (!participants.contains(player.getUniqueId())) {
            player.sendMessage("§cまだゲームに参加していません！");
            return;
        }
        if (!hand.equals("gu") && !hand.equals("choki") && !hand.equals("pa")) {
            player.sendMessage("§c正しい手（gu, choki, pa）を選んでください。");
            return;
        }
        hands.put(player.getUniqueId(), hand);
        player.sendMessage("§a「" + hand + "」を出しました！");
    }

    public void startTimer() {
        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 20L * 30);  // 30秒後に終了
    }

    private void endGame() {
        isActive = false;

        // 手を出していないプレイヤーに返金
        for (UUID uuid : participants) {
            if (!hands.containsKey(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage("§c手を出さなかったので返金されました！");
                    econ.depositPlayer(player, joinPrice);
                }
            }
        }

        // じゃんけんの勝者判定（単純に勝った人を表示）
        Map<String, Set<UUID>> handsMap = new HashMap<>();
        handsMap.put("gu", new HashSet<>());
        handsMap.put("choki", new HashSet<>());
        handsMap.put("pa", new HashSet<>());

        for (Map.Entry<UUID, String> entry : hands.entrySet()) {
            handsMap.get(entry.getValue()).add(entry.getKey());
        }

        Set<UUID> guPlayers = handsMap.get("gu");
        Set<UUID> chokiPlayers = handsMap.get("choki");
        Set<UUID> paPlayers = handsMap.get("pa");

        // 勝者判定
        Set<UUID> winners = new HashSet<>();

        // じゃんけんの基本ルール：
        // グーはチョキに勝つ、チョキはパーに勝つ、パーはグーに勝つ。

        boolean guExists = !guPlayers.isEmpty();
        boolean chokiExists = !chokiPlayers.isEmpty();
        boolean paExists = !paPlayers.isEmpty();

        if (guExists && chokiExists && !paExists) {
            winners.addAll(guPlayers);
        } else if (chokiExists && paExists && !guExists) {
            winners.addAll(chokiPlayers);
        } else if (paExists && guExists && !chokiExists) {
            winners.addAll(paPlayers);
        } else {
            // 引き分け（全て揃った、または2手以上同時）
            winners.clear();
        }

        if (winners.isEmpty()) {
            Bukkit.broadcastMessage("§eじゃんけん「" + name + "」は引き分けでした。");
        } else {
            StringBuilder winnerNames = new StringBuilder();
            for (UUID uuid : winners) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    winnerNames.append(player.getName()).append(" ");
                    // 勝者に賞金支払い(例: 参加費の2倍)
                    econ.depositPlayer(player, joinPrice * 2);
                    player.sendMessage("§aあなたはじゃんけんに勝ちました！賞金 " + (joinPrice * 2) + " 円を受け取りました！");
                }
            }
            Bukkit.broadcastMessage("§aじゃんけん「" + name + "」の勝者は " + winnerNames.toString() + "です！");
        }

        // ゲーム終了後、参加者リスト・手をクリア
        participants.clear();
        hands.clear();
    }

    public boolean isActive() {
        return isActive;
    }
}
