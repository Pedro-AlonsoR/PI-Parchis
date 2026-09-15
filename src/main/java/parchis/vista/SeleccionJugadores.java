package parchis.vista;

import parchis.controlador.ControladorJuego;
import parchis.modelo.Jugador;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

/**
 *
 * @author Pedro Alonso Rengel
 */

public class SeleccionJugadores extends JFrame {

    //Lista colores
    private static final Color[] COLOR_VAL = { Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN };
    private static final String[] COLOR_TXT = { "Rojo", "Azul", "Amarillo", "Verde" };

    //UI de cada jugador
    private static final int MAX = 4;
    private final JTextField[] tfNombre   = new JTextField[MAX];
    private final JComboBox<ColorOpt>[] cbColor = new JComboBox[MAX];
    private final JPanel[] fila           = new JPanel[MAX];

    private JComboBox<Integer> boxNumJug;

    public SeleccionJugadores() {
        super("Nueva partida – Parchís");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        pack();
        setMinimumSize(new Dimension(360, 420));
        setLocationRelativeTo(null);
    }

    //UI
    private void initUI() {
        Font fBase = Optional.ofNullable(UIManager.getFont("Label.font"))
                             .orElse(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.setBackground(Color.WHITE);
        setContentPane(root);

        //Logo
        JLabel logoLabel = new JLabel();
        logoLabel.setPreferredSize(new Dimension(220, 80));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setBorder(new LineBorder(new Color(220,220,220), 1, true));
        logoLabel.setText("Tu logo aquí");
        root.add(logoLabel, BorderLayout.NORTH);

        //Selector de jugadores
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        JLabel lbl = new JLabel("Jugadores:");
        lbl.setFont(fBase.deriveFont(Font.BOLD, 14f));
        boxNumJug = new JComboBox<>(new Integer[]{2,3,4});
        boxNumJug.setFont(fBase);
        boxNumJug.addActionListener(e -> actualizarVisibilidad());
        top.add(lbl);   top.add(boxNumJug);
        root.add(top, BorderLayout.BEFORE_FIRST_LINE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        for (int i = 0; i < MAX; i++) {
            fila[i] = crearFila(i, fBase);
            content.add(fila[i]);
            content.add(Box.createVerticalStrut(10));
        }
        actualizarVisibilidad();
        JScrollPane scroll = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        //Botón iniciar
        JButton btnStart = new JButton("Iniciar partida");
        btnStart.setFont(fBase.deriveFont(Font.BOLD, 15f));
        btnStart.setBackground(new Color(45, 145, 90));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);
        btnStart.setBorder(new EmptyBorder(8,20,8,20));
        btnStart.addActionListener(this::iniciar);
        JPanel south = new JPanel(); south.setOpaque(false);
        south.add(btnStart);
        root.add(south, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { SwingUtilities.invokeLater(root::revalidate); }
        });
    }

    private JPanel crearFila(int idx, Font fBase) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBorder(new EmptyBorder(6, 8, 6, 8));
        p.setBackground(tint(COLOR_VAL[idx], 0.12f));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lab = new JLabel("Jugador " + (idx+1));
        lab.setFont(fBase);
        lab.setPreferredSize(new Dimension(80, 24));

        tfNombre[idx] = new JTextField(8);
        tfNombre[idx].setMaximumSize(new Dimension(140, 28));
        tfNombre[idx].setFont(fBase);

        cbColor[idx] = new JComboBox<>(ColorOpt.values());
        cbColor[idx].setRenderer(new ColorRenderer());
        cbColor[idx].setSelectedItem(ColorOpt.values()[idx]);
        cbColor[idx].setMaximumSize(new Dimension(120, 26));
        int id = idx;
        cbColor[idx].addActionListener(a -> {
            Color sel = ((ColorOpt) cbColor[id].getSelectedItem()).c;
            p.setBackground(tint(sel, 0.12f));
        });

        p.add(lab);
        p.add(Box.createHorizontalStrut(6));
        p.add(tfNombre[idx]);
        p.add(Box.createHorizontalGlue());
        p.add(cbColor[idx]);
        return p;
    }

    private void actualizarVisibilidad() {
        int active = (int) boxNumJug.getSelectedItem();
        for (int i = 0; i < MAX; i++) fila[i].setVisible(i < active);
        revalidate();
    }

    private void iniciar(ActionEvent e) {
        int n = (int) boxNumJug.getSelectedItem();
        List<Jugador> lst = new ArrayList<>();
        Set<Color> usados = new HashSet<>();
        for (int i = 0; i < n; i++) {
            String nombre = tfNombre[i].getText().trim();
            if (nombre.isEmpty()) nombre = "Jugador " + (i+1);
            Color col = ((ColorOpt) cbColor[i].getSelectedItem()).c;
            if (!usados.add(col)) {
                JOptionPane.showMessageDialog(this, "Cada jugador debe tener un color único.");
                return;
            }
            lst.add(new Jugador(nombre, col));
        }
        new ControladorJuego(lst).iniciar();
        dispose();
    }

    //Utilidades
    private static Color tint(Color c, float f) {
        int r = c.getRed();   int g = c.getGreen();   int b = c.getBlue();
        return new Color(r + (int)((255-r)*f), g + (int)((255-g)*f), b + (int)((255-b)*f));
    }

    //Colores
    private enum ColorOpt { ROJO(Color.RED, "Rojo"), AZUL(Color.BLUE, "Azul"), AMARILLO(Color.YELLOW, "Amarillo"), VERDE(Color.GREEN, "Verde");
        final Color c; final String t; ColorOpt(Color c, String t){this.c=c;this.t=t;} public String toString(){return t;} }

    //Renderización de animación
    private static class ColorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,Object v,int i,boolean s,boolean f){
            JLabel l=(JLabel)super.getListCellRendererComponent(list,v,i,s,f);
            if(v instanceof ColorOpt co){
                l.setText(co.t);
                l.setIcon(icon(co.c));
            }
            return l;
        }
        private Icon icon(Color c){
            BufferedImage img=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=img.createGraphics();
            g.setColor(c); g.fillRect(0,0,16,16); g.dispose();
            return new ImageIcon(img);
        }
    }
}
