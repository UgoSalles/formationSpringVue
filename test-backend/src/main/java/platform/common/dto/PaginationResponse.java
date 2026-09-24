package platform.common.dto;

public record PaginationResponse(int page, int limit, long total, int totalPages) {

    public PaginationResponse {
        if (page < 1) page = 1;
        if (limit < 1) limit = 1;
        if (total < 0) total = 0;
        if (totalPages < 1) totalPages = 1;
    }
}
