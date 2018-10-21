package com.battleship.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DBInitializeConfig {

	@Autowired
	private DataSource dataSource;
	
	@PostConstruct
	public void initialize(){
		try {
			Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS games(" +
					"id varchar(30) Primary key, " +
					"user_id varchar(30) not null," +
					"opponent_id INTEGER not null," +
					"status varchar(30) not null," +
					"rule varchar(30) not null," +
					"auto bool DEFAULT 'false'," +
					"created_at DATETIME NOT NULL,"+ 
					"FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE cascade," + 
					"FOREIGN KEY (opponent_id) REFERENCES opponents (id) ON DELETE cascade)" 
					);
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS game_status(" +
					"game_id varchar(30) Primary key," +
					"self_shot_count INTEGER not null," +
					"self_board varchar(300) not null," +
					"opponent_shot_count INTEGER not null," +
					"opponent_board varchar(300) not null," +
					"owner varchar(30) not null," +
					"status varchar(30) not null," +
					"FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE cascade)" 
					);
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS users(" +
					"id varchar(30) Primary key, " +
					"full_name varchar(30))" 
					);
			statement.executeUpdate(
					"CREATE TABLE IF NOT EXISTS opponents(" +
					"id INTEGER Primary key," +
					"user_id varchar(30)," +
					"protocol varchar(50)," +
					"full_name varchar(30))" 
					);
			statement.close();
			connection.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
}