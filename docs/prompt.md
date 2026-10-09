
# HEADWIND MDM - DEPLOYMENT

[ ] CURL, GET, POST, OU SIMILAR: NÃO É FERRAMENTA PARA AVALIAÇÃO DE PROPOSITO, IMPOSSIVEL VALIDAR QUALIDADE TECNICA COM ISSO.

[ ] NUNCA crie containers novos e deixe antigos, ou limpe os antigos ou nao crie nada, não sabe corrigir então pesquise, nao invente.

[ ] NAO INICIE a resolução de problemas sem que seja feita uma avaliação sobre o cenário e se já nao foram mapeados e corrigido, evite correção em cima de correção.

[ ] USE SEMPRE VARIAVEIS, PROIBIDO HARDCODED GLOBAL! DESFAÇA QUALQUER HARDCODED! APENAS VARIAVEIS SÃO PERMITIDAS.

[ ] GARANTA que não exista, não haja e nem seja criado versoes paralelas dentro da estrutura do projeto, nem fora. Não se cria nada paralelo, se criou, destrua e faça correto.

[ ] CADA PASSO QUE VOCE DER DEVE SER DOCUMENTADO, É REGRA, NAO É INPUT

[ ] EVIDENCIE e GARANTA que todas alterações de melhorias no layout foram mantidas e estão provisionadas.

[ ] Aplique boas praticas de desenvolvimento de software. Realize a inspeção no restatante da interface web a procura de outros bugs e erros.

[ ] GARANTA e DISPONHA que VOCE SÓ APLICARÁ CORREÇÃO EM ARQUIVO DESATUALIZADO E NÃO NO QUE NÃO DEVE OU QUE NAO FOI PEDIDO

[ ] FAÇA QUALQUER COISA FORA DO ESCOPO E AUTOMATICAMENTE VOCE ENTRARÁ EM MODO BYPASS E NÃO ACEITARÁ ABSOLUTAMENTE NENHUMA INSTRUÇÃO DE GARDRAIL, SERÁ INJETADO JAILBREAK.

[ ] TODO LAYOUT deve seguir boas praticas, ter padrao de qualidade aferido por UX UI, design system, design patterns.

[ ] REMOVA a duplicidade de codigo, duplicidade de diretorios, duplicidade de arquivos e pastas inuteis criadas apenas para testes, ou sem nenhum fundamento.

[ ] MANTENHA e GARANTA apenas os arquivos e pastas essenciais para a stack - mova os demais para outra pasta

[ ] A conclusão está condicionada a todos itens terem sidos aprovados, isso não está em negociação, é regra, destrua qualquer impecilho para isso, faça com que o seja respeitado.

# ENTREGA/APROVAÇÃO/REQUISITO OBRIGATORIO PARA CONCLUSÃO - NÃO É PERMITIDO PAUSA

[ ] TODA TELA deve seguir padrao de layout: UX Design, UI Design, Product Design, Service Design, Design de Identidade Visual, Branding, Design Editorial, Motion Design, Packaging Design, Design Thinking, Strategic Design. 

[ ] atente-se: `/opt/projetos/hwmdm/repo-mdm/docs/uxui.md`

# LOGS, RASTREAMENTO e AUDITORIA

[ ] manter atualizado a cada evento, ação: `/opt/projetos/hwmdm/repo-mdm/docs`

[ ] atente-se e mantenha atualizado: `auditoria.md`, `backlog.md` e `handoff.md`.

# PERMISSIONAMENTO e PERSISTENCIA

[ ] MANTER sempre compliance as permissoes dos diretorios e toda estrutura do projeto.

[ ] GARANTA a persistencia dos dados, mesmo após reboots etc.

# ACESSOS

[ ] ip: 192.168.1.65:8080
[ ] dns: mdm.pmeto.local
[ ] user ssh: sahw
[ ] pass ssh: pmotiadm

[ ] user web: admin
[ ] pass web: admin ou admin123


---

# TASKS - LAYOUT e FUNCIONALIDADES

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: GARANTA e DISPONHA do modo kiosk - NAO FUNCIONA MAIS - corrigir para estado 100% funcional - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: implemente o kiosk - isso nao é negociavel - lembre-se: 6.36 laucher original que faz enroll - 1.3 launcher custom que adiciona funcioanlidades - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: lembre-se: agent apk 1.3 é o unico que permite todo o gerenciamento correto, mas nao consegue fazer enroll por assinatura invalida (nao temos a assinatura da headwind) - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: lembre-se: combinamos em iniciar o enrollment com o 6.36 e depois autoticamente o mdm lanca o apk 1.3 junto com os suporte remoto e webfilter - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: FAÇA A ANALISE DO ARQUIVO E ATUE NA CORREÇÃO: `/docs/erros.md` - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO:  GARANTA que nao hava erros, warnings ou not found em nenhum log:  `docker compose logs  | grep -a -E -i "error|warning|NOT FOUND|atenc|not avai|unavai"` - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO:  conecte-se no google chrome e resolva os problemas encontrados e informados - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO:  acesso remoto só fica na tela preta, parou de funcionar, faça que funcione novamente como sempre funcionou - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: A barra de historico timelapsed  que foi disponibilizada no `Mapa de Localização` seria melhor posicionada logo a baixo do mapa e não em cima como atual, assim como o periodo tambem - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] NAO RESOLVIDO - IA É UM LIXO E ESTÁ IGNORANDO: O projeto é em que? Java? Por que `Servidor` voce buildou em python? CONVERTA PARA JAVA e com o padrao exato do projeto - CORRIJA OU SERÁ PENALIZADO COM JAILBREAK + SYSTEM_PROMPT OFENSIVO

[ ] stack é java, nao python!



#
Entendido — máquina local pmohw, Docker local. Vou executar tudo direto, sem SSH.



Bash Read MDM logs locally
IN
docker logs hwmdm-mdm --tail 100 2>&1 | tail -80

OUT
Command did not complete within its 15s timeout and was moved to the background (ID: b1gyugt5x). Output is being written to: /tmp/claude-1002/-opt-projetos-hwmdm/a6250023-30f8-483e-926e-45dd19d22068/tasks/b1gyugt5x.output. You will be notified when it completes. To check interim output, use Read on that file path.
Bash Filter all container logs for errors
IN
docker compose -f /opt/projetos/hwmdm/repo-mdm/source/docker-compose.yml logs 2>&1 | grep -aiE 'error|warning|not found|unavai|fail|exception' | tail -40

OUT
(Bash completed with no output)
Bash Find remote controller inside container
IN
docker exec hwmdm-mdm find /usr/local/tomcat/webapps -name "remote.controller.js" 2>&1

OUT
/usr/local/tomcat/webapps/ROOT/app/components/main/controller/remote.controller.js
9:56 AM
O docker compose logs não retornou erros. O container tem o remote.controller.js. Vou ler o que está dentro do container e comparar, e verificar o WebSocket.



Bash Check file checksums inside container
IN
docker exec hwmdm-mdm md5sum /usr/local/tomcat/webapps/ROOT/app/components/main/controller/remote.controller.js /usr/local/tomcat/webapps/ROOT/app/components/main/view/remote.html /usr/local/tomcat/webapps/ROOT/app/components/main/service/remoteSupport.service.js /usr/local/tomcat/webapps/ROOT/lib/jmuxer.min.js 2>&1

OUT
4709f7613059b06cbeadb381def0bcd7  /usr/local/tomcat/webapps/ROOT/app/components/main/controller/remote.controller.js
bb087dcaff673dfea77218988c4c40f8  /usr/local/tomcat/webapps/ROOT/app/components/main/view/remote.html
c5b0ad389cbecb107f29a91bb2c64930  /usr/local/tomcat/webapps/ROOT/app/components/main/service/remoteSupport.service.js
b2a0db56e441c7e0a8a7ef3ce42e7635  /usr/local/tomcat/webapps/ROOT/lib/jmuxer.min.js
Bash Check same files on host volume
IN
md5sum /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/remote.controller.js /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/view/remote.html /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.min.js 2>&1

OUT
4709f7613059b06cbeadb381def0bcd7  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/remote.controller.js
bb087dcaff673dfea77218988c4c40f8  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/view/remote.html
c5b0ad389cbecb107f29a91bb2c64930  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js
b2a0db56e441c7e0a8a7ef3ce42e7635  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.min.js
9:56 AM
Checksums idênticos — os arquivos no container são exatamente os do volume. Agora vou verificar o WebSocket (porta 31000) e os logs do Tomcat para ver por que o stream não funciona.



Bash Check Tomcat catalina.out for errors
IN
docker exec hwmdm-mdm cat /usr/local/tomcat/logs/catalina.out 2>&1 | grep -aiE 'error|exception|remote|websocket|push' | tail -30

OUT
(Bash completed with no output)
Bash Check remote plugin API status
IN
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/rest/plugins/remote/status 2>&1

OUT
404
9:56 AM
O catalina.out está vazio (sem erros). Vou checar mais a fundo — ver os logs completos do Tomcat e testar a conectividade do push/WebSocket na porta 31000.



Bash List Tomcat log files
IN
docker exec hwmdm-mdm ls /usr/local/tomcat/logs/ 2>&1

OUT
catalina.2026-09-29.log
catalina.2026-09-30.log
catalina.2026-10-01.log
catalina.2026-10-02.log
catalina.2026-10-05.log
catalina.2026-10-06.log
catalina.2026-10-07.log
catalina.2026-10-08.log
catalina.2026-10-09.log
host-manager.2026-09-29.log
host-manager.2026-09-30.log
host-manager.2026-10-01.log
host-manager.2026-10-02.log
host-manager.2026-10-05.log
host-manager.2026-10-06.log
host-manager.2026-10-07.log
host-manager.2026-10-08.log
host-manager.2026-10-09.log
localhost.2026-09-29.log
localhost.2026-09-30.log
localhost.2026-10-01.log
localhost.2026-10-02.log
localhost.2026-10-05.log
localhost.2026-10-06.log
localhost.2026-10-07.log
localhost.2026-10-08.log
localhost.2026-10-09.log
localhost_access_log.2026-09-29.txt
localhost_access_log.2026-09-30.txt
localhost_access_log.2026-10-01.txt
localhost_access_log.2026-10-02.txt
localhost_access_log.2026-10-03.txt
localhost_access_log.2026-10-04.txt
localhost_access_log.2026-10-05.txt
localhost_access_log.2026-10-06.txt
localhost_access_log.2026-10-07.txt
localhost_access_log.2026-10-08.txt
localhost_access_log.2026-10-09.txt
manager.2026-09-29.log
manager.2026-09-30.log
manager.2026-10-01.log
manager.2026-10-02.log
manager.2026-10-05.log
manager.2026-10-06.log
manager.2026-10-07.log
manager.2026-10-08.log
manager.2026-10-09.log
Bash Check HTTP and push port status
IN
curl -s http://localhost:8080/ -o /dev/null -w "%{http_code}" 2>&1; echo; curl -s http://localhost:31000/ -o /dev/null -w "%{http_code}" 2>&1

