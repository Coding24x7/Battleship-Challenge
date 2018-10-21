package com.battleship.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.battleship.exceptions.BadRequestException;
import com.battleship.models.ShotRequest;
import com.battleship.models.ShotState;

public class GameUtil {
	public static final String HEADER_CHALLENGER = "challenger";

	/*
	 * Generates new game board with ships placed randomly on it.
	 * 
	 * @return    the game board string
 	 */
	public static String newGameBoard() {
		// TODO: create more random game boards
		return "3....55.55......;" +
			   "3......5........;" +
			   "3....55.55......;" +
			   "333.............;" +
			   "...........222..;" +
			   "...........2.2..;" +
			   "...........22...;" +
			   "...........2.2..;" +
			   "...........222..;" +
			   "................;" +
			   "................;" +
			   "................;" +
   			   ".....111....44..;" +
			   "....1.1.....4...;" +
			   ".....111...44...;" +
			   "................";
	}

	/*
	 * Generates new clean game board for opponent.
	 * 
	 * @return    the string game board
 	 */
	public static String newCleanGameBoard() {
		return "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................;" +
			   "................";
	}

	/*
	 * Converts game board string to array.
	 * 
	 * @param  stringBoard    	string game board
	 * @return    	   			the game board array
 	 */
	public static String[] toArrayBoard(String stringBoard) {
		if (!isValidStringBoard(stringBoard)) {
			return null;
		}
		stringBoard = stringBoard.replaceAll("\\d", "*");
		return stringBoard.split(";");
	}

	/*
	 * Checks validity of board.
	 * 
	 * @param  stringBoard    	string game board
	 * @return    	   			true if valid, else false
 	 */
	private static boolean isValidStringBoard(String stringBoard) {
		if (stringBoard==null || stringBoard.length() != 271) return false;

		return true;
	}

	/*
	public static int getRowIndex(String key) {
		String hexDigit = "0123456789ABCDEF";
		try {
			return hexDigit.indexOf(key.split("x")[0]);
		} catch(Exception e) {
			return -1;
		}
	}

	public static int getColumnIndex(String key) {
		String hexDigit = "0123456789ABCDEF";
		try {
			return hexDigit.indexOf(key.split("x")[1]);
		} catch(Exception e) {
			return -1;
		}
	}
	*/

	/*
	 * Returns index for game board shot location key.
	 * 
	 * @param  key    	shot location key
	 * @return    	   	the index in string game board
 	 */
	private static int getIndex(String key) {
		String hexDigit = "0123456789ABCDEF";
		try {
			int row = hexDigit.indexOf(key.split("x")[0]);
			int column = hexDigit.indexOf(key.split("x")[1]);
			// each row has 16 + 1 (;)
			return 17 * row + column;
		} catch(Exception e) {
			return -1;
		}
	}

	/*
	 * Fires shot on game board.
	 * 
	 * @param  stringBoard    	string game board
	 * @param  shots    		shots to fire
	 * @param  result    		map to store fire result
	 * @return    	   			the updated string game board
 	 */
	public static String fireShots(String stringBoard, List<String> shots, Map<String, ShotState> result) {
		if (!isValidStringBoard(stringBoard)) {
			throw new BadRequestException("Wrong board");
		}

		char[] board = stringBoard.toCharArray();
		for(String shot: shots) {
			int index = getIndex(shot);
			if (index < 0) {
				throw new BadRequestException("Wrong shot location " + shot);
			}
			if (board[index] == '-' || board[index] == 'X') {
				throw new BadRequestException("Alredy fired at this location " + shot);
			}

			switch(board[index]) {
			case '.':
				// miss
				board[index] = '-';
				result.put(shot, ShotState.Miss);
				break;
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
				// hit or kill
				String oldEntry = String.valueOf(board[index]);
				board[index] = 'X';
				if (!(new String(board).contains(oldEntry))) {
					// kill
					result.put(shot, ShotState.Kill);
				} else {
					result.put(shot, ShotState.Hit);
				}
				break;
			default:
				throw new BadRequestException("Wrong board");
			}
		}

		return new String(board);
	}

	/*
	 * Applies shot response on game board and returns updated board.
	 * 
	 * @param  stringBoard    	string game board
	 * @param  shots			shots response to apply
	 * @return    	   			the updated string game board
 	 */
	public static String applyShotResponse(String stringBoard, Map<String, ShotState> shots) {
		if (!isValidStringBoard(stringBoard)) {
			throw new BadRequestException("Wrong board");
		}

		char[] board = stringBoard.toCharArray();

		for(Entry<String, ShotState> e: shots.entrySet()) {
			int index = getIndex(e.getKey());

			switch(e.getValue()) {
			case Hit:
			case Kill:
				board[index] = 'X';
				break;
			case Miss:
				board[index] = '-';
				break;
			}
		}

		return new String(board);
	}

	/*
	 * Generates random ID.
	 * 
	 * @param  source    	source server address
	 * @param  remote		remote server address
	 * @return   			the uniq ID
 	 */
	public static String generateId(String source, String remote) {
		// TODO: ID generation should be source and remote dependent
		return UUID.randomUUID().toString();
	}

	/*
	 * Check all ships destroyed or not.
	 * 
	 * @param  stringBoard    	string game board
	 * @return    	   			true if all ships are destroyed from game board, else false
 	 */
	public static boolean destroyedAllShips(String stringBoard) {
		if (stringBoard.length() > 0 && !(stringBoard.matches(".*[1-9].*"))) {
			return true;
		}
		return false;
	}

	/*
	 * Returns next randomly selected shots for board.
	 * 
	 * @param  stringBoard    	string game board
	 * @param  shotCount		maximum shot counts to return
	 * @return    	   			the list of shots
 	 */
	public static ShotRequest nextAutoShots(String stringBoard, int shotCount) {
		// TODO: Implement brain here
		List<String> shots = new ArrayList<String>();
		String loc = "0x0";
		int startIndex = getIndex(loc);
		boolean firstIndex = true;

		char[] board = stringBoard.toCharArray();
		for(int i=0; i<shotCount; i++) {
			int index = getIndex(loc);
			while((index < 0 || board[index] != '.') && (index != startIndex || firstIndex)) {
				loc = nextShot(loc);
				index = getIndex(loc);
				firstIndex = false;
			}
			if ((index == startIndex && !firstIndex) || shots.contains(loc)) {
				break;
			}
			shots.add(loc);
			loc = nextShot(loc);
		}
		ShotRequest request = new ShotRequest();
		request.setShots(shots);
		return request;
	}

	/*
	 * Returns next shot location after loc.
	 * 
	 * @param  loc    	location on board
	 * @return  		the next location on board
 	 */
	private static String nextShot(String loc) {
		String hexDigit = "0123456789ABCDEF";
		char[] hexDigitChars = hexDigit.toCharArray();

		char[] chars = loc.toCharArray();

		try {
			int row = hexDigit.indexOf(String.valueOf(chars[0]));
			int column = hexDigit.indexOf(String.valueOf(chars[2]));
			if (column > 14) {
				column = 0;
				row++;
			} else {
				column++;
			}
			chars[0] = hexDigitChars[row];
			chars[2] = hexDigitChars[column];

		} catch(Exception e) {
			chars[1] = 0;
		}
		return new String(chars);
	}
}
