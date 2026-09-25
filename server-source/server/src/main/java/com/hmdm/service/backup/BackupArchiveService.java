package com.hmdm.service.backup;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.util.BackgroundTaskRunnerService;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/** Portable snapshots and persistent scheduling. No device or production data is used by tests. */
@Singleton
public class BackupArchiveService {
    private final Path directory = Paths.get(env("HWMDM_BACKUP_DIR", "/opt/hmdm/backups"));
    private final Path files = Paths.get(env("HWMDM_FILES_DIR", "/usr/local/tomcat/work/files"));
    private final ObjectMapper json = new ObjectMapper();
    private ObjectNode schedule;
    private static final Set<String> CONFIG_TABLES = new LinkedHashSet<>(Arrays.asList(
            "customers", "permissions", "userroles", "users", "groups", "icons", "uploadedfiles", "applications", "applicationversions",
            "configurations", "settings", "configurationapplications", "configurationapplicationparameters", "configurationapplicationsettings",
            "configurationfiles", "userrolepermissions", "userrolesettings", "userconfigurationaccess", "userdevicegroupsaccess", "plugins", "pluginsdisabled",
            "plugin_deviceinfo_settings", "plugin_devicelog_settings", "plugin_devicelog_settings_rules", "plugin_moduleregistry_state",
            "plugin_webfilter_settings", "plugin_webfilter_policies", "plugin_webfilter_policy_categories", "plugin_webfilter_policy_entries",
            "plugin_webfilter_app_categories", "plugin_webfilter_sources"));
    @Inject public BackupArchiveService(BackgroundTaskRunnerService runner) {
        runner.submitRepeatableTask(this::tick, 15, 30, TimeUnit.SECONDS);
    }
    public static String env(String key, String fallback) { String value = System.getenv(key); return value == null || value.isEmpty() ? fallback : value; }
    private static void scope(String value) {
        if (!Arrays.asList("configuration", "data", "full").contains(value)) { throw new IllegalArgumentException("Escopo de backup inválido"); }
    }
    private Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + env("SQL_HOST", "postgresql") + "/" + env("SQL_BASE", "hmdm"), env("SQL_USER", "hmdm"), env("SQL_PASS", "hmdm"));
    }
    public synchronized File create(String kind, boolean automatic) throws Exception {
        scope(kind); Files.createDirectories(directory);
        String name = (automatic ? "auto-" : "") + "hwmdm-" + kind + "-" + Instant.now().toString().replace(':','-') + "-" + UUID.randomUUID().toString().substring(0,8) + ".zip";
        Path target = directory.resolve(name), partial = directory.resolve(name + ".partial");
        Path dump = Files.createTempFile(directory, "dump-", ".partial");
        try {
            if (!"configuration".equals(kind)) { run(Arrays.asList("pg_dump", "--format=custom", "--no-owner", "--no-acl", "--file=" + dump), false); }
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(partial))) {
                ObjectNode manifest = json.createObjectNode(); manifest.put("format", "hwmdm-backup"); manifest.put("version", 1); manifest.put("scope", kind); manifest.put("createdAt", Instant.now().toString());
                put(out, "manifest.json", json.writeValueAsBytes(manifest));
                if ("configuration".equals(kind)) { put(out, "configuration.json", json.writeValueAsBytes(exportConfiguration())); }
                else { put(out, "database.dump", dump); }
                // Configuration records refer to uploaded APKs, icons and other files too.
                if (!"data".equals(kind) && Files.isDirectory(files)) {
                    try (java.util.stream.Stream<Path> paths = Files.walk(files)) {
                        for (Path path : (Iterable<Path>) paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))::iterator) {
                            put(out, "files/" + files.relativize(path).toString().replace(File.separatorChar, '/'), path);
                        }
                    }
                }
            }
            Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            return target.toFile();
        } finally { Files.deleteIfExists(dump); Files.deleteIfExists(partial); }
    }
    private void put(ZipOutputStream out, String name, byte[] data) throws IOException { out.putNextEntry(new ZipEntry(name)); out.write(data); out.closeEntry(); }
    private void put(ZipOutputStream out, String name, Path path) throws IOException { out.putNextEntry(new ZipEntry(name)); Files.copy(path, out); out.closeEntry(); }
    private ObjectNode exportConfiguration() throws Exception {
        ObjectNode result = json.createObjectNode();
        try (Connection conn = connection()) {
            conn.setAutoCommit(false); conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); conn.setReadOnly(true);
            for (String table : tableOrder(conn)) {
                ArrayNode rows = result.putArray(table);
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT row_to_json(t)::text FROM \"" + table + "\" t")) {
                    while (rs.next()) { rows.add(json.readTree(rs.getString(1))); }
                }
            }
            conn.commit();
        }
        return result;
    }
    private List<String> tableOrder(Connection conn) throws SQLException {
        Map<String, Set<String>> remaining = new LinkedHashMap<>();
        DatabaseMetaData meta = conn.getMetaData();
        for (String table : CONFIG_TABLES) {
            try (ResultSet exists = meta.getTables(null, "public", table, new String[]{"TABLE"})) { if (!exists.next()) { continue; } }
            Set<String> parents = new HashSet<>();
            try (ResultSet fk = meta.getImportedKeys(null,"public",table)) { while(fk.next()) {String parent=fk.getString("PKTABLE_NAME");if(CONFIG_TABLES.contains(parent)&&!parent.equals(table)){parents.add(parent);}} }
            remaining.put(table, parents);
        }
        List<String> ordered = new ArrayList<>();
        while (!remaining.isEmpty()) {
            String next = remaining.entrySet().stream().filter(e -> ordered.containsAll(e.getValue())).map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalStateException("Dependência circular nas configurações"));
            ordered.add(next); remaining.remove(next);
        }
        return ordered;
    }
    public ObjectNode inspect(File archive) throws Exception {
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry manifest = zip.getEntry("manifest.json");
            if (manifest == null || manifest.getSize() > 65536) { throw new IllegalArgumentException("Arquivo sem manifesto HWMDM"); }
            JsonNode data; try (InputStream in=zip.getInputStream(manifest)) {data=json.readTree(in);}
            if (!"hwmdm-backup".equals(data.path("format").asText()) || data.path("version").asInt()!=1) {throw new IllegalArgumentException("Formato de backup não suportado");}
            String kind=data.path("scope").asText();scope(kind);
            if(zip.getEntry("configuration".equals(kind)?"configuration.json":"database.dump")==null){throw new IllegalArgumentException("Conteúdo do backup incompleto");}
            Set<String> names=new HashSet<>(); long size=0;
            for (Enumeration<? extends ZipEntry> entries=zip.entries();entries.hasMoreElements();) {
                ZipEntry entry=entries.nextElement();String name=entry.getName();
                if(!names.add(name)||name.startsWith("/")||name.contains("\\")||Arrays.asList(name.split("/")).contains("..")){throw new IllegalArgumentException("Caminho inválido no arquivo");}
                if(!name.equals("manifest.json")&&!name.equals("configuration.json")&&!name.equals("database.dump")&&!name.startsWith("files/")){throw new IllegalArgumentException("Conteúdo desconhecido no backup");}
                if(entry.getSize()<0){throw new IllegalArgumentException("Tamanho inválido");}size+=entry.getSize();
                if(size>20L*1024*1024*1024){throw new IllegalArgumentException("Backup excede 20 GiB descompactados");}
            }
            return (ObjectNode)data;
        }
    }
    public synchronized File upload(InputStream input) throws Exception {
        Files.createDirectories(directory);Path temp=Files.createTempFile(directory,"import-",".partial");
        try {
            try(OutputStream out=Files.newOutputStream(temp)){byte[] buf=new byte[65536];long total=0;int n;while((n=input.read(buf))!=-1){total+=n;if(total>10L*1024*1024*1024){throw new IllegalArgumentException("Arquivo excede 10 GiB");}out.write(buf,0,n);}}
            inspect(temp.toFile());Path target=directory.resolve("import-"+UUID.randomUUID()+".zip");Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);return target.toFile();
        } finally {Files.deleteIfExists(temp);}
    }
    public synchronized String restore(File archive) throws Exception {
        String kind=inspect(archive).path("scope").asText();
        File safety=create("full",false); // Complete snapshot before touching any live state.
        try(ZipFile zip=new ZipFile(archive)) {
            if("configuration".equals(kind)) {
                JsonNode data;try(InputStream in=zip.getInputStream(zip.getEntry("configuration.json"))){data=json.readTree(in);}
                restoreConfiguration(data);
            } else {
                Path dump=Files.createTempFile(directory,"restore-",".partial");
                try {try(InputStream in=zip.getInputStream(zip.getEntry("database.dump"))){Files.copy(in,dump,StandardCopyOption.REPLACE_EXISTING);}
                    run(Arrays.asList("pg_restore","--clean","--if-exists","--no-owner","--no-acl","--single-transaction","--exit-on-error",dump.toString()),true);
                } finally {Files.deleteIfExists(dump);}
            }
            for(Enumeration<? extends ZipEntry> entries=zip.entries();entries.hasMoreElements();) {
                ZipEntry entry=entries.nextElement();if(entry.isDirectory()||!entry.getName().startsWith("files/")){continue;}
                Path target=files.resolve(entry.getName().substring(6)).normalize();
                if(!target.startsWith(files)||target.equals(files)){throw new IOException("Destino inválido");}
                Files.createDirectories(target.getParent());
                // Reject existing symlinks in the destination chain.
                for(Path p=target;p!=null&&p.startsWith(files);p=p.getParent()){if(Files.isSymbolicLink(p)){throw new IOException("Link simbólico no destino");}}
                Path temp=Files.createTempFile(target.getParent(),"restore-",".partial");
                try {try(InputStream in=zip.getInputStream(entry)){Files.copy(in,temp,StandardCopyOption.REPLACE_EXISTING);}Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
            }
        } catch(Exception e){throw new IOException("Falha na restauração. Cópia de segurança preservada: "+safety.getName(),e);}
        return safety.getName();
    }
    private void restoreConfiguration(JsonNode data) throws Exception {
        if(!data.isObject()){throw new IllegalArgumentException("Configurações inválidas");}
        try(Connection conn=connection()) {
            conn.setAutoCommit(false);
            try {
                for(String table:tableOrder(conn)) {
                    JsonNode rows=data.get(table);if(rows==null){continue;}if(!rows.isArray()){throw new IllegalArgumentException("Tabela inválida");}
                    List<String> columns=new ArrayList<>(),primary=new ArrayList<>();
                    try(ResultSet rs=conn.getMetaData().getColumns(null,"public",table,null)){while(rs.next()){columns.add(rs.getString("COLUMN_NAME"));}}
                    try(ResultSet rs=conn.getMetaData().getPrimaryKeys(null,"public",table)){while(rs.next()){primary.add(rs.getString("COLUMN_NAME"));}}
                    String cols=quoted(columns),conflict=" ON CONFLICT DO NOTHING";
                    if(!primary.isEmpty()) {List<String> updates=new ArrayList<>();for(String col:columns){if(!primary.contains(col)){updates.add("\""+col+"\"=EXCLUDED.\""+col+"\"");}}
                        conflict=" ON CONFLICT ("+quoted(primary)+") "+(updates.isEmpty()?"DO NOTHING":"DO UPDATE SET "+String.join(",",updates));}
                    String sql="INSERT INTO \""+table+"\" ("+cols+") SELECT "+cols+" FROM json_populate_record(NULL::\""+table+"\", ?::json) WHERE NOT EXISTS (SELECT 1 FROM \""+table+"\" t WHERE to_jsonb(t)=?::jsonb)"+conflict;
                    try(PreparedStatement st=conn.prepareStatement(sql)){for(JsonNode row:rows){if(!row.isObject()){throw new IllegalArgumentException("Registro inválido");}st.setString(1,row.toString());st.setString(2,row.toString());st.executeUpdate();}}
                    for(String col:primary){try(PreparedStatement st=conn.prepareStatement("SELECT pg_get_serial_sequence(?,?)")){st.setString(1,table);st.setString(2,col);try(ResultSet rs=st.executeQuery()){if(rs.next()&&rs.getString(1)!=null){String seq=rs.getString(1);try(PreparedStatement adjust=conn.prepareStatement("SELECT setval(?::regclass, GREATEST(COALESCE((SELECT MAX(\""+col+"\") FROM \""+table+"\"),1),1),true)")){adjust.setString(1,seq);adjust.execute();}}}}}
                }
                conn.commit();
            } catch(Exception e){conn.rollback();throw e;}
        }
    }
    private static String quoted(List<String> names){List<String> result=new ArrayList<>();for(String name:names){result.add("\""+name.replace("\"","\"\"")+"\"");}return String.join(",",result);}
    private void run(List<String> command, boolean restore) throws Exception {
        List<String> args=new ArrayList<>();args.add(command.get(0));args.add("--host="+env("SQL_HOST","postgresql"));args.add("--username="+env("SQL_USER","hmdm"));args.add("--dbname="+env("SQL_BASE","hmdm"));args.addAll(command.subList(1,command.size()));
        Path output=Files.createTempFile(directory,"process-",".log");
        try {ProcessBuilder pb=new ProcessBuilder(args);pb.environment().put("PGPASSWORD",env("SQL_PASS","hmdm"));pb.redirectErrorStream(true);pb.redirectOutput(output.toFile());Process p=pb.start();
            if(!p.waitFor(30,TimeUnit.MINUTES)){p.destroyForcibly();throw new IOException("Operação excedeu 30 minutos");}
            if(p.exitValue()!=0){throw new IOException(command.get(0)+" falhou: "+new String(Files.readAllBytes(output),java.nio.charset.StandardCharsets.UTF_8));}
        } finally {Files.deleteIfExists(output);}
    }
    public synchronized ObjectNode getSchedule() throws IOException {
        if(schedule==null){Path path=directory.resolve("schedule.json");schedule=Files.exists(path)?(ObjectNode)json.readTree(path.toFile()):json.createObjectNode();if(!schedule.has("enabled")){schedule.put("enabled",false);schedule.put("scope","full");schedule.put("time","02:00");schedule.put("timezone","America/Sao_Paulo");ArrayNode days=schedule.putArray("days");for(int i=1;i<=7;i++){days.add(i);}}}
        return schedule.deepCopy();
    }
    public synchronized ObjectNode saveSchedule(ObjectNode value) throws Exception {
        scope(value.path("scope").asText());LocalTime.parse(value.path("time").asText());ZoneId.of(value.path("timezone").asText());
        if(!value.path("days").isArray()||value.path("days").isEmpty()){throw new IllegalArgumentException("Selecione ao menos um dia");}
        for(JsonNode day:value.path("days")){if(!day.isInt()||day.asInt()<1||day.asInt()>7){throw new IllegalArgumentException("Dia inválido");}}
        ObjectNode next=json.createObjectNode();next.put("enabled",value.path("enabled").asBoolean());next.put("scope",value.path("scope").asText());next.put("time",value.path("time").asText());next.put("timezone",value.path("timezone").asText());next.set("days",value.path("days").deepCopy());next.put("nextRun",nextRun(next,Instant.now()).toEpochMilli());
        schedule=next;persistSchedule();return getSchedule();
    }
    private Instant nextRun(JsonNode value,Instant now){ZoneId zone=ZoneId.of(value.path("timezone").asText());LocalTime time=LocalTime.parse(value.path("time").asText());LocalDate date=now.atZone(zone).toLocalDate();Set<Integer> days=new HashSet<>();value.path("days").forEach(d->days.add(d.asInt()));for(int i=0;i<8;i++){LocalDate day=date.plusDays(i);Instant instant=day.atTime(time).atZone(zone).toInstant();if(days.contains(day.getDayOfWeek().getValue())&&instant.isAfter(now)){return instant;}}throw new IllegalArgumentException("Agendamento inválido");}
    private void persistSchedule() throws IOException {Files.createDirectories(directory);Path temp=directory.resolve("schedule.json.partial");json.writeValue(temp.toFile(),schedule);Files.move(temp,directory.resolve("schedule.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
    private synchronized void tick(){try{getSchedule();if(!schedule.path("enabled").asBoolean()){return;}Instant now=Instant.now();if(schedule.path("nextRun").asLong(Long.MAX_VALUE)>now.toEpochMilli()){return;}try{File result=create(schedule.path("scope").asText(),true);schedule.put("lastBackup",result.getName());schedule.remove("lastError");}catch(Exception e){schedule.put("lastError",e.getMessage());}schedule.put("lastRun",now.toEpochMilli());schedule.put("nextRun",nextRun(schedule,Instant.now()).toEpochMilli());persistSchedule();}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).error("Falha no backup agendado",e);}}
}
