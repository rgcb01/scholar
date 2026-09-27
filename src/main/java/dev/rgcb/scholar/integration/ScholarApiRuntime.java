package dev.rgcb.scholar.integration;

import dev.rgcb.scholar.api.ScholarApi;
import dev.rgcb.scholar.application.FileScholarDocumentRepository;
import dev.rgcb.scholar.application.FileDocumentRecoveryService;
import dev.rgcb.scholar.application.RecoveryPolicy;
import dev.rgcb.scholar.application.ScholarApplication;
import java.nio.file.Path;
import java.time.Clock;
import net.minecraft.client.Minecraft;

/** Client-owned bridge. A single application instance is shared with the production Home screen. */
public final class ScholarApiRuntime {
    private static Path root;
    private static ScholarApplication application;
    private static ScholarApi api;

    private ScholarApiRuntime() {}

    public static synchronized ScholarApi get() {
        application();
        return api;
    }

    public static synchronized ScholarApplication application() {
        var client = Minecraft.getInstance();
        if (client == null) throw new IllegalStateException("Scholar client is not initialized.");
        var currentRoot = client.gameDirectory.toPath().resolve("scholar").toAbsolutePath();
        if (!currentRoot.equals(root)) {
            root = currentRoot;
            application = new ScholarApplication(new FileScholarDocumentRepository(root),
                    new FileDocumentRecoveryService(root.resolve("recovery")), Clock.systemUTC(), RecoveryPolicy.DEFAULT);
            api = new ScholarApiService(application, client::isSameThread);
        }
        return application;
    }

    public static synchronized void tickRecovery() {
        if (application != null) application.recoveryTick();
    }

    public static synchronized void captureRecoveryNow() {
        if (application != null) application.captureRecoveryNow();
    }
}
