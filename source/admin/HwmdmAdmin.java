import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;

public class HwmdmAdmin {

    static final String ROOT = System.getenv("HWMDM_ROOT");
    static final String STACK_DIR = ROOT + "/source";
    static final String ENV_FILE = STACK_DIR + "/.env";
    static final String PROJECT = env("COMPOSE_PROJECT_NAME", "hwmdm");
    static final String SELF_SERVICE = "admin";
    static final int PORT = Integer.parseInt(env("ADMIN_PORT", "8090"));
    static final String PASS_SALT = env("HMDM_PASS_SALT", "5YdSYHyg2U");
    static final Set<Integer> ADMIN_ROLES = parseRoles(env("ADMIN_ROLE_IDS", "1,2"));
    static final int SESSION_TTL = Integer.parseInt(env("ADMIN_SESSION_TTL", "28800"));
    static final String MDM_INTERNAL_URL = env("MDM_INTERNAL_URL", "http://hmdm:8080").replaceAll("/+$", "");
    static final String JOBS_FILE = env("ADMIN_JOBS_FILE", "/data/jobs.json");

    static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    static final List<Map<String, String>> JOBS = Collections.synchronizedList(new ArrayList<>());

    static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("page_title", "Gestão do servidor"),
        Map.entry("tab_config", "Configuração"),
        Map.entry("tab_containers", "Containers"),
        Map.entry("tab_jobs", "Execuções"),
        Map.entry("tab_backup", "Backup"),
        Map.entry("btn_save", "Salvar"),
        Map.entry("btn_save_apply", "Salvar e aplicar (recria os containers)"),
        Map.entry("btn_restart", "Reiniciar"),
        Map.entry("btn_recreate", "Recriar"),
        Map.entry("btn_stop", "Parar"),
        Map.entry("btn_up", "Subir"),
        Map.entry("btn_build", "Reconstruir"),
        Map.entry("btn_restart_stack", "Reiniciar a stack"),
        Map.entry("btn_down_stack", "Derrubar e subir a stack"),
        Map.entry("btn_build_stack", "Reconstruir e subir a stack"),
        Map.entry("btn_login", "Entrar"),
        Map.entry("btn_back", "Voltar"),
        Map.entry("btn_open_backup", "Abrir o Backup do MDM"),
        Map.entry("lbl_self", "esta gestão"),
        Map.entry("lbl_loading", "carregando..."),
        Map.entry("lbl_no_jobs", "Nenhuma execução."),
        Map.entry("lbl_login_hint", "Mesmo usuário e senha do painel MDM (perfil Admin ou Super-Admin)."),
        Map.entry("lbl_user", "Usuário"),
        Map.entry("lbl_pass", "Senha"),
        Map.entry("lbl_login_fail", "Usuário ou senha inválidos, ou sem perfil de administrador."),
        Map.entry("lbl_active_server", "Servidor ativo"),
        Map.entry("lbl_block_hint", "página de bloqueio do Web Filter e links públicos usam este endereço."),
        Map.entry("lbl_not_defined", "(não definido)"),
        Map.entry("lbl_backup_desc", "O backup do banco e dos arquivos é o módulo que já existe no painel MDM (criar, agendar, baixar, enviar, restaurar). Os arquivos ficam em <code>source/volumes/backups</code>, fora dos containers.")
    );

    static final String[][] TABS = {
        {"config", LABELS.get("tab_config")},
        {"containers", LABELS.get("tab_containers")},
        {"jobs", LABELS.get("tab_jobs")},
        {"backup", LABELS.get("tab_backup")}
    };

    static final String[][] FIELD_GROUPS = {
        {"Servidor",
            "SERVER_MODE|Modo do servidor|dev = SERVIDOR-DEV (desenvolvimento/homologação); prd = SERVIDOR-PRD (produção)",
            "SERVER_DEV_URL|Endereço do SERVIDOR-DEV|Ex.: http://mdm.pmeto.local",
            "SERVER_PRD_URL|Endereço do SERVIDOR-PRD|Ex.: https://mdm.olimpia.sp.gov.br",
            "BASE_DOMAIN|Domínio/host do MDM|Gravado no QR de matrícula; errado aqui = nenhum tablet matricula",
            "LOCAL_IP|IP do servidor|IP pelo qual os tablets alcançam esta máquina",
            "PROTOCOL|Protocolo interno|http ou https",
            "PUBLIC_PROTOCOL|Protocolo público|https quando há proxy TLS na frente",
            "PROXY_ADDRESSES|Proxies confiáveis|IPs separados por vírgula; vazio se não há proxy",
            "TZ|Fuso horário|Ex.: America/Sao_Paulo"},
        {"DNS / Web Filter",
            "WEBFILTER_BIND_ADDR|Endereço do DNS do filtro|Vazio = IP do servidor",
            "WEBFILTER_CERTS_DIR|Pasta dos certificados DoT|cert.pem e key.pem"},
        {"Portas",
            "MDM_HTTP_PORT|Porta HTTP do painel|",
            "MDM_PUBLIC_PORT|Porta pública (sem :porta na URL)|",
            "MDM_PUSH_PORT|Porta do push|",
            "POSTGRES_PORT|Porta do PostgreSQL no host|",
            "ADMIN_PORT|Porta desta gestão|"},
        {"Banco de dados",
            "SQL_USER|Usuário|",
            "SQL_BASE|Base|",
            "SQL_PASS|Senha|"},
        {"MDM",
            "ADMIN_EMAIL|E-mail do administrador|",
            "SHARED_SECRET|Segredo compartilhado|Mantenha o mesmo ao restaurar um banco",
            "CLIENT_VERSION|Versão do launcher|",
            "HMDM_VARIANT|Variante|",
            "HMDM_URL|URL do WAR|Vazio = WAR do repositório",
            "DOWNLOAD_CREDENTIALS|Credenciais de download|",
            "FORCE_RECONFIGURE|Reconfigurar no próximo boot|true/false",
            "APK_SUFIXO|Sufixo dos APKs publicados|"}
    };

    static final Set<String> SECRET_KEYS = Set.of("SQL_PASS", "SHARED_SECRET", "DOWNLOAD_CREDENTIALS");
    static final Map<String, String> SETTINGS_KEYS = Map.of(
        "SERVER_MODE", "server.mode",
        "SERVER_DEV_URL", "server.dev.url",
        "SERVER_PRD_URL", "server.prd.url"
    );

    static final String STYLE =
        ":root{--bg:#f4f6f9;--card:#fff;--fg:#1f2933;--muted:#616e7c;--line:#d9e2ec;--accent:#0d9488;--ok:#1a7f37;--bad:#cf222e}" +
        "@media (prefers-color-scheme:dark){:root{--bg:#0d1117;--card:#161b22;--fg:#e6edf3;--muted:#8b949e;--line:#30363d;--accent:#2dd4bf;--ok:#3fb950;--bad:#f85149}}" +
        "*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:14px/1.45 system-ui,sans-serif}" +
        "main{width:100%;padding:16px 24px}h1{font-size:20px;margin:0 0 12px}h2{font-size:16px;margin:0 0 10px}" +
        ".card{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:16px;margin-bottom:16px}" +
        "nav{display:flex;gap:4px;flex-wrap:wrap;margin-bottom:16px}nav a{padding:8px 14px;border-radius:6px;color:var(--fg);text-decoration:none;border:1px solid var(--line);background:var(--card)}" +
        "nav a.on{background:var(--accent);color:#fff;border-color:var(--accent)}" +
        ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:12px}" +
        "label{display:block;font-weight:600;margin-bottom:4px}small{color:var(--muted);display:block;margin-top:2px}" +
        "input,select{width:100%;padding:8px;border:1px solid var(--line);border-radius:6px;background:var(--bg);color:var(--fg)}" +
        "button,.btn{padding:8px 14px;border:0;border-radius:6px;background:var(--accent);color:#fff;cursor:pointer;text-decoration:none;display:inline-block}" +
        "button.sec{background:transparent;color:var(--fg);border:1px solid var(--line)}" +
        "table{width:100%;border-collapse:collapse}td,th{padding:8px;border-bottom:1px solid var(--line);text-align:left;vertical-align:top}" +
        ".ok{color:var(--ok)}.bad{color:var(--bad)}pre{white-space:pre-wrap;background:var(--bg);padding:10px;border-radius:6px;max-height:420px;overflow:auto}" +
        ".row{display:flex;gap:8px;flex-wrap:wrap;align-items:center}";

    // ================================================================ helpers

    static String env(String key, String def) {
        String v = System.getenv(key);
        return v != null ? v : def;
    }

    static Set<Integer> parseRoles(String s) {
        Set<Integer> r = new HashSet<>();
        for (String p : s.split(",")) r.add(Integer.parseInt(p.trim()));
        return r;
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte v : b) sb.append(String.format("%02x", v & 0xff));
        return sb.toString();
    }

    static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return hex(b);
    }

    static String randomUrlSafe(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String now() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
    }

    // ================================================================ .env

    static Map<String, String> readEnv() {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(Path.of(ENV_FILE))) {
                var m = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$").matcher(line);
                if (m.matches()) values.put(m.group(1), m.group(2));
            }
        } catch (IOException ignored) {}
        return values;
    }

    static List<String> readEnvLines() {
        try { return new ArrayList<>(Files.readAllLines(Path.of(ENV_FILE))); }
        catch (IOException e) { return new ArrayList<>(); }
    }

    static void writeEnv(Map<String, String> changes) throws IOException {
        List<String> lines = readEnvLines();
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : lines) {
            var m = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)=").matcher(line);
            if (m.find() && changes.containsKey(m.group(1))) {
                out.add(m.group(1) + "=" + changes.get(m.group(1)));
                seen.add(m.group(1));
            } else {
                out.add(line);
            }
        }
        for (var e : changes.entrySet()) {
            if (!seen.contains(e.getKey())) out.add(e.getKey() + "=" + e.getValue());
        }
        Path tmp = Path.of(ENV_FILE + ".tmp");
        Files.writeString(tmp, String.join("\n", out) + "\n");
        Path envPath = Path.of(ENV_FILE);
        if (Files.exists(envPath)) Files.move(envPath, Path.of(ENV_FILE + ".anterior"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, envPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // ================================================================ banco

    static Connection db() throws SQLException {
        var envVals = readEnv();
        String host = env("SQL_HOST", "postgresql");
        int port = Integer.parseInt(env("SQL_PORT", "5432"));
        String base = envVals.getOrDefault("SQL_BASE", env("SQL_BASE", "hmdm"));
        String user = envVals.getOrDefault("SQL_USER", env("SQL_USER", "hmdm"));
        String pass = envVals.getOrDefault("SQL_PASS", env("SQL_PASS", "hmdm"));
        return DriverManager.getConnection(
            "jdbc:postgresql://" + host + ":" + port + "/" + base + "?connectTimeout=5", user, pass);
    }

    static void ensureSchema() throws SQLException {
        try (var c = db(); var st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS hwmdm_system_settings (" +
                "key VARCHAR(100) PRIMARY KEY, value TEXT, updated TIMESTAMP DEFAULT now())");
        }
    }

    static void syncSettings(Map<String, String> envVals) {
        try (var c = db(); var ps = c.prepareStatement(
                "INSERT INTO hwmdm_system_settings(key, value, updated) VALUES (?, ?, now()) " +
                "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated = now()")) {
            for (var e : SETTINGS_KEYS.entrySet()) {
                if (envVals.containsKey(e.getKey())) {
                    ps.setString(1, e.getValue());
                    ps.setString(2, envVals.get(e.getKey()));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ignored) {}
    }

    static String mdmSessionUser(String jsessionid) {
        if (jsessionid == null || jsessionid.isBlank()) return null;
        try {
            var url = new URI(MDM_INTERNAL_URL + "/rest/private/users/current").toURL();
            var conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Cookie", "JSESSIONID=" + jsessionid);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != 200) return null;
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // minimal JSON parsing for {"data":{"login":"...","userRole":{"id":N}}}
            var loginM = Pattern.compile("\"login\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            var roleM = Pattern.compile("\"userRole\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(\\d+)").matcher(body);
            if (loginM.find() && roleM.find()) {
                String login = loginM.group(1);
                int roleId = Integer.parseInt(roleM.group(1));
                return ADMIN_ROLES.contains(roleId) ? login : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    static boolean checkLogin(String login, String password) {
        try {
            String md5 = hex(MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8))).toUpperCase();
            String digest = hex(MessageDigest.getInstance("SHA-1").digest((md5 + PASS_SALT).getBytes(StandardCharsets.UTF_8)));
            try (var c = db(); var ps = c.prepareStatement("SELECT password, userroleid FROM users WHERE login = ?")) {
                ps.setString(1, login);
                var rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString(1).equalsIgnoreCase(digest) && ADMIN_ROLES.contains(rs.getInt(2));
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ================================================================ docker

    static String[] composeCmd(String... args) {
        var cmd = new ArrayList<>(List.of("docker", "compose", "-p", PROJECT,
            "--project-directory", STACK_DIR, "-f", STACK_DIR + "/docker-compose.yaml", "--env-file", ENV_FILE));
        cmd.addAll(List.of(args));
        return cmd.toArray(new String[0]);
    }

    static String[] compose(String... args) {
        try {
            var p = new ProcessBuilder(composeCmd(args)).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean ok = p.waitFor(900, TimeUnit.SECONDS);
            int rc = ok ? p.exitValue() : -1;
            return new String[]{String.valueOf(rc), out.trim()};
        } catch (Exception e) {
            return new String[]{"-1", e.getMessage()};
        }
    }

    static List<String> services() {
        var r = compose("config", "--services");
        if (!"0".equals(r[0])) return List.of();
        return Arrays.stream(r[1].split("\n"))
            .filter(s -> !s.isBlank() && !s.equals(SELF_SERVICE))
            .collect(Collectors.toList());
    }

    static List<Map<String, String>> status() {
        var r = compose("ps", "-a", "--format", "json");
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : r[1].split("\n")) {
            line = line.trim();
            if (line.startsWith("{")) {
                parseJsonRow(line, rows);
            } else if (line.startsWith("[")) {
                for (String item : splitJsonArray(line)) parseJsonRow(item, rows);
            }
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    static void parseJsonRow(String json, List<Map<String, String>> rows) {
        Map<String, String> m = new LinkedHashMap<>();
        var matcher = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        while (matcher.find()) m.put(matcher.group(1), matcher.group(2));
        if (!m.isEmpty()) rows.add(m);
    }

    static List<String> splitJsonArray(String arr) {
        List<String> items = new ArrayList<>();
        int depth = 0; int start = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) items.add(arr.substring(start, i + 1)); }
        }
        return items;
    }

    static void saveJobs() {
        try {
            Files.createDirectories(Path.of(JOBS_FILE).getParent());
            var sb = new StringBuilder("[");
            synchronized (JOBS) {
                for (int i = 0; i < JOBS.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(jobToJson(JOBS.get(i)));
                }
            }
            sb.append("]");
            Files.writeString(Path.of(JOBS_FILE + ".tmp"), sb.toString());
            Files.move(Path.of(JOBS_FILE + ".tmp"), Path.of(JOBS_FILE), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("nao foi possivel gravar o historico de execucoes: " + e);
        }
    }

    static String jobToJson(Map<String, String> job) {
        var sb = new StringBuilder("{");
        int i = 0;
        for (var e : job.entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"")
              .append(e.getValue().replace("\\", "\\\\").replace("\"", "\\\"")
                      .replace("\n", "\\n").replace("\r", "\\r")).append("\"");
        }
        return sb.append("}").toString();
    }

    static Map<String, String> runJob(String title, String[]... commands) {
        var job = new LinkedHashMap<String, String>();
        job.put("id", randomHex(4));
        job.put("title", title);
        job.put("started", now());
        job.put("state", "rodando");
        job.put("output", "");
        synchronized (JOBS) {
            JOBS.add(0, job);
            while (JOBS.size() > 30) JOBS.remove(JOBS.size() - 1);
        }
        saveJobs();
        Thread.startVirtualThread(() -> {
            boolean ok = true;
            for (String[] args : commands) {
                var r = compose(args);
                job.put("output", job.get("output") + "$ docker compose " + String.join(" ", args) + "\n" + r[1] + "\n\n");
                if (!"0".equals(r[0])) { ok = false; break; }
            }
            job.put("state", ok ? "ok" : "falhou");
            job.put("finished", now());
            saveJobs();
        });
        return job;
    }

    // ================================================================ HTTP

    static String page(String title, String body, String tab) {
        var nav = new StringBuilder("<nav>");
        for (String[] t : TABS) {
            nav.append("<a href=\"/").append(t[0]).append("\" class=\"")
               .append(t[0].equals(tab) ? "on" : "").append("\">").append(esc(t[1])).append("</a>");
        }
        nav.append("</nav>");
        return "<!doctype html><html lang=pt-BR><head><meta charset=utf-8>" +
            "<meta name=viewport content='width=device-width,initial-scale=1'><title>" + esc(title) + "</title><style>" + STYLE + "</style>" +
            "</head><body><main><h1>" + esc(LABELS.get("page_title")) + "</h1>" +
            (tab.isEmpty() ? "" : nav.toString()) + body + "</main></body></html>";
    }

    static Map<String, String> parseForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                form.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                         URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    static String getCookie(HttpExchange ex, String name) {
        String header = ex.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].trim().equals(name)) return kv[1].trim();
        }
        return null;
    }

    static void respond(HttpExchange ex, int code, String body, String ctype, Map<String, String> headers) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", ctype);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        if (headers != null) headers.forEach((k, v) -> ex.getResponseHeaders().set(k, v));
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    static void respond(HttpExchange ex, int code, String body) throws IOException {
        respond(ex, code, body, "text/html; charset=utf-8", null);
    }

    static void redirect(HttpExchange ex, String to, Map<String, String> headers) throws IOException {
        var h = new LinkedHashMap<String, String>();
        h.put("Location", to);
        if (headers != null) h.putAll(headers);
        respond(ex, 303, "", "text/html; charset=utf-8", h);
    }

    static Session getSession(HttpExchange ex) {
        String sid = getCookie(ex, "hwmdm_admin");
        Session s = sid != null ? SESSIONS.get(sid) : null;
        if (s != null && s.exp > System.currentTimeMillis() / 1000) return s;
        String jsessionid = getCookie(ex, "JSESSIONID");
        String login = mdmSessionUser(jsessionid);
        if (login != null) {
            String newSid = randomUrlSafe(32);
            s = new Session(login, System.currentTimeMillis() / 1000 + SESSION_TTL, randomHex(16));
            SESSIONS.put(newSid, s);
            s.newCookie = "hwmdm_admin=" + newSid + "; HttpOnly; SameSite=Lax; Path=/";
            return s;
        }
        return null;
    }

    static boolean csrfOk(Session s, Map<String, String> form) {
        String token = form.getOrDefault("csrf", "");
        return MessageDigest.isEqual(token.getBytes(), s.csrf.getBytes());
    }

    // ================================================================ pages

    static String loginPage(String error) {
        return page(LABELS.get("btn_login"),
            "<div class=card style=\"max-width:420px\"><h2>" + esc(LABELS.get("btn_login")) + "</h2>" +
            "<p><small>" + esc(LABELS.get("lbl_login_hint")) + "</small></p>" +
            (error.isEmpty() ? "" : "<p class=bad>" + esc(error) + "</p>") +
            "<form method=post action=/login><p><label>" + esc(LABELS.get("lbl_user")) + "</label><input name=login autofocus></p>" +
            "<p><label>" + esc(LABELS.get("lbl_pass")) + "</label><input name=password type=password></p>" +
            "<button>" + esc(LABELS.get("btn_login")) + "</button></form></div>", "");
    }

    static String configPage(Session s, String msg) {
        var envVals = readEnv();
        var parts = new StringBuilder();
        Set<String> allKeys = new HashSet<>();
        for (String[] group : FIELD_GROUPS) {
            var cells = new StringBuilder();
            String groupName = group[0];
            for (int i = 1; i < group.length; i++) {
                String[] f = group[i].split("\\|", 3);
                String key = f[0], label = f[1], hint = f.length > 2 ? f[2] : "";
                allKeys.add(key);
                String val = esc(envVals.getOrDefault(key, ""));
                String inp;
                if ("SERVER_MODE".equals(key)) {
                    String cur = envVals.getOrDefault(key, "dev");
                    inp = "<select name=SERVER_MODE><option value=dev " + (!"prd".equals(cur) ? "selected" : "") +
                        ">Desenvolvimento (SERVIDOR-DEV)</option>" +
                        "<option value=prd " + ("prd".equals(cur) ? "selected" : "") +
                        ">Produção (SERVIDOR-PRD)</option></select>";
                } else {
                    String t = SECRET_KEYS.contains(key) ? "password" : "text";
                    inp = "<input type=" + t + " name=" + key + " value=\"" + val + "\">";
                }
                cells.append("<div><label>").append(esc(label)).append(" <small style=\"display:inline\">")
                     .append(key).append("</small></label>").append(inp).append("<small>").append(esc(hint)).append("</small></div>");
            }
            parts.append("<div class=card><h2>").append(esc(groupName)).append("</h2><div class=grid>")
                 .append(cells).append("</div></div>");
        }
        String active = "prd".equals(envVals.get("SERVER_MODE")) ? envVals.get("SERVER_PRD_URL") : envVals.get("SERVER_DEV_URL");
        return page(LABELS.get("tab_config"),
            (msg.isEmpty() ? "" : "<div class=\"card ok\">" + esc(msg) + "</div>") +
            "<div class=card>" + esc(LABELS.get("lbl_active_server")) + ": <b>" + esc(active != null ? active : LABELS.get("lbl_not_defined")) +
            "</b> &mdash; " + esc(LABELS.get("lbl_block_hint")) + "</div>" +
            "<form method=post action=/config><input type=hidden name=csrf value=\"" + s.csrf + "\">" +
            parts +
            "<div class=row><button name=apply value=0>" + esc(LABELS.get("btn_save")) + "</button>" +
            "<button name=apply value=1>" + esc(LABELS.get("btn_save_apply")) + "</button></div></form>",
            "config");
    }

    static String containersPage(Session s) {
        var rows = new StringBuilder();
        for (var r : status()) {
            String svc = r.getOrDefault("Service", "");
            String state = r.getOrDefault("State", "");
            String health = r.getOrDefault("Health", "");
            String cls = "running".equals(state) && (health.isEmpty() || "healthy".equals(health)) ? "ok" : "bad";
            String acts;
            if (SELF_SERVICE.equals(svc)) {
                acts = "<small>" + esc(LABELS.get("lbl_self")) + "</small>";
            } else {
                var ab = new StringBuilder();
                for (String[] pair : new String[][]{
                    {"restart", LABELS.get("btn_restart")}, {"recreate", LABELS.get("btn_recreate")},
                    {"stop", LABELS.get("btn_stop")}, {"up", LABELS.get("btn_up")}, {"build", LABELS.get("btn_build")}}) {
                    ab.append("<form method=post action=/action style=\"display:inline\"><input type=hidden name=csrf value=\"")
                      .append(s.csrf).append("\"><input type=hidden name=service value=\"").append(esc(svc))
                      .append("\"><button class=sec name=act value=").append(pair[0]).append(">").append(esc(pair[1]))
                      .append("</button></form> ");
                }
                acts = ab.toString();
            }
            rows.append("<tr><td><b>").append(esc(r.getOrDefault("Name", ""))).append("</b><br><small>")
                .append(esc(r.getOrDefault("Image", ""))).append("</small></td><td class=").append(cls).append(">")
                .append(esc(state)).append(" ").append(esc(health)).append("</td><td>")
                .append(esc(r.getOrDefault("Status", ""))).append("</td><td><a href=\"/logs?s=")
                .append(URLEncoder.encode(svc, StandardCharsets.UTF_8)).append("\">logs</a></td><td>").append(acts).append("</td></tr>");
        }
        var stack = new StringBuilder();
        for (String[] pair : new String[][]{
            {"restart", LABELS.get("btn_restart_stack")}, {"down", LABELS.get("btn_down_stack")}, {"build", LABELS.get("btn_build_stack")}}) {
            stack.append("<form method=post action=/action style=\"display:inline\"><input type=hidden name=csrf value=\"")
                 .append(s.csrf).append("\"><input type=hidden name=service value=\"*\"><button name=act value=")
                 .append(pair[0]).append(">").append(esc(pair[1])).append("</button></form> ");
        }
        return page(LABELS.get("tab_containers"),
            "<div class=card><h2>Stack " + esc(PROJECT) + "</h2><div class=row>" + stack + "</div></div>" +
            "<div class=card style=\"overflow-x:auto\"><table><tr><th>Container</th><th>Estado</th><th>Status</th>" +
            "<th></th><th>Ações</th></tr>" + rows + "</table></div>", "containers");
    }

    static String jobsPage() {
        return page(LABELS.get("tab_jobs"),
            "<div class=card><h2>" + esc(LABELS.get("tab_jobs")) + "</h2><div id=j>" + esc(LABELS.get("lbl_loading")) + "</div></div><script>" +
            "function esc(t){return (t||'').replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]))}" +
            "async function f(){try{const r=await fetch('/jobs.json');const js=await r.json();" +
            "document.getElementById('j').innerHTML=js.length?js.map(j=>\"<h3>\"+esc(j.title)+\" &mdash; \"" +
            "+esc(j.state)+\" <small style=display:inline>\"+esc(j.started)+\"</small></h3><pre>\"+esc(j.output)+\"</pre>\").join(''):" +
            "'" + esc(LABELS.get("lbl_no_jobs")).replace("'", "\\'") + "'}catch(e){}setTimeout(f,2000)}f()</script>", "jobs");
    }

    static String backupPage() {
        var envVals = readEnv();
        String active = "prd".equals(envVals.get("SERVER_MODE")) ? envVals.get("SERVER_PRD_URL") : envVals.get("SERVER_DEV_URL");
        String baseUrl = active != null ? active.replaceAll("/+$", "") : "";
        return page(LABELS.get("tab_backup"),
            "<div class=card><h2>" + esc(LABELS.get("tab_backup")) + "</h2><p>" + LABELS.get("lbl_backup_desc") + "</p>" +
            "<a class=btn target=_top href=\"" + esc(baseUrl) + "/#/governance\">" + esc(LABELS.get("btn_open_backup")) + "</a></div>",
            "backup");
    }

    // ================================================================ routing

    static void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath().replaceAll("/+$", "");
        if (path.isEmpty()) path = "/";

        if ("/health".equals(path)) { respond(ex, 200, "ok", "text/plain", null); return; }
        if ("/login".equals(path)) { respond(ex, 200, loginPage("")); return; }

        Session s = getSession(ex);
        if (s == null) { redirect(ex, "/login", null); return; }
        var headers = s.newCookie != null ? Map.of("Set-Cookie", s.newCookie) : (Map<String, String>) null;

        if ("/".equals(path) || "/config".equals(path)) {
            respond(ex, 200, configPage(s, ""), "text/html; charset=utf-8", headers);
        } else if ("/containers".equals(path)) {
            respond(ex, 200, containersPage(s), "text/html; charset=utf-8", headers);
        } else if ("/jobs".equals(path)) {
            respond(ex, 200, jobsPage(), "text/html; charset=utf-8", headers);
        } else if ("/jobs.json".equals(path)) {
            var sb = new StringBuilder("[");
            synchronized (JOBS) {
                for (int i = 0; i < JOBS.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(jobToJson(JOBS.get(i)));
                }
            }
            sb.append("]");
            respond(ex, 200, sb.toString(), "application/json", headers);
        } else if ("/logs".equals(path)) {
            String query = ex.getRequestURI().getQuery();
            String svc = "";
            if (query != null) for (String p : query.split("&")) {
                if (p.startsWith("s=")) svc = URLDecoder.decode(p.substring(2), StandardCharsets.UTF_8);
            }
            var allowed = services();
            allowed.add(SELF_SERVICE);
            if (!allowed.contains(svc)) { respond(ex, 404, "serviço desconhecido", "text/plain", null); return; }
            var r = compose("logs", "--no-color", "--tail", "400", svc);
            respond(ex, 200, page("Logs", "<div class=card><h2>Logs: " + esc(svc) + "</h2><pre>" + esc(r[1]) + "</pre>" +
                "<a class=btn href=/containers>" + esc(LABELS.get("btn_back")) + "</a></div>", "containers"), "text/html; charset=utf-8", headers);
        } else if ("/backup".equals(path)) {
            respond(ex, 200, backupPage(), "text/html; charset=utf-8", headers);
        } else {
            respond(ex, 404, "não encontrado", "text/plain", null);
        }
    }

    static void handlePost(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath().replaceAll("/+$", "");
        var form = parseForm(ex);

        if ("/login".equals(path)) {
            String login = form.getOrDefault("login", "");
            String password = form.getOrDefault("password", "");
            boolean ok;
            try { ok = checkLogin(login, password); }
            catch (Exception e) { respond(ex, 503, loginPage("Banco indisponível: " + e.getMessage())); return; }
            if (!ok) { respond(ex, 401, loginPage(LABELS.get("lbl_login_fail"))); return; }
            String sid = randomUrlSafe(32);
            SESSIONS.put(sid, new Session(login, System.currentTimeMillis() / 1000 + SESSION_TTL, randomHex(16)));
            redirect(ex, "/config", Map.of("Set-Cookie", "hwmdm_admin=" + sid + "; HttpOnly; SameSite=Lax; Path=/"));
            return;
        }

        Session s = getSession(ex);
        if (s == null || !csrfOk(s, form)) { redirect(ex, "/login", null); return; }

        if ("/config".equals(path)) {
            Set<String> known = new HashSet<>();
            for (String[] group : FIELD_GROUPS) for (int i = 1; i < group.length; i++) known.add(group[i].split("\\|")[0]);
            Map<String, String> changes = new LinkedHashMap<>();
            for (String k : known) if (form.containsKey(k)) changes.put(k, form.get(k).replace("\n", " ").trim());
            if (!"dev".equals(changes.get("SERVER_MODE")) && !"prd".equals(changes.get("SERVER_MODE")))
                changes.put("SERVER_MODE", "dev");
            writeEnv(changes);
            syncSettings(readEnv());
            String msg = "Configuração salva.";
            if ("1".equals(form.get("apply"))) {
                var svcs = services();
                var args = new ArrayList<>(List.of("up", "-d", "--remove-orphans"));
                args.addAll(svcs);
                runJob("Aplicar configuracao", args.toArray(new String[0]));
                msg += " Aplicando nos containers (veja " + LABELS.get("tab_jobs") + ").";
            }
            var headers = s.newCookie != null ? Map.of("Set-Cookie", s.newCookie) : (Map<String, String>) null;
            respond(ex, 200, configPage(s, msg), "text/html; charset=utf-8", headers);
        } else if ("/action".equals(path)) {
            String svc = form.getOrDefault("service", "");
            String act = form.getOrDefault("act", "");
            var allowed = services();
            List<String> targets = "*".equals(svc) ? allowed : List.of(svc);
            if (!targets.stream().allMatch(allowed::contains)) {
                respond(ex, 400, "serviço inválido", "text/plain", null); return;
            }
            switch (act) {
                case "restart" -> runJob(LABELS.get("btn_restart") + " " + svc, merge("restart", targets));
                case "stop" -> runJob(LABELS.get("btn_stop") + " " + svc, merge("stop", targets));
                case "up" -> runJob(LABELS.get("btn_up") + " " + svc, merge("up", "-d", targets));
                case "recreate" -> runJob(LABELS.get("btn_recreate") + " " + svc, merge("up", "-d", "--force-recreate", targets));
                case "build" -> runJob(LABELS.get("btn_build") + " " + svc, merge("build", targets), merge("up", "-d", targets));
                case "down" -> runJob(LABELS.get("btn_down_stack"), merge("stop", targets), merge("rm", "-f", targets), merge("up", "-d", targets));
                default -> { respond(ex, 400, "ação inválida", "text/plain", null); return; }
            }
            redirect(ex, "/jobs", null);
        } else if ("/logout".equals(path)) {
            SESSIONS.values().remove(s);
            redirect(ex, "/login", null);
        } else {
            respond(ex, 404, "não encontrado", "text/plain", null);
        }
    }

    static String[] merge(String cmd, List<String> targets) {
        var list = new ArrayList<String>();
        list.add(cmd);
        list.addAll(targets);
        return list.toArray(new String[0]);
    }

    static String[] merge(String cmd, String flag, List<String> targets) {
        var list = new ArrayList<String>();
        list.add(cmd);
        list.add(flag);
        list.addAll(targets);
        return list.toArray(new String[0]);
    }

    static String[] merge(String cmd, String f1, String f2, List<String> targets) {
        var list = new ArrayList<String>();
        list.add(cmd);
        list.add(f1);
        list.add(f2);
        list.addAll(targets);
        return list.toArray(new String[0]);
    }

    // ================================================================ main

    static class Session {
        String login;
        long exp;
        String csrf;
        String newCookie;
        Session(String login, long exp, String csrf) {
            this.login = login; this.exp = exp; this.csrf = csrf;
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        for (int attempt = 0; attempt < 60; attempt++) {
            try { ensureSchema(); syncSettings(readEnv()); break; }
            catch (Exception e) {
                System.out.println("banco ainda indisponivel: " + e);
                Thread.sleep(5000);
            }
        }
        var server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 50);
        server.createContext("/", ex -> {
            try {
                if ("POST".equalsIgnoreCase(ex.getRequestMethod())) handlePost(ex);
                else handleGet(ex);
            } catch (Exception e) {
                e.printStackTrace();
                try { respond(ex, 500, "erro interno: " + e.getMessage(), "text/plain", null); }
                catch (IOException ignored) {}
            }
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("hwmdm-admin ouvindo na porta " + PORT);
        // keep-alive
        synchronized (HwmdmAdmin.class) { HwmdmAdmin.class.wait(); }
    }
}
