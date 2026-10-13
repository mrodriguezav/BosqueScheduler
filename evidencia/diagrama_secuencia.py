import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
plt.rcParams["font.family"] = "DejaVu Sans"
fig, ax = plt.subplots(figsize=(7.4, 7.0), dpi=250)
ax.set_xlim(0, 100); ax.set_ylim(2, 95); ax.axis("off")
xc, xg, xv, xr, xh = 10, 29, 49, 68, 87
act = [("Coordinador", xc), ("Grilla Angular\n+ Controller", xg), ("Validador-\nRestricciones", xv), ("Regla\n(cada una)", xr), ("Horario", xh)]
top = 92
for name, x in act:
    ax.add_patch(Rectangle((x-8.5, top-1.2), 17, 5.4, fc="#e3f0f0", ec="k", lw=0.9, zorder=3))
    ax.text(x, top+1.5, name, ha="center", va="center", fontsize=6.8, weight="bold", zorder=4)
    ax.plot([x, x], [top-1.2, 4], ls=(0, (4, 3)), lw=0.7, color="#555", zorder=1)
def bar(x, y1, y2): ax.add_patch(Rectangle((x-0.9, y2), 1.8, y1-y2, fc="white", ec="k", lw=0.8, zorder=2))
def msg(x1, x2, y, t, ret=False, pad=0.9):
    ax.annotate("", xy=(x2, y), xytext=(x1, y), arrowprops=dict(arrowstyle="->", lw=0.9, ls="--" if ret else "-", color="k"), zorder=3)
    ax.text((x1+x2)/2, y+pad, t, ha="center", va="bottom", fontsize=6.1, bbox=dict(fc="white", ec="none", pad=0.6), zorder=5)
def selfcall(x, y, t):
    ax.plot([x+0.9, x+7, x+7], [y, y, y-3.2], lw=0.9, color="k", zorder=3)
    ax.annotate("", xy=(x+0.9, y-3.2), xytext=(x+7, y-3.2), arrowprops=dict(arrowstyle="->", lw=0.9, color="k"), zorder=3)
    ax.text(x+8, y-1.6, t, ha="left", va="center", fontsize=6.1, bbox=dict(fc="white", ec="none", pad=0.6), zorder=5)
def frame(x1, x2, y1, y2, tag, cond):
    ax.add_patch(Rectangle((x1, y2), x2-x1, y1-y2, fill=False, ec="#444", lw=0.9, zorder=2))
    w = 4.2 + 1.3*len(tag)
    ax.add_patch(Rectangle((x1, y1-3.2), w, 3.2, fc="#eee", ec="#444", lw=0.9, zorder=4)); ax.text(x1+0.8, y1-1.6, tag, fontsize=6, va="center", zorder=5)
    ax.text(x1+w+1.2, y1-1.6, cond, fontsize=6, va="center", zorder=5, bbox=dict(fc="white", ec="none", pad=0.4))

bar(xg, 83, 11)
msg(xc, xg, 83, "1: soltar s sobre el bloque b")
selfcall(xg, 78, "1.1: construir la candidata c (s con bloqueId = b)")
bar(xv, 71, 47.5)
msg(xg, xv, 71, "2: evaluar(c, horario)")
frame(42, 99, 69.2, 51.2, "loop", "[para cada regla r registrada]")
bar(xr, 63.5, 54)
msg(xv, xr, 63.5, "3: evaluar(c, horario)")
bar(xh, 60.5, 57.2)
msg(xr, xh, 60.5, "3.1: consultar estado")
msg(xh, xr, 57.2, "3.2: ocupantes y conteos", ret=True)
msg(xr, xv, 54, "3.3: Violacion[] (vacía si cumple)", ret=True)
msg(xv, xg, 47.5, "4: Violacion[] (duras y avisos)", ret=True)
frame(3, 99, 45, 7, "alt", "[existe una violación de severidad dura]")
msg(xg, xc, 38.5, "5a: rechazo con regla y motivo", ret=True)
ax.text(xg+2, 38.6, "(RF2). s no se mueve", fontsize=6.1, va="bottom", zorder=5)
ax.plot([3, 99], [32.5, 32.5], ls=(0, (6, 3)), lw=0.8, color="#444", zorder=2)
ax.text(5, 31.4, "[solo avisos o ninguna violación]", fontsize=6, va="top", zorder=5, bbox=dict(fc="white", ec="none", pad=0.4))
bar(xh, 27, 14.5)
msg(xg, xh, 27, "5b: mover(s.id, b.id, s.aulaId)")
ax.plot([xh+0.9, xh+6, xh+6], [22.5, 22.5, 19.3], lw=0.9, color="k", zorder=3)
ax.annotate("", xy=(xh+0.9, 19.3), xytext=(xh+6, 19.3), arrowprops=dict(arrowstyle="->", lw=0.9, color="k"), zorder=3)
ax.text(xh-1.8, 20.9, "5b.1: quitar(id); colocar(s')", ha="right", va="center", fontsize=6.1, bbox=dict(fc="white", ec="none", pad=0.6), zorder=5)
msg(xh, xg, 14.5, "5c: estado actualizado", ret=True)
msg(xg, xc, 9.6, "6: mostrar aceptación y avisos", ret=True)
ax.text(xg+2, 9.7, "(p. ej. RB1, RF4)", fontsize=6.1, va="bottom", zorder=5)
fig.savefig("evidencia/diagrama_secuencia.png", bbox_inches="tight", facecolor="white")
