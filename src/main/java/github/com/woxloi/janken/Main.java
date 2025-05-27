package github.com.woxloi.janken;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin {

    private final Map<String, JankenGame> activeGames = new HashMap<>();
    private Economy econ;

    @Override
    public void onEnable() {
        getLogger().info("プラグインの起動に成功しました！");

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vaultが見つかりません！プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        econ = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
        if (econ == null) {
            getLogger().warning("経済プラグインが見つかりません！プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cコンソールからはこのコマンドは実行できないよ！！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cゲームを開くには /janken start ゲーム名 だよ！");
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("start")) {
            if (args.length < 2) {
                player.sendMessage("§cゲーム名を指定してね！ /janken start ゲーム名");
                return true;
            }

            String gameName = args[1];

            File dataFolder = getDataFolder();
            File file = new File(dataFolder, "games.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (!config.contains("games." + gameName)) {
                player.sendMessage("§cそのゲームは存在しません！");
                return true;
            }

            int joinPrice = config.getInt("games." + gameName + ".join_price", 1000);

            if (econ.getBalance(player) < joinPrice) {
                player.sendMessage("§cお金が足りません！参加費は " + joinPrice + " 円です。");
                return true;
            }

            // 既にアクティブなゲームがあればそのまま使う（重複作成防止）
            JankenGame game = activeGames.get(gameName);
            if (game == null || !game.isActive()) {
                game = new JankenGame(gameName, config, econ, this);
                activeGames.put(gameName, game);
                game.startTimer();
                Bukkit.broadcastMessage("§aじゃんけん「" + gameName + "」が開始されました！30秒後に終了します！");
            }

            game.addPlayer(player);
            econ.withdrawPlayer(player, joinPrice);
            player.sendMessage("§a参加費 " + joinPrice + " 円を支払いました！");

            return true;

        } else if (sub.equalsIgnoreCase("create")) {
            if (!player.isOp()) {
                player.sendMessage("§cこのコマンドはOPだけが使えます！");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cゲーム名を指定してね！ /janken create ゲーム名");
                return true;
            }

            String gameName = args[1];

            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            File file = new File(dataFolder, "games.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (config.contains("games." + gameName)) {
                player.sendMessage("§cそのゲーム名は既に存在します！");
                return true;
            }

            config.set("games." + gameName + ".join_price", 1000);
            try {
                config.save(file);
                player.sendMessage("§aゲーム「" + gameName + "」を作成しました！");
            } catch (IOException e) {
                player.sendMessage("§cゲーム作成に失敗しました...");
                e.printStackTrace();
            }
            return true;

        } else if (sub.equalsIgnoreCase("gu") || sub.equalsIgnoreCase("choki") || sub.equalsIgnoreCase("pa")) {
            // 参加しているゲームを探す
            JankenGame playerGame = null;
            for (JankenGame game : activeGames.values()) {
                if (game.isActive() && game.hasPlayer(player)) {
                    playerGame = game;
                    break;
                }
            }

            if (playerGame == null) {
                player.sendMessage("§c参加中のじゃんけんゲームが見つかりません！ /janken start ゲーム名 で参加してね。");
                return true;
            }

            String hand = sub.toLowerCase();
            playerGame.setHand(player, hand);
            Bukkit.broadcastMessage("§a" + player.getName() + "さんが「" + hand + "」を出しました！");

            return true;

        } else {
            if (player.isOp()) {
                player.sendMessage("§cコマンドが違うよ！");
                player.sendMessage("§c/janken start ゲーム名");
                player.sendMessage("§c/janken create ゲーム名");
                player.sendMessage("§c/janken gu");
                player.sendMessage("§c/janken choki");
                player.sendMessage("§c/janken pa");
                player.sendMessage("§c以下のコマンドを打って試してみてね！");
            } else {
                player.sendMessage("§cコマンドが違うよ！");
                player.sendMessage("§c/janken start ゲーム名");
                player.sendMessage("§c/janken gu");
                player.sendMessage("§c/janken choki");
                player.sendMessage("§c/janken pa");
                player.sendMessage("§c以下のコマンドを打って試してみてね！");
            }
            return true;
        }
    }
}
