package dev.rgcb.scholar.api;

import dev.rgcb.scholar.api.document.ScholarDocuments;
import dev.rgcb.scholar.api.quantity.ScholarUnits;
import dev.rgcb.scholar.api.event.ScholarEvents;
import dev.rgcb.scholar.integration.ScholarApiRuntime;

/** Supported entry point for ordinary NeoForge addons running with Scholar on the client. */
public interface ScholarApi {
    /** Returns the active client API. Call after Minecraft client initialization. */
    static ScholarApi get() { return ScholarApiRuntime.get(); }

    /** API compatibility generation; independent of the mod version. */
    default int majorVersion() { return 1; }
    default int minorVersion() { return 0; }

    ScholarDocuments documents();
    ScholarUnits units();
    ScholarEvents events();
}
