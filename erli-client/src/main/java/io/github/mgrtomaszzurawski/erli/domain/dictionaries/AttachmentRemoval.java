package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;
import java.util.Objects;

/**
 * The outcome of {@link DictionariesAccess#deleteAttachments(List)}.
 *
 * <p>The API reports removals and failures separately rather than failing the whole call, so a
 * partial success is normal — check {@link #isComplete()}.
 *
 * @param removedAttachmentIds the attachments that were removed
 * @param errors the failures the API reported, verbatim; empty when everything was removed
 */
public record AttachmentRemoval(List<Long> removedAttachmentIds, List<String> errors) {

    public AttachmentRemoval {
        removedAttachmentIds = List.copyOf(Objects.requireNonNull(removedAttachmentIds, "removedAttachmentIds"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    /** Whether every requested attachment was removed. */
    public boolean isComplete() {
        return errors.isEmpty();
    }
}
