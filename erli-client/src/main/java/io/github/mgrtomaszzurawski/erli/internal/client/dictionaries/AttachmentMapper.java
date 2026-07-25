package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Market;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewAttachment;
import io.github.mgrtomaszzurawski.erli.rest.model.AddAttachmentRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.AttachmentResponse;
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

    private AttachmentMapper() {
    }

    static Attachment toDomain(GetAttachmentsResponseInner rawAttachment) {
        Objects.requireNonNull(rawAttachment, "raw GetAttachmentsResponseInner");
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
        Objects.requireNonNull(rawAttachment, "raw AttachmentResponse");
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
        PatchAttachmentRequest request = new PatchAttachmentRequest().id((int) update.id());
        if (update.filePath() != null) {
            request.filePath(update.filePath());
        }
        if (update.name() != null) {
            request.name(update.name());
        }
        if (update.originalName() != null) {
            request.originalName(update.originalName());
        }
        // The generated model initialises `markets` to an empty list, and on a PATCH an empty array is
        // an explicit "clear the markets" rather than "leave them alone". Null it out when the caller
        // did not set it so the codec's NON_NULL inclusion drops the key entirely.
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
                .attachmentId((int) attachmentId)
                .productIds(productIds.stream().map(Long::intValue).toList());
    }

    private static AddAttachmentRequest.KindEnum toRequestKind(AttachmentKind kind) {
        AddAttachmentRequest.KindEnum resolved = AddAttachmentRequest.KindEnum.fromValue(kind.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException("The API does not accept attachment kind: " + kind.wireValue());
        }
        return resolved;
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
