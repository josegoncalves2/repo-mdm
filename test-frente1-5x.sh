#!/bin/bash
# Teste Frente 1: IP / Layout / Kiosk Rollout — 5 rodadas cada

set +e

TESTS_PASSED=0
TESTS_FAILED=0
LOOP_COUNT=5
KNOWN_PROXY_IPS="10.0.17.106,10.0.9.1,10.1.1.1"
KIOSK_APK="hmdm-6.37.3-kiosk.apk"

log_pass() {
  echo "✓ $@"
  ((TESTS_PASSED++))
}

log_fail() {
  echo "✗ $@"
  ((TESTS_FAILED++))
}

test_proxy_config() {
  local i=1
  echo ""
  echo "=== TESTE 1: proxy.addresses configurado (5 rodadas) ==="

  while [ $i -le $LOOP_COUNT ]; do
    echo "Rodada $i:"

    if grep -q 'proxy.addresses" value="10.0.17.106,10.0.9.1,10.1.1.1"' /home/jclabs/projetos/hwmdm/source/templates/conf/context_template.xml; then
      log_pass "  Template context_template.xml OK"
    else
      log_fail "  Template proxy.addresses vazio/errado"
    fi

    if grep -q 'proxy.addresses" value="10.0.17.106,10.0.9.1,10.1.1.1"' /home/jclabs/projetos/hwmdm/source/volumes/hmdm-config/ROOT.xml; then
      log_pass "  Live ROOT.xml OK"
    else
      log_fail "  Live proxy.addresses vazio/errado"
    fi

    ((i++))
  done
}

test_layout_fix() {
  local i=1
  echo ""
  echo "=== TESTE 2: docker-entrypoint.sh tem fix de layout (5 rodadas) ==="

  while [ $i -le $LOOP_COUNT ]; do
    echo "Rodada $i:"

    if grep -q "jar cfM" /home/jclabs/projetos/hwmdm/source/docker-entrypoint.sh; then
      log_pass "  docker-entrypoint.sh usa jar cfM (repack correto)"
    else
      log_fail "  docker-entrypoint.sh NÃO usa jar cfM"
    fi

    if grep -q "jar uf.*only adds and overwrites" /home/jclabs/projetos/hwmdm/source/docker-entrypoint.sh; then
      log_pass "  Bug antigo (jar uf) documentado"
    else
      log_fail "  Documentação de bug jar uf não encontrada"
    fi

    if grep -q "PLUGINS_KEEP" /home/jclabs/projetos/hwmdm/source/docker-entrypoint.sh; then
      log_pass "  Plugins preservados durante regeneração"
    else
      log_fail "  Preservação de plugins não encontrada"
    fi

    ((i++))
  done
}

test_kiosk_apk_available() {
  local i=1
  echo ""
  echo "=== TESTE 3: APK 6.37.3-kiosk disponível (5 rodadas) ==="

  while [ $i -le $LOOP_COUNT ]; do
    echo "Rodada $i:"

    if [ -f "/home/jclabs/projetos/hwmdm/source/volumes/work/files/$KIOSK_APK" ]; then
      local size=$(stat -c%s "/home/jclabs/projetos/hwmdm/source/volumes/work/files/$KIOSK_APK" 2>/dev/null)
      log_pass "  $KIOSK_APK existe ($size bytes)"
    else
      log_fail "  $KIOSK_APK NÃO ENCONTRADO"
    fi

    local latest_kiosk=$(ls -1 /home/jclabs/projetos/hwmdm/source/volumes/work/files/hmdm-*-kiosk.apk 2>/dev/null | sort -V | tail -1 | xargs basename)
    if [ "$latest_kiosk" = "$KIOSK_APK" ]; then
      log_pass "  $KIOSK_APK é versão mais nova de -kiosk"
    else
      log_fail "  $KIOSK_APK NÃO é a mais nova (actual: $latest_kiosk)"
    fi

    if grep -q "startLockTask\|setLockTaskPackages\|setStatusBarDisabled" /home/jclabs/projetos/hwmdm/android-source/app/src/main/java/com/hmdm/launcher/pro/ProUtils.java; then
      log_pass "  ProUtils.java contém LockTask APIs"
    else
      log_fail "  LockTask APIs não encontradas em ProUtils.java"
    fi

    ((i++))
  done
}

test_baseipfilter_enhancements() {
  local i=1
  echo ""
  echo "=== TESTE 4: BaseIPFilter.java com guards contra IPs infra (5 rodadas) ==="

  while [ $i -le $LOOP_COUNT ]; do
    echo "Rodada $i:"

    local filter_file="/home/jclabs/projetos/hwmdm/server-source/common/src/main/java/com/hmdm/rest/filter/BaseIPFilter.java"

    if grep -q "INFRASTRUCTURE_IPS" "$filter_file"; then
      log_pass "  INFRASTRUCTURE_IPS definido"
    else
      log_fail "  INFRASTRUCTURE_IPS não definido"
    fi

    if grep -q "getOperationalRemoteAddr" "$filter_file"; then
      log_pass "  getOperationalRemoteAddr() implementado"
    else
      log_fail "  getOperationalRemoteAddr() não encontrado"
    fi

    if grep -q "parseForwardedIp" "$filter_file"; then
      log_pass "  parseForwardedIp() implementado"
    else
      log_fail "  parseForwardedIp() não encontrado"
    fi

    if grep -q "isInfrastructureIp" "$filter_file"; then
      log_pass "  isInfrastructureIp() implementado"
    else
      log_fail "  isInfrastructureIp() não encontrado"
    fi

    ((i++))
  done
}

# Run all tests
test_proxy_config
test_layout_fix
test_kiosk_apk_available
test_baseipfilter_enhancements

# Summary
echo ""
echo "======================================"
echo "RESUMO FRENTE 1 (5 rodadas cada)"
echo "======================================"
echo "✓ Testes passaram: $TESTS_PASSED"
echo "✗ Testes falharam: $TESTS_FAILED"
echo "======================================"

if [ $TESTS_FAILED -eq 0 ]; then
  echo "✓ TODAS AS RODADAS PASSARAM"
  exit 0
else
  echo "✗ FALHAS DETECTADAS"
  exit 1
fi
