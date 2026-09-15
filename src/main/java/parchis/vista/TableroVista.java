package parchis.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author Pedro Alonso Rengel
 */
public class TableroVista extends javax.swing.JFrame {

    //Ficha callback
    public interface FichaClickListener {

        void onFichaClicked(Color color, int idx);
    }
    private final List<FichaClickListener> FICHAS_LISTENERS = new ArrayList<>();

    public void addFichaListener(FichaClickListener l) {
        FICHAS_LISTENERS.add(l);
    }

    //Dado
    private final JLabel LBL_DADO = new JLabel();
    private final Icon[] CARAS = cargarDados();
    private Timer timerDado;

    //Fichas y casillas
    private final Map<Integer, JPanel> CASILLAS = new HashMap<>();
    private final Map<Color, List<VistaFicha>> FICHAS = new HashMap<>();
    private Map<Color, JPanel> spawns = new HashMap<>();

    //Meta
    private final JPanel panelMeta;
    private static final int META_KEY = -1;

    private VistaFicha fichaSel;
    private Timer timerFicha;

    public TableroVista() {
        initComponents();

        Dimension metaSize = meta.getPreferredSize();
        meta.setMinimumSize(metaSize);
        meta.setPreferredSize(metaSize);
        meta.setMaximumSize(metaSize);
        meta.setLayout(new BorderLayout());

        JPanel metaGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)) {
            @Override
            public Dimension getPreferredSize() {
                return metaSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return metaSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return metaSize;
            }
        };
        metaGrid.setOpaque(false);
        meta.add(metaGrid, BorderLayout.CENTER);
        panelMeta = metaGrid;

