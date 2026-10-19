package de.raindancer.modules.rift;

import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What the module declares at bootstrap, and how it behaves before it is running. */
class RiftCommandsTest {

    @Test
    @DisplayName("/rift is declared with its aliases, without the module running")
    void declared() {
        List<ModuleCommand> declared = RiftCommands.declared();

        assertThat(declared).hasSize(1);
        assertThat(declared.getFirst().name()).isEqualTo("rift");
        assertThat(declared.getFirst().names()).contains("pv", "playervault");
        assertThat(declared.getFirst().options()).contains("sort", "[search words]");
        assertThat(declared.getFirst().handler()).isNotNull();
    }

    @Test
    @DisplayName("running it while the module is stopped says so rather than throwing a null")
    void stopped() {
        RiftCommands.stopped();

        assertThat(RiftCommands.isRunning()).isFalse();
        assertThatThrownBy(() -> RiftCommands.declared().getFirst().handler().execute(null, new String[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not running");
    }
}
