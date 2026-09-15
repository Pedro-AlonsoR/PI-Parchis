package parchis;

import parchis.vista.SeleccionJugadores;

/**
 *
 * @author Pedro Alonso Rengel
 */

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new SeleccionJugadores().setVisible(true));
    }
}