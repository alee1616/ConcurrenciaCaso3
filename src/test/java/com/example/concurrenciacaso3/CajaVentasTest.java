package com.example.concurrenciacaso3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CajaVentasTest {

    @Test
    void cincoEjecucionesCompletasTerminanEn15VentasY1500() throws Exception {
        for (int corrida = 1; corrida <= 5; corrida++) {
            SimulacionService s = new SimulacionService(5, 15, null);
            assertTrue(s.iniciar());
            assertTrue(s.esperarFin(10_000));
            CajaVentas.Estado e = s.estado();
            assertEquals(SimulacionService.Fase.COMPLETADA, s.fase());
            assertEquals(15, e.ventas(), "corrida " + corrida);
            assertEquals(1500, e.recaudacion(), "corrida " + corrida);
            assertTrue(e.consistente());
            assertEquals(0, s.trabajadoresActivos());
        }
    }

    @Test
    void cancelarConservaSoloVentasConfirmadas() throws Exception {
        SimulacionService s = new SimulacionService(100, 200, null);
        assertTrue(s.iniciar());
        Thread.sleep(250);
        assertTrue(s.cancelar());
        assertTrue(s.esperarFin(10_000));
        CajaVentas.Estado e = s.estado();
        assertEquals(SimulacionService.Fase.CANCELADA, s.fase());
        assertEquals(e.ventas() * CajaVentas.PRECIO_VENTA, e.recaudacion());
        assertTrue(e.consistente());
        assertTrue(e.ventas() >= 0 && e.ventas() < 15);
        assertEquals(0, s.trabajadoresActivos());
    }

    @Test
    void inicioDuplicadoEsRechazado() throws Exception {
        SimulacionService s = new SimulacionService(50, 80, null);
        assertTrue(s.iniciar());
        assertFalse(s.iniciar());
        assertTrue(s.esperarFin(10_000));
    }

    @Test
    void cancelarSinSimulacionEnEjecucionDevuelveFalse() {
        SimulacionService s = new SimulacionService(50, 80, null);
        assertFalse(s.cancelar());
    }

    @Test
    void cancelarYVolverAIniciarUsaTareasNuevasYDatosLimpios() throws Exception {
        SimulacionService s = new SimulacionService(50, 80, null);
        assertTrue(s.iniciar());
        Thread.sleep(100);
        s.cancelar();
        assertTrue(s.esperarFin(10_000));
        assertTrue(s.iniciar());
        assertTrue(s.esperarFin(10_000));
        assertEquals(15, s.estado().ventas());
        assertEquals(1500, s.estado().recaudacion());
    }

    @Test
    void lecturasConcurrentesNuncaVenEstadoInconsistente() throws Exception {
        CajaVentas caja = new CajaVentas(3, 50_000);
        AtomicBoolean fin = new AtomicBoolean(false);
        AtomicBoolean inconsistente = new AtomicBoolean(false);

        Thread lector = new Thread(() -> {
            while (!fin.get()) {
                if (!caja.snapshot().consistente()) {
                    inconsistente.set(true);
                }
            }
        });
        lector.start();

        Thread[] escritores = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            escritores[i] = new Thread(() -> {
                for (int k = 0; k < 50_000; k++) {
                    caja.registrarVenta(idx);
                }
            });
            escritores[i].start();
        }
        for (Thread t : escritores) {
            t.join();
        }
        fin.set(true);
        lector.join();

        assertFalse(inconsistente.get());
        assertEquals(150_000, caja.snapshot().ventas());
        assertEquals(15_000_000, caja.snapshot().recaudacion());
    }

    // validaciones

    @Test
    void rechazaVendedorInexistenteSinModificarNada() {
        CajaVentas caja = new CajaVentas(3);
        assertThrows(IllegalArgumentException.class, () -> caja.registrarVenta(3));
        assertThrows(IllegalArgumentException.class, () -> caja.registrarVenta(-1));
        assertEquals(0, caja.snapshot().ventas());
        assertEquals(0, caja.snapshot().recaudacion());
    }

    @Test
    void rechazaLaSextaVentaDeUnVendedor() {
        CajaVentas caja = new CajaVentas(3);
        for (int i = 0; i < 5; i++) {
            caja.registrarVenta(0);
        }
        assertThrows(IllegalStateException.class, () -> caja.registrarVenta(0));
        assertEquals(5, caja.snapshot().ventas());
        assertEquals(500, caja.snapshot().recaudacion());
        assertTrue(caja.snapshot().consistente());
    }

    @Test
    void rechazaParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new CajaVentas(0));
        assertThrows(IllegalArgumentException.class, () -> new SimulacionService(-1, 5, null));
        assertThrows(IllegalArgumentException.class, () -> new SimulacionService(10, 5, null));
        assertThrows(NullPointerException.class,
                () -> new Vendedor(0, null, () -> false, 1, 2));
    }
}