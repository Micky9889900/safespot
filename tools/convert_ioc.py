#!/usr/bin/env python3
"""Download AssoEchap stalkerware-indicators (CC-BY 4.0) and convert ioc.yaml to app/src/main/assets/ioc.json"""
import json, sys, urllib.request, yaml, os
URL = "https://raw.githubusercontent.com/AssoEchap/stalkerware-indicators/master/ioc.yaml"
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
data = yaml.safe_load(urllib.request.urlopen(URL).read())
out = []
for e in data:
    out.append({"name": e.get("name"), "type": e.get("type", "stalkerware"),
                "packages": e.get("packages") or [],
                "certificates": [c.upper().replace(":", "") for c in (e.get("certificates") or [])],
                "websites": e.get("websites") or [], "c2": (e.get("c2") or {}).get("domains", []) if isinstance(e.get("c2"), dict) else []})
dst = os.path.join(root, "app/src/main/assets/ioc.json")
import datetime
json.dump({"updated": datetime.date.today().isoformat(), "source": URL, "license": "CC-BY 4.0, Echap (stalkerware-indicators)", "apps": out}, open(dst, "w"))
print(len(out), "entries,", sum(len(x["packages"]) for x in out), "packages,", sum(len(x["certificates"]) for x in out), "certs ->", dst)
