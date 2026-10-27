package com.example.concurrenciacaso3;

import javafx.concurrent.Task;

public class Vendedor extends Task<Void> {
    private final int idVendedor;
    private final CajaVentas caja;
    private final Runnable onVentaCallback;
    private static final int MAX_VENTAS = 5;

    public Vendedor(int idVendedor, CajaVentas caja, Runnable onVentaCallback) {
        this.idVendedor = idVendedor;
        this.caja = caja;
        this.onVentaCallback = onVentaCallback;
    }

    @Override
    protected Void call() throws Exception {
        for (int i = 1; i <= MAX_VENTAS; i++) {
            // Revisión 1: Si presionaron cancelar, detenerse inmediatamente
            if (isCancelled()) {
                updateMessage("Cancelado");
                break;
            }

            // Simula el tiempo que tarda la venta (1.2 segundos)
            Thread.sleep(1200);

            // Revisión 2: Verificar cancelación después de la espera antes de guardar
            if (isCancelled()) {
                updateMessage("Cancelado");
                break;
            }

            // Guardar la venta en la caja segura
            caja.registrarVenta();

            // Avisar a la ventana para que actualice la pantalla
            if (onVentaCallback != null) {
                onVentaCallback.run();
            }

            // Actualizar barra de progreso y texto del vendedor
            updateProgress(i, MAX_VENTAS);
            updateMessage("Venta " + i + " de " + MAX_VENTAS);
        }

        if (!isCancelled()) {
            updateMessage("Completado (5/5)");
        }
        return null;
    }

    public int getIdVendedor() {
        return idVendedor;
    }
}