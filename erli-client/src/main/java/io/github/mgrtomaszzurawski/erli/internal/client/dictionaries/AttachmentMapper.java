package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.model.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.core.model.Market;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentRemoval;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewAttachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ProductAttachmentResult;
import io.github.mgrtomaszzurawski.erli.rest.model.AddAttachmentRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.AttachmentResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.DeleteAttachmentsResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.GetAttachmentsResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.GetAttachmentsResponseInnerCreated;
import io.github.mgrtomaszzurawski.erli.rest.model.GetAttachmentsResponseInnerCreatedUser;
import io.github.mgrtomaszzurawski.erli.rest.model.ManageAttachedProducts;
import io.github.mgrtomaszzurawski.erli.rest.model.PatchAttachmentRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps between the generated Layer-1 attachment schemas and the public {@link Attachment} domain
 * record. {@code AttachmentResponse} (single) and {@code GetAttachmentsResponseInner} (list element)
 * are separate generated types with the same shape, so both are mapped through one private form.
 * Internal: never exported.
 */
final class AttachmentMapper {

    private static final String ATTACHMENT_ID_LABEL = "attachment id";
    private static final String PRODUCT_ID_LABEL = "product id";

    private AttachmentMapper() {
    }

    static Attachment toDomain(GetAttachmentsResponseInner rawAttachment) {
        requireBody(rawAttachment, "GET /dictionaries/attachments");
        return build(
                rawAttachment.getId(),
                rawAttachment.getShopId(),
                rawAttachment.getVersion(),
                rawAttachment.getName(),
                rawAttachment.getOriginalName(),
                rawAttachment.getFilePath(),
                rawAttachment.getKind() == null ? null : rawAttachment.getKind().getValue(),
                rawAttachment.getBaseUrl(),
                rawAttachment.getType(),
                rawAttachment.getAttachedProductIds(),
                rawAttachment.getCreated(),
                rawAttachment.getUpdated());
    }

    static Attachment toDomain(AttachmentResponse rawAttachment) {
        requireBody(rawAttachment, "the attachment endpoint");
        return build(
                rawAttachment.getId(),
                rawAttachment.getShopId(),
                rawAttachment.getVersion(),
                rawAttachment.getName(),
                rawAttachment.getOriginalName(),
                rawAttachment.getFilePath(),
                rawAttachment.getKind() == null ? null : rawAttachment.getKind().getValue(),
                rawAttachment.getBaseUrl(),
                rawAttachment.getType(),
                rawAttachment.getAttachedProductIds(),
                rawAttachment.getCreated(),
                rawAttachment.getUpdated());
    }

    static AddAttachmentRequest toCreateRequest(NewAttachment attachment) {
        Objects.requireNonNull(attachment, "attachment");
        return new AddAttachmentRequest()
                .kind(toRequestKind(attachment.kind()))
                .name(attachment.name())
                .originalName(attachment.originalName())
                .filePath(attachment.filePath())
                .markets(toMarketValues(attachment.markets()));
    }

    static PatchAttachmentRequest toPatchRequest(AttachmentUpdate update) {
        Objects.requireNonNull(update, "update");
        PatchAttachmentRequest request = new PatchAttachmentRequest().id(toIntId(update.id(), ATTACHMENT_ID_LABEL));
        if (update.filePath() != null) {
            request.filePath(update.filePath());
        }
        if (update.name() != null) {
            request.name(update.name());
        }
        if (update.originalName() != null) {
            request.originalName(update.originalName());
        }
        // The generated model initialises `markets` to an empty list, which would be serialised as
        // "markets":[] — a value the API rejects (minItems: 1). AttachmentUpdate refuses an empty list
        // outright, so null here only ever means "the caller left markets alone"; nulling the field
        // lets the codec's NON_NULL inclusion drop the key entirely.
        request.markets(update.markets() == null ? null : toMarketValues(update.markets()));
        return request;
    }

    static ManageAttachedProducts toAttachRequest(long attachmentId, List<Long> productIds) {
        return manageRequest(ManageAttachedProducts.ActionEnum.ATTACH, attachmentId, productIds);
    }

    static ManageAttachedProducts toDetachRequest(long attachmentId, List<Long> productIds) {
        return manageRequest(ManageAttachedProducts.ActionEnum.DETACH, attachmentId, productIds);
    }