// F1  ··  AYUDA con enlaces clicables
        AbstractAction ayudaAction = new AbstractAction("Ayuda") {
            @Override
            public void actionPerformed(ActionEvent e) {

                String html = """
        <html><body style='font-family:sans-serif;font-size:12pt'>
          <h2 style='text-align:center;margin:0 0 6px 0;'>Reglas básicas</h2>
          <ol style='margin:0 0 0 16px;padding:0;'>
            <li><b>Salida (5):</b> Sólo puedes sacar una ficha de la casa con un <strong>5</strong>.</li>
            <li><b>Tirada de 6:</b> Con un <strong>6</strong> repites turno; tres 6 seguidos envían la ficha a casa.</li>
            <li><b>Mismo turno:</b> Durante una racha de 6 mueves siempre <u>la misma ficha</u>.</li>
            <li><b>Casilla segura:</b> Las fichas en casillas de color no pueden ser comidas.</li>
            <li><b>Ocupación máx:</b> En una casilla sólo caben <strong>2 fichas</strong>.</li>
            <li><b>Bloqueos:</b> Dos fichas del mismo color forman bloqueo; nadie puede pasar.</li>
            <li><b>Comer:</b> Si comes, avanzas <strong>20</strong> casillas y la víctima vuelve a casa.</li>
            <li><b>Entrar en meta:</b> Al llevar una ficha a meta otra avanza <strong>10</strong>.</li>
            <li><b>Meta exacta:</b> Para entrar necesitas el número exacto; si te pasas retrocedes.</li>
          </ol>
          <p style='text-align:center;margin:10px 0 2px 0;'><i>¡Disfruta de la partida!</i></p>
          <hr style='border:none;border-top:1px solid #ccc;margin:8px 0 4px 0;'>
          <small>
            CC:&nbsp;
            <a href='https://creativecommons.org'>Parchís</a> © 2025&nbsp;by&nbsp;
            <a href='https://creativecommons.org'>Pedro&nbsp;Alonso&nbsp;Rengel</a>
            — Licencia&nbsp;<a href='https://creativecommons.org/licenses/by/4.0/'>CC BY 4.0</a>
          </small>
        </body></html>
        """;

                // --- JEditorPane con soporte de enlaces -----------------
                JEditorPane pane = new JEditorPane("text/html", html);
                pane.setEditable(false);
                pane.setBorder(null);
                pane.addHyperlinkListener(ev -> {
                    if (ev.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                        try {      // abre el navegador por defecto
                            java.awt.Desktop.getDesktop().browse(ev.getURL().toURI());
                        } catch (Exception ex) {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                        }
                    }
                });

                JScrollPane scroller = new JScrollPane(pane);
                scroller.setPreferredSize(new Dimension(410, 340));

                JOptionPane.showMessageDialog(
                        TableroVista.this,
                        scroller,
                        "Ayuda",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        JButton btnAyuda = new JButton(ayudaAction);
        btnAyuda.setFocusable(false);
        btnAyuda.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btnAyuda.setBackground(new Color(230, 230, 230));
        btnAyuda.setBorderPainted(false);
        jPanel7.add(Box.createHorizontalStrut(16));
        jPanel7.add(btnAyuda);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F1"), "ayuda");
        rp.getActionMap().put("ayuda", ayudaAction);

        Container tablero = getContentPane();
        JPanel root = new JPanel(new BorderLayout());
        root.add(tablero, BorderLayout.CENTER);

        JPanel dadoCol = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        dadoCol.setOpaque(false);

        LBL_DADO.setHorizontalAlignment(SwingConstants.CENTER);
        LBL_DADO.setIcon(CARAS[0]);
        dadoCol.add(LBL_DADO);

        final int ANCHO_DADO = 250;
        dadoCol.setPreferredSize(new Dimension(ANCHO_DADO, 10));
        root.add(dadoCol, BorderLayout.EAST);

        setContentPane(root);
        pack();
        redimensionarDado();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                dadoCol.setPreferredSize(new Dimension(ANCHO_DADO, getHeight()));
                dadoCol.revalidate();
                redimensionarDado();
            }
        });

        estilizarBoton(jButton1, new Color(230, 230, 230));

        registrarSpawns();
        registrarCasillas();
        inicializarFichas();
    }

    //Visualización de las fichas
    private class VistaFicha {

        final JLabel lbl;
        final Color color;

        VistaFicha(Color c, int idx) {
            color = c;
            lbl = new JLabel(crearIcono(c));
            lbl.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    resaltar(c, idx);
                    FICHAS_LISTENERS.forEach(f -> f.onFichaClicked(c, idx));
                }
            });
        }
    }

    //Crear iconos
    private static Icon crearIcono(Color c) {
        String path = switch (c.getRGB()) {
            case 0xFFFF0000 ->
                "/imagenes/fRo.png";
            case 0xFF0000FF ->
                "/imagenes/fAz.png";
            case 0xFF00FF00 ->
                "/imagenes/fVe.png";
            default ->
                "/imagenes/fAm.png";
        };
        ImageIcon raw = new ImageIcon(TableroVista.class.getResource(path));

        java.awt.Image mini = raw.getImage().getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(mini);
    }

    //Seleccionar ficha (visual)
    private VistaFicha fichaSeleccionada;

    private void seleccionarFicha(Color c, int idx) {
        if (fichaSeleccionada != null) {
            fichaSeleccionada.lbl.setBorder(null);
        }
        fichaSeleccionada = FICHAS.get(c).get(idx);
        fichaSeleccionada.lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    //Registrar spawns
    private void registrarSpawns() {
        spawns.put(Color.RED, SpawnRojo);
        spawns.put(Color.BLUE, SpawnAzul);
        spawns.put(Color.GREEN, SpawnVerde);
        spawns.put(Color.YELLOW, SpawnAmarillo);
        spawns.values().forEach(p -> p.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2)));
    }

    //Registrar casillas
    private void registrarCasillas() {
        CASILLAS.put(1, Casilla1);
        CASILLAS.put(2, Casilla2);
        CASILLAS.put(3, Casilla3);
        CASILLAS.put(4, Casilla4);
        CASILLAS.put(5, Casilla5);
        CASILLAS.put(6, Casilla6);
        CASILLAS.put(7, Casilla7);
        CASILLAS.put(8, Casilla8);
        CASILLAS.put(9, Casilla9);
        CASILLAS.put(10, Casilla10);
        CASILLAS.put(11, Casilla11);
        CASILLAS.put(12, Casilla12);
        CASILLAS.put(13, Casilla13);
        CASILLAS.put(14, Casilla14);
        CASILLAS.put(15, Casilla15);
        CASILLAS.put(16, Casilla16);
        CASILLAS.put(17, Casilla17);
        CASILLAS.put(18, Casilla18);
        CASILLAS.put(19, Casilla19);
        CASILLAS.put(20, Casilla20);
        CASILLAS.put(21, Casilla21);
        CASILLAS.put(22, Casilla22);
        CASILLAS.put(23, Casilla23);
        CASILLAS.put(24, Casilla24);
        CASILLAS.put(25, Casilla25);
        CASILLAS.put(26, Casilla26);
        CASILLAS.put(27, Casilla27);
        CASILLAS.put(28, Casilla28);
        CASILLAS.put(29, Casilla29);
        CASILLAS.put(30, Casilla30);
        CASILLAS.put(31, Casilla31);
        CASILLAS.put(32, Casilla32);
        CASILLAS.put(33, Casilla33);
        CASILLAS.put(34, Casilla34);
        CASILLAS.put(35, Casilla35);
        CASILLAS.put(36, Casilla36);
        CASILLAS.put(37, Casilla37);
        CASILLAS.put(38, Casilla38);
        CASILLAS.put(39, Casilla39);
        CASILLAS.put(40, Casilla40);
        CASILLAS.put(41, Casilla41);
        CASILLAS.put(42, Casilla42);
        CASILLAS.put(43, Casilla43);
        CASILLAS.put(44, Casilla44);
        CASILLAS.put(45, Casilla45);
        CASILLAS.put(46, Casilla46);
        CASILLAS.put(47, Casilla47);
        CASILLAS.put(48, Casilla48);
        CASILLAS.put(49, Casilla49);
        CASILLAS.put(50, Casilla50);
        CASILLAS.put(51, Casilla51);
        CASILLAS.put(52, Casilla52);
        CASILLAS.put(53, Casilla53);
        CASILLAS.put(54, Casilla54);
        CASILLAS.put(55, Casilla55);
        CASILLAS.put(56, Casilla56);
        CASILLAS.put(57, Casilla57);
        CASILLAS.put(58, Casilla58);
        CASILLAS.put(59, Casilla59);
        CASILLAS.put(60, Casilla60);
        CASILLAS.put(61, Casilla61);
        CASILLAS.put(62, Casilla62);
        CASILLAS.put(63, Casilla63);
        CASILLAS.put(64, Casilla64);
        CASILLAS.put(65, Casilla65);
        CASILLAS.put(66, Casilla66);
        CASILLAS.put(67, Casilla67);
        CASILLAS.put(68, Casilla68);
        //pasillos
        // Pasillo rojo (R)
        CASILLAS.put(101, pasilloR1);
        CASILLAS.put(102, pasilloR2);
        CASILLAS.put(103, pasilloR3);
        CASILLAS.put(104, pasilloR4);
        CASILLAS.put(105, pasilloR5);
        CASILLAS.put(106, pasilloR6);
        CASILLAS.put(107, pasilloR7);

        // Pasillo azul (A)
        CASILLAS.put(201, pasilloA1);
        CASILLAS.put(202, pasilloA2);
        CASILLAS.put(203, pasilloA3);
        CASILLAS.put(204, pasilloA4);
        CASILLAS.put(205, pasilloA5);
        CASILLAS.put(206, pasilloA6);
        CASILLAS.put(207, pasilloA7);

        // Pasillo amarillo (Am)
        CASILLAS.put(301, pasilloAm1);
        CASILLAS.put(302, pasilloAm2);
        CASILLAS.put(303, pasilloAm3);
        CASILLAS.put(304, pasilloAm4);
        CASILLAS.put(305, pasilloAm5);
        CASILLAS.put(306, pasilloAm6);
        CASILLAS.put(307, pasilloAm7);

        // Pasillo verde (V)
        CASILLAS.put(401, pasilloV1);
        CASILLAS.put(402, pasilloV2);
        CASILLAS.put(403, pasilloV3);
        CASILLAS.put(404, pasilloV4);
        CASILLAS.put(405, pasilloV5);
        CASILLAS.put(406, pasilloV6);
        CASILLAS.put(407, pasilloV7);

        // Casilla central de meta
        CASILLAS.put(META_KEY, panelMeta);

    }

    private void crearMapaSpawns() {
        spawns = Map.of(
                Color.RED, SpawnRojo,
                Color.BLUE, SpawnAzul,
                Color.GREEN, SpawnVerde,
                Color.YELLOW, SpawnAmarillo
        );
    }

    //Crear fichas
    private void inicializarFichas() {
        crearGrupo(Color.RED, SpawnRojo);
        crearGrupo(Color.BLUE, SpawnAzul);
        crearGrupo(Color.GREEN, SpawnVerde);
        crearGrupo(Color.YELLOW, SpawnAmarillo);
    }

    private void crearGrupo(Color c, JPanel spawn) {
        List<VistaFicha> lst = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            VistaFicha vf = new VistaFicha(c, i);
            spawn.add(vf.lbl);
            lst.add(vf);
        }
        FICHAS.put(c, lst);
    }

    private void resaltar(Color c, int idx) {
        if (fichaSel != null) {
            fichaSel.lbl.setBorder(null);
        }
        fichaSel = FICHAS.get(c).get(idx);
        fichaSel.lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    //Logica mover
    public void moverFicha(Color color, int idxFicha, int numCasilla) {
        VistaFicha vf = FICHAS.get(color).get(idxFicha);

        JPanel destino = switch (numCasilla) {
            case 0 ->
                spawns.get(color);
            case META_KEY ->
                panelMeta;
            default ->
                CASILLAS.get(numCasilla);
        };
        if (destino == null) {
            System.err.println("Casilla desconocida " + numCasilla);
            return;
        }

        if (!(destino.getLayout() instanceof FlowLayout)) {
            destino.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        }
        destino.add(vf.lbl);
        destino.revalidate();
        destino.repaint();
    }

    //Animación de mover ficha
    public void moverFichaAnim(Color c, int idxFicha, int[] camino, Runnable fin) {
        if (camino.length == 0) {
            fin.run();
            return;
        }

        timerFicha = new Timer(50, null);
        timerFicha.addActionListener(new ActionListener() {
            int paso = 0;

            public void actionPerformed(ActionEvent e) {
                moverFicha(c, idxFicha, camino[paso]);
                paso++;

                repaint();
                if (paso >= camino.length) {
                    timerFicha.stop();
                    SwingUtilities.invokeLater(fin);
                }
            }
        });
        timerFicha.start();
    }

    //Animación dado
    public void lanzarDadoAnim(int res, Runnable cb) {
        deshabilitarDado(true);

        final int[] t = {0};
        timerDado = new Timer(40, null);
        timerDado.addActionListener(e -> {
            int cara = (t[0] % 6) + 1;
            actualizarDadoIcono(cara);
            if (++t[0] > 24) {
                timerDado.stop();
                actualizarDadoIcono(res);
                SwingUtilities.invokeLater(cb);
                deshabilitarDado(false);
            }
        });
        timerDado.start();
    }

    public void setColorBotonPastel(Color base) {
        Color pastel = new Color(
                (base.getRed() + 255) / 2,
                (base.getGreen() + 255) / 2,
                (base.getBlue() + 255) / 2);

        jButton1.setBackground(pastel);
        jButton1.setOpaque(true);
        jButton1.setBorderPainted(false);
        jButton1.setForeground(new Color(40, 40, 40));
    }

    private void actualizarDadoIcono(int valor) {
        ImageIcon iconoOriginal = (ImageIcon) CARAS[valor - 1];
        int size = Math.min(LBL_DADO.getWidth(), LBL_DADO.getHeight());
        if (size <= 0) {
            size = 64;
        }
        Image img = iconoOriginal.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        LBL_DADO.setIcon(new ImageIcon(img));
    }

    private void redimensionarDado() {
        LBL_DADO.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                actualizarDadoIcono(1);
            }
        });
    }

    public JButton getBotonDado() {
        return jButton1;
    }

    public void addDadoListener(ActionListener al) {
        jButton1.addActionListener(al);
    }

    public void deshabilitarDado(boolean b) {
        jButton1.setEnabled(!b);
    }

    public void setMensaje(String t) {
        jButton1.setText(t);
    }

    public void mostrarEstado(String t) {
        setMensaje(t);
    }

    //Cargar imagenes dado
    private Icon[] cargarDados() {
        Icon[] v = new Icon[6];
        for (int i = 0; i < 6; i++) {
            URL u = getClass().getResource("/imagenes/d" + (i + 1) + ".gif");
            v[i] = new ImageIcon(u);
        }
        return v;
    }

    //Diseño boton
    private void estilizarBoton(JButton b, Color pastel) {
        b.setBackground(pastel);
        b.setOpaque(true);
        b.setForeground(Color.DARK_GRAY);

        b.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
    }

