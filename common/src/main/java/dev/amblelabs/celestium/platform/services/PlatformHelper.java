package dev.amblelabs.celestium.platform.services;

public interface PlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return {@code true} if in a development environment, {@code false} otherwise.
     */
    boolean isDevelopmentEnvironment();
}