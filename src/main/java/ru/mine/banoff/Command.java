package ru.mine.banoff;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import ru.mine.banoff.BSalary;
import ru.mine.banoff.utils.ConfigUtil;
import ru.mine.banoff.utils.DBUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Command implements CommandExecutor {

    private final DBUtil dbUtil;
    private final BSalary plugin;
    Sound soundac = Sound.valueOf(ConfigUtil.getString("sounds.accepted"));
    Sound sounddec = Sound.valueOf(ConfigUtil.getString("sounds.declined"));

    public Command(BSalary plugin, DBUtil dbUtil) {
        this.plugin = plugin;
        this.dbUtil = dbUtil;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {

        if (!(commandSender instanceof Player)) {
            plugin.getLogger().warning("Нельзя писать от консоли");
            return true;
        }

        Player p = (Player) commandSender;
        String group = BSalary.getPermission().getPrimaryGroup(p).toLowerCase();

        ConfigurationSection salariesSection = plugin.getConfig().getConfigurationSection("salaries");
        if (salariesSection == null || !salariesSection.getKeys(false).contains(group)) {
            p.sendMessage(ConfigUtil.getString("messages.null_group"));
            return true;
        }

        ConfigurationSection groupSection = salariesSection.getConfigurationSection(group);
        if (groupSection == null) {
            p.sendMessage(ConfigUtil.getString("messages.null_group"));
            return true;
        }

        int cooldownSeconds = groupSection.getInt("cooldown", 600);
        List<String> commands = groupSection.getStringList("commands");

        CompletableFuture.runAsync(() -> {
            try (Connection connect = DriverManager.getConnection(dbUtil.getUrl())) {
                try (PreparedStatement pstmt = connect.prepareStatement(
                        "SELECT player_name, cooldown FROM players WHERE player_name = ? LIMIT 1")) {
                    pstmt.setString(1, p.getName());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        long now = System.currentTimeMillis();
                        if (rs.next()) {
                            long cooldown = rs.getLong("cooldown");
                            boolean isCooldownNull = rs.wasNull();

                            if (isCooldownNull || cooldown == 0 || (now - cooldown) >= cooldownSeconds * 1000L) {

                                List<String> preparedCommands = new ArrayList<>();
                                for (String cmd : commands) {
                                    preparedCommands.add(cmd.replace("{player}", p.getName()));
                                }
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    for (String commandForRun : preparedCommands) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandForRun);
                                    }
                                });

                                try (PreparedStatement updpstmt = connect.prepareStatement(
                                        "UPDATE players SET cooldown = ? WHERE player_name = ?")) {
                                    updpstmt.setLong(1, now);
                                    updpstmt.setString(2, p.getName());
                                    int updated = updpstmt.executeUpdate();

                                    if (updated == 0) {
                                        try (PreparedStatement insertStmt = connect.prepareStatement(
                                                "INSERT INTO players (player_name, cooldown) VALUES (?, ?)")) {
                                            insertStmt.setString(1, p.getName());
                                            insertStmt.setLong(2, now);
                                            insertStmt.executeUpdate();
                                        }
                                    }
                                }

                                p.sendMessage(ConfigUtil.getString("messages.accepted"));
                                p.playSound(p.getLocation(), soundac, 1.0f, 1.0f);

                            } else {
                                long timeToLeft = ((cooldown + cooldownSeconds * 1000L) - now) / 1000;

                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    p.sendMessage(ConfigUtil.getString("messages.cooldown")
                                            .replace("{time}", String.valueOf(timeToLeft)));
                                    p.playSound(p.getLocation(), sounddec, 1.0f, 1.0f);
                                });

                            }
                        } else {
                            for (String cmd : commands) {
                                String commandToRun = cmd.replace("{player}", p.getName());
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                                });
                            }
                            try (PreparedStatement insertStmt = connect.prepareStatement(
                                    "INSERT INTO players (player_name, cooldown) VALUES (?, ?)")) {
                                insertStmt.setString(1, p.getName());
                                insertStmt.setLong(2, System.currentTimeMillis());
                                insertStmt.executeUpdate();
                            }

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                p.sendMessage(ConfigUtil.getString("messages.accepted"));
                                p.playSound(p.getLocation(), soundac, 1.0f, 1.0f);
                            });

                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return true;
    }
}
