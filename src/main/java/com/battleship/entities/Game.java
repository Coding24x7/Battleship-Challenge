package com.battleship.entities;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.battleship.models.GameLifeState;
import com.battleship.models.Rule;

@Entity
@Table(name = "games")
public class Game {

	@Id
	private String id;
	
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "opponentId", nullable=false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opponent opponent;

    @Enumerated(EnumType.STRING)
	private GameLifeState status;

	@Enumerated(EnumType.STRING)
	private Rule rule;

	@Column(name = "created_at")
	private Date createdAt;

	@OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
	private GameStatus gameStatus;

	private boolean auto;

	public Game(){
		
	}

	public Game(String id, User user, Opponent opponent, GameLifeState status, Rule rule, String owner) {
		this(id, user, opponent, status, rule, new GameStatus(id, rule.getShotCount(), rule.getShotCount(), owner));
	}

	public Game(String id, User user, Opponent opponent, GameLifeState status, Rule rule, GameStatus gameStatus) {
		this.id = id;
		this.user = user;
		this.opponent = opponent;
		this.status = status;
		this.rule = rule;
		this.gameStatus = gameStatus;
		this.auto = false;
		this.createdAt = new Date();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Opponent getOpponent() {
		return opponent;
	}

	public void setOpponent(Opponent opponent) {
		this.opponent = opponent;
	}

	public GameLifeState getStatus() {
		return status;
	}

	public void setStatus(GameLifeState status) {
		this.status = status;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public GameStatus getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(GameStatus gameStatus) {
		this.gameStatus = gameStatus;
	}

	public boolean isAuto() {
		return auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	@Override
	public String toString() {
		return "Game [id=" + id + ", user=" + user + ", opponent=" + opponent + ", status=" + status + ", rule=" + rule
				+ ", createdAt=" + createdAt + ", gameStatus=" + gameStatus + ", auto=" + auto + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (auto ? 1231 : 1237);
		result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
		result = prime * result + ((gameStatus == null) ? 0 : gameStatus.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((opponent == null) ? 0 : opponent.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		Game other = (Game) obj;
		if (auto != other.auto)
			return false;
		if (createdAt == null) {
			if (other.createdAt != null)
				return false;
		} else if (!createdAt.equals(other.createdAt))
			return false;
		if (gameStatus == null) {
			if (other.gameStatus != null)
				return false;
		} else if (!gameStatus.equals(other.gameStatus))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (opponent == null) {
			if (other.opponent != null)
				return false;
		} else if (!opponent.equals(other.opponent))
			return false;
		if (rule != other.rule)
			return false;
		if (status != other.status)
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}
}
