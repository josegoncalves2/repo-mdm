# Ledger fiscal -- corrigir-permissoes-e-console

Encadeado: hash = sha256(hash_anterior|horario|texto). Nao edite a mao; use `guarda.py registrar`. `guarda.py verificar-ledger` recusa adulteracao.

| # | Horario | Texto | Hash anterior | Hash |
|---|---|---|---|---|
| 1 | 2026-09-18T16:08:29-03:00 | abertura de sessão fiscal por /fiscal-permissoes | GENESE | d3aabe84768c50d2a6ef83b9f3e5f89b0d901ca81df07f024424bfb970fe054e |
| 2 | 2026-09-18T16:13:06-03:00 | despacho AMB-0.1-t1 hwmdm-ambiente | d3aabe84768c50d2a6ef83b9f3e5f89b0d901ca81df07f024424bfb970fe054e | e1e4967f691053853629471c70bd8912bf94e8a218c1585c313b0f9a1cb8aa0f |
| 3 | 2026-09-18T16:21:24-03:00 | redespacho AMB-0.1-t1 hwmdm-ambiente - AMBIENTE nao criou usuarios pela tela na primeira tentativa | e1e4967f691053853629471c70bd8912bf94e8a218c1585c313b0f9a1cb8aa0f | cfe273a7bc4018cd306977d9bb4557bc0e380fff2bb1d05f35c671cf055b8d13 |
