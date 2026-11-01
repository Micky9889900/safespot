W="Pure white solid background, no shadow on background."
B="Pure black solid background."
I="Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square."
groups=[]
def g(name,items): groups.append((name,items))
g("Home screen",[
("Main scan orb (idle)","btn_scan_main.png","Large round glossy 3D scan button, dark navy sphere with silver ring, glowing red eye-shaped lens in center, subtle radar lines, soft red glow. "+B+" 1:1 square, centered."),
("Main scan orb (scanning)","btn_scan_main_active.png","Same glossy navy scan orb with silver ring, red eye lens glowing brighter, red radar sweep beam rotating across surface, energy sparks. "+B+" 1:1 square, centered."),
("Security score ring frame (ScoreRing)","frame_score_ring.png","Empty circular 3D gauge frame, polished silver bezel, dark navy inner dial with fine tick marks, hollow empty center, faint red glow on rim. "+B+" 1:1 square, centered."),
("Shield status: protected","ic_status_safe.png","Glossy 3D shield, navy and silver, green check mark embossed, soft green glow. "+W+" 1:1 square, centered."),
("Shield status: at risk","ic_status_risk.png","Glossy 3D shield, navy and silver, cracked, red warning exclamation glowing, red alert glow. "+W+" 1:1 square, centered."),
("Shield status: not scanned yet","ic_status_unknown.png","Glossy 3D shield, navy and silver, glowing silver question mark, calm soft glow. "+W+" 1:1 square, centered."),
("StatCard background","bg_stat_card.png","Wide rounded-rectangle glass card, frosted dark navy glass, thin silver edge highlight, faint red corner glow, empty center, no text. "+B+" 16:9 wide."),
("Stat icon: Apps scanned","ic_stat_apps.png","Stack of glossy app tiles with magnifier. "+I+" "+W),
("Stat icon: Risky apps","ic_stat_risky.png","App tile with red warning triangle. "+I+" "+W),
("Stat icon: Sensitive permissions","ic_stat_perms.png","Padlock with tiny eye on it. "+I+" "+W),
("Stat icon: Days protected","ic_stat_days.png","Calendar with small shield. "+I+" "+W),
("Chart panel background (LineChart/Donut/Bars)","bg_chart_panel.png","Wide dark navy glass panel with faint grid lines, silver border, subtle red and cyan glow streaks, empty, no text, no numbers. "+B+" 16:9 wide."),
("Status chip: battery","ic_chip_battery.png","Small battery with green charge. "+I+" "+W),
("Status chip: temperature","ic_chip_temp.png","Small thermometer with cyan frost. "+I+" "+W),
("Status chip: RAM","ic_chip_ram.png","Small memory chip with blue glow. "+I+" "+W),
("Status chip: storage","ic_chip_storage.png","Small storage drive. "+I+" "+W),
("Status chip: VPN on","ic_chip_vpn_on.png","Small closed padlock with globe, green glow. "+I+" "+W),
("Status chip: VPN off","ic_chip_vpn_off.png","Small open padlock with globe, grey dim. "+I+" "+W),
("Recommended card banner","banner_recommended.png","Wide premium banner, dark navy gradient, silver light streak, small glowing red eye emblem on left, empty space right for text, no text. "+B+" 3:1 wide."),
("Timeline item dot","ic_timeline_dot.png","Tiny glossy navy orb with silver rim and red glowing core. "+B+" 1:1 square, centered."),
])
tools=[("#scan","Spyware Scan","ic_tool_spy.png","Shield with red spy eye and magnifier"),
("#deep","Antivirus","ic_tool_antivirus.png","Shield crushing a green virus germ"),
("#vpn","Free VPN","ic_tool_vpn.png","Globe wrapped by a padlock tunnel, blue glow"),
("junk","Junk Cleaner","ic_tool_junk.png","Broom sweeping glowing dust particles, orange accent"),
("battery","Battery","ic_tool_battery.png","Battery with leaf and green charge bolt"),
("booster","Booster","ic_tool_booster.png","Rocket launching with purple flame"),
("cooler","CPU Cooler","ic_tool_cooler.png","CPU chip covered in ice crystals, cyan frost"),
("apps","App Manager","ic_tool_apps.png","Open box with app tiles, gold accent"),
("wifi","Wi-Fi Check","ic_tool_wifi.png","Wi-Fi waves with small shield, cyan glow"),
("perms","Permissions","ic_tool_perms.png","Key and padlock, pink accent"),
("speed","Speed Test","ic_tool_speed.png","Speedometer gauge with lightning, yellow accent"),
("storage","Storage","ic_tool_storage.png","Storage drive with pie chart, mint accent"),
("notif","Notifications","ic_tool_notif.png","Bell with small shield, peach accent")]
g("Tool tiles & icons (ToolCatalog / ToolOrb / ToolTile)",
 [(f"{t} icon (id {i})",f,d+". "+I+" "+W) for i,t,f,d in tools]+
 [("ToolTile background","bg_tool_tile.png","Square rounded tile, frosted dark navy glass, thin silver edge, soft red inner glow at bottom, empty, no text. "+B+" 1:1 square."),
  ("ToolOrb glow base","bg_tool_orb.png","Glossy round navy orb pedestal with silver ring and soft halo, empty center. "+B+" 1:1 square, centered.")])
