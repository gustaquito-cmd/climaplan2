package com.universidad.climaplan.repository;

import com.universidad.climaplan.entity.DestinoFavorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestinoFavoritoRepository extends JpaRepository<DestinoFavorito, Long> {

    List<DestinoFavorito> findAllByUsuarioId(Long usuarioId);

    Optional<DestinoFavorito> findByIdAndUsuarioId(Long id, Long usuarioId);
}