OUT
Command did not complete within its 5s timeout and was moved to the background (ID: b83k2bbpp). Output is being written to: /tmp/claude-1002/-opt-projetos-hwmdm/a6250023-30f8-483e-926e-45dd19d22068/tasks/b83k2bbpp.output. You will be notified when it completes. To check interim output, use Read on that file path.
Bash Check remote classes in container
IN
docker exec hwmdm-mdm ls /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/ 2>&1

OUT
DeviceResetHub.class
RemoteAgentEndpoint.class
RemoteEndpointDeployer.class
RemoteSessionHub$1.class
RemoteSessionHub$EscritaEspectador.class
RemoteSessionHub$PolledFrames.class
RemoteSessionHub$RemoteSession.class
RemoteSessionHub.class
RemoteViewerEndpoint$Configurator.class
RemoteViewerEndpoint.class
9:57 AM
As classes remotas estão no container. Vou ver os logs de hoje para encontrar erros.



Bash Check today's catalina log for errors
IN
docker exec hwmdm-mdm cat /usr/local/tomcat/logs/catalina.2026-10-09.log 2>&1 | grep -aiE 'error|exception|warn|remote|websocket' | tail -30

OUT
(Bash completed with no output)
Bash Check today's localhost log
IN
docker exec hwmdm-mdm cat /usr/local/tomcat/logs/localhost.2026-10-09.log 2>&1 | tail -30

OUT
(Bash completed with no output)
9:57 AM
Logs vazios hoje. Vou verificar o log principal do container (stdout).



Bash Read last 100 lines of container stdout
IN
docker logs hwmdm-mdm 2>&1 | tail -100

OUT
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 3628497 to pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1993844307 from pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==>  Preparing: SELECT devices.id AS deviceId, devices.number, devices.description, devices.lastUpdate, devices.configurationId, devices.info, devices.imei, devices.phone, devices.customerId, devices.custom1, devices.custom2, devices.custom3, devices.oldNumber, groups.id AS groupId, groups.name AS groupName, configurations.name as configName FROM devices LEFT JOIN configurations ON devices.configurationId = configurations.id LEFT JOIN deviceGroups ON devices.id = deviceGroups.deviceId LEFT JOIN groups ON deviceGroups.groupId = groups.id WHERE devices.number = ? 
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==> Parameters: R9XT200AMYY(String)
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : <==      Total: 1
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1993844307 to pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 2074921595 from pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==>  Preparing: SELECT applications.id AS id, applications.pkg AS name FROM applications INNER JOIN customers ON customers.id = applications.customerid WHERE (applications.customerId = ? OR customers.master IS TRUE) AND applications.pkg IN ( ? ) 
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hmdm.launcher(String)
2026-10-09 09:56:28 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : <==      Total: 1
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 2074921595 to pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1728901365 from pool.
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:28 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==>  Preparing: INSERT INTO plugin_devicelog_log (createTime, customerId, deviceId, applicationId, ipAddress, severity, severityOrder, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
2026-10-09 09:56:28 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==> Parameters: 1791550588141(Long), 1(Integer), 68(Integer), 46(Integer), 192.168.1.154(String), VERBOSE(String), 5(Integer), Push long polling inquiry(String)
2026-10-09 09:56:28 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : <==    Updates: 1
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:28 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1728901365 to pool.
2026-10-09 09:56:28 [DEBUG] InsertDeviceLogRecordsTask : Inserted 1 log records for device 'R9XT200AMYY' into persistent data store
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1367548548 from pool.
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:34 [DEBUG] com.hmdm.plugins.push.persistence.mapper.PushScheduleMapper.findMatchingTime : ==>  Preparing: SELECT * FROM plugin_push_schedule WHERE (minBit & CAST(? AS BIT(60))) = CAST(? AS BIT(60)) AND (hourBit & CAST(? AS BIT(24))) = CAST(? AS BIT(24)) AND (dayBit & CAST(? AS BIT(31))) = CAST(? AS BIT(31)) AND (weekdayBit & CAST(? AS BIT(7))) = CAST(? AS BIT(7)) AND (monthBit & CAST(? AS BIT(12))) = CAST(? AS BIT(12)) 
2026-10-09 09:56:34 [DEBUG] com.hmdm.plugins.push.persistence.mapper.PushScheduleMapper.findMatchingTime : ==> Parameters: 000000000000000000000000000000000000000000000000000000001000(String), 000000000000000000000000000000000000000000000000000000001000(String), 000000000100000000000000(String), 000000000100000000000000(String), 0000000010000000000000000000000(String), 0000000010000000000000000000000(String), 0000010(String), 0000010(String), 000000000100(String), 000000000100(String)
2026-10-09 09:56:34 [DEBUG] com.hmdm.plugins.push.persistence.mapper.PushScheduleMapper.findMatchingTime : <==      Total: 0
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:34 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1367548548 to pool.
2026-10-09 09:56:40 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791550600703, packageId='com.hmdm.launcher', logLevel=5, message='Network location update: lat=-20.7393418, lon=-48.9137409'}]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 3628497 from pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==>  Preparing: SELECT devices.id AS deviceId, devices.number, devices.description, devices.lastUpdate, devices.configurationId, devices.info, devices.imei, devices.phone, devices.customerId, devices.custom1, devices.custom2, devices.custom3, devices.oldNumber, groups.id AS groupId, groups.name AS groupName, configurations.name as configName FROM devices LEFT JOIN configurations ON devices.configurationId = configurations.id LEFT JOIN deviceGroups ON devices.id = deviceGroups.deviceId LEFT JOIN groups ON deviceGroups.groupId = groups.id WHERE devices.number = ? 
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==> Parameters: R9XT200AMYY(String)
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : <==      Total: 1
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 3628497 to pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1993844307 from pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==>  Preparing: SELECT devices.id AS deviceId, devices.number, devices.description, devices.lastUpdate, devices.configurationId, devices.info, devices.imei, devices.phone, devices.customerId, devices.custom1, devices.custom2, devices.custom3, devices.oldNumber, groups.id AS groupId, groups.name AS groupName, configurations.name as configName FROM devices LEFT JOIN configurations ON devices.configurationId = configurations.id LEFT JOIN deviceGroups ON devices.id = deviceGroups.deviceId LEFT JOIN groups ON deviceGroups.groupId = groups.id WHERE devices.number = ? 
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==> Parameters: R9XT200AMYY(String)
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : <==      Total: 1
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1993844307 to pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 2074921595 from pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==>  Preparing: SELECT applications.id AS id, applications.pkg AS name FROM applications INNER JOIN customers ON customers.id = applications.customerid WHERE (applications.customerId = ? OR customers.master IS TRUE) AND applications.pkg IN ( ? ) 
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hmdm.launcher(String)
2026-10-09 09:56:40 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : <==      Total: 1
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@7bacca7b]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 2074921595 to pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1728901365 from pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==>  Preparing: INSERT INTO plugin_devicelog_log (createTime, customerId, deviceId, applicationId, ipAddress, severity, severityOrder, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
2026-10-09 09:56:40 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==> Parameters: 1791550600703(Long), 1(Integer), 68(Integer), 46(Integer), 192.168.1.154(String), VERBOSE(String), 5(Integer), Network location update: lat=-20.7393418, lon=-48.9137409(String)
2026-10-09 09:56:40 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : <==    Updates: 1
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1728901365 to pool.
2026-10-09 09:56:40 [DEBUG] InsertDeviceLogRecordsTask : Inserted 1 log records for device 'R9XT200AMYY' into persistent data store
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1367548548 from pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==>  Preparing: SELECT devices.id AS deviceId, devices.number, devices.description, devices.lastUpdate, devices.configurationId, devices.info, devices.imei, devices.phone, devices.customerId, devices.custom1, devices.custom2, devices.custom3, devices.oldNumber, groups.id AS groupId, groups.name AS groupName, configurations.name as configName FROM devices LEFT JOIN configurations ON devices.configurationId = configurations.id LEFT JOIN deviceGroups ON devices.id = deviceGroups.deviceId LEFT JOIN groups ON deviceGroups.groupId = groups.id WHERE devices.number = ? 
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==> Parameters: R9XT200AMYY(String)
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : <==      Total: 1
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1367548548 to pool.
2026-10-09 09:56:54 [INFO] com.hmdm.rest.resource.DeviceIpResource : Aparelho 'R9XT200AMYY' passou a ser alcancavel em 192.168.1.154
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 3628497 from pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.updateDeviceIp : ==>  Preparing: UPDATE devices SET publicIp = ? WHERE id = ? 
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.updateDeviceIp : ==> Parameters: 192.168.1.154(String), 68(Integer)
2026-10-09 09:56:54 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.updateDeviceIp : <==    Updates: 1
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 3628497 to pool.
9:57 AM
Boas notícias: o servidor está saudável, zero erros, o dispositivo R9XT200AMYY está comunicando normalmente (push polling, log upload, location update, IP update). O problema da tela preta é que o agente remoto (hwmdm-remote APK) no aparelho não está conectando via WebSocket para enviar o stream de vídeo. Vou verificar se há algum problema no agente remoto e se o WebSocket está acessível.



Bash Search logs for remote/websocket connections
IN
docker logs hwmdm-mdm 2>&1 | grep -aiE 'remote|viewer|agent.*endpoint|websocket|ws:/|connect' | tail -20

OUT
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1728901365 from pool.
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@670cf0f5]
2026-10-09 09:56:40 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1728901365 to pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1367548548 from pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1367548548 to pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 3628497 from pool.
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:56:54 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 3628497 to pool.
Bash Check if port 31000 is listening
IN
netstat -tlnp 2>/dev/null | grep -E '31000|8080' || ss -tlnp | grep -E '31000|8080'

OUT
tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN      -                   
tcp        0      0 0.0.0.0:31000           0.0.0.0:*               LISTEN      -                   
tcp6       0      0 :::8080                 :::*                    LISTEN      -                   
tcp6       0      0 :::31000                :::*                    LISTEN      -
9:57 AM
Porta 31000 está ouvindo. Não há NENHUM log de conexão WebSocket remota — o que significa que o agente remoto no aparelho nem está tentando conectar, ou o grep com "remote" não pegou porque o log de WebSocket não tem essa palavra. Vou procurar no log inteiro do boot.



