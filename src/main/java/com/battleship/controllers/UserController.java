package com.battleship.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.battleship.dao.GameDao;
import com.battleship.dao.OpponentDao;
import com.battleship.dao.UserDao;
import com.battleship.entities.Game;
import com.battleship.entities.GameStatus;
import com.battleship.entities.Opponent;
import com.battleship.entities.User;
import com.battleship.exceptions.BadRequestException;
import com.battleship.models.GameLifeState;
import com.battleship.models.GameProgressState;
import com.battleship.models.GameStatusResponse;
import com.battleship.models.NewGameRequest;
import com.battleship.models.NewGameResponse;
import com.battleship.models.ShotRequest;
import com.battleship.models.ShotResponse;
import com.battleship.models.ShotState;
import com.battleship.utils.AutoUtil;
import com.battleship.utils.GameUtil;

@Controller
@RequestMapping(path="/user")
public class UserController {
	Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired 
	private GameDao gameDao;
	@Autowired 
	private OpponentDao opponentDao;
	@Autowired 
	private UserDao userDao;
	@Autowired
	private AutoUtil autoUtil;

	// REST Client parameters
	RestTemplate restTemplate;
	String fireURL = "http://%s/api/v1/protocol/game/%s";
	String newGameURL = "http://%s/api/v1/protocol/game/new";

	// List of request sent to remote server
	private static List<String> requestSent;

	@Autowired
	public UserController(RestTemplateBuilder restTemplateBuilder) {
		requestSent = new ArrayList<String>();
		restTemplate = new RestTemplate();
	}

	// add new request
	private boolean addToSent(String remoteAddress) {
		boolean result = false;
		synchronized(requestSent) {
			if (!requestSent.contains(remoteAddress)) {
				requestSent.add(remoteAddress);
				result = true;
			}
		}
		return result;
	}

	// remove added request
	private void removeFromSent(String remoteAddress) {
		synchronized(requestSent) {
			requestSent.remove(remoteAddress);
		}
	}

	/*
	 * Checks new game request already sent for remote server or not.
	 * 
	 * @param  remoteAddress 	remote server address
	 * @return      			true if already request sent for same remote server, else false
 	 */
	public static boolean hasSent(String remoteAddress) {
		boolean sent = false;
		synchronized(requestSent) {
			if (requestSent.contains(remoteAddress)) {
				sent = true;
			}
		}
		return sent;
	}

	/*
	 * Returns game status.
	 * 
	 * @param  gameId 		game id
	 * @return      		game status
 	 */
	@GetMapping(path="/game/{gameId}")
	public @ResponseBody GameStatusResponse getGameStatus (@PathVariable(value="gameId") String gameId) {
		logger.info("Got game status request for game " + gameId);

		// check gameId
		if (!gameDao.existsById(gameId)) {
			logger.error("Wrong game id " + gameId);
			throw new BadRequestException("Wrong game id " + gameId);
		}
		return new GameStatusResponse(gameDao.findById(gameId).get());
	}

	/*
	 * Starts new game. This method updates database with new game details.
	 * 
	 * @param  selfAddress  self server address
	 * @param  request 		new game request
	 * @return      		the response from the remote server
 	 */
	@PostMapping(path="/game/new")
	public @ResponseBody String startNewGame (@RequestBody NewGameRequest gameRequest, HttpServletRequest request) {
		String selfAddress = request.getHeader("host");

		logger.info("Got new game creation request for protocol " + gameRequest.getProtocol());

		// check for opponent
		List<Game> games = gameDao.findByUserIdAndStatus(gameRequest.getUserId(), GameLifeState.inprogress);
		for (Game g : games) {
			if (g.getOpponent().getProtocol().equals(gameRequest.getProtocol())) {
				// user is already playing
				// assumption is we are not allowing duplicate userId for same server
				logger.info("User " + gameRequest.getUserId() + " already playing aginst " + gameRequest.getProtocol());
				throw new BadRequestException("User " + gameRequest.getUserId() + " already playing aginst " + gameRequest.getProtocol());
			}
		}

		// check already sent request
		if (!addToSent(gameRequest.getProtocol())) {
			// request already sent for same remote server
			logger.info("Already request sent for " + gameRequest.getProtocol());
			throw new BadRequestException("Already request sent for " + gameRequest.getProtocol());
		}

		NewGameResponse response = null;

		// check already received request
		try {
			response = ProtocolController.hasReceived(gameRequest);
		} catch (Exception e) {
			this.removeFromSent(gameRequest.getProtocol());
			throw e;
		}
		
		//	send protocol request
		if (response == null) {
			logger.info("No request found from remote server " + gameRequest.getProtocol());
			try {
				response = challengeToRemoteServer(selfAddress, gameRequest);
			} catch (Exception e) {
				this.removeFromSent(gameRequest.getProtocol());
				throw new BadRequestException(e.toString());
			}
		}

		if (response == null) {
			logger.info("No response from remote server " + gameRequest.getProtocol());
			// possibility that remote server reject request because it already sent one to this server
			// check once more
			response = ProtocolController.hasReceived(gameRequest);
		}

		this.removeFromSent(gameRequest.getProtocol());

		if (response == null) {
			// not able to create new game
			return null;
		}

		logger.info("Got response " + response + " from remote server " + gameRequest.getProtocol());
		// create new game and add to database with status running
		setGameEntry(gameRequest, response);

		// return game id
		return response.getGameId();
	}
	
