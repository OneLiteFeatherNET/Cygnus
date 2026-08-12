package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.hud.HudComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHudContainer {

    private final Map<Class<? extends HudComponent>, HudComponent> components = new ConcurrentHashMap<>();

    public <T extends HudComponent> void register(Class<T> clazz, T component) {
        components.put(clazz, component);
    }

    @SuppressWarnings("unchecked")
    public <T extends HudComponent> Optional<T> get(Class<T> clazz) {
        return Optional.ofNullable((T) components.get(clazz));
    }

    public void remove(Class<? extends HudComponent> clazz) {
        HudComponent removed = components.remove(clazz);
        if (removed != null) {
            removed.hide();
        }
    }

    public void renderAll() {
        components.values().stream()
                .filter(HudComponent::isVisible)
                .forEach(HudComponent::render);
    }

    public void hideAll() {
        components.values().forEach(HudComponent::hide);
    }

    public Collection<HudComponent> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }
}
