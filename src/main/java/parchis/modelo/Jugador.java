package parchis.modelo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pedro Alonso Rengel
 */

public class Jugador {

    private final String NOMBRE;
    private final Color COLOR;
    private final List<Ficha> FICHAS = new ArrayList<>();

    public Jugador(String n, Color c) {
        NOMBRE = n;
        COLOR = c;
        for (int i = 0; i < 4; i++) {
            FICHAS.add(new Ficha(c));
        }
    }

    public String getNOMBRE() {
        return NOMBRE;
    }

    public Color getCOLOR() {
        return COLOR;
    }

    public List<Ficha> getFICHAS() {
        return FICHAS;
    }

    //Utilidades
    public int indiceFicha(Ficha f) {
        return FICHAS.indexOf(f);
    }

    public long fichasEnMeta() {
        return FICHAS.stream().filter(Ficha::haLlegadoMeta).count();
    }

    public boolean fichaEnCasa() {
        return FICHAS.stream().anyMatch(Ficha::estaEnCasa);
    }

    public Ficha primeraFichaEnJuego() {
        return FICHAS.stream()
                .filter(fi -> !fi.estaEnCasa() && !fi.haLlegadoMeta())
                .findFirst().orElse(null);
    }

    public Ficha fichaEnCasilla(int n) {
        return FICHAS.stream()
                .filter(fi -> fi.getNumeroCasilla() == n)
                .findFirst().orElse(null);
    }

}
