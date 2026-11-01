import json,numpy as np
from PIL import Image
from scipy import ndimage as nd
M=json.load(open('map.json'));R='../../app/src/main/res/drawable-nodpi/'
BG=('bg_home','bg_scan','bg_vpn_map')
big=('btn_scan','btn_vpn','ill_','onb_','empty_','frame_','fx_','bg_','banner')
log=[]
for src,t in M.items():
  im=Image.open(src).convert('RGB'); a=np.asarray(im).astype(float)
  if t.startswith(BG):
    w=720; im=im.resize((w,int(im.height*w/im.width)),Image.LANCZOS); im.save(R+t+'.webp',quality=55,method=6); continue
  c=a[:6,:6].reshape(-1,3).mean(0); white=c.mean()>128
  if white:
    d=(255-a).max(2); bgm=d<22
  else:
    d=a.max(2); bgm=d<26
  lab,_=nd.label(bgm); edge=set(np.unique(np.r_[lab[0],lab[-1],lab[:,0],lab[:,-1]]))-{0}
  bg=np.isin(lab,list(edge))
  alpha=np.where(bg,0.0,1.0)
  if white:
    soft=np.clip(d/40,0,1)  # anti-alias near edge
    near=nd.binary_dilation(bg,iterations=2)&~bg
    alpha=np.where(near,np.maximum(soft,0.0),alpha)
  else:
    glow=np.clip((d-8)/200,0,1)**0.8
    alpha=np.where(bg,glow,alpha)
    # unpremultiply glow pixels on black
    m=bg&(alpha>0.02); a[m]=np.clip(a[m]/alpha[m][:,None],0,255)
  alpha=nd.gaussian_filter(alpha,0.6)*(1-bg*0)  # tiny feather
  rgba=np.dstack([a,alpha*255]).astype(np.uint8); o=Image.fromarray(rgba,'RGBA')
  ys,xs=np.where(alpha>0.04); box=[xs.min(),ys.min(),xs.max()+1,ys.max()+1]
  o=o.crop(box); wide=o.width>o.height*1.4
  if not wide:  # square-pad
    s=max(o.size); sq=Image.new('RGBA',(s,s),(0,0,0,0)); sq.paste(o,((s-o.width)//2,(s-o.height)//2)); o=sq
  mx=512 if t.startswith(big) else 256
  if wide: mx=600
  o.thumbnail((mx,mx),Image.LANCZOS); o.save(R+t+'.webp',quality=72,method=6)