g("Tool run & hero screens (RunState / Hero)",[
("Run progress ring frame","frame_run_ring.png","Large empty circular progress frame, silver bezel, navy dial, animated-looking energy particles around edge, hollow center. "+B+" 1:1 square."),
("Junk cleaning in progress","ill_junk_running.png","3D broom vortex sucking glowing junk files into a navy bin, orange sparks. "+B+" 1:1 square, centered."),
("Cooler in progress","ill_cooler_running.png","3D CPU chip being frozen by swirling snowflakes, cyan glow. "+B+" 1:1 square, centered."),
("Booster in progress","ill_booster_running.png","3D rocket blasting off a phone, purple flame trail. "+B+" 1:1 square, centered."),
("Speed test in progress","ill_speed_running.png","3D speedometer needle in red zone with lightning. "+B+" 1:1 square, centered."),
("Usage access needed (UsageGate)","ill_usage_gate.png","3D phone with key and padlock, friendly, silver and amber glow. "+W+" 1:1 square, centered."),
])
g("Primary buttons",[
("PrimaryButton background","btn_primary.png","Wide pill button, glossy red gradient, silver rim, soft glow, empty, no text. "+B+" 4:1 wide."),
("PrimaryButton pressed","btn_primary_pressed.png","Same wide glossy red pill button pressed in, darker red, inner shadow, no text. "+B+" 4:1 wide."),
("GhostButton background","btn_ghost.png","Wide pill button outline, thin glowing silver border, transparent-looking dark navy fill, no text. "+B+" 4:1 wide."),
])
g("VPN screen",[
("ConnectButton: off","btn_vpn_off.png","Large round glossy 3D power button, dark navy with silver ring, grey power symbol, no glow. "+B+" 1:1 square, centered."),
("ConnectButton: connecting","btn_vpn_connecting.png","Large round glossy 3D power button, dark navy with silver ring, amber power symbol, spinning amber light arc around it. "+B+" 1:1 square, centered."),
("ConnectButton: connected","btn_vpn_on.png","Large round glossy 3D power button, dark navy with silver ring, bright green power symbol, strong green glow halo. "+B+" 1:1 square, centered."),
("VPN world map background","bg_vpn_map.png","Dark navy world map made of glowing dots, silver connection lines, red pulse points, empty top area. 9:16 vertical. "+B),
("Server card background","bg_server_card.png","Wide rounded card, frosted navy glass, silver edge, faint blue glow left side, empty, no text. "+B+" 4:1 wide."),
("Fastest server (auto) icon","ic_server_auto.png","Globe with lightning bolt and crown. "+I+" "+W),
("Signal strength good","ic_ping_good.png","Three green signal bars. "+I+" "+W),
("Signal strength weak","ic_ping_weak.png","One red signal bar, two grey. "+I+" "+W),
("Download speed","ic_vpn_down.png","Glowing cyan down arrow. "+I+" "+W),
("Upload speed","ic_vpn_up.png","Glowing red up arrow. "+I+" "+W),
("Quad9 DNS toggle icon","ic_dns_shield.png","Shield with DNS server stack. "+I+" "+W),
("Open network warning","ic_wifi_open_warn.png","Wi-Fi symbol with red warning triangle. "+I+" "+W),
])
g("Scan & result screens",[
("Scan radar (Radar)","ill_radar.png","3D circular radar screen, navy glass, silver rings, red sweep beam, small blips. "+B+" 1:1 square, centered."),
("Result: safe","ill_result_safe.png","Big glossy 3D shield, navy and silver, green check, confetti sparkles, triumphant glow. "+B+" 1:1 square, centered."),
("Result: threat found","ill_result_threat.png","Big glossy 3D shield with red spy eye bug caught in a net, red alarm glow. "+B+" 1:1 square, centered."),
("Result: cleaned","ill_result_cleaned.png","Sparkling clean phone with broom and shine stars, orange and silver glow. "+B+" 1:1 square, centered."),
("Result: cooled","ill_result_cooled.png","Phone covered in frost with happy snowflake, cyan glow. "+B+" 1:1 square, centered."),
("Result: boosted","ill_result_boosted.png","Phone with rocket trail and stars, purple glow. "+B+" 1:1 square, centered."),
("Risk level: high","ic_risk_high.png","Red skull-eye badge. "+I+" "+W),
("Risk level: medium","ic_risk_medium.png","Amber warning badge. "+I+" "+W),
("Risk level: low","ic_risk_low.png","Green check badge. "+I+" "+W),
("App detail Trust ring frame","frame_trust_ring.png","Small circular gauge frame, silver bezel, navy dial, hollow center. "+B+" 1:1 square."),
("Native ad card frame","bg_ad_card.png","Wide rounded card, dark navy glass, silver border, empty, no text, no logos. "+B+" 3:1 wide."),
])
g("Bottom navigation",[(f"Nav: {n}",f"ic_nav_{k}{s}.png",f"{d}, {'bright with red glow' if s else 'flat silver, dim'}. Simple, readable at small size. "+I+" "+W)
 for k,n,d in [("home","Home","House with small shield"),("scan","Scan","Magnifier with red eye"),("vpn","VPN","Globe with padlock"),("tools","Tools","Wrench and sparkles"),("me","Me","Genie lamp")] for s in ["","_active"]]
 +[("Bottom nav bar background","bg_bottom_nav.png","Wide floating dark navy glass bar, silver top edge, rounded, empty. "+B+" 5:1 wide.")])
