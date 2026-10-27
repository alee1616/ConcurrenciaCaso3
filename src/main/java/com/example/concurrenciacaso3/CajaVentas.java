package com.example.concurrenciacaso3;

import java.util.Arrays;

/**
 * Recurso compartido: contador de ventas + recaudación (+ avance por vendedor).
 * Todo el estado se protege con UN solo monitor (this):
 *  - registrarVenta() valida y luego actualiza contador, recaudación y avance como
 *    una unidad atómica (si la validación falla no se modifica nada).
 *  - snapshot() lee todo bajo el mismo monitor: nunca se observa un estado intermedio.
 */
public final class CajaVentas {

    public static final int PRECIO_VENTA = 100;

    /** Copia inmutable y consistente del estado de la caja. */
    public record Estado(int ventas, int recaudacion, int[] porVendedor) {
        public Estado {
            porVendedor = porVendedor.clone();
        }

        @Override
        public int[] porVendedor() {
            return porVendedor.clone();
        }

        /** Invariante: recaudación = ventas × C$100 y la suma por vendedor = ventas. */
        public boolean consistente() {
            int suma = 0;
            for (int v : porVendedor) {
                suma += v;
            }
            return recaudacion == ventas * PRECIO_VENTA && suma == ventas;
        }
    }

    private final int[] ventasPorVendedor;
    private final int maxPorVendedor;
    private int ventas;
    private int recaudacion;

    public CajaVentas(int vendedores) {
        this(vendedores, Vendedor.VENTAS_POR_VENDEDOR);
    }

    public CajaVentas(int vendedores, int maxPorVendedor) {
        if (vendedores <= 0) {
            throw new IllegalArgumentException("Debe haber al menos un vendedor");
        }
        if (maxPorVendedor <= 0) {
            throw new IllegalArgumentException("El máximo de ventas por vendedor debe ser positivo");
        }
        this.ventasPorVendedor = new int[vendedores];
        this.maxPorVendedor = maxPorVendedor;
    }

    /** Registra una venta completa (contador + recaudación + avance del vendedor). */
    public synchronized void registrarVenta(int indiceVendedor) {
        if (indiceVendedor < 0 || indiceVendedor >= ventasPorVendedor.length) {
            throw new IllegalArgumentException("Vendedor inexistente: " + indiceVendedor);
        }
        if (ventasPorVendedor[indiceVendedor] >= maxPorVendedor) {
            throw new IllegalStateException("El vendedor " + indiceVendedor
                    + " ya completó sus " + maxPorVendedor + " ventas");
        }
        ventas++;
        recaudacion += PRECIO_VENTA;
        ventasPorVendedor[indiceVendedor]++;
    }

    /** Lectura consistente del estado completo. */
    public synchronized Estado snapshot() {
        return new Estado(ventas, recaudacion, ventasPorVendedor);
    }

    @Override
    public synchronized String toString() {
        return "CajaVentas{ventas=" + ventas + ", recaudacion=" + recaudacion
                + ", porVendedor=" + Arrays.toString(ventasPorVendedor) + "}";
    }
}