package bankingPractice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class PostgresAccountDao implements AccountDao {

    private final DataSource dataSource;

    public PostgresAccountDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(AccountDTO account) {
        String sql = """
                INSERT INTO accounts (account_number, holder_name, pin, balance)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (account_number)
                DO UPDATE SET holder_name = EXCLUDED.holder_name,
                              pin = EXCLUDED.pin,
                              balance = EXCLUDED.balance
                """;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, account.getAccountNumber());
            ps.setString(2, account.getHolderName());
            ps.setInt(3, account.getPin());
            ps.setDouble(4, account.getBalance());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save account " + account.getAccountNumber(), e);
        }
    }

    @Override
    public AccountDTO findById(Long accountNumber) {
        String sql = "SELECT account_number, holder_name, pin, balance FROM accounts WHERE account_number = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account " + accountNumber, e);
        }
    }

    @Override
    public List<AccountDTO> findAll() {
        String sql = "SELECT account_number, holder_name, pin, balance FROM accounts";
        List<AccountDTO> accounts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                accounts.add(mapRow(rs));
            }
            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch accounts", e);
        }
    }

    private AccountDTO mapRow(ResultSet rs) throws SQLException {
        Long accountNumber = rs.getLong("account_number");
        String holderName = rs.getString("holder_name");
        int pin = rs.getInt("pin");
        double balance = rs.getDouble("balance");
        return new AccountDTO(accountNumber, holderName, pin, balance);
    }
}
