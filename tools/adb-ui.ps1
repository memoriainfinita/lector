# Probar LECTOR en el móvil por adb. Cargar con: . .\tools\adb-ui.ps1
#
# Requisitos: adb (el de scrcpy está en el PATH), Python con Pillow (Small) y sqlite3 (Db).
# El Xiaomi solo acepta toques por adb con "Depuración USB (ajustes de seguridad)" activado,
# y tras activarlo hizo falta reiniciar el móvil. Comprobar: adb shell input keyevent 0 2>&1
# y buscar "SecurityException" en la salida.
#
# Antes de probar, en este orden:
#   1. Copia de la base de datos y de los ajustes: adb shell am force-stop codelab.lector, y
#      adb exec-out run-as codelab.lector cat databases/lector.db (y -wal, -shm) y
#      files/datastore/settings.preferences_pb a archivos locales. No abrir esa copia con SQLite:
#      integra el -wal y la deja distinta. Al terminar, restaurarla con adb push a
#      /data/local/tmp y run-as codelab.lector cp.
#   2. Volumen multimedia a 0 si algo puede sonar: input keyevent KEYCODE_VOLUME_DOWN repetido
#      (cmd media_session volume --set no funciona en este móvil). Anotar el valor de antes con
#      dumpsys audio (STREAM_MUSIC, streamVolume) y devolverlo al final.
#   3. Borrar del móvil y cambios de clase de carpeta: solo sobre copias de prueba de un libro
#      pequeño en una carpeta propia, nunca sobre libros reales.
#
# Cuidado:
#   - uiautomator dump falla sin avisar mientras hay animaciones (la barra de "buscando libros"):
#     Nodes borra el volcado anterior y reintenta, para no leer uno viejo.
#   - Un volcado tarda 2-3 s: los avisos con "Deshacer" (5 s) se pueden perder, y un toque por
#     coordenada cae en lo que haya debajo. No tocar a ciegas donde haya acciones peligrosas
#     (así se abrió "Borrar del móvil" de un libro real); localizar por texto exacto con TapText.
#   - Las posiciones cambian con la orientación y con la sesión de escucha (la fila de filtros
#     sube cuando no está "Seguir escuchando").

$S = Join-Path $env:TEMP 'lector-shots'
New-Item -ItemType Directory -Force $S | Out-Null

# Captura completa en $S.
function Shot([string]$name) { cmd /c "adb exec-out screencap -p > `"$S\$name.png`""; "$S\$name.png" }

# Captura reducida (540 de ancho, vertical) para revisarla.
function Small([string]$name) {
    Shot $name | Out-Null
    python -c "from PIL import Image; im=Image.open(r'$S\$name.png'); im.resize((540,1200)).save(r'$S\$name-s.png')"
    "$S\$name-s.png"
}

# Árbol de la interfaz: texto, descripción, centro y límites de cada elemento.
function Nodes {
    $ok = $false
    foreach ($i in 1..8) {
        adb shell rm -f /sdcard/ui.xml
        $out = adb shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-String
        if ($out -match 'dumped to') { $ok = $true; break }
        Start-Sleep -Milliseconds 800
    }
    if (-not $ok) { throw "volcado fallido" }
    [xml]$x = adb exec-out cat /sdcard/ui.xml
    $x.SelectNodes('//node') | ForEach-Object {
        if ($_.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
            [pscustomobject]@{
                text = $_.text; desc = $_.'content-desc'; click = $_.clickable
                x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2); y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
                b = $_.bounds
            }
        }
    }
}

# Elemento por texto o descripción exactos. Una coincidencia parcial puede caer en otro
# elemento ("Undo" dentro de "can't be undone").
function Find([string]$t) {
    Nodes | Where-Object { $_.text -eq $t -or $_.desc -eq $t } | Select-Object -First 1
}

function TapText([string]$t, [int]$wait = 800) {
    $h = Find $t
    if (-not $h) { "NO ENCONTRADO: $t"; return }
    adb shell input tap $h.x $h.y
    Start-Sleep -Milliseconds $wait
    "tap '$t' @ $($h.x),$($h.y)"
}

function Tap([int]$x, [int]$y, [int]$wait = 800) { adb shell input tap $x $y; Start-Sleep -Milliseconds $wait }
function Back([int]$wait = 800) { adb shell input keyevent KEYCODE_BACK; Start-Sleep -Milliseconds $wait }

# Lista legible de lo que hay en pantalla.
function Texts { Nodes | Where-Object { $_.text -or $_.desc } | ForEach-Object { "$($_.text)$(if($_.desc){' {'+$_.desc+'}'}) @$($_.x),$($_.y)" } }

# Consulta la base de datos del móvil sobre una copia aparte (nunca sobre la copia de seguridad).
function Db([string]$sql) {
    $d = Join-Path $env:TEMP 'lector-probe'; New-Item -ItemType Directory -Force $d | Out-Null
    foreach ($f in 'lector.db','lector.db-wal','lector.db-shm') { Remove-Item "$d\$f" -ErrorAction SilentlyContinue; cmd /c "adb exec-out run-as codelab.lector cat databases/$f > `"$d\$f`" 2>nul" }
    python -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); [print(r) for r in c.execute(sys.argv[2])]" "$d\lector.db" $sql
}
