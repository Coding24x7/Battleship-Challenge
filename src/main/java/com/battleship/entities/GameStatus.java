package com.battleship.entities;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.battleship.models.GameProgressState;
import com.battleship.utils.GameUtil;

@Entity
@Table(name="game_status")
public class GameStatus {
	@Id
	private String gameId;
	private int selfShotCount;
	private String selfBoard;
	private int opponentShotCount;
	private String opponentBoard;
	@NotNull
	private String owner;

	@Enumerated(EnumType.STRING)
	private GameProgressState status;

	public GameStatus(){
		
	}

	public GameStatus(String gameId, int selfShotCount, int opponentShotCount, String owner) {
		this(gameId, selfShotCount, GameUtil.newGameBoard(), opponentShotCount,
				GameUtil.newCleanGameBoard(), owner, GameProgressState.player_turn);
	}

	public GameStatus(String gameId, int selfShotCount, String selfBoard, int opponentShotCount,
			String opponentBoard, String owner, GameProgressState status) {
		this.gameId = gameId;
		this.selfShotCount = selfShotCount;
		this.selfBoard = selfBoard;
		this.opponentShotCount = opponentShotCount;
		this.opponentBoard = opponentBoard;
		this.owner = owner;
		this.status = status;
	}

	@Id
	public String getGameId() {
		return gameId;
	}
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public int getSelfShotCount() {
		return selfShotCount;
	}
	public void setSelfShotCount(int selfShotCount) {
		this.selfShotCount = selfShotCount;
	}

	public String getSelfBoard() {
		return selfBoard;
	}
	public void setSelfBoard(String selfBoard) {
		this.selfBoard = selfBoard;
	}

	public int getOpponentShotCount() {
		return opponentShotCount;
	}
	public void setOpponentShotCount(int opponentShotCount) {
		this.opponentShotCount = opponentShotCount;
	}

	public String getOpponentBoard() {
		return opponentBoard;
	}
	public void setOpponentBoard(String opponentBoard) {
		this.opponentBoard = opponentBoard;
	}

	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public GameProgressState getStatus() {
		return status;
	}
	public void setStatus(GameProgressState status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "GameStatus [gameId=" + gameId + ", selfShotCount=" + selfShotCount + ", selfBoard=" + selfBoard
				+ ", opponentShotCount=" + opponentShotCount + ", opponentBoard=" + opponentBoard + ", owner=" + owner
				+ ", status=" + status + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameId == null) ? 0 : gameId.hashCode());
		result = prime * result + ((opponentBoard == null) ? 0 : opponentBoard.hashCode());
		result = prime * result + opponentShotCount;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((selfBoard == null) ? 0 : selfBoard.hashCode());
		result = prime * result + selfShotCount;
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameStatus other = (GameStatus) obj;
		if (gameId == null) {
			if (other.gameId != null)
				return false;
		} else if (!gameId.equals(other.gameId))
			return false;
		if (opponentBoard == null) {
			if (other.opponentBoard != null)
				return false;
		} else if (!opponentBoard.equals(other.opponentBoard))
			return false;
		if (opponentShotCount != other.opponentShotCount)
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (selfBoard == null) {
			if (other.selfBoard != null)
				return false;
		} else if (!selfBoard.equals(other.selfBoard))
			return false;
		if (selfShotCount != other.selfShotCount)
			return false;
		if (status != other.status)
			return false;
		return true;
	}
	
}
