package com.battleship.models;

import com.battleship.entities.Game;
import com.battleship.utils.GameUtil;

class PlayerInfo {
	private String userId;
	private String[] board;

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String[] getBoard() {
		return board;
	}
	public void setBoard(String[] board) {
		this.board = board;
	}
}

public class GameStatusResponse {
	GameProgressResponse game;
	PlayerInfo self;
	PlayerInfo opponent;

	public GameStatusResponse() {
		
	}

	public GameStatusResponse(Game g) {
		if (g==null) {
			return;
		}
		PlayerInfo p1 = new PlayerInfo();
		p1.setUserId(g.getUser().getId());
		p1.setBoard(GameUtil.toArrayBoard(g.getGameStatus().getSelfBoard()));
		this.self = p1;
		PlayerInfo p2 = new PlayerInfo();
		p2.setUserId(g.getOpponent().getUserId());
		p2.setBoard(GameUtil.toArrayBoard(g.getGameStatus().getOpponentBoard()));
		this.opponent = p2;
		game = new GameProgressResponse(g.getGameStatus().getStatus(), g.getGameStatus().getOwner());
	}

	public GameProgressResponse getGame() {
		return game;
	}
	public void setGame(GameProgressResponse game) {
		this.game = game;
	}
	public PlayerInfo getSelf() {
		return self;
	}
	public void setSelf(PlayerInfo self) {
		this.self = self;
	}
	public PlayerInfo getOpponent() {
		return opponent;
	}
	public void setOpponent(PlayerInfo opponent) {
		this.opponent = opponent;
	}
}