g("Badges & streak (Store.badges)",[
("Badge: First scan","badge_first_scan.png","Medal with shield. "),("Badge: Deep diver","badge_deep_scan.png","Medal with microscope. "),
("Badge: 3-day streak","badge_streak3.png","Medal with flame and number 3. "),("Badge: 7-day streak","badge_streak7.png","Gold trophy with number 7. "),
("Badge: Zero risk","badge_zero_risk.png","Medal with glowing diamond. "),("Badge: Fortress (score 90)","badge_score90.png","Medal with silver castle. "),
("Badge: Fixer","badge_fixer.png","Medal with wrench. "),("Badge: Private tunnel","badge_vpn_first.png","Medal with globe tunnel. "),
("Badge: Vigilant (10 scans)","badge_ten_scans.png","Medal with red eye. "),("Badge: locked","badge_locked.png","Grey medal with padlock, dim. "),
("Streak flame","ic_streak_flame.png","Glossy flame, red and amber. ")])
groups[-1]=(groups[-1][0],[(a,b,c+I+" "+W) for a,b,c in groups[-1][1]])
g("Onboarding",[
("Page 1: Find hidden spyware","onb_spyware.png","3D phone with magnifier revealing a hidden red spy eye bug, navy and silver. 9:16 vertical. "+B),
("Page 2: Free VPN","onb_vpn.png","3D glowing globe inside a shield tunnel, blue and silver light. 9:16 vertical. "+B),
("Page 3: Phone care","onb_care.png","3D phone surrounded by broom, battery, snowflake and rocket icons. 9:16 vertical. "+B),
("Page 4: Meet your genie","onb_genie.png","Magic lamp releasing purple smoke and sparkles, empty space for mascot. 9:16 vertical. "+B),
("Page indicator dot active","ic_dot_active.png","Tiny glossy red glowing pill. "+B+" 1:1 square."),
])
moods=[("idle","calm smile, floating"),("happy","big smile, waving"),("worried","worried face, hands on cheeks"),("cheer","arms up cheering, sparkles"),("sleep","eyes closed, sleeping, tiny z"),("point","pointing finger to the side")]
g("Genie mascot (6 moods × 2 genders; idle exists)",[(f"Genie {gd} {m}",f"genie_{gd}_{m}.png",f"Same {gd} 3D genie character as reference, {d}, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.") for gd in ["male","female"] for m,d in moods if m!="idle"]
 +[("Genie aura (GenieAura)","fx_genie_aura.png","Soft purple and silver magic smoke ring with sparkles, empty center. "+B+" 1:1 square.")])
