package com.battleship.models;

public class NewGameResponse {
	private String userId;
	private String fullName;
	private String gameId;
	private String starting;
	private Rule rule;

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getGameId() {
		return gameId;
	}
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}
	public String getStarting() {
		return starting;
	}
	public void setStarting(String starting) {
		this.starting = starting;
	}
	public Rule getRule() {
		return rule;
	}
	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public NewGameResponse() {
		
	}

	public NewGameResponse(String userId, String fullName, String gameId, String starting, Rule rule) {
		super();
		this.userId = userId;
		this.fullName = fullName;
		this.gameId = gameId;
		this.starting = starting;
		this.rule = rule;
	}
}
