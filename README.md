# Candy Land Game

A Java implementation of the classic Candy Land board game supporting up to 3 players with a graphical user interface. This project includes automated testing, continuous integration, and code quality analysis with SonarQube.

## Features

- **1-3 Players**: Support for up to 3 players in local or online multiplayer modes
- **Online Multiplayer**: Connect to a game server and play with friends over the network
- **Graphical User Interface**: Full GUI with visual game board and controls
- **Classic Gameplay**: Traditional Candy Land rules with colored cards and special character cards
- **Interactive Board**: Visual representation of the 134-space board with colored spaces
- **Player Tracking**: Real-time display of player positions and game progress
- **Game Controls**: Easy-to-use interface for drawing cards and managing turns
- **Complete Game Board**: 134-space board with special character locations
- **Turn-based Play**: Players take turns drawing cards and moving
- **Win Condition**: First player to reach the end wins
- **Automated Testing**: Comprehensive unit test suite with code coverage
- **Code Quality**: Continuous code quality analysis with SonarQube
- **CI/CD Pipeline**: GitHub Actions workflow for automated builds and analysis

## Screenshots

The game features:
- **Player Setup Dialog**: Enter player names and select number of players
- **Visual Game Board**: Colorful board with snake-like path and player positions
- **Game Controls Panel**: Draw cards, view game status, and track player positions
- **Menu System**: New game, game rules, and help options

## Project Structure

```
src/
├── main/java/com/example/candyland/
│   ├── gui/
│   │   ├── CandyLandGUI.java                # Main GUI window and application entry point
│   │   ├── PlayerSetupDialog.java           # Player setup dialog
│   │   ├── MultiplayerConnectionDialog.java # Multiplayer connection dialog
│   │   ├── GameBoardPanel.java              # Visual game board panel
│   │   └── GameControlPanel.java            # Game controls and information panel
│   ├── network/
│   │   ├── server/
│   │   │   └── CandyLandServer.java         # Multiplayer game server
│   │   ├── client/
│   │   │   └── GameClient.java              # Network client for multiplayer
│   │   ├── GameMessage.java                 # Base class for network messages
│   │   ├── JoinGameMessage.java             # Join game request message
│   │   ├── PlayerJoinedMessage.java         # Player joined notification
│   │   ├── GameStateMessage.java            # Game state synchronization
│   │   ├── DrawCardMessage.java             # Card draw request
│   │   ├── CardDrawnMessage.java            # Card drawn notification
│   │   ├── GameOverMessage.java             # Game over notification
│   │   └── ErrorMessage.java                # Error message
│   ├── CandyLandGame.java                   # Core game logic and rules
│   ├── Player.java                          # Player representation
│   ├── Board.java                           # Game board with spaces and special locations
│   ├── Card.java                            # Game cards (color and character cards)
│   ├── Deck.java                            # Card deck management
│   └── Color.java                           # Board colors enumeration
└── test/java/com/example/candyland/
    ├── PlayerTest.java                      # Unit tests for Player class
    ├── BoardTest.java                       # Unit tests for Board class
    └── CandyLandGameTest.java               # Unit tests for CandyLandGame class
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## CI/CD Pipeline

This project uses GitHub Actions for continuous integration with automated testing and code quality analysis.

### Workflow Triggers
- **Push to main branch**: Full build, test, and SonarQube analysis
- **Pull requests**: Build, test, and analysis for code review

### Pipeline Features
- **Automated Testing**: Runs complete test suite on every commit
- **Code Coverage**: Generates coverage reports with JaCoCo
- **SonarQube Analysis**: Static code analysis for quality, security, and maintainability
- **Dependency Caching**: Optimized build times with Maven and SonarQube caching
- **Artifact Upload**: Test results and analysis artifacts for review

### Setting Up SonarQube Integration

To enable SonarQube analysis in your fork:

1. **Set up SonarQube Server** - Ensure you have access to a SonarQube Server instance
2. **Create a Project** in SonarQube Server with key `candy-land-java-game`
3. **Generate Authentication Token** in SonarQube Server
4. **Add repository secrets** in GitHub Settings > Secrets and variables > Actions:
   - `SONAR_TOKEN`: Your SonarQube Server authentication token
   - `SONAR_HOST_URL`: Your SonarQube Server URL (e.g., `https://your-sonarqube-server.com`)

#### Getting Your SonarQube Server Token
1. Log in to your SonarQube Server
2. Go to **My Account** > **Security** 
3. Generate a new token with appropriate permissions
4. Copy the token and add as `SONAR_TOKEN` in GitHub repository secrets

