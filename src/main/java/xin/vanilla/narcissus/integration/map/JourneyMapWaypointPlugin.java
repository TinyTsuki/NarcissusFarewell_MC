package xin.vanilla.narcissus.integration.map;

import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;

import java.io.File;

public final class JourneyMapWaypointPlugin implements IClientPlugin {
    @Override
    public String getModId() { return JourneyMap.MOD_ID; }

    @Override
    public void initialize(IClientAPI api) {
        try {
            JourneyMapApiContract.method(IClientAPI.class, "getDataPath", File.class, String.class);
            JourneyMap.Adapter store = LegacyJourneyMapAdapter.bind(getClass().getClassLoader());
            if (store == null) throw new ClassNotFoundException("JourneyMap waypoint store unavailable");
            JourneyMap.install(new ApiAdapter(api, store));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            JourneyMap.fail(error);
        }
    }

    @Override
    public void onEvent(ClientEvent event) { }

    static final class ApiAdapter implements JourneyMap.Adapter {
        private final IClientAPI api;
        private final JourneyMap.Adapter store;

        ApiAdapter(IClientAPI api, JourneyMap.Adapter store) {
            this.api = api;
            this.store = store;
        }

        @Override
        public boolean ready() throws ReflectiveOperationException {
            return api.getDataPath(JourneyMap.MOD_ID) != null && store.ready();
        }

        @Override
        public void add(JourneyMap.Point point) throws ReflectiveOperationException { store.add(point); }

        @Override
        public void remove(JourneyMap.Point point) throws ReflectiveOperationException { store.remove(point); }
    }
}