themes=[("navy","dark navy"),("light","white and silver"),("amoled","pure black"),("frost","icy blue"),("rose","rose pink"),("emerald","emerald green")]
g("Settings / Me & theme previews",[(f"Theme preview: {t}",f"theme_{t}.png",f"Mini phone screen mockup in {d} theme, shield orb and small tiles, no text. "+W+" 9:16 vertical.") for t,d in themes]+[
("Theme preview: system","theme_system.png","Mini phone screen split half dark navy, half white, no text. "+W+" 9:16 vertical."),
("Settings: Change genie","ic_set_genie.png","Genie lamp. "+I+" "+W),("Settings: Theme","ic_set_theme.png","Paint palette. "+I+" "+W),
("Settings: Daily reminder","ic_set_reminder.png","Alarm clock with shield. "+I+" "+W),("Settings: Diagnostics","ic_set_diag.png","Heart-rate line on a phone. "+I+" "+W),
("Settings: About","ic_set_about.png","Info letter i. "+I+" "+W),("Settings: Privacy","ic_set_privacy.png","Document with padlock. "+I+" "+W),
("Settings: Data sources","ic_set_sources.png","Database stack with check. "+I+" "+W)])
g("Empty states",[
("No threats / empty list","empty_no_threats.png","Peaceful shield sleeping on a cloud, navy and silver. "+W+" 1:1 square, centered."),
("No history yet","empty_no_history.png","Empty chart with little magnifier. "+I+" "+W),
("No servers / offline","empty_offline.png","Globe with unplugged cable. "+I+" "+W),
("Crash screen","ill_crash.png","Sad robot holding a wrench. "+I+" "+W)])
g("Backgrounds",[(f"Background {n}",f,d+" No text, empty middle. 9:16 vertical.") for n,f,d in [
("home dark","bg_home_dark.png","Deep navy gradient with soft silver light rays and faint red glow at top, subtle hexagon pattern."),
("home light","bg_home_light.png","Soft white to light silver gradient, faint navy hexagon pattern, tiny red glow top."),
("amoled","bg_home_amoled.png","Pure black with very faint navy hexagon lines and tiny red glow."),
("frost","bg_home_frost.png","Icy blue gradient with frost crystals at edges."),
("rose","bg_home_rose.png","Deep rose to plum gradient with soft light rays."),
("emerald","bg_home_emerald.png","Deep emerald to navy gradient with soft light rays."),
("scan","bg_scan.png","Dark navy with glowing red radar circles fading out."),
("tools","bg_tools.png","Dark navy with floating faint glass tiles and silver bokeh."),
("result celebration","bg_result.png","Dark navy with burst of silver and green light rays from center.")]])
STYLE="STYLE LOCK (paste first): Premium mobile security app UI asset for \"SafeSpot\". Glossy 3D clay-like render, smooth rounded shapes, dark navy main color, polished silver metal trim, red accent glow (like a red spy eye), soft studio lighting, subtle bloom, centered, clean, high detail, no text, no letters, no watermark. Plain solid single-color background only (pure white or pure black as stated)."
CODE="""Code changes needed later (not done yet):
1. Put all PNGs in app/src/main/res/drawable-nodpi/ (exact filenames). Remove backgrounds first (white→alpha, black items: use as-is or screen blend), trim, max ~512px for icons, ~1080x1920 for backgrounds; convert to WebP to keep APK small.
2. ToolSpec: add an iconRes field; ToolOrb/ToolTile draw Image(painterResource) instead of the emoji, emoji as fallback.
3. HomeHero/ScoreRing: draw frame_score_ring + ic_status_* behind the score arc; add btn_scan_main(_active) as the scan button.
4. ConnectButton: swap Canvas circle for btn_vpn_off/connecting/on by Vpn.State, keep spin/pulse animation on top.
5. MainActivity tabs: replace Icons.Filled.* with ic_nav_* (active/inactive).
6. Glass/StatCard/PrimaryButton/GhostButton: optional image backgrounds (bg_stat_card, btn_primary...), with 9-patch-like stretching or fixed sizes.
7. Store.badges: icon emoji → badge_* drawables; StatusChips emoji → ic_chip_*.
8. Onboarding Page: use onb_* images; Genie: add genie_{gender}_{mood}.png (Genie.kt already looks these names up).
9. Themes: bg_home_* per theme via Modifier.paint; ThemePicker uses theme_* previews.
10. Result screens: ill_result_* above Particles."""
md=["# SafeSpot UI – Grok Imagine prompts","",f"> {STYLE}","",CODE.replace("Code changes","## Code changes",1),""]
tx=["SAFESPOT UI PROMPTS","",STYLE,""]
n=0;counts=[]
for name,items in groups:
  md+=[f"## {name} ({len(items)})",""];tx+=["="*30,name.upper()+f" ({len(items)})","="*30,""]
  counts.append((name,len(items)))
  for t,f,p in items:
    n+=1;md+=[f"**{n}. {t}** → `{f}`","",f"```\n{p}\n```",""];tx+=[f"{n}. {t}",f"File: {f}",p,""]
    assert len(p.split())<=60,(f,len(p.split()))
tx+=["",CODE]
open("SafeSpot-UI-prompts.md","w").write("\n".join(md));open("SafeSpot-UI-prompts.txt","w").write("\n".join(tx))
print(n);[print(c) for c in counts]
