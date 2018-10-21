package com.battleship.dao;

import org.springframework.data.repository.CrudRepository;

import com.battleship.entities.GameStatus;

public interface GameStatusDao extends CrudRepository<GameStatus, Long> {

}