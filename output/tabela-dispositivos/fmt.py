import sys, json
for l in sys.stdin:
    l = l.strip()
    if not l:
        print("  (sem resultado)"); continue
    d = json.loads(l)
    print(f"{d['viewport']:>5} | quadro {d['wrapClient']:>4} | tabela {d['tableWidth']:>4} | rolagemH {str(d['hScroll']):5} | Acoes visivel {str(d['actionsVisibleAtScroll0']):5} x={d['actionsLeftAtScroll0']:>4} pos={d['actionsTdPosition']:6} | linha {d['rowHeight']}px | cab {d['theadH']}px | workspace {d['workspace']}")
