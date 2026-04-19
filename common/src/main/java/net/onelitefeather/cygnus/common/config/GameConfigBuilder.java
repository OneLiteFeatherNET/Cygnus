package net.onelitefeather.cygnus.common.config;

public final class GameConfigBuilder implements GameConfig.Builder {

    private int minPlayers;
    private int maxPlayers;
    private int lobbyTime;
    private int maxGameTime;
    private int slenderTeamSize;
    private int survivorTeamSize;

    @Override
    public GameConfig.Builder minPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        return this;
    }

    @Override
    public GameConfig.Builder maxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    @Override
    public GameConfig.Builder lobbyTime(int lobbyTime) {
        if (lobbyTime <= GameConfig.FORCE_START_TIME) {
            throw new IllegalArgumentException("Lobby time must be greater than " + GameConfig.FORCE_START_TIME);
        }
        this.lobbyTime = lobbyTime;
        return this;
    }

    @Override
    public GameConfig.Builder gameTime(int gameTime) {
        this.maxGameTime = gameTime;
        return this;
    }

    @Override
    public GameConfig.Builder slenderTeamSize(int slenderTeamSize) {
        int minSlenderSize = InternalGameConfig.defaultConfig().slenderTeamSize();
        if (slenderTeamSize < minSlenderSize) {
            throw new IllegalArgumentException("Slender team size must be at least " + minSlenderSize);
        }
        this.slenderTeamSize = slenderTeamSize;
        return this;
    }

    @Override
    public GameConfig.Builder survivorTeamSize(int survivorTeamSize) {
        int minSurvivorSize = InternalGameConfig.defaultConfig().slenderTeamSize() + 1;
        if (survivorTeamSize < minSurvivorSize) {
            throw new IllegalArgumentException("Survivor team size must be at least " + minSurvivorSize);
        }
        this.survivorTeamSize = survivorTeamSize;
        return this;
    }

    @Override
    public GameConfig build() {
        return new GameConfigImpl(minPlayers, maxPlayers, lobbyTime, maxGameTime, slenderTeamSize, survivorTeamSize);
    }
}
