package server.repository;

import model.Reason;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReasonRepository {

    // Thêm một lý do mới vào database
    public boolean addReason(Reason reason) {
        String sql = "INSERT INTO reasons (target_id, reason_type, reason_text) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, reason.getTargetId());
            pstmt.setString(2, reason.getReasonType());
            pstmt.setString(3, reason.getReason());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lấy danh sách lý do theo ID đối tượng và loại (ví dụ: lấy tất cả lý do từ chối của Item ID 1)
    public List<Reason> getReasonsByTargetIdAndType(int targetId, String reasonType) {
        List<Reason> reasons = new ArrayList<>();
        String sql = "SELECT * FROM reasons WHERE target_id = ? AND reason_type = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, targetId);
            pstmt.setString(2, reasonType);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Reason r = new Reason();
                    r.setId(rs.getInt("id"));
                    r.setTargetId(rs.getInt("target_id"));
                    r.setReasonType(rs.getString("reason_type"));
                    r.setReason(rs.getString("reason_text"));
                    reasons.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reasons;
    }

    // Lấy lý do mới nhất cho một đối tượng (ví dụ: lấy lý do từ chối gần nhất)
    public Reason getLatestReason(int targetId, String reasonType) {
        String sql = "SELECT * FROM reasons WHERE target_id = ? AND reason_type = ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, targetId);
            pstmt.setString(2, reasonType);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Reason r = new Reason();
                    r.setId(rs.getInt("id"));
                    r.setTargetId(rs.getInt("target_id"));
                    r.setReasonType(rs.getString("reason_type"));
                    r.setReason(rs.getString("reason_text"));
                    return r;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
