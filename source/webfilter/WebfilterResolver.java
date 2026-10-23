import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

// Supervisor do Blocky (DNS do Web Filter): semeia e atualiza as listas de categorias,
// sobe o Blocky com a configuracao gerada pelo MDM e volta para a ultima configuracao boa
// quando uma nova nao sobe. Todo valor vem do ambiente (source/.env).
public class WebfilterResolver {
    static final String DNS_DIR = env("WEBFILTER_DNS_DIR");
    static final String LISTS_DIR = env("WEBFILTER_LISTS_DIR");
    static final String STATE_DIR = env("WEBFILTER_STATE_DIR");
    static final String BLOCKY = env("BLOCKY_BINARY");
    static final String SEED_DIR = env("WEBFILTER_SEED_DIR");
    static final String LOOPBACK = env("WEBFILTER_LOOPBACK_ADDR");
    static final String API = "http://" + env("WEBFILTER_API_ADDR");
    static final int DNS_PORT = envInt("WEBFILTER_DNS_PORT");
    static final int DOT_PORT = envInt("WEBFILTER_DOT_PORT");
    static final List<String> BIND_ADDRS = splitCsv(env("WEBFILTER_BIND_ADDRS"));
    static final int REFRESH_MAX = envInt("WEBFILTER_REFRESH_MAX_SECONDS");
    static final int REFRESH_MIN = envInt("WEBFILTER_REFRESH_MIN_SECONDS");
    static final int LOOP_SECONDS = envInt("WEBFILTER_LOOP_SECONDS");
    static final int HEALTH_TIMEOUT = envInt("WEBFILTER_HEALTH_TIMEOUT");
    static final int RETRY_FIRST_SECONDS = envInt("WEBFILTER_RETRY_FIRST_SECONDS");
    static final int RETRY_MAX_SECONDS = envInt("WEBFILTER_RETRY_MAX_SECONDS");
    static final int DRY_RUN_DNS_PORT = envInt("WEBFILTER_DRY_RUN_DNS_PORT");
    static final String DRY_RUN_API_ADDR = env("WEBFILTER_DRY_RUN_API_ADDR");
    static final int RESTART_GOOD_SECONDS = envInt("WEBFILTER_RESTART_GOOD_SECONDS");
    static final int API_REFRESH_ATTEMPTS = envInt("WEBFILTER_API_REFRESH_ATTEMPTS");
    static final int API_REFRESH_BACKOFF = envInt("WEBFILTER_API_REFRESH_BACKOFF");
    static final int API_TIMEOUT = envInt("WEBFILTER_API_TIMEOUT");
    static final int DOWNLOAD_TIMEOUT = envInt("WEBFILTER_DOWNLOAD_TIMEOUT");
    static final int DNS_PROBE_TIMEOUT = envInt("WEBFILTER_DNS_PROBE_TIMEOUT");
    static final int STOP_TIMEOUT = envInt("WEBFILTER_STOP_TIMEOUT");
    static final int HEALTH_POLL_SECONDS = envInt("WEBFILTER_HEALTH_POLL_SECONDS");
    static final byte[] PROBE_QNAME = qname(env("WEBFILTER_PROBE_NAME"));
    static final Pattern LABEL = Pattern.compile("^[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?$");
    static final Pattern WHITESPACE = Pattern.compile("\\s+");
    static final String SENTINEL = "*.webfilter-sentinel.invalid";
    // HTTP/1.1 explicito: o padrao do HttpClient tenta upgrade h2c, e a API do Blocky fecha
    // a conexao sem responder ("header parser received no bytes").
    static final HttpClient HTTP = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    static String env(String key) {
        String value = System.getenv(key);
        if (value == null) {
            throw new IllegalStateException("variavel de ambiente ausente: " + key);
        }
        return value;
    }

    static int envInt(String key) {
        return Integer.parseInt(env(key).trim());
    }

    static synchronized void log(String level, String message) {
        System.out.println(LocalDateTime.now().format(LOG_TIME) + " [" + level + "] " + message);
        System.out.flush();
    }

    static void info(String message) { log("INFO", message); }

    static void warning(String message) { log("WARNING", message); }

