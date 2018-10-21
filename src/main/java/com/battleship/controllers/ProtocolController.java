package com.battleship.controllers;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.battleship.dao.GameDao;
import com.battleship.entities.Game;
import com.battleship.entities.GameStatus;
import com.battleship.exceptions.BadRequestException;
import com.battleship.models.GameLifeState;
import com.battleship.models.GameProgressResponse;
import com.battleship.models.GameProgressState;
import com.battleship.models.NewGameRequest;
import com.battleship.models.NewGameResponse;
import com.battleship.models.ShotRequest;
import com.battleship.models.ShotResponse;
import com.battleship.models.ShotState;
import com.battleship.utils.AutoUtil;
import com.battleship.utils.GameUtil;

@Controller
@RequestMapping(path="/protocol")
public class ProtocolController {
	private static Logger logger = LoggerFactory.getLogger(ProtocolController.class);

	@Autowired 
	private GameDao gameDao;

	@Autowired
	private AutoUtil autoUtil;

	// incoming request tracking parameters
	private static Map<String, NewGameRequest> requestReceived;
	private static Map<String, NewGameResponse> requestAttended;

	public ProtocolController() {
		requestReceived = new HashMap<String, NewGameRequest>();
		requestAttended = new HashMap<String, NewGameResponse>();
	}

	// add remote server request to received list
	private boolean addToReceived(String source, NewGameRequest request) {
		boolean result = false;
		synchronized(requestReceived) {
			if (!requestReceived.containsKey(source)) {
				requestReceived.put(source, request);
				result = true;
			}
		}
		return result;
	}

	// checks incoming request attended by local user or not
	// removes request if attended by local user
	private NewGameResponse checkAndRemoveAttended(String source, boolean force) {
		NewGameResponse response = null;
		synchronized(requestReceived) {
			if (requestAttended.containsKey(source) || force) {
				requestReceived.remove(source);
				response = requestAttended.remove(source);
			}
		}
		return response;
	}

	/*
	 * Checks new game request already received from remote server or not.
	 * 
	 * @param  request 	new game request
	 * @return      	the new game response if already received request from remote server, else null
 	 */
	public static NewGameResponse hasReceived(NewGameRequest request) {
		NewGameResponse response = null;
		synchronized(requestReceived) {
			if (requestReceived.containsKey(request.getProtocol()) && !requestAttended.containsKey(request.getProtocol())) {
				NewGameRequest remoteRequest = requestReceived.get(request.getProtocol());
				if (remoteRequest.getRule() == request.getRule()) {
					// local and remote requests are matching

					String gameId = GameUtil.generateId(remoteRequest.getProtocol(), request.getProtocol());
					// create response for remote server request
					NewGameResponse remoteResponse = new NewGameResponse(request.getUserId(), request.getFullName(),
							gameId, remoteRequest.getUserId(), request.getRule());

					requestAttended.put(request.getProtocol(), remoteResponse);

					// create response for local server request
					response = new NewGameResponse(remoteRequest.getUserId(), remoteRequest.getFullName(), 
							gameId, remoteRequest.getUserId(), request.getRule());
				}
			}
		}
		return response;
	}

