package com.battleship.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.battleship.entities.Game;
import com.battleship.models.GameLifeState;

public interface GameDao extends CrudRepository<Game, String> {
	List<Game> findByUserId(String userId);
	List<Game> findByUserIdAndStatus(String userId, GameLifeState status);
	List<Game> findByStatusAndAuto(GameLifeState status, boolean auto);
}