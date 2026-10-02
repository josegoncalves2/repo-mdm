<%@ page contentType="application/json; charset=UTF-8" %>
<%@ page import="java.sql.*, org.json.*" %>
<%
    String deviceIdStr = request.getParameter("deviceId");
    String fromStr = request.getParameter("from");
    String toStr = request.getParameter("to");
    String limitStr = request.getParameter("limit");

    if (deviceIdStr == null || fromStr == null || toStr == null) {
        out.print("[]");
        return;
    }

    int deviceId = Integer.parseInt(deviceIdStr);
    long fromMs = Long.parseLong(fromStr);
    long toMs = Long.parseLong(toStr);
    int limit = limitStr != null ? Integer.parseInt(limitStr) : 1000;
    if (limit > 10000) limit = 10000;

    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    JSONArray arr = new JSONArray();

    try {
        Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection("jdbc:postgresql://postgresql:5432/hmdm", "hmdm", "M4YUX9XpWS5knpJSgVAj9r1d");
        ps = conn.prepareStatement(
            "SELECT lat, lon, alt, speed, ts, recorded_at FROM device_location_history " +
            "WHERE device_id = ? AND ts >= ? AND ts <= ? ORDER BY ts ASC LIMIT ?"
        );
        ps.setInt(1, deviceId);
        ps.setTimestamp(2, new Timestamp(fromMs));
        ps.setTimestamp(3, new Timestamp(toMs));
        ps.setInt(4, limit);
        rs = ps.executeQuery();

        while (rs.next()) {
            JSONObject obj = new JSONObject();
            obj.put("lat", rs.getDouble("lat"));
            obj.put("lon", rs.getDouble("lon"));
            obj.put("alt", rs.getDouble("alt"));
            obj.put("speed", rs.getDouble("speed"));
            obj.put("ts", rs.getTimestamp("ts").getTime());
            obj.put("recorded_at", rs.getTimestamp("recorded_at").getTime());
            arr.put(obj);
        }
    } catch (Exception e) {
        // return empty array on error
    } finally {
        if (rs != null) try { rs.close(); } catch (Exception e) {}
        if (ps != null) try { ps.close(); } catch (Exception e) {}
        if (conn != null) try { conn.close(); } catch (Exception e) {}
    }

    out.print(arr.toString());
%>
