package com.example.concurrenciacaso3;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CajaVentasApp extends Application {

    private CajaController controlador;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(CajaVentasApp.class.getResource("caja-view.fxml"));
        Scene scene = new Scene(loader.load(), 820, 780);
        scene.getStylesheets().add(CajaVentasApp.class.getResource("caja.css").toExternalForm());

        controlador = loader.getController();
        stage.setOnCloseRequest(controlador::alCerrarVentana);

        stage.setTitle("Caja de ventas concurrente");
        stage.setMinWidth(760);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controlador != null) {
            controlador.detener();
        }
    }
}