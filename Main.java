import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public class Main {

    private static final String[] TABLAS = {
        "categoria", "usuario", "direccionusuario", "pedido", "producto",
        "detallepedido", "ingrediente", "detallereceta", "puesto", "empleado",
        "empresa", "movimientomateriaprima", "movimientoproducto", "produccion"
    };

    public static void main(String[] args) {
        boolean conectado = false;
        System.out.println("SISTEMA TAMALES | Java + MySQL");
        System.out.println("Inicio automatico: MySQL privado + SQL final + conexion JDBC.\n");

        try {
            Path archivoSql = localizarSql();
            ConexionBD.cargarControlador(archivoSql.getParent());
            ConfiguracionBD configuracion = ConexionAutomatica.preparar(archivoSql);
            try (Connection conexion = ConexionBD.conectar(configuracion)) {
                conectado = true;
                mostrarServidor(conexion);
                comprobarVersionDeTablas(conexion);
                mostrarTablas(conexion);
                mostrarProductos(conexion);
                mostrarEmpleados(conexion);
                mostrarRutinas(conexion);
                System.out.println("\nPRUEBA COMPLETADA: conexion y consultas realizadas correctamente.");
                System.out.println("Puedes volver a presionar Run: se conservan los datos de esta instancia.");
            }
            if (!Arrays.asList(args).contains("--solo-conexion") && !GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> new Ventana(configuracion).setVisible(true));
            }
        } catch (SQLException error) {
            System.err.println(conectado
                ? "\nLa conexion se abrio, pero una consulta no pudo completarse."
                : "\nNo se pudo abrir la conexion con MySQL.");
            System.err.printf("Codigo SQL: %d | SQLState: %s%n",
                error.getErrorCode(), error.getSQLState());
            System.err.println(error.getMessage());
            System.err.println(orientacion(error, conectado));
            System.exit(1);
        } catch (IOException error) {
            System.err.println("\nNo se pudo completar el inicio automatico: " + error.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException error) {
            System.err.println("La configuracion local de la instancia no es valida: " + error.getMessage());
            System.exit(1);
        }
    }

    private static Path localizarSql() throws IOException {
        Path desde = Path.of("").toAbsolutePath();
        Path encontrado = buscarSql(desde);
        if (encontrado != null) {
            return encontrado;
        }
        try {
            Path clases = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            encontrado = buscarSql(clases);
            if (encontrado != null) {
                return encontrado;
            }
        } catch (java.net.URISyntaxException | SecurityException error) {
            // Si el editor cambia la carpeta, tambien intentamos desde las clases.
        }
        throw new IOException("No encuentro sistema_tamales_final.sql. Abre la carpeta SistemaTamales completa, sin separar sus archivos.");
    }

    private static Path buscarSql(Path desde) {
        for (Path carpeta = desde; carpeta != null; carpeta = carpeta.getParent()) {
            Path sql = carpeta.resolve("sistema_tamales_final.sql");
            if (Files.isRegularFile(sql)) {
                return sql;
            }
        }
        return null;
    }

    private static void mostrarServidor(Connection conexion) throws SQLException {
        String sql = "SELECT DATABASE() AS base, VERSION() AS version, "
            + "CURRENT_TIMESTAMP AS fecha, @@port AS puerto";
        try (Statement consulta = conexion.createStatement(); ResultSet datos = consulta.executeQuery(sql)) {
            if (datos.next()) {
                System.out.println("CONEXION ESTABLECIDA");
                System.out.println("Base consultada: " + datos.getString("base"));
                System.out.println("Servidor: " + datos.getString("version"));
                System.out.println("Puerto: " + datos.getInt("puerto"));
                System.out.println("Fecha del servidor: " + datos.getString("fecha"));
            }
        }
    }

    private static void comprobarVersionDeTablas(Connection conexion) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND "
            + "((LOWER(table_name) = 'empleado' AND LOWER(column_name) = 'nombre') "
            + "OR (LOWER(table_name) = 'pedido' AND LOWER(column_name) = 'subtotal'))";
        try (Statement consulta = conexion.createStatement(); ResultSet datos = consulta.executeQuery(sql)) {
            datos.next();
            if (datos.getInt(1) > 0) {
                System.out.println("\nAVISO: esta base conserva columnas de la version anterior.");
                System.out.println("Conectar no demuestra que ya se haya cargado el SQL final.");
            } else {
                System.out.println("Revision inicial: no aparecen las dos columnas antiguas comprobadas.");
            }
        }
    }

    private static void mostrarTablas(Connection conexion) throws SQLException {
        System.out.println("\nTABLAS DEL SQL Y CANTIDAD DE REGISTROS");
        for (String tabla : TABLAS) {
            String sql = "SELECT COUNT(*) AS cantidad FROM `" + tabla + "`";
            try (Statement consulta = conexion.createStatement(); ResultSet datos = consulta.executeQuery(sql)) {
                datos.next();
                System.out.printf("%-27s %d%n", tabla, datos.getLong("cantidad"));
            }
        }
    }

    private static void mostrarProductos(Connection conexion) throws SQLException {
        System.out.println("\nPRODUCTOS | JOIN producto + categoria (primeros 5)");
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, p.precio, p.existencia "
            + "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria "
            + "ORDER BY p.id_producto LIMIT ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, 5);
            try (ResultSet datos = consulta.executeQuery()) {
                int filas = 0;
                while (datos.next()) {
                    System.out.printf("%d | %s | %s | $%s | Existencia: %d%n",
                        datos.getInt("id_producto"), datos.getString("nombre"),
                        datos.getString("categoria"), datos.getBigDecimal("precio").toPlainString(),
                        datos.getInt("existencia"));
                    filas++;
                }
                if (filas == 0) {
                    System.out.println("La consulta funciono, pero no hay productos con categoria.");
                }
            }
        }
    }

    private static void mostrarEmpleados(Connection conexion) throws SQLException {
        System.out.println("\nEMPLEADOS | JOIN empleado + usuario + puesto (primeros 5)");
        String sql = "SELECT e.id_empleado, u.nombre, u.apellido, p.nombre AS puesto "
            + "FROM empleado e JOIN usuario u ON u.id_usuario = e.id_usuario "
            + "JOIN puesto p ON p.id_puesto = e.id_puesto ORDER BY e.id_empleado LIMIT ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, 5);
            try (ResultSet datos = consulta.executeQuery()) {
                int filas = 0;
                while (datos.next()) {
                    String apellido = datos.getString("apellido");
                    String nombre = datos.getString("nombre")
                        + (apellido == null || apellido.isBlank() ? "" : " " + apellido);
                    System.out.printf("%d | %s | %s%n", datos.getInt("id_empleado"),
                        nombre, datos.getString("puesto"));
                    filas++;
                }
                if (filas == 0) {
                    System.out.println("La consulta funciono, pero no hay empleados vinculados.");
                }
            }
        }
    }

    private static String orientacion(SQLException error, boolean conectado) {
        return switch (error.getErrorCode()) {
            case 1045 -> "Las credenciales de la instancia privada no coinciden. No borres su carpeta de datos.";
            case 1044, 1142 -> "El usuario de MySQL no tiene permisos para consultar esta base.";
            case 1049 -> "La base privada no termino de inicializarse. Conserva el mensaje anterior para revisarlo.";
            case 1054 -> "Falta una columna esperada. Revisa si estas usando el SQL final.";
            case 1146 -> "Falta una tabla esperada o su nombre no coincide, incluyendo mayusculas.";
            default -> conectado
                ? "Revisa que la estructura de las tablas corresponda al SQL final."
                : "Revisa el mensaje de inicio anterior. Se necesita MySQL Server 8.0 u 8.4 instalado, no solamente Workbench.";
        };
    }

    private static void mostrarRutinas(Connection conexion) throws SQLException {
        System.out.println("\nRUTINAS DISPONIBLES EN LA BASE");
        Set<String> funciones = new HashSet<>();
        String sql = "SELECT routine_name, routine_type FROM information_schema.routines "
            + "WHERE routine_schema = DATABASE() ORDER BY routine_type, routine_name";
        int total = 0;
        try (Statement consulta = conexion.createStatement(); ResultSet datos = consulta.executeQuery(sql)) {
            while (datos.next()) {
                String nombre = datos.getString("routine_name");
                String tipo = datos.getString("routine_type");
                System.out.println(tipo + " | " + nombre);
                if ("FUNCTION".equals(tipo)) {
                    funciones.add(nombre);
                }
                total++;
            }
        }
        if (total == 0) {
            System.out.println("No hay rutinas visibles para esta conexion.");
        }
        if (funciones.contains("fn_validar_stock_producto")
                && funciones.contains("fn_obtener_precio_vigente")) {
            System.out.println("\nFUNCIONES DEL SQL FINAL | Producto 1");
            String prueba = "SELECT fn_validar_stock_producto(1, 1) AS disponible, "
                + "fn_obtener_precio_vigente(1, 1) AS precio_normal, "
                + "fn_obtener_precio_vigente(1, 100) AS precio_cien";
            try (Statement consulta = conexion.createStatement(); ResultSet datos = consulta.executeQuery(prueba)) {
                if (datos.next()) {
                    System.out.println("Stock para una pieza (1=si, 0=no): " + datos.getString("disponible"));
                    System.out.println("Precio unitario al pedir una pieza: $" + datos.getString("precio_normal"));
                    System.out.println("Precio unitario al pedir 100 piezas: $" + datos.getString("precio_cien"));
                }
            }
            System.out.println("Solo se consultaron funciones de lectura; no se ejecutaron procedimientos.");
        } else {
            System.out.println("Las dos funciones esperadas no estan visibles; se omite su prueba.");
        }
    }

    private record ConfiguracionBD(String host, int puerto, String base, String usuario, String clave) {
        public ConfiguracionBD {
            if (host == null || base == null || usuario == null || clave == null
                    || !host.matches("[A-Za-z0-9._-]+") || !base.matches("[A-Za-z0-9_]+")
                    || usuario.isBlank() || puerto < 1 || puerto > 65535) {
                throw new IllegalArgumentException("La configuracion de MySQL no es valida.");
            }
        }

        @Override
        public String toString() {
            return "ConfiguracionBD[host=" + host + ", puerto=" + puerto + ", base=" + base
                + ", usuario=<privado>, clave=<oculta>]";
        }
    }

    private record DatosTabla(String[] columnas, List<Object[]> filas) {
    }

    @FunctionalInterface
    private interface Consulta<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private static final class Ventana extends JFrame {
        private static final long serialVersionUID = 1L;
        private final ConfiguracionBD configuracion;
        private final JTable categorias = tabla();
        private final JTable productos = tabla();
        private final JTable reportes = tabla();
        private final JLabel estado = new JLabel("Conectado. No se modifica nada hasta que guardes una operación.");
        private final JLabel nota = new JLabel("Selecciona un reporte. Las consultas no cambian datos.");
        private final List<AbstractButton> botones = new ArrayList<>();
        private boolean ocupado;

        private Ventana(ConfiguracionBD configuracion) {
            super("Sistema Tamales | Java + MySQL");
            this.configuracion = configuracion;
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent evento) {
                    if (ocupado) {
                        JOptionPane.showMessageDialog(Ventana.this, "Espera a que termine la operación antes de cerrar.");
                    } else {
                        dispose();
                    }
                }
            });
            JPanel contenido = new JPanel(new BorderLayout(12, 12));
            contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            contenido.add(new JLabel("SISTEMA TAMALES  ·  " + configuracion.base()
                + "  ·  Puerto " + configuracion.puerto()), BorderLayout.NORTH);
            JTabbedPane pestanas = new JTabbedPane();
            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
            acciones.add(boton("Crear categoría", this::crear));
            acciones.add(boton("Editar seleccionada", this::editar));
            acciones.add(boton("Eliminar seleccionada", this::eliminar));
            acciones.add(boton("Actualizar lista", this::cargarCategorias));
            pestanas.addTab("Categorías · CRUD", panelTabla(categorias, acciones,
                new JLabel("Selecciona una fila para editar o eliminar. Las categorías con productos no se pueden borrar.")));
            JPanel accionesProductos = new JPanel(new FlowLayout(FlowLayout.LEFT));
            accionesProductos.add(boton("Consultar productos", this::cargarProductos));
            pestanas.addTab("Productos", panelTabla(productos, accionesProductos,
                new JLabel("Catálogo de consulta. Esta pestaña no modifica productos ni existencias.")));
            JPanel accionesReportes = new JPanel(new FlowLayout(FlowLayout.LEFT));
            accionesReportes.add(boton("Ventas de una fecha", this::ventasFecha));
            accionesReportes.add(boton("Ventas por día", () -> consultarReporte("Ventas por día",
                "Solo pedidos TERMINADO; agrupados por día, no por hora.", ReportesService::ventasPorDia)));
            accionesReportes.add(boton("Clientes del trimestre", this::clientesTrimestre));
            accionesReportes.add(boton("Ganancias estimadas", () -> consultarReporte("Ganancias estimadas",
                "Costos ponderados de producción. N/D = información insuficiente; no es ganancia histórica real.",
                ReportesService::gananciasEstimadas)));
            pestanas.addTab("Reportes", panelTabla(reportes, accionesReportes, nota));
            pestanas.addChangeListener(evento -> {
                if (ocupado) return;
                if (pestanas.getSelectedIndex() == 0) cargarCategorias();
                if (pestanas.getSelectedIndex() == 1) cargarProductos();
            });
            contenido.add(pestanas, BorderLayout.CENTER);
            contenido.add(estado, BorderLayout.SOUTH);
            setContentPane(contenido);
            setSize(1080, 650);
            setMinimumSize(new java.awt.Dimension(900, 480));
            setLocationRelativeTo(null);
            cargarCategorias();
        }

        private static JTable tabla() {
            JTable tabla = new JTable();
            tabla.setFillsViewportHeight(true);
            tabla.setRowHeight(27);
            tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tabla.setAutoCreateRowSorter(true);
            javax.swing.table.DefaultTableCellRenderer formato = new javax.swing.table.DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;
                @Override protected void setValue(Object valor) {
                    super.setValue(valor == null ? "N/D" : valor);
                }
            };
            tabla.setDefaultRenderer(Object.class, formato);
            tabla.setDefaultRenderer(Number.class, formato);
            tabla.setDefaultRenderer(Boolean.class, formato);
            tabla.setDefaultRenderer(java.util.Date.class, formato);
            return tabla;
        }

        private JButton boton(String texto, Runnable accion) {
            JButton boton = new JButton(texto);
            boton.addActionListener(evento -> { if (!ocupado) accion.run(); });
            botones.add(boton);
            return boton;
        }

        private static JPanel panelTabla(JTable tabla, JPanel acciones, JLabel ayuda) {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(acciones, BorderLayout.NORTH);
            panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
            panel.add(ayuda, BorderLayout.SOUTH);
            return panel;
        }

        private void mostrar(JTable tabla, DatosTabla datos) {
            tabla.setModel(new DefaultTableModel(datos.filas().toArray(new Object[0][]), datos.columnas()) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int fila, int columna) { return false; }
                @Override public Class<?> getColumnClass(int columna) {
                    for (int fila = 0; fila < getRowCount(); fila++) {
                        Object valor = getValueAt(fila, columna);
                        if (valor != null) return valor.getClass();
                    }
                    return Object.class;
                }
            });
            estado.setText("Consulta terminada: " + datos.filas().size() + " registros.");
            if (tabla == reportes) {
                for (int columna = 0; columna < datos.columnas().length; columna++) {
                    if (datos.columnas()[columna].equals("Total") || datos.columnas()[columna].equals("Ventas")) {
                        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                        for (Object[] fila : datos.filas()) {
                            if (fila[columna] != null) total = total.add(new java.math.BigDecimal(fila[columna].toString()));
                        }
                        estado.setText("Consulta terminada: " + datos.filas().size() + " registros. Total de ventas: $"
                            + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    }
                }
            }
        }

        private <T> void tarea(String mensaje, boolean escritura, Consulta<T> consulta, Consumer<T> alTerminar) {
            if (ocupado) return;
            ocupado = true;
            botones.forEach(boton -> boton.setEnabled(false));
            estado.setText(mensaje + "...");
            new SwingWorker<T, Void>() {
                @Override protected T doInBackground() throws SQLException {
                    try (Connection conexion = escritura ? ConexionBD.conectarEscritura(configuracion)
                                                         : ConexionBD.conectar(configuracion)) {
                        return consulta.ejecutar(conexion);
                    }
                }
                @Override protected void done() {
                    ocupado = false;
                    botones.forEach(boton -> boton.setEnabled(true));
                    try {
                        alTerminar.accept(get());
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        mostrarError(error);
                    } catch (ExecutionException error) {
                        mostrarError(error.getCause());
                    } catch (RuntimeException error) {
                        mostrarError(error);
                    }
                }
            }.execute();
        }

        private void mostrarError(Throwable error) {
            String mensaje = error.getMessage();
            if (error instanceof SQLException sql) {
                if (sql.getErrorCode() == 1451) mensaje = "No se puede eliminar: hay productos que usan esta categoría.";
                else if (sql.getErrorCode() == 1205 || sql.getErrorCode() == 1213)
                    mensaje = "Otra operación está usando estos datos. Actualiza la lista y vuelve a intentar.";
                else if (sql.getErrorCode() == 1142) mensaje = "La cuenta no tiene permiso para esa operación.";
            }
            if (mensaje == null || mensaje.isBlank()) mensaje = "No se pudo completar la operación.";
            estado.setText("No se completó la operación. Puedes actualizar la lista para consultar su estado.");
            JOptionPane.showMessageDialog(this, mensaje, "Revisar operación", JOptionPane.WARNING_MESSAGE);
        }

        private void cargarCategorias() {
            tarea("Consultando categorías", false, CatalogoService::listarCategorias,
                datos -> mostrar(categorias, datos));
        }

        private void cargarProductos() {
            tarea("Consultando productos", false, ReportesService::productos,
                datos -> mostrar(productos, datos));
        }

        private int filaSeleccionada() {
            int fila = categorias.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Primero selecciona una categoría en la tabla.");
                return -1;
            }
            return categorias.convertRowIndexToModel(fila);
        }

        private void crear() {
            String nombre = JOptionPane.showInputDialog(this, "Nombre de la categoría (máximo 50 caracteres):", "Crear categoría", JOptionPane.QUESTION_MESSAGE);
            if (nombre == null) return;
            tarea("Guardando categoría", true, conexion -> CatalogoService.crearCategoria(conexion, nombre), id -> {
                cargarCategorias();
                JOptionPane.showMessageDialog(this, "Categoría creada con ID " + id + ".");
            });
        }

        private void editar() {
            int fila = filaSeleccionada();
            if (fila < 0) return;
            int id = ((Number) categorias.getModel().getValueAt(fila, 0)).intValue();
            String original = String.valueOf(categorias.getModel().getValueAt(fila, 1));
            String nombre = (String) JOptionPane.showInputDialog(this, "Nuevo nombre de la categoría " + id + ":",
                "Editar categoría", JOptionPane.QUESTION_MESSAGE, null, null, original);
            if (nombre == null) return;
            tarea("Actualizando categoría", true, conexion -> {
                CatalogoService.actualizarCategoria(conexion, id, original, nombre);
                return id;
            }, actualizado -> {
                cargarCategorias();
                JOptionPane.showMessageDialog(this, "Categoría " + actualizado + " actualizada.");
            });
        }

        private void eliminar() {
            int fila = filaSeleccionada();
            if (fila < 0) return;
            int id = ((Number) categorias.getModel().getValueAt(fila, 0)).intValue();
            String nombre = String.valueOf(categorias.getModel().getValueAt(fila, 1));
            int respuesta = JOptionPane.showOptionDialog(this,
                "¿Eliminar la categoría " + id + " — " + nombre + "?\nNo se borrarán productos. Esta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[] {"Eliminar", "Cancelar"}, "Cancelar");
            if (respuesta != 0) return;
            tarea("Eliminando categoría", true, conexion -> {
                CatalogoService.eliminarCategoria(conexion, id, nombre);
                return id;
            }, eliminado -> {
                cargarCategorias();
                JOptionPane.showMessageDialog(this, "Categoría " + eliminado + " eliminada.");
            });
        }

        private void consultarReporte(String titulo, String descripcion, Consulta<DatosTabla> consulta) {
            nota.setText(descripcion);
            tarea(titulo, false, consulta, datos -> mostrar(reportes, datos));
        }

        private void ventasFecha() {
            String entrada = JOptionPane.showInputDialog(this, "Fecha de venta (AAAA-MM-DD):", java.time.LocalDate.now().toString());
            if (entrada == null) return;
            try {
                java.time.LocalDate fecha = java.time.LocalDate.parse(entrada.strip());
                consultarReporte("Ventas del " + fecha, "Pedidos TERMINADO de " + fecha + ". Sin filas = no hubo ventas terminadas ese día.",
                    conexion -> ReportesService.ventasDiarias(conexion, fecha));
            } catch (java.time.DateTimeException error) {
                mostrarError(new IllegalArgumentException("Escribe una fecha válida, por ejemplo 2026-05-10."));
            }
        }

        private void clientesTrimestre() {
            String entrada = JOptionPane.showInputDialog(this, "Año a consultar:", Integer.toString(java.time.LocalDate.now().getYear()));
            if (entrada == null) return;
            try {
                int anio = Integer.parseInt(entrada.strip());
                consultarReporte("Clientes del primer trimestre", "Clientes con algún pedido entre el 1 de enero y el 31 de marzo de " + anio + ".",
                    conexion -> ReportesService.clientesTrimestre(conexion, anio));
            } catch (NumberFormatException error) {
                mostrarError(new IllegalArgumentException("Escribe un año válido, por ejemplo 2026."));
            }
        }
    }

    private static final class CatalogoService {
        private static final String BLOQUEO = "sistema_tamales:categoria";

        private static DatosTabla listarCategorias(Connection conexion) throws SQLException {
            String sql = "SELECT c.id_categoria, c.nombre, COUNT(p.id_producto) AS productos "
                + "FROM categoria c LEFT JOIN producto p ON p.id_categoria = c.id_categoria "
                + "GROUP BY c.id_categoria, c.nombre ORDER BY c.nombre, c.id_categoria";
            java.util.List<Object[]> filas = new java.util.ArrayList<>();
            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet datos = consulta.executeQuery()) {
                while (datos.next()) {
                    filas.add(new Object[] {datos.getInt(1), datos.getString(2), datos.getLong(3)});
                }
            }
            return new DatosTabla(new String[] {"ID", "Categoría", "Productos"}, filas);
        }

        private static int crearCategoria(Connection conexion, String nombre) throws SQLException {
            String limpio = validarNombre(nombre);
            return modificar(conexion, () -> {
                comprobarNombreDisponible(conexion, limpio, 0);
                try (PreparedStatement insertar = conexion.prepareStatement(
                        "INSERT INTO categoria (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    insertar.setString(1, limpio);
                    if (insertar.executeUpdate() != 1) {
                        throw new SQLException("No se pudo crear la categoría.");
                    }
                    try (ResultSet clave = insertar.getGeneratedKeys()) {
                        if (!clave.next()) {
                            throw new SQLException("No se recibió el identificador de la categoría.");
                        }
                        return clave.getInt(1);
                    }
                }
            });
        }

        private static void actualizarCategoria(Connection conexion, int id,
                String nombreOriginal, String nombreNuevo) throws SQLException {
            validarId(id);
            String limpio = validarNombre(nombreNuevo);
            modificar(conexion, () -> {
                comprobarVersion(conexion, id, nombreOriginal);
                comprobarNombreDisponible(conexion, limpio, id);
                try (PreparedStatement actualizar = conexion.prepareStatement(
                        "UPDATE categoria SET nombre = ? WHERE id_categoria = ?")) {
                    actualizar.setString(1, limpio);
                    actualizar.setInt(2, id);
                    actualizar.executeUpdate();
                }
                return null;
            });
        }

        private static void eliminarCategoria(Connection conexion, int id,
                String nombreOriginal) throws SQLException {
            validarId(id);
            modificar(conexion, () -> {
                comprobarVersion(conexion, id, nombreOriginal);
                try (PreparedStatement productos = conexion.prepareStatement(
                        "SELECT id_producto FROM producto WHERE id_categoria = ? LIMIT 1")) {
                    productos.setInt(1, id);
                    try (ResultSet datos = productos.executeQuery()) {
                        if (datos.next()) {
                            throw new IllegalArgumentException(
                                "Esta categoría tiene productos. No se puede eliminar.");
                        }
                    }
                }
                try (PreparedStatement eliminar = conexion.prepareStatement(
                        "DELETE FROM categoria WHERE id_categoria = ?")) {
                    eliminar.setInt(1, id);
                    if (eliminar.executeUpdate() != 1) {
                        throw new SQLException("No se pudo eliminar la categoría. Actualiza la lista.");
                    }
                } catch (SQLException error) {
                    if (error.getErrorCode() == 1451) {
                        throw new SQLException("Esta categoría está en uso y no se puede eliminar.",
                            error.getSQLState(), error.getErrorCode(), error);
                    }
                    throw error;
                }
                return null;
            });
        }

        private static String validarNombre(String nombre) {
            String limpio = nombre == null ? "" : nombre.strip();
            int caracteres = limpio.codePointCount(0, limpio.length());
            if (caracteres < 1 || caracteres > 50) {
                throw new IllegalArgumentException("El nombre debe tener entre 1 y 50 caracteres.");
            }
            return limpio;
        }

        private static void validarId(int id) {
            if (id < 1) {
                throw new IllegalArgumentException("Selecciona una categoría válida.");
            }
        }

        private static void comprobarNombreDisponible(Connection conexion, String nombre,
                int idActual) throws SQLException {
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT id_categoria FROM categoria "
                        + "WHERE TRIM(nombre) = ? AND id_categoria <> ? LIMIT 1")) {
                consulta.setString(1, nombre);
                consulta.setInt(2, idActual);
                try (ResultSet datos = consulta.executeQuery()) {
                    if (datos.next()) {
                        throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
                    }
                }
            }
        }

        private static void comprobarVersion(Connection conexion, int id,
                String nombreOriginal) throws SQLException {
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT nombre FROM categoria WHERE id_categoria = ? FOR UPDATE")) {
                consulta.setInt(1, id);
                try (ResultSet datos = consulta.executeQuery()) {
                    if (!datos.next()) {
                        throw new IllegalArgumentException(
                            "La categoría ya no existe. Actualiza la lista.");
                    }
                    if (!java.util.Objects.equals(datos.getString(1), nombreOriginal)) {
                        throw new IllegalArgumentException(
                            "La categoría cambió en otra ventana. Actualiza la lista antes de continuar.");
                    }
                }
            }
        }

        @FunctionalInterface
        private interface Cambio<T> {
            T ejecutar() throws SQLException;
        }

        private static <T> T modificar(Connection conexion, Cambio<T> cambio) throws SQLException {
            if (!conexion.getAutoCommit()) {
                throw new SQLException("La conexión tiene una operación pendiente; no se modificó nada.");
            }
            try (PreparedStatement bloqueo = conexion.prepareStatement("SELECT GET_LOCK(?, 5)")) {
                bloqueo.setString(1, BLOQUEO);
                bloqueo.setQueryTimeout(10);
                try (ResultSet resultado = bloqueo.executeQuery()) {
                    if (!resultado.next() || resultado.getInt(1) != 1) {
                        throw new SQLException("Otra ventana está modificando categorías. Intenta de nuevo.");
                    }
                }
            }

            boolean iniciada = false;
            boolean confirmada = false;
            boolean reutilizable = true;
            Throwable fallo = null;
            try {
                conexion.setAutoCommit(false);
                iniciada = true;
                T resultado = cambio.ejecutar();
                conexion.commit();
                confirmada = true;
                return resultado;
            } catch (SQLException | RuntimeException error) {
                fallo = error;
                if (iniciada && !confirmada) {
                    try {
                        conexion.rollback();
                    } catch (SQLException errorRollback) {
                        reutilizable = false;
                        error.addSuppressed(errorRollback);
                        try {
                            conexion.close();
                        } catch (SQLException errorCierre) {
                            error.addSuppressed(errorCierre);
                        }
                    }
                }
                throw error;
            } finally {
                SQLException errorLimpieza = null;
                try {
                    if (reutilizable && !conexion.isClosed()) {
                        if (iniciada) {
                            try {
                                conexion.setAutoCommit(true);
                            } catch (SQLException error) {
                                errorLimpieza = error;
                            }
                        }
                        try (PreparedStatement liberar = conexion.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                            liberar.setString(1, BLOQUEO);
                            try (ResultSet resultado = liberar.executeQuery()) {
                                if (!resultado.next() || resultado.getInt(1) != 1) {
                                    throw new SQLException("No se pudo liberar el bloqueo de categorías.");
                                }
                            }
                        } catch (SQLException error) {
                            if (errorLimpieza == null) {
                                errorLimpieza = error;
                            } else {
                                errorLimpieza.addSuppressed(error);
                            }
                        }
                    }
                } catch (SQLException error) {
                    errorLimpieza = error;
                }
                if (errorLimpieza != null) {
                    if (fallo != null) {
                        fallo.addSuppressed(errorLimpieza);
                    } else {
                        throw new SQLException(confirmada
                            ? "El cambio se guardó, pero falló la conexión después. Actualiza la lista."
                            : "No se pudo cerrar la operación de categorías.", errorLimpieza);
                    }
                }
            }
        }
    }

    private static final class ReportesService {

        private ReportesService() {
        }

        static DatosTabla productos(Connection conexion) throws SQLException {
            return consultar(conexion, """
                SELECT p.id_producto AS ID, p.nombre AS Producto, c.nombre AS Categoria,
                       p.precio AS Precio, p.precio_mayoreo AS `Precio mayoreo`,
                       p.cantidad_minima_mayoreo AS `Minimo mayoreo`,
                       p.existencia AS Existencia, p.activo AS Activo
                FROM producto p
                INNER JOIN categoria c ON c.id_categoria = p.id_categoria
                ORDER BY p.nombre, p.id_producto
                """);
        }

        static DatosTabla ventasDiarias(Connection conexion, java.time.LocalDate fecha)
                throws SQLException {
            validarFecha(fecha);
            return consultar(conexion, """
                SELECT p.id_pedido AS Pedido, p.fecha AS Fecha,
                       CONCAT_WS(' ', u.nombre, u.apellido) AS Cliente,
                       p.metodo_pago AS `Metodo pago`, p.estado_pago AS `Estado pago`,
                       p.total AS Total
                FROM pedido p
                INNER JOIN usuario u ON u.id_usuario = p.id_usuario
                WHERE p.estado = 'TERMINADO' AND p.fecha >= ? AND p.fecha < ?
                ORDER BY p.fecha, p.id_pedido
                """, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        }

        static DatosTabla ventasPorDia(Connection conexion) throws SQLException {
            return consultar(conexion, """
                SELECT DATE(fecha) AS Fecha, COUNT(*) AS Pedidos, SUM(total) AS Ventas
                FROM pedido
                WHERE estado = 'TERMINADO'
                GROUP BY DATE(fecha)
                ORDER BY Fecha DESC
                """);
        }

        static DatosTabla clientesTrimestre(Connection conexion, int anio) throws SQLException {
            validarAnio(anio);
            java.time.LocalDate inicio = java.time.LocalDate.of(anio, 1, 1);
            java.time.LocalDate fin = java.time.LocalDate.of(anio, 4, 1);
            return consultar(conexion, """
                SELECT u.id_usuario AS ID, CONCAT_WS(' ', u.nombre, u.apellido) AS Cliente,
                       u.correo AS Correo, u.telefono AS Telefono
                FROM usuario u
                WHERE u.rol = 'CLIENTE'
                  AND EXISTS (
                      SELECT 1 FROM pedido p
                      WHERE p.id_usuario = u.id_usuario
                        AND p.fecha >= ? AND p.fecha < ?
                  )
                ORDER BY u.nombre, u.apellido, u.id_usuario
                """, inicio.atStartOfDay(), fin.atStartOfDay());
        }

        static DatosTabla gananciasEstimadas(Connection conexion) throws SQLException {
            // Sin costo completo no inventamos una ganancia.
            return consultar(conexion, """
                SELECT base.id_pedido AS Pedido, base.fecha AS Fecha,
                       base.unidades AS Unidades, base.total AS Venta,
                       ROUND(base.costo_estimado, 2) AS `Costo estimado`,
                       ROUND(base.total - base.costo_estimado, 2) AS `Ganancia estimada`,
                       ROUND(100 * (base.total - base.costo_estimado)
                             / NULLIF(base.costo_estimado, 0), 2) AS `Ganancia sobre costo %`,
                       ROUND(100 * (base.total - base.costo_estimado)
                             / NULLIF(base.total, 0), 2) AS `Margen sobre venta %`,
                       base.observacion AS Observacion
                FROM (
                    SELECT p.id_pedido, p.fecha, p.total, detalle.unidades,
                           CASE
                               WHEN p.total IS NULL OR p.total <= 0
                                    OR detalle.detalles IS NULL
                                    OR detalle.costos_faltantes > 0
                               THEN NULL
                               ELSE detalle.costo_estimado
                           END AS costo_estimado,
                           CASE
                               WHEN p.total IS NULL OR p.total <= 0 THEN 'Total no valido'
                               WHEN detalle.detalles IS NULL THEN 'Pedido sin detalle'
                               WHEN detalle.costos_faltantes > 0
                                   THEN 'Faltan costos validos de produccion'
                               ELSE 'Estimado con costo promedio ponderado'
                           END AS observacion
                    FROM pedido p
                    LEFT JOIN (
                        SELECT dp.id_pedido, COUNT(*) AS detalles,
                               SUM(dp.cantidad) AS unidades,
                               SUM(CASE
                                       WHEN dp.cantidad <= 0 OR costo.costo_unitario IS NULL
                                       THEN 1 ELSE 0
                                   END) AS costos_faltantes,
                               SUM(dp.cantidad * costo.costo_unitario) AS costo_estimado
                        FROM detallepedido dp
                        LEFT JOIN (
                            SELECT id_producto,
                                   CASE
                                       WHEN SUM(CASE
                                                    WHEN cantidad <= 0 OR costo_total <= 0
                                                    THEN 1 ELSE 0
                                                END) > 0
                                       THEN NULL
                                       ELSE SUM(costo_total) / NULLIF(SUM(cantidad), 0)
                                   END AS costo_unitario
                            FROM produccion
                            GROUP BY id_producto
                        ) costo ON costo.id_producto = dp.id_producto
                        GROUP BY dp.id_pedido
                    ) detalle ON detalle.id_pedido = p.id_pedido
                    WHERE p.estado = 'TERMINADO'
                ) base
                ORDER BY base.fecha DESC, base.id_pedido DESC
                """);
        }

        private static void validarFecha(java.time.LocalDate fecha) {
            if (fecha == null) {
                throw new IllegalArgumentException("Selecciona una fecha valida.");
            }
            validarAnio(fecha.getYear());
        }

        private static void validarAnio(int anio) {
            if (anio < 1 || anio > 9998) {
                throw new IllegalArgumentException("El anio debe estar entre 1 y 9998.");
            }
        }

        private static DatosTabla consultar(Connection conexion, String sql, Object... parametros)
                throws SQLException {
            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setQueryTimeout(30);
                for (int i = 0; i < parametros.length; i++) {
                    consulta.setObject(i + 1, parametros[i]);
                }
                try (ResultSet resultado = consulta.executeQuery()) {
                    java.sql.ResultSetMetaData datos = resultado.getMetaData();
                    int cantidadColumnas = datos.getColumnCount();
                    String[] columnas = new String[cantidadColumnas];
                    for (int i = 0; i < cantidadColumnas; i++) {
                        columnas[i] = datos.getColumnLabel(i + 1);
                    }
                    List<Object[]> filas = new ArrayList<>();
                    while (resultado.next()) {
                        Object[] fila = new Object[cantidadColumnas];
                        for (int i = 0; i < cantidadColumnas; i++) {
                            fila[i] = resultado.getObject(i + 1);
                        }
                        filas.add(fila);
                    }
                    return new DatosTabla(columnas, filas);
                }
            }
        }
    }


    private static final class ConexionBD {

        private static Driver controlador;
        private static URLClassLoader cargador;

        private ConexionBD() {
        }

        private static synchronized void cargarControlador(Path carpetaProyecto) throws IOException {
            if (controlador != null) {
                return;
            }
            Path jar = carpetaProyecto.resolve(".vscode/lib/mysql-connector-j-9.4.0.jar");
            if (!Files.isRegularFile(jar)) {
                throw new IOException("Falta el conector de MySQL en .vscode/lib. "
                    + "Copia la carpeta SistemaTamales completa, no solo Main.java.");
            }
            URLClassLoader nuevo = new URLClassLoader(
                new java.net.URL[] {jar.toUri().toURL()}, Main.class.getClassLoader());
            try {
                Driver driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, nuevo)
                    .getDeclaredConstructor().newInstance();
                cargador = nuevo;
                controlador = driver;
            } catch (ReflectiveOperationException | ClassCastException | LinkageError error) {
                IOException fallo = new IOException("No se pudo cargar el conector JDBC incluido en el proyecto.", error);
                try {
                    nuevo.close();
                } catch (IOException cierre) {
                    fallo.addSuppressed(cierre);
                }
                throw fallo;
            }
        }

        public static Connection conectar(ConfiguracionBD configuracion) throws SQLException {
            Connection conexion = abrir(configuracion, true);
            try {
                conexion.setReadOnly(true);
                return conexion;
            } catch (SQLException error) {
                try {
                    conexion.close();
                } catch (SQLException cierre) {
                    error.addSuppressed(cierre);
                }
                throw error;
            }
        }

        public static Connection conectarServidor(ConfiguracionBD configuracion) throws SQLException {
            return abrir(configuracion, false);
        }

        public static Connection conectarEscritura(ConfiguracionBD configuracion) throws SQLException {
            return abrir(configuracion, true);
        }

        private static Connection abrir(ConfiguracionBD configuracion, boolean seleccionarBase) throws SQLException {
            if (configuracion == null) {
                throw new IllegalArgumentException("Falta configurar la conexion a MySQL.");
            }
            Properties propiedades = new Properties();
            propiedades.setProperty("user", configuracion.usuario());
            propiedades.setProperty("password", configuracion.clave());
            propiedades.setProperty("connectTimeout", "5000");
            propiedades.setProperty("socketTimeout", "15000");
            propiedades.setProperty("characterEncoding", "UTF-8");
            propiedades.setProperty("connectionTimeZone", "UTC");
            String url = "jdbc:mysql://" + configuracion.host() + ":" + configuracion.puerto() + "/"
                + (seleccionarBase ? configuracion.base() : "");
            if (controlador == null) {
                throw new SQLException("El conector JDBC todavia no se ha cargado.");
            }
            Connection conexion = controlador.connect(url, propiedades);
            if (conexion == null) {
                throw new SQLException("El conector JDBC no reconoce la direccion de MySQL.");
            }
            return conexion;
        }
    }

    private static final class ConexionAutomatica {
        private static final String BASE = "sistema_tamales";
        private static final String USUARIO = "tamales_app";
        private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        private static final SecureRandom AZAR = new SecureRandom();

        private ConexionAutomatica() {
        }

        public static ConfiguracionBD preparar(Path archivoSql) throws IOException, SQLException {
            if (!Files.isRegularFile(archivoSql)) {
                throw new IOException("No se encuentra el archivo SQL final junto al proyecto.");
            }
            Path raiz = Path.of(System.getProperty("user.home"), ".sistematamales");
            directorioPrivado(raiz);
            Path estado = raiz.resolve("instancia");
            directorioPrivado(estado);
            Path bloqueo = estado.resolve("arranque.lock");
            archivoPrivado(bloqueo);
            try (FileChannel canal = FileChannel.open(bloqueo, StandardOpenOption.WRITE);
                 FileLock lock = bloquear(canal)) {
                if (lock == null) {
                    throw new IOException("Otra ejecucion esta preparando Sistema Tamales. Espera a que termine.");
                }
                return prepararConBloqueo(archivoSql.toAbsolutePath(), estado);
            }
        }

        private static ConfiguracionBD prepararConBloqueo(Path sql, Path estado) throws IOException, SQLException {
            Path datos = estado.resolve("data");
            Path marcador = estado.resolve("instancia.properties");
            rechazarEnlace(datos);
            Properties configuracion = leerEstado(marcador, datos);
            Path mysqld = localizarMySQL();
            if (configuracion == null) {
                configuracion = new Properties();
                configuracion.setProperty("gestionada", "SistemaTamales");
                configuracion.setProperty("formato", "1");
                configuracion.setProperty("puerto", Integer.toString(puertoLibre()));
                configuracion.setProperty("clave.root", claveAleatoria());
                configuracion.setProperty("clave.app", claveAleatoria());
                configuracion.setProperty("estado", "pendiente");
                configuracion.setProperty("root.configurada", "false");
                guardarEstado(marcador, configuracion);
            }
            String fase = configuracion.getProperty("estado");
            if ("pendiente".equals(fase)) {
                if (tieneArchivos(datos)) {
                    throw new IOException("Hay una inicializacion incompleta en la carpeta privada. "
                        + "No se borrara ni reinicializara su contenido; revisa inicializacion.log.");
                }
                directorioPrivado(datos);
                System.out.println("Primera ejecucion: creando una instancia privada de MySQL (puede tardar un minuto)...");
                inicializarMySQL(mysqld, datos, estado);
                configuracion.setProperty("estado", "inicializada");
                guardarEstado(marcador, configuracion);
            } else if (!Files.isDirectory(datos) || !tieneArchivos(datos)) {
                throw new IOException("La instancia privada registrada no tiene sus datos. No se recreara automaticamente.");
            }
            int puerto = Integer.parseInt(configuracion.getProperty("puerto"));
            ProcessHandle servidor = servidorPropio(estado.resolve("mysql.pid"), datos, mysqld);
            if (servidor == null && !puertoDisponible(puerto)) {
                puerto = puertoLibre();
                configuracion.setProperty("puerto", Integer.toString(puerto));
                guardarEstado(marcador, configuracion);
                System.out.println("El puerto anterior esta ocupado; la instancia privada usara otro puerto libre.");
            }
            ConfiguracionBD administrador = new ConfiguracionBD("127.0.0.1", puerto, BASE, "root",
                configuracion.getProperty("clave.root"));
            ConfiguracionBD aplicacion = new ConfiguracionBD("127.0.0.1", puerto, BASE, USUARIO,
                configuracion.getProperty("clave.app"));
            if (servidor == null) {
                System.out.println("Iniciando MySQL privado en 127.0.0.1:" + puerto + "...");
                servidor = iniciarServidor(mysqld, datos, estado, configuracion).toHandle();
            } else {
                System.out.println("Usando la instancia privada que ya esta encendida en el puerto " + puerto + ".");
            }
            try (Connection conexion = esperarConexion(administrador, servidor)) {
                verificarDatos(conexion, datos);
                configuracion.setProperty("root.configurada", "true");
                guardarEstado(marcador, configuracion);
                Path inicio = estado.resolve("inicializar.sql");
                rechazarEnlace(inicio);
                Files.deleteIfExists(inicio);
                InicializadorBD.inicializarInstanciaPrivada(conexion, sql, datos);
                configurarAplicacion(conexion, aplicacion);
                configuracion.setProperty("estado", "lista");
                guardarEstado(marcador, configuracion);
            }
            return aplicacion;
        }

        private static Properties leerEstado(Path marcador, Path datos) throws IOException {
            rechazarEnlace(marcador);
            if (!Files.exists(marcador)) {
                if (tieneArchivos(datos)) {
                    throw new IOException("La carpeta de datos contiene archivos sin un marcador valido. "
                        + "Por seguridad no se usara ni sobrescribira.");
                }
                return null;
            }
            Properties estado = new Properties();
            try (Reader lector = Files.newBufferedReader(marcador, StandardCharsets.UTF_8)) {
                estado.load(lector);
            }
            String fase = estado.getProperty("estado", "");
            if (!"SistemaTamales".equals(estado.getProperty("gestionada"))
                    || !"1".equals(estado.getProperty("formato"))
                    || !Set.of("pendiente", "inicializada", "lista").contains(fase)
                    || !estado.getProperty("clave.root", "").matches("[0-9a-f]{64}")
                    || !estado.getProperty("clave.app", "").matches("[0-9a-f]{64}")
                    || !Set.of("true", "false").contains(estado.getProperty("root.configurada", ""))) {
                throw new IOException("El marcador de la instancia privada no es compatible. No se modificara.");
            }
            try {
                int puerto = Integer.parseInt(estado.getProperty("puerto", ""));
                if (puerto < 1024 || puerto > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException error) {
                throw new IOException("El puerto registrado en la instancia privada no es valido.");
            }
            permisosPrivados(marcador, false);
            return estado;
        }

        private static void inicializarMySQL(Path mysqld, Path datos, Path estado) throws IOException {
            Path log = estado.resolve("inicializacion.log");
            archivoPrivado(log);
            List<String> comando = List.of(mysqld.toString(), "--no-defaults", "--initialize-insecure",
                "--basedir=" + directorioBase(mysqld), "--datadir=" + datos);
            Process proceso = new ProcessBuilder(comando).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
            try {
                if (!proceso.waitFor(120, TimeUnit.SECONDS)) {
                    detenerProcesoNuevo(proceso);
                    throw new IOException("MySQL no termino su inicializacion en dos minutos. "
                        + "Se detuvo solo el proceso nuevo; revisa inicializacion.log antes de reintentar.");
                }
                if (proceso.exitValue() != 0) {
                    throw new IOException("MySQL no pudo crear la instancia privada. Revisa inicializacion.log; "
                        + "no se han borrado los archivos creados.");
                }
            } catch (InterruptedException error) {
                detenerProcesoNuevo(proceso);
                Thread.currentThread().interrupt();
                throw new IOException("Se interrumpio la inicializacion de MySQL.");
            }
        }

        private static Process iniciarServidor(Path mysqld, Path datos, Path estado, Properties configuracion)
                throws IOException {
            Path log = estado.resolve("mysql.log");
            Path consola = estado.resolve("servidor.log");
            archivoPrivado(log);
            archivoPrivado(consola);
            rechazarEnlace(estado.resolve("mysql.pid"));
            List<String> comando = new ArrayList<>(List.of(mysqld.toString(), "--no-defaults",
                "--basedir=" + directorioBase(mysqld), "--datadir=" + datos,
                "--port=" + configuracion.getProperty("puerto"), "--bind-address=127.0.0.1",
                "--mysqlx=OFF", "--skip-log-bin", "--pid-file=" + estado.resolve("mysql.pid"),
                "--log-error=" + log));
            if (!WINDOWS) {
                Path socket = estado.resolve("mysql.sock");
                rechazarEnlace(socket);
                comando.add("--socket=" + socket);
            }
            if (!Boolean.parseBoolean(configuracion.getProperty("root.configurada"))) {
                Path inicio = estado.resolve("inicializar.sql");
                archivoPrivado(inicio);
                Files.writeString(inicio, "ALTER USER 'root'@'localhost' IDENTIFIED BY '"
                    + configuracion.getProperty("clave.root") + "';\n", StandardCharsets.UTF_8);
                comando.add("--init-file=" + inicio);
            }
            return new ProcessBuilder(comando).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(consola.toFile())).start();
        }

        private static ProcessHandle servidorPropio(Path pid, Path datos, Path mysqld) throws IOException {
            rechazarEnlace(pid);
            if (!Files.exists(pid)) {
                return null;
            }
            long numero;
            try {
                numero = Long.parseLong(Files.readString(pid, StandardCharsets.UTF_8).trim());
            } catch (NumberFormatException error) {
                throw new IOException("El archivo PID de la instancia privada no es valido.");
            }
            ProcessHandle proceso = ProcessHandle.of(numero).orElse(null);
            if (proceso == null || !proceso.isAlive()) {
                return null;
            }
            ProcessHandle.Info informacion = proceso.info();
            String ejecutable = informacion.command().orElse("");
            String[] argumentos = informacion.arguments().orElse(new String[0]);
            boolean correcto = !ejecutable.isEmpty()
                && Path.of(ejecutable).toRealPath().equals(mysqld.toRealPath())
                && (argumentos.length == 0 || Arrays.asList(argumentos).contains("--datadir=" + datos));
            if (!correcto) {
                throw new IOException("El PID registrado no identifica la instancia privada esperada. "
                    + "No se adoptara ni detendra ese proceso.");
            }
            return proceso;
        }

        private static Connection esperarConexion(ConfiguracionBD administrador, ProcessHandle servidor)
                throws IOException, SQLException {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            while (System.nanoTime() < limite) {
                if (!servidor.isAlive()) {
                    throw new IOException("El servidor privado se detuvo durante el arranque. Revisa mysql.log y servidor.log.");
                }
                try {
                    return ConexionBD.conectarServidor(administrador);
                } catch (SQLException error) {
                    if (error.getErrorCode() == 1045) {
                        throw new SQLException("La instancia privada rechazo sus credenciales guardadas. "
                            + "No se restableceran automaticamente.", error.getSQLState(), error.getErrorCode());
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Se interrumpio la espera de MySQL.");
                }
            }
            throw new IOException("La instancia privada no estuvo disponible en un minuto. Revisa mysql.log y servidor.log.");
        }

        private static void verificarDatos(Connection conexion, Path esperado) throws SQLException, IOException {
            try (Statement consulta = conexion.createStatement();
                 ResultSet datos = consulta.executeQuery("SELECT @@datadir")) {
                if (!datos.next() || !Path.of(datos.getString(1)).toRealPath().equals(esperado.toRealPath())) {
                    throw new IOException("El servidor no usa la carpeta privada esperada. No se ejecutara ningun cambio.");
                }
            }
        }

        private static void configurarAplicacion(Connection administrador, ConfiguracionBD aplicacion)
                throws SQLException {
            boolean existe;
            try (Statement consulta = administrador.createStatement();
                 ResultSet datos = consulta.executeQuery("SELECT COUNT(*) FROM mysql.user "
                     + "WHERE User='tamales_app' AND Host='localhost'")) {
                datos.next();
                existe = datos.getInt(1) > 0;
            }
            if (existe) {
                try (Connection prueba = ConexionBD.conectar(aplicacion)) {
                    if (!prueba.isValid(5)) {
                        throw new SQLException("La cuenta privada de lectura no responde.");
                    }
                } catch (SQLException error) {
                    throw new SQLException("La cuenta privada existente no coincide con la configuracion guardada. "
                        + "No se cambiara su clave.", error.getSQLState(), error.getErrorCode());
                }
            }
            try (Statement consulta = administrador.createStatement()) {
                if (!existe) {
                    consulta.executeUpdate("CREATE USER 'tamales_app'@'localhost' IDENTIFIED BY '"
                        + aplicacion.clave() + "'");
                }
                consulta.executeUpdate("GRANT SELECT ON sistema_tamales.* TO 'tamales_app'@'localhost'");
                consulta.executeUpdate("GRANT INSERT, UPDATE, DELETE ON sistema_tamales.categoria TO 'tamales_app'@'localhost'");
                consulta.executeUpdate("GRANT EXECUTE ON FUNCTION sistema_tamales.fn_validar_stock_producto "
                    + "TO 'tamales_app'@'localhost'");
                consulta.executeUpdate("GRANT EXECUTE ON FUNCTION sistema_tamales.fn_obtener_precio_vigente "
                    + "TO 'tamales_app'@'localhost'");
            } catch (SQLException error) {
                throw new SQLException("No se pudo preparar la cuenta privada de la aplicación.",
                    error.getSQLState(), error.getErrorCode());
            }
            try (Connection prueba = ConexionBD.conectar(aplicacion)) {
                if (!prueba.isValid(5)) {
                    throw new SQLException("No se pudo validar la cuenta privada de lectura.");
                }
            }
        }

        private static Path localizarMySQL() throws IOException {
            String indicado = System.getenv("SISTEMA_TAMALES_MYSQLD");
            if (indicado != null && !indicado.isBlank()) {
                Path ruta = Path.of(indicado).toAbsolutePath();
                if (esMySQLCompatible(ruta)) {
                    return ruta.toRealPath();
                }
                throw new IOException("SISTEMA_TAMALES_MYSQLD no apunta a un servidor MySQL 8.0 u 8.4 compatible.");
            }
            List<Path> candidatos = new ArrayList<>();
            String entorno = System.getenv("PATH");
            if (entorno != null) {
                for (String carpeta : entorno.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                    if (!carpeta.isBlank()) {
                        candidatos.add(Path.of(carpeta, WINDOWS ? "mysqld.exe" : "mysqld"));
                    }
                }
            }
            for (String programa : List.of("ProgramFiles", "ProgramFiles(x86)")) {
                String carpeta = System.getenv(programa);
                if (carpeta != null) {
                    candidatos.add(Path.of(carpeta, "MySQL", "MySQL Server 8.4", "bin", "mysqld.exe"));
                    candidatos.add(Path.of(carpeta, "MySQL", "MySQL Server 8.0", "bin", "mysqld.exe"));
                }
            }
            if (!WINDOWS) {
                for (String ruta : List.of("/usr/local/mysql/bin/mysqld", "/opt/homebrew/opt/mysql@8.4/bin/mysqld",
                        "/opt/homebrew/opt/mysql/bin/mysqld", "/usr/local/opt/mysql@8.4/bin/mysqld",
                        "/usr/local/opt/mysql/bin/mysqld", "/usr/sbin/mysqld", "/usr/bin/mysqld", "/usr/libexec/mysqld")) {
                    candidatos.add(Path.of(ruta));
                }
            }
            for (Path candidato : candidatos) {
                if (esMySQLCompatible(candidato)) {
                    return candidato.toRealPath();
                }
            }
            throw new IOException("No se encontro MySQL Server 8.0 u 8.4 instalado. Instala MySQL Server y vuelve a pulsar Run. "
                + "Workbench por si solo no incluye el servidor. No se descargara ni modificara otro servicio automaticamente.");
        }

        private static boolean esMySQLCompatible(Path archivo) throws IOException {
            if (!Files.isRegularFile(archivo) || !Files.isExecutable(archivo)) {
                return false;
            }
            Process proceso;
            try {
                proceso = new ProcessBuilder(archivo.toString(), "--no-defaults", "--version")
                    .redirectErrorStream(true).start();
            } catch (IOException error) {
                return false;
            }
            try {
                if (!proceso.waitFor(10, TimeUnit.SECONDS)) {
                    detenerProcesoNuevo(proceso);
                    return false;
                }
                String version = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return proceso.exitValue() == 0 && !version.toLowerCase(Locale.ROOT).contains("mariadb")
                    && version.toLowerCase(Locale.ROOT).contains("mysql")
                    && java.util.regex.Pattern.compile("\\b8\\.(?:0|4)\\.\\d+\\b").matcher(version).find();
            } catch (InterruptedException error) {
                detenerProcesoNuevo(proceso);
                Thread.currentThread().interrupt();
                throw new IOException("Se interrumpio la comprobacion de MySQL.");
            }
        }

        private static Path directorioBase(Path mysqld) {
            return mysqld.getParent().getParent();
        }

        private static int puertoLibre() throws IOException {
            try (ServerSocket socket = new ServerSocket()) {
                socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
                return socket.getLocalPort();
            }
        }

        private static boolean puertoDisponible(int puerto) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.setReuseAddress(false);
                socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), puerto));
                return true;
            } catch (IOException error) {
                return false;
            }
        }

        private static String claveAleatoria() {
            byte[] bytes = new byte[32];
            AZAR.nextBytes(bytes);
            return HexFormat.of().formatHex(bytes);
        }

        private static boolean tieneArchivos(Path directorio) throws IOException {
            if (!Files.exists(directorio)) {
                return false;
            }
            if (!Files.isDirectory(directorio, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("La ruta privada de datos no es una carpeta.");
            }
            try (var archivos = Files.list(directorio)) {
                return archivos.findAny().isPresent();
            }
        }

        private static void directorioPrivado(Path directorio) throws IOException {
            rechazarEnlace(directorio);
            if (Files.getFileAttributeView(directorio.getParent(), PosixFileAttributeView.class) != null) {
                Files.createDirectories(directorio,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            } else {
                Files.createDirectories(directorio);
            }
            permisosPrivados(directorio, true);
        }

        private static void archivoPrivado(Path archivo) throws IOException {
            rechazarEnlace(archivo);
            if (!Files.exists(archivo)) {
                if (Files.getFileAttributeView(archivo.getParent(), PosixFileAttributeView.class) != null) {
                    Files.createFile(archivo,
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                } else {
                    Files.createFile(archivo);
                }
            }
            permisosPrivados(archivo, false);
        }

        private static void permisosPrivados(Path archivo, boolean directorio) throws IOException {
            if (Files.getFileAttributeView(archivo, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) != null) {
                Files.setPosixFilePermissions(archivo,
                    PosixFilePermissions.fromString(directorio ? "rwx------" : "rw-------"));
            }
        }

        private static void rechazarEnlace(Path ruta) throws IOException {
            if (Files.isSymbolicLink(ruta)) {
                throw new IOException("Una ruta de la instancia privada es un enlace. No se utilizara.");
            }
        }

        private static void guardarEstado(Path archivo, Properties estado) throws IOException {
            rechazarEnlace(archivo);
            Path temporal = Files.createTempFile(archivo.getParent(), ".instancia-", ".tmp");
            permisosPrivados(temporal, false);
            try {
                try (Writer salida = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                    estado.store(salida, "Instancia privada de SistemaTamales. No compartir.");
                }
                try {
                    Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException error) {
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
                }
                permisosPrivados(archivo, false);
            } finally {
                Files.deleteIfExists(temporal);
            }
        }

        private static FileLock bloquear(FileChannel canal) throws IOException {
            try {
                return canal.tryLock();
            } catch (OverlappingFileLockException error) {
                return null;
            }
        }

        private static void detenerProcesoNuevo(Process proceso) {
            proceso.destroy();
            try {
                if (!proceso.waitFor(3, TimeUnit.SECONDS)) {
                    proceso.destroyForcibly();
                }
            } catch (InterruptedException error) {
                proceso.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class InicializadorBD {

        private static final String BASE = "sistema_tamales";
        private static final Set<String> TABLAS = Set.of(
            "CATEGORIA", "USUARIO", "DIRECCIONUSUARIO", "PEDIDO", "PRODUCTO",
            "DETALLEPEDIDO", "INGREDIENTE", "DETALLERECETA", "PUESTO", "EMPLEADO",
            "EMPRESA", "MOVIMIENTOMATERIAPRIMA", "MOVIMIENTOPRODUCTO", "PRODUCCION");
        private static final Set<String> PROCEDIMIENTOS = Set.of(
            "CALCULO_VENTAS_DIARIAS", "CALCULO_VENTAS_DIARIAS_TOTAL",
            "REPORTE_CLIENTES_PRIMER_TRIMESTRE", "INSERTAR_CLIENTE_EXCEPCION",
            "SP_CREAR_PEDIDO", "SP_AGREGAR_DETALLE_PEDIDO");
        private static final Set<String> FUNCIONES = Set.of(
            "FN_VALIDAR_STOCK_PRODUCTO", "FN_OBTENER_PRECIO_VIGENTE");
        private static final Set<String> ALIAS = Set.of(
            "P", "DP", "U", "E", "I", "MP", "MMP", "PRD", "PD", "P2", "DP2", "PR2", "C", "PR");
        private static final Set<String> PROHIBIDOS = Set.of(
            "DROP", "ALTER", "TRUNCATE", "RENAME", "DELETE", "REPLACE",
            "GRANT", "REVOKE", "PRIVILEGES", "GLOBAL", "PERSIST", "PERSIST_ONLY",
            "LOAD", "SOURCE", "OUTFILE", "DUMPFILE", "LOAD_FILE", "DEFINER",
            "PREPARE", "EXECUTE", "DEALLOCATE", "CALL", "DO", "INSTALL", "UNINSTALL",
            "SHUTDOWN", "RESTART", "RESET", "FLUSH", "KILL", "LOCK", "UNLOCK",
            "TRIGGER", "EVENT", "DIRECTORY", "TABLESPACE", "SLEEP", "BENCHMARK", "GET_LOCK", "SYS_EXEC", "SYS_EVAL");

        private InicializadorBD() {
        }

        public static void inicializarInstanciaPrivada(Connection admin, Path sqlFile,
                                                      Path datadirEsperado)
                throws SQLException, IOException {
            verificarInstanciaPrivada(admin, datadirEsperado);
            if (existeBase(admin)) {
                verificarObjetos(admin);
                System.out.println("Base existente verificada: 14 tablas, 6 procedimientos y 2 funciones. Sin cambios.");
                return;
            }

            List<String> sentencias;
            try {
                sentencias = validarScript(Files.readString(sqlFile, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException error) {
                throw new IOException("El SQL no paso la validacion de instalacion. No se modifico la base. "
                    + error.getMessage());
            }

            boolean reservada = false;
            int numeroSentencia = 0;
            try {
                ejecutarSinMostrarResultados(admin,
                    "CREATE SCHEMA `sistema_tamales` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                reservada = true;
                admin.setCatalog(BASE);
                for (String sentencia : sentencias) {
                    numeroSentencia++;
                    ejecutarSinMostrarResultados(admin, sentencia);
                }
                verificarObjetos(admin);
                System.out.println("Base privada preparada: 14 tablas, 6 procedimientos y 2 funciones.");
            } catch (SQLException error) {
                String mensaje;
                if (!reservada) {
                    mensaje = "No se pudo reservar una base nueva. No se ejecuto el SQL de instalacion "
                        + "ni se modifico una base existente.";
                } else {
                    mensaje = "La importacion de la base privada quedo incompleta en la sentencia "
                        + numeroSentencia + ". No se borro la base parcial y no se reintentara automaticamente. "
                        + "No se modifico ninguna base preexistente.";
                }
                if (error.getErrorCode() == 1419) {
                    mensaje += " El servidor requiere autorizacion administrativa adicional para crear funciones; "
                        + "no se cambiaron privilegios ni opciones globales.";
                }
                throw new SQLException(mensaje, error.getSQLState(), error.getErrorCode());
            }
        }

        private static void verificarInstanciaPrivada(Connection admin, Path esperado)
                throws SQLException, IOException {
            Path directorio;
            Path marcador;
            try {
                directorio = esperado.toRealPath();
                Path padre = esperado.toAbsolutePath().normalize().getParent();
                if (padre == null || !Files.isDirectory(directorio)) {
                    throw new IOException();
                }
                marcador = padre.resolve("instancia.properties");
                if (!Files.isRegularFile(marcador)
                        || !marcador.toRealPath().getParent().equals(padre.toRealPath())) {
                    throw new IOException();
                }
            } catch (IOException error) {
                throw new IOException("No se pudo comprobar el directorio y marcador de la instancia privada. Sin cambios.");
            }

            Properties marca = new Properties();
            try (Reader lector = Files.newBufferedReader(marcador, StandardCharsets.UTF_8)) {
                marca.load(lector);
            }
            if (!"SistemaTamales".equals(marca.getProperty("gestionada"))
                    || !"1".equals(marca.getProperty("formato"))) {
                throw new IOException("El marcador no identifica una instancia privada de SistemaTamales. Sin cambios.");
            }

            try (Statement consulta = admin.createStatement();
                    ResultSet resultado = consulta.executeQuery("SELECT @@datadir")) {
                if (!resultado.next()) {
                    throw new SQLException("No se pudo verificar la instancia privada. Sin cambios.", "HY000");
                }
                Path realServidor;
                try {
                    realServidor = Path.of(resultado.getString(1)).toRealPath();
                } catch (IOException | RuntimeException error) {
                    throw new IOException("El directorio del servidor no se puede verificar localmente. Sin cambios.");
                }
                if (!directorio.equals(realServidor)) {
                    throw new SQLException("La conexion no pertenece a la instancia privada esperada. Sin cambios.", "HY000");
                }
            }
        }

        private static boolean existeBase(Connection admin) throws SQLException {
            try (PreparedStatement consulta = admin.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?")) {
                consulta.setString(1, BASE);
                try (ResultSet resultado = consulta.executeQuery()) {
                    resultado.next();
                    return resultado.getInt(1) != 0;
                }
            }
        }

        private static void verificarObjetos(Connection admin) throws SQLException {
            Set<String> tablas = new HashSet<>();
            Set<String> procedimientos = new HashSet<>();
            Set<String> funciones = new HashSet<>();
            try (PreparedStatement consulta = admin.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'")) {
                consulta.setString(1, BASE);
                try (ResultSet resultado = consulta.executeQuery()) {
                    while (resultado.next()) {
                        tablas.add(resultado.getString(1).toUpperCase(Locale.ROOT));
                    }
                }
            }
            try (PreparedStatement consulta = admin.prepareStatement(
                    "SELECT routine_name, routine_type FROM information_schema.routines WHERE routine_schema = ?")) {
                consulta.setString(1, BASE);
                try (ResultSet resultado = consulta.executeQuery()) {
                    while (resultado.next()) {
                        String nombre = resultado.getString(1).toUpperCase(Locale.ROOT);
                        if ("PROCEDURE".equals(resultado.getString(2))) {
                            procedimientos.add(nombre);
                        } else if ("FUNCTION".equals(resultado.getString(2))) {
                            funciones.add(nombre);
                        }
                    }
                }
            }
            if (!TABLAS.equals(tablas) || !PROCEDIMIENTOS.equals(procedimientos) || !FUNCIONES.equals(funciones)) {
                throw new SQLException("La base privada existe, pero sus objetos no coinciden con el SQL final completo. "
                    + "No se reparara ni se importaran datos sobre esa base automaticamente.", "HY000");
            }
        }

        private static void ejecutarSinMostrarResultados(Connection conexion, String sql) throws SQLException {
            try (Statement sentencia = conexion.createStatement()) {
                boolean resultado = sentencia.execute(sql);
                while (true) {
                    if (resultado) {
                        try (ResultSet datos = sentencia.getResultSet()) {
                            // Estos resultados son de comprobacion, no se muestran en la consola.
                        }
                    } else if (sentencia.getUpdateCount() == -1) {
                        break;
                    }
                    resultado = sentencia.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
                }
            }
        }

        private static List<String> validarScript(String sql) {
            List<String> permitidas = new ArrayList<>();
            Set<String> tablas = new HashSet<>();
            Set<String> procedimientos = new HashSet<>();
            Set<String> funciones = new HashSet<>();
            int esquemas = 0;
            int usos = 0;
            int numero = 0;
            for (String sentencia : separarSentencias(sql)) {
                numero++;
                List<String> tokens = tokensSinLiterales(sentencia);
                String compacta = sentencia.replaceAll("\\s+", "").replace("`", "").toUpperCase(Locale.ROOT);
                if (compacta.equals("DROPSCHEMAIFEXISTSSISTEMA_TAMALES")) {
                    continue;
                }
                if (compacta.equals("CREATESCHEMASISTEMA_TAMALESDEFAULTCHARACTERSETUTF8MB4COLLATEUTF8MB4_UNICODE_CI")) {
                    esquemas++;
                    continue;
                }
                if (tokens.isEmpty() || tokens.stream().anyMatch(PROHIBIDOS::contains)) {
                    throw sentenciaNoPermitida(numero);
                }
                comprobarClausulasFrom(tokens, numero);
                String tipo = tokens.get(0);
                if (tipo.equals("SET")) {
                    if (!setPermitido(compacta)) {
                        throw sentenciaNoPermitida(numero);
                    }
                } else if (tipo.equals("USE")) {
                    if (!tokens.equals(List.of("USE", "SISTEMA_TAMALES"))) {
                        throw sentenciaNoPermitida(numero);
                    }
                    usos++;
                } else if (tipo.equals("CREATE") && tokens.size() > 2) {
                    if (tokens.stream().filter("CREATE"::equals).count() != 1) {
                        throw sentenciaNoPermitida(numero);
                    }
                    String objeto = tokens.get(1);
                    int posicion = 2;
                    if (objeto.equals("TABLE") && tokens.size() > 5
                            && tokens.subList(2, 5).equals(List.of("IF", "NOT", "EXISTS"))) {
                        posicion = 5;
                    }
                    String nombre = nombreObjeto(tokens, posicion, numero);
                    boolean valido = switch (objeto) {
                        case "TABLE" -> TABLAS.contains(nombre) && tablas.add(nombre);
                        case "PROCEDURE" -> PROCEDIMIENTOS.contains(nombre) && procedimientos.add(nombre);
                        case "FUNCTION" -> FUNCIONES.contains(nombre) && funciones.add(nombre)
                            && !tokens.contains("INSERT") && !tokens.contains("UPDATE");
                        default -> false;
                    };
                    if (!valido) {
                        throw sentenciaNoPermitida(numero);
                    }
                } else if (!tipo.equals("INSERT") && !tipo.equals("SELECT")) {
                    throw sentenciaNoPermitida(numero);
                }

                for (int i = 0; i < tokens.size(); i++) {
                    String token = tokens.get(i);
                    if (token.equals(".") && i > 0) {
                        String prefijo = tokens.get(i - 1);
                        if (!prefijo.matches("[0-9]+") && !prefijo.equals("SISTEMA_TAMALES")
                                && !TABLAS.contains(prefijo) && !ALIAS.contains(prefijo)) {
                            throw sentenciaNoPermitida(numero);
                        }
                    }
                    if (token.equals("INSERT")) {
                        if (i + 1 >= tokens.size() || !tokens.get(i + 1).equals("INTO")) {
                            throw sentenciaNoPermitida(numero);
                        }
                        comprobarTabla(tokens, i + 2, numero);
                    } else if (token.equals("UPDATE")) {
                        comprobarTabla(tokens, i + 1, numero);
                        int siguiente = i + 2;
                        if (siguiente < tokens.size() && tokens.get(siguiente).equals(".")) {
                            siguiente += 2;
                        }
                        if (siguiente >= tokens.size() || !tokens.get(siguiente).equals("SET")) {
                            throw sentenciaNoPermitida(numero);
                        }
                    } else if (token.equals("REFERENCES")) {
                        comprobarTabla(tokens, i + 1, numero);
                    } else if (token.equals("FROM") || token.equals("JOIN")) {
                        if (i + 1 >= tokens.size()) {
                            throw sentenciaNoPermitida(numero);
                        }
                        if (!tokens.get(i + 1).equals("(")) {
                            comprobarTabla(tokens, i + 1, numero);
                        }
                    } else if (token.equals("ENGINE")) {
                        if (i + 2 >= tokens.size() || !tokens.get(i + 1).equals("=")
                                || !tokens.get(i + 2).equals("INNODB")) {
                            throw sentenciaNoPermitida(numero);
                        }
                    }
                }
                permitidas.add(sentencia);
            }
            if (esquemas != 1 || usos != 1 || !TABLAS.equals(tablas)
                    || !PROCEDIMIENTOS.equals(procedimientos) || !FUNCIONES.equals(funciones)) {
                throw new IllegalArgumentException("El archivo no contiene el conjunto esperado de esquema, tablas y rutinas.");
            }
            return List.copyOf(permitidas);
        }

        private static boolean setPermitido(String sentencia) {
            return sentencia.equals("SET@OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS,UNIQUE_CHECKS=0")
                || sentencia.equals("SET@OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS,FOREIGN_KEY_CHECKS=0")
                || sentencia.equals("SET@OLD_SQL_MODE=@@SQL_MODE,SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'")
                || sentencia.equals("SETSQL_MODE=@OLD_SQL_MODE")
                || sentencia.equals("SETFOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS")
                || sentencia.equals("SETUNIQUE_CHECKS=@OLD_UNIQUE_CHECKS");
        }

        private static void comprobarClausulasFrom(List<String> tokens, int numero) {
            Set<Integer> nivelesFrom = new HashSet<>();
            int nivel = 0;
            for (String token : tokens) {
                if (token.equals("(")) {
                    nivel++;
                } else if (token.equals(")")) {
                    nivelesFrom.remove(nivel--);
                    if (nivel < 0) {
                        throw sentenciaNoPermitida(numero);
                    }
                } else if (token.equals("FROM")) {
                    nivelesFrom.add(nivel);
                } else if (Set.of("WHERE", "GROUP", "ORDER", "HAVING", "UNION", "LIMIT", "FOR", "END", ";")
                        .contains(token)) {
                    nivelesFrom.remove(nivel);
                } else if (token.equals(",") && nivelesFrom.contains(nivel)) {
                    throw sentenciaNoPermitida(numero);
                }
            }
            if (nivel != 0) {
                throw sentenciaNoPermitida(numero);
            }
        }

        private static void comprobarTabla(List<String> tokens, int posicion, int numero) {
            if (!TABLAS.contains(nombreObjeto(tokens, posicion, numero))) {
                throw sentenciaNoPermitida(numero);
            }
        }

        private static String nombreObjeto(List<String> tokens, int posicion, int numero) {
            if (posicion >= tokens.size()) {
                throw sentenciaNoPermitida(numero);
            }
            String nombre = tokens.get(posicion);
            if (posicion + 1 < tokens.size() && tokens.get(posicion + 1).equals(".")) {
                if (!nombre.equals("SISTEMA_TAMALES") || posicion + 2 >= tokens.size()) {
                    throw sentenciaNoPermitida(numero);
                }
                nombre = tokens.get(posicion + 2);
            }
            return nombre;
        }

        private static List<String> tokensSinLiterales(String sentencia) {
            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < sentencia.length();) {
                char c = sentencia.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                } else if (c == '\'' || c == '"' || c == '`') {
                    char comilla = c;
                    StringBuilder contenido = new StringBuilder();
                    i++;
                    while (i < sentencia.length()) {
                        c = sentencia.charAt(i++);
                        if (c == '\\' && i < sentencia.length()) {
                            contenido.append(sentencia.charAt(i++));
                        } else if (c == comilla) {
                            if (i < sentencia.length() && sentencia.charAt(i) == comilla) {
                                contenido.append(sentencia.charAt(i++));
                            } else {
                                break;
                            }
                        } else {
                            contenido.append(c);
                        }
                    }
                    if (comilla == '`') {
                        tokens.add(contenido.toString().toUpperCase(Locale.ROOT));
                    } else {
                        tokens.add("<LITERAL>");
                    }
                } else if (Character.isLetterOrDigit(c) || c == '_') {
                    int inicio = i++;
                    while (i < sentencia.length()
                            && (Character.isLetterOrDigit(sentencia.charAt(i)) || sentencia.charAt(i) == '_')) {
                        i++;
                    }
                    tokens.add(sentencia.substring(inicio, i).toUpperCase(Locale.ROOT));
                } else {
                    tokens.add(String.valueOf(c));
                    i++;
                }
            }
            return tokens;
        }

        private static IllegalArgumentException sentenciaNoPermitida(int numero) {
            return new IllegalArgumentException("La sentencia " + numero + " no pertenece al conjunto permitido para instalar esta base.");
        }

        public static List<String> separarSentencias(String sql) {
            if (sql == null) {
                throw new IllegalArgumentException("El contenido SQL no puede ser nulo.");
            }

            List<String> sentencias = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            String delimitador = ";";
            int posicion = sql.startsWith("\uFEFF") ? 1 : 0;
            int linea = 1;
            boolean inicioDeLinea = true;

            while (posicion < sql.length()) {
                char caracter = sql.charAt(posicion);

                if (caracter == '\n') {
                    actual.append(caracter);
                    posicion++;
                    linea++;
                    inicioDeLinea = true;
                    continue;
                }
                if (Character.isWhitespace(caracter)) {
                    actual.append(caracter);
                    posicion++;
                    continue;
                }

                if (caracter == '#' || esComentarioDeLinea(sql, posicion)) {
                    while (posicion < sql.length() && sql.charAt(posicion) != '\n') {
                        posicion++;
                    }
                    actual.append(' ');
                    continue;
                }

                if (sql.startsWith("/*", posicion)) {
                    if (sql.startsWith("/*!", posicion) || sql.startsWith("/*+", posicion)) {
                        throw error(linea, "Los comentarios ejecutables no se admiten en este importador.");
                    }
                    int lineaInicial = linea;
                    posicion += 2;
                    while (posicion < sql.length() && !sql.startsWith("*/", posicion)) {
                        if (sql.charAt(posicion) == '\n') {
                            linea++;
                            inicioDeLinea = true;
                        }
                        posicion++;
                    }
                    if (posicion == sql.length()) {
                        throw error(lineaInicial, "Hay un comentario de bloque sin cerrar.");
                    }
                    posicion += 2;
                    actual.append(' ');
                    continue;
                }

                if (inicioDeLinea && esInstruccionDelimiter(sql, posicion)) {
                    if (!actual.toString().isBlank()) {
                        throw error(linea, "Falta terminar la sentencia anterior a DELIMITER.");
                    }
                    int fin = sql.indexOf('\n', posicion);
                    if (fin < 0) {
                        fin = sql.length();
                    }
                    String[] partes = sql.substring(posicion, fin).trim().split("\\s+");
                    if (partes.length != 2 || !delimitadorValido(partes[1])) {
                        throw error(linea, "La instruccion DELIMITER no tiene un separador valido.");
                    }
                    delimitador = partes[1];
                    actual.setLength(0);
                    posicion = fin;
                    continue;
                }

                if (sql.startsWith(delimitador, posicion)) {
                    agregarSentencia(sentencias, actual);
                    posicion += delimitador.length();
                    inicioDeLinea = false;
                    continue;
                }

                inicioDeLinea = false;
                if (caracter == '\'' || caracter == '"' || caracter == '`') {
                    char comilla = caracter;
                    int lineaInicial = linea;
                    boolean cerrada = false;
                    actual.append(caracter);
                    posicion++;
                    while (posicion < sql.length()) {
                        caracter = sql.charAt(posicion++);
                        actual.append(caracter);
                        if (caracter == '\n') {
                            linea++;
                        }
                        if (caracter == '\\') {
                            if (posicion == sql.length()) {
                                break;
                            }
                            char escapado = sql.charAt(posicion++);
                            actual.append(escapado);
                            if (escapado == '\n') {
                                linea++;
                            }
                        } else if (caracter == comilla) {
                            if (posicion < sql.length() && sql.charAt(posicion) == comilla) {
                                actual.append(sql.charAt(posicion++));
                            } else {
                                cerrada = true;
                                break;
                            }
                        }
                    }
                    if (!cerrada) {
                        throw error(lineaInicial, "Hay una cadena o un identificador sin cerrar.");
                    }
                    continue;
                }

                actual.append(caracter);
                posicion++;
            }

            agregarSentencia(sentencias, actual);
            return List.copyOf(sentencias);
        }

        private static boolean esComentarioDeLinea(String sql, int posicion) {
            if (!sql.startsWith("--", posicion)) {
                return false;
            }
            int siguiente = posicion + 2;
            return siguiente == sql.length()
                || Character.isWhitespace(sql.charAt(siguiente))
                || Character.isISOControl(sql.charAt(siguiente));
        }

        private static boolean esInstruccionDelimiter(String sql, int posicion) {
            String palabra = "DELIMITER";
            int fin = posicion + palabra.length();
            return fin <= sql.length()
                && sql.substring(posicion, fin).toUpperCase(Locale.ROOT).equals(palabra)
                && (fin == sql.length() || Character.isWhitespace(sql.charAt(fin)));
        }

        private static boolean delimitadorValido(String valor) {
            return !valor.isEmpty() && valor.length() <= 16
                && valor.chars().noneMatch(c -> Character.isLetterOrDigit(c)
                    || Character.isWhitespace(c) || c == '\'' || c == '"'
                    || c == '`' || c == '\\' || c == '#')
                && !valor.contains("/*") && !valor.contains("*/") && !valor.contains("--");
        }

        private static void agregarSentencia(List<String> sentencias, StringBuilder actual) {
            String sentencia = actual.toString().trim();
            if (!sentencia.isEmpty()) {
                sentencias.add(sentencia);
            }
            actual.setLength(0);
        }

        private static IllegalArgumentException error(int linea, String mensaje) {
            return new IllegalArgumentException("SQL, linea " + linea + ": " + mensaje);
        }
    }
}