	/*
	 * Fires shots on remote server board.
	 * This method updates database as per remote server response.
	 * 
	 * @param  gameId  game id
	 * @param  shots   list of shots
	 * @return         the response from the remote server
 	 */
	@PutMapping(path="/game/{gameId}/fire")
	public @ResponseBody ShotResponse fire(@PathVariable(value="gameId") String gameId, @RequestBody ShotRequest shots) {
		logger.info("Got fire request for game " + gameId);

		// check gameId
		if (!gameDao.existsById(gameId)) {
			logger.error("Wrong game id " + gameId);
			throw new BadRequestException("Wrong game id " + gameId);
		}

		Game game = gameDao.findById(gameId).get();

		// check game status (finished or inprogress)
		if (game.getStatus().equals(GameLifeState.finished)) {
			logger.error("Game for " + gameId + " is already finished");
			throw new BadRequestException("Game for " + gameId + " is already finished");
		}

		// check owner (as per turn)
		if (!game.getUser().getId().equals(game.getGameStatus().getOwner())) {
			logger.error("It is not turn for " + game.getUser().getId());
			throw new BadRequestException("It is not turn for " + game.getUser().getId());
		}

		// check autopilot mode
		if (game.isAuto()) {
			logger.error("User " + game.getUser().getId() + " is playing game " + gameId + " in autopilot mode");
			throw new BadRequestException("Game " + gameId + " is in autopilot mode");
		}

		return fireShots(game, shots);
	}

	/*
	 * Sets autopilot mode for given gameId.
	 * 
	 * @param  gameId  game id
 	 */
	@PostMapping(path="/game/{gameId}/auto")
	@ResponseStatus(value = HttpStatus.OK)
	public void auto(@PathVariable(value="gameId") String gameId) {
		logger.info("Got autopilot request for game " + gameId);

		// check gameId
		if (!gameDao.existsById(gameId)) {
			logger.error("Wrong game id " + gameId);
			throw new BadRequestException("Wrong game id " + gameId);
		}

		Game game = gameDao.findById(gameId).get();
		// check game status
		if (game.getStatus() == GameLifeState.finished) {
			logger.error("Game " + gameId + " is already finished, can't change to autopilot mode");
			throw new BadRequestException("Game " + gameId + " is already finished, can't change to autopilot mode");
		}

		// check game mode
		if (game.isAuto()) {
			logger.error("Game " + gameId + " is already in autopilot mode");
			throw new BadRequestException("Game " + gameId + " is already in autopilot mode");
		}

		setToAutoMode(game);
	}

	/*
	 * Fires shots on remote server board without checking autopilot mode.
	 * This method updates database as per remote server response.
	 * 
	 * @param  game    the game
	 * @param  shots   list of shots
	 * @return         the response from the remote server
 	 */
	public ShotResponse fireShots(Game game, ShotRequest shots) {
		// check game status (finished or inprogress)
		if (game.getStatus().equals(GameLifeState.finished)) {
			logger.error("Game for " + game.getId() + " is already finished");
			throw new BadRequestException("Game for " + game.getId() + " is already finished");
		}

		// check owner (as per turn)
		if (!game.getUser().getId().equals(game.getGameStatus().getOwner())) {
			logger.error("It is not turn for " + game.getUser().getId());
			throw new BadRequestException("It is not turn for " + game.getUser().getId());
		}

		// check shot count
		if (game.getGameStatus().getSelfShotCount() < shots.getShots().size() || shots.getShots().size() == 0) {
			logger.error(shots.getShots().size() + " shots not allowed for " + game.getUser().getId());
			throw new BadRequestException(shots.getShots().size() + " shots not allowed for " + game.getUser().getId());
		}

		// send protocol request
		ShotResponse response = null;
		try {
			response = fireOnRemoteServer(game.getId(), game.getOpponent().getProtocol(), shots);
			logger.info("Got response from " + game.getOpponent().getProtocol() + " : " + response.toString());
		} catch (Exception e) {
			logger.error("Fire on remote server failed " + e.getMessage());
			throw e;
		}
		
		if (response != null) {
			// update db
			updateGameStatus(game, response);
		}

		// return result
		return response;	
	}

