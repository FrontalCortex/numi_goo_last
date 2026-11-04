# Uygulama ikonunun bütün dosyalarını tek bir saydam PNG'den (design/sobi_ikon_kaynak.png) üretir.
#
# Üretilenler:
#   mipmap-*dpi/ic_launcher_foreground.png   uyarlanabilir ikonun ön katmanı (108dp)
#   mipmap-*dpi/ic_launcher_monochrome.png   Android 13 temalı ikon (tek renk; gözler, burun, ağız, iç kulak delik)
#   mipmap-*dpi/ic_launcher.png / _round.png Android 8.0 öncesi hazır ikonlar (48dp)
#   drawable-*dpi/ic_notification.png        bildirim küçük ikonu (24dp, tek renk silüet)
#   src/main/ic_launcher-playstore.png       Play Store ikonu (512x512, köşesiz; köşeyi Play yuvarlıyor)
#
# Kullanım (depo kökünden):  powershell -ExecutionPolicy Bypass -File tools\ikon_uret.ps1 [-Onizleme yol.png]
param(
    [string]$Kaynak = "design\sobi_ikon_kaynak.png",
    [string]$Onizleme = ""
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$kod = @"
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

public static class IkonUret {
    // Arka katman rengi: drawable/ic_launcher_background.xml ile aynı olmalı.
    static readonly Color Zemin = Color.FromArgb(0x5B, 0x3F, 0xD9);

    static byte[] Oku(Bitmap b) {
        var r = b.LockBits(new Rectangle(0, 0, b.Width, b.Height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        var d = new byte[r.Stride * b.Height];
        System.Runtime.InteropServices.Marshal.Copy(r.Scan0, d, 0, d.Length);
        b.UnlockBits(r);
        return d;
    }
    static Bitmap Yaz(byte[] d, int w, int h) {
        var b = new Bitmap(w, h, PixelFormat.Format32bppArgb);
        var r = b.LockBits(new Rectangle(0, 0, w, h), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        System.Runtime.InteropServices.Marshal.Copy(d, 0, r.Scan0, d.Length);
        b.UnlockBits(r);
        return b;
    }

    // Kaynağı temizler (silme artığı yarı saydam kırıntılar sıfır, içteki 253-254 alfa tam opak),
    // içeriğe göre kırpar. merkez: içeriğin, en uzak noktası en yakın olacak şekilde seçilmiş merkezi.
    public static Bitmap Hazirla(string yol, out PointF merkez, out float yaricap) {
        Bitmap ham;
        using (var f = new Bitmap(yol)) ham = new Bitmap(f);
        int w = ham.Width, h = ham.Height;
        var d = Oku(ham);
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w * 4 + x * 4;
            byte a = d[i + 3];
            if (a <= 40) { d[i] = d[i + 1] = d[i + 2] = d[i + 3] = 0; continue; }
            if (a >= 230) d[i + 3] = 255;
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
        }
        var temiz = Yaz(d, w, h);
        ham.Dispose();
        var kirp = temiz.Clone(new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1), PixelFormat.Format32bppArgb);
        temiz.Dispose();

        // En küçük kapsayan daireye yakın merkez: kaba ızgarada ara.
        var kd = Oku(kirp); int kw = kirp.Width, kh = kirp.Height;
        var px = new System.Collections.Generic.List<Point>();
        for (int y = 0; y < kh; y += 3) for (int x = 0; x < kw; x += 3)
            if (kd[y * kw * 4 + x * 4 + 3] > 128) px.Add(new Point(x, y));
        float enIyiR = float.MaxValue; PointF enIyi = new PointF(kw / 2f, kh / 2f);
        for (int cy = kh / 3; cy <= kh * 2 / 3; cy += 4) for (int cx = kw * 2 / 5; cx <= kw * 3 / 5; cx += 4) {
            float m = 0;
            foreach (var p in px) {
                float dx = p.X - cx, dy = p.Y - cy, dd = dx * dx + dy * dy;
                if (dd > m) { m = dd; if (m >= enIyiR) break; }
            }
            if (m < enIyiR) { enIyiR = m; enIyi = new PointF(cx, cy); }
        }
        merkez = enIyi; yaricap = (float)Math.Sqrt(enIyiR) + 2;
        return kirp;
    }

    // Tek renk sürüm: beyaz, alfa = kaynak alfası; koyu yüz çizgileri ve sarı iç kulaklar delik.
    public static Bitmap TekRenk(Bitmap k) {
        var d = Oku(k);
        for (int i = 0; i < d.Length; i += 4) {
            int b = d[i], g = d[i + 1], r = d[i + 2];
            int lum = (r * 299 + g * 587 + b * 114) / 1000;
            bool koyu = lum < 90;
            bool sari = r > 200 && g > 140 && b < 120;
            if (koyu || sari) d[i + 3] = 0;
            d[i] = d[i + 1] = d[i + 2] = 255;
        }
        return Yaz(d, k.Width, k.Height);
    }

    static Graphics G(Bitmap b) {
        var g = Graphics.FromImage(b);
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        g.SmoothingMode = SmoothingMode.AntiAlias;
        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
        g.CompositingQuality = CompositingQuality.HighQuality;
        return g;
    }

    // Kaynağı, merkezi (cx,cy)'ye ve yarıçapı r piksele gelecek şekilde çizer.
    static void Ciz(Graphics g, Bitmap k, PointF m, float kr, float cx, float cy, float r) {
        float s = r / kr;
        using (var ia = new ImageAttributes()) {
            ia.SetWrapMode(WrapMode.TileFlipXY); // kenarda bicubic sızıntısı olmasın
            var hedef = new Rectangle((int)Math.Round(cx - m.X * s), (int)Math.Round(cy - m.Y * s),
                                      (int)Math.Round(k.Width * s), (int)Math.Round(k.Height * s));
            g.DrawImage(k, hedef, 0, 0, k.Width, k.Height, GraphicsUnit.Pixel, ia);
        }
    }

    // Uyarlanabilir katman: 108dp tuval, içerik 33.5dp yarıçaplı daireye (güvenli alan 33dp) sığar.
    public static Bitmap Katman(Bitmap k, PointF m, float kr, int boy, bool zeminli) {
        var b = new Bitmap(boy, boy, PixelFormat.Format32bppArgb);
        using (var g = G(b)) {
            g.Clear(zeminli ? Zemin : Color.Transparent);
            Ciz(g, k, m, kr, boy / 2f, boy / 2f, boy * 33.5f / 108f);
        }
        return b;
    }

    // Eski cihaz ikonu: 48dp; görünen kısım uyarlanabilir ikonun ortadaki 72dp'si.
    // yuvarlak: daire; değilse 1dp kenar boşluklu yuvarlatılmış kare.
    public static Bitmap Eski(Bitmap k, PointF m, float kr, int boy, bool yuvarlak) {
        var b = new Bitmap(boy, boy, PixelFormat.Format32bppArgb);
        float dp = boy / 48f;
        using (var g = G(b)) {
            g.Clear(Color.Transparent);
            var yol = new GraphicsPath();
            if (yuvarlak) yol.AddEllipse(0.5f * dp, 0.5f * dp, boy - dp, boy - dp);
            else {
                float x = 1 * dp, w = boy - 2 * dp, c = 9 * dp;
                yol.AddArc(x, x, c, c, 180, 90); yol.AddArc(x + w - c, x, c, c, 270, 90);
                yol.AddArc(x + w - c, x + w - c, c, c, 0, 90); yol.AddArc(x, x + w - c, c, c, 90, 90);
                yol.CloseFigure();
            }
            using (var f = new SolidBrush(Zemin)) g.FillPath(f, yol);
            g.SetClip(yol);
            // 108dp katmanın 72dp'si boy pikseline geliyor: ölçek boy/72 dp başına.
            Ciz(g, k, m, kr, boy / 2f, boy / 2f, 33.5f * boy / 72f);
        }
        return b;
    }

    // Bildirim ikonu: 24dp, uzun kenar ortadaki 20dp'ye.
    public static Bitmap Bildirim(Bitmap tek, int boy) {
        var b = new Bitmap(boy, boy, PixelFormat.Format32bppArgb);
        using (var g = G(b)) {
            g.Clear(Color.Transparent);
            float s = boy * 20f / 24f / Math.Max(tek.Width, tek.Height);
            float w = tek.Width * s, h = tek.Height * s;
            using (var ia = new ImageAttributes()) {
                ia.SetWrapMode(WrapMode.TileFlipXY);
                g.DrawImage(tek, new Rectangle((int)Math.Round((boy - w) / 2), (int)Math.Round((boy - h) / 2), (int)Math.Round(w), (int)Math.Round(h)),
                            0, 0, tek.Width, tek.Height, GraphicsUnit.Pixel, ia);
            }
        }
        return b;
    }

    // Play Store: 512x512 kare; uyarlanabilir ikonun ortadaki 72dp'si.
    public static Bitmap Play(Bitmap k, PointF m, float kr) {
        var b = new Bitmap(512, 512, PixelFormat.Format32bppArgb);
        using (var g = G(b)) {
            g.Clear(Zemin);
            Ciz(g, k, m, kr, 256, 256, 33.5f * 512f / 72f);
        }
        return b;
    }

    public static void Kaydet(Bitmap b, string yol) {
        Directory.CreateDirectory(Path.GetDirectoryName(yol));
        b.Save(yol, ImageFormat.Png);
        b.Dispose();
    }

    // Önizleme: daire ve yuvarlatılmış kare maskeleri, farklı boylarda; tek renk ve bildirim.
    public static void Onizleme(Bitmap k, Bitmap tek, PointF m, float kr, string yol) {
        int[] boylar = { 192, 96, 48 };
        var o = new Bitmap(1100, 560, PixelFormat.Format32bppArgb);
        using (var g = G(o)) {
            g.Clear(Color.FromArgb(40, 40, 46));
            int x = 20;
            foreach (bool daire in new[] { true, false }) {
                foreach (int s in boylar) {
                    using (var tam = Katman(k, m, kr, s * 108 / 72, true)) {
                        var yolM = new GraphicsPath();
                        if (daire) yolM.AddEllipse(x, 20, s, s); else yolM.AddRectangle(new Rectangle(x, 20, s, s));
                        if (!daire) { yolM = new GraphicsPath(); float c = s * 0.45f;
                            yolM.AddArc(x, 20, c, c, 180, 90); yolM.AddArc(x + s - c, 20, c, c, 270, 90);
                            yolM.AddArc(x + s - c, 20 + s - c, c, c, 0, 90); yolM.AddArc(x, 20 + s - c, c, c, 90, 90); yolM.CloseFigure(); }
                        g.SetClip(yolM);
                        g.DrawImage(tam, x - s * 18 / 72, 20 - s * 18 / 72, tam.Width, tam.Height);
                        g.ResetClip();
                    }
                    x += s + 20;
                }
            }
            // güvenli alan çemberi gösterimi (büyük katman)
            using (var tam = Katman(k, m, kr, 324, true)) {
                g.DrawImage(tam, 20, 230, 324, 324);
                using (var p = new Pen(Color.Yellow, 2)) g.DrawEllipse(p, 20 + 162 - 99, 230 + 162 - 99, 198, 198);
                using (var p = new Pen(Color.Orange, 1)) g.DrawRectangle(p, 20 + 54, 230 + 54, 216, 216);
            }
            using (var tk = Katman(tek, m, kr, 324, false)) {
                using (var f = new SolidBrush(Color.FromArgb(30, 60, 90))) g.FillEllipse(f, 380, 230, 324, 324);
                g.DrawImage(tk, 380, 230, 324, 324);
            }
            using (var bi = Bildirim(tek, 96)) g.DrawImage(bi, 740, 230, 96, 96);
            using (var bi = Bildirim(tek, 24)) g.DrawImage(bi, 860, 230, 24, 24);
            using (var p = Play(k, m, kr)) g.DrawImage(p, 740, 360, 180, 180);
        }
        Kaydet(o, yol);
    }
}
"@
Add-Type -TypeDefinition $kod -ReferencedAssemblies System.Drawing

$res = "app\src\main\res"
$m = New-Object System.Drawing.PointF
$r = [float]0
$k = [IkonUret]::Hazirla((Resolve-Path $Kaynak).Path, [ref]$m, [ref]$r)
$tek = [IkonUret]::TekRenk($k)
Write-Host "kaynak kırpıldı: $($k.Width)x$($k.Height), merkez $m, yarıçap $r"

if ($Onizleme) { [IkonUret]::Onizleme($k, $tek, $m, $r, $Onizleme); Write-Host "önizleme: $Onizleme"; return }

$kok = (Get-Location).Path
$yogunluk = [ordered]@{ mdpi = 1.0; hdpi = 1.5; xhdpi = 2.0; xxhdpi = 3.0; xxxhdpi = 4.0 }
foreach ($y in $yogunluk.Keys) {
    $c = $yogunluk[$y]
    $mip = Join-Path $kok "$res\mipmap-$y"
    [IkonUret]::Kaydet([IkonUret]::Katman($k, $m, $r, [int](108 * $c), $false), "$mip\ic_launcher_foreground.png")
    [IkonUret]::Kaydet([IkonUret]::Katman($tek, $m, $r, [int](108 * $c), $false), "$mip\ic_launcher_monochrome.png")
    [IkonUret]::Kaydet([IkonUret]::Eski($k, $m, $r, [int](48 * $c), $false), "$mip\ic_launcher.png")
    [IkonUret]::Kaydet([IkonUret]::Eski($k, $m, $r, [int](48 * $c), $true), "$mip\ic_launcher_round.png")
    [IkonUret]::Kaydet([IkonUret]::Bildirim($tek, [int](24 * $c)), (Join-Path $kok "$res\drawable-$y\ic_notification.png"))
}
[IkonUret]::Kaydet([IkonUret]::Play($k, $m, $r), (Join-Path $kok "app\src\main\ic_launcher-playstore.png"))
Write-Host "ikon dosyaları üretildi"