    static void error(String message) { log("ERROR", message); }

    static List<String> splitCsv(String csv) {
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (!part.strip().isEmpty()) out.add(part.strip());
        }
        return out;
    }

    static byte[] qname(String name) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String label : name.strip().replaceAll("^\\.+|\\.+$", "").split("\\.")) {
            byte[] bytes = label.getBytes(StandardCharsets.US_ASCII);
            out.write(bytes.length);
            out.writeBytes(bytes);
        }
        out.write(0);
        return out.toByteArray();
    }

    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String sha256(String text) {
        return hex(sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    static String sha256File(Path path) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[0x100000];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        return hex(digest.digest());
    }

    static void writeAtomic(Path path, String content) throws IOException {
        Path tmp = Paths.get(path + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    static String subMulti(String text, String regex, String replacement) {
        return Pattern.compile(regex, Pattern.MULTILINE).matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }

    static double now() {
        return System.currentTimeMillis() / 1000.0;
    }

    static void sleepSeconds(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000.0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String normalize(String line) {
        int hash = line.indexOf('#');
        if (hash >= 0) line = line.substring(0, hash);
        line = line.strip().toLowerCase(Locale.ROOT);
        if (line.isEmpty()) return null;
        String[] fields = WHITESPACE.split(line);
        String host = fields[fields.length - 1];
        while (host.startsWith("*.") || host.startsWith(".")) {
            host = host.startsWith("*.") ? host.substring(2) : host.substring(1);
        }
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        String[] labels = host.split("\\.", -1);
        if (labels.length < 2 || host.length() > 253) return null;
        String tld = labels[labels.length - 1];
        if (!tld.isEmpty() && tld.chars().allMatch(Character::isDigit)) return null;
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) return null;
        }
        return "*." + host;
    }

    // Le a fonte linha a linha: listas de ~1M dominios nao cabem em heap como corpo unico + split.
    static void downloadInto(String url, TreeSet<String> domains) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(DOWNLOAD_TIMEOUT))
            .header("User-Agent", "hwmdm-source/webfilter/1.0")
            .GET().build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String domain = normalize(line);
                if (domain != null) domains.add(domain);
            }
        }
    }

    // Grava direto no disco calculando o hash no caminho; so troca o arquivo se o conteudo mudou.
    static boolean writeListIfChanged(Path target, TreeSet<String> domains) throws IOException {
        Path tmp = Paths.get(target + ".tmp");
        MessageDigest digest = sha256();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new DigestOutputStream(Files.newOutputStream(tmp), digest), StandardCharsets.UTF_8))) {
            for (String domain : domains) {
                writer.write(domain);
                writer.write('\n');
            }
        }
        String newHash = hex(digest.digest());
        if (Files.exists(target) && sha256File(target).equals(newHash)) {
            Files.delete(tmp);
            return false;
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return true;
    }

    static boolean updateCategory(String category, List<String> sources) throws Exception {
        TreeSet<String> domains = new TreeSet<>();
        for (String url : sources) downloadInto(url, domains);
        if (domains.isEmpty() && !sources.isEmpty()) {
            throw new IOException("nenhum dominio nas fontes");
        }
        if (sources.isEmpty()) domains.add(SENTINEL);
        if (!writeListIfChanged(Paths.get(LISTS_DIR, category + ".txt"), domains)) return false;
        info("lista " + category + " atualizada: " + domains.size() + " dominios");
        return true;
    }

    static List<String> referencedFiles(String config) {
        Pattern pattern = Pattern.compile("^\\s*-\\s+((?:" + Pattern.quote(DNS_DIR) + "|" + Pattern.quote(LISTS_DIR)
            + ")/\\S+\\.txt)\\s*$", Pattern.MULTILINE);
        List<String> files = new ArrayList<>();
        Matcher m = pattern.matcher(config);
        while (m.find()) files.add(m.group(1));
        return files;
    }

    static String withoutMissingCert(String config) {
        List<String> certFiles = new ArrayList<>();
        Matcher m = Pattern.compile("^(?:certFile|keyFile): (\\S+)$", Pattern.MULTILINE).matcher(config);
        while (m.find()) certFiles.add(m.group(1));
        if (certFiles.stream().allMatch(f -> Files.exists(Paths.get(f)))) return config;
        warning("certificado DoT ausente (" + String.join(", ", certFiles) + "): Blocky usa um autoassinado");
        return subMulti(config, "^(?:certFile|keyFile): .*\\n", "");
    }

    static String withBindListeners(String configPath) throws IOException {
        List<String> addrs = new ArrayList<>(List.of(LOOPBACK));
        for (String addr : BIND_ADDRS) {
            if (!addr.equals(LOOPBACK)) addrs.add(addr);
        }
        StringJoiner dns = new StringJoiner(",");
        StringJoiner dot = new StringJoiner(",");
        for (String addr : addrs) {
            dns.add(addr + ":" + DNS_PORT);
            dot.add(addr + ":" + DOT_PORT);
        }
        String config = subMulti(Files.readString(Paths.get(configPath)), "^  dns: .*$", "  dns: " + dns);
        config = withoutMissingCert(subMulti(config, "^  tls: .*$", "  tls: " + dot));
        Path runtime = Paths.get(STATE_DIR, "blocky.runtime.yml");
        Files.writeString(runtime, config);
        info("DNS em " + dns + "; DoT em " + dot);
        return runtime.toString();
    }

    static boolean dnsOk(int port) {
        ByteArrayOutputStream query = new ByteArrayOutputStream();
        query.writeBytes(new byte[]{0x12, 0x34, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0});
        query.writeBytes(PROBE_QNAME);
        query.writeBytes(new byte[]{0, 1, 0, 1});
        byte[] request = query.toByteArray();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(DNS_PROBE_TIMEOUT * 1000);
            socket.send(new DatagramPacket(request, request.length, InetAddress.getByName(LOOPBACK), port));
            byte[] reply = new byte[512];
            DatagramPacket packet = new DatagramPacket(reply, reply.length);
            socket.receive(packet);
            return packet.getLength() >= 2 && reply[0] == request[0] && reply[1] == request[1];
        } catch (IOException e) {
            return false;
        }
    }

    static boolean apiRefresh() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "/api/lists/refresh"))
            .timeout(Duration.ofSeconds(API_TIMEOUT))
            .POST(HttpRequest.BodyPublishers.noBody()).build();
        for (int attempt = 0; attempt < API_REFRESH_ATTEMPTS; attempt++) {
            try {
                HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 400) throw new IOException("HTTP " + response.statusCode());
                info("listas recarregadas sem reinicio");
                return true;
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                if (attempt + 1 == API_REFRESH_ATTEMPTS) {
                    warning("refresh falhou apos " + API_REFRESH_ATTEMPTS + " tentativas: " + e);
                    return false;
                }
                sleepSeconds(API_REFRESH_BACKOFF * Math.pow(2.0, attempt));
            }
        }
        return false;
    }

    static Process launch(String configPath) throws IOException {
        return new ProcessBuilder(BLOCKY, "--config", configPath).inheritIO().start();
    }

    static void terminate(Process proc) {
        if (proc == null || !proc.isAlive()) return;
        proc.destroy();
        try {
            if (!proc.waitFor(STOP_TIMEOUT, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                proc.waitFor();
            }
        } catch (InterruptedException e) {
            proc.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    static List<String> sortedNames(Path dir) throws IOException {
        List<String> names = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(p -> names.add(p.getFileName().toString()));
        }
        Collections.sort(names);
        return names;
    }

    static void seedLists() throws IOException {
        Path seedDir = Paths.get(SEED_DIR);
        if (!Files.isDirectory(seedDir)) return;
        for (String name : sortedNames(seedDir)) {
            if (!name.endsWith(".txt.gz")) continue;
            Path target = Paths.get(LISTS_DIR, name.substring(0, name.length() - 3));
            if (Files.exists(target)) continue;
            Path tmp = Paths.get(target + ".seed.tmp");
            try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(seedDir.resolve(name)))) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            info("lista " + name.substring(0, name.length() - 7) + " semeada da copia versionada");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--health".equals(args[0])) {
            System.exit(dnsOk(DNS_PORT) ? 0 : 1);
        }
        Files.createDirectories(Paths.get(LISTS_DIR));
        Files.createDirectories(Paths.get(STATE_DIR));
        seedLists();
        Updater updater = new Updater();
        Supervisor supervisor = new Supervisor();
        CountDownLatch stopping = new CountDownLatch(1);
        AtomicBoolean listsChanged = new AtomicBoolean();
        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopping.countDown();
            try {
                mainThread.join(TimeUnit.SECONDS.toMillis(STOP_TIMEOUT + 5L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            supervisor.stop();
        }, "hwmdm-webfilter-shutdown"));
        Thread updaterThread = new Thread(() -> {
            while (stopping.getCount() > 0) {
                if (updater.runOnce()) listsChanged.set(true);
                try {
                    stopping.await(LOOP_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "hwmdm-webfilter-updater");
        updaterThread.setDaemon(true);
        updaterThread.start();
        while (stopping.getCount() > 0) {
            try {
                supervisor.reconcile(listsChanged.getAndSet(false));
            } catch (IOException e) {
                error("ciclo do supervisor falhou: " + e);
            }
            stopping.await(LOOP_SECONDS, TimeUnit.SECONDS);
        }
        supervisor.stop();
    }

    static final class Updater {
        final Path stateFile = Paths.get(STATE_DIR, "updater.json");
        final Map<String, Map<String, Object>> state = new LinkedHashMap<>();

        Updater() {
            try {
                if (Json.parse(Files.readString(stateFile, StandardCharsets.UTF_8)) instanceof Map<?, ?> saved) {
                    for (Map.Entry<?, ?> e : saved.entrySet()) {
                        if (e.getValue() instanceof Map<?, ?> entry) {
                            Map<String, Object> copy = new LinkedHashMap<>();
                            entry.forEach((k, v) -> copy.put(String.valueOf(k), v));
                            state.put(String.valueOf(e.getKey()), copy);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                state.clear();
            }
        }

        void save() throws IOException {
            StringBuilder sb = new StringBuilder("{");
            boolean firstCategory = true;
            for (Map.Entry<String, Map<String, Object>> category : state.entrySet()) {
                if (!firstCategory) sb.append(", ");
                firstCategory = false;
                sb.append(Json.quote(category.getKey())).append(": {");
                boolean firstField = true;
                for (Map.Entry<String, Object> field : category.getValue().entrySet()) {
                    if (!firstField) sb.append(", ");
                    firstField = false;
                    Object v = field.getValue();
                    sb.append(Json.quote(field.getKey())).append(": ")
                      .append(v instanceof String s ? Json.quote(s) : String.valueOf(v));
                }
                sb.append("}");
            }
            writeAtomic(stateFile, sb.append("}").toString());
        }

        static double num(Map<String, Object> entry, String key) {
            return entry.get(key) instanceof Number n ? n.doubleValue() : 0.0;
        }

        static String fingerprint(List<String> sources) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < sources.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(Json.quote(sources.get(i)));
            }
            return sha256(sb.append("]").toString());
        }

        boolean runOnce() {
            Map<?, ?> sourcesByCategory;
            try {
                if (!(Json.parse(Files.readString(Paths.get(DNS_DIR, "sources.json"), StandardCharsets.UTF_8)) instanceof Map<?, ?> parsed)) {
                    return false;
                }
                sourcesByCategory = parsed;
            } catch (IOException | RuntimeException e) {
                return false;
            }
            boolean changed = false;
            double t = now();
            for (Map.Entry<?, ?> e : sourcesByCategory.entrySet()) {
                String category = String.valueOf(e.getKey());
                List<String> sources = new ArrayList<>();
                if (e.getValue() instanceof List<?> list) {
                    for (Object item : list) sources.add(String.valueOf(item));
                }
                Map<String, Object> entry = state.getOrDefault(category, new LinkedHashMap<>());
                boolean exists = Files.exists(Paths.get(LISTS_DIR, category + ".txt"));
                boolean fresh = t - num(entry, "ok") < REFRESH_MAX;
                boolean triedRecently = t - num(entry, "try") < REFRESH_MIN;
                String fp = fingerprint(sources);
                boolean sameSources = fp.equals(entry.get("sources"));
                if (sameSources && (exists && fresh || triedRecently)) continue;
                entry.put("try", t);
                entry.put("sources", fp);
                try {
                    changed |= updateCategory(category, sources);
                    entry.put("ok", t);
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    warning("lista " + category + " nao atualizada (" + ex.getMessage() + "); mantida a anterior");
                }
                state.put(category, entry);
                try {
                    save();
                } catch (IOException ex) {
                    warning("estado do atualizador nao gravado: " + ex.getMessage());
                }
            }
            return changed;
        }
    }

    static final class Supervisor {
        Process proc;
        String runningHash;
        final Path goodConfig = Paths.get(STATE_DIR, "blocky.good.yml");
        String profilesHash;
        String failedHash;
        double retryAt = 0.0;
        int retryDelay = RETRY_FIRST_SECONDS;
        double lastGoodAttempt = 0.0;
        boolean startedOnce = false;

        String profilesDigest() throws IOException {
            MessageDigest digest = sha256();
            Path dir = Paths.get(DNS_DIR, "profiles");
            if (Files.isDirectory(dir)) {
                for (String name : sortedNames(dir)) {
                    if (!name.endsWith(".txt")) continue;
                    digest.update(name.getBytes(StandardCharsets.UTF_8));
                    digest.update(sha256File(dir.resolve(name)).getBytes(StandardCharsets.UTF_8));
                }
            }
            return hex(digest.digest());
        }

        void stop() {
            terminate(proc);
            proc = null;
        }

        boolean start(String configPath) throws IOException {
            stop();
            info("iniciando Blocky com " + configPath);
            proc = launch(withBindListeners(configPath));
            double deadline = now() + HEALTH_TIMEOUT;
            while (now() < deadline) {
                if (!proc.isAlive()) {
                    error("Blocky terminou com codigo " + proc.exitValue());
                    return false;
                }
                if (dnsOk(DNS_PORT)) {
                    startedOnce = true;
                    return true;
                }
                sleepSeconds(HEALTH_POLL_SECONDS);
            }
            error("Blocky nao ficou saudavel em " + HEALTH_TIMEOUT + "s");
            return false;
        }

        void scheduleRetry(String hash) {
            if (!hash.equals(failedHash)) retryDelay = RETRY_FIRST_SECONDS;
            failedHash = hash;
            retryAt = now() + retryDelay;
            warning("configuracao " + hash.substring(0, 12) + ": nova tentativa em " + retryDelay + "s");
            retryDelay = Math.min(retryDelay * 2, RETRY_MAX_SECONDS);
        }

        boolean dryRun(String configPath) throws IOException {
            String config = withoutMissingCert(Files.readString(Paths.get(configPath)));
            config = subMulti(config, "^  dns: .*$", "  dns: " + LOOPBACK + ":" + DRY_RUN_DNS_PORT);
            config = subMulti(config, "^  tls: .*\\n", "");
            config = subMulti(config, "^  http: .*$", "  http: " + DRY_RUN_API_ADDR);
            Path trial = Paths.get(STATE_DIR, "blocky.trial.yml");
            Files.writeString(trial, config);
            info("ensaiando a configuracao em portas alternativas antes de trocar a instancia em uso");
            Process trialProc = launch(trial.toString());
            try {
                double deadline = now() + HEALTH_TIMEOUT;
                while (now() < deadline) {
                    if (!trialProc.isAlive()) return false;
                    if (dnsOk(DRY_RUN_DNS_PORT)) return true;
                    sleepSeconds(HEALTH_POLL_SECONDS);
                }
                return false;
            } finally {
                terminate(trialProc);
            }
        }

        boolean ready(Path configPath) throws IOException {
            List<String> missing = new ArrayList<>();
            for (String file : referencedFiles(Files.readString(configPath))) {
                if (!Files.exists(Paths.get(file))) missing.add(file);
            }
            if (!missing.isEmpty()) info("aguardando listas antes de iniciar: " + String.join(", ", missing));
            return missing.isEmpty();
        }

        void reconcile(boolean listsChanged) throws IOException {
            Path config = Paths.get(DNS_DIR, "blocky.yml");
            if (!Files.exists(config)) return;
            if (proc != null && !proc.isAlive()) {
                error("Blocky caiu (codigo " + proc.exitValue() + ")");
                proc = null;
                runningHash = null;
            }
            String hash = sha256File(config);
            boolean mayFallBack = startedOnce || hash.equals(failedHash);
            if (proc == null && mayFallBack && Files.exists(goodConfig) && now() - lastGoodAttempt >= RESTART_GOOD_SECONDS) {
                lastGoodAttempt = now();
                String digest = profilesDigest();
                if (start(goodConfig.toString())) {
                    info("Blocky de volta com a ultima configuracao boa");
                    runningHash = sha256File(goodConfig);
                    profilesHash = digest;
                } else {
                    stop();
                }
            }
            if (!hash.equals(runningHash) && switchDue(hash) && ready(config)) {
                switchTo(config, hash);
                return;
            }
            if (proc != null && proc.isAlive()) {
                String digest = profilesDigest();
                if ((listsChanged || !digest.equals(profilesHash)) && apiRefresh()) profilesHash = digest;
            }
        }

        boolean switchDue(String hash) {
            return !hash.equals(failedHash) || now() >= retryAt;
        }

        void switchTo(Path config, String hash) throws IOException {
            boolean retrying = hash.equals(failedHash);
            if (retrying && proc != null && proc.isAlive() && !dryRun(config.toString())) {
                scheduleRetry(hash);
                return;
            }
            Path candidate = Paths.get(STATE_DIR, "blocky.candidate.yml");
            Files.copy(config, candidate, StandardCopyOption.REPLACE_EXISTING);
            String digest = profilesDigest();
            if (start(candidate.toString())) {
                Files.copy(candidate, goodConfig, StandardCopyOption.REPLACE_EXISTING);
                runningHash = hash;
                profilesHash = digest;
                failedHash = null;
                retryDelay = RETRY_FIRST_SECONDS;
                info("Blocky pronto com a configuracao " + hash.substring(0, 12));
                return;
            }
            scheduleRetry(hash);
            stop();
            runningHash = null;
            if (Files.exists(goodConfig)) {
                error("configuracao " + hash.substring(0, 12) + " nao subiu; voltando para a ultima configuracao boa");
            } else {
                error("configuracao " + hash.substring(0, 12) + " nao subiu e ainda nao ha configuracao boa; DNS fechado ate a nova tentativa");
            }
        }
    }

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object parse(String text) {
            Json json = new Json(text);
            Object value = json.value();
            json.ws();
            if (json.i != json.s.length()) throw new IllegalArgumentException("JSON com sobra na posicao " + json.i);
            return value;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private Object value() {
            ws();
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw new IllegalArgumentException("JSON invalido na posicao " + i);
            return Double.parseDouble(s.substring(start, i));
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') { i++; return map; }
            while (true) {
                ws();
                String key = string();
                ws();
                if (s.charAt(i++) != ':') throw new IllegalArgumentException("esperado ':' na posicao " + (i - 1));
                map.put(key, value());
                ws();
                char c = s.charAt(i++);
                if (c == '}') return map;
                if (c != ',') throw new IllegalArgumentException("esperado ',' na posicao " + (i - 1));
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') { i++; return list; }
            while (true) {
                list.add(value());
                ws();
                char c = s.charAt(i++);
                if (c == ']') return list;
                if (c != ',') throw new IllegalArgumentException("esperado ',' na posicao " + (i - 1));
            }
        }

        private String string() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("esperado texto na posicao " + i);
            i++;
            StringBuilder sb = new StringBuilder();
            char c;
            while ((c = s.charAt(i++)) != '"') {
                if (c != '\\') { sb.append(c); continue; }
                char esc = s.charAt(i++);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(esc);
                }
            }
            return sb.toString();
        }

        static String quote(String text) {
            StringBuilder sb = new StringBuilder("\"");
            for (char c : text.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    default -> {
                        if (c < ' ' || c > '~') sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.append('"').toString();
        }
    }
}
