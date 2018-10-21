package com.battleship.dao;

import org.springframework.data.repository.CrudRepository;

import com.battleship.entities.User;

public interface UserDao extends CrudRepository<User, String> {

}