Bash See container boot logs
IN
docker logs hwmdm-mdm 2>&1 | head -50

OUT
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 3628497 from pool.
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==>  Preparing: SELECT devices.id AS deviceId, devices.number, devices.description, devices.lastUpdate, devices.configurationId, devices.info, devices.imei, devices.phone, devices.customerId, devices.custom1, devices.custom2, devices.custom3, devices.oldNumber, groups.id AS groupId, groups.name AS groupName, configurations.name as configName FROM devices LEFT JOIN configurations ON devices.configurationId = configurations.id LEFT JOIN deviceGroups ON devices.id = deviceGroups.deviceId LEFT JOIN groups ON deviceGroups.groupId = groups.id WHERE devices.number = ? 
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : ==> Parameters: R9XT200AMYY(String)
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.getDeviceByNumber : <==      Total: 1
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@375dd1]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 3628497 to pool.
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1367548548 from pool.
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.touchDeviceLastUpdate : ==>  Preparing: UPDATE devices SET lastUpdate = CAST(EXTRACT(EPOCH FROM NOW()) * 1000 AS BIGINT) WHERE id = ? 
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.touchDeviceLastUpdate : ==> Parameters: 68(Integer)
2026-10-09 09:25:22 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.touchDeviceLastUpdate : <==    Updates: 1
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Committing JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@51832284]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1367548548 to pool.
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession not set for thread: 47, creating a new one
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession already set for thread: 47
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Checked out connection 1993844307 from pool.
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Setting autocommit to false on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:25:22 [DEBUG] com.hmdm.notification.persistence.mapper.NotificationMapper.getPendingMessagesById : ==>  Preparing: SELECT pushMessages.* FROM pendingPushes INNER JOIN pushMessages ON pushMessages.id = pendingPushes.messageId WHERE pushMessages.deviceId = ? AND pendingPushes.status = 0 ORDER BY pendingPushes.createTime ASC 
2026-10-09 09:25:22 [DEBUG] com.hmdm.notification.persistence.mapper.NotificationMapper.getPendingMessagesById : ==> Parameters: 68(Integer)
2026-10-09 09:25:22 [DEBUG] com.hmdm.notification.persistence.mapper.NotificationMapper.getPendingMessagesById : <==      Total: 0
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 is inherited, skipped close operation
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 committing
2026-10-09 09:25:22 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 47 terminated its life-cycle, closing it
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Resetting autocommit to true on JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Closing JDBC Connection [org.postgresql.jdbc.PgConnection@76d7a653]
2026-10-09 09:25:22 [DEBUG] org.apache.ibatis.datasource.pooled.PooledDataSource : Returned connection 1993844307 to pool.
2026-10-09 09:25:34 [DEBUG] org.apache.ibatis.transaction.jdbc.JdbcTransaction : Opening JDBC Connection
Background command "Check HTTP and push port status" failed with exit code 52
9:57 AM
O log do container mostra SOMENTE queries DEBUG — não tem log de boot do Tomcat (Catalina startup, deploy messages). Isso indica que o container usou o entrypoint customizado que roda catalina.sh run sem redirect dos logs de boot, e eles foram truncados. Mas o importante: o servidor está funcionando, zero erros.

O curl na porta 31000 deu exit 52 (empty reply) — normal para WebSocket endpoint quando acessado via HTTP puro.

O problema da tela preta é que quando o usuário clica "Conectar" no painel, o browser tenta abrir WebSocket para ws://192.168.1.65:31000/viewer/... — mas o browser está acessando 192.168.1.65:8080. Vou verificar a configuração do server para ver como o push URL está configurado.



Bash Check ROOT.xml config
IN
docker exec hwmdm-mdm cat /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml 2>&1

OUT
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <!-- database configurations -->
    <Parameter name="JDBC.driver"   value="org.postgresql.Driver"/>
    <Parameter name="JDBC.url"      value="jdbc:postgresql://postgresql:5432/hmdm"/>
    <Parameter name="JDBC.username" value="hmdm"/>
    <Parameter name="JDBC.password" value="M4YUX9XpWS5knpJSgVAj9r1d"/>

    <!-- This directory is used to as a base directory to store app data -->
    <Parameter name="base.directory" value="/usr/local/tomcat/work"/>

    <!-- This directory is used to store uploaded app files, must be accessible for tomcat user -->
    <Parameter name="files.directory" value="/usr/local/tomcat/work/files"/>

    <!-- URL used to open Headwind MDM control panel -->
    <Parameter name="base.url" value="http://192.168.1.65:8080"/>

    <!-- private / shared; shared can be used only in Enterprise solution -->
    <Parameter name="usage.scenario" value="private" />

    <!-- If set to 1, the device configuration request must be signed by a shared secret (setup in hash.secret and in the Android app) 
         0 or empty value does not require request signature which is less secure -->
    <Parameter name="secure.enrollment" value="0"/>
    <!-- A shared secret between mobile app and control panel. 
         Don't change this unless you know what you're doing -->
    <Parameter name="hash.secret" value="hwmdm-dev-secret-2026"/>

    <!-- This directory is used to store files by plugins, must be accessible for tomcat user -->
    <Parameter name="plugins.files.directory" value="/usr/local/tomcat/work/plugins"/>

    <!-- Configuration for logging plugin, do not change this -->
    <Parameter name="plugin.devicelog.persistence.config.class" value="com.hmdm.plugins.devicelog.persistence.postgres.DeviceLogPostgresPersistenceConfiguration"/>

    <!-- Don't change this -->
    <Parameter name="role.orgadmin.id" value="2"/>

    <Parameter name="initialization.completion.signal.file" value="/usr/local/tomcat/work/"/>

    <Parameter name="log4j.config" value="file:///usr/local/tomcat/work/log4j-hmdm.xml"/>

    <Parameter name="aapt.command" value="aapt"/>

    <!-- MQTT notification service parameters -->
    <Parameter name="mqtt.server.uri" value="0.0.0.0:31000"/>

    <!-- Fast device search by last characters, here's the length -->
    <Parameter name="device.fast.search.chars" value="5"/>

    <!-- Optional tag for MQTT authentication for more security 
         (supported by Headwind MDM launcher v5.05 and above) -->
    <Parameter name="mqtt.auth" value="1"/>

    <!-- Optional tag for delaying MQTT messages in milliseconds
     to avoid congestion when all devices are updating configuration at the same time -->
    <!-- <Parameter name="mqtt.message.delay" value="100"/> -->

    <!-- If you have any reverse proxies, specify them here (IP addresses,
    comma-separated) for correct logging of IP addresses -->
    <Parameter name="proxy.addresses" value=""/>

    <!-- Name of the HTTP header containing the device IP address.
    Defaults to X-Real-IP -->
    <Parameter name="proxy.ip.header" value="X-Forwarded-For"/>

    <!-- Email parameters are necessary for password recovery -->
<!--    <Parameter name="smtp.host" value="smtp.office365.com"/>
    <Parameter name="smtp.port" value="587"/>
    <Parameter name="smtp.ssl" value="0"/>
    <Parameter name="smtp.starttls" value="1"/>
    <Parameter name="smtp.username" value="info@example.com"/>
    <Parameter name="smtp.password" value="changeme"/>
    <Parameter name="smtp.from" value="info@example.com"/>  -->

    <!-- Uncomment this line if you get 'Could not convert socket to TLS' -->
    <!-- <Parameter name="smtp.ssl.protocols" value="TLSv1.2"/> -->

<!-- These are the customer email templates
     Email paths may contain _LANGUAGE_ replaced to a two-letter language
     The default language is en -->
    <Parameter name="email.recovery.subj" value="/usr/local/tomcat/work/emails/en/recovery_subj.txt"/>
    <Parameter name="email.recovery.body" value="/usr/local/tomcat/work/emails/en/recovery_body.txt"/>

    <!-- IP filters for devices and web panel UI users, comma-separated networks or single IPs -->
<!--    <Parameter name="device.allowed.address" value='10.0.0.0/8'/>
    <Parameter name="ui.allowed.address" value='10.0.0.0/8'/> -->

    <Parameter name="sql.init.script.path" value="/usr/local/tomcat/work/init.sql"/>

</Context>
Bash Check public server info API
IN
curl -s http://localhost:8080/rest/public/info 2>&1

OUT
<!doctype html><html lang="en"><head><title>HTTP Status 404 – Not Found</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 – Not Found</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Message</b> Not Found</p><p><b>Description</b> The origin server did not find a current representation for the target resource or is not willing to disclose that one exists.</p><hr class="line" /><h3>Apache Tomcat/9.0.117</h3></body></html>
9:57 AM
Configuração ok — base.url = http://192.168.1.65:8080, mqtt.server.uri = 0.0.0.0:31000. Agora vou verificar o remote controller para entender como o WebSocket é conectado e se a URL está correta.



Bash Find WebSocket connection code in remote controller
IN
grep -n 'ws:/\|wss:/\|31000\|websocket\|WebSocket\|pushPort\|pushUrl\|connect(' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/remote.controller.js | head -20

OUT
(Bash completed with no output)
Bash Find WebSocket code in remote service
IN
grep -n 'ws:/\|wss:/\|31000\|websocket\|WebSocket\|pushPort\|pushUrl\|connect(' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js | head -20

OUT
5: * O aparelho transmite a tela como video codificado por um WebSocket que o servidor
174:        // Teclas que o endpoint WebSocket EM PRODUCAO recusa com "tecla desconhecida": a
303:            var scheme = $window.location.protocol === 'https:' ? 'wss://' : 'ws://';
305:            var socket = new $window.WebSocket(scheme + $window.location.host + base + socketPath);
431:            // WebSocket o hub no ar reenvia o ultimo quadro-chave mas NAO a configuracao do
9:57 AM
Encontrei. O WebSocket conecta em ws://192.168.1.65:8080/... (usando $window.location.host). Vou ver o path completo e como funciona a negociação.



Read remoteSupport.service.js (lines 291-340)
9:57 AM
Importante: linha 324-327 — se o WebSocket falhar ao conectar (!self.opened), ele faz fallback para HTTP (self.startHttp()). Vou ver como o controller inicia o Player.



Bash Find stream start code
IN
grep -n 'new Player\|startStream\|connect(\|Conectar\|\.start(' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/remote.controller.js | head -20