// ───────── Ventana de victoria ─────────
    public void mostrarVictoria(String nombreGanador, Color colorGanador) {

        Color pastel = pintarVictoria(colorGanador, 0.40f);

        /* Contenido principal */
        JLabel label = new JLabel(
                "<html><div style='text-align:center;'>"
                + "<h1 style='margin:0'>" + nombreGanador + "</h1>"
                + "<h2 style='margin:4px 0 0 0'>¡ha ganado la partida!</h2>"
                + "</div></html>",
                SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 18f));
        label.setOpaque(false);          // fondo lo pone el panel raíz
        label.setForeground(Color.DARK_GRAY);

        /* Botón para iniciar nueva partida */
        JButton nuevo = new JButton("Nueva partida");
        nuevo.setFocusPainted(false);
        nuevo.addActionListener(ev -> {
            Window win = SwingUtilities.getWindowAncestor((Component) ev.getSource());
            win.dispose();               // cierra el diálogo
            TableroVista.this.dispose(); // cierra el tablero
            new parchis.vista.SeleccionJugadores().setVisible(true);
        });

        /* Construcción del diálogo modal */
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(pastel);
        content.setBorder(BorderFactory.createEmptyBorder(25, 35, 20, 35));
        content.add(label, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        south.setOpaque(false);
        south.add(nuevo);
        content.add(south, BorderLayout.SOUTH);

        JDialog dlg = new JDialog(this, "¡Victoria!", true);
        dlg.setContentPane(content);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setVisible(true);
    }

    private static Color pintarVictoria(Color c, float factor) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        r += (int) ((255 - r) * factor);
        g += (int) ((255 - g) * factor);
        b += (int) ((255 - b) * factor);
        return new Color(r, g, b);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        SpawnRojo = new javax.swing.JPanel();
        SpawnVerde = new javax.swing.JPanel();
        SpawnAmarillo = new javax.swing.JPanel();
        SpawnAzul = new javax.swing.JPanel();
        Casilla35 = new javax.swing.JPanel();
        Casilla36 = new javax.swing.JPanel();
        Casilla37 = new javax.swing.JPanel();
        Casilla38 = new javax.swing.JPanel();
        Casilla39 = new javax.swing.JPanel();
        Casilla40 = new javax.swing.JPanel();
        Casilla41 = new javax.swing.JPanel();
        Casilla42 = new javax.swing.JPanel();
        Casilla43 = new javax.swing.JPanel();
        Casilla44 = new javax.swing.JPanel();
        Casilla45 = new javax.swing.JPanel();
        Casilla46 = new javax.swing.JPanel();
        Casilla47 = new javax.swing.JPanel();
        Casilla48 = new javax.swing.JPanel();
        Casilla49 = new javax.swing.JPanel();
        Casilla50 = new javax.swing.JPanel();
        Casilla51 = new javax.swing.JPanel();
        Casilla52 = new javax.swing.JPanel();
        Casilla53 = new javax.swing.JPanel();
        Casilla54 = new javax.swing.JPanel();
        Casilla55 = new javax.swing.JPanel();
        Casilla56 = new javax.swing.JPanel();
        Casilla57 = new javax.swing.JPanel();
        Casilla58 = new javax.swing.JPanel();
        Casilla59 = new javax.swing.JPanel();
        Casilla63 = new javax.swing.JPanel();
        Casilla61 = new javax.swing.JPanel();
        Casilla60 = new javax.swing.JPanel();
        Casilla62 = new javax.swing.JPanel();
        Casilla64 = new javax.swing.JPanel();
        Casilla65 = new javax.swing.JPanel();
        Casilla66 = new javax.swing.JPanel();
        Casilla67 = new javax.swing.JPanel();
        Casilla68 = new javax.swing.JPanel();
        Casilla1 = new javax.swing.JPanel();
        Casilla2 = new javax.swing.JPanel();
        Casilla3 = new javax.swing.JPanel();
        Casilla4 = new javax.swing.JPanel();
        Casilla5 = new javax.swing.JPanel();
        Casilla6 = new javax.swing.JPanel();
        Casilla7 = new javax.swing.JPanel();
        Casilla8 = new javax.swing.JPanel();
        Casilla9 = new javax.swing.JPanel();
        Casilla10 = new javax.swing.JPanel();
        Casilla11 = new javax.swing.JPanel();
        Casilla12 = new javax.swing.JPanel();
        Casilla13 = new javax.swing.JPanel();
        Casilla14 = new javax.swing.JPanel();
        Casilla15 = new javax.swing.JPanel();
        Casilla16 = new javax.swing.JPanel();
        Casilla17 = new javax.swing.JPanel();
        Casilla18 = new javax.swing.JPanel();
        Casilla19 = new javax.swing.JPanel();
        Casilla20 = new javax.swing.JPanel();
        Casilla21 = new javax.swing.JPanel();
        Casilla22 = new javax.swing.JPanel();
        Casilla23 = new javax.swing.JPanel();
        Casilla24 = new javax.swing.JPanel();
        Casilla25 = new javax.swing.JPanel();
        Casilla26 = new javax.swing.JPanel();
        Casilla27 = new javax.swing.JPanel();
        Casilla28 = new javax.swing.JPanel();
        Casilla29 = new javax.swing.JPanel();
        Casilla30 = new javax.swing.JPanel();
        Casilla31 = new javax.swing.JPanel();
        Casilla32 = new javax.swing.JPanel();
        Casilla34 = new javax.swing.JPanel();
        Casilla33 = new javax.swing.JPanel();
        pasilloV1 = new javax.swing.JPanel();
        pasilloV2 = new javax.swing.JPanel();
        pasilloV3 = new javax.swing.JPanel();
        pasilloV4 = new javax.swing.JPanel();
        pasilloV5 = new javax.swing.JPanel();
        pasilloV6 = new javax.swing.JPanel();
        pasilloV7 = new javax.swing.JPanel();
        pasilloR1 = new javax.swing.JPanel();
        pasilloR2 = new javax.swing.JPanel();
        pasilloR3 = new javax.swing.JPanel();
        pasilloR4 = new javax.swing.JPanel();
        pasilloR5 = new javax.swing.JPanel();
        pasilloR6 = new javax.swing.JPanel();
        pasilloR7 = new javax.swing.JPanel();
        pasilloA1 = new javax.swing.JPanel();
        pasilloA2 = new javax.swing.JPanel();
        pasilloA3 = new javax.swing.JPanel();
        pasilloA4 = new javax.swing.JPanel();
        pasilloA5 = new javax.swing.JPanel();
        pasilloA6 = new javax.swing.JPanel();
        pasilloA7 = new javax.swing.JPanel();
        pasilloAm1 = new javax.swing.JPanel();
        pasilloAm2 = new javax.swing.JPanel();
        pasilloAm3 = new javax.swing.JPanel();
        pasilloAm4 = new javax.swing.JPanel();
        pasilloAm5 = new javax.swing.JPanel();
        pasilloAm6 = new javax.swing.JPanel();
        pasilloAm7 = new javax.swing.JPanel();
        meta = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        SpawnRojo.setBackground(new java.awt.Color(255, 0, 0));
        SpawnRojo.setPreferredSize(new java.awt.Dimension(150, 150));
        SpawnRojo.setLayout(null);

        SpawnVerde.setBackground(new java.awt.Color(0, 204, 0));
        SpawnVerde.setPreferredSize(new java.awt.Dimension(150, 150));
        SpawnVerde.setLayout(null);

        SpawnAmarillo.setBackground(new java.awt.Color(255, 255, 51));
        SpawnAmarillo.setPreferredSize(new java.awt.Dimension(150, 150));
        SpawnAmarillo.setLayout(null);

        SpawnAzul.setBackground(new java.awt.Color(51, 51, 255));
        SpawnAzul.setPreferredSize(new java.awt.Dimension(150, 150));
        SpawnAzul.setLayout(null);

        Casilla35.setBackground(new java.awt.Color(255, 255, 255));
        Casilla35.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla35.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla35.setLayout(null);

        Casilla36.setBackground(new java.awt.Color(255, 255, 255));
        Casilla36.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla36.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla36.setLayout(null);

        Casilla37.setBackground(new java.awt.Color(255, 255, 255));
        Casilla37.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla37.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla37.setLayout(null);

        Casilla38.setBackground(new java.awt.Color(255, 255, 255));
        Casilla38.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla38.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla38.setLayout(null);

        Casilla39.setBackground(new java.awt.Color(255, 0, 0));
        Casilla39.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla39.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla39.setLayout(null);

        Casilla40.setBackground(new java.awt.Color(255, 255, 255));
        Casilla40.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla40.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla40.setLayout(null);

        Casilla41.setBackground(new java.awt.Color(255, 255, 255));
        Casilla41.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla41.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla41.setLayout(null);

        Casilla42.setBackground(new java.awt.Color(255, 255, 255));
        Casilla42.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla42.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla42.setLayout(null);

        Casilla43.setBackground(new java.awt.Color(255, 255, 255));
        Casilla43.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla43.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla43.setLayout(null);

        Casilla44.setBackground(new java.awt.Color(255, 255, 255));
        Casilla44.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla44.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla44.setLayout(null);

        Casilla45.setBackground(new java.awt.Color(255, 255, 255));
        Casilla45.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla45.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla45.setLayout(null);

        Casilla46.setBackground(new java.awt.Color(255, 102, 102));
        Casilla46.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla46.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla46.setLayout(null);

        Casilla47.setBackground(new java.awt.Color(255, 255, 255));
        Casilla47.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla47.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla47.setLayout(null);

        Casilla48.setBackground(new java.awt.Color(255, 255, 255));
        Casilla48.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla48.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla48.setLayout(null);

        Casilla49.setBackground(new java.awt.Color(255, 255, 255));
        Casilla49.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla49.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla49.setLayout(null);

        Casilla50.setBackground(new java.awt.Color(255, 255, 255));
        Casilla50.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla50.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla50.setLayout(null);

        Casilla51.setBackground(new java.awt.Color(153, 255, 153));
        Casilla51.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla51.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla51.setLayout(null);

        Casilla52.setBackground(new java.awt.Color(255, 255, 255));
        Casilla52.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla52.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla52.setLayout(null);

        Casilla53.setBackground(new java.awt.Color(255, 255, 255));
        Casilla53.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla53.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla53.setLayout(null);

        Casilla54.setBackground(new java.awt.Color(255, 255, 255));
        Casilla54.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla54.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla54.setLayout(null);

        Casilla55.setBackground(new java.awt.Color(255, 255, 255));
        Casilla55.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla55.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla55.setLayout(null);

        Casilla56.setBackground(new java.awt.Color(0, 204, 0));
        Casilla56.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla56.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla56.setLayout(null);

        Casilla57.setBackground(new java.awt.Color(255, 255, 255));
        Casilla57.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla57.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla57.setLayout(null);

        Casilla58.setBackground(new java.awt.Color(255, 255, 255));
        Casilla58.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla58.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla58.setLayout(null);

        Casilla59.setBackground(new java.awt.Color(255, 255, 255));
        Casilla59.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla59.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla59.setLayout(null);

        Casilla63.setBackground(new java.awt.Color(153, 255, 153));
        Casilla63.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla63.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla63.setLayout(null);

        Casilla61.setBackground(new java.awt.Color(255, 255, 255));
        Casilla61.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla61.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla61.setLayout(null);

        Casilla60.setBackground(new java.awt.Color(255, 255, 255));
        Casilla60.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla60.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla60.setLayout(null);

        Casilla62.setBackground(new java.awt.Color(255, 255, 255));
        Casilla62.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla62.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla62.setLayout(null);

        Casilla64.setBackground(new java.awt.Color(255, 255, 255));
        Casilla64.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla64.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla64.setLayout(null);

        Casilla65.setBackground(new java.awt.Color(255, 255, 255));
        Casilla65.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla65.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla65.setLayout(null);

        Casilla66.setBackground(new java.awt.Color(255, 255, 255));
        Casilla66.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla66.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla66.setLayout(null);

        Casilla67.setBackground(new java.awt.Color(255, 255, 255));
        Casilla67.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla67.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla67.setLayout(null);

        Casilla68.setBackground(new java.awt.Color(255, 255, 153));
        Casilla68.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla68.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla68.setLayout(null);

        Casilla1.setBackground(new java.awt.Color(255, 255, 255));
        Casilla1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla1.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla1.setLayout(null);

        Casilla2.setBackground(new java.awt.Color(255, 255, 255));
        Casilla2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla2.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla2.setLayout(null);

        Casilla3.setBackground(new java.awt.Color(255, 255, 255));
        Casilla3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla3.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla3.setLayout(null);

        Casilla4.setBackground(new java.awt.Color(255, 255, 255));
        Casilla4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla4.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla4.setLayout(null);

        Casilla5.setBackground(new java.awt.Color(255, 255, 51));
        Casilla5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla5.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla5.setLayout(null);

        Casilla6.setBackground(new java.awt.Color(255, 255, 255));
        Casilla6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla6.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla6.setLayout(null);

        Casilla7.setBackground(new java.awt.Color(255, 255, 255));
        Casilla7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla7.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla7.setLayout(null);

        Casilla8.setBackground(new java.awt.Color(255, 255, 255));
        Casilla8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla8.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla8.setLayout(null);

        Casilla9.setBackground(new java.awt.Color(255, 255, 255));
        Casilla9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla9.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla9.setLayout(null);

        Casilla10.setBackground(new java.awt.Color(255, 255, 255));
        Casilla10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla10.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla10.setLayout(null);

        Casilla11.setBackground(new java.awt.Color(255, 255, 255));
        Casilla11.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla11.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla11.setLayout(null);

        Casilla12.setBackground(new java.awt.Color(255, 255, 153));
        Casilla12.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla12.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla12.setLayout(null);

        Casilla13.setBackground(new java.awt.Color(255, 255, 255));
        Casilla13.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla13.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla13.setLayout(null);

        Casilla14.setBackground(new java.awt.Color(255, 255, 255));
        Casilla14.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla14.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla14.setLayout(null);

        Casilla15.setBackground(new java.awt.Color(255, 255, 255));
        Casilla15.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla15.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla15.setLayout(null);

        Casilla16.setBackground(new java.awt.Color(255, 255, 255));
        Casilla16.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla16.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla16.setLayout(null);

        Casilla17.setBackground(new java.awt.Color(102, 153, 255));
        Casilla17.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla17.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla17.setLayout(null);

        Casilla18.setBackground(new java.awt.Color(255, 255, 255));
        Casilla18.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla18.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla18.setLayout(null);

        Casilla19.setBackground(new java.awt.Color(255, 255, 255));
        Casilla19.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla19.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla19.setLayout(null);

        Casilla20.setBackground(new java.awt.Color(255, 255, 255));
        Casilla20.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla20.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla20.setLayout(null);

        Casilla21.setBackground(new java.awt.Color(255, 255, 255));
        Casilla21.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla21.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla21.setLayout(null);

        Casilla22.setBackground(new java.awt.Color(51, 51, 255));
        Casilla22.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla22.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla22.setLayout(null);

        Casilla23.setBackground(new java.awt.Color(255, 255, 255));
        Casilla23.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla23.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla23.setLayout(null);

        Casilla24.setBackground(new java.awt.Color(255, 255, 255));
        Casilla24.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla24.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla24.setLayout(null);

        Casilla25.setBackground(new java.awt.Color(255, 255, 255));
        Casilla25.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla25.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla25.setLayout(null);

        Casilla26.setBackground(new java.awt.Color(255, 255, 255));
        Casilla26.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla26.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla26.setLayout(null);

        Casilla27.setBackground(new java.awt.Color(255, 255, 255));
        Casilla27.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla27.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla27.setLayout(null);

        Casilla28.setBackground(new java.awt.Color(255, 255, 255));
        Casilla28.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla28.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla28.setLayout(null);

        Casilla29.setBackground(new java.awt.Color(102, 153, 255));
        Casilla29.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla29.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla29.setLayout(null);

        Casilla30.setBackground(new java.awt.Color(255, 255, 255));
        Casilla30.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla30.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla30.setLayout(null);

        Casilla31.setBackground(new java.awt.Color(255, 255, 255));
        Casilla31.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla31.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla31.setLayout(null);

        Casilla32.setBackground(new java.awt.Color(255, 255, 255));
        Casilla32.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla32.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla32.setLayout(null);

        Casilla34.setBackground(new java.awt.Color(255, 102, 102));
        Casilla34.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla34.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla34.setLayout(null);

        Casilla33.setBackground(new java.awt.Color(255, 255, 255));
        Casilla33.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        Casilla33.setPreferredSize(new java.awt.Dimension(64, 22));
        Casilla33.setLayout(null);

        pasilloV1.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV1.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV1.setLayout(null);

        pasilloV2.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV2.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV2.setLayout(null);

        pasilloV3.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV3.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV3.setLayout(null);

        pasilloV4.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV4.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV4.setLayout(null);

        pasilloV5.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV5.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV5.setLayout(null);

        pasilloV6.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV6.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV6.setLayout(null);

        pasilloV7.setBackground(new java.awt.Color(0, 204, 0));
        pasilloV7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloV7.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloV7.setLayout(null);

        pasilloR1.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR1.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR1.setLayout(null);

        pasilloR2.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR2.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR2.setLayout(null);

        pasilloR3.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR3.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR3.setLayout(null);

        pasilloR4.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR4.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR4.setLayout(null);

        pasilloR5.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR5.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR5.setLayout(null);

        pasilloR6.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR6.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR6.setLayout(null);

        pasilloR7.setBackground(new java.awt.Color(255, 0, 0));
        pasilloR7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloR7.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloR7.setLayout(null);

        pasilloA1.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA1.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA1.setLayout(null);

        pasilloA2.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA2.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA2.setLayout(null);

        pasilloA3.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA3.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA3.setLayout(null);

        pasilloA4.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA4.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA4.setLayout(null);

        pasilloA5.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA5.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA5.setLayout(null);

        pasilloA6.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA6.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA6.setLayout(null);

        pasilloA7.setBackground(new java.awt.Color(51, 51, 255));
        pasilloA7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloA7.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloA7.setLayout(null);

        pasilloAm1.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm1.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm1.setLayout(null);

        pasilloAm2.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm2.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm2.setLayout(null);

        pasilloAm3.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm3.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm3.setLayout(null);

        pasilloAm4.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm4.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm4.setLayout(null);

        pasilloAm5.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm5.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm5.setLayout(null);

        pasilloAm6.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm6.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm6.setLayout(null);

        pasilloAm7.setBackground(new java.awt.Color(255, 255, 51));
        pasilloAm7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pasilloAm7.setPreferredSize(new java.awt.Dimension(64, 22));
        pasilloAm7.setLayout(null);

        meta.setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout metaLayout = new javax.swing.GroupLayout(meta);
        meta.setLayout(metaLayout);
        metaLayout.setHorizontalGroup(
            metaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 204, Short.MAX_VALUE)
        );
        metaLayout.setVerticalGroup(
            metaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 210, Short.MAX_VALUE)
        );

        jButton1.setText("Tirar dado");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(322, 322, 322)
                .addComponent(jButton1)
                .addContainerGap(322, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jButton1)
                .addContainerGap(43, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(SpawnRojo, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla35, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla36, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla37, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla38, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla39, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla40, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla41, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla42, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla34, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloR7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla33, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla31, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla30, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(SpawnAzul, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla50, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla51, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla52, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla49, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla53, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla48, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla54, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla47, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV3, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla55, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla46, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV4, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla56, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla45, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla57, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla44, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV6, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla58, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla43, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloV7, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla59, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(meta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla25, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA7, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla9, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla24, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA6, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla10, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla23, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA5, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla11, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla22, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA4, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla12, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla21, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA3, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla13, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla20, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla14, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla19, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloA1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla15, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla18, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla17, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla16, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(layout.createSequentialGroup()
                .addComponent(SpawnVerde, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla60, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla61, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla62, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla63, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla64, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla65, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla66, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla67, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pasilloAm7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pasilloAm1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla68, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Casilla8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Casilla1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(SpawnAmarillo, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SpawnRojo, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(Casilla35, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla36, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla37, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla38, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla39, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(Casilla40, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla41, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla42, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(Casilla34, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(pasilloR5, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloR7, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(Casilla33, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla32, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla31, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla30, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla29, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(Casilla28, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla27, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla26, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(SpawnAzul, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla50, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla51, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla52, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla49, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla53, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla48, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla54, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla47, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV3, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla55, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla46, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV4, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla56, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla45, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV5, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla57, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla44, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV6, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla58, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla43, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloV7, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla59, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(meta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla25, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA7, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla9, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla24, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA6, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla10, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla23, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA5, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla11, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla22, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA4, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla12, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla21, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA3, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla13, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla20, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla14, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla19, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloA1, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla15, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla18, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla17, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla16, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SpawnVerde, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla60, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla61, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla62, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla63, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla64, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla65, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla66, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla67, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pasilloAm7, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm5, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(pasilloAm1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla68, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Casilla8, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla7, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla5, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(Casilla1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(SpawnAmarillo, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TableroVista.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TableroVista.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TableroVista.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TableroVista.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TableroVista().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Casilla1;
    private javax.swing.JPanel Casilla10;
    private javax.swing.JPanel Casilla11;
    private javax.swing.JPanel Casilla12;
    private javax.swing.JPanel Casilla13;
    private javax.swing.JPanel Casilla14;
    private javax.swing.JPanel Casilla15;
    private javax.swing.JPanel Casilla16;
    private javax.swing.JPanel Casilla17;
    private javax.swing.JPanel Casilla18;
    private javax.swing.JPanel Casilla19;
    private javax.swing.JPanel Casilla2;
    private javax.swing.JPanel Casilla20;
    private javax.swing.JPanel Casilla21;
    private javax.swing.JPanel Casilla22;
    private javax.swing.JPanel Casilla23;
    private javax.swing.JPanel Casilla24;
    private javax.swing.JPanel Casilla25;
    private javax.swing.JPanel Casilla26;
    private javax.swing.JPanel Casilla27;
    private javax.swing.JPanel Casilla28;
    private javax.swing.JPanel Casilla29;
    private javax.swing.JPanel Casilla3;
    private javax.swing.JPanel Casilla30;
    private javax.swing.JPanel Casilla31;
    private javax.swing.JPanel Casilla32;
    private javax.swing.JPanel Casilla33;
    private javax.swing.JPanel Casilla34;
    private javax.swing.JPanel Casilla35;
    private javax.swing.JPanel Casilla36;
    private javax.swing.JPanel Casilla37;
    private javax.swing.JPanel Casilla38;
    private javax.swing.JPanel Casilla39;
    private javax.swing.JPanel Casilla4;
    private javax.swing.JPanel Casilla40;
    private javax.swing.JPanel Casilla41;
    private javax.swing.JPanel Casilla42;
    private javax.swing.JPanel Casilla43;
    private javax.swing.JPanel Casilla44;
    private javax.swing.JPanel Casilla45;
    private javax.swing.JPanel Casilla46;
    private javax.swing.JPanel Casilla47;
    private javax.swing.JPanel Casilla48;
    private javax.swing.JPanel Casilla49;
    private javax.swing.JPanel Casilla5;
    private javax.swing.JPanel Casilla50;
    private javax.swing.JPanel Casilla51;
    private javax.swing.JPanel Casilla52;
    private javax.swing.JPanel Casilla53;
    private javax.swing.JPanel Casilla54;
    private javax.swing.JPanel Casilla55;
    private javax.swing.JPanel Casilla56;
    private javax.swing.JPanel Casilla57;
    private javax.swing.JPanel Casilla58;
    private javax.swing.JPanel Casilla59;
    private javax.swing.JPanel Casilla6;
    private javax.swing.JPanel Casilla60;
    private javax.swing.JPanel Casilla61;
    private javax.swing.JPanel Casilla62;
    private javax.swing.JPanel Casilla63;
    private javax.swing.JPanel Casilla64;
    private javax.swing.JPanel Casilla65;
    private javax.swing.JPanel Casilla66;
    private javax.swing.JPanel Casilla67;
    private javax.swing.JPanel Casilla68;
    private javax.swing.JPanel Casilla7;
    private javax.swing.JPanel Casilla8;
    private javax.swing.JPanel Casilla9;
    private javax.swing.JPanel SpawnAmarillo;
    private javax.swing.JPanel SpawnAzul;
    private javax.swing.JPanel SpawnRojo;
    private javax.swing.JPanel SpawnVerde;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel meta;
    private javax.swing.JPanel pasilloA1;
    private javax.swing.JPanel pasilloA2;
    private javax.swing.JPanel pasilloA3;
    private javax.swing.JPanel pasilloA4;
    private javax.swing.JPanel pasilloA5;
    private javax.swing.JPanel pasilloA6;
    private javax.swing.JPanel pasilloA7;
    private javax.swing.JPanel pasilloAm1;
    private javax.swing.JPanel pasilloAm2;
    private javax.swing.JPanel pasilloAm3;
    private javax.swing.JPanel pasilloAm4;
    private javax.swing.JPanel pasilloAm5;
    private javax.swing.JPanel pasilloAm6;
    private javax.swing.JPanel pasilloAm7;
    private javax.swing.JPanel pasilloR1;
    private javax.swing.JPanel pasilloR2;
    private javax.swing.JPanel pasilloR3;
    private javax.swing.JPanel pasilloR4;
    private javax.swing.JPanel pasilloR5;
    private javax.swing.JPanel pasilloR6;
    private javax.swing.JPanel pasilloR7;
    private javax.swing.JPanel pasilloV1;
    private javax.swing.JPanel pasilloV2;
    private javax.swing.JPanel pasilloV3;
    private javax.swing.JPanel pasilloV4;
    private javax.swing.JPanel pasilloV5;
    private javax.swing.JPanel pasilloV6;
    private javax.swing.JPanel pasilloV7;
    // End of variables declaration//GEN-END:variables
}
