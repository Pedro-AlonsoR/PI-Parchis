package parchis.modelo;

import java.awt.Color;

/**
 *
 * @author Pedro Alonso Rengel
 */

public class Ficha {

    private final Color COLOR;
    private int numeroCasilla = 0;

    public Ficha(Color c) {
        this.COLOR = c;
    }

    public Color getCOLOR() {
        return COLOR;
    }

    public boolean estaEnCasa() {
        return numeroCasilla == 0;
    }

    public boolean haLlegadoMeta() {
        return numeroCasilla == -1;
    }

    public int getNumeroCasilla() {
        return numeroCasilla;
    }

    public void setNumeroCasilla(int valor) {
        this.numeroCasilla = valor;
    }

    public void llegarMeta() {
        numeroCasilla = -1;
    }

    public void volverACasa() {
        numeroCasilla = 0;
    }

}
