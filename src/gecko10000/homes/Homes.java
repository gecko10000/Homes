package gecko10000.homes;

import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.sql.SQLHelper;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;

public class Homes extends JavaPlugin {

    protected SQLHelper sql;

    public void setupDatabase() {
        File dataFolder = this.getDataFolder();
        dataFolder.mkdir();
        Connection connection = SQLHelper.openSQLite(dataFolder.toPath().resolve("homes.db"));
        sql = new SQLHelper(connection);
        sql.execute("CREATE TABLE IF NOT EXISTS homes (" +
                "world TEXT," +
                "x DOUBLE," +
                "y DOUBLE," +
                "z DOUBLE," +
                "uuid TEXT," +
                "name TEXT);");
        sql.setCommitInterval(20 * 60 * 5);
    }

    public void onEnable() {
        setupDatabase();
        new CommandHandler(this);
    }

    public void onDisable() {
        sql.close();
    }

}
