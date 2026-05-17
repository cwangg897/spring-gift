package gift.order;

public record OrderCompletedEvent(
    Long orderId,
    Long memberId,
    String kakaoAccessToken,
    Long optionId,
    String optionName,
    int quantity,
    String message,
    String productName,
    int productPrice
) {
}
