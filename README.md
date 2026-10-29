# Escáner Flotante

App gratis para Android: un **botón flotante** que queda arriba de cualquier app. Lo tocás, se abre una ventanita con la cámara, lee el código de barras y **lo escribe en el campo donde estabas**, sin cambiar tu teclado.

Ideal para cargar pedidos, stock o precios en planillas, sistemas web, WhatsApp, etc.

## Descargar

Desde el celular, abrí este link y descargá el APK:

**Releases → última versión → `EscanerFlotante.apk`**

(Link directo: `https://github.com/BrianMCabezas/escaner-flotante/releases/latest/download/EscanerFlotante.apk`)

## Instalar y configurar

1. Abrí el APK descargado. Si Android pregunta, permití instalar apps de esta fuente.
2. Abrí **Escáner Flotante** y tocá **Dar permiso de cámara**.
3. Tocá **Abrir Accesibilidad**, buscá **Escáner Flotante** y activalo.
   - Si aparece gris o dice *"Ajuste restringido"* (Android 13+): volvé a la app, tocá **Abrir info de la app** → menú ⋮ arriba a la derecha → **Permitir ajustes restringidos**. Después activalo de nuevo en Accesibilidad.
4. Listo: aparece el botón azul. Probalo en el campo de prueba de la app.

## Uso

- **Tocar** el botón: escanea y escribe el código en el campo seleccionado.
- **Arrastrar**: lo movés de lugar.
- **Mantener apretado**: abre los ajustes.
- Si no había ningún campo seleccionado, el código se **copia** para que lo pegues.

## Ajustes

- Después de escribir: no hacer nada / apretar Enter / pasar al siguiente campo.
- Vibrar y pitido al leer.
- Mostrar u ocultar el botón.

## Batería y privacidad

- La cámara se prende **solo** al tocar el botón y se apaga apenas lee el código (o a los 20 segundos).
- La lectura se hace en el celular, **sin internet**.
- El permiso de Accesibilidad se usa solo para escribir el código en el campo; la app no guarda ni envía nada.

## Para desarrolladores

Kotlin + CameraX + ML Kit (barcode-scanning, incluido en el APK). Cada push a `main` compila el APK con GitHub Actions y lo publica en Releases.

La clave de firma (`app/escaner.keystore`) está en el repo para que las compilaciones sean actualizables sin desinstalar. Si vas a distribuir tu propia versión, generá tu propia clave y movela a GitHub Secrets.
