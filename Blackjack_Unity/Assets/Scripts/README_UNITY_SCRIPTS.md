# Unity Scripts - Blackjack Multiplayer Game

This folder contains all the C# scripts needed for the Unity client of the Blackjack multiplayer game.

## Folder Structure

```
UnityScripts/
├── Models/              # Data models and message types
├── Network/             # WebSocket client and networking
├── Managers/            # Singleton managers for game systems
├── UI/                  # UI panel controllers
└── Game/                # Game logic and visual components
```

## Setup Instructions

### 1. Prerequisites

Install the following packages in your Unity project:

1. **WebSocket Library** (Choose one):
   - **NativeWebSocket** (Recommended for WebGL)
     - Install via Package Manager: `https://github.com/endel/NativeWebSocket.git#upm`
   - **WebSocket-Sharp** (For standalone builds)
     - Download from: https://github.com/sta/websocket-sharp

2. **Newtonsoft.Json** (JSON serialization)
   - Install via Package Manager: `com.unity.nuget.newtonsoft-json`

3. **TextMeshPro** (UI text)
   - Should be included in Unity 2018.1+
   - Window → TextMeshPro → Import TMP Essential Resources

### 2. Import Scripts

1. Copy all scripts from the `UnityScripts` folder to your Unity project's `Assets/Scripts` folder
2. Create the following folder structure in Unity:
   ```
   Assets/
   ├── Scripts/
   │   ├── Models/
   │   ├── Network/
   │   ├── Managers/
   │   ├── UI/
   │   └── Game/
   └── Resources/
       └── Cards/  (for card sprites)
   ```

### 3. Configure WebSocket Client

1. Open `Network/WebSocketClient.cs`
2. **Uncomment the code** for your chosen WebSocket library:
   - For **NativeWebSocket**: Uncomment the sections marked `FOR NATIVEWEBSOCKET`
   - For **WebSocket-Sharp**: Uncomment the sections marked `FOR WEBSOCKET-SHARP`
3. Update the `serverUrl` field to match your server address (e.g., `ws://localhost:8080`)

### 4. Create Scenes

Create three Unity scenes:

#### Scene 1: Login
- Create a new scene: `Assets/Scenes/Login.unity`
- Add the following GameObjects:
  - **Canvas** (UI)
    - LoginPanel (see UI Setup below)
    - RegisterPanel (see UI Setup below)
  - **NetworkManager** (empty GameObject)
    - Add components: `WebSocketClient`, `AuthManager`

#### Scene 2: MainMenu
- Create a new scene: `Assets/Scenes/MainMenu.unity`
- Add the following GameObjects:
  - **Canvas** (UI)
    - MainMenuPanel
    - ServerListPanel
    - LeaderboardPanel
  - **Managers** (empty GameObject)
    - Add components: `UIManager`, `GameManager`, `LobbyManager`

#### Scene 3: GameTable
- Create a new scene: `Assets/Scenes/GameTable.unity`
- Add GameTablePanel with all game UI elements

### 5. UI Setup

#### LoginPanel Setup
Create a panel with:
- Username InputField
- Password InputField
- Login Button
- "Go to Register" Button
- Error Text

Assign these to the `LoginPanel.cs` script component.

#### RegisterPanel Setup
Create a panel with:
- Username InputField
- Password InputField
- Confirm Password InputField
- Register Button
- "Go to Login" Button
- Error Text

Assign these to the `RegisterPanel.cs` script component.

#### MainMenuPanel Setup
Create a panel with:
- Username Text
- Balance Text
- Points Text
- Quick Match Button
- Server List Button
- Leaderboard Button
- Logout Button

#### GameTablePanel Setup
This is the most complex panel. Create:

**Dealer Area:**
- Dealer card container (Horizontal Layout Group)
- Dealer value text

**Player Slots (5):**
- Create 5 PlayerSlot GameObjects
- Each with: username text, balance text, bet text, hand value text, card container

**Center:**
- Pot text
- Phase text
- Current turn text

**Betting Panel:**
- Bet slider
- Bet amount text
- Place Bet button
- Quick bet buttons (optional)

**Game Actions Panel:**
- Hit button
- Stand button

**Result Panel:**
- Result text
- Play Again button
- Exit button

### 6. Create Prefabs

#### Card Prefab
1. Create: `Assets/Prefabs/Card.prefab`
2. Structure:
   - Card (GameObject)
     - Image component (for card visual)
     - Add `Card.cs` script
3. Assign card back sprite to the script

#### PlayerSlot Prefab
1. Create: `Assets/Prefabs/PlayerSlot.prefab`
2. Include all UI elements from the PlayerSlot script
3. Assign references in the `PlayerSlot.cs` component

#### ServerListItem Prefab
1. Create: `Assets/Prefabs/ServerListItem.prefab`
2. Include: Room ID text, Players text, Min Bet text, Join button

#### LeaderboardItem Prefab
1. Create: `Assets/Prefabs/LeaderboardItem.prefab`
2. Include: Rank text, Username text, Points text, Stats text

