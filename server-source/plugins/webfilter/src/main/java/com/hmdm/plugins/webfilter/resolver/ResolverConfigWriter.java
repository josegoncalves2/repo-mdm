package com.hmdm.plugins.webfilter.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;
import com.hmdm.plugins.webfilter.persistence.WebFilterDAO;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.service.WebFilterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * <p>Generates the files consumed by the <code>webfilter-dns</code> resolver container (design D8):
 * <code>blocky.yml</code>, per-profile allow/deny lists and <code>sources.json</code>, under
 * <code>&lt;plugins.files.directory&gt;/webfilter/dns</code>. Every file is written atomically; the configuration is
 * written last so the resolver never sees a configuration that references lists not yet written.</p>
 */
@Singleton
public class ResolverConfigWriter {

    private static final Logger log = LoggerFactory.getLogger(ResolverConfigWriter.class);

    /**
     * <p>Blocky treats a group that has an allowlist but no denylist entries as "allow only these" and blocks
     * everything else. Every profile group therefore carries this entry, which cannot resolve anyway.</p>
     */
    static final String DENY_SENTINEL = "*.webfilter-sentinel.invalid";

    /** Paths as seen inside the resolver container. */
    private static final String CONTAINER_DNS_DIR = "/app/dns";
    private static final String CONTAINER_LISTS_DIR = "/app/lists";

    private final WebFilterDAO dao;
    private final WebFilterCatalog catalog;
    private final Path dnsDir;
    private final String mdmHost;

    /**
     * <p>Identifies this instance as the owner of the resolver files. A hot redeploy of the web application can
     * leave the previous instance's scheduled task alive in the old class loader; without an owner, both would keep
     * rewriting the files with their own code and the resolver would restart on every alternation. The newest instance
     * claims ownership on its first write; an instance that finds another owner stops writing for good.</p>
     */
    private final String ownerToken = UUID.randomUUID().toString();
    private boolean claimed;
    private boolean superseded;

