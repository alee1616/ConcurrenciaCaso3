package com.example.concurrenciacaso3;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.List;

public class CajaController {

    @FXML
    private ProgressBar pbVendedor1;
    @FXML
    private ProgressBar pbVendedor2;
    @FXML
    private ProgressBar pbVendedor3;
    @FXML
    private Label lblEstadoV1;
    @FXML
    private Label lblEstadoV2;
    @FXML
    private Label lblEstadoV3;
    @FXML
    private Label lblTotalVentas;
    @FXML
    private Label lblRecaudacion;
    @FXML
    private Label  lblEstadoGeneral;
    @FXML
    private Button btnIniciar;
    @FXML
    private Button btnCancelar;

    private final CajaVentas caja = new CajaVentas();
    private final SimulacionService simulacionService = new SimulacionService(caja);

    @FXML
    public void initialize() {
        btnCancelar.setDisable(true); // Deshabilitar cancelar al inicio
        actualizarUI();
    }

    @FXML
    private void handleIniciar() {
        btnIniciar.setDisable(true); // Evitar doble clic en Iniciar
        btnCancelar.setDisable(false);
        lblEstadoGeneral.setText("Estado: Simulación en curso...");

        // Arrancar la simulación
        List<Vendedor> vendedores = simulacionService.iniciarSimulacion(() -> Platform.runLater(this::actualizarUI));

        // Enlazar las barras de progreso a cada vendedor
        vincularVendedor(vendedores.get(0), pbVendedor1, lblEstadoV1);
        vincularVendedor(vendedores.get(1), pbVendedor2, lblEstadoV2);
        vincularVendedor(vendedores.get(2), pbVendedor3, lblEstadoV3);

        // Esperar que terminen sin congelar la pantalla de JavaFX
        new Thread(() -> {
            simulacionService.esperarTerminacion();
            Platform.runLater(() -> {
                actualizarUI();
                btnIniciar.setDisable(false);
                btnCancelar.setDisable(true);
                lblEstadoGeneral.setText("Estado: Finalizado / Detenido");
            });
        }).start();
    }

    @FXML
    private void handleCancelar() {
        lblEstadoGeneral.setText("Estado: Cancelando...");
        btnCancelar.setDisable(true);
        simulacionService.cancelarSimulacion();
    }

    // Conecta el progreso del vendedor con la barra de la pantalla
    private void vincularVendedor(Vendedor vendedor, ProgressBar pb, Label lbl) {
        pb.progressProperty().unbind();
        lbl.textProperty().unbind();
        pb.progressProperty().bind(vendedor.progressProperty());
        lbl.textProperty().bind(vendedor.messageProperty());
    }

    // Actualiza los textos con el dinero y las ventas totales
    private void actualizarUI() {
        lblTotalVentas.setText("Ventas Totales: " + caja.getTotalVentas());
        lblRecaudacion.setText(String.format("Recaudación: C$%.2f", caja.getRecaudacionTotal()));
    }

    // Se llama al cerrar la ventana para matar los hilos activos
    public void cerrar() {
        simulacionService.cancelarSimulacion();
    }
}