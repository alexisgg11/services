package com.alexis.repository;

import com.alexis.model.Producto;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    @EntityGraph(attributePaths = {"tipo", "proveedor", "unidad"})
    @Query("""
        select p from Producto p
        where locate(lower(:buscar), lower(p.descripcion)) > 0
          and (:proveedor is null or p.proveedor.idProveedor = :proveedor)
          and (:tipo is null or p.tipo.idTipo = :tipo)
        """)
    Page<Producto> buscar(@Param("buscar") String buscar, @Param("proveedor") Integer proveedor,
                          @Param("tipo") Integer tipo, Pageable pagina);

    @Override
    @EntityGraph(attributePaths = {"tipo", "proveedor", "unidad"})
    Optional<Producto> findById(Integer id);

    @EntityGraph(attributePaths = {"tipo", "proveedor", "unidad"})
    Optional<Producto> findFirstByOrderByPrecioDescIdProductoAsc();

    @Query("""
        select p.proveedor.idProveedor as idProveedor, count(p) as cantidad
        from Producto p group by p.proveedor.idProveedor order by p.proveedor.idProveedor
        """)
    List<CantidadPorProveedor> contarPorProveedor(Pageable limite);

    interface CantidadPorProveedor {
        Integer getIdProveedor();
        Long getCantidad();
    }
}
