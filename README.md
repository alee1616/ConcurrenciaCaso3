# Caja de Ventas Concurrente (JavaFX)

Tarea sumativa 3 · Sesión 3 · Indicadores IL3.1, IL3.2 e IL3.3

Aplicación JavaFX con **3 vendedores concurrentes**. Cada vendedor realiza **5 ventas de C$100**. Se puede iniciar y cancelar la simulación, se muestra el avance en tiempo real y los recursos compartidos están protegidos.

## Integrantes y responsabilidades

| Integrante | Responsabilidad principal | Archivos |
|---|---|---|
| _Nombre 1_ | Interfaz | `CajaController`, `caja-view.fxml`, `caja.css`, `CajaVentasApp` |
| _Nombre 2_ | Lógica concurrente | `CajaVentas`, `Vendedor`, `SimulacionService` |
| _Nombre 3_ | Pruebas y documentación | `CajaVentasTest`, capturas, informe, README |

> Todos los integrantes pueden explicar la solución completa.

## Repositorio

https://github.com/USUARIO/REPOSITORIO

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | _(poner resultado de `java -version`, ej. 21)_ |
| JavaFX | _(ver `pom.xml`, ej. 21)_ |
| Maven | Wrapper incluido (`mvnw`) |
| IDE usado | IntelliJ IDEA |

## Cómo ejecutar

### Opción 1: desde la terminal

```bash
git clone https://github.com/USUARIO/REPOSITORIO.git
cd REPOSITORIO
./mvnw javafx:run          # Linux / macOS
mvnw.cmd javafx:run        # Windows
```

### Opción 2: desde IntelliJ IDEA

1. Abrir la carpeta del proyecto (`File > Open`).
2. Esperar a que Maven descargue las dependencias.
3. Ejecutar la clase `LauncherCaja` (o `CajaVentasApp`).

## Cómo ejecutar las pruebas

```bash
./mvnw test
```

O en IntelliJ: clic derecho sobre `CajaVentasTest` > Run.

## Uso de la aplicación

- **Iniciar:** lanza 3 vendedores en segundo plano. Se deshabilita mientras haya una simulación activa (no se permiten inicios duplicados).
- **Cancelar:** pide a los vendedores que paren. Cada uno termina su venta en curso y no inicia otra. **Iniciar** solo se habilita cuando los 3 terminaron de verdad.
- **Nueva simulación:** se crean caja, bandera de cancelación, pool y tareas nuevas, con datos en cero.
- **Cerrar la ventana:** si hay vendedores activos, se cancela y se espera (sin congelar la interfaz) a que terminen antes de salir.

## Estructura del proyecto

```
src/main/java/com/example/concurrenciacaso3/
├── CajaVentas.java         # Recurso compartido (un solo monitor)
├── Vendedor.java           # Tarea de cada vendedor (Runnable)
├── SimulacionService.java  # Ciclo de vida: iniciar, cancelar, coordinar
├── CajaController.java     # Controlador de la interfaz
├── CajaVentasApp.java      # Aplicación JavaFX
└── LauncherCaja.java       # Punto de entrada
src/main/resources/com/example/concurrenciacaso3/
├── caja-view.fxml
└── caja.css
src/test/java/com/example/concurrenciacaso3/
└── CajaVentasTest.java
docs/
├── informe.pdf
└── capturas/
```

## Sincronización

- `CajaVentas` protege **contador, recaudación y avance por vendedor con el mismo monitor** (`synchronized`).
- `registrarVenta()` actualiza los tres datos como una sola unidad atómica.
- `snapshot()` lee todo bajo el mismo monitor, por lo que nunca se observa un estado intermedio (ej. 5 ventas con C$400).
- Invariante: `recaudación = ventas × C$100`.

## Cancelación y ciclo de vida

- **Cancelación cooperativa:** una bandera `AtomicBoolean` que `Vendedor` revisa antes de cada venta. No se mata ningún hilo.
- **Sin registros parciales:** la venta ya iniciada se termina y se registra completa.
- **Terminación confirmada:** un hilo coordinador espera a los 3 vendedores (`Future.get`, `awaitTermination`, `join`) y solo entonces publica la fase final (`COMPLETADA` o `CANCELADA`).
- **Hilo JavaFX libre:** la espera ocurre en el coordinador; los cambios de fase llegan a la interfaz con `Platform.runLater`.
- **Interfaz segura:** los vendedores nunca tocan controles; un `Timeline` lee un snapshot cada 100 ms.
- **Cierre limpio:** hilos daemon y cierre que espera la terminación efectiva.

## Pruebas de aceptación

| Prueba | Resultado esperado | Resultado obtenido | Evidencia |
|---|---|---|---|
| 5 ejecuciones completas | 15 ventas y C$1,500 en cada una | _completar_ | `docs/capturas/01-*.png` |
| Cancelar durante la simulación | Recaudación = ventas × 100; contador entre 0 y 15 estable | _completar_ | `docs/capturas/02-*.png` |
| Inicio duplicado | Rechazado, sin segunda simulación | _completar_ | `docs/capturas/03-*.png` |
| Cancelar y volver a iniciar | Nueva simulación con datos en cero y 15 ventas | _completar_ | `docs/capturas/04-*.png` |
| Cerrar con 3 vendedores activos | 0 trabajadores activos, proceso termina | _completar_ | `docs/capturas/05-*.png` |
| Pruebas automáticas | Todos los tests en verde | _completar_ | `docs/capturas/06-*.png` |

