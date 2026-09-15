package parchis.modelo;

import java.util.Random;

/**
 *
 * @author Pedro Alonso Rengel
 */

public class Dado {
    private Random random;
    private int ultimoValor;

    public Dado() {
        random = new Random();
        ultimoValor = 1;
    }

    public int tirar() {
        ultimoValor = 1 + random.nextInt(6-1+1);
        return ultimoValor;
    }

    public int getUltimoValor() {
        return ultimoValor;
    }
}
