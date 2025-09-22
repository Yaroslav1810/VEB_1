package org.example.te;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.fastcgi.FCGIInterface;

public class MiniServer {


    private static final DateTimeFormatter RESP_DTF =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final List<Record> RESPONSES =
            Collections.synchronizedList(new ArrayList<>());
    private static final int LIMIT = 1000;

    public static void main(String[] args) throws Exception {

        FCGIInterface fcgi = new FCGIInterface();
        while(fcgi.FCGIaccept() >= 0){
            if (FCGIInterface.request == null) {
                System.err.println("FCGI: request is null (no data yet)");
                continue;
            }
            try {
                HashMap<String, String> map = new HashMap<>();
                long t0 = System.nanoTime();

                if (!"GET".equalsIgnoreCase(FCGIInterface.request.params.getProperty("REQUEST_METHOD"))) {
                    sendHtml(400,"Error: wrong method, expected GET");
                    continue;
                }
                String query = FCGIInterface.request.params.getProperty("QUERY_STRING");
                if (query == null || query.trim().isEmpty()) {
                    sendHtml(400,"params are null, expected number");
                    continue;
                }
                try {
                    String[] var = query.split("&");
                    for (String s : var) {
                        String[] pair = s.split("=", 2);
                        if (pair.length == 2) {
                            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                            String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            map.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    sendHtml(400,"Error in parsing: " + e.getMessage());
                    continue;
                }
                if (!map.containsKey("x") || !map.containsKey("y") || !map.containsKey("r")) {
                    sendHtml(400,"Wrong params, expected x, y, r");
                    continue;
                }
                try {
                    int x = Integer.parseInt(map.get("x"));
                    double y = Double.parseDouble(map.get("y").replace(",", "."));
                    int r = Integer.parseInt(map.get("r"));

                    if (!validate(x, y, r)) {
                        sendHtml(400, "Error: invalid params, wrong range of values");
                        continue;
                    }

                    boolean res = checkHit(x, y, r);
                    Record rec = new Record(x, y, r, res, RESP_DTF.format(LocalDateTime.now()), (System.nanoTime() - t0) / 1000L);
                    sendResponse(x, y, r, res, rec);
                } catch (NumberFormatException e){
                    sendHtml(400, "Error: x and r must be integers, y must be a number");

                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

    }

    private record Record(int x, double y, int r, boolean hit, String time, long execMicros) {}

    private static void addResponse(Record r) {
        synchronized (RESPONSES) {
            RESPONSES.add(r);
            for (int extra = RESPONSES.size() - LIMIT; extra > 0; extra--) {
                RESPONSES.remove(0);
            }
        }
    }

    private static void sendResponse(int x, double y, int r, boolean hit, Record rec) {
        addResponse(rec);

        StringBuilder sb = new StringBuilder();
        sb.append("<table class='result'>")
                .append("<thead><tr>")
                .append("<th>X</th><th>Y</th><th>R</th><th>Попадание</th><th>Время сервера</th><th>exec, µs</th>")
                .append("</tr></thead><tbody>");

        for (int i = RESPONSES.size() - 1; i >= 0; i--) {
            Record record = RESPONSES.get(i);
            sb.append("<tr>")
                    .append("<td>").append(record.x).append("</td>")
                    .append("<td>").append(String.format(Locale.US,"%.3f", record.y)).append("</td>")
                    .append("<td>").append(record.r).append("</td>")
                    .append("<td>").append(record.hit ? "Да" : "Нет").append("</td>")
                    .append("<td>").append(record.time).append("</td>")
                    .append("<td>").append(record.execMicros).append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table>");
        sendHtml(200, String.valueOf(sb));
    }


    private static boolean checkHit(double x,double y,double R){
        boolean rect=(x>=-R/2.0 && x<=0) && (y>=0 && y<=R);
        boolean tri =(x>=-R/2.0 && x<=0) && (y<=0) && (y>=-x - R/2.0);
        boolean circ=(x>=0 && y<=0) && (x*x + y*y <= (R*R)/4.0);
        return rect || tri || circ;
    }

    private static boolean validate(int x, double y, int r){
        return (x <=3 && x >= -5) && (y <= 5 && y>= -5) && (r <=5 && r>=1);
    }

    private static void sendHtml(int status, String html){
        try {
            byte[] body = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            System.out.print("Status: " + code(status) + "\r\n");
            System.out.print("Content-Type: text/html; charset=UTF-8\r\n");
            System.out.print("Cache-Control: no-store\r\n");
            System.out.print("Content-Length: " + body.length + "\r\n");
            System.out.print("\r\n");
            System.out.write(body, 0, body.length);
            System.out.flush();
        } catch (Exception ignore) {}
    }

    private static String code(int status){
        return switch (status){
            case 200 -> "200 OK";
            case 400 -> "400 Bad Request";
            default -> status + "ok";
        };

    }

}
