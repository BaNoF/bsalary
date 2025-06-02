package ru.mine.banoff.utils;

import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import ru.mine.banoff.BSalary;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class DBUtil implements Listener {

    private final BSalary plugin;
    private final File file;
    private final File db;
    private final String url;
    private Connection connection;

    public DBUtil(BSalary plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder();
        this.db = new File(file, "database.db");
        this.url = "jdbc:sqlite:" + db.getPath();
    }

    public String getUrl() {
        return url;
    }

    @SneakyThrows
    public void initDB() {
        if (!file.exists()) file.mkdirs();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "player_name TEXT NOT NULL," +
                            "cooldown INTEGER" +
                            ");"
            );
        }
    }

    @EventHandler
    public CompletableFuture<Void> onJoin(@NotNull PlayerJoinEvent e) {
        Player player = e.getPlayer();
        return CompletableFuture.runAsync(() -> {
            try (Connection connect = DriverManager.getConnection(url)) {
                try (PreparedStatement pstmt = connect.prepareStatement("SELECT player_name FROM players WHERE player_name = ? LIMIT 1")) {
                    pstmt.setString(1, player.getName());

                    try (ResultSet resultSet = pstmt.executeQuery()) {
                        if (!resultSet.next()) {
                            String request = "INSERT INTO players (player_name) VALUES (?)";
                            try (PreparedStatement pstrq = connect.prepareStatement(request)) {
                                pstrq.setString(1, player.getName());
                                pstrq.executeUpdate();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }


}
