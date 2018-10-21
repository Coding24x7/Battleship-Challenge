
**Battleship**

This is Java REST project for battleship board game. This is implemented by using
spring-boot framework and sqlite database.


**Prerequisites**
    Java 1.8 or greater.
    Maven 3.0 or greater.
	sqlite

**Package Structure**
- com.battleship: Spring boot application
- com.battleship.config: Database configuration
- com.battleship.controllers: REST controllers
- com.battleship.dao: Database repositories
- com.battleship.entities: Database entities
- com.battleship.exception: Custom exception
- com.battleship.model: Data models
- com.battleship.utils: Utility functions

**How to run**
mvn clean package
./run.sh <PORT> <DB_FILE_PATH>

**Features**
1) This is RESTfull project.
2) This project supports all game rules (please refer following GAME section for all rules).
3) It supports data persistence by using sqlite.
4) It has autopilot mode. Right now it just starts from first index and searches for next available position.

**Future Works**
1) Add more rest API for extra features and workings like user account and its related APIs.
2) Support authentication and authorization between endpoints.
3) Presentation layer.
4) Add test cases.
5) Currently new board generation creates only one type of board, need to implement more random board generation.
6) Add brain work to autopilot mode to make smart moves.

**GAME**

Battleship is a two players game, where each one has a board with ships laid out. The purpose
of the game is to destroy all of the other players ships before yours are destroyed. The locations
of these ships are concealed from the rival player, causing each player to have to guess the
location of the adversary’s fleet. Players take turns to fire upon each other.

Each players board is a 16 by 16 grid which is composed of cells that are either empty or
occupied. Upon the grid rests the battleships which stretch across multiple cells. The parts of
the ship can be either be intact or damaged; a ship is destroyed when all of its parts are
damaged. No two ships may touch each other, and can be rotated by 90 degrees (ie. a ship
may have 4 possible rotations). Once the ships have been placed, they don’t move for the entire
match.

There are five types of ships:
- Angle: This is a ship that has the form as the letter “L​”. It’s 4 by 3 cells.
- X-wing: This is a ship that has the form as the letter “X​”. It’s 5 by 3 cells.
- A-wing: This is a ship that has the form as the letter “A​”. It’s 4 by 3 cells.
- B-wing: This is a ship that has the form as the letter “B​”. It’s 5 by 3 cells.
- S-wing: This is a ship that has the form as the letter “S​”. It’s 3 by 3 cells.

A game can be also played with different rules:
1. Standard: Regular battleship game. Players take turn to shot each other.
2. Super-Charge: Each time a player destroys an opponent’s ship, he gets a single extra
turn to shot again.
3. Desperation: Each time a player destroys an opponent’s ship, he’s granted an extra shot
permanently that can be used in the next turn.
4. X-Shot: Each player has X shots per turn, X being a number between 1 and 10.

**Protocol API**

The protocol API will be used to communicate between players during a match. All fields in the
request and response are mandatory, in case of bad request it returns HTTP 400. UI/User should
not use this API, this is internal API for game.

**Endpoint​:** POST /api/v1/protocol/game/new
Description: This endpoint is invoked when another player is challenging us to a match. A new
board must be generated where ships are placed randomly. This request waits for 30 seconds for local
user acceptance.

Request:

Header: challenger=sourceAddress:port

Payload:
{
"userId": "challenger-X",
"fullName": "XYZ XYZ",
"protocol": "192.168.0.10:8080",
"rules": "standard"
}

Response:
{
"userId": "challenger-A",
"fullName": "ABC ABC",
"gameId": "55fca148-4753-4b9e-884f-0fbe17338add",
"starting": "challenger-X",
"rules": "standard"
}

**Endpoint​:** PUT /api/v1/protocol/game/{gameId}
Description: This endpoint is invoked when another player is firing upon us. The selected
position is encoded as an hex string (e.g. 1xB, 0xA, Fx8), the first element being the row
number and the second the column number. The maximum amount of shot per turn depends on
the chosen game mode.

Request:
{
"shots": ["1xB", "0xA", "5x1"]
}

Response:
{
"shots": {
"1xB": "hit",
"0xA": "kill",
"5x1": "miss"
},
"game": {
"status": "player_turn",
"owner": "challenger-A"
}
}

In case of bad request, the game doesn’t advance forward.
If a player’s entire fleet is destroyed, then it must return “won” as game status, with owner being
the winner.
A shot state can be the following:
- Miss: The shot hasn’t landed on a ship.
- Hit: The shot has landed on a ship, but it isn’t destroyed yet.
- Kill: When a shot hits the last healthy part of a ship, then it’s killed.

**User API**

The user API will be used by the user (or the UI) to communicate with its own Battleship instance.

**Endpoint​:** GET /api/v1/user/game/{gameId}
Description: This endpoint is used by the user to get a games status.
Request: N/A
Response:
{
"game": {
"status": "player_turn",
"owner": "challenger-A"
},
"self": {
"user_id": "challenger-A",
"board": ["*....**.**......", "*......*........", "*....**.**......", "*X*.............", ".....***....**..", "....*.*.....*..."
, ".....*X*...**..."]
},
"opponent": {
"user_id": "challenger-X",
"board": ["................", "....-....-......", "........X....X..", "................", ]
}
}

Each board is encoded with a string array of 16 elements, each one representing a row where
the cells are:
- Dot (.): The spot is empty (own board) or is unknown (opponent’s board).
- Asterisk (*): Only valid for your own board, represents a ship’s part.
- Dash (-): Means the shot hit the water (missed).
- Cross (X): Means the ships part located in that cell has been hit.

**Endpoint​:** POST /api/v1/user/game/new
Description: This endpoint is used by the user to start a match against an opponent. A new
board must be generated where ships are placed randomly.

Request:
{
"userId": "challenger-X",
"fullName": "XYZ XYZ",
"protocol": "192.168.0.10:8080",
"rules": "standard"
}

Response: string gameId

**Endpoint​:** PUT /api/v1/user/game/{gameId}/fire
Description: This endpoint is used by the user to fire against an opponent.
Request:
{
"shots": ["1xB", "0xA", "5x1"]
}
Response:
{
"shots": {
"1xB": "hit",
"0xA": "kill",
"5x1": "miss"
},
"game": {
"status": "player_turn",
"owner": "challenger-Y"
}
}

**Endpoint​:** POST /api/v1/user/game/{gameId}/auto
Description: This endpoint is used by the user to set the game on autopilot. This means that
every time it’s the players turn, the code automatically picks a position to shoot according to the
games rules.
Request: Empty body.