### 7. Add Card Sprites

1. Create folder: `Assets/Resources/Cards/`
2. Add 52 card sprites with naming convention:
   - `hearts_A.png`, `hearts_2.png`, ..., `hearts_K.png`
   - `diamonds_A.png`, `diamonds_2.png`, ..., `diamonds_K.png`
   - `clubs_A.png`, `clubs_2.png`, ..., `clubs_K.png`
   - `spades_A.png`, `spades_2.png`, ..., `spades_K.png`
3. Add card back sprite: `card_back.png`

You can find free card assets at:
- https://opengameart.org/
- https://kenney.nl/assets
- Create your own using vector graphics

## Script Descriptions

### Models/
- **UserData.cs** - User account data
- **CardData.cs** - Playing card data
- **PlayerData.cs** - Player in game data
- **GameStateData.cs** - Complete game state
- **ServerInfo.cs** - Available server/room info
- **MessageTypes.cs** - WebSocket message structures

### Network/
- **WebSocketClient.cs** - Manages WebSocket connection to server
  - Singleton pattern
  - Handles connect/disconnect
  - Sends/receives JSON messages
  - Routes messages to appropriate handlers

### Managers/
- **AuthManager.cs** - Authentication system
  - Login/Register
  - Session management
  - User data storage

- **GameManager.cs** - Game flow manager
  - Join/leave rooms
  - Place bets
  - Hit/Stand actions
  - Game state updates

- **UIManager.cs** - UI panel management
  - Show/hide panels
  - Notifications
  - Panel transitions

- **LobbyManager.cs** - Lobby functionality
  - Server list
  - Leaderboard
  - Quick match queue

### UI/
- **LoginPanel.cs** - Login interface controller
- **RegisterPanel.cs** - Registration interface controller
- **MainMenuPanel.cs** - Main menu controller
- **ServerListPanel.cs** - Server browser controller
- **LeaderboardPanel.cs** - Leaderboard display controller
- **GameTablePanel.cs** - Main game UI controller

### Game/
- **Card.cs** - Visual card component
- **CardDealer.cs** - Card dealing animations
- **PlayerSlot.cs** - Player slot UI component
- **BettingController.cs** - Betting UI controller

## Usage Flow

### 1. Start Application
```
Login Scene loads
↓
WebSocketClient connects to server
↓
User logs in or registers
↓
Navigate to Main Menu
```

### 2. Quick Match
```
Click Quick Match button
↓
GameManager.QuickMatch()
↓
Server assigns room
↓
Navigate to Game Table
↓
Game starts
```

### 3. Game Flow
```
Betting Phase
↓ (all players bet)
Dealing Phase (cards dealt)
↓
Player Turns Phase
↓ (players hit/stand)
Dealer Turn Phase
↓
Results Phase
↓
Play Again or Exit
```

## Event System

The application uses C# events for communication between components:

```csharp
// Subscribe to events
AuthManager.Instance.OnLoginSuccess += HandleLoginSuccess;
GameManager.Instance.OnGameStateUpdated += HandleGameStateUpdate;

// Unsubscribe in OnDestroy
AuthManager.Instance.OnLoginSuccess -= HandleLoginSuccess;
```

## Important Notes

1. **Persistent Managers**: AuthManager, GameManager, LobbyManager, and WebSocketClient use `DontDestroyOnLoad()` to persist across scenes.

2. **WebSocket Connection**: Make sure to connect to the WebSocket server before attempting any operations.

3. **Error Handling**: All network operations include error handling and user notifications.

4. **Thread Safety**: Messages from WebSocket are queued and processed on the main Unity thread.

## Customization

### Change Server URL
In `WebSocketClient.cs`:
```csharp
public string serverUrl = "ws://your-server-address:8080";
```

### Modify Bet Limits
In `BettingController.cs`:
```csharp
public float minBet = 10f;
public float maxBet = 1000f;
```

### Adjust Animation Speed
In `CardDealer.cs`:
```csharp
public float dealDuration = 0.5f;
public float dealDelay = 0.2f;
```

## Troubleshooting

### WebSocket Connection Fails
- Check that server is running and accessible
- Verify server URL is correct
- Check firewall settings
- Try using IP address instead of localhost

### Cards Not Displaying
- Ensure card sprites are in `Resources/Cards/` folder
- Check sprite naming convention matches `CardData.GetSpriteName()`
- Verify card prefab is assigned in CardDealer

### UI Not Responding
- Check that all UI references are assigned in Inspector
- Verify EventSystem exists in scene
- Check that buttons have onClick listeners

## Next Steps

1. Add audio effects (card shuffle, deal, win/lose sounds)
2. Implement card animations (DOTween)
3. Add particle effects for wins
4. Create custom card designs
5. Add settings panel (sound volume, etc.)
6. Implement chat system
7. Add player avatars
8. Mobile touch controls

## Support

For issues or questions, refer to the main README.md in the project root.
