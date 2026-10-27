package com.example.concurrenciacaso3;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * Controlador de caja-view.fxml. Toda la interfaz se toca SOLO en el hilo JavaFX:
 *  - un Timeline lee un snapshot consistente cada 100 ms;
 *  - los cambios de fase llegan desde hilos de fondo y se pasan con Platform.runLater.
 */
public class CajaController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

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
    private Label lblEstadoGeneral;
    @FXML
    private Button btnIniciar;
    @FXML
    private Button btnCancelar;

    @FXML
    private Label lblActivos;
    @FXML
    private Label lblIntegridad;
    @FXML
    private Label lblEjecuciones;
    @FXML
    private TextArea txtLog;

    private ProgressBar[] barras;
    private Label[] estadosVendedor;
    private SimulacionService servicio;
    private Timeline refresco;
    private boolean cerrando;
    private int completasCorrectas;

    @FXML
    public void initialize() {
        barras = new ProgressBar[] {pbVendedor1, pbVendedor2, pbVendedor3};
        estadosVendedor = new Label[] {lblEstadoV1, lblEstadoV2, lblEstadoV3};

        servicio = new SimulacionService(300, 700,
                fase -> Platform.runLater(() -> alCambiarFase(fase)));

        refresco = new Timeline(new KeyFrame(Duration.millis(100), e -> actualizarVista()));
        refresco.setCycleCount(Timeline.INDEFINITE);
        refresco.play();

        mostrarFase(SimulacionService.Fase.LISTA);
        actualizarBotones(SimulacionService.Fase.LISTA);
        actualizarVista();
        log("Aplicación lista. Pulsa Iniciar.");
    }

    @FXML
    private void handleIniciar() {
        if (cerrando) {
            return;
        }
        if (servicio.activa() || !servicio.iniciar()) {
            log("Inicio duplicado rechazado: ya hay una simulación en curso.");
        }
        actualizarBotones(servicio.fase());
    }

    @FXML
    private void handleCancelar() {
        if (!servicio.cancelar()) {
            log("No hay una simulación en ejecución que cancelar.");
        }
        actualizarBotones(servicio.fase());
    }

    /** Cierre de ventana: cancela y espera (sin bloquear) a que terminen los vendedores.
     *
     */
    public void alCerrarVentana(WindowEvent evento) {
        if (servicio.activa()) {
            evento.consume();
            cerrando = true;
            log("Cierre solicitado: esperando a que terminen los vendedores...");
            servicio.cancelar();
            actualizarBotones(servicio.fase());
            if (!servicio.activa()) {
                Platform.exit();
            }
        }
    }

    public void detener() {
        if (refresco != null) {
            refresco.stop();
        }
    }

    // fases

    private void alCambiarFase(SimulacionService.Fase fase) {
        mostrarFase(fase);
        actualizarBotones(fase);
        actualizarVista();

        switch (fase) {
            case EJECUTANDO -> log("Simulación iniciada: 3 vendedores en segundo plano.");
            case CANCELANDO -> log("Cancelación solicitada: cada vendedor termina su venta en curso.");
            case COMPLETADA, CANCELADA -> finalizada(fase);
            default -> { }
        }
    }

    private void finalizada(SimulacionService.Fase fase) {
        CajaVentas.Estado e = servicio.estado();
        log(String.format(Locale.US,
                "%s: %d ventas, %s, trabajadores activos: %d, integridad: %s",
                fase == SimulacionService.Fase.COMPLETADA ? "COMPLETADA" : "CANCELADA",
                e.ventas(), dinero(e.recaudacion()), servicio.trabajadoresActivos(),
                e.consistente() ? "OK" : "ERROR"));

        if (fase == SimulacionService.Fase.COMPLETADA
                && e.ventas() == SimulacionService.VENTAS_ESPERADAS
                && e.recaudacion() == SimulacionService.VENTAS_ESPERADAS * CajaVentas.PRECIO_VENTA) {
            completasCorrectas++;
            lblEjecuciones.setText("Ejecuciones completas correctas: " + completasCorrectas);
        }
        if (cerrando) {
            Platform.exit(); // los 3 vendedores ya terminaron
        }
    }

    private void mostrarFase(SimulacionService.Fase fase) {
        lblEstadoGeneral.setText(switch (fase) {
            case LISTA -> "Lista para iniciar";
            case EJECUTANDO -> "En ejecución";
            case CANCELANDO -> "Cancelando… esperando a los vendedores";
            case COMPLETADA -> "Completada";
            case CANCELADA -> "Cancelada · vendedores terminados";
        });
        lblEstadoGeneral.getStyleClass().removeIf(s -> s.startsWith("estado-"));
        lblEstadoGeneral.getStyleClass().add("estado-" + fase.name().toLowerCase());
    }

    /** "Iniciar" solo se habilita cuando la simulación terminó de verdad. */
    private void actualizarBotones(SimulacionService.Fase fase) {
        boolean activa = fase == SimulacionService.Fase.EJECUTANDO
                || fase == SimulacionService.Fase.CANCELANDO;
        btnIniciar.setDisable(activa || cerrando);
        btnCancelar.setDisable(fase != SimulacionService.Fase.EJECUTANDO || cerrando);
    }

    private void actualizarVista() {
        CajaVentas.Estado e = servicio.estado(); // snapshot consistente
        SimulacionService.Fase fase = servicio.fase();
        int[] por = e.porVendedor();
        for (int i = 0; i < por.length; i++) {
            barras[i].setProgress(por[i] / (double) Vendedor.VENTAS_POR_VENDEDOR);
            estadosVendedor[i].setText(textoVendedor(por[i], fase));
        }
        lblTotalVentas.setText(e.ventas() + " / " + SimulacionService.VENTAS_ESPERADAS);
        lblRecaudacion.setText(dinero(e.recaudacion()));
        lblActivos.setText(String.valueOf(servicio.trabajadoresActivos()));

        boolean ok = e.consistente();
        lblIntegridad.setText(ok
                ? "✔ Integridad: recaudación = ventas × C$100"
                : "✖ Integridad rota: recaudación ≠ ventas × C$100");
        lblIntegridad.getStyleClass().removeAll("integridad-ok", "integridad-error");
        lblIntegridad.getStyleClass().add(ok ? "integridad-ok" : "integridad-error");
    }

    /** Texto de estado de un vendedor: "Venta X de 5", "Completado", "Cancelado". */
    private String textoVendedor(int hechas, SimulacionService.Fase fase) {
        int max = Vendedor.VENTAS_POR_VENDEDOR;
        if (hechas >= max) {
            return "Completado (" + max + " de " + max + ")";
        }
        return switch (fase) {
            case LISTA -> "Sin iniciar";
            case EJECUTANDO -> "Venta " + (hechas + 1) + " de " + max;
            case CANCELANDO -> "Cancelando (" + hechas + " de " + max + ")";
            case COMPLETADA -> "Completado (" + hechas + " de " + max + ")";
            case CANCELADA -> "Cancelado (" + hechas + " de " + max + ")";
        };
    }

    private String dinero(int cordobas) {
        return String.format(Locale.US, "C$ %,.2f", (double) cordobas);
    }

    private void log(String mensaje) {
        txtLog.appendText("[" + LocalTime.now().format(HORA) + "] " + mensaje + "\n");
    }
}