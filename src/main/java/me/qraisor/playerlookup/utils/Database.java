package me.qraisor.playerlookup.utils;

import me.qraisor.playerlookup.PlayerLookup;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;


import java.util.logging.Level;
import java.util.logging.Logger;



public class Database {
    private static final PlayerLookup plrLookup = PlayerLookup.getInstance();

    @Getter
    private static final HikariDataSource dataSource;

    static {
        dataSource = setupDatabase();
    }

    private static final Logger mcLogger = Logger.getLogger("Minecraft");

    private static HikariDataSource setupDatabase() {
        final FileConfiguration config = plrLookup.getConfig();
        final HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        final String host = config.getString("mysql.host", "");
        final int port = config.getInt("mysql.port", 3306);
        final String database = config.getString("mysql.database", "");
        final String url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(config.getString("mysql.username", ""));
        dataSource.setPassword(config.getString("mysql.password", ""));
        dataSource.addDataSourceProperty("autoReconnect", "true");
        dataSource.addDataSourceProperty("autoReconnectForPools", "true");
        dataSource.addDataSourceProperty("interactiveClient", "true");
        dataSource.addDataSourceProperty("characterEncoding", "UTF-8");
        if (!config.getBoolean("mysql.ssl", true))
            dataSource.addDataSourceProperty("useSSL", "false");
        return dataSource;
    }

    public static boolean establishConn() {
        try (Connection connection = getDataSource().getConnection()) {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS users(" +
                            "`id` INT(11) NOT NULL AUTO_INCREMENT," +
                            "`username` VARCHAR(255) NOT NULL," +
                            "`uuid` VARCHAR(255) NOT NULL," +
                            "`first_join` INT(255) NOT NULL," +
                            "`last_join` INT(255) NOT NULL," +
                            "PRIMARY KEY (`id`))"
            ).execute();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean playerExists(UUID uuid, String username) {
        plrLookup.log("[PlayerLookup] (Debug) exists "+username);
        try (Connection connection = getDataSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM `users` WHERE `uuid` = ? AND `username` = ?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            return stmt.executeQuery().next();
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean newPlayer(UUID uuid, String username) {
        plrLookup.log("[PlayerLookup] (Debug) New "+username);

        if (Database.playerExists(uuid, username)) {
            plrLookup.log( "[PlayerLookup] (Debug) New -) Does exist "+username);
            return updatePlayer(uuid, username);
        }
        try (Connection connection = getDataSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO `users`(`uuid`, `username`,`first_join`,`last_join`) VALUES (?, ?, ?, ?)"
            );
            int unixTime = Math.toIntExact(System.currentTimeMillis() / 1000L);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setInt(3, unixTime);
            stmt.setInt(4, unixTime);
            stmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    public static boolean updatePlayer(UUID uuid, String username) {
        plrLookup.log("[PlayerLookup] (Debug) Upd "+username);

        if (!Database.playerExists(uuid, username)) {
            plrLookup.log("[PlayerLookup] (Debug) Upd -) Does not exist "+username);
            return newPlayer(uuid, username);
        }
        try (Connection connection = getDataSource().getConnection()) {
            plrLookup.log("[PlayerLookup] (Debug) Upd FINE "+username);
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE `users` SET `last_join` = ? WHERE `uuid` = ? AND `username` = ?"
            );
            int unixTime = Math.toIntExact(System.currentTimeMillis() / 1000L);
            stmt.setInt(1, unixTime);
            stmt.setString(2, uuid.toString());
            stmt.setString(3, username);
            int count = stmt.executeUpdate();
            plrLookup.log("[PlayerLookup] (Debug) Upd UPDATE ROW "+count);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

}