#### Project Setup in SonarQube Server
1. Create a new project in SonarQube Server
2. Use project key: `candy-land-java-game`
3. Configure quality gates and analysis settings as needed

### Quality Gates
The SonarQube analysis checks for:
- **Code Coverage**: Minimum coverage thresholds
- **Security**: Vulnerabilities and security hotspots
- **Maintainability**: Code smells and technical debt
- **Reliability**: Bugs and potential runtime issues

## Building and Running

### Using Maven Commands

1. **Compile the project:**
   ```bash
   mvn compile
   ```

2. **Run tests:**
   ```bash
   mvn test
   ```

3. **Run the GUI game (local mode):**
   ```bash
   mvn exec:java
   ```

4. **Run the command-line game:**
   ```bash
   mvn -q compile exec:java@cli
   # or, without ANSI colors
   mvn -q compile exec:java@cli -Dexec.args="--no-color"
   ```
   At each turn press Enter to draw a card, `b` to show the board, or `q` to quit.

5. **Clean and rebuild:**
   ```bash
   mvn clean compile
   ```

### Online Multiplayer Mode

#### Starting the Game Server

To host an online multiplayer game, first start the server:

```bash
# Using Maven (default port 8888)
mvn exec:java -Dexec.mainClass="com.example.candyland.network.server.CandyLandServer"

# Or specify a custom port
mvn exec:java -Dexec.mainClass="com.example.candyland.network.server.CandyLandServer" -Dexec.args="9999"

# Or compile and run directly
mvn compile
java -cp target/classes com.example.candyland.network.server.CandyLandServer 8888
```

The server will start and listen for client connections. It supports:
- Multiple game rooms (players specify a room ID when joining)
- Up to 3 players per game room
- Automatic game state synchronization
- Connection management and error handling

#### Joining an Online Game

1. Run the game client:
   ```bash
   mvn exec:java
   ```

2. Select "Join Online Game" from the welcome dialog

3. Enter connection details:
   - **Server Host**: IP address or hostname (use `localhost` for local testing)
   - **Server Port**: Port number (default: 8888)
   - **Your Name**: Your player name
   - **Room ID**: Game room identifier (use the same ID to play with friends)

4. Click "Connect" and wait for other players to join

5. Game starts automatically when 2+ players have joined

#### Network Architecture

The multiplayer system uses a client-server architecture:

- **Server (`CandyLandServer`)**: Manages game rooms, player connections, and game state
- **Client (`GameClient`)**: Connects to server and handles network communication
- **Messages**: Serialized Java objects for game events and state updates
- **Protocol**: TCP/IP sockets with ObjectInputStream/ObjectOutputStream

Game rooms are isolated - each room ID represents a separate game instance.

### Using VS Code Tasks

This project includes predefined VS Code tasks that you can run from the Command Palette (`Ctrl+Shift+P` or `Cmd+Shift+P`):

- **Build Candy Land Game** - Compile the project
- **Run Candy Land Game** - Start the GUI game
- **Test Candy Land Game** - Run unit tests
- **Clean and Build** - Clean and recompile

### Using VS Code Debug Configuration

A debug configuration is available for running the game in debug mode:

- **Debug Candy Land Game** - Launch with debugger attached

## How to Play

### Local Game Mode

1. **Start the Game**: Run the application using `mvn exec:java`
2. **Select "Local Game"** from the welcome dialog
3. **Setup Players**: 
   - A dialog will appear asking for the number of players (1-3)
   - Enter names for each player
   - Click "Start Game" to begin
4. **Playing the Game**:
   - The game board will display with colored spaces in a snake-like pattern
   - Players are represented by colored circles on the board
   - Click "Draw Card" to take your turn
   - Move according to the card drawn:
     - Color cards: Move to the next space of that color
     - Double color cards: Move to the second occurrence of that color
     - Special character cards: Move to the character's specific location
5. **Game Information**:
   - Current player is displayed in the control panel
   - Player positions are shown in real-time
   - Game log tracks all moves and events
6. **Winning**: The first player to reach or pass the finish line (golden border) wins!

### Online Multiplayer Mode

1. **Start the Server**: One player must start the game server (see "Starting the Game Server" above)
2. **Launch the Game**: Each player runs `mvn exec:java`
3. **Select "Join Online Game"** from the welcome dialog
4. **Connect**: 
   - Enter the server's host address (or `localhost` if playing on the same machine)
   - Enter the server port (default: 8888)
   - Enter your player name
   - Enter a Room ID (all players must use the same Room ID to play together)
   - Click "Connect"
5. **Wait for Players**: The game starts automatically when 2 or more players have joined
6. **Taking Turns**:
   - Only the current player can draw a card
   - The "Draw Card" button is only enabled for the active player
   - Game state updates automatically for all connected players
   - Move log shows all player actions in real-time