	/*
	 * Starts new game. This method accepts challenge from remote server and wait for local user to accept that challenge.
	 * 
	 * @param  gameRequest 		new game request
	 * @return  	    		new game status response
 	 */
	@PostMapping(path="/game/new")
	public @ResponseBody NewGameResponse startNewGame (@RequestBody NewGameRequest gameRequest, HttpServletRequest request) {
		// get request source address:port
		String remoteAddress = request.getHeader(GameUtil.HEADER_CHALLENGER);

		// get local host
		String selfAddress = request.getHeader("host");

		logger.info("Got challenge request " + request + " from " + remoteAddress);

		// check for already sent requests to same remote server
		if (UserController.hasSent(remoteAddress)) {
			// this server already sent request to remote server
			logger.info("Already request sent for " + remoteAddress);

			// solving this tie by comparing address
			if (selfAddress.compareTo(remoteAddress) > 0) {
				logger.info("Giving priority to local request");
				return null;
			}
		}

		// check for already received request from same remote server
 		if (!addToReceived(remoteAddress, gameRequest)) {
 			logger.info("Already request in progress from " + remoteAddress);
			throw new BadRequestException("Already request in progress from " + remoteAddress);
		}

		NewGameResponse response = null;

		// wait for 30 seconds for local user to accept new game challenge
		long t= System.currentTimeMillis();
		long end = t+30000;

		while(System.currentTimeMillis() < end) {
			try {
				// check any local user accepted challenge or not
				response = checkAndRemoveAttended(remoteAddress, false);
				if (response != null) {
					// local user accepted challenge
					break;
				}
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		if (response == null) {
			// check for last time and remove entry from challenge list forcefully
			response = checkAndRemoveAttended(remoteAddress, true);
		}

		logger.info("Sending response " + response + " for new game request from remote server " + remoteAddress);
		return response;
	}

	/*
	 * Fires shots on local server user board.
	 * This method updates database as per fire shot response.
	 * 
	 * @param  gameId  game id
	 * @param  shots   list of shots
	 * @return         the result of shots on board
 	 */
	@PutMapping(path="/game/{gameId}")
	public @ResponseBody ShotResponse fire(@PathVariable(value="gameId") String gameId, @RequestBody ShotRequest shots) {
		logger.info("Got fire request from remote server for game " + gameId);

		// check gameId
		if (!gameDao.existsById(gameId)) {
			logger.error("Wrong game id " + gameId);
			throw new BadRequestException("Wrong game id " + gameId);
		}

		Game g = gameDao.findById(gameId).get();

		// check game status (finished or inprogress)
		if (g.getStatus().equals(GameLifeState.finished)) {
			logger.error("Game for " + gameId + " is already finished");
			throw new BadRequestException("Game for " + gameId + " is already finished");
		}

		// check owner (as per turn)
		if (!g.getOpponent().getUserId().equals(g.getGameStatus().getOwner())) {
			logger.error("It is not turn for " + g.getOpponent().getUserId());
			throw new BadRequestException("It is not turn for " + g.getOpponent().getUserId());
		}

		// check shot count
		if (g.getGameStatus().getOpponentShotCount() != shots.getShots().size()) {
			logger.error(shots.getShots().size() + " shots not allowed for " + g.getOpponent().getUserId());
			throw new BadRequestException(shots.getShots().size() + " shots not allowed for " + g.getOpponent().getUserId());
		}

		try {
			Map <String, ShotState> shotResult = new HashMap<String, ShotState>();

			// fire shot on selfBoard
			String updatedBoard = GameUtil.fireShots(g.getGameStatus().getSelfBoard(), shots.getShots(), shotResult);

			// update database
			ShotResponse response = updateGameStatus(g, updatedBoard, shotResult);
			logger.info("Shot response, Owner: " + response.getGame().getOwner() + ", Status: " + response.getGame().getStatus() + ", notifying autopilot util.");
			if (response.getGame().getOwner() == g.getUser().getId() || response.getGame().getStatus() == GameProgressState.won) {
				autoUtil.notifyAuto(gameId);
			}

			return response;

		} catch (Exception e) {
			logger.error("Failed to fire for game " + gameId + " , Error: " + e.getMessage());
			throw e;
		}
	}

	/*
	 * Updates game status in database.
	 * 
	 * @param  game  		  game
	 * @param  updatedBoard   updated local user board
	 * @param  shotResult     result of shot fired on local user board
	 * @return         		  shot response for remote request
 	 */
	private ShotResponse updateGameStatus(Game g, String updatedBoard, Map<String, ShotState> shotResult) {
		GameStatus status = g.getGameStatus();

		// set updated local user board
		status.setSelfBoard(updatedBoard);

		// default change turn to self
		status.setOwner(g.getUser().getId());

		// handle destroyed ship condition
		if (shotResult.values().contains(ShotState.Kill)) {
			switch(g.getRule()) {
			case super_charge:
				// give opponent additional shot
				status.setOwner(g.getOpponent().getUserId());
				break;
	
			case desperation:
				// give opponent additional shot permanently
				status.setOpponentShotCount(status.getOpponentShotCount()+1);
				break;

			default:
				// no change
				break;
			}
		}

		GameProgressResponse progressResponse = new GameProgressResponse();
		if (GameUtil.destroyedAllShips(updatedBoard)) {
			// opponent win
			progressResponse.setStatus(GameProgressState.won);
			status.setOwner(g.getOpponent().getUserId());
			status.setStatus(GameProgressState.won);
			g.setStatus(GameLifeState.finished);

		} else {
			// game not finished	
			progressResponse.setStatus(GameProgressState.player_turn);
			status.setStatus(GameProgressState.player_turn);
		}

		progressResponse.setOwner(status.getOwner());
		g.setGameStatus(status);
		ShotResponse response = new ShotResponse(shotResult, progressResponse);
		gameDao.save(g);
		return response;
	}
}
