# SafeSpot UI – Grok Imagine prompts

> STYLE LOCK (paste first): Premium mobile security app UI asset for "SafeSpot". Glossy 3D clay-like render, smooth rounded shapes, dark navy main color, polished silver metal trim, red accent glow (like a red spy eye), soft studio lighting, subtle bloom, centered, clean, high detail, no text, no letters, no watermark. Plain solid single-color background only (pure white or pure black as stated).

## Code changes needed later (not done yet):
1. Put all PNGs in app/src/main/res/drawable-nodpi/ (exact filenames). Remove backgrounds first (white→alpha, black items: use as-is or screen blend), trim, max ~512px for icons, ~1080x1920 for backgrounds; convert to WebP to keep APK small.
2. ToolSpec: add an iconRes field; ToolOrb/ToolTile draw Image(painterResource) instead of the emoji, emoji as fallback.
3. HomeHero/ScoreRing: draw frame_score_ring + ic_status_* behind the score arc; add btn_scan_main(_active) as the scan button.
4. ConnectButton: swap Canvas circle for btn_vpn_off/connecting/on by Vpn.State, keep spin/pulse animation on top.
5. MainActivity tabs: replace Icons.Filled.* with ic_nav_* (active/inactive).
6. Glass/StatCard/PrimaryButton/GhostButton: optional image backgrounds (bg_stat_card, btn_primary...), with 9-patch-like stretching or fixed sizes.
7. Store.badges: icon emoji → badge_* drawables; StatusChips emoji → ic_chip_*.
8. Onboarding Page: use onb_* images; Genie: add genie_{gender}_{mood}.png (Genie.kt already looks these names up).
9. Themes: bg_home_* per theme via Modifier.paint; ThemePicker uses theme_* previews.
10. Result screens: ill_result_* above Particles.

## Home screen (20)

**1. Main scan orb (idle)** → `btn_scan_main.png`

```
Large round glossy 3D scan button, dark navy sphere with silver ring, glowing red eye-shaped lens in center, subtle radar lines, soft red glow. Pure black solid background. 1:1 square, centered.
```

**2. Main scan orb (scanning)** → `btn_scan_main_active.png`

```
Same glossy navy scan orb with silver ring, red eye lens glowing brighter, red radar sweep beam rotating across surface, energy sparks. Pure black solid background. 1:1 square, centered.
```

**3. Security score ring frame (ScoreRing)** → `frame_score_ring.png`

```
Empty circular 3D gauge frame, polished silver bezel, dark navy inner dial with fine tick marks, hollow empty center, faint red glow on rim. Pure black solid background. 1:1 square, centered.
```

**4. Shield status: protected** → `ic_status_safe.png`

```
Glossy 3D shield, navy and silver, green check mark embossed, soft green glow. Pure white solid background, no shadow on background. 1:1 square, centered.
```

**5. Shield status: at risk** → `ic_status_risk.png`

```
Glossy 3D shield, navy and silver, cracked, red warning exclamation glowing, red alert glow. Pure white solid background, no shadow on background. 1:1 square, centered.
```

**6. Shield status: not scanned yet** → `ic_status_unknown.png`

```
Glossy 3D shield, navy and silver, glowing silver question mark, calm soft glow. Pure white solid background, no shadow on background. 1:1 square, centered.
```

**7. StatCard background** → `bg_stat_card.png`

```
Wide rounded-rectangle glass card, frosted dark navy glass, thin silver edge highlight, faint red corner glow, empty center, no text. Pure black solid background. 16:9 wide.
```

**8. Stat icon: Apps scanned** → `ic_stat_apps.png`

```
Stack of glossy app tiles with magnifier. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**9. Stat icon: Risky apps** → `ic_stat_risky.png`

```
App tile with red warning triangle. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**10. Stat icon: Sensitive permissions** → `ic_stat_perms.png`

```
Padlock with tiny eye on it. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**11. Stat icon: Days protected** → `ic_stat_days.png`

```
Calendar with small shield. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**12. Chart panel background (LineChart/Donut/Bars)** → `bg_chart_panel.png`

```
Wide dark navy glass panel with faint grid lines, silver border, subtle red and cyan glow streaks, empty, no text, no numbers. Pure black solid background. 16:9 wide.
```

**13. Status chip: battery** → `ic_chip_battery.png`

