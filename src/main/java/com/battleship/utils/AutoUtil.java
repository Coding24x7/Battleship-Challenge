package com.battleship.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.battleship.controllers.UserController;
import com.battleship.dao.GameDao;
import com.battleship.entities.Game;
import com.battleship.models.GameLifeState;
import com.battleship.models.GameProgressState;
import com.battleship.models.ShotRequest;
import com.battleship.models.ShotResponse;

@Component
public class AutoUtil implements ApplicationRunner {
	Logger logger = LoggerFactory.getLogger(AutoUtil.class);

	@Autowired
	private GameDao gameDao;

	@Autowired
	private UserController userController;

	
	private static Map<String, AutoHandler> autoHandlerFactory = new HashMap<String, AutoHandler>();

	// autopilot mode handler
	private class AutoHandler {
		private String gameId;

		public AutoHandler(String gameId) {
			this.gameId = gameId;
		}

		// waits for notifyHandler
        public void handle()
        {
            // synchronized block ensures only one thread
            // running at a time.
        	Game game = gameDao.findById(gameId).get();
        	
            synchronized(this)
            {
                
            	while(game != null && game.getStatus() != GameLifeState.finished) {
	                try {
	                	ShotResponse response = null;
	                	if (game.getGameStatus().getOwner().equals(game.getUser().getId())) {
	                		// its my turn
	                		response = autoFire(game);	                		
	                	} 

	                	while(response != null && response.getGame().getOwner().equals(game.getUser().getId())) {
	                		// loop needs to handle super-charge bonus turn to shot again after destroying ship
	                		response = autoFire(game);
	                	}
	                		

	                	if (response != null && response.getGame().getStatus() == GameProgressState.won) {
                			// game finished
	                		break;
                		}

	                	logger.info("Waiting for my turn for game " + gameId);
	                	// releases the lock on shared resource
		            	wait();
		            	logger.info("Got notification for my turn for game " + gameId);

		            	// wait for some time, might be others are still working
		            	Thread.sleep(100);

		            	// get latest game data
		            	game = gameDao.findById(gameId).get();

	                } catch (Exception e) {
						logger.error("Error in autopilot mode for game " + gameId + " : " + e.toString());
						try {
							Thread.sleep(100);
							game = gameDao.findById(gameId).get();
						} catch (Exception e1) {
						}
					}
            	}
                logger.info("Terminating autopilot mode for game " + gameId);
            }
        }
 
        private ShotResponse autoFire(Game game) {
        	ShotRequest shots = GameUtil.nextAutoShots(game.getGameStatus().getOpponentBoard(), game.getGameStatus().getSelfShotCount());
        	ShotResponse response = userController.fireShots(game, shots);
        	return response;
        }

        public void notifyHandler()
        {
            synchronized(this)
            {
            	notify();
            	logger.info("Notified autopilot mode handler for game " + gameId);
            }
        }
	}

	/*
	 * Sets game to autopilot mode.
	 * 
	 * @param  gameId  game id
 	 */
	public void setAuto(String gameId) {
		logger.info("Setting autopilot mode for game " + gameId);
		final AutoHandler ah = new AutoHandler(gameId);
		synchronized(autoHandlerFactory) {
			if (!autoHandlerFactory.containsKey(gameId)) {
				try {
					new Thread(ah::handle).start();
					autoHandlerFactory.put(gameId, ah);
					logger.info("Created new autopilot mode handler for game " + gameId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * Notifies autopilot mode handler.
	 * 
	 * @param  gameId  game id
 	 */
	public void notifyAuto(String gameId) {
		logger.info("Notifying to autopilot mode handler for game " + gameId);
		AutoHandler ah = null;
		synchronized(autoHandlerFactory) {
			ah = autoHandlerFactory.get(gameId);
		}
		if (ah != null) {
			ah.notifyHandler();
		}
	}

	@Override
	public void run(ApplicationArguments args) {
		List<Game> games = gameDao.findByStatusAndAuto(GameLifeState.inprogress, true);
		for (Game g: games) {
			setAuto(g.getId());
		}
	}
}
