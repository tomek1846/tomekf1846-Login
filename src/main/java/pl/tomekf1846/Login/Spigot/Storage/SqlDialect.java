package pl.tomekf1846.Login.Spigot.Storage;

public enum SqlDialect {
    MYSQL("`", "`"),
    MARIADB("`", "`"),
    POSTGRESQL("\"", "\""),
    H2("\"", "\"");

    private final String quoteStart;
    private final String quoteEnd;

    SqlDialect(String quoteStart, String quoteEnd) {
        this.quoteStart = quoteStart;
        this.quoteEnd = quoteEnd;
    }

    public String quote(String identifier) {
        return quoteStart + identifier + quoteEnd;
    }
}
