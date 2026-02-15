package bankingPractice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

public class PostgresTransactionService implements TransactionService {

    private final DataSource dataSource;

    public PostgresTransactionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void credit(Long accountNumber, double amount) {
        String sql = """
                UPDATE accounts
                SET balance = balance + ?
                WHERE account_number = ?
                """;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setLong(2, accountNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to credit account " + accountNumber, e);
        }
    }

    @Override
    public boolean debit(Long accountNumber, double amount) {
        String sql = """
                UPDATE accounts
                SET balance = balance - ?
                WHERE account_number = ? AND balance >= ?
                """;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setLong(2, accountNumber);
            ps.setDouble(3, amount);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to debit account " + accountNumber, e);
        }
    }

    @Override
    public boolean transfer(Long sourceId, Long destId, double amount) {
        String debitSql = """
                UPDATE accounts
                SET balance = balance - ?
                WHERE account_number = ? AND balance >= ?
                """;
        String creditSql = """
                UPDATE accounts
                SET balance = balance + ?
                WHERE account_number = ?
                """;

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement debit = conn.prepareStatement(debitSql)) {
                debit.setDouble(1, amount);
                debit.setLong(2, sourceId);
                debit.setDouble(3, amount);

                if (debit.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement credit = conn.prepareStatement(creditSql)) {
                credit.setDouble(1, amount);
                credit.setLong(2, destId);
                if (credit.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // ignore rollback failure
                }
            }
            throw new RuntimeException("Failed to transfer " + amount + " from " + sourceId + " to " + destId, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                    // ignore close failure
                }
            }
        }
    }
}