```
Small battery with green charge. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**14. Status chip: temperature** → `ic_chip_temp.png`

```
Small thermometer with cyan frost. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**15. Status chip: RAM** → `ic_chip_ram.png`

```
Small memory chip with blue glow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**16. Status chip: storage** → `ic_chip_storage.png`

```
Small storage drive. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**17. Status chip: VPN on** → `ic_chip_vpn_on.png`

```
Small closed padlock with globe, green glow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**18. Status chip: VPN off** → `ic_chip_vpn_off.png`

```
Small open padlock with globe, grey dim. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**19. Recommended card banner** → `banner_recommended.png`

```
Wide premium banner, dark navy gradient, silver light streak, small glowing red eye emblem on left, empty space right for text, no text. Pure black solid background. 3:1 wide.
```

**20. Timeline item dot** → `ic_timeline_dot.png`

```
Tiny glossy navy orb with silver rim and red glowing core. Pure black solid background. 1:1 square, centered.
```

## Tool tiles & icons (ToolCatalog / ToolOrb / ToolTile) (15)

**21. Spyware Scan icon (id #scan)** → `ic_tool_spy.png`

```
Shield with red spy eye and magnifier. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**22. Antivirus icon (id #deep)** → `ic_tool_antivirus.png`

```
Shield crushing a green virus germ. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**23. Free VPN icon (id #vpn)** → `ic_tool_vpn.png`

```
Globe wrapped by a padlock tunnel, blue glow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**24. Junk Cleaner icon (id junk)** → `ic_tool_junk.png`

```
Broom sweeping glowing dust particles, orange accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**25. Battery icon (id battery)** → `ic_tool_battery.png`

```
Battery with leaf and green charge bolt. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**26. Booster icon (id booster)** → `ic_tool_booster.png`

```
Rocket launching with purple flame. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**27. CPU Cooler icon (id cooler)** → `ic_tool_cooler.png`

```
CPU chip covered in ice crystals, cyan frost. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**28. App Manager icon (id apps)** → `ic_tool_apps.png`

```
Open box with app tiles, gold accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**29. Wi-Fi Check icon (id wifi)** → `ic_tool_wifi.png`

```
Wi-Fi waves with small shield, cyan glow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**30. Permissions icon (id perms)** → `ic_tool_perms.png`

```
Key and padlock, pink accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**31. Speed Test icon (id speed)** → `ic_tool_speed.png`

```
Speedometer gauge with lightning, yellow accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**32. Storage icon (id storage)** → `ic_tool_storage.png`

```
Storage drive with pie chart, mint accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**33. Notifications icon (id notif)** → `ic_tool_notif.png`

```
Bell with small shield, peach accent. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**34. ToolTile background** → `bg_tool_tile.png`

```
Square rounded tile, frosted dark navy glass, thin silver edge, soft red inner glow at bottom, empty, no text. Pure black solid background. 1:1 square.
```

**35. ToolOrb glow base** → `bg_tool_orb.png`

```
Glossy round navy orb pedestal with silver ring and soft halo, empty center. Pure black solid background. 1:1 square, centered.
```

## Tool run & hero screens (RunState / Hero) (6)

**36. Run progress ring frame** → `frame_run_ring.png`

```
Large empty circular progress frame, silver bezel, navy dial, animated-looking energy particles around edge, hollow center. Pure black solid background. 1:1 square.
```

**37. Junk cleaning in progress** → `ill_junk_running.png`

```
3D broom vortex sucking glowing junk files into a navy bin, orange sparks. Pure black solid background. 1:1 square, centered.
```

**38. Cooler in progress** → `ill_cooler_running.png`

```
3D CPU chip being frozen by swirling snowflakes, cyan glow. Pure black solid background. 1:1 square, centered.
```

**39. Booster in progress** → `ill_booster_running.png`

```
3D rocket blasting off a phone, purple flame trail. Pure black solid background. 1:1 square, centered.
```

**40. Speed test in progress** → `ill_speed_running.png`

```
3D speedometer needle in red zone with lightning. Pure black solid background. 1:1 square, centered.
```

**41. Usage access needed (UsageGate)** → `ill_usage_gate.png`

```
3D phone with key and padlock, friendly, silver and amber glow. Pure white solid background, no shadow on background. 1:1 square, centered.
```

## Primary buttons (3)

**42. PrimaryButton background** → `btn_primary.png`

```
Wide pill button, glossy red gradient, silver rim, soft glow, empty, no text. Pure black solid background. 4:1 wide.
```

**43. PrimaryButton pressed** → `btn_primary_pressed.png`

```
Same wide glossy red pill button pressed in, darker red, inner shadow, no text. Pure black solid background. 4:1 wide.
```

**44. GhostButton background** → `btn_ghost.png`

```
Wide pill button outline, thin glowing silver border, transparent-looking dark navy fill, no text. Pure black solid background. 4:1 wide.
```

## VPN screen (12)

**45. ConnectButton: off** → `btn_vpn_off.png`

```
Large round glossy 3D power button, dark navy with silver ring, grey power symbol, no glow. Pure black solid background. 1:1 square, centered.
```

**46. ConnectButton: connecting** → `btn_vpn_connecting.png`

```
Large round glossy 3D power button, dark navy with silver ring, amber power symbol, spinning amber light arc around it. Pure black solid background. 1:1 square, centered.
```

**47. ConnectButton: connected** → `btn_vpn_on.png`

```
Large round glossy 3D power button, dark navy with silver ring, bright green power symbol, strong green glow halo. Pure black solid background. 1:1 square, centered.
```

**48. VPN world map background** → `bg_vpn_map.png`

```
Dark navy world map made of glowing dots, silver connection lines, red pulse points, empty top area. 9:16 vertical. Pure black solid background.
```

**49. Server card background** → `bg_server_card.png`

```
Wide rounded card, frosted navy glass, silver edge, faint blue glow left side, empty, no text. Pure black solid background. 4:1 wide.
```

**50. Fastest server (auto) icon** → `ic_server_auto.png`

```
Globe with lightning bolt and crown. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**51. Signal strength good** → `ic_ping_good.png`

```
Three green signal bars. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**52. Signal strength weak** → `ic_ping_weak.png`

```
One red signal bar, two grey. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**53. Download speed** → `ic_vpn_down.png`

```
Glowing cyan down arrow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**54. Upload speed** → `ic_vpn_up.png`

```
Glowing red up arrow. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**55. Quad9 DNS toggle icon** → `ic_dns_shield.png`

```
Shield with DNS server stack. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**56. Open network warning** → `ic_wifi_open_warn.png`

```
Wi-Fi symbol with red warning triangle. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

## Scan & result screens (11)

**57. Scan radar (Radar)** → `ill_radar.png`

```
3D circular radar screen, navy glass, silver rings, red sweep beam, small blips. Pure black solid background. 1:1 square, centered.
```

**58. Result: safe** → `ill_result_safe.png`

```
Big glossy 3D shield, navy and silver, green check, confetti sparkles, triumphant glow. Pure black solid background. 1:1 square, centered.
```

**59. Result: threat found** → `ill_result_threat.png`

```
Big glossy 3D shield with red spy eye bug caught in a net, red alarm glow. Pure black solid background. 1:1 square, centered.
```

**60. Result: cleaned** → `ill_result_cleaned.png`

```
Sparkling clean phone with broom and shine stars, orange and silver glow. Pure black solid background. 1:1 square, centered.
```

**61. Result: cooled** → `ill_result_cooled.png`

```
Phone covered in frost with happy snowflake, cyan glow. Pure black solid background. 1:1 square, centered.
```

**62. Result: boosted** → `ill_result_boosted.png`

```
Phone with rocket trail and stars, purple glow. Pure black solid background. 1:1 square, centered.
```

**63. Risk level: high** → `ic_risk_high.png`

```
Red skull-eye badge. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**64. Risk level: medium** → `ic_risk_medium.png`

```
Amber warning badge. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**65. Risk level: low** → `ic_risk_low.png`

```
Green check badge. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**66. App detail Trust ring frame** → `frame_trust_ring.png`

```
Small circular gauge frame, silver bezel, navy dial, hollow center. Pure black solid background. 1:1 square.
```

**67. Native ad card frame** → `bg_ad_card.png`

```
Wide rounded card, dark navy glass, silver border, empty, no text, no logos. Pure black solid background. 3:1 wide.
```

## Bottom navigation (11)

**68. Nav: Home** → `ic_nav_home.png`

```
House with small shield, flat silver, dim. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**69. Nav: Home** → `ic_nav_home_active.png`

```
House with small shield, bright with red glow. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**70. Nav: Scan** → `ic_nav_scan.png`

```
Magnifier with red eye, flat silver, dim. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**71. Nav: Scan** → `ic_nav_scan_active.png`

```
Magnifier with red eye, bright with red glow. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**72. Nav: VPN** → `ic_nav_vpn.png`

```
Globe with padlock, flat silver, dim. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**73. Nav: VPN** → `ic_nav_vpn_active.png`

```
Globe with padlock, bright with red glow. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**74. Nav: Tools** → `ic_nav_tools.png`

```
Wrench and sparkles, flat silver, dim. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**75. Nav: Tools** → `ic_nav_tools_active.png`

```
Wrench and sparkles, bright with red glow. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**76. Nav: Me** → `ic_nav_me.png`

```
Genie lamp, flat silver, dim. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**77. Nav: Me** → `ic_nav_me_active.png`

```
Genie lamp, bright with red glow. Simple, readable at small size. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**78. Bottom nav bar background** → `bg_bottom_nav.png`

```
Wide floating dark navy glass bar, silver top edge, rounded, empty. Pure black solid background. 5:1 wide.
```

## Badges & streak (Store.badges) (11)

**79. Badge: First scan** → `badge_first_scan.png`

```
Medal with shield. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**80. Badge: Deep diver** → `badge_deep_scan.png`

```
Medal with microscope. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**81. Badge: 3-day streak** → `badge_streak3.png`

```
Medal with flame and number 3. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**82. Badge: 7-day streak** → `badge_streak7.png`

```
Gold trophy with number 7. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**83. Badge: Zero risk** → `badge_zero_risk.png`

```
Medal with glowing diamond. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**84. Badge: Fortress (score 90)** → `badge_score90.png`

```
Medal with silver castle. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**85. Badge: Fixer** → `badge_fixer.png`

```
Medal with wrench. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**86. Badge: Private tunnel** → `badge_vpn_first.png`

```
Medal with globe tunnel. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**87. Badge: Vigilant (10 scans)** → `badge_ten_scans.png`

```
Medal with red eye. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**88. Badge: locked** → `badge_locked.png`

```
Grey medal with padlock, dim. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**89. Streak flame** → `ic_streak_flame.png`

```
Glossy flame, red and amber. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

## Onboarding (5)

**90. Page 1: Find hidden spyware** → `onb_spyware.png`

```
3D phone with magnifier revealing a hidden red spy eye bug, navy and silver. 9:16 vertical. Pure black solid background.
```

**91. Page 2: Free VPN** → `onb_vpn.png`

```
3D glowing globe inside a shield tunnel, blue and silver light. 9:16 vertical. Pure black solid background.
```

**92. Page 3: Phone care** → `onb_care.png`

```
3D phone surrounded by broom, battery, snowflake and rocket icons. 9:16 vertical. Pure black solid background.
```

**93. Page 4: Meet your genie** → `onb_genie.png`

```
Magic lamp releasing purple smoke and sparkles, empty space for mascot. 9:16 vertical. Pure black solid background.
```

**94. Page indicator dot active** → `ic_dot_active.png`

```
Tiny glossy red glowing pill. Pure black solid background. 1:1 square.
```

## Genie mascot (6 moods × 2 genders; idle exists) (11)

**95. Genie male happy** → `genie_male_happy.png`

```
Same male 3D genie character as reference, big smile, waving, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**96. Genie male worried** → `genie_male_worried.png`

```
Same male 3D genie character as reference, worried face, hands on cheeks, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**97. Genie male cheer** → `genie_male_cheer.png`

```
Same male 3D genie character as reference, arms up cheering, sparkles, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**98. Genie male sleep** → `genie_male_sleep.png`

```
Same male 3D genie character as reference, eyes closed, sleeping, tiny z, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**99. Genie male point** → `genie_male_point.png`

```
Same male 3D genie character as reference, pointing finger to the side, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**100. Genie female happy** → `genie_female_happy.png`

```
Same female 3D genie character as reference, big smile, waving, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**101. Genie female worried** → `genie_female_worried.png`

```
Same female 3D genie character as reference, worried face, hands on cheeks, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**102. Genie female cheer** → `genie_female_cheer.png`

```
Same female 3D genie character as reference, arms up cheering, sparkles, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**103. Genie female sleep** → `genie_female_sleep.png`

```
Same female 3D genie character as reference, eyes closed, sleeping, tiny z, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**104. Genie female point** → `genie_female_point.png`

```
Same female 3D genie character as reference, pointing finger to the side, full body, Pixar-style. Plain solid pure white background. 1:1 square, centered.
```

**105. Genie aura (GenieAura)** → `fx_genie_aura.png`

```
Soft purple and silver magic smoke ring with sparkles, empty center. Pure black solid background. 1:1 square.
```

## Settings / Me & theme previews (14)

**106. Theme preview: navy** → `theme_navy.png`

```
Mini phone screen mockup in dark navy theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**107. Theme preview: light** → `theme_light.png`

```
Mini phone screen mockup in white and silver theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**108. Theme preview: amoled** → `theme_amoled.png`

```
Mini phone screen mockup in pure black theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**109. Theme preview: frost** → `theme_frost.png`

```
Mini phone screen mockup in icy blue theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**110. Theme preview: rose** → `theme_rose.png`

```
Mini phone screen mockup in rose pink theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**111. Theme preview: emerald** → `theme_emerald.png`

```
Mini phone screen mockup in emerald green theme, shield orb and small tiles, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**112. Theme preview: system** → `theme_system.png`

```
Mini phone screen split half dark navy, half white, no text. Pure white solid background, no shadow on background. 9:16 vertical.
```

**113. Settings: Change genie** → `ic_set_genie.png`

```
Genie lamp. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**114. Settings: Theme** → `ic_set_theme.png`

```
Paint palette. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**115. Settings: Daily reminder** → `ic_set_reminder.png`

```
Alarm clock with shield. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**116. Settings: Diagnostics** → `ic_set_diag.png`

```
Heart-rate line on a phone. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**117. Settings: About** → `ic_set_about.png`

```
Info letter i. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**118. Settings: Privacy** → `ic_set_privacy.png`

```
Document with padlock. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**119. Settings: Data sources** → `ic_set_sources.png`

```
Database stack with check. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

## Empty states (4)

**120. No threats / empty list** → `empty_no_threats.png`

```
Peaceful shield sleeping on a cloud, navy and silver. Pure white solid background, no shadow on background. 1:1 square, centered.
```

**121. No history yet** → `empty_no_history.png`

```
Empty chart with little magnifier. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**122. No servers / offline** → `empty_offline.png`

```
Globe with unplugged cable. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

**123. Crash screen** → `ill_crash.png`

```
Sad robot holding a wrench. Glossy 3D clay-style icon, dark navy body, silver metal trim, red accent, soft glow, centered, 1:1 square. Pure white solid background, no shadow on background.
```

## Backgrounds (9)

**124. Background home dark** → `bg_home_dark.png`

```
Deep navy gradient with soft silver light rays and faint red glow at top, subtle hexagon pattern. No text, empty middle. 9:16 vertical.
```

**125. Background home light** → `bg_home_light.png`

```
Soft white to light silver gradient, faint navy hexagon pattern, tiny red glow top. No text, empty middle. 9:16 vertical.
```

**126. Background amoled** → `bg_home_amoled.png`

```
Pure black with very faint navy hexagon lines and tiny red glow. No text, empty middle. 9:16 vertical.
```

**127. Background frost** → `bg_home_frost.png`

```
Icy blue gradient with frost crystals at edges. No text, empty middle. 9:16 vertical.
```

**128. Background rose** → `bg_home_rose.png`

```
Deep rose to plum gradient with soft light rays. No text, empty middle. 9:16 vertical.
```

**129. Background emerald** → `bg_home_emerald.png`

```
Deep emerald to navy gradient with soft light rays. No text, empty middle. 9:16 vertical.
```

**130. Background scan** → `bg_scan.png`

```
Dark navy with glowing red radar circles fading out. No text, empty middle. 9:16 vertical.
```

**131. Background tools** → `bg_tools.png`

```
Dark navy with floating faint glass tiles and silver bokeh. No text, empty middle. 9:16 vertical.
```

**132. Background result celebration** → `bg_result.png`

```
Dark navy with burst of silver and green light rays from center. No text, empty middle. 9:16 vertical.
```
