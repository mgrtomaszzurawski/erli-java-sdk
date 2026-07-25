package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityStatus;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductBuyability;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.rest.model.CheckBuyabilityRequestInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CheckBuyabilityResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookSave;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductsNeedSyncRequest;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Translates between the {@code hooks} domain records and the generated Layer-1 models, in both
 * directions. Kept in an internal package so no {@code *Raw} type appears in an exported signature.
 * Internal.
 */
final class HookMapper {

    private HookMapper() {
    }

    /**
     * The domain record validates what the API is documented to accept — an {@code https} URL within
     * the length limits. That is the right guard on the write path, but here the data comes back
     * <em>from</em> the server, so a stored subscription that predates those rules (or a malformed URL)
     * would throw a bare {@link IllegalArgumentException} out of {@code list()} and fail the whole call
     * outside the SDK's exception contract. The failure is translated instead, naming the subscription
     * so the caller can fix or delete it.
     */
    static Hook toDomain(HookResponseInner rawHook) {
        Objects.requireNonNull(rawHook, "raw HookResponseInner");
        HookKind kind = toHookKind(rawHook.getHookName());
        String url = requireUrl(rawHook);
        try {
            return new Hook(kind, URI.create(url), Optional.ofNullable(rawHook.getAccessToken()));
        } catch (IllegalArgumentException rejected) {
            throw new ErliTransportException(
                    "The " + kind.wireValue() + " subscription registered on this shop is not usable by the SDK",
                    rejected);
        }
    }

    static HookSave toRaw(Hook hook) {
        HookSave rawHook = new HookSave()
                .hookName(toRawHookName(hook.kind()))
                .url(hook.url().toString());
        hook.accessToken().ifPresent(rawHook::accessToken);
        return rawHook;
    }

    static List<CheckBuyabilityRequestInner> toRaw(List<BuyabilityQuery> queries) {
        Objects.requireNonNull(queries, "queries");
        return queries.stream()
                .map(query -> new CheckBuyabilityRequestInner()
                        .productId(query.productId().value())
                        .quantity(query.quantity()))
                .toList();
    }

    static ProductsNeedSyncRequest toRaw(ProductSyncNotification notification) {
        ProductsNeedSyncRequest rawRequest = new ProductsNeedSyncRequest()
                .externalProductIds(notification.productIds().stream()
                        .map(ProductExternalId::value)
                        .toList());
        // "Whole product" is expressed by leaving `fields` out. The generated model initialises the
        // property to an empty list, which would go on the wire as "fields":[] — a different statement
        // ("sync no fields"), so the field is cleared rather than merely left alone.
        rawRequest.fields(notification.fields().isEmpty() ? null : List.copyOf(notification.fields()));
        return rawRequest;
    }

    static ProductBuyability toDomain(CheckBuyabilityResponseInner rawBuyability) {
        Objects.requireNonNull(rawBuyability, "raw CheckBuyabilityResponseInner");
        return new ProductBuyability(
                ProductExternalId.of(requireProductId(rawBuyability)),
                Optional.ofNullable(rawBuyability.getStatus()).map(HookMapper::toBuyabilityStatus),
                Optional.ofNullable(rawBuyability.getStock()));
    }

    private static String requireUrl(HookResponseInner rawHook) {
        String url = rawHook.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("HookResponseInner is missing the required 'url' field");
        }
        return url;
    }

    private static String requireProductId(CheckBuyabilityResponseInner rawBuyability) {
        String productId = rawBuyability.getProductId();
        if (productId == null || productId.isBlank()) {
            throw new IllegalStateException(
                    "CheckBuyabilityResponseInner is missing the required 'productId' field");
        }
        return productId;
    }

    // Explicit mapping rather than valueOf(name()): the exhaustive switch turns a hook name added by a
    // future spec regeneration into a compile error here instead of a runtime surprise. Same convention
    // as ShopMapper. Note the null branch covers two cases since CORE-12 — an absent hookName, or one
    // the API already serves but this SDK's spec does not describe (the codec decodes that to null).
    private static HookKind toHookKind(HookResponseInner.HookNameEnum rawName) {
        if (rawName == null) {
            throw new IllegalStateException(
                    "hook is missing its 'hookName', or names a hook this SDK version does not recognise");
        }
        return switch (rawName) {
            case CHECK_BUYABILITY -> HookKind.CHECK_BUYABILITY;
            case PRODUCTS_NEED_SYNC -> HookKind.PRODUCTS_NEED_SYNC;
            case ORDER_CREATED -> HookKind.ORDER_CREATED;
            case ORDER_STATUS_CHANGED -> HookKind.ORDER_STATUS_CHANGED;
            case ORDER_SELLER_STATUS_CHANGED -> HookKind.ORDER_SELLER_STATUS_CHANGED;
        };
    }

    private static HookSave.HookNameEnum toRawHookName(HookKind kind) {
        return switch (kind) {
            case CHECK_BUYABILITY -> HookSave.HookNameEnum.CHECK_BUYABILITY;
            case PRODUCTS_NEED_SYNC -> HookSave.HookNameEnum.PRODUCTS_NEED_SYNC;
            case ORDER_CREATED -> HookSave.HookNameEnum.ORDER_CREATED;
            case ORDER_STATUS_CHANGED -> HookSave.HookNameEnum.ORDER_STATUS_CHANGED;
            case ORDER_SELLER_STATUS_CHANGED -> HookSave.HookNameEnum.ORDER_SELLER_STATUS_CHANGED;
        };
    }

    private static BuyabilityStatus toBuyabilityStatus(CheckBuyabilityResponseInner.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case ACTIVE -> BuyabilityStatus.ACTIVE;
            case INACTIVE -> BuyabilityStatus.INACTIVE;
        };
    }
}
