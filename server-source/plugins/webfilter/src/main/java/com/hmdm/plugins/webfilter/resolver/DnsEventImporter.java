package com.hmdm.plugins.webfilter.resolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdm.plugins.webfilter.persistence.mapper.WebFilterMapper;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEvent;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/** Imports the Blocky 0.34 TSV log without guessing device identity behind shared IPs. */
@Singleton
public class DnsEventImporter implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DnsEventImporter.class);
    private final Path directory;
    private final Path checkpoint;
    private final WebFilterMapper mapper;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Long> offsets = new HashMap<>();
    private boolean loaded;
    @Inject public DnsEventImporter(WebFilterMapper mapper, @Named("plugins.files.directory") String directory) {
        this.mapper = mapper;
        this.directory = Paths.get(directory, "webfilter", "queries");
        this.checkpoint = Paths.get(directory, "webfilter", "dns-events-offsets.json");
    }
    @Override public synchronized void run() {
        try {
            if (!loaded) {
                if (Files.exists(checkpoint)) {
                    json.readTree(checkpoint.toFile()).fields().forEachRemaining(e -> offsets.put(e.getKey(), e.getValue().asLong()));
                }
                loaded = true;
            }
            if (!Files.isDirectory(directory)) { return; }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.log")) {
                for (Path file : files) { read(file); }
            }
            Path tmp = checkpoint.resolveSibling(checkpoint.getFileName() + ".tmp");
            json.writeValue(tmp.toFile(), offsets);
            Files.move(tmp, checkpoint, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) { log.warn("Falha ao importar eventos DNS; será tentado novamente", e); }
    }
    private void read(Path path) throws Exception {
        String name = path.getFileName().toString();
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long offset = offsets.getOrDefault(name, 0L);
            file.seek(offset <= file.length() ? offset : 0);
            int count = 0;
            while (file.getFilePointer() < file.length() && count++ < 5000) {
                long start = file.getFilePointer();
                String line = file.readLine();
                long end = file.getFilePointer();
                file.seek(end - 1);
                boolean complete = file.read() == '\n';
                file.seek(end);
                if (!complete) { break; }
                importLine(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8), name + ":" + start);
                offsets.put(name, end);
            }
        }
    }
    void importLine(String line, String key) {
        String[] fields = line.split("\t", -1);
        if (fields.length < 10 || !"BLOCKED".equalsIgnoreCase(fields[8])) { return; }
        String host = fields[5].replaceFirst("\\.$", "").toLowerCase(Locale.ROOT);
        if (!host.matches("[a-z0-9_.-]{1,253}")) { return; }
        WebFilterEvent event = new WebFilterEvent();
        List<Map<String,Object>> devices = mapper.findDnsDevices(fields[1]);
        if (devices.size() == 1) {
            Map<String,Object> device = devices.get(0);
            event.setDeviceId(((Number)device.get("id")).intValue());
            event.setCustomerId(((Number)device.get("customerid")).intValue());
            if (device.get("configurationid") != null) { event.setConfigurationId(((Number)device.get("configurationid")).intValue()); }
        } else {
            // DoT names carry the profile; an unidentified shared resolver has no tenant identity.
            Matcher profile = Pattern.compile("c(\\d+)-p(\\d+)").matcher(fields[2]);
            List<WebFilterPolicy> policies = mapper.findAllEnabledPolicies();
            if (profile.find()) {
                int customer = Integer.parseInt(profile.group(1)), configuration = Integer.parseInt(profile.group(2));
                if (policies.stream().noneMatch(p -> p.getCustomerId() == customer && p.getConfigurationId() == configuration)) { return; }
                event.setCustomerId(customer); event.setConfigurationId(configuration);
            } else {
                Set<Integer> customers = new HashSet<>();
                policies.forEach(p -> customers.add(p.getCustomerId()));
                if (customers.size() != 1) { return; }
                event.setCustomerId(customers.iterator().next());
            }
        }
        try {
            // Blocky writes the query log in UTC without an offset, even with TZ=America/Sao_Paulo in its container.
            event.setCreatedAt(LocalDateTime.parse(fields[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneOffset.UTC).toInstant().toEpochMilli());
        } catch (RuntimeException e) { log.warn("Data DNS inválida em {}", key); return; }
        event.setHost(host); event.setClientIp(fields[1]); event.setSourceKey(key);
        Matcher category = Pattern.compile("(?:group|groups)[ :]+\\[?([a-zA-Z0-9_-]+)").matcher(fields[4]);
        if (category.find()) { event.setCategory(category.group(1)); }
        mapper.insertDnsEvent(event);
    }
}
