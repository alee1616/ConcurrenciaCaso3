package com.example.concurrenciacaso3;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

/**
 * Tarea de un vendedor: realiza hasta 5 ventas de C$100.
 * Cancelación cooperativa: la bandera se consulta ANTES de iniciar cada venta.
 * Una venta ya iniciada se termina y se registra completa (sin registros parciales).
 */
public final class Vendedor implements Runnable {

    public static final int VENTAS_POR_VENDEDOR = 5;

    private final int indice;
    private final CajaVentas caja;
    private final BooleanSupplier cancelado;
    private final int minMs;
    private final int maxMs;

    public Vendedor(int indice, CajaVentas caja, BooleanSupplier cancelado, int minMs, int maxMs) {
        if (indice < 0) {
            throw new IllegalArgumentException("Índice de vendedor inválido: " + indice);
        }
        if (minMs < 0 || maxMs < minMs) {
            throw new IllegalArgumentException("Rango de tiempo inválido: " + minMs + "-" + maxMs);
        }
        this.indice = indice;
        this.caja = Objects.requireNonNull(caja, "caja");
        this.cancelado = Objects.requireNonNull(cancelado, "cancelado");
        this.minMs = minMs;
        this.maxMs = maxMs;
    }

    @Override
    public void run() {
        for (int i = 0; i < VENTAS_POR_VENDEDOR; i++) {
            if (cancelado.getAsBoolean()) {
                return; // no se inicia una venta nueva
            }
            atenderCliente();            // trabajo fuera de la sección crítica
            caja.registrarVenta(indice); // sección crítica corta y atómica
        }
    }

    private void atenderCliente() {
        try {
            long ms = maxMs > minMs
                    ? ThreadLocalRandom.current().nextLong(minMs, maxMs + 1)
                    : minMs;
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // la venta en curso igual se registra
        }
    }
}