    private static ManageAttachedProducts manageRequest(
            ManageAttachedProducts.ActionEnum action, long attachmentId, List<Long> productIds) {
        Objects.requireNonNull(productIds, "productIds");
        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("productIds must name at least one product");
        }
        return new ManageAttachedProducts()
                .action(action)
                .attachmentId(toIntId(attachmentId, ATTACHMENT_ID_LABEL))
                .productIds(productIds.stream().map(productId -> toIntId(productId, PRODUCT_ID_LABEL)).toList());
    }

    /**
     * An empty or 204 body decodes to {@code null}; surface that as a transport fault rather than
     * letting it escape as a {@link NullPointerException} from a mapper.
     */
    private static void requireBody(Object body, String operation) {
        if (body == null) {
            throw new ErliTransportException(operation + " returned no response body");
        }
    }

    /**
     * Narrow a domain {@code long} id to the {@code int} Layer 1 declares, refusing to wrap silently —
     * a truncated id would address a different, existing attachment or product.
     */
    private static int toIntId(long id, String what) {
        try {
            return Math.toIntExact(id);
        } catch (ArithmeticException tooLarge) {
            throw new IllegalArgumentException(
                    "The API cannot represent this " + what + ": " + id + " exceeds the 32-bit range", tooLarge);
        }
    }

    private static AddAttachmentRequest.KindEnum toRequestKind(AttachmentKind kind) {
        AddAttachmentRequest.KindEnum resolved = AddAttachmentRequest.KindEnum.fromValue(kind.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException("The API does not accept attachment kind: " + kind.wireValue());
        }
        return resolved;
    }

    static ProductAttachmentResult toProductAttachmentResult(ManageAttachedProductsResponse rawResult) {
        if (rawResult == null) {
            // Defensive: an empty body would mean the API stopped reporting per-product outcomes.
            return new ProductAttachmentResult(true, List.of(), List.of());
        }
        List<Long> updated = rawResult.getUpdated() == null
                ? List.of()
                : rawResult.getUpdated().stream().map(Integer::longValue).toList();
        List<ProductAttachmentResult.ProductError> errors = rawResult.getErrors() == null
                ? List.of()
                : rawResult.getErrors().stream()
                        .map(error -> new ProductAttachmentResult.ProductError(
                                Optional.ofNullable(error.getProductId()).map(Integer::longValue),
                                error.getError()))
                        .toList();
        // A body that omits `ok` but reports no errors is a success; only an explicit false is not.
        boolean ok = rawResult.getOk() == null ? errors.isEmpty() : rawResult.getOk();
        return new ProductAttachmentResult(ok, updated, errors);
    }

    /**
     * The delete request body is a bare JSON array of ids, so the generator emits no class for it and
     * the body is the list itself.
     */
    static List<Integer> toDeleteRequest(List<Long> attachmentIds) {
        Objects.requireNonNull(attachmentIds, "attachmentIds");
        if (attachmentIds.isEmpty()) {
            throw new IllegalArgumentException("attachmentIds must name at least one attachment");
        }
        return attachmentIds.stream().map(attachmentId -> toIntId(attachmentId, ATTACHMENT_ID_LABEL)).toList();
    }

    static AttachmentRemoval toRemoval(DeleteAttachmentsResponse rawRemoval) {
        requireBody(rawRemoval, "DELETE /dictionaries/attachments");
        List<Long> removed = rawRemoval.getRemovedAttachments() == null
                ? List.of()
                : rawRemoval.getRemovedAttachments().stream().map(Integer::longValue).toList();
        List<String> errors = rawRemoval.getErrors() == null
                ? List.of()
                : rawRemoval.getErrors().stream().map(String::valueOf).toList();
        return new AttachmentRemoval(removed, errors);
    }

    private static List<String> toMarketValues(List<Market> markets) {
        return markets.stream().map(Market::wireValue).toList();
    }

    private static Attachment build(
            Integer id,
            Integer shopId,
            Integer version,
            String name,
            String originalName,
            String filePath,
            String kindWireValue,
            String baseUrl,
            String type,
            List<Integer> attachedProductIds,
            GetAttachmentsResponseInnerCreated created,
            GetAttachmentsResponseInnerCreated updated) {
        return new Attachment(
                requireNumber(id, "id"),
                requireNumber(shopId, "shopId"),
                requireNumber(version, "version"),
                require(name, "name"),
                require(originalName, "originalName"),
                require(filePath, "filePath"),
                Optional.ofNullable(kindWireValue).map(AttachmentKind::fromWire),
                Optional.ofNullable(baseUrl),
                Optional.ofNullable(type),
                attachedProductIds == null
                        ? List.of()
                        : attachedProductIds.stream().map(Integer::longValue).toList(),
                toAudit(requireCreated(created)),
                Optional.ofNullable(updated).map(AttachmentMapper::toAudit));
    }

    private static GetAttachmentsResponseInnerCreated requireCreated(GetAttachmentsResponseInnerCreated created) {
        if (created == null) {
            throw new IllegalStateException("Attachment is missing the required 'created' field");
        }
        return created;
    }

    private static Attachment.AttachmentAudit toAudit(GetAttachmentsResponseInnerCreated rawAudit) {
        if (rawAudit.getTime() == null) {
            throw new IllegalStateException("Attachment audit entry is missing the required 'time' field");
        }
        GetAttachmentsResponseInnerCreatedUser user = rawAudit.getUser();
        if (user == null || user.getUserId() == null) {
            throw new IllegalStateException("Attachment audit entry is missing the required 'user.userId' field");
        }
        return new Attachment.AttachmentAudit(
                rawAudit.getTime(), user.getUserId(), Optional.ofNullable(user.getEmail()));
    }

    private static long requireNumber(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Attachment is missing the required '" + fieldName + "' field");
        }
        return value.longValue();
    }

    private static String require(String value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Attachment is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
