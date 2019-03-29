import java.sql.*;
import java.time.Duration;
import java.time.Instant;

public class Database {
    private static Connection connection = null;
    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";


    private void getDatabaseConnection() {
        try {
            Class.forName(DRIVER).newInstance();
            connection = DriverManager.getConnection("jdbc:derby:zoo;create=true", "username", "password");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            System.out.println("Could not locate driver");
        } catch (SQLException sql) {
            System.out.println("Problem starting database");
        }
    }

    /**
     * Sets rowLocking, which is required for this issue to occur
     * Sets timeout to 15 seconds for testing purposes (otherwise default is 60 secs)
     * Setting derby.locks.deadlockTimeout has no effect
     */
    private void setDatabaseConfig() {
        try {
            Statement statement = connection.createStatement();

            /*
             * This line needs to be set in order for the bug to be present
             */
            statement.execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.storage.rowLocking', 'false')");

            /*
             * This line does not seem to have an effect
             * Ran it with waitTimeout set to 60 seconds and still had no effect
             */
            statement.execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.deadlockTimeout', '5')");

            statement.execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '15')");
            statement.close();
        } catch (SQLException sql) {
            System.out.println("Problem configuring database");
        }
    }

    public static void helloWorld() {
        System.out.println("Hello World");
    }

    private void initializeDatabase() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE ZOOANIMALS (" +
                    "Name VARCHAR(255), " +
                    "Amount INT, " +
                    "Feed BOOLEAN, " +
                    "Zoo VARCHAR(255)" +
                    ")");
            statement.close();
        } catch (SQLException sql) {
            System.out.println("Problem initializing database");
        }
    }

    /**
     * This method initializes the trigger which causes the issue
     */
    private void setDatabaseTriggers() {
        try {
            Statement statement = connection.createStatement();

            try {
                statement.execute("DROP PROCEDURE helloWorld");
            } catch(SQLException sql) {/*Ignore*/}
            statement.execute("CREATE PROCEDURE helloWorld() " +
                    "LANGUAGE JAVA " +
                    "EXTERNAL NAME 'Database.helloWorld' " +
                    "PARAMETER STYLE JAVA " +
                    "NO SQL");

            /*
             * Update trigger code block
             */
            try {
                statement.execute("DROP TRIGGER ZooUpdateTrigger");
            } catch(SQLException sql) {/*Ignore*/}
            statement.execute("CREATE TRIGGER ZooUpdateTrigger " +
                    "AFTER UPDATE ON ZOOANIMALS FOR EACH STATEMENT MODE DB2SQL " +
                    "CALL helloWorld()");

            /*
             * Insert trigger code block
             */
            try {
                statement.execute("DROP TRIGGER ZooInsertTrigger");
            } catch(SQLException sql) {/*Ignore*/}
            statement.execute("CREATE TRIGGER ZooInsertTrigger " +
                    "AFTER INSERT ON ZOOANIMALS FOR EACH STATEMENT MODE DB2SQL " +
                    "CALL helloWorld()");

            /*
             * Delete trigger code block
             */
            try {
                statement.execute("DROP TRIGGER ZooDeleteTrigger");
            } catch(SQLException sql) {/*Ignore*/}
            statement.execute("CREATE TRIGGER ZooDeleteTrigger " +
                    "AFTER DELETE ON ZOOANIMALS FOR EACH STATEMENT MODE DB2SQL " +
                    "CALL helloWorld()");
        } catch (SQLException sql) {
            System.out.println("Problem setting up triggers for database");
        }
    }

    /**
     * The calls in this function cause the issue
     */
    private void updateDatabase() {
        Instant start;
        Instant end;
        Duration timeElapsed;

        try {
            Statement statement = connection.createStatement();

            start = Instant.now();
            /*
             * This statement causes deadlock when there is a trigger on Delete
             *   Requires a trigger on Insert to occur
             */
            statement.execute("INSERT INTO ZOOANIMALS " +
                    "(Name, Amount, Feed, Zoo) " +
                    "VALUES " +
                    "('Gorilla', 1, true, 'Cincinnati')");

            end = Instant.now();
            timeElapsed = Duration.between(start, end);
            System.out.println("Insert time: " + timeElapsed.getSeconds() + " seconds");


            start = Instant.now();
            /*
             * This statement causes deadlock when there is a trigger on Insert or Delete
             *   Requires a trigger on Update to occur
             */
            statement.execute("UPDATE ZOOANIMALS SET " +
                    "Name = 'Gorilla', " +
                    "Amount = 3, " +
                    "Feed = true " +
                    "WHERE Zoo = 'Cincinnati'");

            end = Instant.now();
            timeElapsed = Duration.between(start, end);
            System.out.println("Update time: " + timeElapsed.getSeconds() + " seconds");

            statement.close();
        } catch (SQLException sql) {
            System.out.println("Problem updating database");
        }
    }

    private void printDatabase() {
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM ZOOANIMALS");

            while (result.next()) {
                System.out.println(result.getString(1) + " " +
                        result.getString(2) + " " +
                        result.getString(3) + " " +
                        result.getString(4));
            }

            statement.close();
        } catch (SQLException sql) {
            System.out.println("Problem retrieving from database");
        }
    }

    private void deleteDatabase() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("DELETE FROM ZOOANIMALS " +
                    "WHERE Name = 'Gorilla'");
            statement.execute("DROP TABLE ZOOANIMALS");
            statement.close();
        } catch (SQLException sql) {
            System.out.println("Problem deleting database");
        }
    }

    private void closeDatabase() {
        try {
            connection.close();
        } catch (SQLException sql) {
            System.out.println("Problem closing database");
        }
    }

    public static void main(String[] args) {
        Database db = new Database();
        db.getDatabaseConnection();
        db.setDatabaseConfig();
        db.initializeDatabase();
        db.setDatabaseTriggers();
        /* Should  be fine up to this point */

        // This call will cause the issue
        db.updateDatabase();

        db.printDatabase();
        db.deleteDatabase();
        db.closeDatabase();
    }
}