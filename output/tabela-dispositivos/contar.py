import re, sys
s = open(sys.argv[1]).read()
pats = {"th": r"<th\b", "td": r"<td\b", "col": r"<col\b", "button": r"<button\b",
        "menuitem": r'role="menuitem"', "ng-click": r"ng-click=", "ng-if": r"ng-if=",
        "settings.columnDisplayed": r"settings\.columnDisplayed", "glyphicon-": r"glyphicon glyphicon-"}
print(" ".join(f"{k}={len(re.findall(p, s))}" for k, p in pats.items()))