    @Inject
    public ResolverConfigWriter(WebFilterDAO dao, WebFilterCatalog catalog,
                                @Named("plugins.files.directory") String pluginsDir,
                                @Named("base.url") String baseUrl) {
        this.dao = dao;
        this.catalog = catalog;
        this.dnsDir = Paths.get(pluginsDir, "webfilter", "dns");
        this.mdmHost = hostOf(baseUrl);
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String clientName(int customerId, int configurationId) {
        return "c" + customerId + "-p" + configurationId;
    }

    /**
     * <p>Regenerates every resolver file from the enabled policies of all customers.</p>
     */
    public synchronized void writeAll() {
        try {
            Files.createDirectories(dnsDir.resolve("profiles"));
            if (!ownsFiles()) {
                return;
            }
            List<WebFilterPolicy> policies = dao.getAllEnabledPolicies();

            Set<String> usedCategories = new TreeSet<>();
            usedCategories.add(WebFilterCatalog.REQUIRED_CATEGORY);
            Set<String> profileFiles = new TreeSet<>();
            StringBuilder denyGroups = new StringBuilder();
            StringBuilder allowGroups = new StringBuilder();
            StringBuilder clients = new StringBuilder();
            // Dominios que precisam resolver para qualquer aparelho, identificado ou nao: o proprio
            // servidor MDM e o dominio DNS do filtro. Se o aparelho nao resolve o dominio do filtro,
            // o DNS privado do Android falha fechado e ele fica SEM REDE -- o remedio viraria o veneno.
            Set<String> defaultAllow = new TreeSet<>();
            // Aparelho identificado pelo IP cai no grupo default: ele precisa levar tambem os
            // dominios "sempre bloqueados" dos perfis, nao so as categorias.
            Set<String> defaultDeny = new TreeSet<>();
            defaultDeny.add(DENY_SENTINEL);

            for (WebFilterPolicy policy : policies) {
                String client = clientName(policy.getCustomerId(), policy.getConfigurationId());
                String group = "p-" + client;

                Set<String> deny = new TreeSet<>();
                deny.add(DENY_SENTINEL);
                Set<String> protectedForCustomer = protectedDomains(policy.getCustomerId());
                defaultAllow.addAll(protectedForCustomer);
                Set<String> allow = new TreeSet<>(protectedForCustomer);
                for (WebFilterEntry e : dao.getEntries(policy.getId())) {
                    if (WebFilterEntry.KIND_DOMAIN.equals(e.getKind())) {
                        (WebFilterEntry.LIST_ALLOW.equals(e.getList()) ? allow : deny).add("*." + e.getValue());
                        if (!WebFilterEntry.LIST_ALLOW.equals(e.getList())) {
                            defaultDeny.add("*." + e.getValue());
                        }
                    }
                }
                String denyFile = client + "-deny.txt";
                String allowFile = client + "-allow.txt";
                writeAtomically(dnsDir.resolve("profiles").resolve(denyFile), String.join("\n", deny) + "\n");
                writeAtomically(dnsDir.resolve("profiles").resolve(allowFile), String.join("\n", allow) + "\n");
                profileFiles.add(denyFile);
                profileFiles.add(allowFile);

                denyGroups.append("    ").append(group).append(":\n")
                        .append("      - ").append(CONTAINER_DNS_DIR).append("/profiles/").append(denyFile).append("\n");
                allowGroups.append("    ").append(group).append(":\n")
                        .append("      - ").append(CONTAINER_DNS_DIR).append("/profiles/").append(allowFile).append("\n");

                Set<String> categories = new TreeSet<>(dao.getCategories(policy.getId()));
                categories.add(WebFilterCatalog.REQUIRED_CATEGORY);
                usedCategories.addAll(categories);
                clients.append("    ").append(client).append(":\n")
                        .append("      - ").append(group).append("\n");
                for (String category : categories) {
                    clients.append("      - cat-").append(category).append("\n");
                }
            }

            for (String category : usedCategories) {
                denyGroups.append("    cat-").append(category).append(":\n")
                        .append("      - ").append(CONTAINER_LISTS_DIR).append("/").append(category).append(".txt\n");
            }

            // FALHA FECHADA. As chaves de clientGroupsBlock sao casadas pelo Blocky contra o IP, o CIDR
            // ou o nome reverso do cliente -- nunca contra um rotulo de perfil como "c1-p11". Sem a chave
            // "default", todo aparelho que consulta o resolvedor caia fora de qualquer grupo e saia SEM
            // filtro nenhum: era por isto que o tablet abria facebook, instagram e conteudo adulto com o
            // perfil ativo. O grupo default aplica todas as categorias em uso por qualquer perfil, de modo
            // que um aparelho ainda nao identificado e' filtrado pelo criterio mais restrito, nunca liberado.
            // So as listas de categoria entram aqui: os grupos de perfil carregam allowlist propria e
            // liberariam, no default, dominios que pertencem a um unico perfil.
            if (!usedCategories.isEmpty()) {
                // O sentinela mantem o grupo com denylist: sem ele o Blocky leria um grupo que so tem
                // allowlist como "libere apenas estes" e derrubaria todo o resto da internet.
                writeAtomically(dnsDir.resolve("profiles").resolve("default-deny.txt"), String.join("\n", defaultDeny) + "\n");
                writeAtomically(dnsDir.resolve("profiles").resolve("default-allow.txt"),
                        String.join("\n", defaultAllow) + "\n");
                profileFiles.add("default-deny.txt");
                profileFiles.add("default-allow.txt");
                denyGroups.append("    p-default:\n")
                        .append("      - ").append(CONTAINER_DNS_DIR).append("/profiles/default-deny.txt\n");
                allowGroups.append("    p-default:\n")
                        .append("      - ").append(CONTAINER_DNS_DIR).append("/profiles/default-allow.txt\n");

                clients.append("    default:\n")
                        .append("      - p-default\n");
                for (String category : usedCategories) {
                    clients.append("      - cat-").append(category).append("\n");
                }
            }

            writeAtomically(dnsDir.resolve("sources.json"), sourcesJson(usedCategories));
            writeAtomically(dnsDir.resolve("blocky.yml"), blockyYaml(denyGroups, allowGroups, clients));
            // Only after the new configuration stopped referencing them
            removeStaleProfiles(profileFiles);
            log.info("Web filter resolver configuration written: {} active profile(s), categories {}",
                    policies.size(), usedCategories);
        } catch (IOException | RuntimeException e) {
            log.error("Failed to write the web filter resolver configuration to {}", dnsDir, e);
        }
    }

    private boolean ownsFiles() throws IOException {
        if (superseded) {
            return false;
        }
        Path owner = dnsDir.resolve(".owner");
        if (!claimed) {
            writeAtomically(owner, ownerToken + "\n");
            claimed = true;
            return true;
        }
        String current = Files.exists(owner) ? new String(Files.readAllBytes(owner), StandardCharsets.UTF_8).trim() : "";
        if (!ownerToken.equals(current)) {
            superseded = true;
            log.warn("Web filter resolver files are now owned by a newer instance of the application; this instance stops writing them");
            return false;
        }
        return true;
    }

    /**
     * <p>Domains which are always allowed for every profile: the MDM server itself and the filter's own DNS domain
     * (design D6).</p>
     */
    private Set<String> protectedDomains(int customerId) {
        Set<String> result = new TreeSet<>();
        String host = WebFilterValidator.normalizeDomain(mdmHost);
        if (host != null) {
            result.add("*." + host);
        }
        WebFilterSettings settings = dao.getSettings(customerId);
        if (settings != null) {
            String dns = WebFilterValidator.normalizeDomain(settings.getDnsDomain());
            if (dns != null) {
                result.add("*." + dns);
            }
        }
        return result;
    }

    private String sourcesJson(Set<String> categories) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        for (String category : categories) {
            ArrayNode urls = root.putArray(category);
            catalog.getSiteSources(category).forEach(urls::add);
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
    }

    private static String blockyYaml(CharSequence denyGroups, CharSequence allowGroups, CharSequence clients) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Gerado pelo plugin Web Filter do HWMDM. Nao edite: o arquivo e regenerado a cada alteracao.\n");
        yaml.append("upstreams:\n");
        yaml.append("  groups:\n");
        yaml.append("    default:\n");
        yaml.append("      - tcp-tls:1.1.1.1:853\n");
        yaml.append("      - tcp-tls:8.8.8.8:853\n");
        yaml.append("  strategy: parallel_best\n");
        yaml.append("ports:\n");
        yaml.append("  dns: 0.0.0.0:53\n");
        yaml.append("  tls: 853\n");
        yaml.append("  http: 127.0.0.1:4000\n");
        yaml.append("certFile: /app/certs/cert.pem\n");
        yaml.append("keyFile: /app/certs/key.pem\n");
        yaml.append("log:\n");
        yaml.append("  level: info\n");
        yaml.append("queryLog:\n");
        yaml.append("  type: csv\n");
        yaml.append("  target: /app/queries\n");
        yaml.append("  logRetentionDays: 0\n");
        yaml.append("blocking:\n");
        yaml.append("  blockType: nxDomain\n");
        yaml.append("  blockTTL: 1m\n");
        yaml.append("  loading:\n");
        yaml.append("    refreshPeriod: 0m\n");
        yaml.append("    strategy: failOnError\n");
        yaml.append("  denylists:\n").append(denyGroups);
        if (allowGroups.length() > 0) {
            yaml.append("  allowlists:\n").append(allowGroups);
        }
        if (clients.length() > 0) {
            yaml.append("  clientGroupsBlock:\n").append(clients);
        }
        return yaml.toString();
    }

    private void removeStaleProfiles(Set<String> keep) throws IOException {
        List<Path> stale = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dnsDir.resolve("profiles"), "*.txt")) {
            for (Path f : files) {
                if (!keep.contains(f.getFileName().toString())) {
                    stale.add(f);
                }
            }
        }
        for (Path f : stale) {
            Files.deleteIfExists(f);
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (Files.exists(target) && java.util.Arrays.equals(Files.readAllBytes(target), bytes)) {
            return;
        }
        Path tmp = target.resolveSibling("." + target.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
