package org.xp.pgsql;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgsqlConnChecker {
    private static final Logger LOG = LoggerFactory.getLogger(PgsqlConnChecker.class);

    private final int sleepSeconds;
    private final String host;
    private final String db;
    private final String table;
    private final String user;
    private final String pass;

    private final String jdbcUrl;
    private final String checkSql;

    public PgsqlConnChecker(int sleepSeconds, String host, String db, String table, String user, String pass) {
        this.sleepSeconds = sleepSeconds;
        this.host = host;
        this.db = db;
        this.table = table;
        this.user = user;
        this.pass = pass;
        
        this.jdbcUrl = "jdbc:postgresql://" + host + "/" + db;
        this.checkSql = "select * from public." + table + " limit 1;";
    }

    private static void close(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void checkConn() {
        int i = 0;
        while (true) {
            ++i;
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(jdbcUrl, user, pass);

                LOG.debug(String.format("======== Connection No. %05d", i));
                LOG.debug(String.format("Host: %s", host));
                LOG.debug(String.format("DB  : %s", db));
                LOG.debug(String.format("Created Connection %s", conn));

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(checkSql);

                if (rs.next()) {
                    int id = rs.getInt("id");
                    // String name = rs.getString("name");
                    LOG.debug(String.format("id: %d", id));
                }

                TimeUnit.SECONDS.sleep(sleepSeconds);
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        String msg = conn.toString();
                        conn.close();
                        LOG.debug(String.format("Closed Connection %s", msg));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private static void printUsage() {
        // <host> <db> <user> [password]
        System.out.println("Usage:");
        System.out.println("java -cp target/pgsql.jar org.xp.pgsql.PgsqlConnChecker" +
                           " <sleepSeconds> <host> <db> <table> <user> [password]");
        System.out.println("[password]: if missing, will ask for it");
        System.out.println();
    }

    /**
     * To test connections every 1 second
     * java -cp target/pgsql.jar org.xp.pgsql.PgsqlConnChecker 1 <host> <db> <table> <user> [password]
     */
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println(args.length);
            printUsage();
            return;
        }

        int sleepSeconds = Integer.parseInt(args[0]);
        String host = args[1];
        String db = args[2];
        String table = args[3];
        String user = args[4];
        String pass = "";
        if (args.length == 6) {
            pass = args[5];
        } else {
            Console console = System.console();
            pass = new String(console.readPassword("Password: "));
        }

        PgsqlConnChecker pg = new PgsqlConnChecker(sleepSeconds, host, db, table, user, pass);
        pg.checkConn();
    }
}
