import re
P={}
for n,f in re.findall(r'\*\*(\d+)\. [^*]+\*\* → `([a-z0-9_]+)\.png`',open('../../design-prompts/SafeSpot-UI-prompts.md').read()): P[int(n)]=f
names={int(n):t for n,t in re.findall(r'\*\*(\d+)\. ([^*]+)\*\*',open('../../design-prompts/SafeSpot-UI-prompts.md').read())}
order=[l.split()[1] for l in open('order.txt')]
# zip index -> (prompt, conf, note)
M={}
seq={0:1,1:2,2:3,3:4,4:5,5:6,6:7,7:8,8:9,9:10,10:11,11:12,12:13,13:14,14:15,15:16,16:17,17:18,18:19,19:20,
20:23,21:24,22:25,23:26,24:28,25:29,26:30,27:21,28:22,29:27,30:31,31:32,32:33,33:34,34:35,35:36,36:37,37:38,38:39,39:40,
40:41,41:42,42:43,43:44,44:45,45:46,46:47,47:48,48:49,49:50,50:51,51:52,52:53,53:54,54:55,55:56,56:57,57:58,58:59,59:60,
60:61,61:62,62:63,63:64,64:65,65:66,66:67,67:68,68:69,69:70,70:71,71:72,72:74,73:73,74:75,75:76,76:77,77:78,
78:79,79:80,80:81,81:82,82:83,83:84,84:87,85:88,86:85,87:89,88:90,90:91,91:93,93:94,95:120,119:130,120:124,118:126}
low={15:'storage chip looks like a device, not storage',17:'open padlock read as VPN off',84:'crosshair-plus medal, not a red eye',86:'star+broom medal, no wrench',88:'phone+shield+red eyes; no magnifier',90:'globe+padlock with red roots; no shield tunnel',85:'padlock medal but not grey/dim (dimmed in code)',93:'red orb on white, not a pill on black',120:'has shield in middle (prompt asked empty middle)',118:'has shield orb in middle; used for AMOLED',14:'RAM chip shown as shield block'}
for k,v in seq.items(): M[k]=(v,'low' if k in (84,86,90) else 'medium' if k in low else 'high',low.get(k,''))
extra={89:'blue shield/lens on black – not in sheet',92:'navy ball (inactive dot?) – not in sheet',94:'toggle pill – not in sheet',96:'empty bin – not in sheet',97:'earth globe – maybe empty_offline but no cable',98:'cracked shield – not the sad robot for #123',99:'phone+lock+key gold',100:'bell+key',101:'handshake globe padlock',102:'crown shield key',103:'eye shield key',104:'two phones UI',105:'green shield confetti (not full bg)',116:'shield red eye on black',117:'app-icon-like shield',121:'progress track',122:'progress bar red fill',123:'divider line',124:'close X button',125:'back arrow button'}
for k in range(106,116): extra[k]='genie pose – OFF-MODEL (red-eye ninja ghost, not the turban genie reference); gender unclear'
rows=['| source file | zip# | prompt # | target filename | confidence | notes |','|---|---|---|---|---|---|']
for k in sorted(M): v,c,n=M[k]; rows.append(f'| {order[k]} | {k} | {v} | {P[v]}.webp | {c} | {n} |')
miss=sorted(set(P)-set(v for v,_,_ in M.values()))
out='# SafeSpot UI batch 1 – mapping\n\nZip order (by timestamp) follows prompt order; verified visually. Applied as .webp in res/drawable-nodpi.\n\n'+'\n'.join(rows)
out+='\n\n## Unmatched / not applied\n'+'\n'.join(f'- {order[k]} (zip#{k}): {t}' for k,t in sorted(extra.items()))
out+='\n\n## Duplicates\n- None exact. Genie poses 106–115 are one off-model set (not applied).\n'
out+=f'\n## Prompts with no image yet ({len(miss)})\n'+'\n'.join(f'- #{m} {names[m]} → {P[m]}.png' for m in miss)+'\n'
open('MAPPING.md','w').write(out)
import json;json.dump({order[k]:P[v] for k,(v,c,n) in M.items()},open('map.json','w'))
print(len(M),len(miss),miss)
