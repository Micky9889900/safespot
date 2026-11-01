#!/usr/bin/env python3
"""Fetch VPN Gate's public relay list and write pages/vpn/servers.json in SafeSpot's schema.
Run daily (e.g. a GitHub Action) and publish pages/vpn/ to micky9889900.github.io/vpn/.
To switch to paid/own servers later, just put your own entries (provider="safespot") in the list."""
import base64, csv, io, json, re, sys, urllib.request, datetime, os
URL = "https://www.vpngate.net/api/iphone/"
out = os.path.join(os.path.dirname(__file__), "..", "pages", "vpn", "servers.json")
raw = urllib.request.urlopen(urllib.request.Request(URL, headers={"User-Agent": "SafeSpot-refresh"}), timeout=60).read().decode("utf-8", "ignore")
lines = [l for l in raw.splitlines() if l and not l.startswith("*")]
rows = list(csv.reader(io.StringIO("\n".join(lines))))
servers = []
for r in rows[1:]:
    if len(r) < 15: continue
    try: cfg = base64.b64decode(r[14]).decode("utf-8", "ignore")
    except Exception: continue
    m = re.search(r"(?m)^remote\s+(\S+)\s+(\d+)", cfg)
    if not m: continue
    proto = (re.search(r"(?m)^proto\s+(\S+)", cfg) or [None, "tcp"])[1]
    # strip comments/blank lines and the long retry options (the app sets fast timeouts itself)
    cfg = "\n".join(l for l in cfg.replace("\r", "").splitlines() if l.strip() and not l.lstrip().startswith(("#", ";"))
                    and not re.match(r"(resolv-retry|connect-retry|connect-timeout|server-poll-timeout)\b", l.strip())) + "\n"
    servers.append({"id": f"vg-{r[1]}-{m.group(2)}", "type": "openvpn", "host": r[1], "port": int(m.group(2)), "proto": proto,
                    "country": r[5], "countryCode": r[6], "config": cfg, "provider": "vpngate",
                    "speedMbps": round(int(r[4] or 0) / 1e6, 1), "pingMs": int(r[3] or 0) if r[3].isdigit() else 0,
                    "score": int(r[2]) if r[2].isdigit() else 0})
# Keep the top ~40 by VPN Gate score (uptime x throughput), max 6 per country so users still get choice; UDP gets a small bonus.
servers.sort(key=lambda s: -(s["score"] * (1.15 if s["proto"].startswith("udp") else 1)))
picked, per = [], {}
for s_ in servers:
    if per.get(s_["countryCode"], 0) >= 6: continue
    per[s_["countryCode"]] = per.get(s_["countryCode"], 0) + 1; picked.append(s_)
    if len(picked) >= 40: break
servers = picked
os.makedirs(os.path.dirname(out), exist_ok=True)
json.dump({"version": 1, "updated": datetime.datetime.utcnow().isoformat() + "Z", "source": "VPN Gate Academic Experiment (vpngate.net)", "servers": servers}, open(out, "w"), separators=(",", ":"))
print(f"{len(servers)} servers -> {out}")
