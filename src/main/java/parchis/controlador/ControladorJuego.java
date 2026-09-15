package parchis.controlador;

import java.awt.Color;
import java.util.*;
import javax.swing.SwingUtilities;
import parchis.modelo.Dado;
import parchis.modelo.Jugador;
import parchis.vista.TableroVista;

/**
 *
 * @author Pedro Alonso Rengel
 */
public class ControladorJuego {

    //Estados de las fichas
    private enum Estado {
        ESPERANDO_DADO,
        ESPERANDO_FICHA,
        ESPERANDO_FICHA_EXTRA,
        ANIMANDO
    }

    //Contar extra
    private boolean debeContarExtra = false;
    private int pasosExtra = 0;
    private Color colorExtra = null;

    //Casillas del tablero
    private static final List<Integer> RECORRIDO = List.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
            35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68);

    //Casillas seguras
    private static final Set<Integer> CASILLAS_SEGURAS = Set.of(
            5, 12, 17, 22, 29, 34, 39, 46, 51, 56, 63, 68);

    //Casillas de salida
    private static final Map<Color, Integer> SALIDA = Map.of(
            Color.RED, 39,
            Color.BLUE, 22,
            Color.GREEN, 56,
            Color.YELLOW, 5);

    //Pasillo y meta
    private static final int META = -1;

    private static final Map<Color, int[]> PASILLO = Map.of(
            Color.RED, new int[]{101, 102, 103, 104, 105, 106, 107},
            Color.BLUE, new int[]{201, 202, 203, 204, 205, 206, 207},
            Color.YELLOW, new int[]{301, 302, 303, 304, 305, 306, 307},
            Color.GREEN, new int[]{401, 402, 403, 404, 405, 406, 407});

    private static final Map<Color, Integer> ENTRADA_PASILLO = Map.of(
            Color.RED, 34,
            Color.BLUE, 17,
            Color.YELLOW, 68,
            Color.GREEN, 51);

    //Tirar varias veces
    private boolean partidaTerminada = false;

    private int rachaSeis = 0;
    private Color colorRacha = null;
    private int idxRacha = -1;

    //Modelo minimo
    private record Ficha(Color color, int idx, int pos) {

    }

    private Dado dado;
    private final Map<Color, List<Ficha>> FICHAS = new HashMap<>();
    private final List<Jugador> JUGADORES;
    private int turno = 0, valorDado = 0;
    private Estado estado = Estado.ESPERANDO_DADO;

    //Vista tablero
    private final TableroVista vista = new TableroVista();

    public ControladorJuego(List<Jugador> jugadores) {
        this.JUGADORES = jugadores;
        this.dado = new Dado();
        jugadores.forEach(j -> {
            List<Ficha> lst = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                lst.add(new Ficha(j.getCOLOR(), i, -2));
            }
            FICHAS.put(j.getCOLOR(), lst);
        });
        vista.addDadoListener(e -> onTirarDado());
        vista.addFichaListener(this::onFichaClicked);
    }

    public void iniciar() {
        vista.setVisible(true);
        prepararTurno();
    }

    //Lanzar dado
    private void onTirarDado() {
        if (estado != Estado.ESPERANDO_DADO || partidaTerminada) {
            return;
        }

        valorDado = this.dado.tirar();
        estado = Estado.ANIMANDO;

        vista.lanzarDadoAnim(valorDado, () -> {

            //Tres seis seguidos
            if (valorDado == 6 && rachaSeis == 2) {
                enviarFichaARespawn(colorRacha, idxRacha);
                vista.mostrarEstado("¡Tres seises seguidos! La ficha vuelve a casa");
                finDeTurnoYResetCadena();
                return;
            }

            boolean hayMovimiento;

            //Racha de seis
            if (rachaSeis > 0) {
                Ficha fCadena = FICHAS.get(colorRacha).get(idxRacha);
                hayMovimiento = puedeMover(fCadena);
            } else {
                hayMovimiento = existeMovimiento();
            }

            if (hayMovimiento) {
                estado = Estado.ESPERANDO_FICHA;
                vista.mostrarEstado("Selecciona ficha (" + valorDado + ")");
            } else {
                vista.mostrarEstado("Sin movimientos posibles");
                finDeTurnoYResetCadena();
            }
        });
    }

    //Clickear ficha
    private void onFichaClicked(Color c, int idx) {
        if (partidaTerminada) {
            return;
        }

        if (estado == Estado.ESPERANDO_FICHA) {

            if (!c.equals(jugActual().getCOLOR())) {
                return;
            }

            //Solo selecciona una tras racha
            if (rachaSeis > 0 && !(c.equals(colorRacha) && idx == idxRacha)) {
                return;
            }

            Ficha f = FICHAS.get(c).get(idx);
            if (!puedeMover(f)) {
                return;
            }

            if (valorDado == 6 && rachaSeis == 0) {
                colorRacha = c;
                idxRacha = idx;
            }

            ejecutarMovimiento(f);
            return;
        }

        //Contar tras comer/meta
        if (estado == Estado.ESPERANDO_FICHA_EXTRA) {
            if (!c.equals(colorExtra)) {
                return;
            }
            Ficha f = FICHAS.get(c).get(idx);
            if (!puedeAvanzarExtraCon(f)) {
                return;
            }
            ejecutarConteoExtra(f);
        }
    }

    //Reglas movimiento
    private boolean puedeMover(Ficha f) {
        if (f.pos == -2) {
            return valorDado == 5 && !casillaLlena(SALIDA.get(f.color()));
        }
        if (f.pos == META) {
            return false;
        }

        int destino = avanza(f.color(), f.pos, valorDado);

        if (esPistaComun(f.pos) && esPistaComun(destino)
                && rutaCruzadaPorBloqueo(f.pos, destino)) {
            return false;
        }
        return !casillaLlena(destino);
    }

    private boolean existeMovimiento() {
        return FICHAS.get(jugActual().getCOLOR()).stream().anyMatch(this::puedeMover);
    }

    //Ejecutar movimiento
    private void ejecutarMovimiento(Ficha f) {
        int origen = f.pos;
        int pasos = (origen == -2) ? 1 : valorDado;
        int destino = (origen == -2) ? SALIDA.get(f.color())
                : avanza(f.color(), origen, pasos);

        if (origen > 0 && esPistaComun(origen) && esPistaComun(destino) && rutaCruzadaPorBloqueo(origen, destino)) {
            vista.mostrarEstado("Movimiento bloqueado: hay bloqueo en el camino");
            return;
        }
        if (casillaLlena(destino)) {
            vista.mostrarEstado("Hay dos fichas en la casilla " + destino + " – no puedes entrar");
            return;
        }

        estado = Estado.ANIMANDO;
        int[] camino = calcularCamino(f.color(), origen, pasos);
        int idxFicha = f.idx;
        boolean desdeSpawn = (origen == -2);

        vista.moverFichaAnim(f.color(), idxFicha, camino, () -> {
            FICHAS.get(f.color()).set(idxFicha, new Ficha(f.color(), idxFicha, destino));

            //Puede comer
            if (esPistaComun(destino)) {
                procesarComer(destino, f.color(), desdeSpawn);
            }

            //Contar 10
            if (destino == META) {
                procesarMeta(f.color());
            }

            comprobarFinTurno(f.color());
        });
    }

    private void procesarMeta(Color colorMover) {
        if (haGanado(colorMover)) {
            vista.mostrarEstado(jugActual().getNOMBRE() + " ¡ha ganado la partida!");
            vista.deshabilitarDado(true);
            return;
        }
        if (hayFichaQuePuedeExtra(colorMover, 10)) {
            debeContarExtra = true;
            pasosExtra = 10;
            colorExtra = colorMover;
            estado = Estado.ESPERANDO_FICHA_EXTRA;
            vista.mostrarEstado("¡Entras en meta! Elige ficha para avanzar 10");
        }
    }

    //Gestion fin turno
    private void comprobarFinTurno(Color colorMover) {

        //Comprueba si la partida termina
        if (!partidaTerminada && haGanado(colorMover)) {
            partidaTerminada = true;

            String nombre = getNombreJugador(colorMover);
            vista.mostrarVictoria(nombre, colorMover);   // ← ahora con color
            vista.deshabilitarDado(true);                // sigue bloqueando el dado
            partidaTerminada = true;
            return;
        }

        if (debeContarExtra) {
            return;
        }

        if (valorDado == 6) {
            rachaSeis++;

            if (rachaSeis == 3) {
                enviarFichaARespawn(colorRacha, idxRacha);
                vista.mostrarEstado("¡Tres seises seguidos! La ficha vuelve a casa");
                finDeTurnoYResetCadena();
            } else {
                estado = Estado.ESPERANDO_DADO;
                vista.mostrarEstado("Has sacado 6 (" + rachaSeis + "/3) – vuelve a tirar");
            }
        } else {
            finDeTurnoYResetCadena();
        }
    }

    //Recorrido fichas
    private boolean esPistaComun(int cas) {
        return cas > 0 && cas < 100;
    }

    private int siguiente(Color color, int pos) {
        if (pos == -2) {
            return SALIDA.get(color);
        }
        if (pos == META) {
            return META;
        }
        if (esPistaComun(pos)) {
            int entrada = ENTRADA_PASILLO.get(color);
            if (pos == entrada) {
                return PASILLO.get(color)[0];
            }
            int idx = RECORRIDO.indexOf(pos);
            return RECORRIDO.get((idx + 1) % RECORRIDO.size());
        }

        //Entrar en pasillo
        int[] pas = PASILLO.get(color);
        for (int i = 0; i < pas.length; i++) {
            if (pos == pas[i]) {
                return (i == pas.length - 1) ? META : pas[i + 1];
            }
        }
        return pos;
    }

    private int anterior(Color color, int pos) {
        if (pos == META) {
            return PASILLO.get(color)[PASILLO.get(color).length - 1];
        }
        int[] pas = PASILLO.get(color);
        for (int i = 0; i < pas.length; i++) {
            if (pos == pas[i]) {
                return (i == 0) ? pas[0] : pas[i - 1];
            }
        }
        return pos;
    }

    private int avanza(Color color, int origen, int pasos) {
        int p = origen;
        boolean forward = true;
        for (int i = 0; i < pasos; i++) {
            if (forward) {
                p = siguiente(color, p);
                if (p == META) {
                    forward = false;
                }
            } else {
                p = anterior(color, p);
                if (p == PASILLO.get(color)[0]) {
                    forward = true;
                }
            }
        }
        return p;
    }

    private int[] calcularCamino(Color color, int origen, int pasos) {
        int[] arr = new int[pasos];
        int p = origen;
        boolean forward = true;
        for (int i = 0; i < pasos; i++) {
            if (forward) {
                p = siguiente(color, p);
                if (p == META) {
                    forward = false;
                }
            } else {
                p = anterior(color, p);
            }
            arr[i] = p;
        }
        return arr;
    }

    //Turnos
    private void prepararTurno() {
        Jugador j = jugActual();
        vista.mostrarEstado("Tira el dado " + j.getNOMBRE());
        vista.setColorBotonPastel(j.getCOLOR());   //  ← NUEVA LÍNEA
        vista.deshabilitarDado(false);
    }

    private void pasarTurno() {
        turno = (turno + 1) % JUGADORES.size();
        prepararTurno();
    }

    private Jugador jugActual() {
        return JUGADORES.get(turno);
    }

    //Bloqueos
    private List<Ficha> fichasEn(int casilla) {
        return FICHAS.values().stream().flatMap(List::stream)
                .filter(f -> f.pos == casilla).toList();
    }

    private boolean hayBloqueoEn(int cas) {
        List<Ficha> lst = fichasEn(cas);
        return lst.size() == 2 && lst.get(0).color() == lst.get(1).color();
    }

    private boolean rutaCruzadaPorBloqueo(int from, int to) {
        int p = from;
        for (int i = 0; i < 68; i++) {
            p = (p % 68) + 1;
            if (hayBloqueoEn(p)) {
                return true;
            }
            if (p == to) {
                break;
            }
        }
        return false;
    }

    //Comprueba la meta
    private boolean casillaLlena(int casilla) {
        if (esMeta(casilla)) {
            return false;
        }
        return fichasEn(casilla).size() >= 2;
    }

    //Logica comer
    private void procesarComer(int destino, Color colorMover, boolean desdeSpawn) {
        List<Ficha> oc = fichasEn(destino);
        if (oc.size() <= 1) {
            return;
        }
        if (hayBloqueoEn(destino) || esSeguraGlobal(destino)) {
            return;
        }
        Optional<Ficha> victimaOpt = oc.stream().filter(f -> !f.color().equals(colorMover)).findFirst();
        if (victimaOpt.isEmpty()) {
            return;
        }
        Ficha victima = victimaOpt.get();
        if (esSuSalida(destino, victima.color())) {
            return;
        }

        vista.mostrarEstado(getNombreJugador(colorMover) + " come a " + getNombreJugador(victima.color()));
        FICHAS.get(victima.color()).set(victima.idx(), new Ficha(victima.color(), victima.idx(), -2));
        vista.moverFicha(victima.color(), victima.idx(), 0);

        boolean obligacion = !desdeSpawn && !SALIDA.containsValue(destino);
        if (obligacion && hayFichaQuePuedeExtra(colorMover, 20)) {
            debeContarExtra = true;
            pasosExtra = 20;
            colorExtra = colorMover;
            estado = Estado.ESPERANDO_FICHA_EXTRA;
            vista.mostrarEstado("¡Has comido! Elige ficha para avanzar 20");
        }
    }

    //Contar extra tras comer/meta
    private boolean puedeAvanzarExtraCon(Ficha f) {
        if (f.pos <= 0) {
            return false;
        }

        int destino = avanza(f.color(), f.pos, pasosExtra);

        if (!esMeta(destino) && casillaLlena(destino)) {
            return false;
        }

        if (esPistaComun(f.pos) && esPistaComun(destino)
                && rutaCruzadaPorBloqueo(f.pos, destino)) {
            return false;
        }

        return true;
    }

    private boolean hayFichaQuePuedeExtra(Color c, int pasos) {
        return FICHAS.get(c).stream().anyMatch(f -> {
            if (f.pos <= 0) {
                return false;
            }
            int d = avanza(c, f.pos, pasos);
            if (casillaLlena(d)) {
                return false;
            }
            if (esPistaComun(f.pos) && esPistaComun(d) && rutaCruzadaPorBloqueo(f.pos, d)) {
                return false;
            }
            return true;
        });
    }

    //Controlador
    private void ejecutarConteoExtra(Ficha f) {
        int pasos = pasosExtra;

        debeContarExtra = false;
        pasosExtra = 0;
        colorExtra = null;

        estado = Estado.ANIMANDO;

        int destino = avanza(f.color(), f.pos, pasos);
        int[] camino = calcularCamino(f.color(), f.pos, pasos);

        vista.moverFichaAnim(f.color(), f.idx, camino, () -> {

            FICHAS.get(f.color()).set(f.idx,
                    new Ficha(f.color(), f.idx, destino));

            if (esPistaComun(destino)) {
                procesarComer(destino, f.color(), false);
            }

            if (destino == META) {
                procesarMeta(f.color());
            }

            comprobarFinTurno(f.color());
        });
    }

    //Utilidades
    private boolean esSuSalida(int cas, Color col) {
        return SALIDA.get(col) == cas;
    }

    private boolean esSeguraGlobal(int cas) {
        return CASILLAS_SEGURAS.contains(cas);
    }

    private String getNombreJugador(Color c) {
        return JUGADORES.stream().filter(j -> j.getCOLOR().equals(c)).findFirst().map(Jugador::getNOMBRE).orElse("");
    }

    private boolean haGanado(Color c) {
        return FICHAS.get(c).stream().allMatch(f -> f.pos == META);
    }

    //Meta
    private boolean esMeta(int pos) {
        return pos == -1;
    }

    private void finDeTurnoYResetCadena() {
        rachaSeis = 0;
        colorRacha = null;
        idxRacha = -1;
        estado = Estado.ESPERANDO_DADO;
        pasarTurno();
    }

    private void enviarFichaARespawn(Color col, int idx) {
        FICHAS.get(col).set(idx, new Ficha(col, idx, -2));
        vista.moverFicha(col, idx, 0);
    }
}
