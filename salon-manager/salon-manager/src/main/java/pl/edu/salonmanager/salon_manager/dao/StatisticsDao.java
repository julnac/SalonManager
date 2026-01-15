package pl.edu.salonmanager.salon_manager.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.dto.statistics.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StatisticsDao {

    private final JdbcTemplate jdbcTemplate;

    public List<ClientStatisticsDto> getClientStatistics() {
        String sql = """
            SELECT
                u.id as client_id,
                CONCAT(u.first_name, ' ', u.last_name) as client_name,
                u.email as client_email,
                COUNT(r.id) as total_visits,
                COALESCE(CAST(AVG(EXTRACT(EPOCH FROM (r.end_time - r.start_time)) / 60) AS INTEGER), 0) as average_duration_minutes,
                COALESCE(AVG(r.total_price), 0) as average_spending,
                COALESCE(SUM(r.total_price), 0) as total_spending
            FROM users u
            LEFT JOIN reservations r ON u.id = r.user_id
                AND r.status IN ('APPROVED_BY_SALON', 'CONFIRMED_BY_CLIENT')
            GROUP BY u.id, u.first_name, u.last_name, u.email
            HAVING COUNT(r.id) > 0
            ORDER BY total_spending DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer avgDuration = rs.getInt("average_duration_minutes");
            if (rs.wasNull()) {
                avgDuration = 0;
            }

            return new ClientStatisticsDto(
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getString("client_email"),
                rs.getLong("total_visits"),
                avgDuration,
                rs.getBigDecimal("average_spending"),
                rs.getBigDecimal("total_spending")
            );
        });
    }

    public Optional<ClientStatisticsDto> getClientStatisticsById(Long userId) {
        String sql = """
            SELECT
                u.id as client_id,
                CONCAT(u.first_name, ' ', u.last_name) as client_name,
                u.email as client_email,
                COUNT(r.id) as total_visits,
                COALESCE(CAST(AVG(EXTRACT(EPOCH FROM (r.end_time - r.start_time)) / 60) AS INTEGER), 0) as average_duration_minutes,
                COALESCE(AVG(r.total_price), 0) as average_spending,
                COALESCE(SUM(r.total_price), 0) as total_spending
            FROM users u
            LEFT JOIN reservations r ON u.id = r.user_id
                AND r.status IN ('APPROVED_BY_SALON', 'CONFIRMED_BY_CLIENT')
            WHERE u.id = ?
            GROUP BY u.id, u.first_name, u.last_name, u.email
            HAVING COUNT(r.id) > 0
            """;

        List<ClientStatisticsDto> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer avgDuration = rs.getInt("average_duration_minutes");
            if (rs.wasNull()) {
                avgDuration = 0;
            }

            return new ClientStatisticsDto(
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getString("client_email"),
                rs.getLong("total_visits"),
                avgDuration,
                rs.getBigDecimal("average_spending"),
                rs.getBigDecimal("total_spending")
            );
        }, userId);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }


    public int cancelOldPendingReservations(LocalDateTime cutoffDate) {
        String sql = """
            UPDATE reservations
            SET status = 'CANCELLED'
            WHERE status = 'CREATED'
                AND start_time < ?
            """;

        return jdbcTemplate.update(sql, cutoffDate);
    }
}
