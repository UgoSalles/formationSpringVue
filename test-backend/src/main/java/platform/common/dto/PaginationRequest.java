package platform.common.dto;

public record PaginationRequest(int page, int limit) {

    public PaginationRequest {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;
    }

    public static PaginationRequest defaults() {
        return new PaginationRequest(1, 10);
    }
}
