package dev.rgcb.scholar.document;

import java.util.List;
import java.util.Optional;

/** Stable-ID and dependency surface shared by authored computation blocks during transfer. */
public interface ComputationTransferBlock extends BlockNode {
    Optional<String> definedVariableId();

    /** Dependencies in stable authored occurrence order, including repeated references. */
    List<VariableDependencyReference> variableDependencies();

    /** Return an immutable semantic block with only the supplied stable IDs rewritten. */
    ComputationTransferBlock withVariableTransferIds(Optional<String> definedId,
                                                     List<VariableDependencyReference> dependencies);
}
