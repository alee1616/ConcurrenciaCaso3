package com.example.concurrenciacaso3;

public class CajaVentas {
    private int totalVentas = 0;
    private double recaudacionTotal = 0.0;
    private static final double PRECIO_VENTA = 100.0;

    // Con synchronized aseguramos que solo un vendedor guarde su venta a la vez
    public synchronized boolean registrarVenta() {
        totalVentas++;
        recaudacionTotal += PRECIO_VENTA;
        return true;
    }

    public synchronized int getTotalVentas() {
        return totalVentas;
    }

    public synchronized double getRecaudacionTotal() {
        return recaudacionTotal;
    }

    // Reinicia la caja a 0 para una nueva simulación
    public synchronized void reiniciar() {
        totalVentas = 0;
        recaudacionTotal = 0.0;
    }
}