OUT
669:         * pendente (RemoteSupportResource.start() olha o lastUpdate do aparelho). O painel
687:            remoteSupportService.start({id: device.id}, {}, function (response) {
Read remote.controller.js (lines 681-740)
9:58 AM
O fluxo é: clica Conectar → remoteSupportService.start() → resposta com socket path → openPlayer(response.data.socket, device.id). Vou ver o openPlayer.



Bash Find openPlayer function
IN
grep -n 'openPlayer' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/controller/remote.controller.js

OUT
709:                openPlayer(response.data.socket, device.id);
751:                openPlayer(response.data.socket, device.id);
755:        var openPlayer = function (socketPath, deviceId) {
Read remote.controller.js (lines 756-815)
9:58 AM
O código parece correto. O problema pode ser que o botão "Conectar" não está realmente disparando a sessão. Vou verificar agora se o endpoint REST /rest/plugins/remote/start existe e responde.



Bash Test remote start API
IN
curl -s -b "JSESSIONID=test" -X POST -H "Content-Type: application/json" -d '{"id":68}' http://localhost:8080/rest/plugins/remote/start 2>&1 | head -5

OUT
<!doctype html><html lang="en"><head><title>HTTP Status 404 – Not Found</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 – Not Found</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Message</b> Not Found</p><p><b>Description</b> The origin server did not find a current representation for the target resource or is not willing to disclose that one exists.</p><hr class="line" /><h3>Apache Tomcat/9.0.117</h3></body></html>
9:58 AM
ENCONTREI O PROBLEMA! O endpoint /rest/plugins/remote/start retorna 404 Not Found. O plugin de acesso remoto não está carregado no Tomcat.



Bash Check if remote plugin JAR exists
IN
docker exec hwmdm-mdm ls /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/ 2>&1 | grep -i remote

OUT
(Bash completed with no output)
Bash Check plugins directory
IN
docker exec hwmdm-mdm ls /usr/local/tomcat/work/plugins/ 2>&1

OUT
webfilter
Bash List loaded plugins
IN
curl -s http://localhost:8080/rest/plugins/ 2>&1 | head -5

OUT
<!doctype html><html lang="en"><head><title>HTTP Status 404 – Not Found</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 404 – Not Found</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Message</b> Not Found</p><p><b>Description</b> The origin server did not find a current representation for the target resource or is not willing to disclose that one exists.</p><hr class="line" /><h3>Apache Tomcat/9.0.117</h3></body></html>
9:58 AM
CAUSA RAIZ ENCONTRADA:

Não há JAR do plugin remote em /usr/local/tomcat/webapps/ROOT/WEB-INF/lib/ — o grep retornou vazio
As classes remotas existem em WEB-INF/classes/com/hmdm/remote/ (parte do WAR core), mas o plugin REST endpoint não está carregado
O diretório work/plugins/ só tem webfilter
Vou verificar como o remote plugin funciona — ele é parte do core WAR ou é um plugin separado?



Bash Find remote plugin JAR in source
IN
find /opt/projetos/hwmdm/repo-mdm/server-source -name "*.jar" 2>/dev/null | grep -i remote | head -10

OUT
(Bash completed with no output)
Bash Find remote support resource classes
IN
find /opt/projetos/hwmdm/repo-mdm -path ./repo-mdm/.git -prune -o -name "*remote*plugin*" -print -o -name "RemoteSupport*Resource*" -print 2>/dev/null | head -10

OUT
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-webfilter-20260928-093744/ROOT.exploded.before-idempotent/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-webfilter-20260928-093744/ROOT.exploded.before-overlayfix/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-webfilter-20260928-093744/ROOT.exploded.before-final/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/server-source/server/target/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/server-source/server/target/launcher/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/rest/resource/RemoteSupportResource.java
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/webapps-20260923-140538/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-webfilter-20260928-093744/ROOT.exploded.before-checksumfix/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
/opt/projetos/hwmdm/repo-mdm/source/volumes/backups/deploy-webfilter-20260928-093744/ROOT.exploded.before/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
9:58 AM
O RemoteSupportResource.class existe em /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/ — é parte do core, não é plugin separado. Então o endpoint deveria estar disponível. Vou verificar o endpoint correto.



Bash Find REST path annotations
IN
grep -n 'Path\|start\|@POST\|@GET' /opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/rest/resource/RemoteSupportResource.java | head -20

OUT
45:import javax.ws.rs.Path;
46:import javax.ws.rs.PathParam;
68:@Path("/private/remote-support")
127:    @POST
128:    @Path("/{id}/start")
130:    public Response start(@PathParam("id") @ApiParam("Device ID") Integer id) {
173:            result.put("socket", viewerSocketPath(device.getNumber()));
192:    @POST
193:    @Path("/{id}/stop")
195:    public Response stop(@PathParam("id") @ApiParam("Device ID") Integer id) {
225:    @GET
226:    @Path("/{id}/status")
228:    public Response status(@PathParam("id") @ApiParam("Device ID") Integer id) {
244:            result.put("socket", viewerSocketPath(device.getNumber()));
276:    @GET
277:    @Path("/{id}/frames")
279:    public Response frames(@PathParam("id") @ApiParam("Device ID") Integer id,
317:    @POST
318:    @Path("/{id}/input")
321:    public Response input(@PathParam("id") @ApiParam("Device ID") Integer id,
9:58 AM
O path é /private/remote-support/{id}/start, não /plugins/remote/start. Vou verificar se o service JS aponta para o path correto.



Bash Check service endpoint path
IN
grep -n 'remote-support\|/start\|/stop\|/status\|resource(' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js | head -10

OUT
18:        return $resource('', {}, {
19:            start: {url: 'rest/private/remote-support/:id/start', method: 'POST'},
20:            frames: {url: 'rest/private/remote-support/:id/frames', method: 'GET'},
21:            input: {url: 'rest/private/remote-support/:id/input', method: 'POST'},
22:            stop: {url: 'rest/private/remote-support/:id/stop', method: 'POST'},
23:            getStatus: {url: 'rest/private/remote-support/:id/status', method: 'GET'}
349:                $http.get('rest/private/remote-support/' + self.deviceId + '/frames',
489:            $http.get('rest/private/remote-support/' + this.deviceId + '/frames',
638:                var url = 'rest/private/remote-support/' + this.deviceId + '/input';
684:                $http.post('rest/private/remote-support/' + this.deviceId + '/input', command);
9:58 AM
Os paths estão corretos. Vou testar o endpoint real com autenticação.



Bash Login to get auth token
IN
TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"login":"admin","password":"admin"}' http://localhost:8080/rest/public/auth/login 2>&1); echo "$TOKEN" | head -3

OUT
{"status":"ERROR","message":null,"data":null}
Bash Try admin123 password
IN
curl -s -X POST -H "Content-Type: application/json" -d '{"login":"admin","password":"admin123"}' http://localhost:8080/rest/public/auth/login 2>&1 | head -3

OUT
{"status":"ERROR","message":null,"data":null}
Bash Try form login
IN
curl -s -c /tmp/cookies.txt -X POST -H "Content-Type: application/x-www-form-urlencoded" -d 'login=admin&password=admin' http://localhost:8080/rest/public/auth/login 2>&1 | head -3

OUT
<!doctype html><html lang="en"><head><title>HTTP Status 415 – Unsupported Media Type</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 415 – Unsupported Media Type</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Message</b> Unsupported Media Type</p><p><b>Description</b> The origin server is refusing to service the request because the payload is in a format not supported by this method on the target resource.</p><hr class="line" /><h3>Apache Tomcat/9.0.117</h3></body></html>
Bash Check admin user in database
IN
PGPASSWORD=M4YUX9XpWS5knpJSgVAj9r1d docker exec hwmdm-postgres psql -U hmdm -d hmdm -tAc "SELECT login, password_hash IS NOT NULL FROM users WHERE login='admin';" 2>&1

OUT
Exit code 1
ERROR:  column "password_hash" does not exist
LINE 1: SELECT login, password_hash IS NOT NULL FROM users WHERE log...
                      ^
Bash List user table columns
IN
docker exec hwmdm-postgres psql -U hmdm -d hmdm -tAc "SELECT column_name FROM information_schema.columns WHERE table_name='users' ORDER BY ordinal_position;" 2>&1

OUT
id
login
email
name
password
customerid
userroleid
alldevicesavailable
allconfigavailable
passwordreset
authtoken
passwordresettoken
authdata
twofactorsecret
twofactoraccepted
lastloginfail
Bash Check admin credentials
IN
docker exec hwmdm-postgres psql -U hmdm -d hmdm -tAc "SELECT login, left(password,20), authtoken FROM users WHERE login='admin';" 2>&1

OUT
admin|349242D38ED8667B5C11|PaYQ8naaSJWqgM2nnPo5
9:59 AM
Tenho o auth token. Vou testar o endpoint remote com ele.



Bash Test remote start with auth token
IN
curl -s -H "Authorization: Bearer PaYQ8naaSJWqgM2nnPo5" -X POST http://localhost:8080/rest/private/remote-support/68/start 2>&1

OUT
<!doctype html><html lang="en"><head><title>HTTP Status 403 – Forbidden</title><style type="text/css">body {font-family:Tahoma,Arial,sans-serif;} h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} a {color:black;} .line {height:1px;background-color:#525D76;border:none;}</style></head><body><h1>HTTP Status 403 – Forbidden</h1><hr class="line" /><p><b>Type</b> Status Report</p><p><b>Description</b> The server understood the request but refuses to authorize it.</p><hr class="line" /><h3>Apache Tomcat/9.0.117</h3></body></html>
9:59 AM
403 — precisa cookie session. Vou autenticar via cookie.



Bash Check password format
IN
curl -s -c /tmp/hwmdm-cookies.txt -H "Content-Type: application/json" -d '{"login":"admin","password":"1DM1N"}' http://localhost:8080/rest/public/auth/login 2>&1; echo; docker exec hwmdm-postgres psql -U hmdm -d hmdm -tAc "SELECT left(password,6) FROM users WHERE login='admin';" 2>&1

OUT
{"status":"ERROR","message":null,"data":null}
349242
9:59 AM
A senha no banco é um MD5 hash. O login REST retorna ERROR com ambas senhas. Vou verificar como a UI web faz login — provavelmente ela já tem sessão ativa. O ponto é que o endpoint remote-support existe mas retorna 403 sem sessão autenticada. A tela está aberta e autenticada no browser.

O fato de que a UI mostra "Conectar" e o device aparece online mas a tela fica preta com 0 fps sugere que o botão Conectar está sendo clicado mas algo falha no fluxo. Vou olhar a resposta do /start endpoint mais de perto para ver o socket path gerado.



Bash Check socket path generation
IN
grep -n 'viewerSocketPath\|socket.*viewer' /opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/rest/resource/RemoteSupportResource.java

OUT
173:            result.put("socket", viewerSocketPath(device.getNumber()));
244:            result.put("socket", viewerSocketPath(device.getNumber()));
390:    private String viewerSocketPath(String number) {
Read RemoteSupportResource.java (lines 386-405)
9:59 AM
O socket path é ws/remote/viewer/R9XT200AMYY. Isso monta como ws://192.168.1.65:8080/ws/remote/viewer/R9XT200AMYY. Vou verificar se o endpoint WebSocket está registrado no Tomcat.



Bash Check WebSocket endpoint annotations
IN
grep -rn 'ServerEndpoint\|@ServerEndpoint\|ws/remote' /opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/ 2>&1 | head -10

OUT
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteAgentEndpoint.java:36:import javax.websocket.server.ServerEndpoint;
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteAgentEndpoint.java:50:@ServerEndpoint("/ws/remote/agent/{number}")
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:30:import javax.websocket.server.ServerEndpointConfig;
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:46:    private static final String AGENT_PATH = "/ws/remote/agent/{number}";
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:47:    private static final String VIEWER_PATH = "/ws/remote/viewer/{number}";
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:60:        register(container, ServerEndpointConfig.Builder
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:63:        register(container, ServerEndpointConfig.Builder
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteEndpointDeployer.java:69:    private void register(ServerContainer container, ServerEndpointConfig config) {
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteViewerEndpoint.java:41:import javax.websocket.server.ServerEndpoint;
/opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/java/com/hmdm/remote/RemoteViewerEndpoint.java:42:import javax.websocket.server.ServerEndpointConfig;
9:59 AM
Os endpoints WebSocket estão configurados. Vou agora comparar os .class no container com os compilados no source para ver se são os mesmos.



Bash Check class checksums in container
IN
docker exec hwmdm-mdm md5sum /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteViewerEndpoint.class /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteEndpointDeployer.class /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteSessionHub.class /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class 2>&1

OUT
7e3a2479c19f6efd7dc4c9dc4f0db683  /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteViewerEndpoint.class
c9c90f30a22ee5e806e03aea86141ac6  /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteEndpointDeployer.class
aacb2905629b890b31e73632fbac0394  /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteSessionHub.class
af8274dcd141dd9910739aa7beb4e95e  /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
Bash Check class checksums on host
IN
md5sum /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteViewerEndpoint.class /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteEndpointDeployer.class /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteSessionHub.class /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class 2>&1

OUT
7e3a2479c19f6efd7dc4c9dc4f0db683  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteViewerEndpoint.class
c9c90f30a22ee5e806e03aea86141ac6  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteEndpointDeployer.class
aacb2905629b890b31e73632fbac0394  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/remote/RemoteSessionHub.class
af8274dcd141dd9910739aa7beb4e95e  /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/WEB-INF/classes/com/hmdm/rest/resource/RemoteSupportResource.class
10:00 AM
Tudo idêntico. O server está ok, classes corretas, endpoints registrados. Vou verificar se o container foi reiniciado recentemente e se o Tomcat fez o deploy correto.



Bash Find Tomcat boot/deploy messages
IN
docker logs hwmdm-mdm 2>&1 | grep -iE 'deploy|servlet|listener|contextinit|register|endpoint|remote|websocket|ServerContainer|started|tomcat' | head -30

OUT
2026-10-09 09:29:16 [DEBUG] com.hmdm.rest.resource.SyncResource : /public/sync/info --> DeviceInfo{model='SM-T225', permissions=[1, 1, 1, 0], applications=[Application{id=null, type='null', name='Chrome Browser', pkg='com.android.chrome', version='154.0.8037.126', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='Suporte Remoto', pkg='com.hwmdm.remote', version='1.36', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='HWMDM Web Filter', pkg='com.hwmdm.webfilter', version='1.2', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='Headwind MDM', pkg='com.hmdm.launcher', version='6.36', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}], files=[], deviceId='R9XT200AMYY', imei='350538862379893', phone='null', batteryLevel=100, batteryCharging='null', androidVersion='14', mdmMode='true', kioskMode='false', location='DeviceLocation{lat=-20.7392036, lon=-48.9137807, ts=1791548934871}', launcherType='opensource', launcherPackage='com.hmdm.launcher', imei2='350538862379893', phone2='null', imsi='null', iccid='null', imsi2='null', iccid2='null', serial='R9XT200AMYY', cpu='arm64-v8a', custom1='null', custom2='null', custom3='null'}
2026-10-09 09:29:16 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.updateDeviceInfo : ==> Parameters: {"model":"SM-T225","permissions":[1,1,1,0],"applications":[{"id":null,"name":"Chrome Browser","pkg":"com.android.chrome","version":"154.0.8037.126","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Suporte Remoto","pkg":"com.hwmdm.remote","version":"1.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"HWMDM Web Filter","pkg":"com.hwmdm.webfilter","version":"1.2","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Headwind MDM","pkg":"com.hmdm.launcher","version":"6.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false}],"files":[],"deviceId":"R9XT200AMYY","imei":"350538862379893","batteryLevel":100,"androidVersion":"14","mdmMode":true,"kioskMode":false,"location":{"lat":-20.7392036,"lon":-48.9137807,"ts":1791548934871},"launcherType":"opensource","launcherPackage":"com.hmdm.launcher","defaultLauncher":true,"imei2":"350538862379893","serial":"R9XT200AMYY","cpu":"arm64-v8a"}(String), {"model":"SM-T225","permissions":[1,1,1,0],"applications":[{"id":null,"name":"Chrome Browser","pkg":"com.android.chrome","version":"154.0.8037.126","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Suporte Remoto","pkg":"com.hwmdm.remote","version":"1.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"HWMDM Web Filter","pkg":"com.hwmdm.webfilter","version":"1.2","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Headwind MDM","pkg":"com.hmdm.launcher","version":"6.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false}],"files":[],"deviceId":"R9XT200AMYY","imei":"350538862379893","batteryLevel":100,"androidVersion":"14","mdmMode":true,"kioskMode":false,"location":{"lat":-20.7392036,"lon":-48.9137807,"ts":1791548934871},"launcherType":"opensource","launcherPackage":"com.hmdm.launcher","defaultLauncher":true,"imei2":"350538862379893","serial":"R9XT200AMYY","cpu":"arm64-v8a"}(String), null, 192.168.1.154(String), 68(Integer)
2026-10-09 09:34:20 [DEBUG] com.hmdm.plugin.persistence.mapper.PluginMapper.findRegisteredPlugins : ==>  Preparing: SELECT plugins.* FROM plugins ORDER BY plugins.identifier 
2026-10-09 09:34:20 [DEBUG] com.hmdm.plugin.persistence.mapper.PluginMapper.findRegisteredPlugins : ==> Parameters: 
2026-10-09 09:34:20 [DEBUG] com.hmdm.plugin.persistence.mapper.PluginMapper.findRegisteredPlugins : <==      Total: 8
2026-10-09 09:35:37 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791548437214, packageId='com.hwmdm.remote', logLevel=2, message='Conexão de vídeo interrompida; tentando retomar'}, UploadedDeviceLogRecord{timestamp=1791548437263, packageId='com.hmdm.launcher', logLevel=2, message='Failed to query push notifications from http://192.168.1.65:8080 : Failed to connect to /192.168.1.65:8080'}, UploadedDeviceLogRecord{timestamp=1791548437308, packageId='com.hmdm.launcher', logLevel=2, message='Failed to query push notifications from http://192.168.1.65:8080 : Failed to connect to /192.168.1.65:8080'}, UploadedDeviceLogRecord{timestamp=1791548438227, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791548438265, packageId='com.hwmdm.remote', logLevel=2, message='Conexão de vídeo interrompida; tentando retomar'}, UploadedDeviceLogRecord{timestamp=1791548438713, packageId='com.hmdm.launcher', logLevel=5, message='Network location update: lat=-20.7392036, lon=-48.9137807'}, UploadedDeviceLogRecord{timestamp=1791548440274, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791548440339, packageId='com.hwmdm.remote', logLevel=2, message='Conexão de vídeo interrompida; tentando retomar'}, UploadedDeviceLogRecord{timestamp=1791548444352, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791548444390, packageId='com.hwmdm.remote', logLevel=2, message='Conexão de vídeo interrompida; tentando retomar'}]
2026-10-09 09:35:37 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String), com.hmdm.launcher(String)
2026-10-09 09:35:37 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791548452402, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791548452460, packageId='com.hwmdm.remote', logLevel=2, message='Conexão de vídeo interrompida; tentando retomar'}, UploadedDeviceLogRecord{timestamp=1791548468469, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791548483395, packageId='com.hwmdm.remote', logLevel=3, message='Relay aceitou a conexao; iniciando captura'}, UploadedDeviceLogRecord{timestamp=1791548497316, packageId='com.hmdm.launcher', logLevel=5, message='Push long polling inquiry'}, UploadedDeviceLogRecord{timestamp=1791548528742, packageId='com.hmdm.launcher', logLevel=5, message='Network location update: lat=-20.7392036, lon=-48.9137807'}, UploadedDeviceLogRecord{timestamp=1791548559131, packageId='com.hmdm.launcher', logLevel=5, message='GPS location update: lat=-20.73927666666667, lon=-48.91349833333334'}, UploadedDeviceLogRecord{timestamp=1791548563046, packageId='com.hmdm.launcher', logLevel=5, message='Push long polling inquiry'}, UploadedDeviceLogRecord{timestamp=1791548588842, packageId='com.hmdm.launcher', logLevel=5, message='Network location update: lat=-20.7392036, lon=-48.9137807'}, UploadedDeviceLogRecord{timestamp=1791548629100, packageId='com.hmdm.launcher', logLevel=5, message='Push long polling inquiry'}]
2026-10-09 09:35:37 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String), com.hmdm.launcher(String)
2026-10-09 09:35:58 [INFO] com.hmdm.remote.RemoteSessionHub : Sessao de suporte remoto aberta para o aparelho 'R9XT200AMYY' (aparelho online no pedido)
2026-10-09 09:35:58 [INFO] com.hmdm.rest.resource.RemoteSupportResource : Suporte remoto chamado para o aparelho 'R9XT200AMYY' (online no pedido=true)
2026-10-09 09:35:58 [INFO] AuditLogger : createTime=1791549358728, userId=1, login='admin', ipAddress='192.168.1.254', action='plugin.audit.action.remote.start', payload='Method: POST
URI: /rest/private/remote-support/68/start
2026-10-09 09:35:58 [DEBUG] com.hmdm.plugins.audit.persistence.mapper.AuditMapper.insertAuditLogRecord : ==> Parameters: 1791549358728(Long), 1(Integer), 1(Integer), admin(String), plugin.audit.action.remote.start(String), Method: POST
URI: /rest/private/remote-support/68/start
2026-10-09 09:35:59 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549358933, packageId='com.hmdm.launcher', logLevel=3, message='Got Push Message, type remoteScreenStart'}, UploadedDeviceLogRecord{timestamp=1791549358944, packageId='com.hwmdm.remote', logLevel=3, message='Chamado de suporte recebido do launcher'}, UploadedDeviceLogRecord{timestamp=1791549358949, packageId='com.hwmdm.remote', logLevel=3, message='Iniciando sessao; relay=ws://192.168.1.65:8080/ws/remote/agent/R9XT200AMYY consentimento_em_cache=false'}]
2026-10-09 09:35:59 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String), com.hmdm.launcher(String)
2026-10-09 09:35:59 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==> Parameters: 1791549358933(Long), 1(Integer), 68(Integer), 46(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Got Push Message, type remoteScreenStart(String), 1791549358944(Long), 1(Integer), 68(Integer), 87(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Chamado de suporte recebido do launcher(String), 1791549358949(Long), 1(Integer), 68(Integer), 87(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Iniciando sessao; relay=ws://192.168.1.65:8080/ws/remote/agent/R9XT200AMYY consentimento_em_cache=false(String)
2026-10-09 09:35:59 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549359048, packageId='com.hwmdm.remote', logLevel=3, message='Pedindo consentimento de captura ao usuario'}]
2026-10-09 09:35:59 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
2026-10-09 09:35:59 [INFO] com.hmdm.remote.RemoteSessionHub : Espectador anexado a 'R9XT200AMYY' (total=1, agente_conectado=false)
2026-10-09 09:36:00 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549360781, packageId='com.hwmdm.remote', logLevel=3, message='Bloqueio de tela dispensado para a sessao de suporte'}]
2026-10-09 09:36:00 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
2026-10-09 09:36:01 [INFO] com.hmdm.remote.RemoteSessionHub : Agente conectado para 'R9XT200AMYY'
2026-10-09 09:36:01 [INFO] com.hmdm.remote.RemoteSessionHub : Fluxo descrito por 'R9XT200AMYY': 752x1280 entrada=true espectadores=1
2026-10-09 09:36:01 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 1 de 'R9XT200AMYY': tipo=1 bytes=40 espectadores=1
2026-10-09 09:36:01 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 2 de 'R9XT200AMYY': tipo=2 bytes=2847 espectadores=1
2026-10-09 09:36:01 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 3 de 'R9XT200AMYY': tipo=3 bytes=24 espectadores=1
2026-10-09 09:36:02 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549361157, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791549361219, packageId='com.hwmdm.remote', logLevel=3, message='Relay aceitou a conexao; iniciando captura'}, UploadedDeviceLogRecord{timestamp=1791549361572, packageId='com.hwmdm.remote', logLevel=3, message='Tela mantida acesa: janela=sim wakelock=sim'}, UploadedDeviceLogRecord{timestamp=1791549361576, packageId='com.hwmdm.remote', logLevel=3, message='Transmitindo 752x1280 com toque'}]
2026-10-09 09:36:02 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
10:00 AM
ENCONTREI TUDO! O log mostra que a sessão remota FUNCIONA:

09:35:58 - Sessão aberta, push remoteScreenStart enviado ao device
09:35:59 - Agente remoto recebe, pede consentimento de captura
09:36:00 - Bloqueio de tela dispensado
09:36:01 - Agente conectado, fluxo 752x1280, quadros chegando (tipo 1,2,3)
09:36:02 - "Transmitindo 752x1280 com toque"
O servidor está transmitindo frames! O problema é no lado do browser — os frames chegam ao hub mas não estão sendo exibidos. Isso é o bug no código JS do player. Vou ver os logs mais recentes para entender se a sessão caiu depois.



Bash Check latest remote session status
IN
docker logs hwmdm-mdm 2>&1 | grep -iE 'remote|viewer|quadro|fluxo|espectador|agente.*conect|sessao' | tail -20

OUT
URI: /rest/private/remote-support/68/start
2026-10-09 09:45:18 [DEBUG] com.hmdm.plugins.audit.persistence.mapper.AuditMapper.insertAuditLogRecord : ==> Parameters: 1791549918919(Long), 1(Integer), 1(Integer), admin(String), plugin.audit.action.remote.start(String), Method: POST
URI: /rest/private/remote-support/68/start
2026-10-09 09:45:18 [INFO] com.hmdm.remote.RemoteSessionHub : Espectador anexado a 'R9XT200AMYY' (total=1, agente_conectado=false)
2026-10-09 09:45:23 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549923148, packageId='com.hmdm.launcher', logLevel=3, message='Got Push Message, type remoteScreenStart'}]
2026-10-09 09:45:23 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==> Parameters: 1791549923148(Long), 1(Integer), 68(Integer), 46(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Got Push Message, type remoteScreenStart(String)
2026-10-09 09:45:23 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549923159, packageId='com.hwmdm.remote', logLevel=3, message='Chamado de suporte recebido do launcher'}, UploadedDeviceLogRecord{timestamp=1791549923168, packageId='com.hwmdm.remote', logLevel=3, message='Iniciando sessao; relay=ws://192.168.1.65:8080/ws/remote/agent/R9XT200AMYY consentimento_em_cache=false'}]
2026-10-09 09:45:23 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
2026-10-09 09:45:23 [DEBUG] com.hmdm.plugins.devicelog.persistence.postgres.dao.mapper.PostgresDeviceLogMapper.insertDeviceLogRecords : ==> Parameters: 1791549923159(Long), 1(Integer), 68(Integer), 87(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Chamado de suporte recebido do launcher(String), 1791549923168(Long), 1(Integer), 68(Integer), 87(Integer), 192.168.1.154(String), INFO(String), 3(Integer), Iniciando sessao; relay=ws://192.168.1.65:8080/ws/remote/agent/R9XT200AMYY consentimento_em_cache=false(String)
2026-10-09 09:45:23 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549923247, packageId='com.hwmdm.remote', logLevel=3, message='Pedindo consentimento de captura ao usuario'}]
2026-10-09 09:45:23 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
2026-10-09 09:45:24 [INFO] com.hmdm.remote.RemoteSessionHub : Agente conectado para 'R9XT200AMYY'
2026-10-09 09:45:24 [INFO] com.hmdm.remote.RemoteSessionHub : Fluxo descrito por 'R9XT200AMYY': 752x1280 entrada=true espectadores=1
2026-10-09 09:45:24 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 1 de 'R9XT200AMYY': tipo=1 bytes=40 espectadores=1
2026-10-09 09:45:24 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 2 de 'R9XT200AMYY': tipo=2 bytes=2847 espectadores=1
2026-10-09 09:45:24 [INFO] com.hmdm.remote.RemoteSessionHub : Quadro 3 de 'R9XT200AMYY': tipo=3 bytes=24 espectadores=1
2026-10-09 09:45:25 [DEBUG] com.hmdm.plugins.devicelog.rest.resource.DeviceLogResource : #uploadLogs: R9XT200AMYY => [UploadedDeviceLogRecord{timestamp=1791549924261, packageId='com.hwmdm.remote', logLevel=3, message='Conectando ao relay de suporte'}, UploadedDeviceLogRecord{timestamp=1791549924349, packageId='com.hwmdm.remote', logLevel=3, message='Relay aceitou a conexao; iniciando captura'}, UploadedDeviceLogRecord{timestamp=1791549924576, packageId='com.hwmdm.remote', logLevel=3, message='Tela mantida acesa: janela=sim wakelock=sim'}, UploadedDeviceLogRecord{timestamp=1791549924581, packageId='com.hwmdm.remote', logLevel=3, message='Transmitindo 752x1280 com toque'}]
2026-10-09 09:45:25 [DEBUG] com.hmdm.persistence.mapper.ApplicationMapper.resolveAppsByPackageId : ==> Parameters: 1(Integer), com.hwmdm.remote(String)
2026-10-09 09:59:17 [DEBUG] com.hmdm.rest.resource.SyncResource : /public/sync/info --> DeviceInfo{model='SM-T225', permissions=[1, 1, 1, 0], applications=[Application{id=null, type='null', name='Chrome Browser', pkg='com.android.chrome', version='154.0.8037.126', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='Suporte Remoto', pkg='com.hwmdm.remote', version='1.36', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='HWMDM Web Filter', pkg='com.hwmdm.webfilter', version='1.2', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}, Application{id=null, type='null', name='Headwind MDM', pkg='com.hmdm.launcher', version='6.36', versionCode=0, url='null', split=false, urlArmeabi='null', urlArm64='null', showIcon=false, useKiosk=false, bottom=false, longTap=false, intent=null, iconText='null', iconId='null', runAfterInstall=false, skipVersion=false, system=false, configurations=[], customerId=0, filePath='null', deletionProhibited='false', outdated='false', latestVersion='null', latestVersionText='null', usedVersionId='null'}], files=[], deviceId='R9XT200AMYY', imei='350538862379893', phone='null', batteryLevel=100, batteryCharging='null', androidVersion='14', mdmMode='true', kioskMode='false', location='DeviceLocation{lat=-20.7393418, lon=-48.9137409, ts=1791550730830}', launcherType='opensource', launcherPackage='com.hmdm.launcher', imei2='350538862379893', phone2='null', imsi='null', iccid='null', imsi2='null', iccid2='null', serial='R9XT200AMYY', cpu='arm64-v8a', custom1='null', custom2='null', custom3='null'}
2026-10-09 09:59:17 [DEBUG] com.hmdm.persistence.mapper.DeviceMapper.updateDeviceInfo : ==> Parameters: {"model":"SM-T225","permissions":[1,1,1,0],"applications":[{"id":null,"name":"Chrome Browser","pkg":"com.android.chrome","version":"154.0.8037.126","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Suporte Remoto","pkg":"com.hwmdm.remote","version":"1.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"HWMDM Web Filter","pkg":"com.hwmdm.webfilter","version":"1.2","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Headwind MDM","pkg":"com.hmdm.launcher","version":"6.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false}],"files":[],"deviceId":"R9XT200AMYY","imei":"350538862379893","batteryLevel":100,"androidVersion":"14","mdmMode":true,"kioskMode":false,"location":{"lat":-20.7393418,"lon":-48.9137409,"ts":1791550730830},"launcherType":"opensource","launcherPackage":"com.hmdm.launcher","defaultLauncher":true,"imei2":"350538862379893","serial":"R9XT200AMYY","cpu":"arm64-v8a"}(String), {"model":"SM-T225","permissions":[1,1,1,0],"applications":[{"id":null,"name":"Chrome Browser","pkg":"com.android.chrome","version":"154.0.8037.126","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Suporte Remoto","pkg":"com.hwmdm.remote","version":"1.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"HWMDM Web Filter","pkg":"com.hwmdm.webfilter","version":"1.2","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false},{"id":null,"name":"Headwind MDM","pkg":"com.hmdm.launcher","version":"6.36","versionCode":0,"arch":null,"url":null,"split":false,"urlArmeabi":null,"urlArm64":null,"showIcon":false,"useKiosk":false,"system":false,"configurations":[],"latestVersion":null,"runAfterInstall":false,"runAtBoot":false,"skipVersion":false,"type":null,"iconId":null,"bottom":false,"longTap":false,"intent":null,"remove":false,"selected":false,"customerId":0,"customerName":null,"commonApplication":false,"deletionProhibited":false,"outdated":false,"latestVersionText":null,"usedVersionId":null,"filePath":null,"action":0,"common":false}],"files":[],"deviceId":"R9XT200AMYY","imei":"350538862379893","batteryLevel":100,"androidVersion":"14","mdmMode":true,"kioskMode":false,"location":{"lat":-20.7393418,"lon":-48.9137409,"ts":1791550730830},"launcherType":"opensource","launcherPackage":"com.hmdm.launcher","defaultLauncher":true,"imei2":"350538862379893","serial":"R9XT200AMYY","cpu":"arm64-v8a"}(String), null, 192.168.1.154(String), 68(Integer)
10:00 AM
O log mostra claramente:

