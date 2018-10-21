package com.battleship.models;

import java.util.Map;

public class ShotResponse {
	Map<String, ShotState> shots;
	GameProgressResponse game;

	public ShotResponse() {
		
	}

	public ShotResponse(Map<String, ShotState> shots, GameProgressResponse game) {
		super();
		this.shots = shots;
		this.game = game;
	}

	public Map<String, ShotState> getShots() {
		return shots;
	}
	public void setShots(Map<String, ShotState> shots) {
		this.shots = shots;
	}
	public GameProgressResponse getGame() {
		return game;
	}
	public void setGame(GameProgressResponse game) {
		this.game = game;
	}

	@Override
	public String toString() {
		return "ShotResponse [shots=" + shots + ", game=" + game + "]";
	}
}
