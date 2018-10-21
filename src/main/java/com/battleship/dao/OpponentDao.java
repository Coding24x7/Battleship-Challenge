package com.battleship.dao;

import org.springframework.data.repository.CrudRepository;

import com.battleship.entities.Opponent;

public interface OpponentDao extends CrudRepository<Opponent, Long> {
	Opponent findByUserIdAndProtocol(String userId, String protocol);
}