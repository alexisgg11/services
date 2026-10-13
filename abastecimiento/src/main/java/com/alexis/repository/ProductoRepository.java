package com.alexis.repository;

import com.alexis.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

/** SQL fijo; todos los valores externos se enlazan con PreparedStatement. */
@org.springframework.stereotype.Repository
public class ProductoRepository {
    private final DataSource dataSource;
    private final int queryTimeout;
    private static final String FROM = """
        FROM panaderia.producto p
        JOIN panaderia.tipo_producto t USING (id_tipo)
        JOIN panaderia.proveedor v USING (id_proveedor)
        JOIN panaderia.unidad_medida u USING (id_unidad)
        """;
    private static final String SELECT = """
        SELECT p.id_producto, p.descripcion, p.precio, p.id_tipo, p.id_proveedor, p.id_unidad,
               t.descripcion AS tipo, v.nombre AS proveedor, u.descripcion AS unidad, u.simbolo
        """ + FROM;
    private static final String FILTROS = """
        WHERE strpos(lower(p.descripcion), lower(?)) > 0
        AND (?::integer IS NULL OR p.id_proveedor = ?)
        AND (?::integer IS NULL OR p.id_tipo = ?)
        """;

    public ProductoRepository(DataSource dataSource, int queryTimeout) {
        this.dataSource = dataSource;
        this.queryTimeout = queryTimeout;
    }
    @org.springframework.beans.factory.annotation.Autowired
    public ProductoRepository(DataSource dataSource, com.alexis.config.AppConfig config) {
        this(dataSource, config.queryTimeoutSeconds());
    }