7. **Game Progress**: Watch the board update as players take turns
8. **Winning**: First player to reach the end wins - all players are notified!

## GUI Features

### Main Window
- **Visual Game Board**: See the entire 134-space board with colored spaces
- **Player Positions**: Real-time tracking of all players with colored markers
- **Game Controls**: Intuitive interface for taking turns

### Player Setup Dialog
- **Player Count Selection**: Choose 1-3 players using a spinner
- **Name Entry**: Custom names for each player
- **Input Validation**: Ensures all players have names before starting

### Game Control Panel
- **Current Turn Display**: Shows whose turn it is
- **Draw Card Button**: Large, easy-to-click button for taking turns
- **Game Status**: Current game state and progress
- **Move Log**: Scrollable text area showing all game moves
- **Player Positions**: Live update of each player's position

### Menu System
- **Game Menu**: Start new games or exit
- **Help Menu**: Access game rules and about information

## Game Controls

- **Draw Card**: Click the "Draw Card" button to take your turn
- **New Game**: Use the Game menu to start over
- **Game Rules**: Access help from the Help menu for rule clarification

## Game Rules

- **Color Cards**: Move to the next space of the matching color
- **Double Color Cards**: Move to the second occurrence of the color
- **Special Character Cards**: Move directly to the character's location
- **No Backward Movement**: Players never move backward with character cards
- **Winning**: Reach or pass position 134 to win

## Classes Overview

### GUI Classes

#### `CandyLandGUI`
- Main application window and entry point for the GUI version
- Manages overall game flow and window layout
- Provides menu system for game options and help

#### `PlayerSetupDialog`
- Modal dialog for initial game setup
- Handles player count selection and name entry
- Validates input before starting the game

#### `GameBoardPanel`
- Visual representation of the 134-space game board
- Draws colored spaces in snake-like pattern
- Shows player positions with colored markers
- Highlights special character locations and finish line

#### `GameControlPanel`
- Game control interface with draw card functionality
- Displays current game state and player information
- Provides scrollable game log of all moves
- Shows real-time player position updates

### Core Game Classes

#### `CandyLandGame`
- Main game controller and logic handler
- Manages game flow, player turns, and win conditions
- Provides API for GUI components to interact with game state
- Handles both console and GUI modes

#### `Player`
- Represents a game player with name and board position
- Tracks player movement and position
- Provides string representation for display

#### `Board`
- Represents the 134-space game board
- Manages colored spaces and special character locations
- Calculates movement based on cards drawn
- Determines winning conditions

#### `Card`
- Represents game cards (color or character cards)
- Supports both regular and double color cards
- Handles special character card types

#### `Deck`
- Manages the deck of game cards
- Handles shuffling and card drawing
- Auto-reshuffles when empty

#### `Color`
- Enumeration of board colors: RED, PURPLE, YELLOW, BLUE, ORANGE, GREEN
- Provides color mapping for visual representation

## Testing

The project includes comprehensive unit tests with code coverage reporting:

- `PlayerTest`: Tests player creation, movement, and state management
- `BoardTest`: Tests board functionality, special locations, and win conditions
- `CandyLandGameTest`: Tests core game logic and player management

### Running Tests Locally
```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report (opens in browser)
open target/site/jacoco/index.html
```

### Code Quality Analysis
```bash
# Run SonarQube analysis locally (requires SONAR_TOKEN and SONAR_HOST_URL environment variables)
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=candy-land-java-game \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.login=$SONAR_TOKEN
```

## Development

### Code Style
- Follow Java naming conventions
- Use clear, descriptive method and variable names
- Include Javadoc comments for public methods
- Maintain separation of concerns between classes

### Contributing
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes and add tests
4. Run tests locally: `mvn test`
5. Ensure code quality: Check SonarQube analysis results
6. Commit your changes: `git commit -m "Add your feature"`
7. Push to the branch: `git push origin feature/your-feature`
8. Submit a pull request

### Adding Features
- New game rules can be added to the `Board` class
- Additional card types can be implemented in the `Card` class
- Game variations can be added to `CandyLandGame`
- GUI enhancements can be made to the components in the `gui` package

### Code Quality Standards
The project maintains high code quality through:
- **Unit Testing**: Comprehensive test coverage for all core functionality
- **Static Analysis**: SonarQube analysis for code quality, security, and maintainability
- **Code Coverage**: JaCoCo integration for test coverage reporting
- **Continuous Integration**: Automated testing and analysis on every commit

## License

This project is for educational purposes and implements the classic Candy Land game mechanics.