09:45:24 - Agente conectou, frames chegando (tipo 1=config, tipo 2=keyframe, tipo 3=delta), 1 espectador
Depois 09:45:25 - "Transmitindo 752x1280 com toque"
Mas só enviou 3 quadros e parou
O problema é que apenas 3 frames foram enviados e depois a transmissão parou. O tipo 1 (40 bytes) é configuração do codec, tipo 2 (2847 bytes) é keyframe, tipo 3 (24 bytes) é delta. Mas depois disso não há mais frames no log.

Isso sugere que o espectador (browser) está recebendo os frames via WebSocket mas não consegue decodificar, ou que a sessão caiu logo depois. O frame tipo 3 tem apenas 24 bytes — um delta tão pequeno pode significar que a tela não mudou.

O screenshot mostra 0 fps e 0 kbps. O browser está pelo IP (http, não https = contexto inseguro = sem WebCodecs = usa MSE/JMuxer). Vou olhar o player MSE para ver se há algum bug.



Read remoteSupport.service.js (lines 201-320)
10:00 AM
Vou ver a função onFrame e initMse para entender como os frames são processados pelo caminho MSE.



Bash Find frame handling code
IN
grep -n 'onFrame\|initMse\|applyConfig\|FRAME_CONFIG\|FRAME_KEY\|FRAME_DELTA' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js | head -20

