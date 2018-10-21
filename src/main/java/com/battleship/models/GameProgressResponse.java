package com.battleship.models;

public class GameProgressResponse {
	private GameProgressState status;
	private String owner;

	public GameProgressResponse() {
		
	}

	public GameProgressResponse(GameProgressState status, String owner) {
		this.status = status;
		this.owner = owner;
	}

	public GameProgressState getStatus() {
		return status;
	}
	public void setStatus(GameProgressState status) {
		this.status = status;
	}

	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
}