	/*
	 * Sends new game challenge to remote server.
	 * 
	 * @param  selfAddress  self server address
	 * @param  request 		new game request
	 * @return      		the response from the remote server
 	 */
	private NewGameResponse challengeToRemoteServer(String selfAddress, NewGameRequest request) {
		try {
			logger.info("Connecting to remote server " + request.getProtocol() + " to create new game.");
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
			headers.add(GameUtil.HEADER_CHALLENGER, selfAddress);

			restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

			HttpEntity<NewGameRequest> r = new HttpEntity<NewGameRequest>(request, headers);

			NewGameResponse response = restTemplate.postForObject(String.format(newGameURL, request.getProtocol()), r, NewGameResponse.class);

			return response;
	
		} catch (HttpClientErrorException e) {
				throw new BadRequestException(e.getResponseBodyAsString());
        } catch (Exception e) {
			throw new BadRequestException(e.toString());
		}
	}

	/*
	 * Fires shots on remote server for given gameId.
	 * 
	 * @param  gameId 		  game id
	 * @param  remoteAddress  remote server address
	 * @param  shots 		  shot request giving input to fire on remote server
	 * @return      		  the response from the remote server
 	 */
	private ShotResponse fireOnRemoteServer(String gameId, String remoteAddress, ShotRequest shots) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON); 
			HttpEntity<ShotRequest> entity = new HttpEntity<ShotRequest>(shots, headers);
			ResponseEntity<ShotResponse> response = restTemplate.exchange(String.format(fireURL, remoteAddress, gameId), HttpMethod.PUT, 
					entity, ShotResponse.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				throw new BadRequestException("not able to fire on remote server " + remoteAddress + " for game " + gameId);
			}
			return response.getBody();
	
		} catch (HttpClientErrorException e) {
			throw new BadRequestException(e.getResponseBodyAsString());
		} catch (Exception e) {
			throw new BadRequestException(e.toString());
		}
	}

	/*
	 * Create new game entry in database.
	 * 
	 * @param  request 	request parameter to create new game
	 * @param  response response of new game creation
	 */
	private void setGameEntry(NewGameRequest request, NewGameResponse response) {
		// TODO: make it atomic, one db call to store
		// update opponent
		Opponent o = opponentDao.findByUserIdAndProtocol(response.getUserId(), request.getProtocol());
		if (o == null) {
			o = new Opponent(response.getUserId(), response.getFullName(), request.getProtocol());
			opponentDao.save(o);
		}

		// update self user
		User u;
		Optional<User> ou = userDao.findById(request.getUserId());
		if (!ou.isPresent()) {
			u = new User(request.getUserId(), request.getFullName());
			userDao.save(u);
		} else {
			u = ou.get();
		}

		// update game data
		Game g = new Game(response.getGameId(), u, o, GameLifeState.inprogress, response.getRule(), response.getStarting());
		gameDao.save(g);
	}

	/*
	 * Updates opponent entry, self user entry and game status in database.
	 * 
	 * @param  game 	game to set in database
	 * @param  response response of fire function
	 */
	private void updateGameStatus(Game game, ShotResponse response) {
		GameStatus status = game.getGameStatus();
		String opponentBoard = status.getOpponentBoard();

		// update opponent board
		opponentBoard = GameUtil.applyShotResponse(opponentBoard, response.getShots());
		status.setOpponentBoard(opponentBoard);

		// change turn
		status.setOwner(response.getGame().getOwner());

		if (response.getShots().values().contains(ShotState.Kill)) {
		// destroyed ship
			switch(game.getRule()) {
			case desperation:
				status.setSelfShotCount(status.getSelfShotCount()+1);
				break;

			default:
				// no change
				break;
			}
		}

		if (response.getGame().getStatus().equals(GameProgressState.won)) {
			// win
			status.setStatus(GameProgressState.won);
			game.setStatus(GameLifeState.finished);
		} else {
			// game not finished
			status.setStatus(GameProgressState.player_turn);
		}

		game.setGameStatus(status);
		gameDao.save(game);
	}

	/*
	 * Sets game on autopilot mode.
	 * 
	 * @param  game game to set for autopilot mode
	 */
	private void setToAutoMode(Game game){
		game.setAuto(true);
		gameDao.save(game);
		autoUtil.setAuto(game.getId());
	}
}