    private PreparedStatement preparar(Connection cn, String sql) throws SQLException {
        var ps = cn.prepareStatement(sql);
        ps.setQueryTimeout(queryTimeout);
        return ps;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("id_producto"), rs.getString("descripcion"), rs.getBigDecimal("precio"),
            rs.getInt("id_tipo"), rs.getString("tipo"), rs.getInt("id_proveedor"), rs.getString("proveedor"),
            rs.getInt("id_unidad"), rs.getString("unidad"), rs.getString("simbolo"));
    }

    private void filtros(PreparedStatement ps, String buscar, Integer proveedor, Integer tipo) throws SQLException {
        ps.setString(1, buscar);
        ps.setObject(2, proveedor, Types.INTEGER); ps.setObject(3, proveedor, Types.INTEGER);
        ps.setObject(4, tipo, Types.INTEGER); ps.setObject(5, tipo, Types.INTEGER);
    }

    public PaginaProductos listar(String buscar, Integer proveedor, Integer tipo, int pagina, int tamano) throws SQLException {
        try (var cn = dataSource.getConnection()) {
            cn.setReadOnly(true);
            cn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            cn.setAutoCommit(false);
            try {
                long total;
                try (var ps = preparar(cn, "SELECT count(*) " + FROM + FILTROS)) {
                    filtros(ps, buscar, proveedor, tipo);
                    try (var rs = ps.executeQuery()) { rs.next(); total = rs.getLong(1); }
                }
                int paginas = (int)Math.max(1, (total + tamano - 1) / tamano);
                int actual = Math.min(pagina, paginas);
                var items = new ArrayList<Producto>();
                try (var ps = preparar(cn, SELECT + FILTROS + " ORDER BY p.id_producto LIMIT ? OFFSET ?")) {
                    filtros(ps, buscar, proveedor, tipo);
                    ps.setInt(6, tamano); ps.setLong(7, (long)(actual - 1) * tamano);
                    try (var rs = ps.executeQuery()) { while (rs.next()) items.add(mapear(rs)); }
                }
                cn.commit();
                return new PaginaProductos(items, total, actual, tamano, paginas);
            } catch (SQLException | RuntimeException error) {
                cn.rollback();
                throw error;
            }
        }
    }

    public Optional<Producto> obtener(int id) throws SQLException {
        try (var cn = dataSource.getConnection(); var ps = preparar(cn, SELECT + " WHERE p.id_producto = ?")) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    private Producto masCaro(Connection cn) throws SQLException {
        try (var ps = preparar(cn, SELECT + " ORDER BY p.precio DESC, p.id_producto ASC LIMIT 1");
             var rs = ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
    }

    public Optional<Producto> masCaro() throws SQLException {
        try (var cn = dataSource.getConnection()) { return Optional.ofNullable(masCaro(cn)); }
    }

    public Resumen resumen() throws SQLException {
        try (var cn = dataSource.getConnection()) {
            cn.setReadOnly(true);
            cn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            cn.setAutoCommit(false);
            try {
                var porProveedor = new LinkedHashMap<Integer, Long>();
                long total = 0;
                try (var ps = preparar(cn, "SELECT id_proveedor, count(*) AS cantidad FROM panaderia.producto GROUP BY id_proveedor ORDER BY id_proveedor LIMIT 101");
                     var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long cantidad = rs.getLong("cantidad");
                        total += cantidad;
                        porProveedor.put(rs.getInt("id_proveedor"), cantidad);
                    }
                }
                if (porProveedor.size() > 100) throw new SQLException("El catálogo supera el límite permitido.", "54000");
                Producto masCaro = masCaro(cn);
                cn.commit();
                return new Resumen(total, masCaro, porProveedor);
            } catch (SQLException | RuntimeException error) {
                cn.rollback(); throw error;
            }
        }
    }

    public int guardar(Integer id, String descripcion, BigDecimal precio, int tipo, int proveedor, int unidad) throws SQLException {
        String sql = id == null
            ? "INSERT INTO panaderia.producto (descripcion,precio,id_tipo,id_proveedor,id_unidad) VALUES (?,?,?,?,?) RETURNING id_producto"
            : "UPDATE panaderia.producto SET descripcion=?,precio=?,id_tipo=?,id_proveedor=?,id_unidad=? WHERE id_producto=? RETURNING id_producto";
        try (var cn = dataSource.getConnection(); var ps = preparar(cn, sql)) {
            ps.setString(1, descripcion); ps.setBigDecimal(2, precio);
            ps.setInt(3, tipo); ps.setInt(4, proveedor); ps.setInt(5, unidad);
            if (id != null) ps.setInt(6, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (var cn = dataSource.getConnection(); var ps = preparar(cn, "DELETE FROM panaderia.producto WHERE id_producto=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Catálogos acotados: máximo 100 elementos de cada clase. */
    public Map<String, Object> catalogos() throws SQLException {
        try (var cn = dataSource.getConnection()) {
            var tipos = new ArrayList<TipoProducto>();
            try (var ps = preparar(cn, "SELECT id_tipo, descripcion FROM panaderia.tipo_producto ORDER BY id_tipo LIMIT 101");
                 var rs = ps.executeQuery()) {
                while (rs.next()) tipos.add(new TipoProducto(rs.getInt("id_tipo"), rs.getString("descripcion")));
            }
            var proveedores = new ArrayList<Proveedor>();
            try (var ps = preparar(cn, "SELECT id_proveedor, ruc, nombre FROM panaderia.proveedor ORDER BY id_proveedor LIMIT 101");
                 var rs = ps.executeQuery()) {
                while (rs.next()) proveedores.add(new Proveedor(rs.getInt("id_proveedor"), rs.getString("ruc"), rs.getString("nombre")));
            }
            var unidades = new ArrayList<UnidadMedida>();
            try (var ps = preparar(cn, "SELECT id_unidad, descripcion, simbolo FROM panaderia.unidad_medida ORDER BY id_unidad LIMIT 101");
                 var rs = ps.executeQuery()) {
                while (rs.next()) unidades.add(new UnidadMedida(rs.getInt("id_unidad"), rs.getString("descripcion"), rs.getString("simbolo")));
            }
            if (tipos.size() > 100 || proveedores.size() > 100 || unidades.size() > 100)
                throw new SQLException("El catálogo supera el límite permitido.", "54000");
            return Map.of("tipos", tipos, "proveedores", proveedores, "unidades", unidades);
        }
    }

    public boolean saludable() throws SQLException {
        try (var cn = dataSource.getConnection(); var ps = preparar(cn, "SELECT 1 FROM panaderia.producto LIMIT 1");
             var rs = ps.executeQuery()) { return true; }
    }
}
