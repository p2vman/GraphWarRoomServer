package io.p2vman.graphwarplugins.management;

import com.google.auto.service.AutoService;
import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.events.OnAsyncChatMessageEvent;
import io.p2vman.graphwarserver.events.OnCommandsRegisterEvent;
import io.p2vman.graphwarserver.events.OnHandshakeEvent;
import io.p2vman.graphwarserver.events.OnPlayerLoginEvent;
import io.p2vman.graphwarserver.plugin.Plugin;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import net.engio.mbassy.listener.Handler;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Objects;

@AutoService(Plugin.class)
public class ManagementPlugin implements Plugin {
    private final Config config;

    public ManagementPlugin() throws Exception {
        File file = new File("./management.yaml");
        if (!file.exists()) {
            Files.copy(Objects.requireNonNull(ManagementPlugin.class.getClassLoader().getResourceAsStream("management.yaml")), file.toPath());
        }

        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(Config.class, options);
        Yaml yaml = new Yaml(constructor);

        Config config;
        try (FileReader reader = new FileReader(file)) {
            config = yaml.load(reader);
        }
        this.config = config;
    }

    @Override
    public void init(EventLoopGroupType.EventLoopContext ctx) {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onServerStarting(BasicServer server) {
        server.getEventbus().subscribe(this);
    }

    @Handler
    public void onCommandsRegister(OnCommandsRegisterEvent event) {
        var dispatcher = event.getDispatcher();

    }

    @Handler
    public void onPlayerLogin(OnPlayerLoginEvent event) {

    }

    @Handler
    public void onHandshake(OnHandshakeEvent event) {

    }

    @Handler
    public void onChatMessage(OnAsyncChatMessageEvent event) {

    }
}