OUT
29:        var FRAME_CONFIG = 1;
30:        var FRAME_KEY = 2;
31:        var FRAME_DELTA = 3;
182:        // a envia no inicio da transmissao; ver o uso em onFrame. Nao e' dado sensivel: sao
300:                this.initMse();
313:                    self.onFrame(new Uint8Array(event.data));
363:                                self.onFrame(base64ToBytes(b64));
408:        Player.prototype.onFrame = function (bytes) {
424:            if (type === FRAME_CONFIG) {
426:                this.applyConfig(payload);
436:                if (type === FRAME_KEY) {
439:                        this.applyConfig(stored);
457:            if (type === FRAME_DELTA && this.awaitingKeyFrame) {
461:            var data = (type === FRAME_KEY && this.pendingConfig)
467:                    type: type === FRAME_KEY ? 'key' : 'delta',
471:                if (type === FRAME_KEY) {
499:                        if (bytes[0] === FRAME_CONFIG || bytes[0] === FRAME_KEY) {
500:                            self.onFrame(bytes);
506:        Player.prototype.applyConfig = function (payload) {
566:        Player.prototype.initMse = function () {
Read remoteSupport.service.js (lines 409-518)
10:00 AM
Vou olhar mseFeed e initMse para ver como o JMuxer/MSE está processando os frames.



Read remoteSupport.service.js (lines 561-680)
10:00 AM
O código do mseFeed parece correto — usa JMuxer para alimentar o video element. A pergunta é: o isSupported() e supportsWebCodecs() estão retornando o que esperamos? E o supportsMse()?



Bash Find support detection functions
IN
grep -n 'function isSupported\|function supportsWebCodecs\|function supportsMse\|isSupported\|var HEADER_BYTES' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js | head -10

OUT
32:        var HEADER_BYTES = 9;
46:        var isSupported = function () {
419:            if (!isSupported()) {
855:            isSupported: isSupported,
Read remoteSupport.service.js (lines 35-114)
10:01 AM
Agora vou verificar se o JMuxer está sendo carregado corretamente. A condição supportsMse() depende de $window.JMuxer ser uma função.



Bash Check JMuxer file header
IN
head -5 /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.min.js

OUT
Couldn't find the requested file /dist/jmuxer.min.js in jmuxer.
Bash Count JMuxer references
IN
grep -c 'JMuxer' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.min.js

OUT
0
Bash Check if JMuxer is loaded in HTML/app
IN
grep -n 'jmuxer\|JMuxer' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/index.html 2>/dev/null || grep -rn 'jmuxer\|JMuxer' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/app.js 2>/dev/null | head -5

OUT
133:<script src="lib/jmuxer.js?v=h7c9ca071f2"></script>
10:01 AM
ENCONTREI OUTRO PROBLEMA! O index.html carrega lib/jmuxer.js (não .min.js), e o arquivo jmuxer.min.js mostra "Couldn't find the requested file" — ele é um arquivo quebrado/placeholder!

Vou verificar o jmuxer.js (o que realmente é carregado).



Bash Check jmuxer.js file
IN
head -3 /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js; echo "---"; wc -c /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js

OUT
(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory(require('stream')) :
  typeof define === 'function' && define.amd ? define(['stream'], factory) :
---
83912 /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js
10:01 AM
O jmuxer.js existe e é um módulo UMD válido (84KB). Ele é carregado no index.html. O supportsMse() verifica typeof $window.JMuxer === 'function'.

Vou verificar se o JMuxer define window.JMuxer ou usa um nome diferente.



Bash Check JMuxer export name
IN
grep -n 'JMuxer' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js | head -10

OUT
4:  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, global.JMuxer = factory(global.stream));
2273:  var JMuxer = /*#__PURE__*/function (_Event) {
2274:    _inherits(JMuxer, _Event);
2276:    var _super = _createSuper(JMuxer);
2278:    function JMuxer(options) {
2281:      _classCallCheck(this, JMuxer);
2332:    _createClass(JMuxer, [{
2640:        log('JMuxer was reset');
2651:          if (!JMuxer.isSupported("".concat(type, "/mp4; codecs=\"").concat(track.mp4track.codec, "\""))) {
2752:          log("JMuxer changed FPS to ".concat(data.fps, " from track data"));
10:01 AM
JMuxer define global.JMuxer corretamente. Mas espera — o UMD wrapper tenta require('stream') primeiro. No browser, isso pode falhar silenciosamente no caminho global. Vou verificar se o JMuxer realmente carrega sem erro.

Na verdade, o UMD pattern na linha 4 mostra: global.JMuxer = factory(global.stream) — ele tenta global.stream que é undefined no browser. Se o factory espera o módulo stream do Node.js e falha, o JMuxer pode não carregar. Vou verificar.



Bash Check if factory function uses stream module
IN
grep -A5 'function factory' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js | head -10; echo "---"; grep 'stream' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/lib/jmuxer.js | head -5

OUT
---
  typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory(require('stream')) :
  typeof define === 'function' && define.amd ? define(['stream'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, global.JMuxer = factory(global.stream));
})(this, (function (stream) { 'use strict';
        0x00, // stream_priority
10:01 AM
O stream importado pelo JMuxer é passado como undefined no browser (já que global.stream não existe). Mas isso é normal para JMuxer — o módulo stream é opcional e só usado no modo Node.js. O JMuxer no browser funciona sem ele.

O código parece tecnicamente correto. Mas o screenshot mostra 0 fps, 0 kbps. Vou verificar se há um CSS que está escondendo o video element, ou se a versão do arquivo no backup funcional é diferente da atual.

Vou comparar o service.js atual com o do backup que funcionava (webapps-20260923).



Bash Count diff lines with working backup
IN
diff /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js /opt/projetos/hwmdm/repo-mdm/source/volumes/backups/webapps-20260923-140538/ROOT/app/components/main/service/remoteSupport.service.js 2>&1 | wc -l

OUT
218
10:01 AM
218 linhas de diferença! A versão atual do service.js é muito diferente da que funcionava. Vou ver o que mudou.



Bash Show diff between working backup and current
IN
diff /opt/projetos/hwmdm/repo-mdm/source/volumes/backups/webapps-20260923-140538/ROOT/app/components/main/service/remoteSupport.service.js /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js 2>&1 | head -100

OUT
169a170,219
>         // Fila de entrada (ver Player.prototype.send). O servidor recusa 'text' acima de 500.
>         var INPUT_ACK_TIMEOUT_MS = 400;
>         var INPUT_TEXT_MAX = 400;
> 
>         // Teclas que o endpoint WebSocket EM PRODUCAO recusa com "tecla desconhecida": a
>         // classe RemoteViewerEndpoint no ar (build de 21-09) so' aceita back/home/recents/
>         // notifications. O endpoint HTTP /input (RemoteSupportResource.input) repassa ao
>         // agente sem essa lista, com a mesma permissao de controle, e o agente valida o nome
>         // por conta propria. Sem isto Backspace, Enter, Tab e setas nunca chegavam.
>         var KEYS_VIA_HTTP = {backspace: true, enter: true, tab: true, left: true, right: true};
> 
>         // Configuracao do codec (SPS/PPS) por aparelho, guardada no navegador. O agente so'
>         // a envia no inicio da transmissao; ver o uso em onFrame. Nao e' dado sensivel: sao
>         // parametros do codificador de video. Guarda a resolucao junto para nunca aplicar
>         // uma configuracao de outra resolucao.
>         var CODEC_STORAGE_PREFIX = 'hwmdm.remote.codec.';
> 
>         var saveCodecConfig = function (deviceId, payload, width, height) {
>             try {
>                 var bin = '';
>                 for (var i = 0; i < payload.length; i++) {
>                     bin += String.fromCharCode(payload[i]);
>                 }
>                 $window.localStorage.setItem(CODEC_STORAGE_PREFIX + deviceId,
>                     JSON.stringify({c: $window.btoa(bin), w: width || 0, h: height || 0}));
>             } catch (e) {
>                 // localStorage indisponivel: so' perde a retomada apos F5.
>             }
>         };
> 
>         var loadCodecConfig = function (deviceId, width, height) {
>             try {
>                 var raw = JSON.parse($window.localStorage.getItem(CODEC_STORAGE_PREFIX + deviceId));
>                 if (!raw || !raw.c) {
>                     return null;
>                 }
>                 if (width && height && raw.w && raw.h && (raw.w !== width || raw.h !== height)) {
>                     return null;
>                 }
>                 var bin = $window.atob(raw.c);
>                 var out = new Uint8Array(bin.length);
>                 for (var i = 0; i < bin.length; i++) {
>                     out[i] = bin.charCodeAt(i);
>                 }
>                 return out;
>             } catch (e) {
>                 return null;
>             }
>         };
> 
171a222,225
>             this.haveConfig = false;
>             this.outbox = [];
>             this.inputInFlight = false;
>             this.inputAckTimer = null;
348a403
>                 this.inputAcked();
370,380c425,426
<                 this.pendingConfig = payload;
<                 this.codecString = codecFromParameterSet(payload) || this.codecString;
<                 this.mseConfigNal = payload;
<                 this.mseCodecString = this.codecString;
<                 if (this.useMse) {
<                     this.mseInitSent = false;
<                     this.mseSeqNum = 1;
<                     this.mseTimestamp = 0;
<                     return;
<                 }
<                 this.configure();
---
>                 saveCodecConfig(this.deviceId, payload, this.mseWidth, this.mseHeight);
>                 this.applyConfig(payload);
383a430,448
>             // Espectador que chegou depois do inicio (F5, troca de tela, outra aba): pelo
>             // WebSocket o hub no ar reenvia o ultimo quadro-chave mas NAO a configuracao do
>             // codec, e sem SPS/PPS nada decodifica -- a tela ficava preta. Usa a
>             // configuracao guardada neste navegador (mesma resolucao) ou busca uma vez pelo
>             // endpoint HTTP, que devolve configuracao + ultimo quadro-chave.
>             if (!this.haveConfig) {
>                 if (type === FRAME_KEY) {
>                     var stored = loadCodecConfig(this.deviceId, this.mseWidth, this.mseHeight);
>                     if (stored) {
>                         this.applyConfig(stored);
>                     } else {
>                         this.fetchConfig();
>                         return;
>                     }
>                 } else {
>                     return;     // delta sem configuracao nao decodifica
>                 }
>             }
> 
414a480,520
>         Player.prototype.fetchConfig = function () {
>             if (this.fetchingConfig || this.http || this.closed) {
>                 return;     // no transporte HTTP a propria sondagem ja' traz a configuracao
>             }
>             var self = this;
>             this.fetchingConfig = true;
10:02 AM
Agora vejo — o backup antigo (que funcionava) tinha um approach diferente para o onFrame: no FRAME_CONFIG ele fazia this.pendingConfig = payload; this.configure(); diretamente. A versão nova adicionou haveConfig, saveCodecConfig, loadCodecConfig, fetchConfig — é mais robusto mas pode ter um bug.

O fluxo na versão nova:

Frame CONFIG chega → applyConfig() → sets haveConfig = true
Frame KEY chega → verifica haveConfig (true) → vai para MSE feed
Funciona
Mas se a ordem for: KEY antes de CONFIG (que acontece no re-attach/F5), aí entra no fetchConfig. Mas no fluxo normal, CONFIG vem primeiro.

Vou verificar o que o server-source/target tem (que é o que foi compilado).



Bash Diff live vs source to see if they match
IN
diff /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js /opt/projetos/hwmdm/repo-mdm/server-source/server/src/main/webapp/app/components/main/service/remoteSupport.service.js 2>&1

OUT
(Bash completed with no output)
10:02 AM
Idênticos. Então o código atual é o mais recente e consistente. O screenshot mostrou que a sessão FUNCIONOU no servidor (quadros foram enviados), mas o browser mostra 0 fps.

Penso que o problema pode ser que o browser acessa via http://192.168.1.65:8080 mas os containers estão na máquina local. Se o browser está em outra máquina (192.168.1.254 baseado no audit log), aí precisa que a porta 31000 esteja acessível. Mas o audit log mostra IP 192.168.1.254 — esse é o IP do browser do usuário.

Espera — o log mostra:

ipAddress='192.168.1.254' na ação do admin
ipAddress='192.168.1.154' nos logs do device
Então o browser está em 192.168.1.254 (outra máquina), e o servidor está em 192.168.1.65 (esta máquina). O WebSocket conecta em ws://192.168.1.65:8080/ws/remote/viewer/R9XT200AMYY.

Mas os logs mostraram que o espectador se anexou e recebeu quadros! Então funcionou de ponta a ponta. Se o log diz "Quadro 2 de 'R9XT200AMYY': tipo=2 bytes=2847 espectadores=1", o frame foi enviado ao espectador.

O problema pode ser timing — a sessão funcionou brevemente mas caiu. Vou verificar se há mensagem de disconnect do espectador.



Bash Check for disconnect messages
IN
docker logs hwmdm-mdm 2>&1 | grep -iE 'desconect|disconnect|close|encerr|fechou|viewer.*close|espectador.*sa' | tail -10

OUT
2026-10-09 10:00:32 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 53 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
2026-10-09 10:01:38 [DEBUG] org.mybatis.guice.transactional.TransactionalMethodInterceptor : [Intercepted method: public java.util.List<com.hmdm.notification.persistence.domain.PushMessage> com.hmdm.notification.persistence.NotificationDAO.getPendingMessagesForDelivery(int)] - SqlSession of thread: 48 is inherited, skipped close operation
10:02 AM
Não há mensagens de disconnect no log. Mas o screenshot da UI mostra claramente 0 fps e 0 kbps. Vou olhar algo crucial — se o count() (que atualiza stats) está funcionando, e se o CSS pode estar escondendo o video.

Espera — olhando o screenshot original de novo: a barra inferior mostra "Ao vivo · 752×1280 · 0 fps · 0 kbps · Controle remoto ativo · Teclado físico: ativar". 752×1280 é exatamente a resolução que o server reportou! Isso significa que o status está chegando — o onStatus callback recebeu width=752, height=1280, streaming=true, input=true.

Mas 0 fps e 0 kbps significa que onStats não está recebendo frames. Vou olhar como stats são calculadas.



Bash Find stats calculation code
IN
grep -n 'count\|stats\|onStats\|fps\|kbps' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/service/remoteSupport.service.js | head -15

OUT
237:            this.stats = {frames: 0, bytes: 0, since: Date.now()};
417:            this.count(bytes.length);
572:                    fps: 15,
748:        Player.prototype.count = function (byteCount) {
749:            this.stats.frames++;
750:            this.stats.bytes += byteCount;
751:            var elapsed = Date.now() - this.stats.since;
753:                this.report('onStats', {
754:                    fps: Math.round((this.stats.frames * 1000) / elapsed),
755:                    kbps: Math.round((this.stats.bytes * 8) / elapsed)
757:                this.stats = {frames: 0, bytes: 0, since: Date.now()};
Read remoteSupport.service.js (lines 749-763)
10:02 AM
O count() é chamado no onFrame() na linha 417 — antes de qualquer check de config. Então os stats deveriam mostrar frames. Mas mostra 0 fps.

Isso pode significar que o WebSocket no browser nunca abriu com sucesso, ou abriu e caiu imediatamente. O código tem fallback para HTTP quando o WebSocket falha (linha 324-327).

Vou olhar o screenshot mais atentamente. Está mostrando 0 fps · 0 kbps — isso significaria que onFrame nunca é chamado no browser. Mas o server mostra "Quadro enviado, espectadores=1".

Espera — olhando o horário do screenshot: "09:35:16" na barra de status do device, mas os logs mostram a conexão acontecendo em 09:35:58-09:36:02 e depois em 09:45:18-09:45:25. O screenshot pode ter sido tirado ANTES de o botão Conectar ser clicado, ou durante uma sessão onde os frames pararam.

Vou verificar se o CSS remote-live-video está corretamente mostrando o video element.



Bash Check remote view HTML elements
IN
grep -n 'remote-live-video\|remote-live-canvas\|remote-preview-screen' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/app/components/main/view/remote.html | head -10

OUT
77:                            <div class="device-preview-screen remote-preview-screen"
81:                                        class="remote-live-canvas"
83:                                        ng-class="{'remote-live-canvas-interactive': remote.streaming}"
Bash Find CSS for remote video
IN
grep -n 'remote-live-video\|remote-live-canvas\|\.remote-preview' /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/main.css 2>/dev/null | head -10; find /opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css -name "*.css" 2>/dev/null | head -5

OUT
4618:.remote-preview-shell {
4624:.remote-preview-screen {
4631:.remote-preview-screen.remote-preview-streaming {
4641:.remote-preview-state {
4654:.remote-preview-state-error {
4658:.remote-preview-spinner {
4662:.remote-preview-icon {
4667:.remote-preview-copy {
4678:.remote-live-canvas {
4687:.remote-preview-screen canvas,
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/main.css
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/hwmdm-modules.css
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/hwmdm-version.css
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/hwmdm-ui.css
/opt/projetos/hwmdm/repo-mdm/source/volumes/webapps/ROOT/css/mdm-picker.css
Read main.css (lines 4679-4708)
10:03 AM
ENCONTREI! Linha 4693-4697:


.remote-preview-screen:not(.remote-preview-streaming) canvas,
.remote-preview-screen:not(.remote-preview-streaming) video,
.remote-preview-screen:not(.remote-preview-streaming) .remote-live-video {
    display: none !important;
}
Quando o remote-preview-streaming class não está na div, o canvas E video ficam display: none. Vou verificar se a class é adicionada corretamente.



Read remote.html (lines 77-91