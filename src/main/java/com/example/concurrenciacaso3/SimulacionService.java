package com.example.concurrenciacaso3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Ciclo de vida de la simulación (independiente de JavaFX para poder probarlo).
 * - iniciar(): caja, bandera, pool y 3 tareas NUEVAS en cada simulación;
 *   devuelve false si ya hay una simulación activa (sin duplicados).
 * - cancelar(): activa la bandera cooperativa; no bloquea.
 * - Un hilo coordinador espera la terminación efectiva de los 3 vendedores y solo
 *   entonces publica la fase final (COMPLETADA / CANCELADA).
 */
public final class SimulacionService {

    public enum Fase { LISTA, EJECUTANDO, CANCELANDO, COMPLETADA, CANCELADA }

    public static final int NUM_VENDEDORES = 3;
    public static final int VENTAS_ESPERADAS = NUM_VENDEDORES * Vendedor.VENTAS_POR_VENDEDOR;

    private static final AtomicInteger CONTADOR_SIMULACIONES = new AtomicInteger();

    private final int minMs;
    private final int maxMs;
    private final Consumer<Fase> alCambiarFase; // se invoca desde hilos NO-FX

    private final Object lock = new Object();
    private Fase fase = Fase.LISTA;
    private CajaVentas caja = new CajaVentas(NUM_VENDEDORES);
    private AtomicBoolean cancelacion = new AtomicBoolean();
    private List<Thread> hilos = List.of();
    private Thread coordinador;

    public SimulacionService(int minMs, int maxMs, Consumer<Fase> alCambiarFase) {
        if (minMs < 0 || maxMs < minMs) {
            throw new IllegalArgumentException("Rango de tiempo inválido: " + minMs + "-" + maxMs);
        }
        this.minMs = minMs;
        this.maxMs = maxMs;
        this.alCambiarFase = alCambiarFase;
    }

    /** @return true si se inició; false si ya había una simulación en curso. */
    public boolean iniciar() {
        final int n = CONTADOR_SIMULACIONES.incrementAndGet();
        final CajaVentas cajaNueva = new CajaVentas(NUM_VENDEDORES);   // datos restablecidos
        final AtomicBoolean cancelNueva = new AtomicBoolean(false);    // bandera nueva
        final List<Thread> hilosNuevos = new CopyOnWriteArrayList<>();
        final AtomicInteger idx = new AtomicInteger(1);
        final ExecutorService pool;

        synchronized (lock) {
            if (fase == Fase.EJECUTANDO || fase == Fase.CANCELANDO) {
                return false; // inicio duplicado
            }
            pool = Executors.newFixedThreadPool(NUM_VENDEDORES, r -> {
                Thread t = new Thread(r, "vendedor-" + idx.getAndIncrement() + "-sim" + n);
                t.setDaemon(true);
                hilosNuevos.add(t);
                return t;
            });
            caja = cajaNueva;
            cancelacion = cancelNueva;
            hilos = hilosNuevos;
            fase = Fase.EJECUTANDO;
        }
        notificar(Fase.EJECUTANDO);

        final List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < NUM_VENDEDORES; i++) {
            futuros.add(pool.submit(new Vendedor(i, cajaNueva, cancelNueva::get, minMs, maxMs)));
        }
        pool.shutdown();

        Thread coord = new Thread(() -> {
            try {
                for (Future<?> f : futuros) {
                    try {
                        f.get();
                    } catch (Exception ignorada) {
                        // un fallo no debe impedir esperar a los demás
                    }
                }
                pool.awaitTermination(10, TimeUnit.SECONDS);
                for (Thread t : hilosNuevos) {
                    t.join(2000); // terminación efectiva de cada hilo
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Fase finalFase = cajaNueva.snapshot().ventas() == VENTAS_ESPERADAS
                    ? Fase.COMPLETADA : Fase.CANCELADA;
            synchronized (lock) {
                fase = finalFase;
            }
            notificar(finalFase);
        }, "coordinador-sim" + n);
        coord.setDaemon(true);

        synchronized (lock) {
            coordinador = coord;
        }
        coord.start();
        return true;
    }

    /** Solicita cancelación cooperativa. No bloquea. */
    public boolean cancelar() {
        synchronized (lock) {
            if (fase != Fase.EJECUTANDO) {
                return false;
            }
            fase = Fase.CANCELANDO;
            cancelacion.set(true);
        }
        notificar(Fase.CANCELANDO);
        return true;
    }

    public Fase fase() {
        synchronized (lock) {
            return fase;
        }
    }

    public boolean activa() {
        Fase f = fase();
        return f == Fase.EJECUTANDO || f == Fase.CANCELANDO;
    }

    public CajaVentas.Estado estado() {
        CajaVentas c;
        synchronized (lock) {
            c = caja;
        }
        return c.snapshot();
    }

    /** Hilos vendedores todavía vivos (para verificar la terminación). */
    public int trabajadoresActivos() {
        List<Thread> copia;
        synchronized (lock) {
            copia = hilos;
        }
        int vivos = 0;
        for (Thread t : copia) {
            if (t.isAlive()) {
                vivos++;
            }
        }
        return vivos;
    }

    /**
     * Espera (para pruebas) a que la simulación actual termine de verdad.
     */
    public boolean esperarFin(long timeoutMs) throws InterruptedException {
        Thread c;
        synchronized (lock) {
            c = coordinador;
        }
        if (c == null) {
            return true;
        }
        c.join(timeoutMs);
        return !c.isAlive();
    }

    private void notificar(Fase f) {
        if (alCambiarFase != null) {
            alCambiarFase.accept(f);
        }
    }
}