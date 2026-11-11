import sys,os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
S=sys.argv[1]; OUT=sys.argv[2]; os.makedirs(OUT,exist_ok=True)
BG=(0x0A,0x0F,0x1E); C1=(0x2F,0x66,0xF0); C2=(0x2E,0xE6,0xB0); ON=(0xE8,0xEE,0xFF); SUB=(0xA7,0xB3,0xD1); OUTL=(0x2A,0x37,0x57)
FB='C:/Windows/Fonts/malgunbd.ttf'; FR='C:/Windows/Fonts/malgun.ttf'
def bg(W,H):
    img=Image.new('RGB',(W,H),BG); gl=Image.new('RGBA',(W,H),(0,0,0,0)); g=ImageDraw.Draw(gl)
    g.ellipse((-W*0.3,-H*0.15,W*0.7,H*0.35),fill=C1+(80,)); g.ellipse((W*0.5,-H*0.1,W*1.3,H*0.3),fill=C2+(45,))
    gl=gl.filter(ImageFilter.GaussianBlur(min(W,H)//6)); img.paste(gl,(0,0),gl); return img
def bar(d,x,y,w,h):
    for i in range(w):
        t=i/(w-1); d.line([(x+i,y),(x+i,y+h)],fill=tuple(int(C1[k]+(C2[k]-C1[k])*t) for k in range(3)))
def shot(img,src,box,radius):
    x,y,w,h=box; s=Image.open(src).convert('RGB').resize((w,h),Image.LANCZOS)
    m=Image.new('L',(w,h),0); ImageDraw.Draw(m).rounded_rectangle((0,0,w-1,h-1),radius,fill=255)
    sh=Image.new('RGBA',img.size,(0,0,0,0)); ImageDraw.Draw(sh).rounded_rectangle((x-6,y+10,x+w+6,y+h+22),radius+6,fill=(0,0,0,150))
    sh=sh.filter(ImageFilter.GaussianBlur(18)); img.paste(sh,(0,0),sh)
    img.paste(s,(x,y),m); ImageDraw.Draw(img).rounded_rectangle((x-2,y-2,x+w+1,y+h+1),radius+2,outline=OUTL,width=3)
def text_c(d,W,y,t,f,fill):
    b=d.textbbox((0,0),t,font=f); d.text(((W-(b[2]-b[0]))//2-b[0],y),t,font=f,fill=fill)
def portrait(name,src,title,sub):
    W,H=1080,1920; img=bg(W,H); d=ImageDraw.Draw(img)
    text_c(d,W,92,title,ImageFont.truetype(FB,68),ON); text_c(d,W,192,sub,ImageFont.truetype(FR,36),SUB)
    bar(d,W//2-60,262,120,6)
    h=1560; w=round(h*1080/2400); shot(img,src,((W-w)//2,320,w,h),44)
    img.save(os.path.join(OUT,name))
def landscape(name,src,title,sub):
    W,H=1920,1080; img=bg(W,H); d=ImageDraw.Draw(img)
    text_c(d,W,56,title,ImageFont.truetype(FB,60),ON); text_c(d,W,146,sub,ImageFont.truetype(FR,32),SUB)
    bar(d,W//2-60,206,120,6)
    w=1560; h=round(w*1080/2400); shot(img,src,((W-w)//2,268,w,h),36)
    img.save(os.path.join(OUT,name))
F=lambda n: os.path.join(S,'final',n)
portrait('01_main.png',F('01_main.png'),'링크만 넣으면 바로 재생','마그넷이나 .torrent 주소를 붙여넣으세요')
portrait('02_preparing.png',F('02_preparing.png'),'다 받기 전에 재생 시작','앞부분 조각부터 받아 빠르게 준비')
landscape('03_player_landscape.png',F('player_landscape.png'),'받으면서 보는 재생 화면','재생 위치와 받은 조각을 한눈에')
portrait('04_player.png',F('player_portrait.png'),'세로 화면에서도 그대로','10초 이동 · 확대 · 배속 조절')
portrait('05_playlist.png',F('02_playlist.png'),'재생 목록으로 한곳에서 관리','최근 50개를 기기에 암호화해 보관')
portrait('06_stored_files.png',F('03_stored_files.png'),'저장 공간도 간편하게 정리','형식별로 모아 보고 바로 삭제')
