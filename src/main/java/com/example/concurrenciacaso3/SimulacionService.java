package com.example.concurrenciacaso3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulacionService {
    private final CajaVentas caja;
    private final List<Vendedor> vendedores = new ArrayList<>();
    private ExecutorService executorService;

    public SimulacionService(CajaVentas caja) {
        this.caja = caja;
    }

    // Inicia a los 3 vendedores al mismo tiempo
    public List<Vendedor> iniciarSimulacion(Runnable onVentaCallback) {
        caja.reiniciar();
        vendedores.clear();
        executorService = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 3; i++) {
            Vendedor vendedor = new Vendedor(i, caja, onVentaCallback);
            vendedores.add(vendedor);
            executorService.submit(vendedor); // Ejecuta en segundo plano
        }
        return vendedores;
    }

    // Cancela la tarea de todos los vendedores
    public void cancelarSimulacion() {
        for (Vendedor vendedor : vendedores) {
            vendedor.cancel();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    // Espera a que todos los hilos terminen completamente
    public void esperarTerminacion() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}